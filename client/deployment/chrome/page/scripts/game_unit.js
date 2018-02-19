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
var DSC_CDN_KWD_IMG = chrome.runtime.getURL('page/assets/cooldown.png');
var DSC_MAN_KWD_IMG = chrome.runtime.getURL('page/assets/mana.png');
var DSC_CDN_IMG_SIZ = '15';
var DSC_MAN_IMG_SIZ = '15';

var DPE_KWD_ABL_DSC = 'description';
var DPE_KWD_ABL_DET = 'details';
var DPE_KWD_ABL_LOR = 'lore';
var DPE_KWD_ABL_NAM = 'name';
var DPE_KWD_ABL_NOT = 'notes';
var DPE_KWD_ABL_SCP = 'scepter';
var DPE_KWD_ABL_MOD = 'scepter_mods';
var DPE_KWD_ABL_CST = 'cost';
var DPE_KWD_ABL_COO = 'cooldown';
var DPE_KWD_ABL_MAN = 'mana';
var DPE_KWD_ABL_ICO = 'icon';



///////////////////////////////////////////////////////////////////////////////
// UTILITIES //////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////
// string substitution with placeholders
// taken from http://stackoverflow.com/questions/18405736/is-there-a-c-sharp-string-format-equivalent-in-javascript
if (!String.prototype.format) {
  String.prototype.format = function() {
    var args = arguments;
    return this.replace(/{(\d+)}/g, function(match, number) { 
      return typeof args[number] != 'undefined'
        ? args[number]
        : match
      ;
    });
  };
}


// make strings iterable
if (!String.prototype[Symbol.iterator]) {
    String.prototype[Symbol.iterator] = function* () {
        for (var i = 0; i < this.length; i++) {
            yield this.charAt(i);
        }
    }
}



///////////////////////////////////////////////////////////////////////////////
// UNIT ABILITIES /////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////
class BaseDescriptor {
    
    constructor(text='') {
        this.text = text.replace(new RegExp("\\\\n", 'g'), ' ');
        this.text = this.text.replace(new RegExp("<h1>", 'g'), '');
        this.text = this.text.replace(new RegExp("</h1>", 'g'), '<br>');
    }
    
    // function to pre-format strings of numbers, i.e. that change with the
    //  level of the hero/item
    preformat_levelnums(string) {
    
        var split = string.split(' ');
        
        // simple test for strings that are definitely not strings of nums
        if ((split.length == 1) || isNaN(split[0])) {
            return string;
        }
        
        // test for it being all numbers and whether they are all the same
        var num = +split[0];
        var allnums = true;
        var allsame = true;
        var result;
        for (var i = 1; i < split.length; i++) {
            if (isNaN(split[i])) {
                allnums = false;
            }
            else if (num != (+split[i])) {
                allsame = false;
            }
        }
        
        // format the string according to the above tests
        if (allnums) {
            if (allsame) {
                result = '' + num;
            }
            else {
                result = split.join(' / ');
            }
        }
        
        else {
            result = string;
        }
        return result;
    }
    
    html() {
        var output = "{0}".format(this.text);
        return output;
    }    
}



class DescriptionDescriptor extends BaseDescriptor {}
class ScepterDescriptor extends BaseDescriptor {}
class CostDescriptor extends BaseDescriptor {}


class LoreDescriptor extends BaseDescriptor {
    
    html() {
        var output = "<span class='lore'>{0}</span>".format(this.text);
        return output;
    }  

}



class NoteDescriptor extends BaseDescriptor {
    
    constructor(details=[]) {
        super();
        this.details = [];
        for (let detail of details) {
            this.push(detail);
        }
    }
    
    push(detail) {
        this.details.push(detail);
    }
    
    html() {
        var output = '<div class="extra-desc">';
        for (let detail of this.details) {
            output += '<p>{0}</p>'.format(detail);
        }
        
        output += '</div>';
        return output;
    }
    
}



class NameDescriptor extends BaseDescriptor {
    
    constructor(name='', icon='') {
        super();
        this.name = name;
        this.icon = icon;
    }
    
    html() {
        if (this.icon != '') { return '<img src="{0}" style="height:5rem;width:auto;float:left;padding-right:0.5rem;">  {1}'.format(this.icon, this.name); }
        else { return '{0}'.format(this.name) ; }
    }
}



class DetailDescriptor extends BaseDescriptor {
    
    constructor(name='', value='') {
        super();
        this.name = name;
        this.value = value;
        if (Array.isArray(this.value)) {
            this.value = this.value.join(', ');
        }
        this.value = this.preformat_levelnums(this.value);
    }
    
    
    html() {    
        var name = this.name.replace('%', '');
        var output = '{0} <span class="detail-value">{1}</span>'.format(name, this.value)
        return output;
    }
    
    
}



class DetailsDescriptor extends BaseDescriptor {
    
    constructor(details=[]) {
        super();
        this.details = [];
        this.covered = new Set();
        for (let detail of details) {
            this.push(detail);
        }
    }
    
    push(detail) {
        if (!(detail instanceof DetailDescriptor)) {
            throw 'New details must be DetailDescriptor objects.';
        }
        
        // dont duplicate details
        if (!this.covered.has(detail.name) || (detail.name == '+')) {
            this.details.push(detail);
            this.covered.add(detail.name);
        }
    }
    
    html() {
        var output = '<span class="details-list">';
        for (let detail of this.details) {
            output += '{0}<br>'.format(detail.html());
        }
        output += '</span>';
        return output;
    }
    
    /**
    * Merges two DetailsDescriptor objects into a new one. Doesnt handle duplicates.
    *
    * @param {DetailsDescriptor} other - Other DetailsDescriptor to merge with
    * this one
    * @returns {DetailsDescriptor} a new DetailsDescriptor object
    */
    merge(other) {
        var output = new DetailsDescriptor(this.details);
        for (let detail of other.details) {
            output.push(detail);
        }
        return output;
    }
}



class ScepterModsDescriptor extends DetailsDescriptor {}
     
     
     
class CooldownDescriptor extends BaseDescriptor {

    constructor(text='') {
        super();
        this.text = text;
        if (Array.isArray(this.text)) {
            this.text = this.text.join(', ');
        }
        this.text = this.preformat_levelnums(this.text);
    }
    
    html() {
        var output = '';
        if (this.text != '') {
            output = '<img src="{0}" style="width:{1}px;height:{1}px;">   {2}'.format(DSC_CDN_KWD_IMG, DSC_CDN_IMG_SIZ, this.text);
        }
        return output;
    }
}
    
    
    
class ManaCostDescriptor extends BaseDescriptor {

    constructor(text='') {
        super();
        this.text = text;
        if (Array.isArray(this.text)) {
            this.text = this.text.join(', ');
        }
        this.text = this.preformat_levelnums(this.text);
    }
    
    html() {
        var output = '';
        if (this.text != '') {
            var output = '<img src="{0}" style="width:{1}px;height:{1}px;">   {2}'.format(DSC_MAN_KWD_IMG, DSC_MAN_IMG_SIZ, this.text);
        }
        return output;
    }
    
}    
    


class GameUnitAbility {

    constructor(
        id, name, description, notes, details, cooldown, mana, lore, scepter,
        mods, cost
    ) {
        this.id = id; // string (of int) ID in DOTAPEDIA
        this.name = name; // ability/item local name
        this.description = description; // description
        this.notes = notes; // bulleted/list extra description notes
        this.details = details; // detailed attribute/values
        this.cooldown = cooldown; // ability/item cooldown
        this.mana = mana; // mana cost
        this.lore = lore; // lore
        this.scepter = scepter; // scepter description
        this.mods = mods; // scepter modifications
        this.cost = cost; // cost to purchase item
    }
    
    
    html(level=null) {
        var output = '';
        output += '<span class="ability-name">{0}<hr></span>'.format(this.name.html());
        output += '{0}'.format(this.description.html());
        var next = this.notes.html();
        if (next != '') { output += next; }
        if (level !== null) { output += `LEVEL: ${level}`; }
        next = '{0}'.format(this.details.html());
        if (next != '') { output += '<br>' + next; }
        next = this.scepter.html();
        if (next != '') {output += '<br><span class="scepter-desc">Scepter: {0}</span>'.format(next)};
        next = this.mods.html();
        if (next != '') {output += '<br>{0}'.format(next)};
        next = this.cooldown.html();
        if (next != '') {output += '<br>{0}'.format(next)};
        next = this.mana.html();
        if (next != '') {output += '&emsp;&emsp;{0}'.format(next)};
        // output += '';
        // output += '<br><br>{0}'.format(this.lore.html());
        // output += '';
        output += '';
        return output;
    }
    
    
    /**
    * Converts the Dotapedia ability object into a new GameUnitAbility
    *
    * @param {object} id - ID of the unit in DOTAPEDIA
    * @returns {GameUnitAbility} GameUnitAbility from parsed object
    */
    static from_dotapedia(id) {
        var obj = DOTAPEDIA[id];
        var name = new NameDescriptor(obj[DPE_KWD_ABL_NAM], obj[DPE_KWD_ABL_ICO]);
        var description = new BaseDescriptor(obj[DPE_KWD_ABL_DSC]);
        var notes = new NoteDescriptor(obj[DPE_KWD_ABL_NOT]);
        var details = new DetailsDescriptor();
        for (var i = 0; i < obj[DPE_KWD_ABL_DET].length; i++) {
            details.push(new DetailDescriptor(obj[DPE_KWD_ABL_DET][i][0], obj[DPE_KWD_ABL_DET][i][1]));
        }
        var cooldown = new CooldownDescriptor(obj[DPE_KWD_ABL_COO]);
        var mana = new ManaCostDescriptor(obj[DPE_KWD_ABL_MAN]);
        var lore = new LoreDescriptor(obj[DPE_KWD_ABL_LOR]);
        var scepter = new ScepterDescriptor(obj[DPE_KWD_ABL_SCP]);
        var mods = new ScepterModsDescriptor();
        for (i = 0; i < obj[DPE_KWD_ABL_MOD].length; i++) {
            mods.push(new DetailDescriptor(obj[DPE_KWD_ABL_MOD][i][0], obj[DPE_KWD_ABL_MOD][i][1]));
        }
        var cost = new CostDescriptor(obj[DPE_KWD_ABL_CST]);
        var ability = new GameUnitAbility(
            id, name, description, notes, details, cooldown, mana, lore, 
            scepter, mods, cost
        );
        return ability;
    }
    
}



///////////////////////////////////////////////////////////////////////////////
// GAME UNIT LIBRARY //////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////
class GameUnitLibrary {
    
    constructor(units=[]) {
        this.units = {};
        for (let unit of units) {
            this.push(unit);
        }
    }
    
    push(unit) {
        this.units[unit.id] = unit;
    }
    
    static from_dotapedia(ids=null) {
        var library = new GameUnitLibrary();
        var unit;
        for (var id in DOTAPEDIA) {
            if (DOTAPEDIA.hasOwnProperty(id)) {
                if ((ids === null) || (ids.includes(id))) {
                    unit = GameUnitAbility.from_dotapedia(id);
                    library.push(unit);
                }
            }
        }
        return library;
    }
    
    html(id, level=null) {
        if (this.units.hasOwnProperty(id)) {
            return this.units[id].html(level);
        }
        else {
            return '';
        }
    }
    
    html_talents(ids, levels=[]) {
        var output = '<table class="talent">';
        var left;
        var right;
        var keys = [];
        for (var i = ids.length-1; i > 0; i -= 2) {
            right = '';
            left = '';
            output += '<tr>';
            if (this.units.hasOwnProperty(ids[i])) {
                left = this.units[ids[i]].name.html();
            }
            if (this.units.hasOwnProperty(ids[i-1])) {
                right = this.units[ids[i-1]].name.html();
            }
            output += `<td class="talent-left" lvl=${levels[i]}>${left}&emsp;</td>`;
            output += `<td class="talent-right" lvl=${levels[i-1]}>&emsp;${right}</td>`;
            output += '</tr>';
        }        
        output += '</table>';
        return output;
    }
}