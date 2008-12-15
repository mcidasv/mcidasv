import sys
import os

# This is an ugly hack to deal with Jython's sys.path strangeness: if you 
# want to import a non-compiled python module contained in a JAR, sys.path 
# must contain something like "/path/to/your.jar/path/to/module"
_cwd = os.getcwd()
_mcv_jar = os.path.join(_cwd, 'mcidasv.jar')
_idv_jar = os.path.join(_cwd, 'idv.jar')

# however, if we're not inside a jar just prepend the current directory.
if not os.path.exists(_mcv_jar):
    _mcv_jar = _idv_jar = _cwd

# NOTE: the paths appended to sys.path cannot have a trailing '/'!
_linear_combo = _mcv_jar+'/edu/wisc/ssec/mcidasv/resources/python/linearcombo'
_mcv_python = _mcv_jar+'/edu/wisc/ssec/mcidasv/resources/python'

sys.path.append(_linear_combo)
sys.path.append(_mcv_python)

import imageFilters as filters
import colorutils

from edu.wisc.ssec.mcidasv.control.LinearCombo import Selector
from edu.wisc.ssec.mcidasv.data.hydra import MultiSpectralData

# maps band names to wavenumbers (string -> float)
_namedBands = _linearCombo.getBandNameMappings()

# initial wavenumber changes depending upon the type of data
_initWavenumber = _linearCombo.getInitialWavenumber()

# maps keyword parameters to a list of valid aliases (string -> [string])
# needs some work :(
_KWARG_ALIASES = {
    'wavenumber': ['w', 'wave', 'chan', 'channel', 'b', 'band'],
    'color': ['c', 'color', 'colour']
}

def _extract_kwarg(aliases, arg_dict):
    for alias in aliases:
        if alias in arg_dict:
            return arg_dict[alias]

# this argument stuff is getting a little tedious and will be 10000 bears to
# document--consider multimethods or simplifying
def selector(*args, **kwargs):
    if not args:
        wavenumber = _initWavenumber
        color = 'green'
    elif len(args) == 1:
        if hasattr(args[0], '__float__'):
            wavenumber = args[0].__float__()
            color = 'green'
        else:
            if args[0] not in _namedBands:
                wavenumber = _initWavenumber
                color = args[0]
            else:
                wavenumber = _namedBands[args[0]]
                color = 'green'
    else:
        if args[0] in _namedBands:
            wavenumber, color = _namedBands[args[0]], args[1]
        else:
            wavenumber, color = args
    
    kw_wave = _extract_kwarg(_KWARG_ALIASES['wavenumber'], kwargs)
    kw_color = _extract_kwarg(_KWARG_ALIASES['color'], kwargs)
    
    if kw_wave is not None:
        wavenumber = kw_wave
    if kw_color is not None:
        color = kw_color
    
    if wavenumber in _namedBands:
        wavenumber = _namedBands[wavenumber]
    
    visad_color = colorutils.convertColor(color)
    sel = Selector(wavenumber, visad_color, _linearCombo, _jythonConsole)
    return sel

def field(combination, name=None):
    if not name:
        name = combination.getName()
    _linearCombo.addCombination(name, combination.getData())
    return combination

def bands():
    return _namedBands.keys()

# temp hack for "aliases"
combine = field
makeField = field
_s = selector