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
        
        # determin if it's a negative time
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
        

        
# ~~ Job ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ #
class Job:
    
    def __init__(self, videoFile, replayFiles, keyFile=None, skip=None):
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
        
        # add valid hero labels
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
    
        import cPickle
        from uuid import uuid4
        
        # try loading the mapping from npc_dota name to regular name
        if os.path.exists(self._k):
            self.keys = cPickle.load(open(self._k, 'rb'))
            
        # load the replays and use them to determine valid labels for video
        # parsing
        vidLabelFile = None
        if len(self._r) > 0:
            self.replays = Replays.from_dems(self._r)
            self.valid = Job.build_valid_labels(self.replays, self.keys)
            if not os.path.exists(OPTIONS.JB.SCRATCH): os.makedirs(OPTIONS.JB.SCRATCH)
            vidLabelFile = os.path.abspath(os.path.join(OPTIONS.JB.SCRATCH, str(uuid4())))
            _ = Video.write_valid_labels(self.valid, vidLabelFile)
        
        # process the video
        try:
            self.video = Video.from_video(self._v, vidLabelFile, self.skip)
            
        finally:
            try: os.remove(vidLabelFile)
            except: print 'Could not remove temporary file %s' % vidLabelFile
            
        
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
    def from_urls(videoURL, replayURLs=None, keyFile=None, skip=None, quality=None, verbose=False):
        """
        Constructs a new Job by downloading files instead of directly from 
        files on disk.
        
        Args:
            videoURL (str): valid youtube url to the video to download
            
            replayURLs (list): @see <code>Job.__init__</code>, but replace 
                file paths to url paths
                
            keyFile (str): @see <code>Job.__init__</code>
            
            skip (float): @see <code>Job.__init__</code>
            
            quality (float): @see <code>download_youtube_video</code>
            
            verbose (boolean): whether or not to be verbose with output
            
        Returns:
            Job: job constructed from downloadedurls instead of files
        """
        if replayURLs is None: replayURLs = []
        if quality is None: quality = OPTIONS.JB.QUALITY
        
        if verbose:
            sys.stdout.write('\nDownloading video %s with requested quality %0.0f' % (videoURL, quality))
            sys.stdout.flush()
            
        videoFile = download_youtube_video(videoURL, quality=quality)
        temp = []
        temp.append(videoFile)
        replayFiles = []
        for time, replayURL in replayURLs:
        
            if verbose:
                sys.stdout.write('\nDownloading replay %s' % replayURL)
                sys.stdout.flush()
                
            replayFile = download_file(replayURL)
            temp.append(replayFile)
            replayFiles.append([time, replayFile])
        
        if verbose:
            sys.stdout.write('\nBuilding Job.')
            sys.stdout.flush()
            
        return Job(videoFile, replayFiles, keyFile, skip)
        
        
        
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
            gameTimeSeconds = Time.clock_to_seconds(clockLabel)
            
            # pull every hero's state at this time
            focusHeroName = self.job.video.query(OPTIONS.ND.KEY, vidTime)
            gameHeroNames = self.job.valid[OPTIONS.ND.KEY].query_time(vidTime).data
            currentReplay = self.job.replays.query_time(vidTime).data
            heroStates = {}
            for heroName in gameHeroNames:
                heroNPCName = self.job.keys[heroName.lower()]
                state = currentReplay.query(heroNPCName, gameTimeSeconds)
                abilities = [self.job.keys.get(k, None) for k in state.data['abilities']]
                items = [self.job.keys.get(k, None) for k in state.data['items']]
                heroStates[heroName] = { 'abilities': abilities, 'items': items }
                
            # add results at this time to the output
            self.data.append({
                'heroes': heroStates, 'time': vidTime, 'focus': focusHeroName
            })
            vidTime += self.job.skip
            
            
    def __repr__(self):
        return '<JobResults of %s>' % str(self.job)
        
        
    def get_data(self): return self.data
    
    
    def as_list(self): return list(self.data)
    
    
    def as_json_string(self):
        import json
        return json.dumps(self.data)

        
    
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
    
    
# ~~ download_replay() ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ # 
def download_file(url, outfile=None):
    """
    Downloads a file at a URL.
    
    Args:
        url (str): URL of the file to download
        outfile (str): (optional) local path to save the file. Default is to
            save to a random new file in the executing directory.
            
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
    
    
if __name__ == '__main__':
    replay = [[0.0, r'D:\Dropbox\youtube_game_hud\server\test\replay_r01.dem']]
    quality = 480
    skip = 1.0 # seconds
    # videoURL = r'https://youtu.be/gYwr7SK72PY'
    # video = download_youtube_video(videoURL, quality=quality)
    video = r'D:\Dropbox\youtube_game_hud\server\test\video_480p_r01.avi'
    job = Job(video, replay, skip=skip)
    job.run()