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

# start logging
import logging, os, shutil
import tornado.log
LOGDIR = '/home/austin_w_milt/deployment/logs'
LOGNAME = 'server.log'
LOGFILE = os.path.join(LOGDIR, LOGNAME)
if os.path.exists(LOGDIR):
    try: shutil.rmtree(LOGDIR)
    except: pass
    
if not os.path.exists(LOGDIR): os.makedirs(LOGDIR)
tornado.log.enable_pretty_logging()
logHandler = logging.handlers.TimedRotatingFileHandler(LOGFILE, when='D', interval=1, backupCount=30, utc=True)
log = logging.getLogger('main')
log.setLevel(logging.INFO)
log.addHandler(logHandler)
accessLog = logging.getLogger("tornado.access")
accessLog.propagate = False
# accessLog.addHandler(logHandler)
appLog = logging.getLogger("tornado.application")
appLog.addHandler(logHandler)
genLog = logging.getLogger("tornado.general")
genLog.addHandler(logHandler)


# imports
import tornado.ioloop
import tornado.gen
import tornado.web
import tornado.websocket
import tornado.escape
from tornado.process import Subprocess
from tornado.concurrent import Future
from tornado.queues import Queue
from tornado.iostream import PipeIOStream
import json, sys, os
from uuid import uuid4
from time import time
from utilities.options import Options
from subprocess import CalledProcessError
from datetime import timedelta
from threading import Thread


# CONSTANTS
HTTPS_PORT = 443
CERT_FILE = r'/home/austin_w_milt/ssl_cert/my-ssl-crt.crt.pem'
CERTKEY_FILE = r'/home/austin_w_milt/ssl_cert/my-ssl-key.key.pem'
MSG_KWD_TYPE = 'type'
MSG_KWD_VIDEO = 'video'
MSG_KWD_REPLAYS = 'replays'
MSG_KWD_REQID = 'request_id'
MSG_KWD_INTERVAL = 'interval'
MSG_KWD_QUALITY = 'quality'
MSG_KWD_VIDEOID = 'id'
MSG_KWD_SESSIONID = 'session_id'
MSG_TYPE_PING = 'ping'
MSG_TYPE_REQUEST = 'request'
MSG_TYPE_KILLJOB = 'kill_job'
MSG_TYPE_SETSESSION = 'tell_session_id'
MSG_TYPE_NEWSESSION = 'get_new_session_id'
OUTGOING_KWD_TYPE = 'type'
OUTGOING_KWD_REQUEST = 'request'
OUTGOING_KWD_MSG = 'message'
OUTGOING_TYPE_MSG = 'message'
OUTGOING_TYPE_WARN = 'warning'
OUTGOING_TYPE_ERR = 'error'
OUTOING_TYPE_RES = 'result'
OUTGOING_TYPE_PONG = 'pong'
OUTGOING_TYPE_RECEIVED = 'received'
OUTGOING_TYPE_KILLEDJOB = 'killed_job'
OUTGOING_TYPE_SETSESSION = 'set_session_id'
OPTIONS = Options()
sys.path = [OPTIONS.GL.ROOT] + sys.path
from jobs.jobs import Job
from jobs.jobs_tornado import build_job_call

# gets public IP address of 
def get_public_ip():
    from urllib2 import urlopen
    return urlopen('http://ip.42.pl/raw').read()
    
    
PUBLIC_IP = get_public_ip()
START = time()
sockets = {}
terminated = False


# formats a dict to be converted to JSON message to send to client
# msg = message to send (and potentially display to) client
# rid = request ID as sent by client
# pre = message type identifier client may use for special treatment of message
def make_message(msg, rid='', pre=OUTGOING_TYPE_MSG):
    return json.dumps({OUTGOING_KWD_TYPE: pre, OUTGOING_KWD_REQUEST: rid, OUTGOING_KWD_MSG: msg})
    
def make_warning(msg, rid=''): return make_message(msg, rid, OUTGOING_TYPE_WARN)
def make_error(msg, rid=''): return make_message(msg, rid, OUTGOING_TYPE_ERR)
def make_result(msg, rid=''): return make_message(msg, rid, OUTOING_TYPE_RES)
def make_pong(msg, rid=''): return make_message('', '', OUTGOING_TYPE_PONG)
def make_received(msg, rid=''): return make_message('', '', OUTGOING_TYPE_RECEIVED)
def make_killedjob(msg, rid=''): return make_message(msg, '', OUTGOING_TYPE_KILLEDJOB)
def make_setsession(msg, rid=''): return make_message(msg, '', OUTGOING_TYPE_SETSESSION)


# main page handler
class MainHandler(tornado.web.RequestHandler):
    def get(self):
        self.write('Welcome to Video Game View. Requests cannot be made on the main page.')
        
      
# class to handle spawning the process that downloads files, parses videos and
# replay, and pipes printouts back to the client
class JobSubprocess:
    
    def __init__(self, request, processor):
        self.processor = processor
        self.call, self.expectedResults = build_job_call(
            request[MSG_KWD_VIDEO], request[MSG_KWD_REPLAYS], 
            skip=request.get(MSG_KWD_INTERVAL, None),
            quality=request.get(MSG_KWD_QUALITY, None),
            videoID=request.get(MSG_KWD_VIDEOID, None)
        )
        self.process = None
        self.future = Future()
        self.result = None
        self.killed = False
        

    # pipes subprocess printout to the message queue that is sent to client
    @tornado.gen.coroutine
    def redirect_stream(self, stream, formatter=make_message):
        while True:
            rid = self.processor.request[MSG_KWD_REQID]
            try: data = yield stream.read_until_regex(r'[\r\n]')
            except tornado.iostream.StreamClosedError: break
            else:
                if not self.killed: # otherwise just dispose of the message
                    yield self.processor.websocket.messages.put(formatter('%s\r\n' % (data), rid))
        
        
    # asynchronous spawn and wait for the job subprocess
    @tornado.gen.coroutine
    def wait(self):
        self.process = Subprocess(self.call, stdout=Subprocess.STREAM, stderr=Subprocess.STREAM)
        self.future = self.process.wait_for_exit()
        if self.processor.websocket.messages is not None:
            yield self.redirect_stream(self.process.stdout, make_message)
            yield self.redirect_stream(self.process.stderr, make_warning)
            

        # ignore the return code of the job, but assume we failed if the
        # file that is supposed to have been produced is not there
        try: returnCode = yield self.future
        except CalledProcessError as e:
            if ((not self.killed) or (e.returncode <> -9)): 
                log.error(self.call)
                raise e

        if not os.path.exists(self.expectedResults):
            if self.killed: self.result = 'Job killed.'
            else: self.result = 'Job failed.'
            
        # if the file is there, try to parse the file and then ditch it
        else:
            self.result = open(self.expectedResults, 'r').read()
            try: os.remove(self.expectedResults)
            except: log.warn('Couldnt remove temporary file %s' % self.expectedResults)
            
            
    # kill the suprocess running the video request
    @tornado.gen.coroutine
    def kill(self):
        if (not self.killed) and (not self.future.done()):
            self.killed = True
            self.process.proc.kill()
            
        raise tornado.gen.Return(True)


class WSInvalidRequest(Exception):
    """Called when user sends invalid request to the server."""
    pass
    
    
class WSPing(Exception): 
    """Called when user sends a ping. Result is ignored."""
    pass

    
# asynchronous fifo queue class to hold and periodically execute video 
# processing requests
class WSVideoProcessorQueue:
    def __init__(self):
        self._items = Queue()
        self._consuming = set()
        
    # add job requests to the queue
    @tornado.gen.coroutine
    def put(self, item):
        log.info('Socket %s added request %s to queue.' % (str(item.websocket.id), str(item.id)))
        cursize = self._items.qsize()
        yield self._items.put(item)
        raise tornado.gen.Return(cursize)
        
    # get the oldest item out of the queue
    @tornado.gen.coroutine
    def get(self):
        item = yield self._items.get()
        raise tornado.gen.Return(item)
        
    # non-blocking loop to constantly consume jobs as long as 
    # there are workers to do so
    @tornado.gen.coroutine
    def consume(self):
        while True:
            request = yield self.get()
            try: yield request.run()
            finally: self._items.task_done()
            

# build the queue as a global variable here so it can
# be accessed by instances of the video processor
QUEUE = WSVideoProcessorQueue()
        
        
# class to handle a single video processing request from the client,
# including parsing the request message, building the job subprocess,
# adding to the queue, running the job, and returning results to the user
class WSVideoProcessor:

    def __init__(self, msgdict, websocket):
        self.request = None
        self.websocket = websocket
        self.id = uuid4()
        self.ran = False
        self.valid = False
        try:
        
            # parse and validate the message 
            self.request = WSVideoProcessor.parse_message(msgdict)      
            WSVideoProcessor.validate_request_dict(self.request)
                        
            # dont allow users to overload the system
            if len(self.websocket.requests) >= OPTIONS.SV.SOCKET_LIMIT:
                msg = ''.join((
                    'You currently have %i jobs waiting to be ' % len(self.websocket.requests),
                    'processed. At most %i are allowed. ' % OPTIONS.SV.SOCKET_LIMIT,
                    'Please wait for some jobs to finish ',
                    'before sending more requests.'
                ))
                raise WSInvalidRequest(msg)
                
            # build a job from the validated request message
            self.job = JobSubprocess(self.request, self)
            self.valid = True
            
            # stick the job in the queue and finish
            position = QUEUE.put(self).result()
            self.write('Added request to queue behind %i other requests (ID %s).' % (position, str(self.id)))
            
        except WSInvalidRequest as e: self.write(e.message, formatter=make_error)
       

    # makes sure the user request meets minimum requirements for building
    # a job
    @staticmethod
    def validate_request_dict(request):
        if not isinstance(request, dict):
            raise WSInvalidRequest(u'Invalid video processing request. Should be JSON dict string.')

        if MSG_KWD_VIDEO not in request:
            raise WSInvalidRequest(u'Invalid video processing request. No video url found.')
            
        if MSG_KWD_REQID not in request:
            raise WSInvalidRequest(u'Invalid video processing request. Unique request ID required.')
            

    # parses the client's question into a dictionary to be used to build
    # and manage the video processing request
    @staticmethod
    def parse_message(msgdict):
        messageType = msgdict.get(MSG_KWD_TYPE, None)
        WSVideoProcessor.validate_request_dict(msgdict)
        try:
            video = msgdict[MSG_KWD_VIDEO]
            requestID = msgdict[MSG_KWD_REQID]
            replays = msgdict.get(MSG_KWD_REPLAYS, [])
            skip = msgdict.get(MSG_KWD_INTERVAL, None)
            quality = msgdict.get(MSG_KWD_QUALITY, None)
            videoID = msgdict.get(MSG_KWD_VIDEOID, None)
            return {
                MSG_KWD_TYPE: MSG_TYPE_REQUEST, MSG_KWD_VIDEO: video, 
                MSG_KWD_REPLAYS: replays, MSG_KWD_REQID: requestID,
                MSG_KWD_INTERVAL: skip, MSG_KWD_QUALITY: quality,
                MSG_KWD_VIDEOID: videoID
            }
            
        except KeyError:
            raise WSInvalidRequest(u'Invalid video processing request. One or more required keys is missing.')
      

    # adds messages to the message queue using the selected message formatter  
    @tornado.gen.coroutine    
    def write(self, message, formatter=make_message):
        if self.request is None: msg = formatter(message)
        else: msg = formatter(message, self.request.get(MSG_KWD_REQID, ''))
        yield self.websocket.messages.put(msg)
        
        
    # executes the video processing subprocess and puts results in the message
    # queue
    @tornado.gen.coroutine
    def run(self):
        if self.ran:
            yield self.write('Job already ran.', make_error)
            
        elif self.valid:
            self.ran = True
            log.info('Running job %s from socket %s' % (str(self.id), str(self.websocket.id)))
            yield self.write('Running job %s.' % str(self.id))
            yield self.job.wait()
            yield self.write(self.job.result, make_result)
            log.info('Job %s on socket %s complete.' % (str(self.id), str(self.websocket.id)))
            
        else:
            yield self.write('Trying to run invalid job.', make_error)
            
        self.destroy()
        
    
    # removes self from the socket and makes sure that if it comes up in the
    # queue it will not be run
    @tornado.gen.coroutine
    def destroy(self):
        if self.valid:
            del self.websocket.requests[self.request[MSG_KWD_REQID]]
            yield self.job.kill()
            
        self.valid = False
        raise tornado.gen.Return(True)
        
        
      
# handler for the client's websocket connections to the server
class RequestWebSocket(tornado.websocket.WebSocketHandler):

    # opens a websocket with the client and initializes requests
    # and message queue
    def open(self):
        self.id = None
        self.death = None
        self.requests = {}
        self.messages = None
        self.active = False
        log.info('New WebSocket opened from IP %s' % self.request.remote_ip)
        
        
    # sets the attributes of the websocket so it can be used for making requests
    def instantiate(self):
        self.id = str(uuid4())
        sockets[self.id] = self
        self.death = None
        self.requests = {}
        self.active = True
        self.messages = Queue()
        tornado.ioloop.IOLoop.current().add_callback(self.consume_messages)
        log.info('Attributed WebSocket %s started.' % self.id)
        self.messages.put(make_setsession(self.id))
        
        
    # replaces (by reference) necessary attributes of self to another socket
    def replace(self, other):
    
        # remove this socket from the server session
        self.active = False
        _ = sockets.pop(other.id, None) # remove other from sockets (to add back later)
        if self.id is not None: sockets[self.id] = other
        if self.death is not None:
            tornado.ioloop.IOLoop.current().remove_timeout(self.death)

        # replace this socket's attributes with the other's
        other.active = True
        other.id = self.id
        other.requests = self.requests
        other.messages = self.messages
        tornado.ioloop.IOLoop.current().add_callback(other.consume_messages)
            
        # update the children of this socket
        for request in self.requests.values(): request.websocket = other
        
    # set the compression level of messages (need to compress some due to the
    # large size of results)
    def get_compression_options(self):
        return {'compression_level': 9}
        

    # accept messages from the client and attempt to build a video processing
    # request/job
    def on_message(self, msg):
    
        # figure out what kind of message has been sent and treat it 
        # appropriately
        msgDict = None
        try: msgDict = json.loads(msg)
        except ValueError:
            self.messages.put(make_error('Messages must be JSON style.'))
            return
            
        if not isinstance(msgDict, dict):
            self.messages.put(make_error('Messages must be JSON style object.'))
            return
            
        # pong a ping
        msgType = msgDict.get(MSG_KWD_TYPE, None)
        if msgType == MSG_TYPE_PING: self.messages.put(make_pong(''))
        
        # start a new websocket 
        elif msgType == MSG_TYPE_NEWSESSION: self.instantiate()
        
        # build a video processing request
        elif msgType == MSG_TYPE_REQUEST:
            self.messages.put(make_received('Processing request recieved.', msgDict.get(MSG_KWD_REQID, '')))
            requestID = msgDict.get(MSG_KWD_REQID, None)
            if requestID is None:
                self.messages.put(make_error('Request ID required for a new job.'))
                return

            newRequest = WSVideoProcessor(msgDict, self)      
            if newRequest.valid: self.requests[requestID] = newRequest
            else: newRequest.destroy()
            
        # kill a job
        elif msgType == MSG_TYPE_KILLJOB:
            self.messages.put(make_received('Kill request recieved.', msgDict.get(MSG_KWD_REQID, '')))
            requestID = msgDict.get(MSG_KWD_REQID, None)
            if requestID is None:
                self.messages.put(make_error('Request ID required to kill job.'))

            elif requestID not in self.requests:
                self.messages.put(make_error('Request %s not found in requests.' % str(requestID), requestID))
                
            else:
                log.info('Killing job %s from socket %s.' % (self.requests[requestID].id, self.id))
                self.requests[requestID].destroy()
                self.messages.put(make_killedjob('Request %s killed' % str(requestID)))
                
        # load a different socket based on the user's session id
        elif msgType == MSG_TYPE_SETSESSION:
            sessionID = msgDict.get(MSG_KWD_SESSIONID, None)
            if sessionID not in sockets: self.instantiate()
            else:
                log.info('Replacing old session %s because client reconnected.' % sessionID)
                oldSocket = sockets[sessionID]
                oldSocket.replace(self)
            
        # unknown
        else:
            self.messages.put(make_error('Unrecognized message type. Doing nothing.', msgDict.get(MSG_KWD_REQID, '')))
            
        
    # get rid of all existing requests this socket has made
    def on_close(self):
        log.info('WebSocket %s closed. Setting destruction.' % str(self.id))
        self.death = tornado.ioloop.IOLoop.current().add_timeout(
            timedelta(seconds=OPTIONS.SV.SOCKET_TIMEOUT), self.destroy
        )
        
        
    # destroy and remove this socket from the server
    def destroy(self):
        toDestroy = list(self.requests.values())
        for request in toDestroy: request.destroy()
        del self.requests, toDestroy
        del sockets[self.id]
        log.info('Websocket %s destroyed.' % self.id)
        
        
    # security checking of connection requests
    def check_origin(self, origin):
        #### should implement a version eventually to allow connections
        #### only from your chrome extension clients (I gues youtube?)
        #### see http://www.tornadoweb.org/en/stable/websocket.html
        return True
        
    
    # asynchronous message queue for sending messages to the client
    @tornado.gen.coroutine
    def consume_messages(self):
        while self.active:
            try: 
                message = yield self.messages.get()
                yield self.write_message(message.strip())
                yield self.messages.task_done()
                
            except tornado.websocket.WebSocketClosedError: break
            
            
# threaded process for keeping the temporary directory clear
def manage_temp(
    tempDir=OPTIONS.JB.SCRATCH, intervalSeconds=OPTIONS.SV.TEMP_INTERVAL, 
    ageToDeleteSeconds=OPTIONS.SV.TEMP_AGE2DELETE
):
    from time import sleep, time
    from glob import glob
    import os
    lastCheck = None
    while not terminated:
        if (lastCheck is None) or ((time() - lastCheck) > intervalSeconds):
            lastCheck = time()
            files = glob(os.path.join(tempDir, '*'))
            for f in files:
                modifiedAge = time() - os.path.getmtime(f)
                if modifiedAge > ageToDeleteSeconds:
                    try: os.remove(f)
                    except: log.warn('Could not remove old temporary file %s' % f)
                
        sleep(1)
    
        
        
if __name__ == "__main__":

    # create the web server
    log.info('Starting web application')
    application = tornado.web.Application([
        (r'/', MainHandler),
        (r'/websocket', RequestWebSocket)
    ], debug=True, xheaders=True)

    # set up listening ports
    for port in OPTIONS.SV.PORTS:
        sslOptions = None
        if port == HTTPS_PORT:
            sslOptions = ssl_options = {
                'certfile': CERT_FILE, 'keyfile': CERTKEY_FILE
            }
            
        application.listen(port, ssl_options=sslOptions)
        log.info('Listening on port %i' % port)
    
    # start workers to consume the requests for video processing
    for i in range(OPTIONS.SV.WORKERS):
        tornado.ioloop.IOLoop.current().add_callback(QUEUE.consume)
        log.info('Started worker %i' % i)
        
        
    # start a thread to monitor and manage the temporary directory
    # (to handle deleting files that were not deleted by their processes)
    tempManager = Thread(target=manage_temp)
    tempManager.start()
        
    # start the server
    log.info('Starting server')
    try: tornado.ioloop.IOLoop.current().start()
    except KeyboardInterrupt as e: 
        terminated = True
        raise e