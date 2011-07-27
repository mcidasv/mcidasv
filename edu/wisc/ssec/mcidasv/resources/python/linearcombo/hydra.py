if '_linearCombo' in dir():
    # maps band names to wavenumbers (string -> float)
    _namedBands = _linearCombo.getBandNameMappings()

    # initial wavenumber changes depending upon the type of data
    _initWavenumber = _linearCombo.getInitialWavenumber()
else:
    _namedBands = {}

    # from edu.wisc.ssec.mcidasv.data.hydra.MultiSpectralData
    _initWavenumber = 919.5

import colorutils
import imageFilters as filters
import shell as idvutils

from edu.wisc.ssec.mcidasv.McIDASV import getStaticMcv
from edu.wisc.ssec.mcidasv.control.LinearCombo import Selector
from edu.wisc.ssec.mcidasv.data.hydra import MultiSpectralData

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

def load(path):
    return getStaticMcv().makeOneDataSource(path, None, None)

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
