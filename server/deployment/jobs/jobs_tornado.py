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


import sys, os
from uuid import uuid4
here = os.path.dirname(os.path.abspath(__file__))
sys.path = [os.path.join(here, '..')] + sys.path
from utilities.options import Options
OPTIONS = Options()
JOB_CALLBASE = ['python', os.path.abspath(__file__)]
del here
DEL_ARGVAL = '='
ARG_VIDEO = 'video'
ARG_OUTFILE = 'outfile'
ARG_SKIP = 'skip'
ARG_REPLAYS = 'replays'
ARG_QUALITY = 'quality'



# #############################################################################
# ARGUMENT PARSERS/BUILDERS ###################################################
# #############################################################################

def parse_replay_pairs(arg, reverse=False):
    """Pass in string for forward, list [time, replay, time, replay...] for reverse."""
    if reverse: return' '.join([str(s) for s in arg])
    else:
        splitted = arg.strip().split()
        nsplit = len(splitted)
        if ((nsplit % 2) <> 0):
            raise RuntimeError('Invalid number of replay arguments.')
            
        replays = []
        for i in xrange(0, nsplit, 2):
            replays.append([float(splitted[i]), splitted[i+1]])
            
        return replays
    
    
def parse_float(arg, reverse=False):
    """Pass in string for forward, float for reverse."""
    if reverse: return '%f' % arg
    else: return float(arg)
    
    
def parse_str(arg, reverse=False):
    """Pass in string for forward, string for reverse."""
    if reverse: return '%s' % arg
    else: return str(arg)
    
    
def parse_int(arg, reverse=False):
    """Pass in string for forward, int for reverse."""
    if reverse: return '%i' % arg
    else: return int(arg)
    

KEY2PARSER = {
    ARG_VIDEO: parse_str, ARG_OUTFILE: parse_str, ARG_SKIP: parse_float, 
    ARG_REPLAYS: parse_replay_pairs, ARG_QUALITY: parse_int
}

    
def parse_arg(arg, value=None, reverse=False):
    if reverse: return DEL_ARGVAL.join([arg, KEY2PARSER[arg](value, reverse=reverse)])
    else:
        key, valueStr = arg.split(DEL_ARGVAL, 1)
        return (key, KEY2PARSER[key](valueStr))

    
def parse_args(args):
    return dict(parse_arg(arg) for arg in args)


    
# #############################################################################
# MAIN FUNCTIONS ##############################################################
# #############################################################################

def build_job_call(video, replays, skip=None, outfile=None, quality=None):
    """Builds a call that cal be invoked with Subprocess.call and will call the main() function here."""
    outfile = os.path.join(OPTIONS.JB.SCRATCH, str(uuid4()))
    toCall = list(JOB_CALLBASE)
    toCall.append(parse_arg(ARG_VIDEO, video, True))
    toCall.append(parse_arg(ARG_OUTFILE, outfile, True))
    if skip is not None: toCall.append(parse_arg(ARG_SKIP, skip, True))
    if quality is not None: toCall.append(parse_arg(ARG_QUALITY, quality, True))
    replaysFlat = []
    for r in replays: replaysFlat.extend(r)
    toCall.append(parse_arg(ARG_REPLAYS, replaysFlat, True))
    return toCall, outfile


def main(**args):
    """Runs the job and writes results. Args are as retruned by parse_args()."""
    
    # imports
    from jobs.jobs import Job
    
    # check arguments
    if 'video' not in args: raise RuntimeError('Missing required argument: video')
    if 'outfile' not in args: raise RuntimeError('Missing required argument: outfile')
    video = args['video']
    outfile = args['outfile']
    replays = args.get('replays', None)
    skip = args.get('skip', None)
    
    # run the job
    job = Job.from_urls(video, replayURLs=replays, skip=skip, quality=None, verbose=True)
    job.run()
    job.cleanup()
    
    # write the results
    open(outfile, 'w').write(job.results.as_json_string())
    

if __name__ == '__main__': main(**parse_args(sys.argv[1:]))
    

