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
Downloads javascripts and css files needed for formatting tooltips. Also edits scripts where needed
"""

# global imports
import os

# CONSTANTS
HERE = os.path.dirname(os.path.abspath(__file__))
CE = os.path.join(HERE, '..', '..', '..', 'client', 'deployment', 'chrome', 'page', 'scripts', 'dotapedia.js')
JS_HLIB = os.path.join(CE, 'dotapedia.js')
SCRIPTS = [
    [r'https://www.dota2.com/jsfeed/heropediadata?feeds=itemdata,abilitydata,herodata&v=3714588tUPGwkviXt3e&l=english&callback=HeropediaDFReceive:formatted', JS_HLIB]
]


def fix_hlib(hlib=JS_HLIB):
    hlibstr = open(hlib, 'r').read()
    hlibstr = 'DOTAPEDIA = ' + hlibstr[hlibstr.find('(')+1:hlib.rfind(')')-1] + ';'
    with open(hlib, 'w') as fh: fh.write(hlibstr)


def main(scripts=SCRIPTS):
    import urllib2
    for source, dest in scripts:
        request = urllib2.Request(source)
        connection = urllib2.urlopen(request)
        data = connection.read()
        with open(dest, 'w') as fh: fh.write(data)
        

if __name__ == '__main__':
    main()
    fix_hlib()