"""
Copyright 2018 Austin Walker Milt

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
"""

# contains classes and methods for processing video processing requests as
# discrete jobs

# ########################################################################### #
# ## IMPORTS AND CONSTANTS ################################################## #
# ########################################################################### #

import os
import youtube_dl, os, sys
here = os.path.dirname(os.path.abspath(__file__))
sys.path = [os.path.join(here, '..')] + sys.path
from detection.videos import Video
from detection.replays import Replays
from detection.intervals import LinkedIntervalSet, Interval
from utilities.options import Options
OPTIONS = Options()
del here

REPLAY_GET_FMT = r'https://api.opendota.com/api/replays?match_id=%s'
REPLAY_KEY_CLUSTER = 'cluster'
REPLAY_KEY_SALT = 'replay_salt'
REPLAY_KEY_MATCH = 'match_id'
REPLAY_KEY_SERIES = 'series_id'
REPLAY_KEY_TYPE = 'series_type'
REPLAY_URL_CONSTRUCTOR = lambda clu, mat, salt: r'http://replay%s.valve.net/570/%s_%s.dem.bz2' % (clu, mat, salt)

EMPTY_ABILITY = u'6251' # ability placeholder for heroes with <6 abilities



# ########################################################################### #
# ## CLASSES ################################################################ #
# ########################################################################### #

# ~~ Time ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ #
class Time:

    KWD_NEG = '-'
    
    def __init__(self, value=0.0):
        """
        Times are objects essentially for storing and converting between clock
        time strings (e.g. 5:55) and match times in seconds (e.g. 33.333).
        
        Args:
            value (float/str): (optional) time value. If of type float, assumed
                to be in seconds. If of type str, assumed to a clock time.
                Default value is 0.0 seconds.
                
        Returns:
            Time
        """
        # try to set the time to seconds
        issec = True
        try: self.seconds = float(value)
        except ValueError: issec = False
        
        if not issec:
            self.seconds = self.clock_to_seconds(value)
            
            
    @staticmethod
    def _split_clock_(clock):
        """
        Splits the clock string into minutes and seconds, including indicating
        negative time.
        """

        # determine if it's a negative time
        clock = clock.strip()
        if clock.startswith(Time.KWD_NEG): negative = True
        else: negative = False
        
        # split the minutes from the seconds
        minutes, seconds = clock.split(OPTIONS.CT.SEPARATOR)
        if negative: minutes = minutes[1:]
        minutes = int(minutes)
        seconds = int(seconds)
        
        return (minutes, seconds, negative)
        
        
    @staticmethod
    def _merge_clock_(minutes, seconds, negative):
        """
        Merges clock parts into a clock string, including negative time.
        """
        clock = {True: Time.KWD_NEG, False: ''}.get(negative)
        clock += '%i' % int(minutes) + OPTIONS.CT.SEPARATOR + '%02.0f' % seconds
        return clock
        
            
    @staticmethod
    def clock_to_seconds(clock):
        """
        Converts a clock string to a seconds float. Generally dont need to
        use directly.
        """
        minutes, seconds, negative = Time._split_clock_(clock)
        time = minutes*60. + seconds
        if negative: time *= -1
        return time
        
        
    @staticmethod
    def seconds_to_clock(value):
        """
        Converts a seconds float to a clock string. Generally dont need to use
        directly.
        """
        if value < 0: 
            negative = True
            value *= -1
        else: negative = False
        minutes = int(value / 60.)
        seconds = value % 60.
        return Time._merge_clock_(minutes, seconds, negative)
        
        
    def get_clock(self):
        """
        Gets the clock string from this Time's seconds value.
        """
        return self.seconds_to_clock(self.seconds)
        

        
# for printing stuff during command-line execution that may be going to remote
# destination
def _pg_(msg):
    print '\n' + msg
    sys.stdout.write('\n' + msg)
    sys.stdout.flush()
    
# ~~ Job ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ #
class Job:
    
    def __init__(self, videoFile, replayFiles=None, keyFile=None, skip=None):
        """
        This class is used to process videos and replay files to package match
        data at regular intervals for the client to view.
        
        Args:
            videoFile (str): path to video file to process
            
            replayFiles (list): nested list; each element corresponds to the
                starting time and file associated with the replay, i.e.
                [[start_1, replay_1], [start_2, replay_2]... ]
                
            keyFile (str): (optional) path to keys dict cPickle for translating 
                from hero/item/unit npc_dota names to ID's to pass to the
                client. Default is job.keymap_default
                
            skip (double): (optional) number of seconds to skip between
                detection events in the video. Default is 
                video_parser.parse_skip_default
                
        """
        assert os.path.exists(videoFile), 'Cannot locate video file.'
        if replayFiles is None: replayFiles = []
        for _, replayFile in replayFiles:
            assert os.path.exists(replayFile), 'Cannot locate replay file %s' % replayFile
            
        if keyFile is None: keyFile = OPTIONS.JB.KEYS
        if skip is None: skip = OPTIONS.VP.PARSE_SKIP_DEFAULT
            
        # initialize job run variables
        self._v = videoFile
        self._r = replayFiles
        self._k = keyFile
        self.skip = skip
        self.video = None
        self.replays = None
        self.keys = {}
        self.results = None
        self.valid = None
        
        
    def __repr__(self):
        return '<Job@%s>' % str(id(self))
        
        
    @staticmethod
    def build_valid_labels(replayIntervals, keys={}):
        validLabels = {}
        nd = OPTIONS.ND.KEY
        validHeroes = LinkedIntervalSet([])
        for interval in replayIntervals:
            intervalHeroes = set([keys.get(h, h).upper() for h in interval.data.heroes])
            newInterval = Interval(intervalHeroes, interval.start, interval.end)
            validHeroes.add(newInterval)
            
        validLabels[OPTIONS.ND.KEY] = validHeroes
        return validLabels
        
        
    def run(self):
        """
        Runs the job, i.e. processes video and replay files for detection and
        formats results for output.
        """
        import cPickle, bz2
        from uuid import uuid4
        
        # try loading the mapping from npc_dota name to regular name
        if os.path.exists(self._k):
            self.keys = cPickle.load(open(self._k, 'rb'))
            
        temp = []
        try:
            # try decompressing the replay files if they are compressed (as is
            # standard from the dota database). Assume they are already
            # decompressed if an IOError is thrown
            replays = []
            for start, f in self._r:
                if not os.path.exists(OPTIONS.JB.SCRATCH): os.makedirs(OPTIONS.JB.SCRATCH)
                tf = os.path.join(OPTIONS.JB.SCRATCH, str(uuid4()))
                compressed = bz2.BZ2File(f, 'rb')                    
                with open(tf, 'wb') as oh:
                    temp.append(tf)
                    try:
                        oh.write(compressed.read())
                        replays.append([start, tf])
                        
                    except IOError: replays.append([start, f])
                
            # load the replays and use them to determine valid labels for video
            # parsing. If there are no replays, then pass nothing as valid labels
            vidLabelFile = None
            if len(replays) == 0:
                self.replays = []
                self.valid = None
                
            else:
                self.replays = Replays.from_dems(replays)
                self.valid = Job.build_valid_labels(self.replays, self.keys)
                if not os.path.exists(OPTIONS.JB.SCRATCH): os.makedirs(OPTIONS.JB.SCRATCH)
                vidLabelFile = os.path.abspath(os.path.join(OPTIONS.JB.SCRATCH, str(uuid4())))
                temp.append(vidLabelFile)
                _ = Video.write_valid_labels(self.valid, vidLabelFile)
            
            # process the video
            self.video = Video.from_video(self._v, vidLabelFile, self.skip)
            
        # delete temporary files
        finally:
            for f in temp:
                if os.path.exists(f):
                    try: os.remove(f)
                    except: print 'Could not remove temporary file %s' % f
        
        # grab video times from the video processing results and use to summarize
        self.results = JobResults(self)
        
        
    def cleanup(self, delete=True):
        """
        Does cleanup of the job.
        
        Args:
            delete (boolean): (optional) indicates if video and replay files 
                should be deleted as part of the cleanup; default is True
                
        Returns:
            boolean: True if cleanup is successful, False otherwise
        """
        success = True
        if delete:  
            temp = [self._v] + [r[1] for r in self._r]
            for f in temp:
                try: os.remove(f)
                except:
                    print 'Unable to delete temporary file %s' % f
                    success = False
                
        return success
                
        
    @staticmethod
    def from_urls(videoURL, replays=None, keyFile=None, skip=None, quality=None, verbose=False):
        """
        Constructs a new Job by downloading files instead of directly from 
        files on disk.
        
        Args:
            videoURL (str): valid youtube url to the video to download
            
            replays (list): @see <code>Job.__init__</code>, but replace 
                file paths to url paths (or match IDs). Note, this can
                try to download replays from dota's replay database if you
                provide the match ID instead of a URL. However, if the replay
                is no longer on the database, an error will be raised.
                
            keyFile (str): @see <code>Job.__init__</code>
            
            skip (float): @see <code>Job.__init__</code>
            
            quality (float): @see <code>download_youtube_video</code>
            
            verbose (boolean): whether or not to be verbose with output
            
        Returns:
            Job: job constructed from downloadedurls instead of files
            
        """
        try:
            from urlparse import urlparse
            if replays is None: replays = []
            if quality is None: quality = OPTIONS.JB.QUALITY
            if verbose: _pg_('Downloading video %s with requested quality %0.0f' % (videoURL, quality))
            videoFile = download_youtube_video(videoURL, quality=quality)
            temp = []
            temp.append(videoFile)
            replayFiles = []
            for time, replay in replays:
            
                # if it's a url, try to get it from the url.
                #   Otherwise assume it's a match ID and try to get that from our
                #   database or Valve's.
                if verbose: _pg_('Downloading replay %s' % replay)
                urlParts = urlparse(replay)
                if (urlParts.scheme and urlParts.netloc and urlParts.path):
                    replayFile = download_file(replay)
                    
                else: replayFile = download_replay(replay)
                temp.append(replayFile)
                replayFiles.append([time, replayFile])
            
            if verbose: _pg_('Building job.')
            return Job(videoFile, replayFiles, keyFile, skip)
            
        except Exception as e:
            _pg_(e.message)
            raise e
        
        
        
# ~~ JobResults ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ #
class JobResults:

    def __init__(self, job):
        """
        This class stores results of video processing for a job and provides
        methods for converting to various output formats.
        
        Args:
            job (Job): job to summarize
            
        """
        assert job.video is not None, 'Job has not been run yet.'
        assert job.replays is not None, 'Job has not been run yet.'
        self.job = job
        self.data = []
        vidTime = self.job.video.get_earliest_time()
        latestTime = self.job.video.get_latest_time()
        while (vidTime <= latestTime):
        
            # get the game time in seconds at this video time
            clockLabel = self.job.video.query(OPTIONS.CD.KEY, vidTime)
            try: gameTimeSeconds = Time.clock_to_seconds(clockLabel)
            except ValueError as e: 
                vidTime += self.job.skip
                continue # if the detector didnt find a valid time, skip
            
            # pull every hero's state at this time
            focusHeroName = self.job.video.query(OPTIONS.ND.KEY, vidTime)
            if self.job.valid is None: gameHeroNames = [focusHeroName]
            else: gameHeroNames = self.job.valid[OPTIONS.ND.KEY].query_time(vidTime).data
            try: currentReplay = self.job.replays.query_time(vidTime).data
            except AttributeError: currentReplay = None
            heroStates = {}
            for heroName in gameHeroNames:
                heroNPCName = self.job.keys.get(heroName.lower(), None)
                if currentReplay is None: state = None
                else:
                
                    # if the replay doesnt know the unit (e.g. if the game time is incorrect), 
                    # just fill in with blank (handled below)
                    try: state = currentReplay.query(heroNPCName, gameTimeSeconds)
                    except KeyError: state = None
                    
                # do the same if the replay has no info on the unit (e.g. some
                # npcs like minions). For abilities (since we know some of them
                # as long as we know the hero), fill in from the known keys
                if state is None:
                    allAbilities = [self.job.keys.get(k, None) for k in self.job.keys['ability_order'].get(heroName.lower(), [])]
                    abilities = [[a, 0] for a in allAbilities[:6] if a <> EMPTY_ABILITY] # core abilities up to 6
                    abilities += allAbilities[-8:] # talents
                    items = []
                    
                # otherwise, fill in with the known keys
                else:
                    abilities = [[self.job.keys.get(k[0], None), k[1]] for k in state.data['abilities']]
                    items = [self.job.keys.get(k, None) for k in state.data['items']]
                    
                # remove all the None abilities after the last non-None
                for i in xrange(len(abilities)-1, -1, -1):
                    if abilities[i][0] is not None: break
                    
                abilities = abilities[:i+1]
                
                # same with items
                for i in xrange(len(items)-1, -1, -1):
                    if items[i] is not None: break
                    
                items = items[:i+1]
                
                # add to the output
                heroStates[heroName] = { 'abilities': abilities, 'items': items }
                heroStates[heroName].update(dict((k, state.data[k]) for k in state.data if k not in ('abilities', 'items')))
                
            # add results at this time to the output
            self.data.append({
                'heroes': heroStates, 'time': vidTime, 'focus': focusHeroName, 'gtime': gameTimeSeconds
            })
            vidTime += self.job.skip
            
            
    def __repr__(self):
        return '<JobResults of %s>' % str(self.job)
        
        
    def get_data(self): return self.data
    
    
    def as_list(self): return list(self.data)
    
    
    @staticmethod
    def cast(part, precision=None):
        if type(part) is dict: return dict((JobResults.cast(k), JobResults.cast(part[k])) for k in part)
        elif type(part) is list: return [JobResults.cast(v) for v in part]
        elif type(part) is set: return set([JobResults.cast(v) for v in part])
        else:
            if precision is None: precision = OPTIONS.JB.FLOAT_PRECISION
            fmt = '%%.%if' % precision
            try: vFloat = float(part)
            except (ValueError, TypeError): return part
            try: vInt = int(part)
            except (ValueError, TypeError): vInt = None
            if vInt == vFloat: return '%i' % vInt
            else: return fmt % vFloat
                
                
    def as_json_string(self, precision=None):
        import json
        return json.dumps(JobResults.cast(self.data, precision))
    
    
    def as_gzipped_json_string(self, precision=None):
        from gzip import GzipFile
        from cStringIO import StringIO
        file = StringIO()
        zipper = GzipFile(fileobj=file, mode='wb')
        zipper.write(self.as_json_string(precision))
        zipper.close()
        return file.getvalue()
        
        
    
# ########################################################################### #
# ## HELPER FUNCTIONS ####################################################### #
# ########################################################################### #
    
# ~~ download_youtube_video() ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ # 
def download_youtube_video(url, outfile=None, quality=None):
    """
    Downloads the video with the quality nearest to the user's specification.
    
    If there is no video available that is at least as good as the quality
    requested, attempts to download the best available quality.
    
    Args:
        url (str): Youtube URL of video page to check
        
        outfile (str): (optional) local path to save the file. Default is to
            save to a random new file in the executing directory.
            
        quality (int): (optional) resolution height to search for. Default
            is job.quality_default
        
    Returns:
        str: path to the output video file
    """
    import shutil
    from random import random
    from uuid import uuid4
    
    if quality is None: quality = OPTIONS.JB.QUALITY
    
    if not os.path.exists(OPTIONS.JB.SCRATCH): os.makedirs(OPTIONS.JB.SCRATCH)
    tf = os.path.join(OPTIONS.JB.SCRATCH, str(uuid4()))
    def hook(progress):
        if progress['status'] == 'finished':
            open(tf, 'w').write(progress['filename'])
    
    try:
        # download the requested video
        tempOutTemplate = unicode(os.path.join(OPTIONS.JB.SCRATCH, str(int(random()*1e8)) + '_%(id)s'))
        opts = {
            'keepvideo': True, 'format': 'worstvideo[height>=%i]/worst[height>=%i]/bestvideo/best' % (quality, quality),
            'progress_hooks': [hook], 'quiet': True, 'outtmpl': tempOutTemplate,
            'youtube_include_dash_manifest': True, 'restrictfilenames': True,
            
        }
        # Y = youtube_dl.YoutubeDL({})
        Y = youtube_dl.YoutubeDL(opts)
        v = Y.extract_info(url, download=True)
        
        # save to final location
        temp = open(tf, 'r').read()
        if outfile is None: outfile = os.path.abspath(os.path.join(OPTIONS.JB.SCRATCH, str(uuid4())))
        if not os.path.exists(os.path.dirname(outfile)): os.makedirs(os.path.dirname(outfile))
        if os.path.exists(outfile):
            try: os.remove(outfile)
            except:
                os.remove(temp)
                print 'Unable to overwrite existing file: %s' % outfile
                raise
            
        shutil.copy2(temp, outfile)
        os.remove(temp)
        
    finally:
        if os.path.exists(tf):
            try: os.remove(tf)
            except: print 'Could not delete temporary file %s' % tf
        
    return outfile
    
    
# ~~ download_file() ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ # 
def download_file(url, outfile=None):
    """
    Downloads a file at a URL.
    
    Args:
        url (str): URL of the file to download
        outfile (str): (optional) local path to save the file. Default is to
            save to a random new file in the scratch directory.
            
    Returns:
        str: path to the output file (same as outfile)
    """
    import urllib2, urlparse
    from uuid import uuid4
    paths = []
    request = urllib2.Request(url)
    connection = urllib2.urlopen(request)
    data = connection.read()
    basename = os.path.basename(urlparse.urlparse(url).path)
    if outfile is None: outfile = os.path.abspath(os.path.join(OPTIONS.JB.SCRATCH, str(uuid4())))
    if not os.path.exists(os.path.dirname(outfile)): os.makedirs(os.path.dirname(outfile))
    with open(outfile, 'wb') as fh: fh.write(data)
    return outfile
    
    
# ~~ download_replay() ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ # 
def download_replay(matchID, outfile=None):
    """
    Downloads a replay by the match ID.
    
    Args:
        matchID (str): match ID of the match of which to get a replay
        outfile (str): (optional) local path to save the file. Default is to
            save to a random new file in the scratch directory.
            
    Returns:
        str: path to the output file (same as outfile)
    """
    import urllib2, json, threading
    from google.cloud import storage
    from uuid import uuid4
    
    # update user preferences
    if outfile is None: outfile = os.path.abspath(os.path.join(OPTIONS.JB.SCRATCH, str(uuid4())))
    if not os.path.exists(os.path.dirname(outfile)): os.makedirs(os.path.dirname(outfile))
    
    # first see if the replay is available on the VGV database and download
    #   download from there if so
    # if this doesnt work, see https://cloud.google.com/docs/authentication/getting-started
    gcpClient = storage.Client.from_service_account_json(OPTIONS.JB.CRED)
    gcpBucket = gcpClient.get_bucket(OPTIONS.JB.REPLAY_BUCKET)
    bucketMatches = [b for b in gcpBucket.list_blobs(prefix=str(matchID))]
    if len(bucketMatches) > 0:
        blob = bucketMatches[0]
        blob.download_to_filename(outfile)
    
    # otherwise request the info necessary to construct the replay download url
    #   so we can try to download from the dota servers
    else:
        details = json.loads(urllib2.urlopen(REPLAY_GET_FMT % str(matchID)).read())[0]
        if (type(details) is str): 
            raise ValueError('Could not find the replay for match %s. It may be an incorrect match ID or the replay may no longer exist.' % matchID)
        
        salt = None
        match = None
        cluster = None
        series = None
        seriesType = None
        try:
            salt = details[REPLAY_KEY_SALT]
            match = details[REPLAY_KEY_MATCH]
            cluster = details[REPLAY_KEY_CLUSTER]
            series = details[REPLAY_KEY_SERIES]
            seriesType = details[REPLAY_KEY_TYPE]
        
        except: raise RuntimeError('Unable to construct the replay download URL for %s because some details are missing from the detail request.' % matchID)
        
        # download the replay
        url = REPLAY_URL_CONSTRUCTOR(cluster, match, salt)
        download_file(url, outfile)
        
        # upload the replay to the database
        if OPTIONS.JB.UPLOAD_NEW_REPLAYS:
            upload = lambda: gcpBucket.blob(str(matchID)).upload_from_filename(outfile)
            thread = threading.Thread(target=upload)
            thread.start()
            
    return outfile
            
    
    
    
if __name__ == '__main__':
    replay = [[0.0, r'D:\Dropbox\youtube_game_hud\server\test\replay_r01.dem']]
    quality = 480
    skip = 1.0 # seconds
    # videoURL = r'https://youtu.be/gYwr7SK72PY'
    # video = download_youtube_video(videoURL, quality=quality)
    video = r'D:\Dropbox\youtube_game_hud\server\test\video_480p_r01.avi'
    job = Job(video, replay, skip=skip)
    job.run()