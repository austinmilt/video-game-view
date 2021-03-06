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
ARG_VIDEOID = 'id'



# #############################################################################
# ARGUMENT PARSERS/BUILDERS ###################################################
# #############################################################################

def parse_replay_pairs(arg, reverse=False):
    """Pass in string for forward, list [[time, replay], [time, replay]...] for reverse."""
    if reverse:
        flat = []
        for pair in arg: flat.extend(pair)
        return ' '.join([str(s) for s in flat])

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
    ARG_REPLAYS: parse_replay_pairs, ARG_QUALITY: parse_int,
    ARG_VIDEOID: parse_str
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

def build_job_call(video, replays=None, skip=None, outfile=None, quality=None, videoID=None):
    """Builds a call that cal be invoked with Subprocess.call and will call the main() function here."""
    if outfile is None: outfile = os.path.join(OPTIONS.JB.SCRATCH, str(uuid4()))
    if replays is None: replays = []
    toCall = list(JOB_CALLBASE)
    toCall.append(parse_arg(ARG_VIDEO, video, True))
    toCall.append(parse_arg(ARG_OUTFILE, outfile, True))
    if skip is not None: toCall.append(parse_arg(ARG_SKIP, skip, True))
    if quality is not None: toCall.append(parse_arg(ARG_QUALITY, quality, True))
    if videoID is not None: toCall.append(parse_arg(ARG_VIDEOID, videoID, True))
    toCall.append(parse_arg(ARG_REPLAYS, replays, True))
    return toCall, outfile


def main(**args):
    """Runs the job and writes results. Args are as retruned by parse_args()."""
    
    # imports
    from jobs.jobs import Job
    from shutil import copy2
    
    # check arguments
    if ARG_VIDEO not in args: raise RuntimeError('Missing required argument: %s' % ARG_VIDEO)
    if ARG_OUTFILE not in args: raise RuntimeError('Missing required argument: %s' % ARG_OUTFILE)
    video = args[ARG_VIDEO]
    outfile = args[ARG_OUTFILE]
    replays = args.get(ARG_REPLAYS, None)
    skip = args.get(ARG_SKIP, None)
    quality = args.get(ARG_QUALITY, None)
    videoID = args.get(ARG_VIDEOID, None)
    
    # run the job
    job = Job.from_urls(
        video, replays=replays, skip=skip, quality=quality, verbose=True, 
        videoID=videoID
    )
    if job.results is None:
        job.run()
        job.cleanup()
    
        # write the results
        open(outfile, 'w').write(job.results.as_json_string())
        
    # if the results were downloaded from the vgv database, just return those
    else:
        copy2(job.results, outfile)
    

if __name__ == '__main__': main(**parse_args(sys.argv[1:]))
    

