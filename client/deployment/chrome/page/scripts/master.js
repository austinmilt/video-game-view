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

var TOOLTIPS;
var TIMER;

// creates the hero timer
function create_hero_timer(timerData) {
    return new Promise(function(resolve, reject) {
        TIMER = OrderedHeroTimes.from_json(timerData);
        resolve(TIMER);
    });
}


// create the tooltip library for feeding the manager
function create_tooltip_library(heroTimes) {
    return new Promise(function(resolve, reject) {
        TOOLTIPS = GameUnitLibrary.from_dotapedia(heroTimes.list_ids());
        resolve(TOOLTIPS);
    });
}



// get the replay data and then activate tooltips etc.
function activate(jobID) {
    return new Promise(function(resolve, reject) {
        chrome.runtime.sendMessage({action: 'get_result', id: jobID}, function(response) {
            if (response.type == 'result') {
                create_hero_timer(JSON.parse(pako.inflate(response.data, {to: 'string'})))
                .then(function(result) {return create_tooltip_library(result)})
                .then(resolve);
            }
            else if (response.type == 'error') {
                console.log(response.msg);
                reject(response.msg);
            }
            else {
                console.log('Unable to load results!');
                reject('Unable to load results!');
            }
        })
    });
}


// cleanup
function deactivate() {
    console.log('Nothing to deactivate.');
}


function cleanup() {
    console.log('Nothing to cleanup.');
}


// listeners
chrome.runtime.onMessage.addListener(
    function(msg, sender, sendResponse) {
        if (msg.trigger == 'activate_master') {
            activate(msg.data).then(function() { sendResponse('master activated') });
        }
        else if (msg.trigger == 'deactivate') {
            deactivate();
            cleanup();
            sendResponse('master deactivating');
        }
    }
);