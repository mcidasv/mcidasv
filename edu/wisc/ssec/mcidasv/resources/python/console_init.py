import sys, os

# This is an ugly hack to deal with Jython's sys.path strangeness: if you 
# want to import a non-compiled python module contained in a JAR, sys.path 
# must contain something like "/path/to/your.jar/path/to/module"
_cwd = os.getcwd()
_mcv_jar = os.path.join(_cwd, 'mcidasv.jar')
_idv_jar = os.path.join(_cwd, 'idv.jar')

# however, if we're not inside a jar just prepend the current directory.
if not os.path.exists(_mcv_jar):
    _mcv_jar = _cwd

# similar for idv.jar; assumption of the idv source living in "../idv"
if not os.path.exists(_idv_jar):
    _idv_jar = os.path.join(_cwd, '../idv')

# NOTE: the paths appended to sys.path cannot have a trailing '/'!
_mcv_python = _mcv_jar+'/edu/wisc/ssec/mcidasv/resources/python'
_idv_python = _idv_jar+'/ucar/unidata/idv/resources/python'

sys.path.append(_mcv_python)
sys.path.append(_mcv_python+'/linearcombo')
sys.path.append(_mcv_python+'/utilities')
sys.path.append(_idv_python)

from edu.wisc.ssec.mcidasv import McIDASV
_mcv = McIDASV.getStaticMcv()

import imageFilters
import shell as idvshell
#import background
#import interactive
from background import setViewSize, getColorTable, colorTableNames, allColorTables, firstWindow, allWindows, firstDisplay, allDisplays, createLayer, allLayerTypes, allProjections, projectionNames, getProjection, load_enhancement, load_map, annotate, apply_colorbar, write_image, collect_garbage 
from interactive import see, ncdump, ncdumpToString, dumpObj

_user_python = os.path.join(_mcv.getStore().getUserDirectory().toString(), 'python')
if os.path.exists(_user_python):
    sys.path.append(_user_python)
    for mod in os.listdir(_user_python):
        modname, ext = os.path.splitext(mod)
        if ext == '.py':
            globals()[modname] = __import__(modname, globals(), locals(), ['*'], -1)
        del modname, ext

del _cwd, _mcv_jar, _idv_jar, _user_python, _mcv_python, _idv_python

