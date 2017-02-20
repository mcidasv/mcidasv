"""HYDRA RGB helper module."""

from ucar.unidata.data.grid import DerivedGridFactory
from ucar.unidata.data.grid import GridUtil
from visad import Data
from visad import FieldImpl
from visad.python.JPythonMethods import makeRealType

uniqueID = 0

def mycombineRGB(red, green, blue):
    """Three Color (RGB) Image (Auto-scale) formula."""
    global uniqueID
    uniqueID += 1
    red = GridUtil.setParamType(red, makeRealType("redimage%d" % uniqueID), 0)
    green = GridUtil.setParamType(green, makeRealType("greenimage%d" % uniqueID), 0)
    blue = GridUtil.setParamType(blue, makeRealType("blueimage%d" % uniqueID), 0)
    return DerivedGridFactory.combineGrids([red, green, blue], 1)
