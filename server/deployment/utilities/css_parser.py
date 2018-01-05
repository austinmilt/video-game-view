"""
Copyright 2018 Austin Walker Milt

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
"""


"""
Parses CSS, mostly with the intent of pulling out ability div screen positions.
"""

# CONSTANTS ################################################################# #
PRS_KWD_CLA = '.'
PRS_KWD_IDN = '#'
PRS_KWD_OPN = '{'
PRS_KWD_DFN = ':'
PRS_KWD_END = ';'
PRS_KWD_CLS = '}'
PRS_KWD_COP = '/*'
PRS_KWD_CCL = '*/'
PRS_KWD_GRP = ','
PRS_KWD_CHL = ' > '
PRS_KWD_IMP = '!important'
PRS_KWD_TYP = {PRS_KWD_CLA: 'class', PRS_KWD_IDN: 'id'}
PRS_KWD_SGB = 'builtin'
def GET_TYPE(s): return PRS_KWD_TYP.get(s, PRS_KWD_SGB)


# CLASSES ################################################################### #

# ~~ CSSProperty ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ #
class CSSProperty:
    
    def __init__(self, name=None, value='', important=None):
        """
        CSSProperty is a property of a CSSSelector
        
        Args:
            name (str): (optional) name of the property
            value (str): (optional) value of the property. Composite values
                will be split into a list, but maintain the value as a string
            important (bool): (optional) whether this is an important property
                or not. Default is False. Will be automatically determined from
                value otherwise.
                
        Returns:
            CSSProperty: Instantiated CSSProperty object
        """
        self.name = name
        self.value = value
        self.update()
        if important is not None: self.important = important
        
        
    def __setattr__(self, key, value):
        self.__dict__[key] = value
        if key == 'value': self.update()
        
        
    def __repr__(self):
        return '<CSSProperty %s>' % str(self.name)
        
        
    def update(self):
        self.values, self.important = self.parse_value(self.value)
        
    
    @staticmethod
    def parse_value(valueStr=''):
        """
        Parses a value string into composite parts and whether or not this is
        an important string.
        
        Args:
            valueStr (str): (optional) value string to parse
            
        Returns:
            tuple: first element is a list of composite values, second element
                is a Boolean of whether this is an important property
        """
        valueList = valueStr.split()
        if valueList[-1].lower().startswith(PRS_KWD_IMP):
            return (valueList[:-1], True)
            
        else:
            return (valueList, False)
            
            
    @staticmethod
    def from_string(text):
        """
        Creates a CSSProperty from a property: value string.
        
        Args:
            text (str): text to parse
            
        Returns:
            CSSProperty: new CSSProperty object
        """
        pname, value = text.split(PRS_KWD_DFN)
        pname = pname.strip()
        value = value.strip()
        return CSSProperty(pname, value)
        
        
    def val_to_float(self):
        """
        Attempts to turn the value into a floating point number.
        """
        scomp = dict((i, i) for i in list('01234567890.'))
        newS = ''.join([scomp.get(s, '') for s in self.value])
        return float(newS)
        


# ~~ CSSSelector ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ #
class CSSSelector:

    def __init__(
        self, name=None, stype=PRS_KWD_SGB, properties=[], superior=None, descendants=[], 
        parent=None, children=[]
    ):
        """
        CSSSelector is a selector in CSS that has a set of properties for
        defining the style of an html class or ID.
        
        Args:
            name (str): (optional) name of the selector
            stype (str): (optional) selector type. Can be 'class', 'id', or 'builtin' (Default)
            properties (list): (optional) list of CSSProperties of this selector
            superior (CSSSelector): (optional) selector of which this is one descendant
            descendants (list): (optional) list of CSSSelectors that are descendant of this
            parent (CSSSelector): (optional) selector of which this is a child
            children (list): (optional) list of CSSSelectors that are children of this
            
        Returs:
            CSSSelector: instantiated CSSSelector object
        """
        self.name = name
        self.properties = {}
        self.add_properties(properties)
        self.superior = superior
        self.descendants = descendants
        
        
    def __getitem__(self, key):
        return self.properties[key]
            
            
    def __setitem__(self, key, value):
        assert isinstance(value, CSSProperty), 'New properties must be instances of CSSProperty.'
        self.properties[key] = value
        
        
    def __repr__(self):
        return '<CSSSelector %s (%i properties)>' % (str(self.name), len(self.properties))
        
        
    def add_property(self, property):
        self[property.name] = property
        
        
    def add_properties(self, properties=[]):
        _ = [self.add_property(p) for p in properties]
        
        
    def keys(self):
        return self.properties.keys()
        
        
    def values(self):
        return self.properties.values()
    
    
    @staticmethod
    def declarations_from_string(text, properties=[]):
        """
        Parses a string to return the groups, names, children, and superiors
        for selectors in the declaration part of a css definition.
        
        Args:
            text (str): string to parse
            properties (list): (optional) properties to give every selector
            
        Returns:
            list: CSSSelectors with empty properties but superiors and
                descendants defined
                
        Notes:
            o Doesnt currently support selections by attribute (e.g. 
              input[type="text"])
        """
        text = text.strip()
        groupList = text.split(PRS_KWD_GRP)
        selectors = []
        for group in groupList:
            group = group.strip()
            
            # split by children
            if PRS_KWD_CHL in group:
                parentStr, childStr = group.split(PRS_KWD_CHL)
                childStr = childStr.strip()
                parentStr = parentStr.strip()
                childType = GET_TYPE(childType[0])
                if childStr[0] in (PRS_KWD_CLA, PRS_KWD_IDN): childStr = childStr[1:]
                child = CSSSelector(childStr, properties)
                child.parent = parentStr
                selectors.append(child)
                
            else:
            
                # split by descendants
                descList = group.split()
                if len(descList) > 1:
                
                    # split out by superior and descendant
                    superiorStr, descendantStr = descList
                    superiorStr = superiorStr.strip()
                    descendantType = GET_TYPE(descendantStr[0])
                    if descendantStr[0] in (PRS_KWD_CLA, PRS_KWD_IDN): descendantStr = descendantStr[1:]
                    if superiorStr[0] in (PRS_KWD_CLA, PRS_KWD_IDN): superiorStr = superiorStr[1:]
                    descendant = CSSSelector(descendantStr, descendantType, properties)
                    descendant.superior = superiorStr
                    selectors.append(descendant)
                    
                    
                # single selectors without dependencies
                else:
                    selectorType = GET_TYPE(group[0])
                    if group[0] in (PRS_KWD_CLA, PRS_KWD_IDN): group = group[1:]
                    selector = CSSSelector(group, selectorType, properties)
                    selectors.append(selector)
                    
        return selectors
        
        

# ~~ CSSParser ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ #
class CSSParser:
    
    def __init__(self, text=None, file=None):
        """
        CSSParser is a simple CSS parser that gives the ability to search CSS
        definitions for attributes and values.
        
        Args:
            text (str): (optional) text used to define the parser, as would be
                loaded from a file
                
            file (str): (optional) path to file to use to define the parser, as
                in a .css file.
                
        Returns:
            CSSParser: instantiated CSSParser object
            
        Notes:
            o Doesnt currently support selections by attribute (e.g. 
              input[type="text"])
        """
        if text is not None:
            self.define_from_text(text)
            
        elif file is not None:
            self.define_from_file(file)
            
            
    def __getitem__(self, key):
        return self.selectors[key]
        
        
    def __setitem__(self, key, value):
        assert isinstance(value, CSSSelector), 'New selectors must be instances of CSSSelector.'
        self.selectors[key] = value
        
        
    def __repr__(self):
        return '<CSSParser (%i selectors)>' % len(self.selectors)
        
        
    def keys(self):
        return self.selectors.keys()
        
    
    def values(self):
        return self.selectors.values()
            
            
    def define_from_text(self, text):
        """(Re)Defines the parser from text."""
        self.selectors = {}
        queue = text.strip().replace('\n', ' ').replace('\t', ' ')
        while len(queue) > 0:
            
            # strip whitespace
            queue = queue.lstrip()
            
            # find next definition section
            selectorSection = queue.split(PRS_KWD_CLS, 1)
            if len(selectorSection) == 1: break # found the last selector definition
            current, queue = selectorSection
            selectorsStr, definitionsStr = current.split(PRS_KWD_OPN, 1)
            selectorsStr = selectorsStr.split(PRS_KWD_CCL)[-1]
            
            # build selectors
            properties = [CSSProperty.from_string(s) for s in definitionsStr.split(PRS_KWD_END)[:-1]]
            selectors = CSSSelector.declarations_from_string(selectorsStr, properties)
            self.selectors.update(dict((s.name, s) for s in selectors))
            
            
    def define_from_file(self, file):
        """(Re)Defines the parser from a file."""
        return self.define_from_text(open(file, 'r').read())
        
        
    @staticmethod
    def new_from_text(text):
        """Creates a new parser from text."""
        return CSSParser().define_from_text(text)
        
        
    @staticmethod
    def new_from_file(file):
        """Creates a new parser from a file."""
        return CSSParser().define_from_file(file)
        
        
    
    
if __name__ == '__main__':
    f = r'C:\Users\Austin\Dropbox\youtube_game_hud\client\chrome_extension\tooltip_manager.css'
    C = CSSParser(file=f)
    print C['abilities-container']['width'].value
    import pdb; pdb.set_trace()