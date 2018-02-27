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
 * @file Manages VGV options interface and storage in chrome sync.
 * @author [Austin Milt]{@link https://github.com/austinmilt}
*/



/**
 * Gets options in the options page by their ids.
 * @return {json} - option keys and values
 */
function get_option_values() {
    var optionValueElements = document.getElementsByTagName('input');
    var output = {};
    for (var i = 0; i < optionValueElements.length; i++) {
        
        // get option attributes
        var optionElement = optionValueElements[i];
        var optionType = optionElement['type'];
        var optionID = optionElement['id'];
        
        // do appropriate casting and evaluation of user's choice
        var optionValue;
        if (optionType == 'number') {
            optionValue = Number.parseFloat(optionElement['value']);
        }
        else if (optionType == 'checkbox') {
            optionValue = optionElement['checked'];
        }    
        else {
            optionValue = optionElement['value'];
        }
        
        // add to output
        output[optionID] = optionValue;
    }
    return output;
}


// set options in the options doc by the option id
/**
 * Sets options in the options doc by their ids.
 * @param {json} options - option keys and values
 */
function set_option_values(options) {
    var keys = Object.keys(options);
    for (var i = 0; i < keys.length; i++) {
        var key = keys[i];
        var value = options[key];
        console.log('' + key + ' ' + value);
        var optionElement = document.getElementById(key);
        var optionType = optionElement['type'];
        var optionID = optionElement['id'];
        if (optionType == 'number') {
            optionElement['value'] = value;
        }
        else if (optionType == 'checkbox') {
            optionElement['checked'] = value;
        }
        else {
            optionElement['value'] = value;
        }
    }
}


/**
 * Saves current options to chrome sync storage.
 */
function save_options() {
    var optionValues = get_option_values();
    chrome.storage.sync.set(optionValues, function() {
        var notifier = document.getElementById('save_success');
        notifier.style.display = 'block';
        setTimeout(function() { notifier.style.display = 'none'; }, 2000);
    });
}


/** 
 * Restores option state using the preferences stored in chrome.storage.
 */
function restore_options() {chrome.storage.sync.get(null, set_option_values);}

// add listeners
document.addEventListener('DOMContentLoaded', restore_options);
document.getElementById('save').addEventListener('click', save_options);