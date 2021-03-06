/**
 * @license Apache-2.0
 * Copyright 2018 Austin Walker Milt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
 
/**
 * @file Extension background page and websocket client for communication with server. 
 * Manages connection to the server, piping messages to the popup, starting the
 * viewer, popup states, and popup action requests (e.g. new videos, removal of
 * results, etc.)
 * @author [Austin Milt]{@link https://github.com/austinmilt}
*/

///////////////////////////////////////////////////////////////////////////////
// CONSTANTS AND INITIALIZATION OF GLOBALS ////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////

// CONSTANTS ****************************************************************/

/**
 * @description interval between pings to the server (milliseconds)
 * @constant
 * @default
*/
const PING_INTERVAL = 30000;

/**
 * @description maximum delay for a pong response from the server (milliseconds)
 * @constant
 * @default
*/
const PONG_TIMEOUT = 120000;

/**
 * @description interval between attempts to re-connect to the server (milliseconds)
 * @constant
 * @default
*/
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


// GLOBALS ******************************************************************/
var connectAttempts = 0;
var userDisconnectFlag = false;
var killJobsOnUnload = true;
var CLIENT = null;

// load user options from chrome storage
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
            console.log(`${key}: ${changes[key].newValue}`);
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



///////////////////////////////////////////////////////////////////////////////
// MAIN WEBSOCKET CLASS ///////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////

/** 
 * Class that opens and maintains a (websocket) connection to the analysis 
 * server in order to make video processing requests and receive processing
 * messages and results.
 * <p>
 * Note: This websocket will be disconnected by the GCP balancer after the
 * backend teimout, regardless of ping/pong activity.
 * </p>
 */
function WebSocketClient(host) {
    this.host = host;
    this.socket = null;
    this.timeout = null;
    this.pinger = null;
    var self = this;
    
    /**
     * Closes connection to the server.
     */
    this.disconnect = function() {
        if (self.socket) { self.socket.close(); }
    }
    
    /**
     * Attempts to connect to server, and reacts to success or failure, generating
     * connection-dependent functions and messages.
     * @return {Promise} promise that resolves a Websocket if successful
     */
    this.connect = function() {
        return new Promise(function(resolve, reject) {
        
            connectAttempts += 1;
            self.socket = new WebSocket(self.host);
            state['connection'] = CONN_CONNECTING;
            post_to_ports({'type': 'socket_connecting'});
            
            /**
             * Tell popup(s) that the connection to the server was a success,
             * tell the server what this socket's ID is (in case jobs are
             * running from a previous connection) and resolve the promise.
             */
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
                
                // tell popups we're connected and resolve
                state['connection'] = CONN_CONNECTED;
                post_to_ports({'type': 'socket_opened'});
                console.log('Connection opened.');
                self.pinger = setInterval(self.ping, PING_INTERVAL);
                resolve(self.socket);
                connectAttempts = 0; // reset for auto-reconnection
            }
            
            
            /**
             * Accepts and processes messages from the server, including
             * pongs, results, errors, warnings, and regular messages.
             * @return {json} message from the server, if any (otherwise empty return)
             */
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
                
                // -- server sent results
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
                
                // -- server sent a message (warning, error, or message)
                //    for a specific job
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
                
                // -- server sent a general message
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

            
            /**
             * Placeholder (currently not doing anything when the websocket has an error).
             */
            self.socket.onerror = function(err) {console.log(err);}
            
            
            /**
             * Handles the websocket being closed (by user or server).
             */
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
            
            
            /**
             * Pings the server at regular intervas to keep the connection alive.
             */
            self.ping = function() {
                self.socket.send(MESSAGE_PING);
                self.timeout = setTimeout(function() {
                    self.socket.close();
                }, PONG_TIMEOUT);
            }
            
            
            /**
             * Clears the last ping interval.
             */
            self.pong = function() {
                clearTimeout(self.timeout);
            }
        });
    }
}



///////////////////////////////////////////////////////////////////////////////
// WEBSOCKET AND OTHER HELPER FUNCTIONS ///////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////

/**
 * Post a message to a single port.
 * @param {port} port - port to post message to
 * @param {object} message - message to send to the port, by convention having 'type' as one of the attributes
 */
function post_to_port(port, message) {
    message['popup'] = state['popup'];
    message['connection'] = state['connection'];
    port.postMessage(message);
}


/**
 * Post a message to all opened ports (should be only one).
 * {object} message - @see {@link post_to_port}
 */
function post_to_ports(message) {
    ports.forEach(function(port) {post_to_port(port, message)});
}


/**
 * Kills all running jobs (i.e. that have not gotten results back yet)
 * @see {@link remove_job}
 */
function kill_all_running_jobs() {
    for (var job of state['id_order']) {
        if (killJobsOnUnload && !state[job]['ready']) {
            remove_job(job);
        }
    }
}


/**
 * Loads saved results from local storage.
 * @param {string} requestID - request/job ID of the video to load results for
 * @param {boolean} decompress - if true, will be decompressed from bzip using @see {@link https://github.com/nodeca/pako}
 * @param {boolean} json - if true, json-parses the decompressed result (@link decompress} must be true
 * @return {object} loaded video processing results
 */
function load_result(requestID, decompress=false, json=false) {
    return new Promise(function(resolve, reject) {
        chrome.storage.local.get([requestID], function(response) {
            if (response[requestID] !== undefined) {
                var result = response[requestID];
                if (decompress) { result = pako.inflate(result, {to: 'string'}); }
                if (json && decompress) { result = JSON.parse(result); }
                resolve(result);
            }
            else { reject('Request not found.'); }
        })
    });
}


/**
 * Starts the websocket client.
 */
function start_ws() {
    if (!CLIENT) {
        CLIENT = new WebSocketClient(hostURL);
    }
    userDisconnectFlag = false;
    CLIENT.connect();
}


/**
 * Stops the websocket client.
 */
function stop_ws() {
    if (CLIENT) {
        userDisconnectFlag = true;
        CLIENT.disconnect();
    }
}


/**
 * Formats a video processing request and sends to server via the websocket.
 * @param {string} - video url
 * @param {array} - replays with start time and replay url (or match ID), @see popup.js
 * @param {string} - title of the video (just to be stored for viewing in popup)
 * @param {string} - unique ID of the video (used for storing/querying results in the VGV database)
 */
function send_request(video, replays, title, videoID) {
    if (CLIENT) {
        var request = {
            'type': 'request',
            'video': video,
            'replays': replays,
            'request_id': Math.floor(Math.random()*10000000000),
            'interval': frameInterval,
            'id': videoID
            // 'type': 'request', 
            // 'video': 'https://youtu.be/gYwr7SK72PY',
            // 'replays': [[0.0, 'https://www.dropbox.com/s/achxrbapg6spnlu/replay_r01.dem?dl=1']],
            // 'request_id': Math.floor(Math.random()*10000000000)
        };
        console.log(request);
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


/**
 * Tells a popup the websocket connection status.
 * @param {port} port - popup port to send the state to
 */
function tell_connection_state(port) {
    post_to_port(port, {type: 'connection_state'});
}


/**
 * Tells a popup the status of all jobs (for when the popup needs to reload).
 * @param {port} port - popup port to send the state to
 */
function tell_tracker_state(port) {
    post_to_port(port, {type: 'tracker_state', state: state});
}


/**
 * Tells a popup the "page" that should be opened when the popup starts.
 * @param {port} port - popup port to send the state to
 */
function tell_popup_state(port) { 
    post_to_port(port, {type: 'popup_state'});
}


/**
 * Sets the popup page state to whatever the popup says it should be.
 * @param {string} newState - new state to set for popups when they open
 */
function set_popup_state(newState) { state['popup'] = newState; }


/**
 * Removes a job's results and cancels the job if it's running, e.g. if
 * the user no longer wants those results.
 * @param {string} jobID - ID of the request/job to cancel
 */
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

/**
 * Injects javascript into the current tab with a promise which is resolved
 * when the injection is complete.
 * @param {string} jsPath - relative path (in the extension) to the javascript to inject
 * @return {Promise} a promise which is resolved when the injection is finished
 */
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


/**
 * Injects css into the current tab with a promise which is resolved
 * when the injection is complete.
 * @param {string} cssPath - relative path (in the extension) to the css to inject
 * @return {Promise} a promise which is resolved when the injection is finished
 */
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


/**
 * Injects VGV viewer scripts into the current tab using chained promises.
 * @return {promise} promise that resolves when all scripts have been injected
 */
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


/**
 * Activates a VGV viewer script that has been injected to the current tab.
 * @param {object} msg - json-style message (by VGV convention has 'trigger' attribute)
 * @return {promise} promise that is resolved (or rejected) based on script response
 */
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


/**
 * Activates the VGV viewer in the current tab.
 * @param {string} longID - job/request ID to activate
 */
function start_viewer(longID, tabID) {
    var videoURL = state[longID]['url'];
    
    // navigate to the requested video
    chrome.tabs.update(tabID, {url: videoURL}, function(tab) {
        
        // add a listener that will activate the viewer when scripts have been injected
        var listener = function(tabId2, changeInfo, tab2) {
            if ((tabId2 == tabID) && (changeInfo.status == 'complete')) {
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
}



///////////////////////////////////////////////////////////////////////////////
// REQUEST LISTENERS //////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////

// Add listeners that will ask the server to kill all running jobs when the
// extension is unloaded (e.g. if the browser is closed or the extension is
// reloaded.
chrome.runtime.onSuspend.addListener(function(){killJobsOnUnload = true; kill_all_running_jobs()});
chrome.runtime.onSuspendCanceled.addListener(function(){killJobsOnUnload = false;})

// listen for popups to connect to the background page
chrome.runtime.onConnect.addListener(function(port) {
    
    // add to the list of popups to send update messages to
    ports.add(port);
    
    // add a listener for requests, deletions of results, or
    // termination of the socket
    port.onMessage.addListener(function(msg) {
        if (msg.action == 'request') { send_request(msg.video, msg.replays, msg.title, msg.id); }
        else if (msg.action == 'connect') { start_ws(); }
        else if (msg.action == 'user_disconnect') { stop_ws(); }
        else if (msg.action == 'get_connection_state') { tell_connection_state(port); }
        else if (msg.action == 'get_tracker_state') { tell_tracker_state(port); }
        else if (msg.action == 'get_popup_state') { tell_popup_state(port); }
        else if (msg.action == 'set_popup_state') { set_popup_state(msg.state); }
        else if (msg.action == 'remove_job') { remove_job(msg.job); }
        else if (msg.action == 'start_viewer') { start_viewer(msg.job, msg.tab); }
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