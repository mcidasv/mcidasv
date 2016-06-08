import time

from combineRGB import mycombineRGB
from grid import noUnit

from edu.wisc.ssec.mcidasv.McIDASV import getStaticMcv
from edu.wisc.ssec.mcidasv.data.SandwichSpeedup import sandwichSpeedup

def sandwich(imgIR, imgVIS, minIR=180, maxIR=280, colorTable='Sandwich', useNaN=True, usePlugin=True):
    """
    McIDAS-V implementation of Martin Setvak's "sandwich" imagery.
    Input:
       imgIR:  The IR image being displayed
       imgVIS: The VIS image being displayed
       minIR, maxIR: outside these bounds, no IR layer will be visible
       colorTable: Name of chosen ehancement table for IR image
       useNaN: if True, the VIS image won't be visible outside of minIR/maxIR
       usePlugin: if True, use sandwich_speedup.jar plugin for optimization
    Output:
       rgbImg:  an RGB sandwich 
    """
    if (imgIR.isFlatField()):
        imgIR = imgIR
    else:
        imgIR = imgIR[0]
        
    if (imgIR.isFlatField()):
        imgVIS = imgVIS
    else:
        imgVIS = imgVIS[0]
        
    if (useNaN):
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
    if usePlugin:
        print('Sandwich: Using SandwichSpeedup plugin')
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
    else:
        print('Sandwich: Using Jython for loop')
        for i, pixel in enumerate(scaledFloats):
            # set anything colder than threshold to r,g,b from color table,
            # otherwise just set to 1 (so that result after multiply is just
            # the vis image)
            if (floatsIR[i] < maxIR):
                
                # if anything falls below the minIR, set it to the minIR
                # (scaledFloats=0)
                if (floatsIR[i] < minIR):
                    pixel = 0
                    
                # need to convert float ranging from 0.0 to 1.0 into integer index
                # ranging from 0 to nCols
                # testing
                
                ind = int(pixel * nCols)
                
                rFloats[i] = rTable[ind]
                gFloats[i] = gTable[ind]
                bFloats[i] = bTable[ind]
            else:
                rFloats[i] = noIRContribution    # previously was set to 1
                gFloats[i] = noIRContribution    # see note for rFloats
                bFloats[i] = noIRContribution    # see note for rFloats
    t1 = time.clock()
    print('Sandwich: time spent in for loop [s]: {}'.format(t1 - t0))
    
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
