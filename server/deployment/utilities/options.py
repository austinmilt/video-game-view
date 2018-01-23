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


# Python equivalent of the java version

import os
    
        
# #############################################################################
# DEFINE CUSTOM OPTIONS HERE ##################################################
# #############################################################################

def print_options(instance):
    print '\n'.join(['%s: %s' % (k, str(instance.__dict__[k])) for k in instance.__dict__])
        

class GlobalOptions:
    NAME = 'global'
    def print_options(self): return print_options(self)
    def __init__(self, dataMap, globalOptions):
        self.parser = ValueParsers(GlobalOptions.NAME, dataMap)
        map = {
            'package_root': ['ROOT', self.parser.get_file]
        }
        self.map = {}
        for k in map:
            attribute, method = map[k]
            self.__dict__[attribute] = method(k)
            self.map[self.explicit(k)] = self.__dict__[attribute]
            
    def explicit(self, option): return self.parser.getter.explicit(option)
    
    def substitute_all(self, tosub):
        for k in self.map:
            tosub = tosub.replace('%%%s%%' % k, self.map[k])
        
        return tosub
    
        
class ClockTrainerOptions:
    NAME = 'clock_trainer'
    def print_options(self): return print_options(self)
    def __init__(self, dataMap, globalOptions):
        m = ValueParsers(ClockTrainerOptions.NAME, dataMap, globalOptions)
        self.TRAINING_DIR = m.get_file('training_dir')
        self.SEARCH = m.get_string('search_str')
        self.THRESHOLD = m.get_float('bw_threshold')
        self.GLYPHS = m.get_int('glyphs')
        self.WIDTH_001 = m.get_int('segwidth_001')
        self.WIDTH_010 = m.get_int('segwidth_010')
        self.WIDTH_100 = m.get_int('segwidth_100')
        self.SEGMENT_001 = m.get_list_range_int('positions_001')
        self.SEGMENT_010 = m.get_list_range_int('positions_010')
        self.SEGMENT_100 = m.get_list_range_int('positions_100')
        self.LAYER_SIZE = m.get_int('layer_size')
        self.LEARNING_RATE = m.get_float('learning_rate')
        self.DECAY_RATE = m.get_float('decay_rate')
        self.EPOCHS = m.get_int('epochs')
        self.BATCH_SIZE = m.get_int('batch_size')
        self.LABEL_SIZE = m.get_int('label_size')
        self.PROP_TRAINING = m.get_float('training_proportion')
        self.PROP_VALIDATION = m.get_float('validation_proportion')
        self.DROPOUT_RATE = m.get_float('dropout_rate')
        self.SEPARATOR = m.get_string('time_separator')
        self.IMAGE_FORMAT = m.get_string('image_format')


        
class ClockDetectorOptions:
    NAME = 'clock_detector'
    def print_options(self): return print_options(self)
    def __init__(self, dataMap, globalOptions):
        m = ValueParsers(ClockDetectorOptions.NAME, dataMap, globalOptions)
        self.ROI = m.get_list_double('frame_region')
        self.NETWORK = m.get_file('serialized_network')
        self.KEY = m.get_string('key')
    
    

class NameTrainerOptions:
    NAME = 'name_trainer'
    def print_options(self): return print_options(self)
    def __init__(self, dataMap, globalOptions):
        m = ValueParsers(NameTrainerOptions.NAME, dataMap, globalOptions)
        self.TRAINING_DIR = m.get_file('training_dir')
        self.SEARCH = m.get_string('search_str')
        self.BINARY_THRESHOLD = m.get_float('binary_threshold')

        
        
class NameDetectorOptions:
    NAME = 'name_detector'
    def print_options(self): return print_options(self)
    def __init__(self, dataMap, globalOptions):
        m = ValueParsers(NameDetectorOptions.NAME, dataMap, globalOptions)
        self.ROI = m.get_list_double('frame_region')
        self.NETWORK = m.get_file('serialized_network')
        self.KEY = m.get_string('key')
        
        
        
class VideoParserOptions:
    NAME = 'video_parser'
    def print_options(self): return print_options(self)
    def __init__(self, dataMap, globalOptions):
        m = ValueParsers(VideoParserOptions.NAME, dataMap, globalOptions)
        self.CSV_SEP_COL = m.get_string('csv_separator_column')
        self.CSV_SEP_INTERVAL = m.get_string('csv_separator_interval')
        self.CSV_SEP_LABEL = m.get_string('csv_separator_label')
        self.CSV_COL_FRAME = m.get_string('csv_column_frame')
        self.CSV_COL_TIME = m.get_string('csv_column_time')
        self.CSV_COL_INTERVAL = m.get_string('csv_column_interval')
        self.CSV_COL_LABEL = m.get_string('csv_column_label')
        self.CSV_COL_DETECTOR = m.get_string('csv_column_detector')
        self.PARSE_SKIP_DEFAULT = m.get_float('parse_skip_default')
        self.DETECTORS = m.get_list_string('detectors')
        
        
class JobOptions:
    NAME = 'job'
    def print_options(self): return print_options(self)
    def __init__(self, dataMap, globalOptions):
        m = ValueParsers(JobOptions.NAME, dataMap, globalOptions)
        self.KEYS = m.get_file('keymap_default')
        self.JAR_DIR = m.get_file('jar_dir')
        self.QUALITY = m.get_float('quality_default')
        self.SCRATCH = m.get_file('scratch_directory')
        self.CRED = m.get_file('gcp_credentials')
        self.REPLAY_BUCKET = m.get_string('gcp_replay_bucket')
        self.UPLOAD_NEW_REPLAYS = m.get_boolean('gcp_upload_new_replays')
        
        
class ServerOptions:
    NAME = 'server'
    def print_options(self): return print_options(self)
    def __init__(self, dataMap, globalOptions):
        m = ValueParsers(ServerOptions.NAME, dataMap, globalOptions)
        self.PORTS = m.get_list_int('ports')
        self.TESTING = m.get_boolean('testing')
        self.SOCKET_LIMIT = m.get_int('socket_request_limit')
        self.WORKERS = m.get_int('workers')
        self.SOCKET_TIMEOUT = m.get_float('session_death_timeout')
  
  
# add to this when you need a new set of options
OPTION_CLASSES = [
    ['GL', GlobalOptions],
    ['CT', ClockTrainerOptions],
    ['CD', ClockDetectorOptions],
    ['NT', NameTrainerOptions],
    ['ND', NameDetectorOptions],
    ['VP', VideoParserOptions],
    ['JB', JobOptions],
    ['SV', ServerOptions]
]



# #############################################################################
# HELPERS FOR DEFINING CUSTOM OPTIONS #########################################
# ##############################################################################

class ValueParsers:

    VALUE_SEPARATOR = ','
    RANGE_SEPARATOR = '-'
    
    def __init__(self, unitKey, dataMap, globalOptions=None):
        self.getter = ValueGetter(dataMap, unitKey)
        self.glob = globalOptions
    
    def get(self, string):
        value = self.getter.get(string)
        if self.glob is None: return value
        else: return self.glob.substitute_all(value)

    def get_file(self, string):
        return os.path.abspath(self.get(string))

    def get_string(self, string): return self.get(string)
    
    def get_float(self, string): return float(self.get(string))
    
    def get_int(self, string): return int(self.get(string))
    
    def get_boolean(self, string):
        flag = True
        string = self.get(string).lower()
        if (string == 'false'):  flag = False
        elif (string == 'true'): flag = True
        else:
            val = float(string)
            if val == 0: flag = False
            else: flag = True
            
        return flag
    
    def split_values(self, string): return [s.strip() for s in string.split(ValueParsers.VALUE_SEPARATOR)]
    
    def split_range(self, string): return [s.strip() for s in string.split(ValueParsers.RANGE_SEPARATOR, 1)]
    
    def get_list_double(self, string): return [float(s) for s in self.split_values(self.get(string))]
    
    def get_list_int(self, string): return [int(s) for s in self.split_values(self.get(string))]
    
    def get_list_string(self, string): return self.split_values(self.get(string))
 
    def get_list_range_int(self, string):
        return [[int(r) for r in self.split_range(s)] for s in self.split_values(self.get(string))]
 
 

class ValueGetter:
    SEPARATOR = '.'
    DEFAULT_NAME = ''
    def __init__(self, dataMap, unitKey=None):
        if (unitKey is None): unitKey = ValueGetter.DEFAULT_NAME
        self.key = unitKey
        self.map = dataMap
        
    def explicit(self, *minorKeys): return ValueGetter.SEPARATOR.join([self.key] + list(minorKeys))
    def get(self, *minorKeys):
        return self.map[self.explicit(*minorKeys)]
    
    
# #############################################################################
# CLASSES FOR READING AND CONTAINING OPTIONS ##################################
# #############################################################################
class OptionFileReader:

    COMMENT = '#'
    VALUE_DEFINITION = '='
    DEFAULT_OPTIONS_NAME = 'settings.config'
    DEFAULT_OPTIONS = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', DEFAULT_OPTIONS_NAME))
    
    def __init__(self, optionFile=None, read=True):
        self.file = optionFile
        if (self.file is None): self.file = OptionFileReader.DEFAULT_OPTIONS
        self.options = {}
        if (read):  self.read()
        
        
    @staticmethod
    def is_nonsense(string):
        string = string.strip()
        if (max(string.find(OptionFileReader.COMMENT), string.find(OptionFileReader.VALUE_DEFINITION)) == -1):
            return True
        
        return False
        
        
    @staticmethod
    def is_comment(string): return string.strip().startswith(OptionFileReader.COMMENT)
    
    
    @staticmethod
    def split_definition(string):
        key, value = string.split(OptionFileReader.VALUE_DEFINITION, 1)
        return (key.strip(), value.strip())
    
            
    def read(self):
        assert os.path.exists(self.file), 'Could not locate options file.'
        fH = open(self.file, 'r')
        for line in fH:
            sline = line.strip()
            if (OptionFileReader.is_nonsense(sline)): continue
            if (OptionFileReader.is_comment(sline)): continue
            key, value = OptionFileReader.split_definition(sline)
            self.options[key] = value
                       
            

class Options:

    def __init__(self, dataMap=None):
        if dataMap is None:
            dataMap = OptionFileReader().options
            
        globalOptions = GlobalOptions(dataMap, None)
        for unitName, unitClass in OPTION_CLASSES:
            self.__dict__[unitName] = unitClass(dataMap, globalOptions)
    
    
    @staticmethod
    def from_file(optionsFile=None):
        optionReader = OptionFileReader(optionsFile)
        return Options(optionReader.options)