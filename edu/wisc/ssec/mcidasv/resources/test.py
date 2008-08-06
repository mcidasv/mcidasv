from java.awt import Color
from edu.wisc.ssec.mcidasv.control.LinearCombo import Selector
from edu.wisc.ssec.mcidasv.data.hydra import MultiSpectralData

def selector(channel=MultiSpectralData.init_wavenumber, color=Color.GREEN):
    sel = Selector(float(channel), color, _linearCombo, _jythonConsole)
    return sel

def combine(combination):
    _linearCombo.setComboChoice(combination.getData())