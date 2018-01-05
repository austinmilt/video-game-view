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

"""Main module for detecing game information from video."""

# ########################################################################### #
# ## IMPORTS ################################################################ #
# ########################################################################### #
import os, sys
here = os.path.dirname(os.path.abspath(__file__))
sys.path = [os.path.join(here, '..', 'utilities')] + sys.path
from options import Options
from replays import Replay
from intervals import Interval, LinkedIntervalSet
del here



# ########################################################################### #
# ## CONSTANTS ############################################################## #
# ########################################################################### #
OPTIONS = Options()

# Video
REP_DEM_JAR = os.path.join(OPTIONS.JB.JAR_DIR, 'video_parser.jar')
REP_DEM_JARBASE = [r'java', '-jar', REP_DEM_JAR, '-t', 'parse']
REP_DEM_JARARGS = { 'video': '-v', 'output': '-o', 'skip': '-s', 'input': '-f' }



# ########################################################################### #
# ## CLASSES ################################################################ #
# ########################################################################### #
        
        

# ~~ Video ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ #
class Video:

    def __init__(self, data={}):
        self._update_(data)
        
        
    def __repr__(self):
        return '<Video@%s>' % str(id(self))
        
    
    def _update_(self, data):
        self.times = {}
        for detectorKey in data:
            self.times[detectorKey] = LinkedIntervalSet([])
            sortedTimes = sorted(data[detectorKey], key=lambda t: t[0])
            prevLabel = None
            start = 0.0
            for time, label in sortedTimes:
            
                # if we've arrived at a change in the label, add the current
                # label to the list and start recording a new label duration
                if (prevLabel is not None) and (prevLabel <> label):
                    self.times[detectorKey].add(Interval(prevLabel, start, time))
                    start = time
                    
                prevLabel = label
                
            # add the last label
            self.times[detectorKey].add(Interval(label, start, time))
            
            
    def get_earliest_time(self, detector=None):
        """
        Returns the earliest video time for the given detector.
        
        Args:
            detector (str): (optional) detector key. If not given, looks across
                all detectors.
                
        Returns:
            float: earliest video time of detector
        """
        if detector is None: detectors = self.times.keys()
        else: detectors = (detector, )
        return min([self.times[k].earliest.start for k in detectors])
        
        
    def get_latest_time(self, detector=None):
        """
        Returns the latest video time for the given detector.
        
        Args:
            detector (str): (optional) detector key. If not given, looks across
                all detectors.
                
        Returns:
            float: latest video time of detector
        """
        if detector is None: detectors = self.times.keys()
        else: detectors = (detector, )
        return max([self.times[k].latest.end for k in detectors])
        
        
    def query(self, detector, time):
        """
        Queries the loaded video data for detection results at the requested time.
        
        Args:
            detector (str): detector to query for label
            time (float): video time (seconds) to query for label
            
        Returns:
            str: detector's label at that time
            
        """
        label = None
        return self.times[detector].query_time(time).data
        
        
    def query_all(self, time):
        """
        Queries all detectors at the given time for detection results.
        
        Args:
            time (float): video time (seconds) to query for label
            
        Returns:
            dict: dictionary where keys are detector keys and values are
                the label at the requested time
        """
        return dict((k, self.query(k, time)) for k in self.times)
            
            
    @staticmethod
    def load_csv(file):
        """
        Loads info from a csv into a format that can be used directly to create
        a new Video.
        
        Args:
            file (str): path to the csv file on disk, of the same format as
                output by the video processor (e.g. video_parser.jar).
            
        Returns:
            dict: dictionary of the form
                {
                    detector_key_1: [[video_time_1_1, label_1_1], [video_time_time_1_2, label_1_2], ...],
                    detector_key_2: [[video_time_2_1, label_2_1], [video_time_time_2_2, label_2_2], ...],
                    ...
                }
        """
        import csv
        reader = csv.reader(open(file, 'r'))
        columns = reader.next()
        c2i = dict((columns[i], i) for i in range(len(columns)))
        knownCols = set([OPTIONS.VP.CSV_COL_TIME, OPTIONS.VP.CSV_COL_FRAME])
        data = dict((c, []) for c in columns if c not in knownCols)
        for row in reader:
            time = float(row[c2i[OPTIONS.VP.CSV_COL_TIME]])
            for detectorKey in data:
                data[detectorKey].append([time, row[c2i[detectorKey]]])
            
        return data
        
        
    @staticmethod
    def from_csv(file):
        """
        Creates a new Video from a csv that has been processed from a video file.
        
        Args:
            file (str): path to the csv file on disk
            
        Returns:
            Video: a new Video loaded from the csv
        """
        return Video(Video.load_csv(file))
        
        
    @staticmethod
    def from_video(video, validLabelsFile=None, skip=OPTIONS.VP.PARSE_SKIP_DEFAULT):
        """
        Creates a new Video from a video file using the java video processor.
        
        Args:
            video (str): path to the video file on disk
            
            validLabelsFile (str): (optional) path to file containing valid
                detection results for detectors at various times. File should
                be formatted as output by Video.write_valid_labels()
                
            skip (float): seconds between queries of the video. Default is
                video_parser.parse_skip_default
            
        Returns:
            Video: a new Video after processing the video file
        """
        import subprocess, os
        from uuid import uuid4
        
        # build the video processor call
        jarCall = list(REP_DEM_JARBASE)
        jarCall.extend([REP_DEM_JARARGS['video'], video])
        jarCall.extend([REP_DEM_JARARGS['skip'], str(skip)])
        if validLabelsFile is not None:
            jarCall.extend([REP_DEM_JARARGS['input'], validLabelsFile])
            
        if not os.path.exists(OPTIONS.JB.SCRATCH): os.makedirs(OPTIONS.JB.SCRATCH)
        output = os.path.join(OPTIONS.JB.SCRATCH, str(uuid4()))
        jarCall.extend([REP_DEM_JARARGS['output'], output])
        
        # call the processor and return the output
        cdir = os.path.abspath(os.curdir)
        try:
            os.chdir(OPTIONS.JB.JAR_DIR)
            result = subprocess.call(jarCall)
            if not os.path.exists(output):
                raise RuntimeError('Video processor failed to execute properly.')
                
            return Video.from_csv(output)
            
        finally:
            os.chdir(cdir)
            if os.path.exists(output):
                try: os.remove(output)
                except: print 'Unable to delete temporary file %s' % output
                
                
    @staticmethod
    def write_valid_labels(validLabels, outfile=None):
        """
        Writes a file that can be read by the java video parser that limits
        detection results for detectors based on replay data.
        
        Args:
            validLabels (dict): dictionary where keys are detector keys as
                defined in the video parser (e.g. video_parser.jar) and
                values are LinkedIntervalSet where interval data are
                lists of valid labels in that interval, and interval times
                are video times in seconds
                
            outfile (str): (optional) path to save output file. If not given
                will be saved to random file name in the calling path.
                
        Returns:
            str: [outfile]
            
        """
        import csv
        if outfile is None: outfile = os.path.join(OPTIONS.JB.SCRATCH, str(uuid4()))
        if not os.path.exists(os.path.dirname(outfile)): os.makedirs(os.path.dirname(outfile))
        interval2range = lambda interval: OPTIONS.VP.CSV_SEP_INTERVAL.join([str(interval.start), str(interval.end)])
        interval2labels = lambda interval: OPTIONS.VP.CSV_SEP_LABEL.join(interval.data)
        detectorKeys = sorted(validLabels.keys())
        header = [OPTIONS.VP.CSV_COL_INTERVAL, OPTIONS.VP.CSV_COL_DETECTOR, OPTIONS.VP.CSV_COL_LABEL]
        writer = csv.writer(open(outfile, 'wb'))
        writer.writerow(header)
        for detectorKey in detectorKeys:
            for interval in validLabels[detectorKey]:
                row = [interval2range(interval), detectorKey, interval2labels(interval)]
                writer.writerow(row)
        
        del writer
        return outfile
        
        

# processing command-line requests for videos        
if __name__ == '__main__': pass

    # # test:
    # #   python detection.py https://www.youtube.com/watch?v=JwKx_-77uNY https://www.dropbox.com/s/p0tkek23occkj47/replay.dem?dl=1

    # from time import time
    # from uuid import uuid4
    # import sys
    
    # urlVid = sys.argv[1]
    # urlRep = sys.argv[2]
    
    # start = time()
    # vid = download_video(urlVid, str(uuid4())+'.mp4')
    # print 'took %0.2f sec to download video' % (time() - start)
    
    # start = time()
    # replay = download_file([[0.0, urlRep]], str(uuid4())+'.dem')
    # print 'took %0.2f sec to download replay' % (time() - start)
    
    # start = time()
    # times = process_video(vid, replay)
    # print 'took %0.2f sec to process video and replay' % (time() - start)
    
    # os.remove(vid)
    # os.remove(replay)
    
    # import csv
    # writer = csv.writer(open('test.csv', 'wb'))
    # v2s = {None: ''}
    # for state in times:
        # line = [
            # state.get('time', ''), state.get('hero', ''), 
            # ';'.join([v2s.get(v, v) for v in state.get('abilities', [])]), 
            # ';'.join([v2s.get(v, v) for v in state.get('items', [])])
        # ]
        # writer.writerow(line)
    # del writer
    