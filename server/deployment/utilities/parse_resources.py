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


# this script parses the dota 2 resource txt file for relevant data (e.g.
# conversion between npc_dota_hero names and in-game names) for both the
# server and client.

# #############################################################################
# GLOBAL IMPORTS ##############################################################
# #############################################################################
import os, re



# #############################################################################
# CONSTANTS ###################################################################
# #############################################################################

HERE = os.path.dirname(os.path.abspath(__file__))

EXTRACT_IMAGES = True

KWD_QUOTE = '"'
KWD_TITLESEP = '_'
KWD_OPEN = '{'
KWD_CLOSE = '}'
KWD_COMMENT = '//'

PRE_DES_ABILITY = 'DOTA_Tooltip_ability_'
PRE_DES_ITEM = 'DOTA_Tooltip_ability_item_'
PRE_VAR = 'dota_ability_variable_'
PRE_HNAME = 'npc_dota_hero_'
PRE_UNAME = 'npc_dota_'
PRE_NPC_ABILITY = ''
PRE_NPC_ITEM = 'item_'
PRE_NPC_UNIT = 'npc_dota_'
PRE_NPC_TOKEN = 'Tokens'
PRE_TTP_LORE = '_Lore'
PRE_TTP_DESC = '_Description'
PRE_TTP_NOTE = '_Note'
PRE_TTP_AGHDESC = '_aghanim_description'
PRE_TTP_AGHMOD = '_scepter'
PRE_TTP_AGHS = '_ultimate_scepter'

RGX_HNAME = re.compile('%s.*(?<!_bio)(?<!_hype)$' % PRE_HNAME)
RGX_UNAME = re.compile('(?!%s)%s.*' % (PRE_HNAME, PRE_UNAME))

RGX_DES_ABILITY = re.compile('(?!%s)%s.*' % (PRE_DES_ITEM, PRE_DES_ABILITY), re.IGNORECASE)
RGX_DES_ITEM = re.compile(PRE_DES_ITEM, re.IGNORECASE)
RGX_VAR = re.compile(PRE_VAR)

RGX_NPC_HNAME = re.compile(r'\s*' + KWD_QUOTE + '%s.*(?<!_bio)(?<!_hype)$' % PRE_HNAME)
RGX_NPC_TOKEN = re.compile(r'\s*' + KWD_QUOTE + PRE_NPC_TOKEN)
RGX_NPC_ABILITY = re.compile(r'\s*' + KWD_QUOTE + PRE_NPC_ABILITY)
RGX_NPC_ITEM = re.compile(r'\s*' + KWD_QUOTE + PRE_NPC_ITEM)
RGX_NPC_UNIT = re.compile(r'\s*' + KWD_QUOTE + PRE_NPC_UNIT)

RGX_TTP_LORE = re.compile(r'.*%s' % PRE_TTP_LORE)
RGX_TTP_DESC = re.compile(r'.*%s' % PRE_TTP_DESC)
RGX_TTP_NOTE = re.compile(r'.*%s\d' % PRE_TTP_NOTE)
RGX_TTP_AGDS = re.compile(r'(?!%s).*%s' % (PRE_TTP_AGHS, PRE_TTP_AGHDESC))
RGX_TTP_AGMD = re.compile(r'.*%s$(?<!%s$)' % (PRE_TTP_AGHMOD, PRE_TTP_AGHS))
RGX_TTP_VARI = re.compile(r'(.*\$.*)')

DEF_DOTA = os.path.abspath(os.path.join(os.environ['ProgramFiles(x86)'], 'Steam', 'SteamApps', 'common', 'dota 2 beta', 'game', 'dota'))
DEF_RESOURCE_FILES = {
    'abilities': {
        'local': os.path.join(DEF_DOTA, 'scripts', 'npc', 'npc_abilities.txt'), 
        'url': r'https://raw.githubusercontent.com/dotabuff/d2vpkr/master/dota/scripts/npc/npc_abilities.txt'
    },
    'heroes': {
        'local': os.path.join(DEF_DOTA, 'scripts', 'npc', 'npc_heroes.txt'),
        'url': r'https://raw.githubusercontent.com/dotabuff/d2vpkr/master/dota/scripts/npc/npc_heroes.txt'
    },
    'units': {
        'local': os.path.join(DEF_DOTA, 'scripts', 'npc', 'npc_units.txt'),
        'url': r'https://raw.githubusercontent.com/dotabuff/d2vpkr/master/dota/scripts/npc/npc_units.txt'
    },
    'items': {
        'local': os.path.join(HERE, 'scripts', 'npc', 'items.txt'), ## has to be extracted from vpk first
        'url': r'https://raw.githubusercontent.com/dotabuff/d2vpkr/master/dota/scripts/npc/items.txt' 
    },
    'resource': {
        'local': os.path.join(DEF_DOTA, 'resource', 'dota_english.txt'),
        'url': r'https://raw.githubusercontent.com/dotabuff/d2vpkr/master/dota/resource/dota_english.txt'
    }
}
DEF_ENCODING = 'utf-16'

VPK_EXE = os.path.abspath(os.path.join(HERE, 'vpk', 'vpk.exe'))
VPK_CALL_LISTFILES = [VPK_EXE, 'l']
VPK_CALL = [VPK_EXE, 'x']
VPK_DEF_VPK = os.path.abspath(os.path.join(DEF_DOTA, r'pak01_dir.vpk'))
VPK_DEF_FIL = ['scripts/npc/items.txt']
VPK_DEF_IMAGES = {
    'heroes': [r'resource/flash3/images/heroes'], 
    'items': [r'resource/flash3/images/items'], 
    'abilities': [r'resource/flash3/images/spellicons']
}

IMG_DEF_HOST = r'https://storage.googleapis.com/vgv-assets'
IMG_DEF_SUBDIR = {
    'heroes': r'images/heroes',
    'items': r'images/items',
    'abilities': r'images/spellicons'
}

MAN_KEY_ABL = r'Ability\d'
MAN_KEY_ASP = 'AbilitySpecial'
MAN_KEY_MAN = 'AbilityManaCost'
MAN_KEY_COO = 'AbilityCooldown'
MAN_KEY_CST = 'ItemCost'
MAN_ABS_SKP = ('var_type',)
MAN_KWD_VAR = r'%{k}%'
MAN_KWD_PCT = re.compile(r'(%%)(?=([ .!?]|$))')
MAN_KWD_LIN = re.compile('\\\\n')
MAN_RGX = '(.*(?<=%s%s%s))|(.*(?<=%s%s$))'
MAN_FIL_SRV = os.path.abspath(os.path.join(HERE, '..', 'resources', 'keys.pkl'))
MAN_FIL_CLN = os.path.abspath(os.path.join(HERE, '..', '..', '..', 'client', 'deployment', 'chrome', 'page', 'scripts', 'dotapedia.js'))
MAN_V2K = lambda k: k.split(PRE_VAR)[1]

TTP_NPC_BDICT = {
    'AbilityBehavior': {
        'DOTA_ABILITY_BEHAVIOR_PASSIVE': 'Passive', 
        'DOTA_ABILITY_BEHAVIOR_UNIT_TARGET': 'Targets Units',
        'DOTA_ABILITY_BEHAVIOR_CHANNELLED': 'Channeled',
        'DOTA_ABILITY_BEHAVIOR_POINT': 'Point Target',
        'DOTA_ABILITY_BEHAVIOR_ROOT_DISABLES': 'Disabled By Root',
        'DOTA_ABILITY_BEHAVIOR_AOE': 'AOE',
        'DOTA_ABILITY_BEHAVIOR_NO_TARGET': 'No Target',
        'DOTA_ABILITY_BEHAVIOR_DONT_RESUME_MOVEMENT': 'Casting Stops Movement',
        'DOTA_ABILITY_BEHAVIOR_DONT_RESUME_ATTACK': 'Casting Stops Attack',
        'DOTA_ABILITY_BEHAVIOR_DIRECTIONAL': 'Directional Cast',
        'DOTA_ABILITY_BEHAVIOR_IMMEDIATE': 'Other (Immediate)',
        'DOTA_ABILITY_BEHAVIOR_HIDDEN': 'Other (Hidden)',
        'DOTA_ABILITY_BEHAVIOR_NOT_LEARNABLE': 'Not Learnable',
        'DOTA_ABILITY_BEHAVIOR_TOGGLE': 'Toggle',
        'DOTA_ABILITY_BEHAVIOR_AURA': 'Aura',
        'DOTA_ABILITY_BEHAVIOR_IGNORE_BACKSWING': 'Other (Ignore Backswing)',
        'DOTA_ABILITY_BEHAVIOR_AUTOCAST': 'Autocast',
        'DOTA_ABILITY_BEHAVIOR_ATTACK': 'Attack Modifier',
        'DOTA_ABILITY_BEHAVIOR_IGNORE_PSEUDO_QUEUE': 'Usable While Disabled',
        'DOTA_ABILITY_BEHAVIOR_NORMAL_WHEN_STOLEN': 'Other (Normal When Stolen)',
        'DOTA_ABILITY_BEHAVIOR_OPTIONAL_UNIT_TARGET': 'Usable On Others',
        'DOTA_ABILITY_BEHAVIOR_UNRESTRICTED': 'Other (Unrestricted)',
        'DOTA_ABILITY_BEHAVIOR_DONT_CANCEL_MOVEMENT': 'Usable While Moving',
        'DOTA_ABILITY_BEHAVIOR_DONT_ALERT_TARGET': 'Doesnt Alert Target',
        'DOTA_ABILITY_BEHAVIOR_RUNE_TARGET': 'Can Target Runes',
        'DOTA_ABILITY_BEHAVIOR_DONT_CANCEL_CHANNEL': 'Doesnt Cancel Channeling',
        'DOTA_ABILITY_BEHAVIOR_NOASSIST': 'Other (No Assist)',
        'DOTA_ABILITY_BEHAVIOR_IGNORE_CHANNEL': 'Doesnt Cancel Channeling',
        'DOTA_ABILITY_TYPE_ULTIMATE': 'Ultimate'
    },
    
    'AbilityCastRange': {},
    
    'AbilityCastPoint': {},
    
    'AbilityDamage': {},
    
    'AbilityDuration': {},
    
    'AbilityUnitDamageType': {
        'DAMAGE_TYPE_PHYSICAL': 'Physical',
        'DAMAGE_TYPE_MAGICAL': 'Magical',
        'DAMAGE_TYPE_PURE': 'Pure'
    },
        
    'AbilityUnitTargetTeam': {
        'DOTA_UNIT_TARGET_TEAM_ENEMY': 'Enemies',
        'DOTA_UNIT_TARGET_TEAM_FRIENDLY': 'Allies',
        'DOTA_UNIT_TARGET_TEAM_BOTH': 'Allies and Enemies',
        'DOTA_UNIT_TARGET_TEAM_CUSTOM': 'Other'
    },
    
    'AbilityUnitTargetType': {
        u'DOTA_UNIT_TARGET_HERO': 'Hero',
        'DOTA_UNIT_TARGET_BASIC': 'Non-Ancient',
        'DOTA_UNIT_TARGET_CREEP': 'Creep',
        'DOTA_UNIT_TARGET_CUSTOM': 'Other',
        'DOTA_UNIT_TARGET_TREE': 'Tree',
        'DOTA_UNIT_TARGET_BUILDING': 'Building'
    },
    
    'SpellDispellableType': {
        'SPELL_DISPELLABLE_YES': 'Any',
        'SPELL_DISPELLABLE_NO': 'No',
        'SPELL_DISPELLABLE_STRONG': 'Strong Dispels',
        'SPELL_DISPELLABLE_YES_STRONG': 'Strong Dispels'
    },
    
    'SpellImmunityType': {
        'SPELL_IMMUNITY_ENEMIES_NO': 'No',
        'SPELL_IMMUNITY_ENEMIES_YES': 'Yes',
    }
}
    
    
TTP_NPC_LAM = lambda k, v: [TTP_NPC_BDICT[k].get(s.strip(), s.strip()) for s in v.split('|')]
TTP_NPC_BASIC = {
    'AbilityBehavior': 'BEHAVIOR:',
    'AbilityCastRange': 'CAST RANGE:',
    'AbilityCastPoint': 'CAST POINT:',
    'AbilityDamage': 'DAMAGE:',
    'AbilityDuration': 'DURATION:',
    'AbilityUnitDamageType': 'DAMAGE TYPE:',
    'AbilityUnitTargetTeam': 'TARGETS:', 
    'AbilityUnitTargetType': 'TARGET TYPE:', 
    'SpellDispellableType': 'DISPELLABLE:',
    'SpellImmunityType': 'PIERCES SPELL IMMUNITY:'
}



# #############################################################################
# CLASSES #####################################################################
# #############################################################################

class Name(object):
    
    def __init__(self, name=None, key=None, prefix=''):
        """
        Name class, basically for storing aliases of of a name, i.e. local
        name (e.g. Weaver), npc key (e.g. npc_dota_hero_weaver) and npc short
        name (e.g. weaver) using a key-matching prefix.
        """
        self.name = name
        self.key = key
        self._prefix = prefix
        
    def __repr__(self):
        return '<%s %s>' % (self.__class__.__name__, self.name)
        
    def key_to_short(self, key):
        """Get the short name, e.g. npc_dota_hero_weaver -> weaver."""
        return key.split(self._prefix)[1]
    
    def short_to_key(self, short):
        """Get the key from the short name, e.g. weaver -> npc_dota_hero_weaver."""
        return '%s%s' % (self._prefix, short)
    
    def get_short(self):
        """Get the short name with an assumed prefix, e.g. npc_dota_hero_weaver -> weaver."""
        return self.key_to_short(self.key)
    
    
class HeroName(Name):
    def __init__(self, name=None, key=None):
        """Name for heroes, just defines what the prefix is."""
        super(HeroName, self).__init__(name, key, PRE_HNAME)
        
        
class ItemName(Name):
    def __init__(self, name=None, key=None):
        """Name for items, just defines what the prefix is."""
        super(ItemName, self).__init__(name, key, PRE_NPC_ITEM)
        
        
class UnitName(Name):
    def __init__(self, name=None, key=None):
        super(UnitName, self).__init__(name, key, PRE_NPC_UNIT)
        
        
class ItemTooltipName(Name):
    def __init__(self, name=None, key=None):
        """Name for item tooltips, just defines what the prefix is."""
        super(ItemTooltipName, self).__init__(name, key, PRE_DES_ITEM)
        

class AbilityTooltipName(object):
    
    def __init__(self, name=None, key=None, hero=None):
        """
        Name for ability tooltips. Different from other Names in that the short
        name relies on knowing the hero name.
        """
        self.name = name
        self.key = key
        self._prefix = PRE_DES_ABILITY
        self._hero = hero
        
    def set_hero(self, hero):
        """Sets the hero NPC of self."""
        self._hero = hero
        
    def get_short(self):
        """Get the short name with an assumed prefix, where hero matters"""
        if self._hero is None: raise ValueError('Cannot get short name without knowing the hero.')
        else: return KWD_TITLESEP.join([self._prefix, self._hero.get_short(), self.get_short()])
        
        
class AbilityName(AbilityTooltipName):
    def __init__(self, name=None, key=None, hero=None):
        """Basically same as AbilityTooltipName, but using a different prefix."""
        super(AbilityName, self).__init__(name, key, hero)
        self._prefix = PRE_NPC_ABILITY
        
        
class NPC(object):

    _name_class = Name
    
    def __init__(self, name=None, attributes=None):
        """
        General container for storing info from dota resource files (e.g.
        dota_english.txt, npc_heroes.txt, etc.
        
        Args:
            name (Name): (optional) Name for npc. Default is empty Name().
            attributes (dict): (optional) data to store for this npc. Default
                is empty dict
        """
        if name is None: name = Name()
        if attributes is None: attributes = {}
        self._name = name
        self._data = attributes
        
    def __repr__(self):
        return '<%s %s>' % (self.__class__.__name__, self.get_name())
        
    def get(self, *keys):
        """Get value of data dict at series of *keys."""
        if len(keys) == 0: return self._data
        d = self._data[keys[0]]
        if len(keys) > 1:
            for k in keys[1:]: d = d[k]
            
        return d
        
    def has_key(self, key):
        """
        Determines if uppermost data level of data dict has the requested 
        key.
        """
        return self._data.has_key(key)
    
    def has_keys(self, *keys):
        """
        Determines if dictionary at levels determined by series of *keys has 
        the requested keys.
        """ 
        if len(keys) == 0: return self.has_key(keys[0])
        d = self._data[keys[0]]
        if len(keys) > 1:
            for k in keys[1:]:
                if not d.has_key(k): return False
                d = d[k]
                
        return True
        
    def get_name(self):
        """Gets the local name of the NPC."""
        return self._name.name
    
    def get_key(self):
        """Gets the full npc key of the NPC."""
        return self._name.key
    
    def get_short(self):
        """Gets the short npc key of the NPC."""
        return self._name.get_short()
        
    def keys(self):
        """Gets the keys of the uppermost level of the NPC data dict."""
        return self._data.keys()
    
    def search_all_keys(self, regex=re.compile(''), keys=[]):
        """
        Finds all keys using a regex search (as opposed to match) in the data 
        subdict gotten by self.get(*keys).
        """
        subdict = self.get(*keys)
        return [k for k in subdict.keys() if regex.search(k) is not None]
    
    def set(self, value, *keys):
        """
        Sets the nested attribute to the value given, 
        e.g. set('it', ['this', 'is']) would set self._data['this']['is'] = 'it'.
        If the nested dictionaries dont exist, they are created.
        """
        d = self._data
        for key in keys[:-1]:
            if key not in d: d[key] = {}
            d = d[key]
            
        d[keys[-1]] = value
        
    def set_name(self, name):
        """Sets the local name of the NPC."""
        self._name.name = name
    
    def set_key(self, key):
        """Sets the full npc key of the NPC."""
        self._name.key = key
        
    @classmethod
    def from_name_and_key(cls, name, key, attributes=None):
        """Creates a new NPC with given name, key, and attributes."""
        if attributes is None: attributes = {}
        return cls(cls._name_class(name, key), attributes)
        
        
class HeroNPC(NPC): _name_class = HeroName


class UnitNPC(NPC): _name_class = UnitName


class ItemNPC(NPC): _name_class = ItemName

        
class AbilityNPC(NPC):

    def set_hero(self, hero):
        """Set the hero NPC for this ability."""
        self._name.set_hero(hero)
        
    @staticmethod
    def from_name_and_key(name, key, hero=None, attributes=None):
        """Creates a new AbilityNPC with given name, key, hero, and attributes."""
        if attributes is None: attributes = {}
        return AbilityNPC(AbilityName(name, key, hero), attributes)
        
    
class NPCSet(object):

    _re = re.compile('')
    _npc = NPC
    
    def __init__(self, npcs=[]):
        """
        General container for storing sets of NPCs. Includes methods for
        searching through NPC names, iterating over them, and loading a
        set from a file.
        """
        self.npcs = {}
        self._keys = {}
        self._shorts = {}
        for npc in npcs: self.add(npc)
        
    def __repr__(self):
        return '<%s with %i NPCs>' % (self.__class__.__name__, len(self.npcs))
        
    def __iter__(self):
        for k in self.npcs: yield self.npcs[k]
        
    def add(self, npc):
        """Adds a new NPC to the set. Overwrites existing NPCs."""
        self.npcs[npc.get_name()] = npc
        self._keys[npc.get_key()] = npc
        try: self._shorts[npc.get_short()] = npc
        except ValueError: pass
        
    def get_by_name(self, name):
        """Get the NPC from the set by its local name."""
        return self.npcs[name]
    
    def get_by_key(self, key):
        """Get the NPC from the set by its full npc key."""
        return self._keys[key]
    
    def get_by_short(self, short):
        """Get the NPC from the set by its short npc key."""
        return self._shorts[short]
    
    def has_key(self, key):
        """Determines if this NPCSet has an NPC with the requested full npc key."""
        return self.npcs.has_key(key)
    
    def keys(self):
        """Gets the full npc keys of the NPCs in this set."""
        return self.npcs.keys()
    
    def search_all_keys(self, regex=re.compile('')):
        """
        Finds keys of all NPCs whose keys match from a regex search (as opposed to match).
        """
        return [k for k in self.npcs if regex.search(k) is not None]
    
    @classmethod
    def from_txt(cls, txt, encoding=DEF_ENCODING):
        """Creates a new NPCSet (or subclass) from a given file."""
        import io
        return NPCParser(io.open(txt, 'r', encoding=encoding), cls).parse()
        
        
class HeroNPCSet(NPCSet):
    _re = RGX_NPC_HNAME
    _npc = HeroNPC
    
    
class ItemNPCSet(NPCSet):
    _re = RGX_NPC_ITEM
    _npc = ItemNPC
    
    
class UnitNPCSet(NPCSet):
    _re = RGX_NPC_UNIT
    _npc = UnitNPC
    
    
class AbilityNPCSet(NPCSet):
    _re = RGX_NPC_ABILITY
    _npc = AbilityNPC
    
    
class TokenSet(NPCSet):
    _re = RGX_NPC_TOKEN
    _npc = NPC
    
        
        
class NPCParser:
    
    def __init__(self, filehandle, setclass=NPCSet):
        """
        Parser for parsing dota resource files for data on NPCs etc.
        
        Args:
            filehandle (file-like object): handle to the open file for reading data
            setclass (NPCSet or derivative): class to use for creating new NPCs
                from the resource file. Also determines the strings for matching
                full npc keys. Default is NPCSet
        """
        self.file = filehandle
        self._set = setclass
        
    # testing for start/end of a new subdict
    @staticmethod
    def is_opening(str): return str.strip().startswith(KWD_OPEN)
    
    @staticmethod
    def is_closing(str): return str.strip().startswith(KWD_CLOSE)
    
    # testing for comments
    @staticmethod
    def is_comment(str): return str.strip().startswith(KWD_COMMENT)
    
    # testing for an npc name key to start a new NPC
    def is_npc_key(self, str):
        if self._set._re.match(str) is None: return False
        else: return True
        
    # testing for any key, not necessarily an npc key
    def is_key(self, str):
        if self.is_comment(str): return False
        elif str.count(KWD_QUOTE) == 2: return True
        else: return False
        
    # testing a line to see if it contains data
    @staticmethod
    def is_data_line(line): return len(line.strip().split(KWD_QUOTE)) == 5

    # parsing/cleaning data lines in the file
    @staticmethod
    def parse_data_line(line):
        sline = line.split(KWD_QUOTE)
        return (sline[1], sline[3])
        
    # parsing key lines
    @staticmethod
    def parse_key_line(line): return line.strip().split(KWD_QUOTE)[1]
        
    def parse(self):
        """Parses the NPC file and returns a new NPCSet (or of whatever type calls it).""" 

        # process the file
        npcset = self._set()
        npcFlag = False
        prevKey = ''
        npcAttrKeys = []
        i = 0
        for line in self.file:
            i += 1
        
            # arrived at the end of a subdict, so step back a key
            if self.is_closing(line):
                npcAttrKeys = npcAttrKeys[:-1]
                if len(npcAttrKeys) <= 1:
                    npcFlag = False
                    try: npcset.add(npc)
                    except UnboundLocalError:
                        print 'You may be using the wrong regex to search for keys.'
                        raise
            
            # inside NPC's attributes section
            elif npcFlag:
                
                # arrived at a new subdict, so previous line was a key
                if self.is_opening(line): npcAttrKeys.append(prevKey)
                
                # arrived at a data line (key-value pair), add to attributes
                elif self.is_data_line(line):
                    key, value = self.parse_data_line(line)
                    npc.set(value, *(npcAttrKeys[1:] + [key]))
                    
                # arrived at a new key, store for subdicts
                elif self.is_key(line): prevKey = self.parse_key_line(line)
                    
            # arrived at a new npc's attributes
            elif self.is_npc_key(line):
                npcKey = self.parse_key_line(line)
                prevKey = npcKey
                npc = self._set._npc.from_name_and_key(npcKey, npcKey) ## note hero will be undefined for Abilities and wont be able to get_short
                npcFlag = True
                npcAttrKeys = []
                
        return npcset
        

class Tooltip:
    
    def __init__(self, **kwargs):
        """
        Container for organized tooltip info, for use in main().
        
        Args:
            **kwargs: (Optional) keyword arguments which are the descriptors
                of the tooltip. Those recognized automatically are:
                
                    cooldown (str): cooldown in seconds
                    
                    cost (str): cost of item
                
                    description (str): description text
                    
                    details (list): list of (descriptor, value) tuples 
                        describing custom details of the npc
                        
                    icon (str): url of icon image to use in tooltip
                        
                    lore (str): lore for the NPC
                    
                    mana (str): mana cost
                    
                    name (str): name of the npc being described
                        
                    notes (list): list of strings of notes
                    
                    scepter (str): special description for scepter upgrade
                    
                    scepter_mods(list): list of special modifications made to
                        the NPC upon scepter upgrade
        """
        P = {
            'name': '', 'description': '', 'details': [], 'cooldown': '',
            'mana': '', 'notes': [], 'lore': '', 'scepter': '', 
            'scepter_mods': [], 'cost': '', 'icon': ''
        }
        P.update(kwargs)
        self._keys = P.keys()
        for k in P: self.__dict__[k] = P[k]
        
    def __repr__(self):
        return '<%s for %s>:' % (self.__class__.__name__, self.name)
        
    @staticmethod
    def _is_lore(string): return (RGX_TTP_LORE.match(string) is not None)
    
    @staticmethod
    def _is_desc(string): return (RGX_TTP_DESC.match(string) is not None)
    
    @staticmethod
    def _is_aghdesc(string): return (RGX_TTP_AGDS.match(string) is not None)
    
    @staticmethod
    def _is_note(string): return (RGX_TTP_NOTE.match(string) is not None)
    
    @staticmethod
    def _is_aghmod(string): return (RGX_TTP_AGMD.match(string) is not None)
    
    @staticmethod
    def _is_variable(string): return (RGX_TTP_VARI.match(string) is not None)
    
    @staticmethod
    def _get_name_key(keys):
        if len(keys) == 0: return None
        mlen = min([len(k) for k in keys])
        return [k for k in keys if len(k) == mlen][0]
        
    @staticmethod
    def _descriptor_to_variable(string): return string.rsplit('$',1)[1]
        
    @staticmethod
    def from_npc(npc, variables={}):
        """Makes a new Tooltip from an NPC with extra data added in main()."""
        
        # start by getting a list of AbilitySpecial stuff that will be
        #   substituted for arbitrarily named keys
        abilitySpecial = {}
        if npc.has_key(MAN_KEY_ASP):
            for subdict in npc.get(MAN_KEY_ASP).values(): abilitySpecial.update(subdict)
            
        # now start defining the dictionary items for this tooltip
        nameKey = Tooltip._get_name_key(npc.tooltipData.keys())
        if nameKey is None: return Tooltip() # for npc's with no tooltip info
        name = npc.tooltipData[nameKey]
        ttDict = {  
            'name': name, 'notes': [],  'details': [], 'scepter_mods': [],
            'icon': ''
        }
        
        # image icon
        if npc.icon is not None: ttDict['icon'] = npc.icon
        
        # mana and cooldown (get special formatting in tooltips)
        if npc.has_key(MAN_KEY_MAN): ttDict['mana'] = npc.get(MAN_KEY_MAN)
        if npc.has_key(MAN_KEY_COO): ttDict['cooldown'] = npc.get(MAN_KEY_COO)
        if npc.has_key(MAN_KEY_CST): ttDict['cost'] = npc.get(MAN_KEY_CST)
        
        # other specially treated attributes
        for k in npc.tooltipData:
        
            # basic data without special formatting
            if Tooltip._is_lore(k): ttDict['lore'] = npc.tooltipData[k]
            elif Tooltip._is_desc(k): ttDict['description'] = npc.tooltipData[k]
            elif Tooltip._is_note(k): ttDict['notes'].append(npc.tooltipData[k])
            elif Tooltip._is_aghdesc(k): ttDict['scepter'] = npc.tooltipData[k]
            
            # scepter mods sometimes (usually?) have special ability
            #   descriptors that need handling
            elif Tooltip._is_aghmod(k):
                splitLen = len(k.lower().split(nameKey.lower() + KWD_TITLESEP, 1)[1]) # handles capitalization differences
                valueKey = k[-splitLen:]
                if valueKey in abilitySpecial:
                    value = abilitySpecial[valueKey]
                    descriptor = npc.tooltipData[k]
                    ttDict['scepter_mods'].append([descriptor, value])
            
            # match a special ability key to the descriptor in a key-value pair
            elif k <> nameKey:  
                split = k.lower().split(nameKey.lower() + KWD_TITLESEP, 1)
                if len(split) > 1:
                    splitLen = len(split[1]) # handles capitalization differences
                    valueKey = k[-splitLen:]
                    if valueKey in abilitySpecial:
                        value = abilitySpecial[valueKey]
                        descriptor = npc.tooltipData[k]
                        
                        # replace variables from resource tokens with descriptors
                        if Tooltip._is_variable(descriptor):
                            variableKey = Tooltip._descriptor_to_variable(descriptor)
                            descriptor = '+'
                            value += ' ' + variables[variableKey]

                        ttDict['details'].append([descriptor, value])
                    
        # other basic "details", e.g. spell immunity, damage type, etc that
        #   dont have their own tooltips in the resource file
        for k in TTP_NPC_BASIC:
            if npc.has_key(k):
                ttDict['details'].append([TTP_NPC_BASIC[k], TTP_NPC_LAM(k, npc.get(k))])
                
        return Tooltip(**ttDict)
        
    def to_pystr(self):
        """
        Returns a string representation of a dictionary containing info for this tooltip.
        
        CAUTION: Highly Experimental
        """
        pydict = dict((k, self.__dict__[k]) for k in self._keys)
        return str(pydict)
        
    def to_jsstr(self):
        """
        Returns a string representation of a dictionary containing info for
        this tooltip that could be loaded into a javascript application.
        
        CAUTION: Highly Experimental
        """
        return self.to_pystr().replace('u"', '"').replace("u'", "'")
        
        
        
class ImageMap:
    KEY = None
    REMDIR = ''
    LOCDIR = ''

    def __init__(self, imageMap):
        """
        Container for organized npc images.
        
        Args:
            imageMap (dict): mapping from npc short name to image hosting path
                (local file or url)
        """
        self.map = imageMap
        
        
    def get(self, key): return self.map[key]
    def keys(self): return self.map.keys()
    def values(self): return self.map.values()
    
    
    @staticmethod
    def file_to_key(localFile):
        return os.path.splitext(os.path.basename(localFile))[0]
        
        
    @classmethod
    def local_to_url(cls, local, host=IMG_DEF_HOST, subdir=None, locdir=None):
    
        # update defaults
        if subdir is None: subdir = cls.REMDIR
        if locdir is None: locdir = cls.LOCDIR
        
        # skip the parts of the local file path that are not part of the
        # remote path and build the remote path from the rest
        local = os.path.relpath(local)
        locdir = os.path.relpath(locdir)
        fileSplit = local.split(os.path.sep)
        dirSplit = locdir.split(os.path.sep)
        dn = len(dirSplit)
        fn = len(fileSplit)
        urlParts = host.split('/')
        urlParts.extend(subdir.split('/'))
        i = 0
        while ((i < dn) and (i < fn) and (fileSplit[i] == dirSplit[i])): i += 1
        if (i == fn): raise ValueError('Cannot build url for this file with the given parameters.')
        urlParts.extend(fileSplit[i:])
        
        # build the url and remove any duplicate slashes
        url = '/'.join(urlParts)
        url = url[::-1].replace('//','/',url.count('//')-1)[::-1]
        return url
        
        
    @classmethod
    def from_local(cls, files, l2u=None, f2k=None):
        if l2u is None: l2u = cls.local_to_url
        if f2k is None: f2k = cls.file_to_key
        return cls(dict((f2k(f), l2u(f)) for f in files))
        

        
class HeroImageMap(ImageMap):
    KEY = 'heroes'
    REMDIR = IMG_DEF_SUBDIR[KEY]
    LOCDIR = VPK_DEF_IMAGES[KEY][0]
    
    
    
class ItemImageMap(ImageMap):
    KEY = 'items'
    REMDIR = IMG_DEF_SUBDIR[KEY]
    LOCDIR = VPK_DEF_IMAGES[KEY][0]

    
    
class AbilityImageMap(ImageMap):
    KEY = 'abilities'
    REMDIR = IMG_DEF_SUBDIR[KEY]
    LOCDIR = VPK_DEF_IMAGES[KEY][0]



# #############################################################################
# FUNCTIONS ###################################################################
# #############################################################################

def download_resources(urls=None, destination='.'):
    """
    Downloads a list of text files at the given urls to the give folder.
    
    Args:
        urls (list): (optional) list of urls to download. Default (None) 
            downloads urls of resource files from dotabuff
            
        desination (list): (optional) directory to save files. By default,
            saves to current directory ('.')
        
    Returns:
        list: paths to downloaded files
    """
    import urllib2, urlparse
    if urls is None: urls = [DEF_RESOURCE_FILES[k]['url'] for k in DEF_RESOURCE_FILES]
    paths = []
    for url in urls:
        request = urllib2.Request(url)
        connection = urllib2.urlopen(request)
        data = connection.read()
        basename = os.path.basename(urlparse.urlparse(url).path)
        dest = os.path.join(destination, basename)
        with open(dest, 'w') as fh: fh.write(data)
        paths.append(dest)
        
    return paths
    

def vpk_extract(vpk=VPK_DEF_VPK, files=VPK_DEF_FIL):
    """
    Extracts files from a vpk.
    
    Args:
        vpk (str): (optional) path to the vpk file to extract from. Default is
            VPK_DEF_VPK
        
        files (list): (optional) list of file paths to extract, where the path
            is relative to the root of the vpk. If a file path is determined
            to be a directory, the whole directory and its sub-directories will
            be extracted. Default is VPK_DEF_FIL
            
    Returns:
        list: paths to extracted files, which will have the same directory
            structure as the vpk
    """
    # imports
    import subprocess, os
    
    # get a listing of files in the vpk
    call = VPK_CALL_LISTFILES + [r'%s' % vpk]
    p = subprocess.Popen(call, stdout=subprocess.PIPE)
    out, err = p.communicate()
    vpkFiles = [s.strip() for s in out.strip().split()]
    
    # match requested files to those given in the vpk
    files = [os.path.relpath(f) for f in files]
    toExtract = []
    for vpkFile in vpkFiles:
        vpkRel = os.path.relpath(vpkFile)
        for requestedFile in files:
            if vpkRel.startswith(requestedFile):
                toExtract.append(vpkFile)
                
    # make temporary directories for vpk.exe to extract to
    for f in toExtract:
        if not os.path.exists(os.path.dirname(f)):
            os.makedirs(os.path.dirname(f))
    
    # extract files
    for f in toExtract:
        call = VPK_CALL + [r'%s' % vpk] + [r'%s' % f]
        subprocess.call(call)
    
    # check that all the files now exist
    for f in toExtract:
        if not os.path.exists(f):
            raise ValueError('File not successfully extracted: %s' % f)
            
    return toExtract
    
    
def write_server_data(heroes, abilities, items, units, outfile):
    """
    Writes a dict to a Python cPickle for the server to process video and
    replay data. For use in main()
    """
    import cPickle
    data = {'ability_order': {}}
    for a in abilities:
        if a.has_key('ID'):
            data[a.get_key().lower()] = a.get('ID').lower()
            
    for h in heroes:
        name = h.get_name().lower()
        key = h.get_key().lower()
        data[name] = key
        data[key] = name
        data['ability_order'][name] = h.ablorder
        for a in h.abilities.values():
            data[a.get_key().lower()] = a.get('ID').lower()
            
    for i in items:
        data[i.get_key().lower()] = i.get('ID').lower()
        
    for u in units:
        name = u.get_name().lower()
        key = u.get_key().lower()
        data[name] = key
        data[key] = name
        data['ability_order'][name] = u.ablorder
        for a in u.abilities.values():
            data[a.get_key().lower()] = a.get('ID').lower()

    with open(outfile, 'wb') as fh: cPickle.dump(data, fh)
    return outfile
    

def write_client_data(heroes, abilities, items, units, outfile):
    """
    Writes a javascript file for the client to use when interpreting
    data sent from the server.
    """
    # compile tooltip dictionary for client
    data = {}
    for a in abilities:
        if a.has_key('ID'):
            data[a.get('ID')] = a.tooltip.to_jsstr()
        
    for hero in heroes:
        for ability in hero.abilities.values(): 
            data[ability.get('ID')] = ability.tooltip.to_jsstr()
            
    for item in items:
        data[item.get('ID')] = item.tooltip.to_jsstr()
        
    for unit in units:
        for ability in unit.abilities.values():
            data[ability.get('ID')] = ability.tooltip.to_jsstr()
        
    # write the file
    fh = open(outfile, 'w')
    fh.write('var DOTAPEDIA = {')
    for key in sorted(data.keys()):
        fh.write('\n    \'%s\': %s,' % (key, data[key]))
    fh.write('\n};')
    fh.close()
    return outfile
    
    
def main():

    # short function to merge a bunch of dictionaries
    def merge_dicts(*dicts):
        a = {}
        for d in dicts: a.update(d)
        return a
        
    # short function to get local OR URL file depending on which is available
    def get_file(key):
        if key == 'items':
            try: return vpk_extract(files=VPK_DEF_FIL)[0]
            except: return download_resources(urls=[DEF_RESOURCE_FILES[key]['url']])[0]
                
        elif key == 'images_heroes':
            try: return vpk_extract(files=VPK_DEF_IMAGES['heroes'])
            except: print 'Unable to extract hero resource images.'
            
        elif key == 'images_items':
            try: return vpk_extract(files=VPK_DEF_IMAGES['items'])
            except: print 'Unable to extract item resource images.'
            
        elif key == 'images_abilities':
            try: return vpk_extract(files=VPK_DEF_IMAGES['abilities'])
            except: print 'Unable to extract ability resource images.'
            
        else:
            if os.path.exists(DEF_RESOURCE_FILES[key]['local']): return DEF_RESOURCE_FILES[key]['local']
            else: return download_resources(urls=[DEF_RESOURCE_FILES[key]['url']])[0]
            
    # short function to only get the tooltip data associated with an npc
    def get_tooltips(tooltips, npcKey, otherKeys):
        regex = re.compile(MAN_RGX % (KWD_TITLESEP, npcKey, KWD_TITLESEP, KWD_TITLESEP, npcKey))
        tooltipKeys = []
        
        # match tooltip keys that have this ability key in them, but 
        #   omit those longer tooltip keys that have this ability key
        #   as a substring (reversed case should already be handled 
        #   by regex)
        for k in tooltips:
            if regex.match(k) is not None:
                keep = True
                for kk in otherKeys:
                    if (npcKey <> kk) and (npcKey in kk) and (kk in k):
                        keep = False
                        break
                        
                if keep: tooltipKeys.append(k)
                
        thisTooltips = dict((k, tooltips[k]) for k in tooltipKeys)
        return thisTooltips
        
        
    # parse auxiliary datasets
    heroes = HeroNPCSet.from_txt(get_file('heroes'), encoding='utf-8')
    items = ItemNPCSet.from_txt(get_file('items'), encoding='utf-8')
    units = UnitNPCSet.from_txt(get_file('units'), encoding='utf-8')
    abilities = AbilityNPCSet.from_txt(get_file('abilities'), encoding='utf-8')
    tokenFile = get_file('resource')
    try: tokens = TokenSet.from_txt(tokenFile, encoding='utf-16').get_by_key('Tokens')
    except UnicodeDecodeError:
        tokens = TokenSet.from_txt(tokenFile, encoding='utf-8').get_by_key('Tokens')
    
    # extract descriptions from the resource file
    heroLocalNames = dict((k, tokens.get(k)) for k in tokens.search_all_keys(RGX_HNAME))
    unitLocalNames = dict((k, tokens.get(k)) for k in tokens.search_all_keys(RGX_UNAME))
    variables = dict((MAN_V2K(k), tokens.get(k)) for k in tokens.search_all_keys(RGX_VAR))
    abilityTooltips = dict((k, tokens.get(k)) for k in tokens.search_all_keys(RGX_DES_ABILITY))
    itemTooltips = dict((k, tokens.get(k)) for k in tokens.search_all_keys(RGX_DES_ITEM))
    
    # extract image assets
    imagesHeroes = {}
    imagesItems = {}
    imagesAbilities = {}
    if EXTRACT_IMAGES:
        imagesHeroes = HeroImageMap.from_local(get_file('images_heroes'))
        imagesItems = ItemImageMap.from_local(get_file('images_items'))
        imagesAbilities = AbilityImageMap.from_local(get_file('images_abilities'))
        
    # get ability tooltips
    for ability in abilities:
        ability.tooltipData = get_tooltips(abilityTooltips, ability.get_key(), [])
            
        # substitute ability attributes in for placeholders in the tooltips
        if ability.has_key(MAN_KEY_ASP): 
            abilitySpecialSubDicts = [ability.get(MAN_KEY_ASP, k) for k in ability.get(MAN_KEY_ASP)]
            abilitySpecial = merge_dicts(*[[{k:r[k]}  for k in r if k not in MAN_ABS_SKP][0] for r in abilitySpecialSubDicts])
            
            # attempt a replace for every pair of placeholder with tooltip data
            for attributeKey in abilitySpecial:
                for tooltipDataKey in ability.tooltipData:
                    tooltip = ability.tooltipData[tooltipDataKey]
                    
                    # replace the standard %var% style placeholders
                    tooltip = tooltip.replace(MAN_KWD_VAR.format(k=attributeKey), abilitySpecial[attributeKey])
                    
                    # replace other characters escapes
                    tooltip = MAN_KWD_PCT.sub('%', tooltip)
                    tooltip = MAN_KWD_LIN.sub('<br>', tooltip)
                    ability.tooltipData[tooltipDataKey] = tooltip
                    
        # get local name of ability from tooltip data
        if len(ability.tooltipData) > 0:
            minLen = min([len(k) for k in ability.tooltipData])
            localNameKey = [k for k in ability.tooltipData if len(k) == minLen][0]
            ability.set_name(ability.tooltipData[localNameKey])
            
        # store the icon
        try: ability.icon = imagesAbilities.get(ability.get_key())
        except: ability.icon = None
            
        # create a tooltip for the ability
        ability.tooltip = Tooltip.from_npc(ability, variables)
        

            
    # merge hero and ability data
    for hero in heroes:
        hero.set_name(heroLocalNames.get(hero.get_key(), hero.get_name()))
        
        # get hero ability attributes
        abilityOrder = sorted(hero.search_all_keys(re.compile(MAN_KEY_ABL)), key=lambda s: int(s[7:]))
        hero.ablorder = [hero.get(k) for k in abilityOrder]
        hero.abilities = {}
        for k in hero.ablorder:
            if abilities.has_key(k): hero.abilities[k] = abilities.get_by_key(k)
            
        # store the icon
        try: hero.icon = imagesHeroes.get(hero.get_key())
        except: hero.icon = None
        
        # grab tooltip info for abilities
        for abilityKey in hero.abilities:
            ability = hero.abilities[abilityKey]
            ability.set_hero(hero)
            if hasattr(ability, 'tooltipData'): continue # dont re-make tooltips
            ability.tooltipData = get_tooltips(abilityTooltips, abilityKey, hero.abilities.keys())
            
            # substitute ability attributes in for placeholders in the tooltips
            if ability.has_key(MAN_KEY_ASP): 
                abilitySpecialSubDicts = [ability.get(MAN_KEY_ASP, k) for k in ability.get(MAN_KEY_ASP)]
                abilitySpecial = merge_dicts(*[[{k:r[k]}  for k in r if k not in MAN_ABS_SKP][0] for r in abilitySpecialSubDicts])
                
                # attempt a replace for every pair of placeholder with tooltip data
                for attributeKey in abilitySpecial:
                    for tooltipDataKey in ability.tooltipData:
                        tooltip = ability.tooltipData[tooltipDataKey]
                        
                        # replace the standard %var% style placeholders
                        tooltip = tooltip.replace(MAN_KWD_VAR.format(k=attributeKey), abilitySpecial[attributeKey])
                        
                        # replace other characters escapes
                        tooltip = MAN_KWD_PCT.sub('%', tooltip)
                        tooltip = MAN_KWD_LIN.sub('<br>', tooltip)
                        ability.tooltipData[tooltipDataKey] = tooltip
                        
            # get local name of ability from tooltip data
            if len(ability.tooltipData) > 0:
                minLen = min([len(k) for k in ability.tooltipData])
                localNameKey = [k for k in ability.tooltipData if len(k) == minLen][0]
                ability.set_name(ability.tooltipData[localNameKey])
                
            # store the icon
            try: ability.icon = imagesAbilities.get(ability.get_key())
            except: ability.icon = None
                
            # create a tooltip for the ability
            ability.tooltip = Tooltip.from_npc(ability, variables)
                    
    # merge items with their tooltip data
    for item in items:
    
        # get the local name of the item. Because the local name keys dont
        #   usually have the same case as the tooltips, we have to do a
        #   special compile to match this particular item
        localNameKeyCandidates = tokens.search_all_keys(re.compile(PRE_DES_ITEM + item.get_short(), re.IGNORECASE))
        nkLen = [len(k) for k in localNameKeyCandidates]
        localNameKey = [k for k in localNameKeyCandidates if len(k) == min(nkLen)]
        if len(localNameKey) > 0:
            localName = tokens.get(localNameKey[0])
            item.set_name(localName)

        # get the item tooltip data
        item.tooltipData = get_tooltips(itemTooltips, item.get_key(), items.keys())
        
        # substitute item attributes in for placeholders in the tooltips
        if item.has_key(MAN_KEY_ASP):
            abilitySpecialSubDicts = [item.get(MAN_KEY_ASP, k) for k in item.get(MAN_KEY_ASP)]
            abilitySpecial = merge_dicts(*[[{k:r[k]}  for k in r if k not in MAN_ABS_SKP][0] for r in abilitySpecialSubDicts])
            
            # attempt a replace for every pair of placeholder with tooltip data
            for attributeKey in abilitySpecial:
                for tooltipDataKey in item.tooltipData:
                    tooltip = item.tooltipData[tooltipDataKey]
                    
                    # replace the standard %var% style placeholders
                    tooltip = tooltip.replace(MAN_KWD_VAR.format(k=attributeKey), abilitySpecial[attributeKey])
                    
                    # replace other characters escapes
                    tooltip = MAN_KWD_PCT.sub('%', tooltip)
                    tooltip = MAN_KWD_LIN.sub('<br>', tooltip)
                    item.tooltipData[tooltipDataKey] = tooltip
        
        # store the icon
        try: item.icon = imagesItems.get(item.get_short())
        except: item.icon = None
                    
        # create a tooltip for the item
        item.tooltip = Tooltip.from_npc(item, variables)
                    
    # merge unit and ability data
    for unit in units:
        unit.set_name(unitLocalNames.get(unit.get_key(), unit.get_name()))
        
        # get unit ability attributes
        unitShortName = unit.get_short()
        abilityOrder = sorted(unit.search_all_keys(re.compile(MAN_KEY_ABL)), key=lambda s: int(s[7:]))
        unit.ablorder = [unit.get(k) for k in abilityOrder]
        unit.abilities = {}
        for k in unit.ablorder:
            if abilities.has_key(k): unit.abilities[k] = abilities.get_by_key(k)
            
        # store the icon
        try: unit.icon = imagesHeroes.get(unit.get_key())
        except: unit.icon = None
        
        # grab tooltip info for abilities
        for abilityKey in unit.abilities:
            ability = unit.abilities[abilityKey]
            ability.set_hero(unit)
            if hasattr(ability, 'tooltipData'): continue # dont re-make tooltips
            ability.tooltipData = get_tooltips(abilityTooltips, abilityKey, unit.abilities.keys())
            
            # substitute ability attributes in for placeholders in the tooltips
            if ability.has_key(MAN_KEY_ASP): 
                abilitySpecialSubDicts = [ability.get(MAN_KEY_ASP, k) for k in ability.get(MAN_KEY_ASP)]
                abilitySpecial = merge_dicts(*[[{k:r[k]}  for k in r if k not in MAN_ABS_SKP][0] for r in abilitySpecialSubDicts])
                
                # attempt a replace for every pair of placeholder with tooltip data
                for attributeKey in abilitySpecial:
                    for tooltipDataKey in ability.tooltipData:
                        tooltip = ability.tooltipData[tooltipDataKey]
                        
                        # replace the standard %var% style placeholders
                        tooltip = tooltip.replace(MAN_KWD_VAR.format(k=attributeKey), abilitySpecial[attributeKey])
                    
                        # replace other characters escapes
                        tooltip = MAN_KWD_PCT.sub('%', tooltip)
                        tooltip = MAN_KWD_LIN.sub('<br>', tooltip)
                        ability.tooltipData[tooltipDataKey] = tooltip
            
            # store the icon
            try: ability.icon = imagesAbilities.get(ability.get_key())
            except: ability.icon = None
        
            # create a tooltip for the ability
            ability.tooltip = Tooltip.from_npc(ability, variables)
                        
    # write hero key mapping for server
    write_server_data(heroes, abilities, items, units, MAN_FIL_SRV)
    
    # write tooltip data for items and abilities for client
    write_client_data(heroes, abilities, items, units, MAN_FIL_CLN)


    
if __name__ == '__main__': main()