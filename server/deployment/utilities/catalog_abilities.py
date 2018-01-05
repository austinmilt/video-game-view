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

"""Catalogs order abilities of dota 2 heroes for feeding into javascript tooltips."""

# CONSTANTS ################################################################# #

# is_valid_hero_url()
VLD_KWD_URL = u'http://www.dota2.com/hero'

# get_hero_urls()
PCK_DEF_ICO_SCH = {'class': 'heroPickerIconLink'}
PCK_DEF_URL_TAG = 'href'

# catalog_hero()
CAT_DEF_ABL_SCH = {'class': 'overviewAbilityImg abilityIconWithTooltip'}
CAT_DEF_ABL_NAM = 'abilityname'
CAT_DEF_URL = r'http://www.dota2.com/heroes/'

# catalog_heros()
CHR_KWD_SEP = '/'
CHR_KWD_HDR = ['Hero', 'Abilities']
CHR_KWD_DEL = ','
CHR_DEF_ATT = 6
CHR_DEF_PAU = 5
CHR_DEF_RSE = True

# abilities_to_js()
A2J_KWD_ABL_NMB = dict((i, dict((j, '%i'%j) for j in range(i))) for i in range(21))

# other stuff
URLLIB_HEADERS = {'User-Agent': 'Magic Browser'}
SOUP_PARSER = 'html.parser'




# FUNCTIONS ################################################################# #

# ~~ is_valid_hero_url() ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ #
def is_valid_hero_url(url):
    """
    IS_VALID_HERO_URL() tests the url to see if we expect to be able to get
    hero ability names from the html.
    
    Args:
        url (str): url to test
        
    Returns:
        bool: True if it's a valid url, False otherwise
    """
    return url.startswith(VLD_KWD_URL)
    
    
    
# ~~ get_hero_urls ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ #
def get_hero_urls(url=CAT_DEF_URL, search=PCK_DEF_ICO_SCH, tag=PCK_DEF_URL_TAG):
    """
    GET_HERO_URLS() gets the urls of hero pages from the hero selection url
    
    Args:
        url (str): (optional) url of the hero picker page from which to get 
            hero urls. Default is CAT_DEF_URL.
            
        search (dict): (optional) dictionary to feed a BeautifulSoup object
            to search for all html snippets with hero page urls in them.
            Default is PCK_DEF_ICO_SCH (see script).
            
        tag (str): (optional) tag in html snippets for hero urls. Default
            is PCK_DEF_URL_TAG (see script).
            
    Returns:
        list: list of urls (str) of hero pages
    """
    # imports
    import urllib2
    from bs4 import BeautifulSoup
    
    # read the website
    request = urllib2.Request(url, headers=URLLIB_HEADERS)
    connection = urllib2.urlopen(request)
    html = connection.read()
    
    # parse html into ability names
    soup = BeautifulSoup(html, SOUP_PARSER)
    searchResults = soup.find_all(**search)
    heroes = [s.get(tag) for s in searchResults]
    return heroes
    
    
  
# ~~ catalog_hero() ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ #
def catalog_hero(url, search=CAT_DEF_ABL_SCH, tag=CAT_DEF_ABL_NAM):
    """
    CATALOG_SINGLE() scrubs a website for a hero's ability descriptions
    
    Args:
        url (str): url to scrub (should start with http://www.dota2.com/hero)
        
        search (dict): (optional) dictionary to feed a BeautifulSoup object
            to search for all html snippets with ability names in them.
            Default is CAT_DEF_ABL_SCH (see script).
            
        tag (str): (optional) tag in html snippets for ability names. Default
            is CAT_DEF_ABL_NAM (see script).
        
    Returns:
        list: list of str of ability names returned from the website
    """
    # imports
    import urllib2
    from bs4 import BeautifulSoup
    
    # make sure the url is valid
    assert is_valid_hero_url(url), 'Invalid url for detecting hero abilities.'
    
    # read the website
    request = urllib2.Request(url, headers=URLLIB_HEADERS)
    connection = urllib2.urlopen(request)
    html = connection.read()
    
    # parse html into ability names
    soup = BeautifulSoup(html, SOUP_PARSER)
    searchResults = soup.find_all(**search)
    abilities = [s.get(tag) for s in searchResults]
    return abilities
    

    
# ~~ abilities_to_js() ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ #
def abilities_to_js(abilities):
    """
    ABILITIES_TO_JS() converts a dict of hero abilities to a valid javascript
    object string for writing out the ability library.
    
    Args:
        abilities (dict): hero abilities as returned by catalog_heroes()
        
    Returns:
        a string formatted for writing a javascript file
    """
    heroJS = []
    for hero in sorted(abilities.keys()):
        
        # create the object element for each ability
        abilityJS = []
        n = len(abilities[hero])
        for i in range(n):
            ability = abilities[hero][i]
            abilityJS.append('\n\t\t\'%s\': \'%s\'' % (A2J_KWD_ABL_NMB[n][i], ability))
            
        # join the abilities into an object for the hero
        heroJS.append('\n\t\'%s\': {%s\n\t}' % (hero.upper(), ','.join(abilityJS)))
        
    # join the heroes into an object
    js = 'var ABILITY_ORDER = {%s\n}' % ','.join(heroJS)
    return js

    

# ~~ catalog_heroes() ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ #
def catalog_heroes(
    url=CAT_DEF_URL, file=None, attempts=CHR_DEF_ATT, pause=CHR_DEF_PAU, 
    doraise=CHR_DEF_RSE
):
    """
    CATALOG_HEROES() catalogs the abilities of heroes from the Dota 2 website.
    
    Args:
        url (list): (optional) url of the Dota 2 hero picker from which to
            draw hero names. Default is CAT_DEF_URL (see script).
        
        file (str): (optional) path to output javascript file with catalog to 
            write. Default (None) is to not write the file.
            
        attempts (int): (optional) number of times to attempt getting a hero's
            abilities (sometimes fails because access is too closely spaced
            between heroes) before giving up. Default is CHR_DEF_ATT (see 
            script).
            
        pause (float): (optional) number of seconds to wait between attempts
            to get hero abilities. Default is CHR_DEF_PAU (see script).
            
        doraise (bool): (optional) flag to indicate whether to fail with
            a RuntimeError if no abilities were retrieved after [attempts].
            Default is CHR_DEF_RSE (see script).
        
    Returns:
        dict: dictionary of hero abilities, where keys are hero names and
            values are ability names as returned by catalog_hero()
    """
    # imports
    from urlparse import urlparse
    from time import sleep
    
    # short function to parse a hero url to get the hero's name
    def url_to_hero(address):
        return urlparse(address).path.strip(CHR_KWD_SEP).split(CHR_KWD_SEP)[-1].replace('_', ' ')
        
    # catalog all heroes' abilities
    urls = get_hero_urls(url)
    results = {}
    for heroURL in urls:
        hero = url_to_hero(heroURL)
        abilities = catalog_hero(heroURL)
        
        # try to get hero abilties for some number of attempts. If blocked
        #   raise an exception
        i = 1
        while (len(abilities) == 0) and (i < attempts):
            sleep(pause)
            i += 1
            print 'Failed on %s. Attempt %i of %i.' % (hero, i, attempts)
            abilities = catalog_hero(heroURL)
            
        if doraise and (i == attempts):
            raise RuntimeError('Unable to retrieve abilities for %s' % hero)
                
        results[hero] = abilities
    
    # write to file
    if file is not None:
        js = abilities_to_js(results)
        with open(file, 'w') as fh: fh.write(js)
    
    return results
    
    
if __name__ == '__main__':
    import os
    here = os.path.dirname(os.path.abspath(__file__))
    outfile = os.path.join(here, '..', 'chrome_extension', 'ability_order.js')
    results = catalog_heroes(file=outfile)