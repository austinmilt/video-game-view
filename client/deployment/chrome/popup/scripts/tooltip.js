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

const ATTR_ADDTO = 'vgvtt_addto';
const ATTR_TT = 'vgvtt_html';
const ATTR_CLASSES = 'vgvtt_class';
const CLASS_TT = 'vgvtt';
const TT_HTMLTYPE ='div';

const TMPL_TEXT_NODE_TYPE = 3;
const TMPL_ATTR_TID = 'vgvtt_tid';
const TMPL_CHILD_TYPE = 'span';

class VGVTooltip {
    
    constructor(element, container, html, cssClasses, posBy, sticky) {
        
        // update defaults and check args
        if (!element) { throw new Error('Must provide an element that will have the tooltip.'); }
        if (!container) { container = document.body; }
        if (!html) { html = ''; }
        if (!posBy) { posBy = {by: 'mouse'}; }
        if (!sticky) { sticky = false; }
        
        // create the tooltip
        this.sticky = sticky;
        this.stuck = false;
        this.visible = false;
        var tooltip = document.createElement(TT_HTMLTYPE);
        tooltip.innerHTML = html;
        tooltip.classList.add(CLASS_TT);
        if (cssClasses) { tooltip.classList.add(...cssClasses); }
        var self = this;
        this.display = function(event) {
            if (!self.visible) {
                self.visible = true;
                if (posBy.by == 'mouse') {
                    self._set_position_by_mouse(event);
                }
                else if (posBy.by == 'element') {
                    self._set_pos_by_element(posBy.direction);
                }
                self._c_.appendChild(self._t_);
            }
        }
        this.hide = function(event) {
            if (self.visible) {
                if (self.sticky && self.stuck) { return; }
                try { self._c_.removeChild(self._t_); }
                catch (e) {}
                self.visible = false;
            }
        }
        this.toggle_stuck = function(event) {
            if (self.sticky) { self.stuck = !self.stuck; }
        }
        
        // add the tooltip listeners to display and hide
        this._t_ = tooltip;
        element._vgvtt_ = this;
        this._e_ = element;
        this._c_ = container;
        element.addEventListener('mouseenter', this.display);
        element.addEventListener('mouseleave', this.hide);
        if (sticky) { element.addEventListener('click', this.toggle_stuck); }
    }
    
    
    // removes the tooltip from this element so it can be deleted
    remove() {
        this._e_.removeEventListener('mouseenter', this.display);
        this._e_.removeEventListener('mouseleave', this.hide);
        try { this._e_.removeEventListener('click', this.toggle_stuck); }
        catch (e) { console.log(e); } // remove eventually
        try { this.hide(); }
        catch (e) { console.log(e); } // remove eventually
        try { this._t_.remove(); }
        catch (e) { console.log(e); } // remove eventually
        this._e_ = null;
        this._c_ = null;
    }
    
    
    // get the mouse position in the body as a percent of the body (instead
    // of pixels)
    static _get_mousepos_percent(event) {
        var x = event.clientX + window.scrollX;
        var y = event.clientY + window.scrollY;
        var width = window.innerWidth;
        var height = window.innerHeight;
        return {x: (100.0*x)/width, y: (100.0*y)/height};
    }

    
    // determines the tooltip display position based on where the user's mouse
    // is positioned in the popup
    _set_position_by_mouse(event) {
        var pos = VGVTooltip._get_mousepos_percent(event);
        var tooltip = this._t_;
        var cPos = this._c_.getBoundingClientRect();
        var cpr = {
            left: 100 * cPos.left / window.innerWidth,
            right: 100 * cPos.right / window.innerWidth,
            top: 100 * cPos.top / window.innherHeight,
            bottom: 100 * cPos.bottom / window.innerHeight,
            width: 100 * cPos.width / window.innerWidth,
            height: 100 * cPos.height / window.innerHeight
        };

        if (pos.x > 50) {
            tooltip.style.left = 'auto';
            var right = 100 - pos.x;
            tooltip.style.right = 100 - pos.x + '%';
            tooltip.style.maxWidth = (pos.x - cpr.left) + '%';
        }
        else {
            tooltip.style.right = 'auto';
            tooltip.style.left = pos.x + '%';
            tooltip.style.maxWidth = (cpr.right - pos.x) + '%';
        }    
        
        if (pos.y > 50) {
            tooltip.style.top = 'auto';
            tooltip.style.bottom = (100 - pos.y) + '%';
        }
        else {
            tooltip.style.bottom = 'auto';
            tooltip.style.top = pos.y + '%';
        }
    }
    

    // determines the tooltip display position based on where the element 
    // with the tooltip is
    _set_pos_by_element(tooltip, direction='north') {
        var tooltip = this._t_;
        var ebox = this._e_.getBoundingClientRect();
        var cbox = this._c_.getBoundingClientRect();
        var tbox = this._t_.getBoundingClientRect();
        var width = cbox.width;
        var height = cbox.height;
        var exc = {
            left: 100*(ebox.left - cbox.left)/width, 
            right: 100*(ebox.right - cbox.right)/width,
            hmid: 100*(ebox.left + 0.5*ebox.width - cbox.left)/width,
            top: 100*(ebox.top - cbox.top)/height, 
            bottom: 100*(ebox.bottom - cbox.bottom)/height,
            vmid: 100*(ebox.top + 0.5*ebox.height - cbox.top)/height,
        };
        if (direction == 'north') {
            tooltip.style.bottom = (100 - exc.top) + '%';
            tooltip.style.left = 'calc(' + exc.hmid + '% - 0.5*var(--tooltip-width))';
        }
        else if (direction == 'south') {
            tooltip.style.top = exc.bottom + '%';
            tooltip.style.left = 'calc(' + exc.hmid + '% - 0.5*var(--tooltip-width))';
        }
        else if (direction == 'east') {
            tooltip.style.bottom = (100 - exc.top) + '%';
            tooltip.style.left = exc.right + '%';
        }
        else if (direction == 'west') {
            tooltip.style.bottom = (100 - exc.top) + '%';
            tooltip.style.right = exc.left + '%';
        }
    }
    
    
    // update the html of the entire tooltip or one of its child elements
    set_html(html, childID) {
        if (childID) {
            var child = document.getElementById(childID);
            if (!child) { throw new Error('Could not find child with id ' + childID); }
            child.innerHTML = html;
        }
        else {
            this._t_.innerHTML = html;
        }
    }
    
    
    // update the html of multiple children at once
    set_htmls(htmls) {
        var childIDs = Object.keys(htmls);
        for (var i = 0; i < childIDs.length; i++) {
            this.set_html(htmls[childIDs[i]], childIDs[i]);
        }
    }
}


// a tooltip that displays the innerText of its tooltip element
class VGVIdentityTooltip extends VGVTooltip {
    
    constructor(element, container, cssClasses) {
        super(element, container, element.innerText, cssClasses);
    }
    
    refresh() { this._t_.innerText = this._e_.innerText; }
}



// holds info about an element in a template without immediately constructing
// the html or setting up the hierarchy
class VGVTemplateElement {
    constructor(tag, tid, html, styles, attributes) {
        if (!tid) { tid = null; }
        if (!html) { html = ''; }
        if (!styles) { styles = []; }
        if (!attributes) { attributes = {}; }
        this.tag = tag;
        this.tid = tid;
        this.html = html;
        this.styles = styles;
        this.attributes = attributes;
    }
    
    push(newChild) { this.children.push(newChild); }
    
    static from_array(data) {
        var tag = data[0];
        var tid = null;
        var html = null;
        var styles = [];
        var attributes = {};
        var n = data.length;
        if (n > 1) { tid = data[1]; }
        if (n > 2) { html = data[2]; }
        if (n > 3) { styles = data[3]; }
        if (n > 4) { attributes = data[4]; }
        return new VGVTemplateElement(tag, tid, html, styles, attributes);
    }
    
    construct_element() {
        var element = document.createElement(this.tag);
        var innerHTML = document.createElement(TMPL_CHILD_TYPE);
        innerHTML.innerHTML = this.html;
        element.appendChild(innerHTML);
        element.vgvttHTMLChild = innerHTML;
        if (this.styles.length > 0) { element.classList.add(this.styles); } 
        for (var k of Object.keys(this.attributes)) { 
            element.setAttribute(k, this.attributes[k]);
        }
        return element;
    }
}



// a tooltip built from a template that updates individual elements instead
// of the whole html
class VGVTemplateTooltip {
    
    constructor(templateHierarchy) {
        if (!templateHierarchy) { templateHierarchy = []; }
        this.mutable = {};
        this.html = document.createElement('html');
        this._k_ = [];
        this._d_ = {};
        var queue = new Array();
        for (var te of templateHierarchy) {
            var toQueue = {'te': te, 'parent': null};
            queue.push(toQueue);
        }
        while (queue.length > 0) {
            var current = queue.shift();
            var te = current['te'];
            var element = te.construct_element();
            var parent = this.html;
            if (current['parent']) { parent = current['parent']; }
            parent.appendChild(element);
            if (te.tid) {
                this.mutable[te.tid] = element; 
                this._k_.push(te.tid);
                this._d_[te.tid] = te.html;
            }
            for (var tc of te.children) { queue.push({'te': tc, 'parent': element}); }
        }
    }
    
    
    // construct nested elements from simple arrays
    static from_nested_array(data) {
        
        // populate the queue with outermost elements
        var queue = new Array();
        for (var teData of data) {
            var toQueue = {'data': teData, 'children': [], 'parent': null};
            if (teData.length > 5) { toQueue['children'] = teData[5]; }
            queue.push(toQueue);
        }
        
        // go through queue and get elements until we've covered all
        var hierarchy = [];
        while (queue.length > 0) {
            
            // build the current templateElement
            var current = queue.shift();
            var te = VGVTemplateElement.from_array(current['data']);
            te.children = [];
            
            // deal with parentage
            if (current['parent']) {
                current['parent'].children.push(te); 
                te.parent = current['parent']; 
            }
            else { te.parent = null; hierarchy.push(te); }
            
            // add children to the queue
            if (current['children']) {
                for (var child of current['children']) { 
                    var toQueue = {'data': child, 'children': [], 'parent': te};
                    if (child.length > 5) { toQueue['children'] = child[6]; }
                    queue.push(toQueue);
                }
            }
        }
        return new VGVTemplateTooltip(hierarchy);
    }
    
    
    // creates a new VGVTooltip from the template with the given innerText
    to_vgvtt(elWithTooltip, elTooltipContainer, innerHTML) {
        
        // check args
        var restoreDefaults = true;
        if (!innerHTML) {
            innerHTML = {}; 
            restoreDefaults = false;
        }
        
        // populate DOM elements with user specified text
        var tIDs = Object.keys(innerHTML);
        for (var tID of tIDs) {
            var element = this.mutable[tID];
            if (element) { element.vgvttHTMLChild.innerHTML = innerHTML[tID]; }
        }
        
        // build the VGVTooltip
        var vgvtt = new VGVTooltip(
            elWithTooltip, elTooltipContainer, 
            this.html.cloneNode(true).innerHTML
        );
        
        // restore template default text
        if (restoreDefaults) {
            for (var tID of this._k_) {
                var defaultHTML = this._d_[tID];
                if (!defaultHTML) { defaultHTML = ''; }
                this.mutable[tID].vgvttHTMLChild.innerHTML = defaultHTML;
            }
        }
        
        return vgvtt;
    }
}



// function to build tooltips in a static html
function build_static_tooltips() {
    var elementsWithTooltips = document.querySelectorAll('[' + ATTR_TT + ']');
    for (var i = 0; i < elementsWithTooltips.length; i++) {
        var element = elementsWithTooltips[i];
        var container = document.getElementById(element.getAttribute(ATTR_ADDTO));
        var ttHTML = element.getAttribute(ATTR_TT);
        var extraClasses = element.getAttribute(ATTR_CLASSES);
        new VGVTooltip(element, container, ttHTML, extraClasses);
    }
}
document.addEventListener('DOMContentLoaded', build_static_tooltips);



// test for VGVTemplateTooltip
function test_templating() {
    var templateData = [
        ['h1', 'ability_name', 'Shibboleth turkey', ['ability_name_header']],
        ['hr'],
        ['div', 'basic_description', 'Some description, for instance <span class="bold_descriptor">something bold</span>', ['section_div']],
        ['div', 'special_description', 'Some special stuff happens here', ['section_div'], [], [
            ['span', 'special_stuff_bolding', ' when some bold stuff happens', ['bold_descriptor']]
        ]]
    ];
    var template = VGVTemplateTooltip.from_nested_array(templateData);
    var toTT = document.getElementById('mything');
    var toC = document.getElementById('mycontainer');
    var concreteTooltip = template.to_vgvtt(toTT, toC, {
        'ability_name': 'A different Ability',
        'basic_description': 'We are putting some stuff in here, dunno if I <span class="bold_descriptor">can format </span>it.',
        'special_description': 'On rez, your stuff will be very special.'
    });
    var toTT = document.getElementById('defaultthing');
    var concreteTooltip2 = template.to_vgvtt(toTT, toC);
}
// document.addEventListener('DOMContentLoaded', test_templating);
