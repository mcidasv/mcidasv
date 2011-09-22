from __future__ import with_statement

try:
    import json
except ImportError:
    import simplejson as json

import os
import sys

from jinja2 import Environment
from jinja2 import PackageLoader
from jinja2 import FileSystemLoader
from jinja2 import Template

def _expandpath(path):
    return os.path.abspath(os.path.normpath(os.path.expanduser(os.path.expandvars(path))))

def deserializeDocs(filename):
    with open(filename, 'r') as fp:
        return json.load(fp)
    return ''

def renderTemplate(docs, templateFilename='csstest.html'):
    env = Environment(loader=FileSystemLoader('./templates'))
    template = env.get_template(templateFilename)
    return template.render(docs=docs)

def main():
    if len(sys.argv) >= 2:
        jsonFile = sys.argv[1]
    else:
        jsonFile = './inception.json'
    
    jsonFile = _expandpath(jsonFile)
    docs = deserializeDocs(jsonFile)
    print renderTemplate(docs)

if __name__ == '__main__':
    main()