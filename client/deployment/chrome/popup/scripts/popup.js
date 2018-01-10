/**
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


///////////////////////////////////////////////////////////////////////////////
// TO-DO //////////////////////////////////////////////////////////////////////
// X server accept job cancelation requests and cancel running jobs
// o function to stop tooltips
// o menu to stop tooltips
// o something wrong with client with server shuts down... when you hit Start again gets stuck in Connecting....
// o user disconnecting from server and then reconnecting doesnt work
// o sometimes when user tries to load results some scripts return before ready and causes subsequent calls to fail
// o some clients being logged out (websocket closes) when popup closes(?) dunno why
// o dont force user out of tracker menu when offline (especially when disconnected by server)
// o store tracker state in chrome.storage.sync and reload on opening
// o have some non-interpreted newlines (\n) in your tooltips
// X ability to delete trackers (and/or all trackers)
// X add option to skip confirmation of job deletion
// X talent tooltip is not getting all its styling (has default border-radius and color)
// X hovering over tracker status when tooltip killed leaves tooltip in page
// X prevent popup from re-injecting content scripts that are already in.
// X show warnings in tracker tooltips
// X links to github, bug reporting, feature requests
// X fullscreen tooltips (make tooltip box div a child of ytp-iv-video-content
//      and give it a high zIndex (or z-index in css)
// X max width job status text with tooltip of complete
// X popup queries page to start with upon opening
// X tracker menu item to return to menu without restarting everything
// X program the options menu to work correctly and hook up the components
// X tracker menu item to disconnect from server
// X make job results text or hover be the video name
// X make custom tooltips for popup and options page
// X on server disconnect display a message to the user
// X make your tooltips accept a template
// X make a template for the job status tooltips with color coded warnings
// X fix tooltip overflow in popup resizing window
// X hook up the frame interval on the server (already being sent by websocket client)
// X websocket_client.js isnt correctly using the server reconnect attempts
//////////////////////////////////////////////////////////////////////////////

var started = false;
var connected = false;
var TIMER;
var TOOLTIPS;

const PORT_NAME = 'vgv_foreground';
const TYPE_PONG = "pong";
const TYPE_MSG = "message";
const TYPE_WARNING = "warning";
const TYPE_ERROR = "error";
const TYPE_RESULT = "result";
const TYPE_RECEIVED = "received";
const CLASS_DELJOB = ['request', 'delete_tracker'];
const CLASS_JOB = ['request', 'request_default', 'job_status_title', 'has_vgvtt'];
const CLASS_REQPAR = ['request_paragraph'];
const MESSAGE_CLASS = {};
MESSAGE_CLASS[TYPE_MSG] = ['request', 'request_default', 'has_vgvtt'];
MESSAGE_CLASS[TYPE_WARNING] = ['request', 'request_warning', 'has_vgvtt'];
MESSAGE_CLASS[TYPE_ERROR] = ['request', 'request_error', 'has_vgvtt'];
MESSAGE_CLASS[TYPE_RESULT] = ['request', 'request_result'];

const HTML_START = 'start_button';
const HTML_OPTIONS = 'options_button';
const HTML_FORUM = 'forum_button';
const HTML_WEBSITE = 'website_button';

const HTML_REQUEST = 'request_button';
const HTML_MSGREQ = 'request_incoming';
const HTML_MSGGNL = 'server_incoming';

const HTML_TMM = 'tracker_menu_gomenu';
const HTML_TD = 'tracker_menu_disconnect';

const HTML_PATH_MENU = chrome.runtime.getURL('popup/html/menu.html');
const HTML_PATH_CONN = chrome.runtime.getURL('popup/html/connecting.html');
const HTML_PATH_CFAIL = chrome.runtime.getURL('popup/html/connection_failed.html');
const HTML_PATH_TRACK = chrome.runtime.getURL('popup/html/tracker.html');
const HTML_PATH_EDISCONN = chrome.runtime.getURL('popup/html/error_disconnected.html');

const POPUP_STATE_MENU = 'menu';
const POPUP_STATE_CONNT = 'connect';
const POPUP_STATE_CONNR = 'connecting';
const POPUP_STATE_CONNF = 'connection_failed';
const POPUP_STATE_EDISCONN = 'error_disconnected';
const POPUP_STATE_TRACK = 'tracker';
var popupPage = null;

var showWarnings = false;
var confirmRemoval = true;
chrome.storage.sync.get(['show_warnings', 'confirm_removal'], function(e){
    if (e['show_warnings'] !== undefined) { showWarnings = e['show_warnings']; }
    if (e['confirm_removal'] !== undefined) { confirmRemoval = e['confirm_removal']; }
});



///////////////////////////////////////////////////////////////////////////////
// POPUP INITIALIZATION ///////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////

// connect to the background page that handles job requests and states
var port = chrome.runtime.connect({ name: PORT_NAME });
port.postMessage({action: 'get_connection_state'});
// port.postMessage({action: 'get_viewer_state'});

// initialize the popup's button callbacks and query the background state
document.addEventListener('DOMContentLoaded', function() { open_correct_page({'todo': 'query'}) });

// figure out which page we should open 
function open_correct_page(data) {
    if (data['todo'] == 'query') {
        port.postMessage({action: 'get_connection_state'});
        port.postMessage({action: 'get_popup_state'});
    }
    else if (data['todo'] == POPUP_STATE_MENU) {
        open_menu();
    }
    else if (data['todo'] == POPUP_STATE_CONNT) {
        open_connecting(true);
    }
    else if (data['todo'] == POPUP_STATE_CONNR) {
        open_connecting(false);
    }
    else if (data['todo'] == POPUP_STATE_CONNF) {
        open_failure(data['message']);
    }
    else if (data['todo'] == POPUP_STATE_TRACK) {
        open_tracker();
    }
    else if (data['todo'] == POPUP_STATE_EDISCONN) {
        open_disconnect_error(data['message']);
    }
    else {
        alert('Unsure which page to open. Opening menu.');
        open_menu();
    }
}

// open the main menu
function open_menu() {
    $('#contents').load([HTML_PATH_MENU, '#contents'].join(' '), function() {
        port.postMessage({action: 'set_popup_state', state: POPUP_STATE_MENU});
        startButton = document.getElementById(HTML_START);
        if (connected) { startButton.innerHTML = 'Resume'; }
        else { startButton.innerHTML = 'Start'; }
        startButton.addEventListener('click', menu_start);
        document.getElementById(HTML_OPTIONS).addEventListener('click', menu_options);
        document.getElementById(HTML_FORUM).addEventListener('click', menu_forum);
        document.getElementById(HTML_WEBSITE).addEventListener('click', menu_mainsite);
    });
}


// open the connecting page
function open_connecting(doConnect, msg) {
    $('#contents').load([HTML_PATH_CONN, '#contents'].join(' '), function() {
        port.postMessage({action: 'set_popup_state', state: POPUP_STATE_CONNR});
        if (doConnect) {port.postMessage({action: 'connect'}); }
        if (msg) {
            var newMessage = document.createElement('p');
            newMessage.innerText = msg;
            document.getElementById('connection_message').appendChild(newMessage);
        }
    });
}


// open the failed-to-connect page
function open_failure(msg) {
    $('#contents').load([HTML_PATH_CFAIL, '#contents'].join(' '), function() {
        port.postMessage({action: 'set_popup_state', state: POPUP_STATE_CONNF});
        document.getElementById('failed_message').innerHTML = msg;
        setTimeout(open_menu, 3000);
    });
}


// open the server disconnected unexepectedly page
function open_disconnect_error(msg) {
    $('#contents').load([HTML_PATH_EDISCONN, '#contents'].join(' '), function() {
        port.postMessage({action: 'set_popup_state', state: POPUP_STATE_EDISCONN});
        document.getElementById('disconnect_message').innerHTML = msg;
        setTimeout(open_menu, 5000);
    });
}


// open the job tracker page
function open_tracker() {
    $('#contents').load([HTML_PATH_TRACK, '#contents'].join(' '), function() {
        
        // update the popup page status
        port.postMessage({action: 'set_popup_state', state: POPUP_STATE_TRACK});
        
        // add callbacks for links
        var requestButton = document.getElementById(HTML_REQUEST);
        requestButton.addEventListener('click', make_request);
        
        var mainMenuButton = document.getElementById(HTML_TMM);
        mainMenuButton.addEventListener('click', open_menu);
        
        // disconnect callback depends on connection status
        var disconnectButton = document.getElementById(HTML_TD);
        if (connected) {
            disconnectButton.innerText = 'Go Offline';
            disconnectButton.addEventListener('click', user_disconnect);
        }
        else {
            disconnectButton.innerText = 'Go Online';
            disconnectButton.addEventListener('click', function() {
                open_correct_page({'todo': POPUP_STATE_CONNT})
            });
        }
        
        // make sure the tracker state is properly set
        port.postMessage({ action: 'get_tracker_state' });
    });
}


// connect to the server and move to the tracker page
function menu_start() {
    if (connected) {
        open_correct_page({'todo': POPUP_STATE_TRACK});
    }
    else {
        open_correct_page({'todo': POPUP_STATE_CONNT});
    }
}


// open user options page for changing options
function menu_options() {
    chrome.runtime.openOptionsPage();
}


// navigate to github and/or bug/forum/requests
function menu_forum() {
    window.open('https://github.com/austinmilt/video-game-view', '_blank');
}


// navigate to main website
function menu_mainsite() {
    window.open('https://www.videogameview.com', '_blank');
}



///////////////////////////////////////////////////////////////////////////////
// COMMUNICATING WITH SERVER COMM CLIENT //////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////

// ask the background page to open a socket to the server
function connect() {
    port.postMessage({action: 'connect'});
}


// user disconnect from server
function user_disconnect() {
    port.postMessage({action: 'user_disconnect'});
}


// user delete job from trackers
function remove_job(jobID) {
    var confirmed = true;
    if (confirmRemoval) {
        confirmed = confirm('Are you sure you want to remove this job and lose the results? (You can turn off this confirmation in options.)');
    }
    if (confirmed) {
        port.postMessage({action: 'remove_job', job:jobID});
    }
}


// send a request to the background page to send to the server
function make_request() {
    return new Promise(function(resolve, reject) {
        inject_js('popup/scripts/get_request_data.js')
            .then(function() {
                chrome.tabs.query({active: true, currentWindow: true}, function(tabs) {
                    chrome.tabs.sendMessage(tabs[0].id, {trigger: 'get_request_data'}, function(data) {
                        var e = chrome.runtime.lastError;
                        if (e){ alert('Failed to query request script with error: ' + e.message); }
                        if (data['type'] == 'error') {
                            alert(data['message']);
                        }
                        else if (data['type'] == 'result') {
                            port.postMessage({
                                'action': 'request', 'video': data['video'], 
                                'replays': data['replays'], 'title': data['title']
                            });
                        }
                        else {
                            alert('Issue getting request data. Inform the developer.');
                        }
                    });
                });
            })
            .then(function(){resolve();});
    });
}


// refresh or initialize the html in the popup to show the background state
function refresh_all(backgroundState) {
    connected = backgroundState['connected'];
    if (connected) { socket_opened(); }
    else { socket_closed(); }
    update_general(backgroundState['general']['message'], backgroundState['general']['message_type'])
    document.getElementById(HTML_MSGREQ).innerHTML = '';
    for (var i = 0; i < backgroundState['id_order'].length; i++) {
        var key = backgroundState['id_order'][i];
        add_tracker(
            key, backgroundState[key]['short_id'], 
            backgroundState[key]['title'], backgroundState[key]['warnings']
        );
        if (backgroundState[key]['result']) { 
            activate_result(
                key, backgroundState[key]['result'], 
                backgroundState[key]['url'], backgroundState[key]['title']
            ); 
        }
        else {
            update_tracker(
                key, backgroundState[key]['message'], 
                backgroundState[key]['message_type'], 
                backgroundState[key]['warnings'], 
            ); 
        }
    }
}


// add some html for tracking the status of a video processing request
function add_tracker(longID, shortID, title, warnings) {
    if (popupPage == POPUP_STATE_TRACK) {
        
        // add to a new paragraph
        var newTracker = document.createElement('p');
        newTracker.classList.add(...CLASS_REQPAR);
        newTracker.setAttribute('id', 't_' + longID);
        
        // create the job deletion link
        var deleteJob = document.createElement('bdi');
        deleteJob.innerText = 'X ';
        deleteJob.classList.add(...CLASS_DELJOB);
        var deleteJobTooltip = document.createElement('div');
        deleteJobTooltip.innerText = 'Cancel this job (if running) and delete the results.';
        deleteJob.tooltip = new VGVTooltip(deleteJob, newTracker, deleteJobTooltip.innerHTML);
        deleteJob.addEventListener('click', function(){ remove_job(longID); });
        
        // create the job status text
        var newJobStatus = document.createElement('bdi');
        newJobStatus.classList.add(...CLASS_JOB);
        newJobStatus.innerText = 'Job ' + shortID + ' Status: ';
        newJobStatus.setAttribute('video_title', title);
        newJobStatus.setAttribute('id', 'j_' + longID);
        
        var jobStatusTooltip = document.createElement('div');
        jobStatusTooltip.innerText = 'Status for video: ' + title;
        if ((showWarnings) && (warnings) && (warnings.length > 0)) {
            var warningList = document.createElement('ul');
            warningList.classList.add('tracker_warning_list');
            warningList.innerText = 'Non-fatal warnings:';
            for (var i = 0; i < warnings.length; i++) {
                var warningElem = document.createElement('li');
                warningElem.classList.add('request_warning');
                warningElem.innerText = warnings[i];
                warningList.appendChild(warningElem);
            }
            jobStatusTooltip.appendChild(warningList);
        }
        newJobStatus.tooltip = new VGVTooltip(newJobStatus, newTracker, jobStatusTooltip.innerHTML);
        
        // create the server message text
        var trackerMessage = document.createElement('bdi');
        trackerMessage.classList.add(...MESSAGE_CLASS[TYPE_MSG]);
        trackerMessage.setAttribute('id', longID);
        trackerMessage.tooltip = new VGVIdentityTooltip(trackerMessage, newTracker);
        
        // add to the document
        newTracker.appendChild(deleteJob);
        newTracker.appendChild(newJobStatus);
        newTracker.appendChild(trackerMessage);
        document.getElementById(HTML_MSGREQ).appendChild(newTracker);
    }
}


// update the state of a video processing tracker
function update_tracker(longID, message, messageType, warnings) {
    if (popupPage == POPUP_STATE_TRACK) {
        
        // update server messages in the tracker message
        var trackerMessage = document.getElementById(longID);
        trackerMessage.innerText = message;
        trackerMessage.classList.add(...MESSAGE_CLASS[messageType]);
        trackerMessage.tooltip.refresh();
        
        // update the job status popup
        var jobStatus = document.getElementById('j_' + longID);
        var videoTitle = jobStatus.getAttribute('video_title');
        var jobStatusTooltip = document.createElement('div');
        jobStatusTooltip.innerText = 'Status for video: ' + videoTitle;
        if ((showWarnings) && (warnings) && (warnings.length > 0)) {
            var warningList = document.createElement('ul');
            warningList.classList.add('tracker_warning_list');
            warningList.innerText = 'Non-fatal warnings:';
            for (var i = 0; i < warnings.length; i++) {
                var warningElem = document.createElement('li');
                warningElem.classList.add('request_warning');
                warningElem.innerText = warnings[i];
                warningList.appendChild(warningElem);
            }
            jobStatusTooltip.appendChild(warningList);
        }
        jobStatus.tooltip.set_html(jobStatusTooltip.innerHTML);
    }
}


// remove the tracker from the trackers list
function remove_tracker(longID) {
    if (popupPage == POPUP_STATE_TRACK) {
        var trackerMessage = document.getElementById('t_' + longID);
        for (var child of trackerMessage.childNodes) {
            if (child.hasOwnProperty('tooltip')) {
                try { child.tooltip.remove(); }
                catch (e) { console.log(e); }
            }
            child.remove();
        }
        trackerMessage.remove();
    }
}


// update the short IDs of trackers
function update_shorts(data) {
    for (var longID of Object.keys(data)) {
        var jobStatus = document.getElementById('j_' + longID);
        jobStatus.innerText = 'Job ' + data[longID] + ' Status: ';
    }
}


// update the global message that isnt for a single request
function update_general(message, messageType) {
    if (popupPage == POPUP_STATE_TRACK) {
        var htmlSection = document.getElementById(HTML_MSGGNL);
        htmlSection.innerText = message;
        htmlSection.className = MESSAGE_CLASS[messageType];
    }
}


// update the popup when the socket is successfully connected
function socket_opened() {
    if (popupPage == POPUP_STATE_TRACK) {
        document.getElementById(HTML_REQUEST).disabled = false; 
    }
    else if (popupPage == POPUP_STATE_CONNR) {
        open_correct_page({'todo': POPUP_STATE_TRACK});
    }
}


// update the popup when the socket is closed
function socket_closed(msg) {
    if (popupPage == POPUP_STATE_TRACK) {
        document.getElementById(HTML_REQUEST).disabled = true; 
    }
    open_correct_page({'todo': POPUP_STATE_EDISCONN, 'message': msg});
}


// update the popup when the socket is closed by the user
function socket_closed_by_user() {
    if (popupPage == POPUP_STATE_TRACK) {
        document.getElementById(HTML_REQUEST).disabled = true; 
        var disconnectButton = document.getElementById(HTML_TD);
        if (connected) {
            disconnectButton.innerText = 'Go Offline';
            disconnectButton.addEventListener('click', user_disconnect);
        }
        else {
            disconnectButton.innerText = 'Go Online';
            disconnectButton.addEventListener('click', function() {
                open_correct_page({'todo': POPUP_STATE_CONNT})
            });
        }
    }
    
    else if (popupPage == POPUP_STATE_MENU) {
        startButton = document.getElementById(HTML_START);
        if (connected) { startButton.innerHTML = 'Resume'; }
        else { startButton.innerHTML = 'Start'; }
    }
}


// update the popup when the socket is reconnecting
function socket_reconnecting(msg) {
    if (popupPage == POPUP_STATE_TRACK) {
        document.getElementById(HTML_REQUEST).disabled = true; 
    }
    else if ((popupPage == POPUP_STATE_CONNR) && msg) {
        var newMessage = document.createElement('p');
        newMessage.innerText = msg;
        document.getElementById('connection_message').appendChild(newMessage);
    }
}


// update the popup when the socket fails to connect
function socket_failed(msg) {
    // if (popupPage == POPUP_STATE_CONNR) {
        // open_correct_page({'todo': POPUP_STATE_CONNF, 'message': msg});
    // }
}



// give user a clickable link to view the current results
// in the current tab
function activate_result(longID, result, videoURL, videoTitle) {
    if (popupPage == POPUP_STATE_TRACK) {
        var tracker = document.getElementById(longID);
        tracker.tooltip.remove();
        tracker.innerText = 'Ready. View ' + videoTitle;
        tracker.classList.add(...MESSAGE_CLASS[TYPE_RESULT]);
        tracker.addEventListener("click", function(e){
            chrome.tabs.update({url: videoURL}, function(tab) {
                var listener = function(tabId, changeInfo, tab) {
                    if (tabId == tab.id && changeInfo.status === 'complete') {
                        chrome.tabs.onUpdated.removeListener(listener);
                        start_viewer(JSON.parse(result));
                    }
                }
                chrome.tabs.onUpdated.addListener(listener);
            });
        }, false);
    }
}


// parse messages from the background page
port.onMessage.addListener(function(msg) {
    
    // always update the popup state so we can make sure
    // to handle the incoming data correctly
    popupPage = msg.popup;
    connected = msg.connected;
    // started = msg.viewer; // DOESNT WORK BECAUSE EVERY TAB CAN HAVE A DIFFERENT VIEWER STATE
    
    // popup states
    if (msg.type == 'popup_state') {
        open_correct_page({'todo': msg.popup});
    }
    
    else if (msg.type == 'connection_state') {
        // do nothing because the connection state is sent 
        // and updated with every message and no other actions are required
    }
    
    else if (msg.type == 'viewer_state') {
        // do nothing because the viewer state is sent and
        // update with every message, and no other actions are required
    }
    
    // job tracker messages
    else if (msg.type == 'new_tracker') {
        add_tracker(msg.long_id, msg.short_id, msg.title);
    }
    
    else if (msg.type == 'update_tracker') {
        update_tracker(msg.tracker, msg.message, msg.message_type, msg.warnings);
    }
    
    else if (msg.type == 'update_general') {
        update_general(msg.message, msg.message_type);
    }
    
    else if (msg.type == 'socket_opened') {
        socket_opened();
    }
    
    else if (msg.type == 'socket_closed_by_server') {
        socket_closed(msg.message);
    }
    
    else if (msg.type == 'socket_closed_by_user') {
        socket_closed_by_user();
    }
    
    else if (msg.type == 'socket_reconnecting') {
        socket_reconnecting(msg.message);
    }
    
    else if (msg.type == 'socket_failed') {
        socket_failed(msg.message);
    }
    
    else if (msg.type == 'tracker_state') {
        refresh_all(msg.state);
    }
    
    else if (msg.type == 'result') {
        activate_result(msg.tracker, msg.message, msg.url, msg.title);
    }
    
    else if (msg.type == 'remove_tracker') {
        remove_tracker(msg.tracker);
    }
    
    else if (msg.type == 'update_shorts') {
        update_shorts(msg.data);
    }
    
    else { console.log(msg); }
});




///////////////////////////////////////////////////////////////////////////////
// VIEWER MANAGEMENT //////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////


// general functions for injecting css and javascript with promises
function inject_js(jsPath) {
    return new Promise(function(resolve, reject) {
        chrome.tabs.query({active: true, currentWindow: true}, function(tabs) {
            chrome.tabs.executeScript(tabs[0].id, {file: jsPath}, function(result) {
                var e = chrome.runtime.lastError;
                if (e){ alert('Javascript injection error with message: ' + e.message); }
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
                if (e){ alert('CSS injection error with message: ' + e.message); }
                resolve();
            });
        });
    });
}


// function to inject scripts into activated tab
function inject_scripts() {
    return new Promise(function(resolve, reject) {
        inject_css('popup/scripts/tooltip.css')
        .then(function(){inject_css('page/scripts/tooltip_manager.css');})
        .then(function(){inject_js('page/scripts/jquery-3.2.1.min.js');})
        .then(function(){inject_js('page/scripts/jquery-ui.js');})
        .then(function(){inject_js('page/scripts/master.js');})
        .then(function(){inject_js('page/scripts/dotapedia.js');})
        .then(function(){inject_js('page/scripts/hero_timer.js');})
        .then(function(){inject_js('page/scripts/game_unit.js');})
        .then(function(){inject_js('popup/scripts/tooltip.js');})
        .then(function(){inject_js('page/scripts/tooltip_manager.js')})
        .then(function(){resolve();});
    });
}


// activate scripts
function activate(msg) {
    return new Promise(function(resolve, reject) {
        chrome.tabs.query({active: true, currentWindow: true}, function(tabs) {
            chrome.tabs.sendMessage(tabs[0].id, msg, function (response) {
                var e = chrome.runtime.lastError;
                if (e){ alert('Activation error with mesage: ' + e.message); }
                resolve();
            }) 
        })
    });
}


// starts the program
function start_viewer(data) {
    if (!started) {
        started = true;
        port.postMessage({action: 'set_viewer_state', started: true});
        inject_scripts()
            .then(function(){activate({trigger: 'activate_master', data: data})})
            .then(function(){activate({trigger: 'activate_tooltips'})})
            .then(function(v) {console.log(v)}, null);
    }   
}


// stops the program and clears changes
