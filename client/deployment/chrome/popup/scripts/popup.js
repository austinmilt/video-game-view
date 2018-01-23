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
// o make a get request on videogameview.com to search for 
//     match ids to d/l replays. If you already have the id on your server 
//     (maybe the bucket), just grab it from there. Otherwise download it 
//     from the dota 2 servers. Delete replays that are older than some age. 
//     This way you can allow content creators or users to specify match start 
//     times when a replay isnt available
// o user can rename video results link (e.g. "My pooper scooper")
// o tooltips displaying when nothing on the screen
// o client clicking on link throws problem with the hero_timer
// o put screen indicator in bottom-right of popup screen showing when the client is trying to connect (and clicking start doesnt take you to connect screen)
// o try to connect to server on start, but if it doesnt just let the user stay in the tracker menu without making requests
// o server do a better job of clearing temp files
// o function to stop tooltips
// o menu to stop tooltips
// o something wrong with client with server shuts down... when you hit Start again gets stuck in Connecting....
// o user disconnecting from server and then reconnecting doesnt work
// o sometimes when user tries to load results some scripts return before ready and causes subsequent calls to fail
// o some clients being logged out (websocket closes) when popup closes(?) dunno why
// o dont force user out of tracker menu when offline (especially when disconnected by server)
// o prevent multiple injections of content scripts
// o if client has already started viewing video results, trying to process the current video again throws an "invalid description" error
// o morphling tooltips are off positioned. True for all 6-slotted heroes?
// X have some non-interpreted newlines (\n) in your tooltips
// X background page does not reload options unless the user restarts the app totally (persistent background, dummy). So you need to make it reload each time the user saves options.
// X tooltips have %% where percents need to go.
// X some %variable% variables are not being replaced (see email from arrby). (Cant be fixed because error in dota files)
// X need to tell other tooltips when a new tooltip is to be displayed and hide them (store this in the state of the vgv-container?)
// X options menu tooltips appear in the wrong place
// X store tracker state in chrome.storage.sync and reload on opening
// X server log of activity
// X auto-reconnect websocket without canceling job on server
//////////////////////////////////////////////////////////////////////////////

var started = false;
var TIMER;
var TOOLTIPS;

const CONN_CONNECTED = 'connected';
const CONN_DISCONNECTED = 'disconnected';
const CONN_CONNECTING = 'connecting';
var connection = CONN_DISCONNECTED;

const GRU_TDEL = ':';
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
const HTML_SAMPLE = 'sample_button';
const HTML_WEBSITE = 'website_button';

const HTML_REQUEST = 'request_button';
const HTML_MSGREQ = 'request_incoming';
const HTML_MSGGNL = 'server_incoming';

const HTML_TMM = 'tracker_menu_gomenu';
const HTML_TD = 'tracker_menu_disconnect';

const HTML_PATH_MENU = chrome.runtime.getURL('popup/html/menu.html');
const HTML_PATH_TRACK = chrome.runtime.getURL('popup/html/tracker.html');
const HTML_PATH_REQFORM = chrome.runtime.getURL('popup/html/request_form.html');
const HTML_CONTENT_SELECTOR = '#contents';

const POPUP_STATE_MENU = 'menu';
const POPUP_STATE_TRACK = 'tracker';
var popupPage = null;

// user options
var showWarnings = false;
var confirmRemoval = true;
chrome.storage.sync.get(['show_warnings', 'confirm_removal'], function(e){
    if (e['show_warnings'] !== undefined) { showWarnings = e['show_warnings']; }
    if (e['confirm_removal'] !== undefined) { confirmRemoval = e['confirm_removal']; }
});
chrome.storage.onChanged.addListener(function(changes, namespace) {
    if (namespace == 'storage') {
        for (key in changes) {
            if (key == 'show_warnings') { showWarnings = changes[key].newValue; }
            else if (key == 'confirm_removal') { confirmRemoval = changes[key].newValue; }
        }
    }
});



///////////////////////////////////////////////////////////////////////////////
// POPUP INITIALIZATION ///////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////

// connect to the background page that handles job requests and states
var port = chrome.runtime.connect({ name: PORT_NAME });
port.postMessage({action: 'get_connection_state'});

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
    else if (data['todo'] == POPUP_STATE_TRACK) {
        open_tracker();
    }
    else {
        alert('Unsure which page to open. Opening menu.');
        open_menu();
    }
}

// open the main menu
function open_menu() {
    $('#contents').load([HTML_PATH_MENU, HTML_CONTENT_SELECTOR].join(' '), function() {
        popupPage = POPUP_STATE_MENU;
        port.postMessage({action: 'set_popup_state', state: POPUP_STATE_MENU});
        update_connection_dependents();
        document.getElementById(HTML_START).addEventListener('click', menu_start);
        document.getElementById(HTML_OPTIONS).addEventListener('click', menu_options);
        document.getElementById(HTML_FORUM).addEventListener('click', menu_forum);
        document.getElementById(HTML_SAMPLE).addEventListener('click', menu_sample);
        document.getElementById(HTML_WEBSITE).addEventListener('click', menu_mainsite);
    });
}


// open the job tracker page
function open_tracker() {
    $('#contents').load([HTML_PATH_TRACK, HTML_CONTENT_SELECTOR].join(' '), function() {
        
        // update the popup page status
        popupPage = POPUP_STATE_TRACK;
        port.postMessage({action: 'set_popup_state', state: POPUP_STATE_TRACK});
        
        // add callbacks for links
        var requestButton = document.getElementById(HTML_REQUEST);
        requestButton.addEventListener('click', make_request);
        var mainMenuButton = document.getElementById(HTML_TMM);
        mainMenuButton.addEventListener('click', open_menu);

        // other small updates
        update_connection_dependents();
        build_static_tooltips(document.getElementById('contents'));
        
        // make sure the tracker state is properly set
        port.postMessage({ action: 'get_tracker_state' });
    });
}


// connect to the server and move to the tracker page
function menu_start() {
    if (connection == 'disconnected') { connect(); }
    open_correct_page({'todo': POPUP_STATE_TRACK});
}


// open user options page for changing options
function menu_options() {
    chrome.runtime.openOptionsPage();
}


// navigate to github and/or bug/forum/requests
function menu_forum() {
    window.open('https://groups.google.com/forum/#!forum/video-game-view', '_blank');
}


// navigate to sample video(s)
function menu_sample() {
    window.open('https://www.youtube.com/watch?v=qG8JpKFPNdE', '_blank');
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


// custom errors for problems with parsing the video details
function InvalidTimeError(message) {
    this.name = 'InvalidTimeError';
    this.message = message;
    this.stack = (new Error()).stack;
}
InvalidTimeError.prototype = new Error;

function InvalidDescriptionError(message) {
    this.name = 'InvalidDescriptionError';
    this.message = message;
    this.stack = (new Error()).stack;
}
InvalidDescriptionError.prototype = new Error;


// parse replay start times
function parse_replay_start(time) {
    var hr;
    var min;
    var sec;
    var tsplit = time.split(GRU_TDEL);
    
    // times should always be separated by a colon
    if (tsplit.length == 2) {
        hr = '0';
        min = tsplit[0];
        sec = tsplit[1];
    }
    else if (tsplit.length == 3) {
        hr = tsplit[0];
        min = tsplit[1];
        sec = tsplit[2];
    }
    else {
        alert('Invalid replay start time in video description.');
    }
    
    // check that parts of the time are integer
    if (isNaN(hr) || (parseInt(hr) != parseFloat(hr))) {
        throw new InvalidTimeError('Invalid replay start time in video description.');
    }
    else if (isNaN(min) || (parseInt(min) != parseFloat(min)) || (parseInt(min) >= 60)) {
        throw new InvalidTimeError('Invalid replay start time in video description.');
    }
    else if (isNaN(sec) || (parseInt(sec) != parseFloat(sec)) || (parseInt(sec) >= 60)) {
        throw new InvalidTimeError('Invalid replay start time in video description.');
    }
    
    return parseFloat(hr)*3600 + parseFloat(min)*60 + parseFloat(sec);
}


// open request details form in tracker page
function get_user_request_info() {
    return new Promise(function(resolve, reject) {
        
        // open the form
        var form = document.createElement('div');
        form.setAttribute('id', 'vgv_request_form');
        form.style.display = 'none';
        var screen = document.getElementById('screen');
        screen.appendChild(form);
        var output = {};
        $('#vgv_request_form').load([HTML_PATH_REQFORM, HTML_CONTENT_SELECTOR].join(' '), function() {
            form.style.display = 'block';
            build_static_tooltips(form);
            
            // bind submit and cancel callbacks
            var submit = document.getElementById('vgv_reqform_submit');
            var ff = form;
            var oo = output;
            submit.addEventListener('click', function() {
                
                // parse the replay text
                var replayText = ff.querySelector('#vgv_reqform_replays').value;
                oo['replays'] = [];
                oo['type'] = 'result';
                for (var line of replayText.trim().split('\n')) {
                    if (!line) { continue; }
                    var replayPair = line.trim().split(' ');
                    if (replayPair.length != 2) {
                        reject(new InvalidDescriptionError('Invalid info in user-given replay data.'));
                    }
                    oo['replays'].push(replayPair);
                }
                
                // finish up
                ff.remove();
                resolve(oo);
            });
            
            var cancel = document.getElementById('vgv_reqform_cancel');
            cancel.addEventListener('click', function() {
                oo['type'] = 'canceled';
                ff.remove();
                resolve(oo);
            });
        });
    });
}


// does the actual porting of request to the background script
function post_request_to_background(requestData) {
    
    // process replay start time strings
    if (!requestData['replays']) { throw new Error('Missing required replay information.'); }
    for (var i = 0; i < requestData['replays'].length; i++) {
        requestData['replays'][i][0] = parse_replay_start(requestData['replays'][i][0]);
    }
    
    // now that we have the details, post to the background
    port.postMessage({
        'action': 'request', 'video': requestData['video'], 
        'replays': requestData['replays'], 'title': requestData['title']
    });
}


// send a request to the background page to send to the server
function make_request() {
    return new Promise(function(resolve, reject) {
        
        // get the request data for sending to the background page
        inject_js('popup/scripts/get_request_data.js')
            .then(function() {
                chrome.tabs.query({active: true, currentWindow: true}, function(tabs) {
                    chrome.tabs.sendMessage(tabs[0].id, {trigger: 'get_request_data'}, function(response) {
                        var e = chrome.runtime.lastError;
                        if (e){ alert('Failed to query request script with error: ' + e.message); }
                        if (response['type'] == 'error') {
                            alert(response['message']);
                        }
                        else if (response['type'] == 'result') {
                            
                            // if some replay info is missing, prompt the user
                            // for it
                            var data = response['data'];
                            if (!data['replays']) {
                                get_user_request_info()
                                .then(function(resolution) {
                                    if (resolution['type'] == 'result') {
                                        data['replays'] = resolution['replays'];
                                        post_request_to_background(data);
                                    }
                                })
                                .catch(function(rejection) { alert(rejection) });
                            }
                            
                            // otherwise send the request to the background
                            else {
                                post_request_to_background(data);
                            }
                        }
                        else {
                            alert('Issue getting request data. Inform the developer.');
                        }
                    });
                });
            })
            .then(function(){resolve()})
            .catch(function(rejection){ alert(rejection) });
    });
}


///////////////////////////////////////////////////////////////////////////////
// POPUP PAGE UPDATES /////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////

// fills in static tooltips when a new "page" is opened in the popup
function build_static_tooltips(outer) {
    if (!outer) { outer = document; }
    var elementsWithTooltips = outer.querySelectorAll('[' + ATTR_TT + ']');
    for (var i = 0; i < elementsWithTooltips.length; i++) {
        var element = elementsWithTooltips[i];
        var container = document.getElementById(element.getAttribute(ATTR_ADDTO));
        var ttHTML = element.getAttribute(ATTR_TT);
        var extraClasses = element.getAttribute(ATTR_CLASSES);
        new VGVTooltip(element, container, ttHTML, extraClasses);
    }
}

// make updates to pages based on the connection status (e.g. enabling disabling
// certain features)
function update_connection_dependents() {
    
    // on the tracker page, need to change
    //  o Go Online/Offline
    //  o Make request link
    if (popupPage == POPUP_STATE_TRACK) {
        var disconnectButton = document.getElementById(HTML_TD);
        var requestButton = document.getElementById(HTML_REQUEST);
        if (connection == CONN_CONNECTED) {
            requestButton.setAttribute('enabled', true);
            disconnectButton.innerText = 'Go Offline';
            disconnectButton.removeEventListener('click', connect);
            disconnectButton.addEventListener('click', user_disconnect);
            disconnectButton.setAttribute('enabled', true);
        }
        else if (connection == CONN_CONNECTING) {
            requestButton.setAttribute('enabled', false);
            disconnectButton.innerText = 'Connecting';
            disconnectButton.removeEventListener('click', user_disconnect);
            disconnectButton.removeEventListener('click', connect);
            disconnectButton.setAttribute('enabled', false);
        }
        else if (connection == CONN_DISCONNECTED) {
            requestButton.setAttribute('enabled', false);
            disconnectButton.innerText = 'Go Online';
            disconnectButton.removeEventListener('click', user_disconnect);
            disconnectButton.addEventListener('click', connect);
            disconnectButton.setAttribute('enabled', true);
        }
        else { alert(connection); }
    }
    
    
    // on the main menu, just change the Start/Resume
    else if (popupPage == POPUP_STATE_MENU) {
        var startButton = document.getElementById(HTML_START);
        if (connection == CONN_CONNECTED) { startButton.innerHTML = 'Resume'; }
        else if (connection == CONN_CONNECTING) { startButton.innerHTML = 'Resume'; }
        else if (connection == CONN_DISCONNECTED) { startButton.innerHTML = 'Start'; }
        else { alert(connection); }
    }
}

// refresh or initialize the html in the popup to show the background state
function refresh_all(backgroundState) {
    connection = backgroundState['connection'];
    if (connection == CONN_CONNECTED) { socket_opened(); }
    else if (connection == CONN_CONNECTING) { socket_reconnecting(); }
    else if (connection == CONN_DISCONNECTED) { socket_closed(); }
    update_general(backgroundState['general']['message'], backgroundState['general']['message_type']);
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


// update the popup when the socket is trying to connect
function socket_connecting() {
    update_connection_dependents();
}


// update the popup when the socket is successfully connected
function socket_opened() {
    update_connection_dependents();
}



// update the popup when the socket is closed
function socket_closed(msg) {
    update_connection_dependents();
}


// update the popup when the socket is closed by the user
function socket_closed_by_user() {
    update_connection_dependents();
}


// update the popup when the socket is reconnecting
function socket_reconnecting(msg) {
    update_connection_dependents(msg);
}


// update the status led
function set_status_led() {
    var led = document.getElementById('led_status');
    led.setAttribute('status', connection);
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
    connection = msg.connection;
    set_status_led();
    
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
    
    else if (msg.type == 'socket_connecting') {
        socket_connecting();
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
