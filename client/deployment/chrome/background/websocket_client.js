/*
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
*/

const PING_INTERVAL = 30000;
const PONG_TIMEOUT = 120000;
const CONNECT_INTERVAL = 5000;
const CONN_CONNECTED = 'connected';
const CONN_DISCONNECTED = 'disconnected';
const CONN_CONNECTING = 'connecting';
const MESSAGE_PING = JSON.stringify({'type': 'ping'})
const MESSAGE_KWD_TYPE = "type";
const MESSAGE_KWD_MSG = "message";
const MESSAGE_KWD_REQUEST = "request";
const TYPE_PONG = "pong";
const TYPE_MSG = "message";
const TYPE_WARNING = "warning";
const TYPE_ERROR = "error";
const TYPE_RESULT = "result";
const TYPE_RECEIVED = "received";
const TYPE_KILLEDJOB = "killed_job";
const TYPE_SETSESSION = "set_session_id";
const hostURL = 'wss://www.videogameview.com/websocket';

var connectAttempts = 0;
var userDisconnectFlag = false;
var killJobsOnUnload = true;

// user options
var frameInterval = 1.0;
var connectAttemptsMax = 3;
var alertOnResults = false;
chrome.storage.sync.get(['connect_attempts', 'frame_interval'], function(e) {
    if (e['connect_attempts'] !== undefined) { connectAttemptsMax = e['connect_attempts']; }
    if (e['frame_interval'] !== undefined) { frameInterval = e['frame_interval']; }
    if (e['alert_results'] !== undefined) { alertOnResults = e['alert_results']; }
});
chrome.storage.onChanged.addListener(function(changes, namespace) {
    if (namespace == 'sync') {
        for (key in changes) {
            if (key == 'connect_attempts') { connectAttemptsMax = changes[key].newValue; }
            else if (key == 'frame_interval') { frameInterval = changes[key].newValue; }
            else if (key == 'alert_results') { alertOnResults = changes[key].newValue; }
        }
    }
});


// tracking tabs and request results
var state = {
    'general': { 'message': '', 'message_type': TYPE_MSG },
    'id_order': [], 'connection': CONN_DISCONNECTED, 'popup': 'menu',
    'session_id': null
};
var ports = new Set();


// load saved result info if any
var requestCount = 0;
var savedState = {};
chrome.storage.local.get(['background_state'], function(e) {
    if (e['background_state'] !== undefined) {
        savedState = e['background_state']; 
        if (savedState.hasOwnProperty('id_order')) {
            for (var requestID of savedState['id_order']) {
                if (savedState[requestID]['ready']) {
                    state[requestID] = savedState[requestID];
                    state['id_order'].push(requestID);
                    state['short_id'] = requestCount + 1;
                    requestCount += 1;
                }
            }
        }
    }
});


// functions to deal with ports
function post_to_port(port, message) {
    message['popup'] = state['popup'];
    message['connection'] = state['connection'];
    port.postMessage(message);
}

function post_to_ports(message) {
    ports.forEach(function(port) {post_to_port(port, message)});
}


// add a listener that will ask the server to kill all running
// jobs when the extension is unloaded (e.g. if the browser is closed or
// the extension is reloaded)
function kill_all_running_jobs() {
    for (var job of state['id_order']) {
        if (killJobsOnUnload && !state[job]['ready']) {
            remove_job(job);
        }
    }
}
chrome.runtime.onSuspend.addListener(function(){killJobsOnUnload = true; kill_all_running_jobs()});
chrome.runtime.onSuspendCanceled.addListener(function(){killJobsOnUnload = false;})


// function to load saved results from local storage
function load_result(requestID, decompress=false, json=false) {
    return new Promise(function(resolve, reject) {
        chrome.storage.local.get([requestID], function(response) {
            if (response[requestID] !== undefined) {
                var result = response[requestID];
                if (decompress) { result = pako.inflate(result, {to: 'string'}); }
                if (json) { result = JSON.parse(result); }
                resolve(result);
            }
            else { reject('Request not found.'); }
        })
    });
}


// class for opening and maintaining a (websocket) connection to the
// server in order to make video processing requests and then receive
// results
//
// NOTE TO SELF: This websocket will be disconnected by Google Cloud
// Load Balancer after the backend timeout, regardless of ping/pong
// activity.
function WebSocketClient(host) {
    this.host = host;
    this.socket = null;
    this.timeout = null;
    this.pinger = null;
    var self = this;
    
    // method to disconnect from server
    this.disconnect = function() {
        if (self.socket) { self.socket.close(); }
    }
    
    // method to make the connection to the server
    this.connect = function() {
        return new Promise(function(resolve, reject) {
        
            connectAttempts += 1;
            self.socket = new WebSocket(self.host);
            state['connection'] = CONN_CONNECTING;
            post_to_ports({'type': 'socket_connecting'});
            
            // indicate that connection is open and start ping/pong
            self.socket.onopen = function() {
                
                // tell server this session ID to try to recover running jobs
                // or get a new session ID so this can be done later
                if (state['session_id']) {
                    msg = {
                        'type': 'tell_session_id', 
                        'session_id': state['session_id']
                    };
                    self.socket.send(JSON.stringify(msg));
                }
                else {
                    msg = {'type': 'get_new_session_id'};
                    self.socket.send(JSON.stringify(msg));
                }
                
                
                // do other stuff
                state['connection'] = CONN_CONNECTED;
                post_to_ports({'type': 'socket_opened'});
                console.log('Connection opened.');
                self.pinger = setInterval(self.ping, PING_INTERVAL);
                resolve(self.socket);
                connectAttempts = 0; // reset for auto-reconnection
            }
            
            // method to handle responses from the server, including
            // recognizing pongs and parsing results, errors,
            // warnings, and regular messages
            self.socket.onmessage = function(event) {
                var message = JSON.parse(event.data);
                var messageType = message[MESSAGE_KWD_TYPE];
                
                // if it's a pong message, run the pong function
                if (messageType == TYPE_PONG) {
                    self.pong();
                    return;
                }
                
                // if it's just confirmation the message was received,
                // do nothing
                else if (messageType == TYPE_RECEIVED) { return; }
                
                // if it's a message to set the session ID, do that
                else if (messageType == TYPE_SETSESSION) {
                    state['session_id'] = message[MESSAGE_KWD_MSG];
                    return;
                }
                
                // otherwise it's a message to display to user, so find
                // the request that needs info displayed for and post
                // to that
                messageBody = message[MESSAGE_KWD_MSG].trim();
                requestID = message[MESSAGE_KWD_REQUEST];
                var msg = null;
                if (messageBody == '') { return; }
                if ((messageType == TYPE_RESULT) && requestID) {
                    if (state.hasOwnProperty(requestID)) {
                        
                        // store in local storage for later retrieval
                        state[requestID]['ready'] = true;
                        var toStore = {};
                        toStore[requestID] = pako.deflate(messageBody, {to: 'string'});
                        chrome.storage.local.set(toStore);
                        chrome.storage.local.set({'background_state': state});
                      
                        // notify the popup and user that results are ready
                        msg = {
                            type: 'result', tracker: requestID,
                            message: state[requestID]['ready'], message_type: messageType,
                            url: state[requestID]['url'], title: state[requestID]['title']
                        }
                        if (alertOnResults) { 
                            alert(state[requestID]['title'] + ' is ready for viewing.');
                        }
                    }
                    else {
                        msg = {
                            type: 'update_general', message: messageBody,
                            message_type: TYPE_MSG
                        }
                        state['general']['message'] = messageBody;
                        state['general']['message_type'] = messageType;
                    }
                }
                else if (state.hasOwnProperty(requestID)) {
                    if (messageType == TYPE_WARNING) {
                        state[requestID]['warnings'].push(messageBody);
                    }
                    msg = {
                        type: 'update_tracker', tracker: requestID,
                        message: messageBody, message_type: messageType,
                        warnings: state[requestID]['warnings']
                    }
                    state[requestID]['message'] = messageBody;
                    state[requestID]['message_type'] = messageType;
                }
                else if ((requestID === null) || (requestID === undefined)) {
                    msg = { type: 'update_general', message: messageBody,
                        message_type: messageType
                    };
                    state['general']['message'] = messageBody;
                    state['general']['message_type'] = messageType;
                }
                else { return; } // throw away message for requests that no longer exist
                post_to_ports(msg);
                return message;
            }

            
            // placeholder
            self.socket.onerror = function(err) {}
            
            
            // method to clean up and notify when the websocket
            // is closed
            self.socket.onclose = function(closeEvent) {
                
                // update settings that occur on any closure (failed connect, 
                // error disconnet, user disconnect)
                clearInterval(self.pinger);
                
                // reconnect
                if (!userDisconnectFlag && (connectAttempts < connectAttemptsMax)) {
                    state['connection'] = CONN_CONNECTING;
                    post_to_ports({ type: 'socket_reconnecting', message:'(Re)connection attempt ' + connectAttempts });
                    console.log('Connection closed or failed. Attempting reconnect.');
                    var self2 = self;
                    setTimeout(
                        function() { self2.connect().then().catch() }, 
                        CONNECT_INTERVAL
                    );
                }
                
                // notify user of disconnect
                else if (!userDisconnectFlag) {
                    state['connection'] = CONN_DISCONNECTED;
                    var msg = 'Could not (re)connect to server after ';
                    msg += connectAttemptsMax;
                    msg += ' attempts. Closed code ';
                    msg += closeEvent.code;
                    msg += ' with message "';
                    msg += closeEvent.reason + '"';
                    post_to_ports({ type: 'socket_closed_by_server', message: msg });
                    console.log('Connection closed by server.');
                    connectAttempts = 0;
                }
                
                else {
                    state['connection'] = CONN_DISCONNECTED;
                    post_to_ports({ type: 'socket_closed_by_user' });
                    console.log('Connection closed by user.');
                    connectAttempts = 0;
                }
            }
            
            
            // method to send keepAlive and polling interaction
            // with the server
            self.ping = function() {
                self.socket.send(MESSAGE_PING);
                self.timeout = setTimeout(function() {
                    self.socket.close();
                }, PONG_TIMEOUT);
            }
            
            
            // method to do pong response reaction
            self.pong = function() {
                clearTimeout(self.timeout);
            }
        });
    }
}


// open a socket to the server so we can make requests
var CLIENT = null;
function start_ws() {
    if (!CLIENT) {
        CLIENT = new WebSocketClient(hostURL);
    }
    userDisconnectFlag = false;
    CLIENT.connect();
}


// closes the socket when the user requests it
function stop_ws() {
    if (CLIENT) {
        userDisconnectFlag = true;
        CLIENT.disconnect();
    }
}


// send a video processing request to the server
function send_request(video, replays, title) {
    if (CLIENT) {
        var request = {
            'type': 'request',
            'video': video,
            'replays': replays,
            'request_id': Math.floor(Math.random()*10000000000),
            'interval': frameInterval
            // 'type': 'request', 
            // 'video': 'https://youtu.be/gYwr7SK72PY',
            // 'replays': [[0.0, 'https://www.dropbox.com/s/achxrbapg6spnlu/replay_r01.dem?dl=1']],
            // 'request_id': Math.floor(Math.random()*10000000000)
        };
        CLIENT.socket.send(JSON.stringify(request));
        requestID = request['request_id'];
        state['id_order'].push(requestID);
        state[requestID] = {};
        state[requestID]['message'] = '';
        state[requestID]['message_type'] = TYPE_MSG;
        state[requestID]['long_id'] = requestID;
        state[requestID]['short_id'] = requestCount + 1;
        state[requestID]['url'] = request['video'];
        state[requestID]['title'] = title;
        state[requestID]['warnings'] = [];
        state[requestID]['ready'] = false;
        post_to_ports({
            'type': 'new_tracker', 'long_id': requestID, 
            'short_id': state[requestID]['short_id'], 'title': title
        });
        requestCount += 1;
    }
}


// when a popup asks for the connection state, send along
function tell_connection_state(port) {
    post_to_port(port, {type: 'connection_state'});
}


// when a popup asks for the full state of things, send along
function tell_tracker_state(port) {
    post_to_port(port, {type: 'tracker_state', state: state});
}


// when a popup asks for which page it should open, send along
function tell_popup_state(port) { 
    post_to_port(port, {type: 'popup_state'});
}


// set the popup state based on what happened on a popup
function set_popup_state(newState) { state['popup'] = newState; }


// remove a job's results and cancel the job if it's running
function remove_job(jobID) {
    
    // tell the server to kill this job
    if (CLIENT && !state[jobID]['ready']) {
        request = {
            'type': 'kill_job',
            'request_id': jobID
        }
        CLIENT.socket.send(JSON.stringify(request));
    }
    
    // should probably wait for answer from server before doing this, but...
    // tell popups to delete this tracker
    post_to_ports({'type': 'remove_tracker', 'tracker': jobID});
    
    // remove data from the background state
    delete state[jobID];
    
    // update the short ids of the other jobs
    var newOrder = []
    var newCount = 0;
    var newShorts = {};
    for (var job of state['id_order']) {
        if (job != jobID) {
            newOrder.push(job);
            state[job]['short_id'] = newCount + 1;
            newShorts[job] = state[job]['short_id'];
            newCount += 1;
        }
    }
    state['id_order'] = newOrder;
    requestCount = newCount;
    post_to_ports({'type': 'update_shorts', 'data': newShorts});
    
    // update results saved to local storage
    chrome.storage.local.set({'background_state': state});
    chrome.storage.local.remove(String(jobID));
}



///////////////////////////////////////////////////////////////////////////////
// VIEWER MANAGEMENT //////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////


// general functions for injecting css and javascript with promises
function inject_js(jsPath) {
    return new Promise(function(resolve, reject) {
        chrome.tabs.query({active: true, currentWindow: true}, function(tabs) {
            chrome.tabs.executeScript(tabs[0].id, {file: jsPath}, function(result) {
                var e = chrome.runtime.lastError;
                if (e){ console.log('Javascript injection error with message: ' + e.message); }
                resolve();
            });
        });
    });
}


// inject css into activated tab
function inject_css(cssPath) {
    return new Promise(function(resolve, reject) {
        chrome.tabs.query({active: true, currentWindow: true}, function(tabs) {
            chrome.tabs.insertCSS(tabs[0].id, {file: cssPath}, function(result) {
                var e = chrome.runtime.lastError;
                if (e){ console.log('CSS injection error with message: ' + e.message); }
                resolve();
            });
        });
    });
}


// function to inject scripts into activated tab
function inject_scripts() {
    return new Promise(function(resolve, reject) {
        inject_css('popup/scripts/tooltip.css')
        .then(function(){return inject_css('page/scripts/tooltip_manager.css')})
        .then(function(){return inject_js('popup/scripts/jquery-3.2.1.min.js')})
        .then(function(){return inject_js('background/pako.js')})
        .then(function(){return inject_js('popup/scripts/tooltip.js')})
        .then(function(){return inject_js('page/scripts/master.js')})
        .then(function(){return inject_js('page/scripts/dotapedia.js')})
        .then(function(){return inject_js('page/scripts/game_unit.js')})
        .then(function(){return inject_js('page/scripts/hero_timer.js')})
        .then(function(){return inject_js('page/scripts/tooltip_manager.js')})
        .then(function(){resolve()})
        .catch(function(error){console.log(error)});
    });
}


// activate scripts
function activate(msg) {
    return new Promise(function(resolve, reject) {
        chrome.tabs.query({active: true, currentWindow: true}, function(tabs) {
            chrome.tabs.sendMessage(tabs[0].id, msg, function (response) {
                var e = chrome.runtime.lastError;
                if (e !== undefined){ 
                    reject('Activation error in tab "' + tabs[0].title + '" with message: ' + e.message); 
                }
                else { resolve(response); }
            }) 
        });
    });
}


// starts the program
function start_viewer(longID) {
    var videoURL = state[longID]['url'];
    chrome.tabs.query({currentWindow: true, active: true}, function (tab) {
        chrome.tabs.update(tab.id, {url: videoURL}, function(tab) {
            var listener = function(tabId, changeInfo, tab) {
                if ((tabId == tab.id) && (changeInfo.status == 'complete')) {
                    chrome.tabs.onUpdated.removeListener(listener);
                    inject_scripts()
                    .then(function(){return activate({trigger: 'activate_master', data: longID})})
                    .then(function(){return activate({trigger: 'activate_tooltips'})})
                    .then(function(v) {console.log(v)}, null)
                    .catch(function(rejection) { console.log('Rejection:'); console.log(rejection); });
                }
            }
            chrome.tabs.onUpdated.addListener(listener);
        });
    });
}



///////////////////////////////////////////////////////////////////////////////
// REQUEST LISTENERS //////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////

// listen for popups to connect to the background page
chrome.runtime.onConnect.addListener(function(port) {
    
    // add to the list of popups to send update messages to
    ports.add(port);
    
    // add a listener for requests, deletions of results, or
    // termination of the socket
    port.onMessage.addListener(function(msg) {
        if (msg.action == 'request') { send_request(msg.video, msg.replays, msg.title); }
        else if (msg.action == 'connect') { start_ws(); }
        else if (msg.action == 'user_disconnect') { stop_ws(); }
        else if (msg.action == 'get_connection_state') { tell_connection_state(port); }
        else if (msg.action == 'get_tracker_state') { tell_tracker_state(port); }
        else if (msg.action == 'get_popup_state') { tell_popup_state(port); }
        else if (msg.action == 'set_popup_state') { set_popup_state(msg.state); }
        else if (msg.action == 'remove_job') { remove_job(msg.job); }
        else if (msg.action == 'start_viewer') { start_viewer(msg.job); }
        else { post_to_port(port, {mesage: 'Background page received uknown action from popup; see background console for details.'}); console.log(msg); }
    });
    
    
    // when a popup is closed, remove it from those that need to be
    // updated when the server sends a message
    port.onDisconnect.addListener(function() {
        ports.delete(port);
    });
});


// listen for content script to request results (or other stuff?)
chrome.runtime.onMessage.addListener(function(request, sender, sendResponse) {
    if (request.action == 'get_result') {
        var reqID = request.id.toString();
        var func = sendResponse;
        load_result(reqID)
            .then(function(result) { func({type: 'result', data: result})})
            .catch(function(error) { func({type: 'error', msg: error})});
            
        return true;
    }
});