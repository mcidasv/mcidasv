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
        if isinstance(args[0], float) or isinstance(args[0], int):
            wavenumber = float(args[0])
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

def combine(combination, name=None):
    if type(combination) is type('') and '=' not in combination:
        exec '_tmpResult=%s' % (combination)
        if name == None:
            name = combination
        _linearCombo.addCombination(name, _tmpResult.getData())
        return _tmpResult

_s = selector