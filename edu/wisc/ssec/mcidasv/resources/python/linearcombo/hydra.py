import color

from edu.wisc.ssec.mcidasv.control.LinearCombo import Selector

def selector(channel=MultiSpectralData.init_wavenumber, color='green'):
    visad_color = color.convertColor(color)
    sel = Selector(float(channel), visad_color, _linearCombo, _jythonConsole)
    return sel

def combine(combination, name=None):
    if type(combination) is type('') and '=' not in combination:
        exec '_tmpResult=%s' % (combination)
        if name == None:
            name = combination
        _linearCombo.addCombination(name, _tmpResult.getData())
