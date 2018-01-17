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
// CONSTANTS //////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////
var youtubeVideo;
var videoControls;
var youtubePlayer;
var hero = {'hero': null};
var vgvContainer = null;

// user options
var refreshInterval = 1000.0; // milliseconds
var showBorders = false;
chrome.storage.sync.get(['tooltip_interval', 'show_boxes'], function(e){
    if (e['tooltip_interval'] !== undefined) { refreshInterval = e['tooltip_interval'] * 1000.0; }
    if (e['show_boxes'] !== undefined) { showBorders = e['show_boxes']; }
});
chrome.storage.onChanged.addListener(function(changes, namespace) {
    if (namespace == 'storage') {
        for (key in changes) {
            if (key == 'tooltip_interval') { refreshInterval = changes[key].newValue; }
            else if (key == 'show_boxes') { showBorders = changes[key].newValue; }
        }
    }
});

var refreshID;
var URL = window.location.href;
var controlsParent;
var ABILITY_CLASS = {
    4: {
        0: 'ability s-4 s-4-0', 1: 'ability s-4 s-4-1', 2: 'ability s-4 s-4-2',
        3: 'ability s-4 s-4-3'
    },
    5: {
        0: 'ability s-5 s-5-0', 1: 'ability s-5 s-5-1', 2: 'ability s-5 s-5-2',
        3: 'ability s-5 s-5-3', 4: 'ability s-5 s-5-4'
    },
    6: {
        0: 'ability s-6 s-6-0', 1: 'ability s-6 s-6-1', 2: 'ability s-6 s-6-2',
        3: 'ability s-6 s-6-3', 4: 'ability s-6 s-6-4', 5: 'ability s-6 s-6-5'
    }
}
var ITEM_CLASS = {
    4: {
        0: 'ability i-4 i-4-0', 1: 'ability i-4 i-4-1', 2: 'ability i-4 i-4-2', 
        3: 'ability i-4 i-4-3', 4: 'ability i-4 i-4-4', 5: 'ability i-4 i-4-5',
        6: 'ability b-4 b-4-0', 7: 'ability b-4 b-4-1', 8: 'ability b-4 b-4-2',
    },
    5: {
        0: 'ability i-5 i-5-0', 1: 'ability i-5 i-5-1', 2: 'ability i-5 i-5-2', 
        3: 'ability i-5 i-5-3', 4: 'ability i-5 i-5-4', 5: 'ability i-5 i-5-5',
        6: 'ability b-5 b-5-0', 7: 'ability b-5 b-5-1', 8: 'ability b-5 b-5-2',
    },
    6: {
        0: 'ability i-6 i-6-0', 1: 'ability i-6 i-6-1', 2: 'ability i-6 i-6-2', 
        3: 'ability i-6 i-6-3', 4: 'ability i-6 i-6-4', 5: 'ability i-6 i-6-5',
        6: 'ability b-6 b-6-0', 7: 'ability b-6 b-6-1', 8: 'ability b-6 b-6-2',
    }
}

var TALENT_CLASS = {
    4: 'ability talent t-4', 5: 'ability talent t-5', 6: 'ability talent t-6'
}



///////////////////////////////////////////////////////////////////////////////
// UTILITIES //////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////



///////////////////////////////////////////////////////////////////////////////
// INITIALIZATION /////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////

function activate_tooltips() {
    youtubeVideo = document.getElementsByClassName('video-stream')[0];
    youtubePlayer = document.getElementById('movie_player');
    vgvContainer = document.createElement('div');
    vgvContainer.classList.add('vgv-container');
    youtubePlayer.appendChild(vgvContainer);
    document.getElementsByClassName('ytp-chrome-bottom')[0].style.zIndex = '100';
    refreshID = setInterval(create_ability_divs, refreshInterval);
}


///////////////////////////////////////////////////////////////////////////////
// FUNCTIONS //////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////

// update the positioning of the container div
function update_container() {
    var vbox = youtubeVideo.getBoundingClientRect();
    vgvContainer.style.left = youtubeVideo.offsetLeft + 'px';
    vgvContainer.style.top = youtubeVideo.offsetTop = 'px';
    vgvContainer.style.width = vbox.width + 'px';
    vgvContainer.style.height = vbox.height + 'px';
}


// function for creating a single ability div with known attributes
var tooltips = new Set();
var divs = new Set();
function create_ability_div(abilityClass, id) {
    var abilityDiv = document.createElement('div');
    divs.add(abilityDiv);
    abilityDiv.className = abilityClass;
    abilityDiv.setAttribute('abilityname', id);
    abilityDiv.setAttribute('display', 'inline');
    abilityDiv.setAttribute('title', '');
    if (showBorders) { abilityDiv.style.borderStyle = 'solid'; }
    else { abilityDiv.style.borderStyle = 'none'; }
    vgvContainer.appendChild(abilityDiv);
    
    // make the tooltip
    var divClasses = ['ability-tooltip'];
    var html;
    if (abilityClass.includes('talent')) {
        divClasses.push('talent-tooltip');
        html = TOOLTIPS.html_talents(id.split(' '));
    }
    else {
        html = TOOLTIPS.html(id);
    }
    if (!html.trim()) { html = 'Empty slot.'; }
    var tooltip = new VGVTooltip(
        abilityDiv, vgvContainer, html, divClasses, 
        {'by': 'element', 'direction': 'north'}, true
    );
    abilityDiv.addEventListener('mouseenter', function() { close_tooltips_but(tooltip); })
    tooltips.add(tooltip);
}


// close tooltips that are open except a focal one
function close_tooltips_but(tt) {
    for (var tooltip of tooltips) {
        if ((tooltip !== tt) && (tooltip.visible)) { 
            tooltip.stuck = false;
            tooltip.hide();
        }
    }
}


// function for creating ability divs
function create_ability_divs() {
    
    // check that the user hasnt gone to a new webpage and thus we need
    //  to stop (for now)
    if (window.location.href != URL) {
        URL = window.location.href
        stop();
    }
    
    // update the vgvg container
    update_container();
    
    // only update the ability divs if the hero has changed from the
    // last check
    var videoTime = youtubeVideo.currentTime;
    var newHero = TIMER.get_hero(videoTime);
    if (!(newHero.hero == hero.hero)) {
    
        // reset tooltip space
        hero = newHero;
        clear_ability_divs();
        // console.log('' + videoTime + ' ' + hero.hero + ' ' + hero.abilities + ' ' + hero.items + ' ' + hero.talents);
        
        // make ability tooltips
        var abilityClass;
        for (var i = 0; i < hero.abilities.length; i++) {
            abilityClass = ABILITY_CLASS[hero.abilities.length][i];
            create_ability_div(abilityClass, hero.abilities[i]);
        }
        
        // make item tooltips
        var itemClass;
        for (i = 0; i < hero.items.length; i++) {
            if (ITEM_CLASS[hero.abilities.length].hasOwnProperty(i)) { // skipping stash items for now
                itemClass = ITEM_CLASS[hero.abilities.length][i];
                create_ability_div(itemClass, hero.items[i]);
            }
        }
        
        // make talent tooltip
        var talentClass = TALENT_CLASS[hero.abilities.length];
        create_ability_div(talentClass, hero.talents.join(' '));
    }
}

    
    
// function for clearing ability divs in abilities container
function clear_ability_divs() {
    for (var tt of tooltips) { tt.remove(); tooltips.delete(tt); }
    for (var div of divs) { div.remove(); divs.delete(div); }
    while (vgvContainer.lastChild) { vgvContainer.remove(vgvContainer.lastChild); }
}


// function for destroying all tooltip content and stopping the scripts
function deactivate_tooltips() {
    clear_ability_divs();
    clearInterval(refreshID);
    controlsParent.appendChild(videoControls);
}


// listener for activation/deactivation
chrome.runtime.onMessage.addListener(
    function(request, sender, sendResponse) {
        if (request.trigger == 'activate_tooltips') {
            activate_tooltips();
            sendResponse('tooltips activating');
        }
        else if (request.trigger == 'deactivate_tooltips') {
            deactivate_tooltips();
            sendResponse('tooltips deactivating');
        }
    }
);
