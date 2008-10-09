import sys

# This is an ugly hack to deal with Jython's sys.path strangeness: if you want
# to import a non-compiled python module contained in a JAR, sys.path must
# contain something like "/path/to/your.jar/path/to/module"
_COMBO_PATH = '/edu/wisc/ssec/mcidasv/resources/python/linearcombo'
_complete_path = _linearCombo.getClass().getResource(_COMBO_PATH).getPath()
if _complete_path.startswith('file:'):
    _complete_path = _complete_path[5:].replace('!', '')
sys.path.append(_complete_path)

import colorutils

from edu.wisc.ssec.mcidasv.control.LinearCombo import Selector
from edu.wisc.ssec.mcidasv.data.hydra import MultiSpectralData

def selector(channel=MultiSpectralData.init_wavenumber, color='green'):
    visad_color = colorutils.convertColor(color)
    sel = Selector(float(channel), visad_color, _linearCombo, _jythonConsole)
    return sel

def combine(combination, name=None):
    if type(combination) is type('') and '=' not in combination:
        exec '_tmpResult=%s' % (combination)
        if name == None:
            name = combination
        _linearCombo.addCombination(name, _tmpResult.getData())