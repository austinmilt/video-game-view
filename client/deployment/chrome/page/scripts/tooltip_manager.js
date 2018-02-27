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
 * @file Manages the VGV UI, including all DOM elements in the youtube page 
 * and their dynamic updates as the video progresses.
 * @author [Austin Milt]{@link https://github.com/austinmilt}
*/


///////////////////////////////////////////////////////////////////////////////
// CONSTANTS AND GLOBALS //////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////
var youtubeVideo;
var videoControls;
var youtubePlayer;
var focalHero = {'name': null};
var vgvContainer = null;
const ICON_STR = chrome.runtime.getURL('page/assets/str.png');
const ICON_AGI = chrome.runtime.getURL('page/assets/agi.png');
const ICON_INT = chrome.runtime.getURL('page/assets/int.png');
const ICON_DMG = chrome.runtime.getURL('page/assets/dmg.png');
const ICON_SPD = chrome.runtime.getURL('page/assets/speed.png');

// user options
var refreshInterval = 1000.0; // milliseconds
var showBorders = false;
chrome.storage.sync.get(['tooltip_interval', 'show_boxes'], function(e){
    if (e['tooltip_interval'] !== undefined) { refreshInterval = e['tooltip_interval'] * 1000.0; }
    if (e['show_boxes'] !== undefined) { showBorders = e['show_boxes']; }
});
chrome.storage.onChanged.addListener(function(changes, namespace) {
    if (namespace == 'sync') {
        for (key in changes) {
            if (key == 'tooltip_interval') { refreshInterval = changes[key].newValue; }
            else if (key == 'show_boxes') { showBorders = changes[key].newValue; }
        }
    }
});

var refreshID;
var URL = window.location.href;
var controlsParent;

// box index of abilities based on the number of hero ability slots
const ABILITY_I2I = {
    4: {0: 0, 1: 1, 2: 2, 3: null, 4: null, 5: 3},
    5: {0: 0, 1: 1, 2: 2, 3: 3, 4: null, 5: 4},
    6: {0: 0, 1: 1, 2: 2, 3: 3, 4: 4, 5: 5}
}

// style classes for AbilityBox's with different number of hero ability slots
const ABILITY_CLASS = {
    4: {
        0: ['s-4', 's-4-0'], 1: ['s-4', 's-4-1'], 2: ['s-4', 's-4-2'],
        3: ['s-4', 's-4-3']
    },
    5: {
        0: ['s-5', 's-5-0'], 1: ['s-5', 's-5-1'], 2: ['s-5', 's-5-2'],
        3: ['s-5', 's-5-3'], 4: ['s-5', 's-5-4']
    },
    6: {
        0: ['s-6', 's-6-0'], 1: ['s-6', 's-6-1'], 2: ['s-6', 's-6-2'],
        3: ['s-6', 's-6-3'], 4: ['s-6', 's-6-4'], 5: ['s-6', 's-6-5']
    }
}

// style classes for ItemBox's with different number of hero ability slots
const ITEM_CLASS = {
    4: {
        0: ['i-4', 'i-4-0'], 1: ['i-4', 'i-4-1'], 2: ['i-4', 'i-4-2'], 
        3: ['i-4', 'i-4-3'], 4: ['i-4', 'i-4-4'], 5: ['i-4', 'i-4-5'],
        6: ['b-4', 'b-4-0'], 7: ['b-4', 'b-4-1'], 8: ['b-4', 'b-4-2'],
    },
    5: {
        0: ['i-5', 'i-5-0'], 1: ['i-5', 'i-5-1'], 2: ['i-5', 'i-5-2'], 
        3: ['i-5', 'i-5-3'], 4: ['i-5', 'i-5-4'], 5: ['i-5', 'i-5-5'],
        6: ['b-5', 'b-5-0'], 7: ['b-5', 'b-5-1'], 8: ['b-5', 'b-5-2'],
    },
    6: {
        0: ['i-6', 'i-6-0'], 1: ['i-6', 'i-6-1'], 2: ['i-6', 'i-6-2'], 
        3: ['i-6', 'i-6-3'], 4: ['i-6', 'i-6-4'], 5: ['i-6', 'i-6-5'],
        6: ['b-6', 'b-6-0'], 7: ['b-6', 'b-6-1'], 8: ['b-6', 'b-6-2'],
    }
}

// style classes for TalentBox's with different number of hero ability slots
const TALENT_CLASS = {
    4: ['talent', 't-4'], 5: ['talent', 't-5'], 6: ['talent', 't-6']
}

// style classes for AvatarBox's across the top of screen
const AVATAR_CLASS = {
    0: ['avatar', 'a-0'], 1: ['avatar', 'a-1'], 2: ['avatar', 'a-2'],
    3: ['avatar', 'a-3'], 4: ['avatar', 'a-4'], 5: ['avatar', 'a-5'],
    6: ['avatar', 'a-6'], 7: ['avatar', 'a-7'], 8: ['avatar', 'a-8'],
    9: ['avatar', 'a-9']
}



///////////////////////////////////////////////////////////////////////////////
// INITIALIZATION /////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////

var tooltips = new Set();
var boxes = {};

/**
 * Activates the tooltips in the document.
 */
function activate_tooltips() {
    youtubeVideo = document.getElementsByClassName('video-stream')[0];
    youtubePlayer = document.getElementById('movie_player');
    vgvContainer = document.createElement('div');
    vgvContainer.setAttribute('id', 'vgv_container');
    vgvContainer.classList.add('vgv-container');
    youtubePlayer.appendChild(vgvContainer);
    document.getElementsByClassName('ytp-chrome-bottom')[0].style.zIndex = '100';
    initialize_boxes();
    refreshID = setInterval(update, refreshInterval);
}


/** 
 * Intializes all the HoverBox's that will display tooltip info.
 */
function initialize_boxes() {
    
    // build stuff for the focus hero
    boxes['focus'] = {};
    boxes['focus']['talents'] = new TalentBox(6, null, [], [], vgvContainer);
    boxes['focus']['abilities'] = [];
    for (var i = 0; i < 6; i++) {
        boxes['focus']['abilities'].push(new AbilityBox(i, 6, null, [], vgvContainer));
    }
    boxes['focus']['items'] = [];
    for (var i = 0; i < 9; i++) {
        boxes['focus']['items'].push(new ItemBox(i, 6, null, vgvContainer));
    }
    
    // build hero avatar boxes
    boxes['avatars'] = [];
    for (var i = 0; i < 10; i++) {
        boxes['avatars'].push(new AvatarBox(i, vgvContainer));
        // boxes['avatars'][i].element.addEventListener('click', function(event) {console.log(TIMER)});
    }
}


///////////////////////////////////////////////////////////////////////////////
// BOX ELEMENT CLASSES ////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////

/** Base class of boxes in the VGV UI that contain some tooltip info. */
class HoverBox {
    
    /**
     * Creates a HoverBox.
     * @param {DOMElement} [parent=vgvContainer] - parent element that the box should a child of
     * @param {string} [type='div'] - HTML type that this box should be
     * @param {string[]} [styles=[]] - additional CSS styles to give the box
     * @param {string} [tooltipDirection='north'] - direction the tooltip should display relative to the box
     * @param {string[]} [tooltipStyles=[]] - additional CSS styles to give the tooltip
     */
    constructor({
        parent=vgvContainer, type='div', styles=[], tooltipDirection='north',
        tooltipStyles=[]
    }) {
        
        // create the document element which will reveal a tooltip on hover
        var STYLES = ['hoverbox'];
        this.parent = parent;
        this.hovering = false;
        this.element = document.createElement(type);
        this.element.classList.add(...STYLES);
        this.extraStyles = styles;
        this.set_styles(styles);
        
        // create a placeholder tooltip
        this.tooltip = new VGVTooltip(
            this.element, vgvContainer, '', tooltipStyles, 
            {'by': 'element', 'direction': tooltipDirection}, true
        );
        this.tooltip.ttmid = parseInt(Math.random()*10000);
        tooltips.add(this.tooltip.ttmid);
        
        // set a hover function to force other tooltips to close
        var self = this;
        this.element.addEventListener('mouseenter', function() {
            close_tooltips_but(self.tooltip);
            self.hovering = true;
        }); 
        this.element.addEventListener('mouseleave', function() {
            self.hovering = false;
        }); 
                    
        // activate the box
        this.showing = false;
        this.active = false;
        this.activate();
    }
    
    /**
     * Sets optional CSS styles for the box.
     * @param {string[]} [styles=[]] - additional styles for the box
     */
    set_styles(styles=[]) {
        this.element.classList.remove(...this.extraStyles);
        this.element.classList.add(...styles);
        this.extraStyles = styles;
    }
    
    /**
     * Shows the box element to the user (i.e. shows border).
     */
    show() { 
        if (!this.showing) {
            this.element.style.borderStyle = 'solid'; 
            this.showing = true;
        }
    }
    
    /**
     * Hides the box element from the user (tooltip still works).
     */
    hide() { 
        if (this.showing) {
            this.element.style.borderStyle = 'none'; 
            this.showing = false;
        }
    }
    
    /**
     * Removes the box element and its tooltip from the DOM.
     */
    remove() { this.tooltip.remove(); this.element.remove(); }
    
    /**
     * Calls the set_html method of VGVTooltip.
     */
    set_tooltip_html(html, childID=null) {
        this.tooltip.set_html(html, childID);
    }
    
    /**
     * Updates the tooltip html based on class specific conditions
     * (by default does nothing).
     */
    update() {}
    
    /**
     * "Activates" the box by allowing the tooltip to open and
     * optionally showing the box/
     */
    activate(alsoShow=showBorders) {
        if (!this.active) {
            if (alsoShow) { this.show(); }
            else { this.hide(); }
            this.parent.appendChild(this.element);
            this.tooltip.enable();
            this.active = true;
        }
    }
    
    /**
     * "Deactivates" the box by forcing the tooltip to hide (if open),
     * preventing it from opening on hover, and hiding the box (if showing).
     */
    deactivate() {
        if (this.active) {
            this.hide();
            this.element.remove();
            this.tooltip.disable();
            this.element.remove();
            this.active = false;
        }
    }
}


/**
 * Class for UI elements that are ability boxes.
 * @extends HoverBox
 */
class AbilityBox extends HoverBox {
    
    /**
     * Creates an AbilityBox.
     * @param {int} [position=0] - ability index in the hero's ability set
     * @param {int} [slots=4] - number of ability slots the hero has
     * @param {int} [level=0] - current level of the ability
     * @parent {DOMElement} [parent=vgvContainer] - containing element this box should belong to
     */
    constructor(position=0, slots=4, id=null, level=0, parent=vgvContainer) {
        super({
            parent:parent, styles:ABILITY_CLASS[slots][position],
            tooltipDirection:'north', tooltipStyles:['tooltip', 'ability-tooltip']
        });
        this.id = null;
        this.slots = null;
        this.position = null;
        this.level = null;
        this.update(slots, position, id, level, true);
    }
    
    
    /**
     * Updates position and tooltip if conditions are met.
     * @param {int} slots - see {@link AbilityBox}
     * @param {int} position - see {@link AbilityBox}
     * @param {int} [level=null] - see {@link AbilityBox}
     * @param {boolean} [force=false] - force an update regardless of detected changes
     */
    update(slots, position, id, level=null, force=false) {
        
        // update the tooltip info
        if (this.tooltip.visible || force) {
            var changed = (this.id != id) || (this.level != level);
            this.id = id;
            this.level = level;
            if (changed) {
                this.set_tooltip_html(TOOLTIPS.html(this.id, this.level));
            }
        }
        
        // update the box position
        var slotsChanged = this.slots != slots;
        var positionChanged = this.position != position;
        this.slots = slots;
        this.position = position;
        if (slotsChanged || positionChanged) {
            this.set_styles(ABILITY_CLASS[slots][this.position]);
        }
    }
}


/**
 * Class for UI elements that are item boxes.
 * @extends HoverBox
 */
class ItemBox extends HoverBox {
    
    /**
     * Creates an AbilityBox.
     * @param {int} [position=0] - item index in the hero's item set
     * @param {int} [slots=4] - number of ability (yes, ability) slots the hero has
     * @param {int} [id=null] - dotapedia item id
     * @parent {DOMElement} [parent=vgvContainer] - containing element this box should belong to
     */
    constructor(position=0, slots=4, id=null, parent=vgvContainer) {
        super({
            parent: parent, styles: ITEM_CLASS[slots][position],
            tooltipDirection: 'north', tooltipStyles: ['tooltip', 'ability-tooltip']
        });
        this.set_tooltip_html('Empty slot.');
        this.id = null;
        this.slots = null;
        this.position = position;
        this.update(slots, id, true);
    }
    
    /**
     * Updates position and tooltip if conditions are met.
     * @param {int} slots - see {@link ItemBox}
     * @param {int} [id=null] - see {@link ItemBox}
     * @param {boolean} [force=false] - force an update regardless of detected changes
     */
    update(slots, id, force=false) {
        
        // update the tooltip info
        if (this.tooltip.visible || force) {
            var idChanged = (this.id != id);
            this.id = id;
            if (idChanged) {
                var html = TOOLTIPS.html(this.id, null)
                if (!html.trim()) { html = 'Empty slot.'; }
                this.set_tooltip_html(html);
            }
        }

        // update the box position
        var slotsChanged = this.slots != slots;
        this.slots = slots;
        if (slotsChanged) {
            this.set_styles(ITEM_CLASS[slots][this.position]);
        }
    }
}


/**
 * Class for UI elements that are Talent boxes.
 * @extends HoverBox
 */
class TalentBox extends HoverBox {
    
    /**
     * Creates a TalentBox.
     * @param {int} [slots=4] - number of ability (yes, ability) slots the hero has
     * @param {string} [hero=null] - identity of the hero this is displaying info for
     * @param {int[]} [ids=[]] - array of dotapedia talent ids
     * @param {int[]} [levels=[]] - array of talent levels (0 or 1)
     * @parent {DOMElement} [parent=vgvContainer] - containing element this box should belong to
     */
    constructor(slots=4, hero=null, ids=[], levels=[], parent=vgvContainer) {
        super({
            parent: parent, styles: TALENT_CLASS[slots],
            tooltipDirection: 'north', 
            tooltipStyles: ['tooltip', 'talent-tootip']
        });
        this.hero = null;
        this.slots = null;
        this.levels = [];
        this.update(slots, hero, ids, levels, true);
    }
    
    
    /**
     * Updates position and tooltip if conditions are met.
     * @param {int} slots - see {@link TalentBox}
     * @param {string} hero - see {@link TalentBox}
     * @param {int[]} ids - see {@link TalentBox}
     * @param {int[]} levels - see {@link TalentBox}
     * @param {boolean} [force=false] - force an update regardless of detected changes
     */
    update(slots, hero, ids, levels, force=false) {
        
        // update the tooltip info
        if (this.tooltip.visible || force) {
            var changed = (this.hero != hero);
            if (!changed) {
                for (var i = 0; i < 8; i++) {
                    if (levels[i] != this.levels[i]) {
                        changed = true;
                        break;
                    }
                }
            }
            this.hero = hero;
            if (changed) {
                this.levels = levels;
                this.set_tooltip_html(TOOLTIPS.html_talents(ids, levels));
            }
        }
        
        // update the box position
        var slotsChanged = this.slots != slots;
        this.slots = slots;
        if (slotsChanged) {
            this.set_styles(TALENT_CLASS[slots]);
        }
    }
}


/**
 * Class for UI elements that are Avatar boxes. AvatarBox's are very dynamic 
 * and contain info from multiple other boxes (i.e. everything in 
 * AbilityBox, ItemBox, and TalenBox), so they are a bit more complex.
 * @extends HoverBox
 */
class AvatarBox extends HoverBox {
    
    /**
     * Creates an AvatarBox.
     * @param {int} [position=0] - hero index in the top row of hero avatars in the UI
     * @parent {DOMElement} [parent=vgvContainer] - containing element this box should belong to
     */
    constructor(position=0, parent=vgvContainer) {
        super({
            parent:parent, styles:AVATAR_CLASS[position],
            tooltipDirection:'south', tooltipStyles:['tooltip', 'avatar-tooltip']
        });
        this.hero = null;
        this.position = position;
        this.handler = this.initialize_html();
        this.ttFirstCall = true;
        var self = this;
        
        // add event listeners for child elements when the tooltip becomes visible
        this.element.addEventListener('click', function(e) {
            if (self.tooltip.visible) {
                if (self.ttFirstCall) {
                    self.ttFirstCall = false;
                    for (var objList of self.handler.get_listeners()) {
                        objList[0].removeEventListener(objList[1], objList[2]);
                        objList[0].addEventListener(objList[1], objList[2]);
                        // console.log(objList[1] + ' listener added for ' + objList[0].getAttribute('id'));
                    }
                }
            }
            else { self.ttFirstCall = true; }
        });
        
        // add event listener to remove showing child elements when the
        // user leaves the tooltip and it is not stuck
        this.element.addEventListener('mouseleave', function(e) {
            if (!self.tooltip.stuck) {
                self.handler.hide_children(); 
                self.ttFirstCall = true;
            }
        });
        
        
        // add event listener to force update if starting to hover 
        // (just makes sure that tooltip info is up to date when 
        // first viewed)
        this.element.addEventListener('mouseenter', function(e) {
            self.update(self.hero, true)
        });
    }
    
    /**
     * Updates tooltip if conditions are met.
     * @param {string} name name of the hero to update info for
     * @param {boolean} [force=false] - force an update regardless of detected changes
     */
    update(name, force=false) {
        if (name) {
            this.hero = name;
            if (this.tooltip.visible || force) {
                var videoTime = youtubeVideo.currentTime;
                var heroes = TIMER.get_all(videoTime);
                var hero = heroes.get_hero(name);
                var data = {};
                data['hero'] = name;
                data['lvl'] = hero.other['level'];
                data['hp'] = [hero.other['health_current'], hero.other['health_max'], hero.other['health_regen']];
                data['mana'] = [hero.other['mana_current'], hero.other['mana_max'], hero.other['mana_regen']];
                data['str'] = [hero.other['str_natural'], hero.other['str_total']];
                data['agi'] = [hero.other['agi_natural'], hero.other['agi_total']];
                data['int'] = [hero.other['int_natural'], hero.other['int_total']];
                data['dmg'] = [hero.other['dmg_min'], hero.other['dmg_max'], hero.other['dmg_bonus']];
                data['speed'] = hero.other['speed'];
                data['xp'] = hero.other['xp_total'];
                data['worth'] = hero.other['net_worth'];
                data['kda'] = [hero.other['kills'], hero.other['deaths'], hero.other['assists']];
                data['cs'] = [hero.other['last_hits'], hero.other['denies']];
                data['earned'] = hero.other['gold_total'];
                data['talents'] = hero.talents;
                data['abilities'] = hero.abilities;
                data['items'] = hero.items;
                var updated = this.handler.update(data, force);
                if (updated) { 
                    this.set_tooltip_html(this.handler.get_html());
                    for (var objList of this.handler.get_listeners()) {
                        objList[0].removeEventListener(objList[1], objList[2]);
                        objList[0].addEventListener(objList[1], objList[2]);
                    }
                }
            }
        }
    }
    
    /**
     * Sets up the html handler for the tooltip to facilitate dynamic updates.
     * @return {AvatarTooltipHTML} handler for updates to tooltip
     */
    initialize_html() {
        return new AvatarTooltipHTML([
            new HeroATE(), new HealthATE(), new ManaATE(),
            new LevelATE(), new ExperienceATE(), 
            new NetWorthATE(), new GoldATE(), new KDAATE(), new CSATE(), 
            new DamageATE(), new SpeedATE(), 
            new StrengthATE(), new AgilityATE(), new IntelligenceATE(),
            new TalentsATE(), new AbilitiesATE(), new ItemsATE()
        ]);
    }
    
}



///////////////////////////////////////////////////////////////////////////////
// (REALLY) DYNAMIC TOOLTIP HTML //////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////

/**
 * Base class for AvatarBox tooltip HTML items, e.g. health, mana, etc.
 */
class AvatarTooltipElement {
    
    /**
     * Constructs an AvatarTooltipElement.
     * @param {string} key - unique identifier used to handle this element in the {@link AvatarTooltipHTML}
     * @param {Object} value - display and update testing value for this element
     * @param {string[]} [styles=[]] - additional CSS styles this element should get
     * @param {string} [elementType='div'] - HTML type of the DOMElement made from this element
     */
    constructor(key, value, styles=[], elementType='div') {
        this.key = key;
        this.value = value;
        this.element = document.createElement(elementType);
        this.element.classList.add(...styles)
        this.element.innerHTML = this.handler(value);
    }
    
    /**
     * Updates the value and HTML of this element if conditions are met.
     * @param {Object} value - new value this element should take
     * @param {boolean} force - force updating regardless of detected changes
     */
    update(value, force=false) {
        var updated = this.check_update(value);
        if (updated || force) {
            this.value = value;
            this.element.innerHTML = this.handler(value);
        }
        return updated;
    }
    
    /** Checks if the value of this element has changed. */
    check_update(value) { return (this.value != value); }
    
    /** Gets the display HTML of this element.*/
    handler(value) { return String(value); }
    
    /**
     * Gets any addiitonal listeners this element needs started in order
     * for the user to interact or otherwise function.
     */
    get_listeners() { return null; }
    
    /** Attempts to remove the DOMElement from the DOM.*/
    remove() {
        try { this.element.remove(); }
        catch (e) {}
    }
}


/**
 * Hero name element.
 * @extends AvatarTooltipElement
 */
class HeroATE extends AvatarTooltipElement {
    
    /** Creates a HeroATE.*/
    constructor() { super('hero', '', ['name'], 'div'); }
    
    /**
     * Gets the display HTML for this element.
     * @param {string} value - hero name
     * @return {string} HTML of name and hero icon
     */
    handler(value) { 
        var html = '';
        var heroKey = value.replace("'", "");
        var heroInfo = DOTAPEDIA[heroKey];
        if (heroInfo !== undefined) {
            var icon = heroInfo['icon'];
            html += `<img src=${icon}>`;
        }
        html += ` ${value}`;
        
        var warn = "Data in this section is experimental. ";
        warn += "It is impossible to fully reproduce the match information ";
        warn += "at every second of the video due to limits in how accurate ";
        warn += "VGV can be at analyzing videos and combining with replay information.";
        html += ` <b title="${warn}" style="color:#d7be5b;cursor:help;text">( ! )</b>`;
        
        html += '<hr>';
        return html; 
    }
}


/**
 * Hero level element.
 * @extends AvatarTooltipElement
 */
class LevelATE extends AvatarTooltipElement {
    
    /** Creates a LevelATE.*/
    constructor() { super('lvl', 0.); }   
    
    /**
     * Gets the display HTML for this element.
     * @param {int} value - hero's current level
     * @return {string} HTML of hero's level
     */
    handler(value) { return `LEVEL: ${value}`; }
}


/**
 * Hero movement speed element.
 * @extends AvatarTooltipElement
 */
class SpeedATE extends AvatarTooltipElement {
    
    /** Creates a SpeedATE.*/
    constructor() { super('speed', 0., ['ico']); }
    
    /**
     * Gets the display HTML for this element.
     * @param {int} value - hero's movement speed
     * @return {string} HTML of hero's movement speed
     */
    handler(value) { return `<img src=${ICON_SPD}>${value} (BASE)`; }
}


/**
 * Hero experience element.
 * @extends AvatarTooltipElement
 */
class ExperienceATE extends AvatarTooltipElement {
    
    /** Creates a ExperienceATE.*/
    constructor() { super('xp', 0.); }
    
    /**
     * Gets the display HTML for this element.
     * @param {int} value - hero's total XP
     * @return {string} HTML of hero's total experience
     */
    handler(value) { return `XP: ${value}`; }
}


/**
 * Hero net worth element.
 * @extends AvatarTooltipElement
 */
class NetWorthATE extends AvatarTooltipElement {
    
    /** Creates a NetWorthATE.*/
    constructor() { super('worth', 0.); }
    
    /**
     * Gets the display HTML for this element.
     * @param {int} value - player's net worth
     * @return {string} HTML of player's net worth
     */
    handler(value) { return `NET WORTH: ${value}`; }
}


/**
 * Player KDA (kills, deaths, assists) element.
 * @extends AvatarTooltipElement
 */
class KDAATE extends AvatarTooltipElement {
    
    /** Creates a KDAATE.*/
    constructor() { super('kda', [0, 0, 0]); }

    /** @see AvatarTooltipElement */
    check_update(value) { return ((this.value[0] != value[0]) || (this.value[1] != value[1]) || (this.value[2] != value[2])); }
    
    /**
     * Gets the display HTML for this element.
     * @param {int[]} value - hero [kills, deaths, assists]
     * @return {string} HTML of player's KDA
     */
    handler(value) { return `K/D/A: ${value[0]}/${value[1]}/${value[2]}`; }
}


/**
 * Player gold earned element.
 * @extends AvatarTooltipElement
 */
class GoldATE extends AvatarTooltipElement {
    
    /** Creates a GoldATE.*/
    constructor() { super('earned', 0.); }
    
    /**
     * Gets the display HTML for this element.
     * @param {int} value - player's earned gold
     * @return {string} HTML of player's earned gold
     */
    handler(value) { return `GOLD EARNED: ${value}`; }
}


/**
 * Player CS (creep score) element.
 * @extends AvatarTooltipElement
 */
class CSATE extends AvatarTooltipElement {
    
    /** Creates a CSATE.*/
    constructor() { super('cs', [0, 0]); }
    
    /** @see AvatarTooltipElement */
    check_update(value) { return ((this.value[0] != value[0]) || (this.value[1] != value[1])); }
    
    /**
     * Gets the display HTML for this element.
     * @param {int[]} - [creep kills, creep denies]
     * @return {string} HTML of hero's CS
     */
    handler(value) { return `CREEP KILLS/DENIES: ${value[0]}/${value[1]}`; }
}


/**
 * Hero strength level element.
 * @extends AvatarTooltipElement
 */
class StrengthATE extends AvatarTooltipElement {
    
    /** Creates a StrengthATE.*/
    constructor() { super('str', [0., 0.], ['ico']); }
    
    /** @see AvatarTooltipElement */
    check_update(value) { return ((this.value[0] != value[0]) || (this.value[1] != value[1])); }
    
    /**
     * Gets the display HTML for this element.
     * @param {float[]} value - [base, total, bonus] str(ength)
     * @return {string} HTML of hero's strength
     */
    handler(value) {
        var base = Math.round(value[0]);
        var total = Math.round(value[1]);
        var bonus = total - base;
        return `<img src="${ICON_STR}">${base} + ${bonus}`;
    }
}


/**
 * Hero agility element.
 * @extends AvatarTooltipElement
 */
class AgilityATE extends AvatarTooltipElement {
    
    /** Creates a AgilityATE.*/
    constructor() { super('agi', [0., 0.], ['ico']); }
    
    /** @see AvatarTooltipElement */
    check_update(value) { return ((this.value[0] != value[0]) && (this.value[1] != value[1])); }
    
    /**
     * Gets the display HTML for this element.
     * @param {float[]} value - [base, total, bonus] agi(lity)
     * @return {string} HTML of hero's agility
     */
    handler(value) {
        var base = Math.round(value[0]);
        var total = Math.round(value[1]);
        var bonus = total - base;
        return `<img src="${ICON_AGI}">${base} + ${bonus}`;
    }
}


/**
 * Hero intelligence element.
 * @extends AvatarTooltipElement
 */
class IntelligenceATE extends AvatarTooltipElement {
    
    /** Creates a IntelligenceATE.*/
    constructor() { super('int', [0., 0.], ['ico']); }
    
    /** @see AvatarTooltipElement */
    check_update(value) { return ((this.value[0] != value[0]) || (this.value[1] != value[1])); }
    
    /**
     * Gets the display HTML for this element.
     * @param {float[]} value - [base, total, bonus] int(elligence)
     * @return {string} HTML of hero's intelligence
     */
    handler(value) {
        var base = Math.round(value[0]);
        var total = Math.round(value[1]);
        var bonus = total - base;
        return `<img src="${ICON_INT}">${base} + ${bonus}`;
    }
}


/**
 * Hero attack damage element.
 * @extends AvatarTooltipElement
 */
class DamageATE extends AvatarTooltipElement {
    
    /** Creates a DamageATE.*/
    constructor() { super('dmg', [0., 0., 0.], ['ico']); }
    
    /** @see AvatarTooltipElement */
    check_update(value) { return ((this.value[0] != value[0]) || (this.value[1] != value[1]) || (this.value[2] != value[2])); }
    
    /**
     * Gets the display HTML for this element.
     * @param {float[]} value - [min, max, bonus] damage
     * @return {string} HTML of hero damage
     */
    handler(value) {
        var min = value[0];
        var max = value[1];
        var bonus = value[2];
        return `<img src="${ICON_DMG}">[${min}-${max}] + ${bonus}`;
    }
}


/**
 * Hero health element.
 * @extends AvatarTooltipElement
 */
class HealthATE extends AvatarTooltipElement {
    
    /** Creates a HealthATE.*/
    constructor() { super('hp', [0., 0., 0.]); }
    
    /** @see AvatarTooltipElement */
    check_update(value) { return ((this.value[0] != value[0]) || (this.value[1] != value[1]) || (this.value[2] != value[2])); }
    
    /**
     * Gets the display HTML for this element.
     * @param {float[]} value - [current, max, regen] health
     * @return {string} HTML of hero's health
     */
    handler(value) {
        var cur = value[0];
        var max = value[1];
        var pct = Math.round(100*(cur/max));
        var regen = value[2];
        cur = Math.round(cur);
        max = Math.round(max);
        return `<div class="vgv_avbarct"><div class="vgv_avbarfg" style="width:${pct}%;" stat="hp"></div><div class="vgv_avbartxt">HEALTH: ${cur}/${max} (${pct}%) +${regen}</div></div>`;
    }
}


/**
 * Hero mana element.
 * @extends AvatarTooltipElement
 */
class ManaATE extends AvatarTooltipElement {
    
    /** Creates a HeroATE.*/
    constructor() { super('mana', [0., 0., 0.]); }
    
    /** @see AvatarTooltipElement */
    check_update(value) { return ((this.value[0] != value[0]) || (this.value[1] != value[1]) || (this.value[2] != value[2])); }
    
    /**
     * Gets the display HTML for this element.
     * @param {float[]} value - [current, max, regen] mana
     * @return {string} HTML of hero mana
     */
    handler(value) {
        var cur = value[0];
        var max = value[1];
        var pct = Math.round(100*(cur/max));
        var regen = value[2];
        cur = Math.round(cur);
        max = Math.round(max);
        return `<div class="vgv_avbarct"><div class="vgv_avbarfg" style="width:${pct}%;" stat="mana"></div><div class="vgv_avbartxt">MANA: ${cur}/${max} (${pct}%) +${regen}</div></div>`;
    }
}


/**
 * Base class for talents, abilities, and items within an AvatarBox's tooltip.
 * @extends AvatarTooltipElement
 */
class TAIATE extends AvatarTooltipElement {
    
    /** 
     * Creates a TAIATE.
     * @param {string} key - see {@link AvatarTooltipElement}
     * @param {string[]} [substyles=[]] - additional CSS styles to give tooltip of this element
     * @param {Object} [value=[]] - see {@link AvatarTooltipElement}
     * @param {string[]} [styles=[]] - see {@link AvatarTooltipElement}
     * @param {string} [elementType='div'] - see {@link AvatarTooltipElement}
     */
    constructor(key, substyles=[], value=[], styles=[], elementType='div') {
        super(key, value, styles, elementType);
        this.tooltip = new VGVTooltip(
            this.element, vgvContainer, '', ['tooltip'].concat(substyles), 
            {'by': 'element', 'direction': 'south'}, true
        );
        this.eid = String(parseInt(Math.random()*1e10));
        this.element.setAttribute('id', this.eid);
        this.element.classList.add('vgvavtt_clickable');
        this.element.innerHTML = this.handler(value);
        var self = this;
        this.listeners = [
            [this.eid, 'click', function(e) {
                self.tooltip._e_ = document.getElementById(self.eid);
                self.tooltip._c_ = vgvContainer;
                if (self.tooltip.visible) { self.hide(e); }
                else { self.tooltip.display(e); }
            }]
        ];
    }
    
    /**
     * @return {listener[]} listener function to display this element's sub-tooltip when
     * the text is clicked (and any other listeneres added by derivatives).
     */
    get_listeners() {
        var listeners = [];
        for (var etf of this.listeners) {
            listeners.push([document.getElementById(etf[0]), etf[1], etf[2]]);
        }
        return listeners;
    }
    
    
    /**
     * @return {Object[]} all dependents that should be hidden/removed when this is 
     * hidden or removed.
     */
    get_dependents() {
        return [this.tooltip];
    }
    
    /** Removes the sub-tooltip.*/
    remove() {
        for (var dependent of this.get_dependents()) {
            try { this.tooltip.remove(); }
            catch (e) {}
        }
    }
    
    /** Hides the subtooltip and any dependents if it's being displayed.*/
    hide() {
        for (var dependent of this.get_dependents()) {
            try { dependent.hide(); }
            catch (e) {}
        }
    }
}


/**
 * Talent element within an AvatarBox's tooltip.
 * @extends AvatarTooltipElement
 */
class TalentsATE extends TAIATE {
    
    /** Creates a TalentsATE.*/
    constructor() { super('talents', ['talent-tooltip'], []); }
    
    /** @see AvatarTooltipElement */
    check_update(value) {
        if (value.length != this.value.length) { return true; }
        for (var i = 0; i < 8; i++) {
            if (this.value[i][0] != value[i][0]) { return true; }
            if (this.value[i][1] != value[i][1]) { return true; }
        }
        return false;
    }
    
    /** @see AvatarTooltipElement*/
    update(value, force=false) {
        var updated = this.check_update(value);
        if (updated || force) {
            this.value = value;
            var ts = nested_to_separate(value);
            this.tooltip.set_html(TOOLTIPS.html_talents(ts[0], ts[1]));
        }
        return updated;
    }
    
    /**
     * Gets the display HTML for this element.
     * @param {float[]} value - ignored
     * @return {string} HTML of hero talents summary text
     */
    handler(value) {
        return 'TALENTS &#9654;';
    }
}


/**
 * Abilities element within an AvatarBox's tooltip.
 * @extends AvatarTooltipElement
 */
class AbilitiesATE extends TAIATE {
    
    /** Creates an AbilitiesATE.*/
    constructor() { 
        super('abilities', ['vgvavtt-ai']);
        
        // setup ability images and their tooltips that appear when
        // the ability text is clicked
        this.images = [];
        this.imageTooltips = [];
        this.imageListeners = [];
        var self = this;
        for (var i = 0; i < 6; i++) {
            
            // create the img placeholder for the ability
            var img = document.createElement('img');
            var id = String(parseInt(Math.random()*1e10));
            img.setAttribute('id', id);
            img.setAttribute('order', i);
            this.images.push(img);
            this.tooltip._t_.appendChild(img);
            
            // create the tooltip placeholder for the ability (shown when 
            // the img is hovered)
            var abilityTooltip = new VGVTooltip(
                img, vgvContainer, '', ['tooltip', 'ability-tooltip'],
                {'by': 'element', 'direction': 'south'}, true
            );
            this.imageTooltips.push(abilityTooltip);
            
            // define listeners for the ability tooltip to show/hide
            this.imageListeners.push([
                [id, 'mouseenter', function(e) {
                    var order = this.getAttribute('order');
                    var tooltip = self.imageTooltips[parseInt(order)];
                    tooltip._e_ = this;
                    tooltip._c_ = vgvContainer;
                    if (!tooltip.visible) { tooltip.display(e); }
                }],
                [id, 'mouseleave', function(e) {
                    var order = this.getAttribute('order');
                    var tooltip = self.imageTooltips[parseInt(order)];
                    tooltip._e_ = this;
                    tooltip._c_ = vgvContainer;
                    if (tooltip.visible) { tooltip.hide(e); }
                }]
            ]);
        }
    }
    
    /** @see AvatarTooltipElement */
    check_update(value) {
        if (value.length != this.value.length) { return true; }
        for (var i = 0; i < 6; i++) {
            if (this.value[i] && value[i]) {
                if (this.value[i][0] != value[i][0]) { return true; }
                if (this.value[i][1] != value[i][1]) { return true; }
            }
        }
        return false;
    }
    
    
    /** @see AvatarTooltipElement*/
    update(value, force=false) {
        var updated = this.check_update(value);
        if (updated || force) { 
            this.value = value; 
            for (var i = 0; i < 6; i++) {
                if (this.value[i]) {
                    this.imageTooltips[i].set_html(TOOLTIPS.html(value[i][0], value[i][1]));
                    this.images[i].style.display = 'inline-block';
                    this.images[i].src = DOTAPEDIA[value[i][0]]['icon'];
                    for (var etf of this.imageListeners[i]) {
                        var el = document.getElementById(etf[0]);
                        if (el) {
                            el.removeEventListener(etf[1], etf[2]);
                            el.addEventListener(etf[1], etf[2]);
                        }
                    }
                }
                else { this.images[i].style.display = 'none'; }
            }
        }
        return updated;
    }
    
    /**
     * Gets the display HTML for this element.
     * @param {float[]} value - ignored
     * @return {string} HTML of hero abilties summary text
     */
    handler(value) {
        return 'ABILITIES &#9654;';
    }
    
    /** @see TAIATE*/
    get_dependents() {
        return [this.tooltip].concat(this.imageTooltips);
    }
}


/**
 * Items element within an AvatarBox's tooltip.
 * @extends AvatarTooltipElement
 */
class ItemsATE extends TAIATE {
    
    /** Creates an ItemsATE.*/
    constructor() { 
        super('items', ['vgvavtt-ai']);
        
        // setup item images and their tooltips that appear when
        // the item text is clicked
        this.images = [];
        this.imageTooltips = [];
        this.imageListeners = [];
        var self = this;
        for (var i = 0; i < 9; i++) {
            
            // create the img placeholder for the item
            var img = document.createElement('img');
            var id = String(parseInt(Math.random()*1e10));
            img.setAttribute('id', id);
            img.setAttribute('order', i);
            this.images.push(img);
            this.tooltip._t_.appendChild(img);
            
            // create the tooltip placeholder for the item (shown when 
            // the img is hovered)
            var abilityTooltip = new VGVTooltip(
                img, vgvContainer, '', ['tooltip', 'ability-tooltip'],
                {'by': 'element', 'direction': 'south'}, true
            );
            this.imageTooltips.push(abilityTooltip);
            
            // define listeners for the item tooltip to show/hide
            this.imageListeners.push([
                [id, 'mouseenter', function(e) {
                    var order = this.getAttribute('order');
                    var tooltip = self.imageTooltips[parseInt(order)];
                    tooltip._e_ = this;
                    tooltip._c_ = vgvContainer;
                    if (!tooltip.visible) { tooltip.display(e); }
                }],
                [id, 'mouseleave', function(e) {
                    var order = this.getAttribute('order');
                    var tooltip = self.imageTooltips[parseInt(order)];
                    tooltip._e_ = this;
                    tooltip._c_ = vgvContainer;
                    if (tooltip.visible) { tooltip.hide(e); }
                }]
            ]);
        }
    }
    
    /** @see AvatarTooltipElement */
    check_update(value) {
        if (value.length != this.value.length) { return true; }
        for (var i = 0; i < 9; i++) {
            if (this.value[i] && value[i]) {
                if (this.value[i] != value[i]) { return true; }
            }
        }
        return false;
    }
    
    
    /** @see AvatarTooltipElement*/
    update(value, force=false) {
        var updated = this.check_update(value);
        if (updated || force) { 
            this.value = value; 
            for (var i = 0; i < 9; i++) {
                if (this.value[i]) {
                    this.imageTooltips[i].set_html(TOOLTIPS.html(value[i]));
                    this.images[i].style.display = 'inline-block';
                    this.images[i].src = DOTAPEDIA[value[i]]['icon'];
                    for (var etf of this.imageListeners[i]) {
                        try {
                            var el = document.getElementById(etf[0]);
                            el.removeEventListener(etf[1], etf[2]);
                            el.addEventListener(etf[1], etf[2]);
                        } catch (e){}
                    }
                }
                else { this.images[i].style.display = 'none'; }
            }
        }
        return updated;
    }
    
    /**
     * Gets the display HTML for this element.
     * @param {float[]} value - ignored
     * @return {string} HTML of hero items summary text
     */
    handler(value) {
        return 'ITEMS &#9654;';
    }
    
    /** @see TAIATE*/
    get_dependents() {
        return [this.tooltip].concat(this.imageTooltips);
    }
}


/** Container and update class for AvatarBox tooltip HTML. */
class AvatarTooltipHTML {
    
    /**
     * Creates an AvatarTooltipHTML.
     * <p>
     * note attributes should be AvatarTooltipElement's in the order they 
     * should appear in the output html
     * </p>
     * @param {AvatarTooltipElement[]} [elements=[]] - elements to add to this container
     */
    constructor(elements=[]) {
        this.elements = {};
        this.html = document.createElement('div');
        this.keys = new Set();
        for (var elm of elements) {
            this.keys.add(elm.key);
            this.elements[elm.key] = elm;
            this.html.appendChild(elm.element);
        }
    }
    
    
    /** @see AvatarTooltipElement*/
    update(values, force=false) {
        var updated = false;
        for (var k of Object.keys(values)) {
            if (!this.elements.hasOwnProperty(k)) { throw new Error(`Element key ${k} not found in existing attributes.`);}
            if (this.elements[k].update(values[k], force)) { updated = true; }
        }
        return updated;
    }
    
    
    /** Equivalent of handler() in {@link AvatarTooltipElement} but for this class. */
    get_html() { return this.html.innerHTML; }
    
    
    /**
     * @return {listener[]} - listeners that need to be added when the parent tooltip becomes visible.
     */
    get_listeners() {
        var listeners = [];
        for (var key of this.keys) {
            var elisteners = this.elements[key].get_listeners();
            if (elisteners) { listeners.push(...elisteners); }
        }
        return listeners;
    }
    
    
    /** Removes any child elements that have spawned since this has displayed. */
    remove() {
        for (var key of this.keys) {
            this.elements[key].remove();
        }
    }
    
    
    /** Hides any child elements produced from this handler. */
    hide_children() { 
        for (var key of this.keys) {
            try { this.elements[key].hide(); }
            catch (e) {}
        }
    }
}



///////////////////////////////////////////////////////////////////////////////
// HELPER FUNCTIONS ///////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////

/**
 * Updates the position of the VGV UI container in the DOM.
 */
function update_container() {
    var vbox = youtubeVideo.getBoundingClientRect();
    vgvContainer.style.left = youtubeVideo.offsetLeft + 'px';
    vgvContainer.style.top = youtubeVideo.offsetTop = 'px';
    vgvContainer.style.width = vbox.width + 'px';
    vgvContainer.style.height = vbox.height + 'px';
}


/**
 * Closes all tooltips but a focal one.
 * @param {int} ttid - id of the tooltip to keep open
 */
function close_tooltips_but(ttid) {
    for (var tooltip of tooltips) {
        if ((tooltip.ttmid != ttid) && (tooltip.visible)) { 
            tooltip.stuck = false;
            tooltip.hide();
        }
    }
}


/**
 * Helper function that splits talent id and levels to separate arrays.
 * @param {number[]} arr - array of talent ids and levels like [[t1, l1], [t2, l2]...]
 * @return {number[]} [[t1, t2, ...], [l1, l2, ...]]
 */
function nested_to_separate(arr) {
    var out1 = [];
    var out2 = [];
    for (var item of arr) { out1.push(item[0]); out2.push(item[1]); }
    return [out1, out2];
}


/**
 * Updates every HoverBox in the VGV UI.
 */
function update() {
    try {
        
        // check that the user hasnt gone to a new webpage and thus we need
        //  to stop (for now)
        if (window.location.href != URL) {
            URL = window.location.href
            stop();
        }
        
        // update the vgv container
        update_container();
        
        // update the boxes that display tooltip info for the focal hero
        var videoTime = youtubeVideo.currentTime;
        var focalHero = TIMER.get_focus(videoTime);
        var focalSlots = focalHero.abilities.length;
        var heroes = TIMER.get_all(videoTime);
        var talentIdAndLevels = nested_to_separate(focalHero.talents);
        boxes['focus']['talents'].update(focalSlots, focalHero.name, talentIdAndLevels[0], talentIdAndLevels[1]);
        for (var i = 0; i < 6; i++) {
            var box = boxes['focus']['abilities'][i];
            if (focalSlots == 0) {
                box.deactivate(); 
                continue;
            }
            var viewedIndex = ABILITY_I2I[focalSlots][i];
            if (viewedIndex === null) { box.deactivate(); }
            else { 
                var id = focalHero.abilities[viewedIndex][0];
                var level = focalHero.abilities[viewedIndex][1];
                box.update(focalSlots, viewedIndex, id, level); 
                box.activate();
            }
        }
        for (var i = 0; i < 9; i++) {
            var box = boxes['focus']['items'][i];
            if (focalSlots == 0 ) { box.deactivate(); }
            else {
                box.update(focalSlots, focalHero.items[i]); 
                box.activate();
            }
        }
        
        // update avatar boxes (top of screen)
        for (var name of heroes.list_heroes()) {
            var hero = heroes.get_hero(name);
            var box = boxes['avatars'][parseInt(hero.other.order)];
            if (box !== undefined) {
                box.update(name);
            }
        }
    }    
    catch (e) {
        try { clearInterval(refreshID); }
        catch (nada) {}
        throw e;
    }
}


/** Clears the contents of the VGV UI and removes them from the DOM. */
function clear_boxes() {
    for (var tt of tooltips) { tt.remove(); tooltips.delete(tt); }
    for (var box of boxes) { box.remove(); boxes.delete(box); }
    while (vgvContainer.lastChild) { vgvContainer.remove(vgvContainer.lastChild); }
}


/** Deactivates the VGV viewer.*/
function deactivate_tooltips() {
    clear_boxes();
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
        return true;
    }
);
