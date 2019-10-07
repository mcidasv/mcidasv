# A collection of functions to create displays of different ABI RGB
# products and band subtractions.

# abitrucol is derived from McIDAS-X's ABITRUCOL function
# which creates an ABI RGB by deriving a green band
# McIDAS-X's ABITRUCOL is based on the CIMSS Natural True Color method
# http://cimss.ssec.wisc.edu/goes/OCLOFactSheetPDFs/ABIQuickGuide_CIMSSRGB_v2.pdf
def ABITruColRGB(red, grn, blu):
    # red = band 2
    # grn = band 3
    # blu = band 1

    # multiply bands by coefficient and add together
    # to make corrected RGB
    redCoef = red * 0.45
    grnCoef = grn * 0.1
    bluCoef = blu * 0.45

    combGrn = redCoef + grnCoef + bluCoef

    # mask values greater than those passed through the first
    # rescale below.  If this isn't done, then anything outside
    # of the range set by the first rescale will be set to the
    # max value of the rescaling (10)
    redMasked = mask(red, '<', 33, 0) * red
    combGrnMasked = mask(combGrn, '<', 40, 0) * combGrn
    bluMasked = mask(blu, '<', 50, 0) * blu

    # first rescale for the lower end values
    redScaled1 = rescale(redMasked, 0, 33, 0, 10)
    grnScaled1 = rescale(combGrnMasked, 0, 40, 0, 10)
    bluScaled1 = rescale(bluMasked, 0, 50, 0, 10)

    # second rescale for higher end values
    redScaled2 = rescale(red, 33, 255, 10, 255)
    grnScaled2 = rescale(combGrn, 40, 255, 10, 255)
    bluScaled2 = rescale(blu, 50, 255, 10, 255)

    # sum the two rescaled objects together
    final_red = redScaled1 + redScaled2
    final_grn = grnScaled1 + grnScaled2
    final_blu = bluScaled1 + bluScaled2

    # create rgb object
    rgb = combineRGB(final_red, final_grn, final_blu)
    return rgb
