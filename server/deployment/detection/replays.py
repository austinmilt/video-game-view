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

#   contains classes and functions for processing replays to feed into detection
# algorithms

# ########################################################################### #
# ## GLOBAL IMPORTS ######################################################### #
# ########################################################################### #
import os, sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, '..'))
sys.path = [ROOT] + sys.path

from utilities.options import Options
from intervals import LinkedIntervalSet, Interval


# ########################################################################### #
# ## CONSTANTS ############################################################## #
# ########################################################################### #
OPTIONS = Options.from_file()

# Time
TIM_KWD_NEG = '-'

# Hero
HER_KWD_ABL = 'abilities'
HER_KWD_ITM = 'items'

# Replay
REP_CSV_HED_ABL = 'abilities'
REP_CSV_HED_HER = 'hero'
REP_CSV_HED_ITM = 'items'
REP_CSV_HED_TIM = 'time'
REP_CSV_DEL_ARR = ';'
REP_DEM_JAR = os.path.join(OPTIONS.JB.JAR_DIR, 'process_replay.jar')
REP_DEM_JARBASE = [r'java', '-jar', REP_DEM_JAR, None, None]



# ########################################################################### #
# ## CLASSES ################################################################ #
# ########################################################################### #
        
# ~~ Hero ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ #
class Hero:
    
    def __init__(self, name='', data=[]):
        """
        Heroes are objects for summarizing and doing operations on hero state
        data in a dota 2 match replay.
        
        Args:
            name (str): (optional) name of hero. Default is empty.
            
            data (list): (optional) hero state data from processed replay, as
                in a hero's dictionary entry returned from Replay.load_csv().
                
        Returns:
            Hero: an instatiated hero object
        """
        self.name = name
        self._update_(data)
        
        
    def __repr__(self):
        return '<Hero %s>' % self.name
        
        
    def _update_(self, data):
        """
        Updates self with new data. Should not be run directly. Instead create
        a new Hero.
        """
        # imports
        from intervals import Interval, LinkedIntervalSet
        
        # create a LinkedIntervalSet to store the time-based states for this
        #   hero
        states = [Interval({HER_KWD_ABL: d[1], HER_KWD_ITM: d[2]}, d[0]) for d in data]
        self.states = LinkedIntervalSet.from_starts(states)
        
        
    def query(self, time):
        """
        Runs self.states.query_time(time)
        """
        return self.states.query_time(time)



# ~~ Replay ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ #
class Replay:
    
    def __init__(self, data={}):
        """
        Replays are objects for summarizing and doing operations on the outputs
        of a replay analyzer.
        
        Args:
            data (dict): (optional) replay data, as returned from
                Replay.load_csv(). By default, no data are processed into the
                object.
                
        Returns:
            Replay: with data processing performed (if any were supplied).
        """
        self._update_(data)
        
        
    def __repr__(self):
        return '<Replay with %i heroes>' % len(self.heroes)
        
        
    def _update_(self, data):
        """
        Updates the Replay with new data. Should not be run directly. Create a
        new Replay instead.
        """
        self.heroes = dict((hero, Hero(hero, data[hero])) for hero in data)
        
        
    def query(self, hero, time):
        """
        Gets the state of the specified hero at the given match time.
        
        Args:
            hero (str): name of the hero as stored in self.heroes dict
            time (float): game clock time as stored in heroes' state data
            
        Returns:
            Interval: hero state data at the queried time
        """
        return self.heroes[hero].query(time)
        
        
    @staticmethod
    def load_csv(file):
        """
        Loads info from a csv into a format that can be used directly to create
        a new Replay.
        
        Args:
            file (str): path to the csv file on disk, of the same format as
                output by the replay .dem processor (e.g. process_replay.jar).
            
        Returns:
            dict: dictionary, where each key corresponds to a hero and the
                values are lists of [<match time (s) of update>, <ability list>, <item list>]
        """
        import csv
        reader = csv.reader(open(file, 'r'))
        columns = reader.next()
        c2i = dict((columns[i], i) for i in range(len(columns)))
        data = {}
        for row in reader:
        
            # get relevant info from the line
            time = float(row[c2i[REP_CSV_HED_TIM]])
            hero = row[c2i[REP_CSV_HED_HER]]
            abilities = row[c2i[REP_CSV_HED_ABL]].split(REP_CSV_DEL_ARR)
            items = row[c2i[REP_CSV_HED_ITM]].split(REP_CSV_DEL_ARR)
            
            # add to the data dictionary
            if hero not in data: data[hero] = []
            data[hero].append([time, abilities, items])
            
        return data
        
        
    @staticmethod
    def from_csv(file):
        """
        Creates a new Replay from a csv that has been processed from a dem.
        
        Args:
            file (str): path to the csv file on disk
            
        Returns:
            Replay: a new Replay loaded from the csv
        """
        return Replay(Replay.load_csv(file))
        
        
    @staticmethod
    def from_dem(file):
        """
        Creates a new Replay from a dota 2 dem using the replay processor.
        
        Args:
            file (str): path to the replay dem on disk
            
        Returns:
            Replay: a new Replay loaded from the csv
        """
        import subprocess, os
        from uuid import uuid4
        jarCall = list(REP_DEM_JARBASE)
        jarCall[-2] = file
        if not os.path.exists(OPTIONS.JB.SCRATCH): os.makedirs(OPTIONS.JB.SCRATCH)
        jarCall[-1] = os.path.abspath(os.path.join(OPTIONS.JB.SCRATCH, str(uuid4())))
        try:
            result = subprocess.call(jarCall)
            return Replay.from_csv(jarCall[-1])
            
        except Exception as e:
            print 'Invalid replay file. Unable to parse replay.'
            
        finally:
            if os.path.exists(jarCall[-1]):
                try: os.remove(jarCall[-1])
                except: print 'Unable to delete temporary file %s' % jarCall[-1]
                
                
                
class Replays(LinkedIntervalSet):


    @staticmethod
    def from_replays(replays):
        """
        Creates a new Replays from a list of Replay objects and associated times.
        
        Assumes that replays, when orderd by time, form the bounds of their
        neighbors.
        
        Args:
            replays: nested list of Replays and times, where each element is
                [start_time, Replay]
                
        Returns:
            Replays: new Replays with the ordered Replays
        """
        sortedReplays = sorted(replays, key=lambda r: r[0])
        intervals = [Interval(replays[i][1], replays[i][0], replays[i+1][0]) for i in xrange(len(replays)-1)]
        intervals.append(Interval(replays[-1][1], replays[-1][0]))
        return Replays(intervals)
        
        
    @staticmethod
    def from_dems(dems):
        """
        Creates a Replays from .dem replay files.
        
        Args:
            dems (list): nested list where each list element is 
                [start_time, path/to/.dem]
                
        Returns:
            Replays: a new Replays from the loaded .dems
        """
        return Replays.from_replays([[r[0], Replay.from_dem(r[1])] for r in sorted(dems, key=lambda x: x[0])])
        
        
        

if __name__ == '__main__':
    dem = r'D:\Dropbox\youtube_game_hud\test\3603235265.dem'
    replay = Replay.from_dem(dem)
    hero = replay.heroes.keys()[0]
    print '%s\'s items at 10 sec\n' % hero, replay.query(hero, 10).data['items']
