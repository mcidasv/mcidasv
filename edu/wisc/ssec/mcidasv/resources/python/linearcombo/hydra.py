import sys

# This is an ugly hack to deal with Jython's sys.path strangeness: if you 
# want to import a non-compiled python module contained in a JAR, sys.path 
# must contain something like "/path/to/your.jar/path/to/module"
_COMBO_PATH = '/edu/wisc/ssec/mcidasv/resources/python/linearcombo'
_complete_path = _linearCombo.getClass().getResource(_COMBO_PATH).getPath()
if _complete_path.startswith('file:'):
    _complete_path = _complete_path[5:].replace('!', '')
sys.path.append(_complete_path)

import colorutils

from edu.wisc.ssec.mcidasv.control.LinearCombo import Selector
from edu.wisc.ssec.mcidasv.data.hydra import MultiSpectralData

_KWARG_ALIASES = {
    'wavenumber': ['w', 'wave', 'chan', 'channel'],
    'color': ['c', 'color', 'colour']
}

def _extract_kwarg(aliases, arg_dict):
    for alias in aliases:
        if alias in arg_dict:
            return arg_dict[alias]

def selector(*args, **kwargs):
    if not args:
        wavenumber = MultiSpectralData.init_wavenumber
        color = 'green'
    elif len(args) == 1:
        if hasattr(args[0], '__float__'):
            wavenumber = args[0].__float__()
            color = 'green'
        else:
            wavenumber = MultiSpectralData.init_wavenumber
            color = args[0]
    else:
        wavenumber, color = args
    
    kw_wave = _extract_kwarg(_KWARG_ALIASES['wavenumber'], kwargs)
    kw_color = _extract_kwarg(_KWARG_ALIASES['color'], kwargs)
    
    if kw_wave is not None:
        wavenumber = kw_wave
    if kw_color is not None:
        color = kw_color
    
    visad_color = colorutils.convertColor(color)
    sel = Selector(wavenumber, visad_color, _linearCombo, _jythonConsole)
    return sel

def field(combination, name=None):
    if not name:
        name = combination.getName()
    _linearCombo.addCombination(name, combination.getData())
    return combination

# temp hack for "aliases"
combine = field
makeField = field
_s = selector