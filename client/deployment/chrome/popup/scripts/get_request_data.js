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
 * @file This script gets video details necessary for the server to properly
 * process a video. Gets things like the video URL, replay URLs, and checks
 * that necessary information is present.
 * @author [Austin Milt]{@link https://github.com/austinmilt}
*/

(function() {
    
/**
 * @description DOM element ID of the video description
 * @constant
 * @default
*/
const GRU_DESCRIPTION = 'description';

/**
 * @description DOM element class name of the page manager which has the video id as a tag
 * @constant
 * @default
*/
const GRU_VIDEOIDCLASS = 'ytd-page-manager';

/**
 * @description tag name of the unique video id (as found in the URL)
 * @constant
 * @default
*/
const GRU_VIDEOIDTAG = 'video-id';

/**
 * @description tag for VGV info in the video description
 * @constant
 * @default
*/
const GRU_FLAG = '@videogameview';

/**
 * @description DOM element class of the video title
 * @constant
 * @default
*/
const GRU_TITLECLASS = 'title ytd-video-primary-info-renderer';
const VALID_DOMAINS = ['youtube.com', 'youtu.be'];
const REQUIRE_REPLAYS = true;
const RE_BLANK = new RegExp('\s');


/** Custom error for problems with parsing the replay URLs.*/
function InvalidReplayURL(message) {
    this.name = 'InvalidReplayURL';
    this.message = message;
    this.stack = (new Error()).stack;
}
InvalidReplayURL.prototype = new Error;


/** Custom error for not being able to find the [flag]{@link GRU_FLAG} for replay data.*/
function MissingFlagError(message) {
    this.name = 'MissingFlagError';
    this.message = message;
    this.stack = (new Error()).stack;
}
MissingFlagError.prototype = new Error;


/** Custom error for not being able to find the [video description]{@link GRU_DESCRIPTION}*/
function InvalidDescriptionError(message) {
    this.name = 'InvalidDescriptionError';
    this.message = message;
    this.stack = (new Error()).stack;
}
InvalidDescriptionError.prototype = new Error;


/** Custom error for the page not being a youtube video.*/
function NotYoutubeError(message) {
    this.name = 'NotYoutubeError';
    this.message = message;
    this.stack = (new Error()).stack;
}
NotYoutubeError.prototype = new Error;


/** @return {boolean} true if the node is evaluated as a URL*/
function is_url(node) {
    if (typeof node.hasAttribute == 'function') {
        if (node.hasAttribute('href')) { return true; }
    }
    return false;
}


/** @return {string} URL from a node which may just be a hyperlink*/
function get_replay_url(node) {
    if (is_url(node)) {
        try { return (new URL(unescape(node.href))).searchParams.get('q'); }
        catch (e) { throw new InvalidReplayURL('Invalid replay url.'); }
    }
    else { return node.textContent.trim(); }
}


/** @return {string[][]} [[replay_1_start, replay_1_url], [replay_2_start, replay_2_url]...]*/
function get_replay_urls() {
    var elDescription = document.getElementById(GRU_DESCRIPTION);
    var line;
    var flag = false;
    var urls = [];
    var time;
    var url;
    var sline;
    var nextData = 'time';
    for (var child of elDescription.childNodes) {
        var text = child.textContent.trim();
        
        // skip empty lines
        if (!text) { continue; }
        
        // if the current line is the start/end of the relevant section
        if (text.toLowerCase().startsWith(GRU_FLAG)) {
        
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
                if (nextData == 'time') {
                    startTime = text;
                    nextData = 'replay_url';
                }
                else if (nextData == 'replay_url') {
                    url = get_replay_url(child); // also handles match IDs
                    urls.push([startTime, url]);
                    nextData = 'time';
                }
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


/** @return {string} URL of the video to be processed*/
function get_video_url(url) {
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


/** @return {string} video title*/
function get_video_title() {
    return document.getElementsByClassName(GRU_TITLECLASS)[0].innerText;
}


/** @return {string} youtube video ID*/
function get_video_id() {
    return document.getElementsByClassName(GRU_VIDEOIDCLASS)[0].getAttribute(GRU_VIDEOIDTAG);
}


/** @return {object} data necessary for submitting a request to the server*/
function get_request_data() {
    var video = get_video_url();
    var title = get_video_title();
    var videoID = get_video_id();
    var data = {};
    data['video'] = video;
    data['title'] = title;
    data['id'] = videoID;
    try {
        data['replays'] = get_replay_urls(); 
        if (data['replays'].length == 0) { data['replays'] = null; }
    }
    catch (e) {
        if ((e.name != 'InvalidDescriptionError') && (e.name != 'MissingFlagError')) {
            throw e;
        }
    }

    return {'type': 'result', 'data': data};
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

})();

