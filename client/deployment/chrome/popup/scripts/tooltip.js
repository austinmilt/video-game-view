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

class VGVTooltip {
    
    constructor(element, container, html, cssClasses) {
        
        // update defaults and check args
        if (!element) { throw new Error('Must provide an element that will have the tooltip.'); }
        if (!container) { container = document.body; }
        if (!html) { html = ''; }
        
        // create the tooltip
        var tooltip = document.createElement(TT_HTMLTYPE);
        tooltip.innerHTML = html;
        tooltip.classList.add(CLASS_TT);
        tooltip.classList.add(cssClasses);
        var self = this;
        this.enter = function(event) {
            self._set_position(self._t_, event);
            self._c_.appendChild(self._t_);
        }
        this.leave = function(event) {
            try { self._c_.removeChild(self._t_); }
            catch (e) {}
        }
        
        // add the tooltip listeners to display and hide
        this._t_ = tooltip;
        element._vgvtt_ = this;
        this._e_ = element;
        this._c_ = container;
        element.addEventListener('mouseenter', this.enter);
        element.addEventListener('mouseleave', this.leave);
    }
    
    
    // removes the tooltip from this element so it can be deleted
    remove() {
        this._e_.removeEventListener('mouseenter', this.enter);
        this._e_.removeEventListener('mouseleave', this.leave);
        try { this._c_.removeChild(self._t_); }
        catch (e) {}
        this._e_ = null;
        this._c_ = null;
    }
    
    
    // get the mouse position in the body as a percent of the body (instead
    // of pixels)
    static _get_mousepos_percent(event) {
        var x = event.clientX;
        var y = event.clientY;
        var width = document.body.clientWidth;
        var height = document.body.clientHeight;
        return {x: (100.0*x)/width, y: (100.0*y)/height};
    }

    
    // determines the tooltip display position based on where the user's mouse
    // is positioned in the popup
    _set_position(tooltip, event) {
        var pos = VGVTooltip._get_mousepos_percent(event);

        if (pos.x > 50) {
            tooltip.style.left = 'auto';
            tooltip.style.right = (100 - pos.x) + '%';
        }
        else {
            tooltip.style.right = 'auto';
            tooltip.style.left = pos.x + '%';
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
    
    
    // update the html of the entire tooltip or one of its child elements
    set_html(html, childID) {
        if (childID) {
            var child = document.getElementById(childID); //////// REPLACE WITH SOMETHING OTHER THAN ID (so you can do this._e_.get....)
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
    
    
    // build a tooltip with a template and given element values
    static from_template(template, values) {}
}


// a tooltip that displays the innerText of its tooltip element
class VGVIdentityTooltip extends VGVTooltip {
    
    constructor(element, container, cssClasses) {
        super(element, container, element.innerText, cssClasses);
    }
    
    refresh() { this._t_.innerText = this._e_.innerText; }
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
