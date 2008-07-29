from java.awt import Color
from edu.wisc.ssec.mcidasv.control.LinearCombo import Selector

def selector(channel=919.5, color=Color.GREEN):
    sel = Selector(float(channel), color, _linearCombo, _jythonConsole)
    return sel

def combine():
    pass