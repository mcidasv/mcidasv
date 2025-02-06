"""HYDRA RGB helper module."""

from ucar.unidata.data.grid import DerivedGridFactory
from ucar.unidata.data.grid import GridUtil
from visad import Data
from visad import FieldImpl
from visad.python.JPythonMethods import makeRealType

uniqueID = 0

def rayleight_ot(lmbda): return 0.00877 * (lmbda ** (-4.05))

def correct(band, tau):
    data = band.getFloats(False)
    
    for i in range(len(data[0])): data[0][i] -= tau
    
    out = band.clone()
    out.setSamples(data)
    
    return out

def mycombineRGB_Rayleigh(red, green, blue, lmb = (0.65, 0.56, 0.47)):
    rl, gl, bl = lmb[0], lmb[1], lmb[2]
    rt, gt, bt = rayleight_ot(rl), rayleight_ot(gl), rayleight_ot(bl)
    
    red = correct(red, rt)
    green = correct(green, gt)
    blue = correct(blue, bt)
    
    return mycombineRGB(red, green, blue)
        
        
def mycombineRGB(red, green, blue):
    """Three Color (RGB) Image (Auto-scale) formula."""
    global uniqueID
    uniqueID += 1
    
    red = GridUtil.setParamType(red, makeRealType("redimage%d" % uniqueID), 0)
    green = GridUtil.setParamType(green, makeRealType("greenimage%d" % uniqueID), 0)
    blue = GridUtil.setParamType(blue, makeRealType("blueimage%d" % uniqueID), 0)

    return DerivedGridFactory.combineGrids([red, green, blue], 1)
