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
const SEP_TIMES = ':';
const SEP_CATEGORIES = ';';
const SEP_ELEMENTS = ',';
const N_TALENTS = 8;
const JSON_KEY_TIME = 'time';
const JSON_KEY_FOCUS = 'focus';
const JSON_KEY_HEROES = 'heroes';
const JSON_KEY_HERO_ABILITIES = 'abilities';
const JSON_KEY_HERO_ITEMS = 'items';


///////////////////////////////////////////////////////////////////////////////
// HELPER FUNCTIONS ///////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////
function split_abilities_talents(combined) {
    var lastTalent;
    for (lastTalent = combined.length-1; lastTalent >= 0; lastTalent--) {
        if (combined[lastTalent]) { break; }
    }
    var lastAbility = lastTalent - N_TALENTS;
    talents = combined.slice(lastAbility + 1, lastTalent + 1);
    abilities = combined.slice(0, lastAbility + 1);
    return [abilities, talents];
}



///////////////////////////////////////////////////////////////////////////////
// CLASSES ////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////

// contains info for a given hero at a given video time
class HeroTime {
    
    constructor(name, time, abilities=[], talents=[], items=[], other={}, prev=null, next=null) {
        this.name = name;
        this.time = time;
        this.abilities = abilities;
        this.talents = talents;
        this.items = items;
        this.other = other;
        this.prev = prev;
        this.next = next;
    }
    
    is_earlier_than(other) {
        if (this.time < other.time) {
            return true;
        }
        else {
            return false;
        }
    }
    
    is_later_than(other) {
        if (this.time > other.time) {
            return true;
        }
        else {
            return false;
        }
    }
    
    to_string() {
        var output = this.name + ' @ ' + this.time;
        return output
    }
}


// contains info for multiple heroes at a given time
class HeroTimeSet {
    constructor(htimes=[], focus=null, prev=null, next=null) {
        this.heroes = {};
        this.time = null;
        this.focus = null;
        for (var h of htimes) { this.add(h); }
        if (focus !== null) { this.set_focus(focus); }
        this.prev = prev;
        this.next = next;
    }
    
    // adds a HeroTime to the set
    add(hero) {
        if (this.time === null) { this.time = hero.time; }
        else if (hero.time != this.time) { throw new Error('All times in the set must match.'); }
        else if (this.heroes.hasOwnProperty(hero.name)) { throw new Error('Cannot repeat hero names in the set.'); }
        this.heroes[hero.name] = hero;
    }
    
    
    // gets the HeroTime with the given name (HeroTime.name)
    get_hero(name) {
        return this.heroes[name];
    }
    
    
    // sets the focal hero as long as it already belongs to the set
    set_focus(name) {
        if (!this.heroes.hasOwnProperty(name)) { throw new Error('Focus hero must already be in set.'); }
        else { this.focus = name; }
    }
    
    
    // gets the HeroTime of the focal hero
    get_focus() { return this.get_hero(this.focus); }
    
    
    // tests if this set is earlier than another
    is_earlier_than(other) {
        if (this.time < other.time) {
            return true;
        }
        else {
            return false;
        }
    }
    
    
    // tests if this set is later than another
    is_later_than(other) {
        if (this.time > other.time) {
            return true;
        }
        else {
            return false;
        }
    }
    
    
    // lists all the heroes in this set
    list_heroes() { return Object.keys(this.heroes); }
}



// contains info for multiple heroes at multiple times
class OrderedHeroTimes {
    
    // NOTE: This assumes times are provided in order (no sorting necessary)
    constructor(times=[]) {
        this.times = [];
        for (var time in times) {this.push[time];}
        this.length = times.length;
        this.first = null;
        this.last = null;
        if (times.length > 0) {
            this.first = this.times[0];
            this.last = this.times[this.length-1];
        }
    }
    
    
    // adds a new HeroTimeSet to the list
    push(time) {
    
        // add the first element
        if (this.length == 0) {
            this.times.push(time);
            this.first = time;
            this.last = time;
            this.length += 1;
        }
        
        // throw if the new time is too early
        else if (time.is_earlier_than(this.last)) {
            throw 'New time must be later than latest time.';
        }
        
        // add a new last element
        else {
            this.last.next = time;
            time.prev = this.last;
            this.times.push(time);
            this.last = time;
            this.length += 1;
        }
    }
    
    
    // gets the focus HeroTime at the given time
    get_focus(time) {
        var cur = this.first;
        while ((!(cur.next === null)) && (cur.next.time < time)) {
            cur = cur.next;
        }
        return cur.get_focus();
    }
    
    
    // gets the HeroTimeSet at the given time
    get_all(time) {
        var cur = this.first;
        while ((!(cur.next === null)) && (cur.next.time < time)) {
            cur = cur.next;
        }
        return cur;
    }
    
    
    // lists the names of all covered heroes
    list_heroes() {
        var heroList = new Set();
        var cur = this.first;
        while (!(cur.next === null)) {
            for (var name of cur.list_heroes()) {
                heroList.add(name);
            }
            cur = cur.next;
        }
        return Array.from(heroList);
    }
    

    // lists the dotapedia IDs of all covered heroes
    list_ids() {
        var idSet = new Set([]);
        var cur = this.first;
        while (!(cur.next === null)) {
            for (var name of cur.list_heroes()) {
                var hero = cur.get_hero(name);
                for (var elem of hero.abilities) {
                    idSet.add(elem[0]); // dont add the ability level
                }
                for (elem of hero.items) {
                    idSet.add(elem);
                }
                for (elem of hero.talents) {
                    idSet.add(elem[0]); // dont add if the talent is picked
                }
            }
            cur = cur.next;
        }
        let idList = Array.from(idSet);
        return idList;
    }
    
    
    // builds from json data
    static from_json(data) {
        var time;
        var unit;
        var abl;
        var abilities;
        var talents;
        var items;
        var other;
        var heroSet;
        var focus;
        var output = new OrderedHeroTimes();
        
        // loop over all the given times, making a new HeroTimeSet for each
        for (var i = 0; i < data.length; i++) {
            
            // make the HeroTimeSet at this given time
            heroSet = new HeroTimeSet();
            time = parseFloat(data[i][JSON_KEY_TIME]);
            focus = data[i][JSON_KEY_FOCUS];
            for (var name of Object.keys(data[i][JSON_KEY_HEROES])) {
                
                // make a new HeroTime for each hero at this time
                unit = data[i][JSON_KEY_HEROES][name];
                other = {};
                for (var key of Object.keys(unit)) {
                    
                    // parse abilities (and talents)
                    if (key == JSON_KEY_HERO_ABILITIES) {
                        abl = split_abilities_talents(unit[JSON_KEY_HERO_ABILITIES]);
                        abilities = abl[0];
                        talents = abl[1];
                    }
                    
                    // parse items
                    else if (key == JSON_KEY_HERO_ITEMS) {
                        items = unit[JSON_KEY_HERO_ITEMS];
                    }
                    
                    // parse other attributes
                    else {
                        other[key] = unit[key];
                    }
                }
                heroSet.add(new HeroTime(name, time, abilities, talents, items, other));
            }
            
            // STOPGAP FIX, NEEDS TO BE RESOLVED AT THE SERVER
            try { heroSet.set_focus(focus); }
            catch (e) { heroSet.set_focus(Object.keys(heroSet.heroes)[0]); }
            output.push(heroSet);
        }
        return output;
    }
    
    
    // builds from a string (deprecated)
    static from_string(string) {
        var splitTimes = string.split(SEP_TIMES);
        var splitCategories;
        var hero;
        var time;
        var abilities;
        var talents;
        var items;
        var output = new OrderedHeroTimes();
        for (var i = 0; i < splitTimes.length; i++) {
            splitCategories = splitTimes[i].split(SEP_CATEGORIES);
            time = parseFloat(splitCategories.shift());
            hero = splitCategories.shift();
            abilities = splitCategories.shift().split(SEP_ELEMENTS);
            talents = abilities.slice(abilities.length-N_TALENTS, abilities.length);
            abilities = abilities.slice(0, abilities.length-N_TALENTS);
            items = splitCategories.shift().split(SEP_ELEMENTS);
            output.push(new HeroTime(hero, time, abilities, talents, items));
        }
        return output;
    }
    
    
    // builds a string which can then be used to build another timer (deprecated)
    to_string() {
        var cur = this.first;
        var output = '';
        while (!(cur.next === null)) {
            output += cur.time + SEP_CATEGORIES;
            output += cur.name + SEP_CATEGORIES;
            output += cur.abilities.join(SEP_ELEMENTS) + SEP_CATEGORIES;
            output += cur.items.join(SEP_ELEMENTS);
            output += SEP_TIMES;
            cur = cur.next;
        }
        output += cur.time + SEP_CATEGORIES;
        output += cur.name + SEP_CATEGORIES;
        output += cur.abilities.join(SEP_ELEMENTS) + SEP_CATEGORIES;
        output += cur.items.join(SEP_ELEMENTS);
        return output;
    }
}


// test
function timer_test() {
    return new Promise(function(resolve, reject) {
        var data = '0.0;TIMBERSAW;5524,5525,5526,5527,5902,5985,5913,5991,5948,6197,6223,6080;182,44,39,177,14,,,,,,,,,,,,:2.0;QUEEN OF PAIN;5173,5174,5175,5176,5960,5927,6190,6007,5943,5959,6248,6061;77,44,,,,,,,,,,,,,,,:7.0;DROW RANGER;5019,5632,5021,5022,5917,5921,6034,5906,6202,6281,6209,6280;44,39,14,12,16,,,,,,,,,,,,:17.0;QUEEN OF PAIN;5173,5174,5175,5176,5960,5927,6190,6007,5943,5959,6248,6061;77,44,,,,,,,,,,,,,,,:22.0;DROW RANGER;5019,5632,5021,5022,5917,5921,6034,5906,6202,6281,6209,6280;44,39,14,12,16,,,,,,,,,,,,:31.0;LINA;5040,5041,5042,5043,6006,5960,5964,6197,5996,5963,6212,6313;44,244,38,,42,,,,,,,,,,,,:34.0;RIKI;5142,5143,5144,5145,5902,5917,6029,6017,6161,5923,6330,6193;,27,12,44,,,,,,,,,,,,,:46.0;DROW RANGER;5019,5632,5021,5022,5917,5921,6034,5906,6202,6281,6209,6280;44,39,14,12,16,,,,,,,,,,,,:49.0;RIKI;5142,5143,5144,5145,5902,5917,6029,6017,6161,5923,6330,6193;,27,12,44,,,,,,,,,,,,,:61.0;LINA;5040,5041,5042,5043,6006,5960,5964,6197,5996,5963,6212,6313;44,244,38,,42,,,,,,,,,,,,:67.0;WRAITH KING;5086,5087,5088,5089,5938,5925,5917,5959,6210,6080,6201,6088;29,182,239,,,,,,,,,,,,,,:72.0;;;:73.0;RIKI;5142,5143,5144,5145,5902,5917,6029,6017,6161,5923,6330,6193;214,,,44,,,,,,,,,,,,,:76.0;;;:77.0;TIMBERSAW;5524,5525,5526,5527,5902,5985,5913,5991,5948,6197,6223,6080;71,,,177,,,,,,,,,,,,,:91.0;DROW RANGER;5019,5632,5021,5022,5917,5921,6034,5906,6202,6281,6209,6280;44,39,14,88,16,,,,,20,74,,,,,,:101.0;RIKI;5142,5143,5144,5145,5902,5917,6029,6017,6161,5923,6330,6193;214,,,44,,,,,,,,,,,,,:110.0;TIMBERSAW;5524,5525,5526,5527,5902,5985,5913,5991,5948,6197,6223,6080;71,,,177,,,,,,,,,,,,,:115.0;SILENCER;5377,5378,5379,5380,5931,6117,5959,5956,5907,6299,5945,6184;,20,15,,42,,,,,,,,,,,,:123.0;DROW RANGER;5019,5632,5021,5022,5917,5921,6034,5906,6202,6281,6209,6280;44,39,14,88,16,,,,,20,74,,,,,,:128.0;CLOCKWERK;5237,5238,5239,5240,5931,6094,5940,5917,5954,6299,5976,5977;16,182,44,,46,29,,,,,,,,,,,:130.0;SILENCER;5377,5378,5379,5380,5931,6117,5959,5956,5907,6299,5945,6184;,20,15,,42,,,,,,,,,,,,:133.0;WRAITH KING;5086,5087,5088,5089,5938,5925,5917,5959,6210,6080,6201,6088;29,182,239,,,,,,,,,,,,,,:138.0;TIMBERSAW;5524,5525,5526,5527,5902,5985,5913,5991,5948,6197,6223,6080;71,,,177,,,,,,,,,,,,,:167.0;DROW RANGER;5019,5632,5021,5022,5917,5921,6034,5906,6202,6281,6209,6280;44,39,212,,16,,,,,,,,,,,,:172.0;PUDGE;5075,5076,5074,5077,5982,5961,5932,5917,5957,6038,6023,6245;182,43,44,38,29,,,,,,,,,,,,:175.0;;;:176.0;;;:177.0;DROW RANGER;5019,5632,5021,5022,5917,5921,6034,5906,6202,6281,6209,6280;44,39,212,,16,,,,,,,,,,,,:182.0;CLOCKWERK;5237,5238,5239,5240,5931,6094,5940,5917,5954,6299,5976,5977;16,182,44,41,46,29,,,,,,,,,,,:191.0;QUEEN OF PAIN;5173,5174,5175,5176,5960,5927,6190,6007,5943,5959,6248,6061;77,44,41,,,,,,,29,,,,,,,:193.0;LINA;5040,5041,5042,5043,6006,5960,5964,6197,5996,5963,6212,6313;44,244,38,,,,,,,,,,,,,,:209.0;CLOCKWERK;5237,5238,5239,5240,5931,6094,5940,5917,5954,6299,5976,5977;16,41,44,29,46,182,,,,,,,,,,,:211.0;QUEEN OF PAIN;5173,5174,5175,5176,5960,5927,6190,6007,5943,5959,6248,6061;77,44,41,,,,,,,29,,,,,,,:214.0;TIMBERSAW;5524,5525,5526,5527,5902,5985,5913,5991,5948,6197,6223,6080;71,177,11,29,,,,,,,,,,,,,:218.0;SILENCER;5377,5378,5379,5380,5931,6117,5959,5956,5907,6299,5945,6184;,20,15,,,,,,,76,29,44,,,,,:229.0;;;:230.0;SILENCER;5377,5378,5379,5380,5931,6117,5959,5956,5907,6299,5945,6184;,20,15,,,,,,,76,29,44,,,,,:238.0;TIMBERSAW;5524,5525,5526,5527,5902,5985,5913,5991,5948,6197,6223,6080;71,177,11,29,,,,,,,,,,,,,:240.0;PUDGE;5075,5076,5074,5077,5982,5961,5932,5917,5957,6038,6023,6245;182,43,44,,29,,,,,,,,,,,,:241.0;;;:242.0;;;:243.0;;;:244.0;;;:245.0;PUDGE;5075,5076,5074,5077,5982,5961,5932,5917,5957,6038,6023,6245;182,43,44,,29,,,,,,,,,,,,:248.0;;;:249.0;DROW RANGER;5019,5632,5021,5022,5917,5921,6034,5906,6202,6281,6209,6280;44,,212,,16,,,,,,,,,,,,:255.0;CLOCKWERK;5237,5238,5239,5240,5931,6094,5940,5917,5954,6299,5976,5977;16,41,44,29,46,182,2,,,,,,,,,,:257.0;LINA;5040,5041,5042,5043,6006,5960,5964,6197,5996,5963,6212,6313;44,244,38,,40,,,,,,,,,,,,:267.0;RIKI;5142,5143,5144,5145,5902,5917,6029,6017,6161,5923,6330,6193;214,18,,44,,46,,,,,,,,,,,:272.0;CLOCKWERK;5237,5238,5239,5240,5931,6094,5940,5917,5954,6299,5976,5977;16,41,44,50,46,182,,,,,,,,,,,:279.0;DROW RANGER;5019,5632,5021,5022,5917,5921,6034,5906,6202,6281,6209,6280;44,,212,,16,,,,,,,,,,,,:284.0;CLOCKWERK;5237,5238,5239,5240,5931,6094,5940,5917,5954,6299,5976,5977;16,41,44,50,46,182,,,,,,,,,,,:291.0;DROW RANGER;5019,5632,5021,5022,5917,5921,6034,5906,6202,6281,6209,6280;44,,212,,16,,,,,,,,,,,,:292.0;LINA;5040,5041,5042,5043,6006,5960,5964,6197,5996,5963,6212,6313;44,244,38,46,40,,,,,,,,,,,,:299.0;CLOCKWERK;5237,5238,5239,5240,5931,6094,5940,5917,5954,6299,5976,5977;16,41,44,50,46,182,,,,,,,,,,,:302.0;LINA;5040,5041,5042,5043,6006,5960,5964,6197,5996,5963,6212,6313;44,244,38,46,40,,,,,,,,,,,,:304.0;;;:305.0;;;:306.0;;;:307.0;QUEEN OF PAIN;5173,5174,5175,5176,5960,5927,6190,6007,5943,5959,6248,6061;77,44,41,17,29,,,,,,,,,,,,:308.0;;;:309.0;;;:310.0;;;:311.0;;;:312.0;;;:313.0;;;:314.0;;;:315.0;;;:316.0;PUDGE;5075,5076,5074,5077,5982,5961,5932,5917,5957,6038,6023,6245;182,218,44,46,29,,,,,,,,,,,,:317.0;;;:318.0;;;:319.0;;;:320.0;PUDGE;5075,5076,5074,5077,5982,5961,5932,5917,5957,6038,6023,6245;182,218,44,46,29,,,,,,,,,,,,:325.0;RIKI;5142,5143,5144,5145,5902,5917,6029,6017,6161,5923,6330,6193;214,18,,44,,46,,,,,,,,,,,:330.0;QUEEN OF PAIN;5173,5174,5175,5176,5960,5927,6190,6007,5943,5959,6248,6061;77,44,41,17,29,,,,,,,,,,,,:331.0;;;:332.0;;;:333.0;WRAITH KING;5086,5087,5088,5089,5938,5925,5917,5959,6210,6080,6201,6088;25,182,239,29,46,,,,,25,,,,,,,:334.0;LINA;5040,5041,5042,5043,6006,5960,5964,6197,5996,5963,6212,6313;44,244,38,46,40,,,,,,,,,,,,:336.0;RIKI;5142,5143,5144,5145,5902,5917,6029,6017,6161,5923,6330,6193;214,18,,44,,46,,,,,,,,,,,:343.0;TIMBERSAW;5524,5525,5526,5527,5902,5985,5913,5991,5948,6197,6223,6080;46,178,11,29,71,,,,,,,,,,,,:348.0;DROW RANGER;5019,5632,5021,5022,5917,5921,6034,5906,6202,6281,6209,6280;44,,212,,16,,,,,,,,,,,,';
        TIMER = OrderedHeroTimes.from_string(data);
        resolve(TIMER);
    });
}
    

// ///////////////////////////////////////////////////////////////////////////////
// // LOAD TIMER /////////////////////////////////////////////////////////////////
// ///////////////////////////////////////////////////////////////////////////////
// var started = false;
// if (!started) {
    // var data = '0.0;TIMBERSAW;5524,5525,5526,5527,5902,5985,5913,5991,5948,6197,6223,6080;182,44,39,177,14,,,,,,,,,,,,:2.0;QUEEN OF PAIN;5173,5174,5175,5176,5960,5927,6190,6007,5943,5959,6248,6061;77,44,,,,,,,,,,,,,,,:7.0;DROW RANGER;5019,5632,5021,5022,5917,5921,6034,5906,6202,6281,6209,6280;44,39,14,12,16,,,,,,,,,,,,:17.0;QUEEN OF PAIN;5173,5174,5175,5176,5960,5927,6190,6007,5943,5959,6248,6061;77,44,,,,,,,,,,,,,,,:22.0;DROW RANGER;5019,5632,5021,5022,5917,5921,6034,5906,6202,6281,6209,6280;44,39,14,12,16,,,,,,,,,,,,:31.0;LINA;5040,5041,5042,5043,6006,5960,5964,6197,5996,5963,6212,6313;44,244,38,,42,,,,,,,,,,,,:34.0;RIKI;5142,5143,5144,5145,5902,5917,6029,6017,6161,5923,6330,6193;,27,12,44,,,,,,,,,,,,,:46.0;DROW RANGER;5019,5632,5021,5022,5917,5921,6034,5906,6202,6281,6209,6280;44,39,14,12,16,,,,,,,,,,,,:49.0;RIKI;5142,5143,5144,5145,5902,5917,6029,6017,6161,5923,6330,6193;,27,12,44,,,,,,,,,,,,,:61.0;LINA;5040,5041,5042,5043,6006,5960,5964,6197,5996,5963,6212,6313;44,244,38,,42,,,,,,,,,,,,:67.0;WRAITH KING;5086,5087,5088,5089,5938,5925,5917,5959,6210,6080,6201,6088;29,182,239,,,,,,,,,,,,,,:72.0;;;:73.0;RIKI;5142,5143,5144,5145,5902,5917,6029,6017,6161,5923,6330,6193;214,,,44,,,,,,,,,,,,,:76.0;;;:77.0;TIMBERSAW;5524,5525,5526,5527,5902,5985,5913,5991,5948,6197,6223,6080;71,,,177,,,,,,,,,,,,,:91.0;DROW RANGER;5019,5632,5021,5022,5917,5921,6034,5906,6202,6281,6209,6280;44,39,14,88,16,,,,,20,74,,,,,,:101.0;RIKI;5142,5143,5144,5145,5902,5917,6029,6017,6161,5923,6330,6193;214,,,44,,,,,,,,,,,,,:110.0;TIMBERSAW;5524,5525,5526,5527,5902,5985,5913,5991,5948,6197,6223,6080;71,,,177,,,,,,,,,,,,,:115.0;SILENCER;5377,5378,5379,5380,5931,6117,5959,5956,5907,6299,5945,6184;,20,15,,42,,,,,,,,,,,,:123.0;DROW RANGER;5019,5632,5021,5022,5917,5921,6034,5906,6202,6281,6209,6280;44,39,14,88,16,,,,,20,74,,,,,,:128.0;CLOCKWERK;5237,5238,5239,5240,5931,6094,5940,5917,5954,6299,5976,5977;16,182,44,,46,29,,,,,,,,,,,:130.0;SILENCER;5377,5378,5379,5380,5931,6117,5959,5956,5907,6299,5945,6184;,20,15,,42,,,,,,,,,,,,:133.0;WRAITH KING;5086,5087,5088,5089,5938,5925,5917,5959,6210,6080,6201,6088;29,182,239,,,,,,,,,,,,,,:138.0;TIMBERSAW;5524,5525,5526,5527,5902,5985,5913,5991,5948,6197,6223,6080;71,,,177,,,,,,,,,,,,,:167.0;DROW RANGER;5019,5632,5021,5022,5917,5921,6034,5906,6202,6281,6209,6280;44,39,212,,16,,,,,,,,,,,,:172.0;PUDGE;5075,5076,5074,5077,5982,5961,5932,5917,5957,6038,6023,6245;182,43,44,38,29,,,,,,,,,,,,:175.0;;;:176.0;;;:177.0;DROW RANGER;5019,5632,5021,5022,5917,5921,6034,5906,6202,6281,6209,6280;44,39,212,,16,,,,,,,,,,,,:182.0;CLOCKWERK;5237,5238,5239,5240,5931,6094,5940,5917,5954,6299,5976,5977;16,182,44,41,46,29,,,,,,,,,,,:191.0;QUEEN OF PAIN;5173,5174,5175,5176,5960,5927,6190,6007,5943,5959,6248,6061;77,44,41,,,,,,,29,,,,,,,:193.0;LINA;5040,5041,5042,5043,6006,5960,5964,6197,5996,5963,6212,6313;44,244,38,,,,,,,,,,,,,,:209.0;CLOCKWERK;5237,5238,5239,5240,5931,6094,5940,5917,5954,6299,5976,5977;16,41,44,29,46,182,,,,,,,,,,,:211.0;QUEEN OF PAIN;5173,5174,5175,5176,5960,5927,6190,6007,5943,5959,6248,6061;77,44,41,,,,,,,29,,,,,,,:214.0;TIMBERSAW;5524,5525,5526,5527,5902,5985,5913,5991,5948,6197,6223,6080;71,177,11,29,,,,,,,,,,,,,:218.0;SILENCER;5377,5378,5379,5380,5931,6117,5959,5956,5907,6299,5945,6184;,20,15,,,,,,,76,29,44,,,,,:229.0;;;:230.0;SILENCER;5377,5378,5379,5380,5931,6117,5959,5956,5907,6299,5945,6184;,20,15,,,,,,,76,29,44,,,,,:238.0;TIMBERSAW;5524,5525,5526,5527,5902,5985,5913,5991,5948,6197,6223,6080;71,177,11,29,,,,,,,,,,,,,:240.0;PUDGE;5075,5076,5074,5077,5982,5961,5932,5917,5957,6038,6023,6245;182,43,44,,29,,,,,,,,,,,,:241.0;;;:242.0;;;:243.0;;;:244.0;;;:245.0;PUDGE;5075,5076,5074,5077,5982,5961,5932,5917,5957,6038,6023,6245;182,43,44,,29,,,,,,,,,,,,:248.0;;;:249.0;DROW RANGER;5019,5632,5021,5022,5917,5921,6034,5906,6202,6281,6209,6280;44,,212,,16,,,,,,,,,,,,:255.0;CLOCKWERK;5237,5238,5239,5240,5931,6094,5940,5917,5954,6299,5976,5977;16,41,44,29,46,182,2,,,,,,,,,,:257.0;LINA;5040,5041,5042,5043,6006,5960,5964,6197,5996,5963,6212,6313;44,244,38,,40,,,,,,,,,,,,:267.0;RIKI;5142,5143,5144,5145,5902,5917,6029,6017,6161,5923,6330,6193;214,18,,44,,46,,,,,,,,,,,:272.0;CLOCKWERK;5237,5238,5239,5240,5931,6094,5940,5917,5954,6299,5976,5977;16,41,44,50,46,182,,,,,,,,,,,:279.0;DROW RANGER;5019,5632,5021,5022,5917,5921,6034,5906,6202,6281,6209,6280;44,,212,,16,,,,,,,,,,,,:284.0;CLOCKWERK;5237,5238,5239,5240,5931,6094,5940,5917,5954,6299,5976,5977;16,41,44,50,46,182,,,,,,,,,,,:291.0;DROW RANGER;5019,5632,5021,5022,5917,5921,6034,5906,6202,6281,6209,6280;44,,212,,16,,,,,,,,,,,,:292.0;LINA;5040,5041,5042,5043,6006,5960,5964,6197,5996,5963,6212,6313;44,244,38,46,40,,,,,,,,,,,,:299.0;CLOCKWERK;5237,5238,5239,5240,5931,6094,5940,5917,5954,6299,5976,5977;16,41,44,50,46,182,,,,,,,,,,,:302.0;LINA;5040,5041,5042,5043,6006,5960,5964,6197,5996,5963,6212,6313;44,244,38,46,40,,,,,,,,,,,,:304.0;;;:305.0;;;:306.0;;;:307.0;QUEEN OF PAIN;5173,5174,5175,5176,5960,5927,6190,6007,5943,5959,6248,6061;77,44,41,17,29,,,,,,,,,,,,:308.0;;;:309.0;;;:310.0;;;:311.0;;;:312.0;;;:313.0;;;:314.0;;;:315.0;;;:316.0;PUDGE;5075,5076,5074,5077,5982,5961,5932,5917,5957,6038,6023,6245;182,218,44,46,29,,,,,,,,,,,,:317.0;;;:318.0;;;:319.0;;;:320.0;PUDGE;5075,5076,5074,5077,5982,5961,5932,5917,5957,6038,6023,6245;182,218,44,46,29,,,,,,,,,,,,:325.0;RIKI;5142,5143,5144,5145,5902,5917,6029,6017,6161,5923,6330,6193;214,18,,44,,46,,,,,,,,,,,:330.0;QUEEN OF PAIN;5173,5174,5175,5176,5960,5927,6190,6007,5943,5959,6248,6061;77,44,41,17,29,,,,,,,,,,,,:331.0;;;:332.0;;;:333.0;WRAITH KING;5086,5087,5088,5089,5938,5925,5917,5959,6210,6080,6201,6088;25,182,239,29,46,,,,,25,,,,,,,:334.0;LINA;5040,5041,5042,5043,6006,5960,5964,6197,5996,5963,6212,6313;44,244,38,46,40,,,,,,,,,,,,:336.0;RIKI;5142,5143,5144,5145,5902,5917,6029,6017,6161,5923,6330,6193;214,18,,44,,46,,,,,,,,,,,:343.0;TIMBERSAW;5524,5525,5526,5527,5902,5985,5913,5991,5948,6197,6223,6080;46,178,11,29,71,,,,,,,,,,,,:348.0;DROW RANGER;5019,5632,5021,5022,5917,5921,6034,5906,6202,6281,6209,6280;44,,212,,16,,,,,,,,,,,,';
    // var timer = OrderedHeroTimes.from_string(data);
    // console.log(timer.times[0]);
    // started = true;
// }
