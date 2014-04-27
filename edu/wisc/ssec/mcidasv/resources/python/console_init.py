from __future__ import with_statement
import sys
import os

import java
from java.lang import Integer, System

# This is an ugly hack to deal with Jython's sys.path strangeness: if you
# want to import a non-compiled python module contained in a JAR, sys.path
# must contain something like "/path/to/your.jar/path/to/module"
def _mcvinit_classpath_hack():
    """Attempts to locate mcidasv.jar, idv.jar, and visad.jar.
    
    This function will look for the JARs within the classpath, but will also
    try within the following (platform-dependent) paths:
        Windows: "C:\Program Files\McIDAS-V-System"
        OS X: "/Applications/McIDAS-V-System"
        Linux: ""
        
    Returns:
        A dictionary with "mcidasv", "idv", and "visad" keys.
    """
    classpath = System.getProperty('java.class.path')
    
    # supply platform-dependent paths to various JAR files
    # (in case they somehow are not present in the classpath)
    osname = System.getProperty('os.name')
    current_dir = os.path.normpath(os.getcwd())
    mcv_jar = os.path.join(current_dir, 'mcidasv.jar')
    idv_jar = os.path.join(current_dir, 'idv.jar')
    visad_jar = os.path.join(current_dir, 'visad.jar')
    
    # allow the actual classpath to override any default JAR paths
    for entry in classpath.split(System.getProperty('path.separator')):
        if entry.endswith('mcidasv.jar'):
            mcv_jar = entry
        elif entry.endswith('idv.jar'):
            idv_jar = entry
        elif entry.endswith('visad.jar'):
            visad_jar = entry
            
    return {'mcidasv': mcv_jar, 'idv': idv_jar, 'visad': visad_jar}
    
def _mcvinit_jythonpaths():
    """Creates a list of paths containing required Python source code.
    
    This function uses _mcvinit_classpath_hack() to locate JARs and then uses
    those paths to create paths to known Python source code within visad.jar,
    idv.jar, and mcidasv.jar.
    
    Returns:
        A list of paths suitable for appending to Jython's sys.path.
    """
    jars = _mcvinit_classpath_hack()
    return [
        jars['visad'],
        jars['visad'] + '/visad/python',
        jars['idv'],
        jars['idv'] + '/ucar/unidata/idv/resources/python',
        jars['mcidasv'],
        jars['mcidasv'] + '/edu/wisc/ssec/mcidasv/resources/python',
        jars['mcidasv'] + '/edu/wisc/ssec/mcidasv/resources/python/utilities',
        jars['mcidasv'] + '/edu/wisc/ssec/mcidasv/resources/python/linearcombo',
    ]
    
for jythonpath in _mcvinit_jythonpaths():
    if not jythonpath in sys.path:
        sys.path.append(jythonpath)
        
# this is intentionally the first IDV/McV thing imported
from edu.wisc.ssec.mcidasv import McIDASV
_mcv = McIDASV.getStaticMcv()

# make sys.argv look as if the user ran "jython file.py arg1 ... argN"
from edu.wisc.ssec.mcidasv import ArgumentManager
argManager = _mcv.getArgsManager()
if argManager.hasJythonArguments():
    sys.argv = [argManager.getJythonScript()] + argManager.getJythonArguments()
    
# need to get some IDV-specifc init done
from ucar.unidata.idv.ui import ImageGenerator
islInterpreter = ImageGenerator(_mcv)

from edu.wisc.ssec.mcidasv.data.hydra import Statistics

# TODO(jon): is this really what we want!?
from visad.python.JPythonMethods import *

from ucar.visad import Util

from ucar.unidata.util import StringUtil

from ucar.unidata.data import DataUtil
from ucar.unidata.data import DataSelection
from ucar.unidata.data import GeoLocationInfo
from ucar.unidata.data import GeoSelection

from ucar.unidata.data.grid import GridMath
from ucar.unidata.data.grid import GridUtil
from ucar.unidata.data.grid import DerivedGridFactory
from ucar.unidata.data.grid import GridTrajectory

try:
    import imageFilters
except ImportError, e:
    print 'Error attempting to import imageFilters:', e
    print 'sys.path contents:'
    for i, path in enumerate(sys.path):
        print i, path
        
try:
    import shell as idvshell
except ImportError, e:
    print 'Error attempting to import idvshell:', e
    print 'sys.path contents:'
    for i, path in enumerate(sys.path):
        print i, path
        
from see import see

from decorators import deprecated

from background import (
    activeDisplay, allActions, allColorTables, allDisplays, allFontNames,
    allLayerTypes, allProjections, allWindows, boomstick, collectGarbage,
    colorTableNames, firstDisplay, firstWindow, getColorTable, getProjection,
    managedDataSource, pause, performAction, projectionNames, removeAllData,
    removeAllLayers, setViewSize, _MappedAreaImageFlatField, writeImageAtIndex,
)

from mcvadde import (
    enum, DEFAULT_ACCOUNTING, CoordinateSystems, Places, getDescriptor,
    oldADDEImage, listADDEImages, params1, params_area_coords,
    params_image_coords, params_sizeall, disableAddeDebug, enableAddeDebug,
    isAddeDebugEnabled, LATLON, AREA, IMAGE, ULEFT, CENTER, getADDEImage,
    makeLocalADDEEntry,
)

from interactive import (
    describeActions, dumpObj, editFile, expandpath, ncdump, ncdumpToString,
    today, tomorrow, yesterday, _expandpath, _today, _tomorrow, _yesterday,
    getLogLevel, setLogLevel,
)

from islformatters import (
    ImageFormatting, Matte, ImageOverlay, TextOverlay, Clip, Colorbar,
    TransparentColor, TransparentBackground
)

_user_python = os.path.join(_mcv.getStore().getUserDirectory().toString(), 'python')
if os.path.exists(_user_python):
    sys.path.append(_user_python)
    for mod in os.listdir(_user_python):
        modname, ext = os.path.splitext(mod)
        if ext == '.py':
            globals()[modname] = __import__(modname, globals(), locals(), ['*'], -1)
        del modname, ext
        
