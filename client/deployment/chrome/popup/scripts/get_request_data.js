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

const GRU_DESCRIPTION = 'description';
const GRU_FLAG = '@videogameview';
const GRU_TDEL = ':';
const GRU_TITLECLASS = 'title ytd-video-primary-info-renderer';
const VALID_DOMAINS = ['youtube.com', 'youtu.be'];
const REQUIRE_REPLAYS = true;

// custom error for problems with parsing the video details
function InvalidTimeError(message) {
    this.name = 'InvalidTimeError';
    this.message = message;
    this.stack = (new Error()).stack;
}
InvalidTimeError.prototype = new Error;


// custom error for not being able to find the flag for replay data
function MissingFlagError(message) {
    this.name = 'MissingFlagError';
    this.message = message;
    this.stack = (new Error()).stack;
}
MissingFlagError.prototype = new Error;


// custom error for invalid replay data 
function InvalidDescriptionError(message) {
    this.name = 'InvalidDescriptionError';
    this.message = message;
    this.stack = (new Error()).stack;
}
InvalidDescriptionError.prototype = new Error;


// custom error for it not being a youtube video
function NotYoutubeError(message) {
    this.name = 'NotYoutubeError';
    this.message = message;
    this.stack = (new Error()).stack;
}
NotYoutubeError.prototype = new Error;


// test for valid time for replay url
function get_replay_start(time) {
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

// function to parse replay links from video description
function get_replay_urls() {
    var description = document.getElementById(GRU_DESCRIPTION).innerText.split('\n');
    var line;
    var flag = false;
    var urls = [];
    var time;
    var url;
    var sline;
    for (var i = 0; i < description.length; i++) {
        line = description[i].trim();
        
        // if the current line is the start/end of the relevant section
        if (line.toLowerCase().startsWith(GRU_FLAG)) {
        
            // found the end, so finish
            if (flag) {
                return urls;
            }
            
            // found the beginning, so start recording
            else {
                flag = true;
            }
        }
        
        // parse line for relevant info
        else if (flag) {
            try {
                sline = line.split(' ');
                startTime = get_replay_start(sline[0]);
                url = sline[1];
                urls.push([startTime, url]);
            }
            catch (e) {
                throw new InvalidDescriptionError('Invalid info in video description.');
            }
        }
    }
    
    if (!flag && REQUIRE_REPLAYS) {
        throw new MissingFlagError('Missing required info in video description.'); 
    }
    
    
}


// function to get video url
function get_video_url() {
    try { var url = document.location.href; }
    catch (e) { throw new NotYoutubeError('Page is not a youtube page.'); }
    var valid = false;
    for (var i = 0; i < VALID_DOMAINS.length; i++) {
        if (url.includes(VALID_DOMAINS[i])) {
            valid = true;
            break;
        }
    }
    if (valid) { return url; }
    else { throw new NotYoutubeError('Page is not a youtube page.'); }
}


// function to get video title
function get_video_title() {
    return document.getElementsByClassName(GRU_TITLECLASS)[0].innerText;
}


// function to collect and organize request data
function get_request_data() {
    var video = get_video_url();
    var replays = get_replay_urls();
    var title = get_video_title();
    return {'type': 'result', 'video': video, 'replays': replays, 'title': title};
}


// listener for getting request data
chrome.runtime.onMessage.addListener(function(request, sender, sendResponse) {
    if (request.trigger == 'get_request_data') {
        var data = null;
        try { data = get_request_data(); }
        catch (e) { data = {'type': 'error', 'message': e.message }; }
        sendResponse(data);
    }
});