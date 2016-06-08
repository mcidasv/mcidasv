import time

from combineRGB import mycombineRGB
from grid import noUnit

from edu.wisc.ssec.mcidasv.McIDASV import getStaticMcv
from edu.wisc.ssec.mcidasv.data.SandwichSpeedup import sandwichSpeedup

def sandwich(imgIR, imgVIS, minIR=180, maxIR=280, colorTable='Sandwich', useNaN=True):
    """McIDAS-V implementation of Martin Setvak's "sandwich" imagery.
    
    Args:
        imgIR: IR image being displayed.
        imgVIS: VIS image being displayed.
        minIR, maxIR: outside these bounds, no IR layer will be visible.
        colorTable: Name of chosen enhancement table for IR image.
        useNaN: if True, the VIS image won't be visible outside of minIR/maxIR.
        usePlugin: if True, use sandwich_speedup.jar plugin for optimization.
        
    Returns:
       rgbImg: An RGB "sandwich".
    """
    if imgIR.isFlatField():
        imgIR = imgIR
    else:
        imgIR = imgIR[0]
        
    if imgIR.isFlatField():
        imgVIS = imgVIS
    else:
        imgVIS = imgVIS[0]
        
    if useNaN:
        noIRContribution = float('nan')
    else:
        noIRContribution = 1
        
    ct = getStaticMcv().getColorTableManager().getColorTable(colorTable)
    table = ct.getColorTable()
    
    # get the rgb values for each index of the rainbow table
    # flip the color table here so that cold temperatures are red
    rTable = table[0][::-1]   # should use ColorTable.IDX_RED etc.
    gTable = table[1][::-1]
    bTable = table[2][::-1]
    aTable = table[3][::-1]  # alpha layer... all 1's for rainbow table
    nCols = len(rTable) - 1  # TODO: why minus 1?
    
    # scale the IR image from 0 to 1
    floatsIR = imgIR.getFloats(False)[0]
    scaledIR = (imgIR - minIR) / (maxIR - minIR)
    scaledFloats = scaledIR.getFloats(False)[0]
    
    # set up the r, g, b arrays to put the image in to
    rIR = imgIR.clone()
    gIR = imgIR.clone()
    bIR = imgIR.clone()
    rFloats = rIR.getFloats(False)[0]
    gFloats = gIR.getFloats(False)[0]
    bFloats = bIR.getFloats(False)[0]
    
    t0 = time.clock()
    sandwichSpeedup(
        scaledFloats,
        floatsIR,
        rFloats,
        gFloats,
        bFloats,
        rTable,
        gTable,
        bTable,
        minIR,
        maxIR,
        nCols,
        noIRContribution,
    )
    t1 = time.clock()
    # print('Sandwich: time spent in for loop [s]: {}'.format(t1 - t0))
    
    # now scale rgb values by visible image to make the "sandwich" product
    imgVIS = noUnit(imgVIS)
    rIR = noUnit(rIR)
    gIR = noUnit(gIR)
    bIR = noUnit(bIR)
    
    rOutput = imgVIS * rIR
    gOutput = imgVIS * gIR
    bOutput = imgVIS * bIR
    
    rgbImg = mycombineRGB(rOutput, gOutput, bOutput)
    return rgbImg
