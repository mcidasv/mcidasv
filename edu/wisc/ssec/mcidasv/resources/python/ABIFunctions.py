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

# The functions below were created using the Quick Guides
# linked from CIRA's VISIT Quick Guides page:
# http://rammb.cira.colostate.edu/training/visit/quick_guides/
# Information about each RGB/band subtraction below can be
# found on the above webpage.  Note that these RGBs and band
# subtractions were submitted by a variety of sources, all of
# which are referenced on each individual product's page.

# ABI Airmass RGB
def ABIAirmassRGB(b8T, b10T, b12T, b13T):
    # http://rammb.cira.colostate.edu/training/visit/quick_guides/QuickGuide_GOESR_AirMassRGB_final.pdf
    # red = band8 - band10; -26.2C to 0.6C rescalled to 0 to 255
    # grn = band12 - band13; -43.2C to 6.7C rescalled to 0 to 255
    # blu = band8; 243.9K to 208.5K rescalled to 0 to 255
    red = rescale(b8T-b10T, -26.2, 0.6, 0, 255)
    grn = rescale(b12T-b13T, -43.2, 6.7, 0, 255)
    blu = rescale(b8T, 243.9, 208.5, 0, 255)
    return combineRGB(red, grn, blu)

# ABI SO2 RGB
def ABISo2RGB(b9T, b10T, b11T, b13T):
    # http://rammb.cira.colostate.edu/training/visit/quick_guides/Quick_Guide_SO2_RGB.pdf
    # red = band9 - band10; -4C to 2C rescaled to 0 to 255
    # grn = band13 - band11; -4C to 5C rescaled to 0 to 255
    # blu = band13; 243.05K to 302.95K rescaled to 0 to 255
    red = rescale(b9T-b10T, -4, 2, 0, 255)
    grn = rescale(b13T-b11T, -4, 5, 0, 255)
    blu = rescale(b13T, 243.05, 302.95, 0, 255)
    return combineRGB(red, grn, blu)

# ABI Day Cloud Phase Distinction RGB
def ABIDayCloudPhaseRGB(b2A, b5A, b13T):
    # http://rammb.cira.colostate.edu/training/visit/quick_guides/Day_Cloud_Phase_Distinction.pdf
    # red = band 13; 280.65K to 219.56K rescaled to 0 to 255
    # grn = band 2; 0% to 78% rescaled to 0 to 255
    # blu = band 5; 1% to 59% rescaled to 0 to 255
    red = rescale(b13T, 280.65, 219.65, 0, 255)
    grn = rescale(b2A, 0, 78, 0, 255)
    blu = rescale(b5A, 1, 59, 0, 255)
    return combineRGB(red, grn, blu)

# ABI Ash RGB
def ABIAshRGB(b11T, b13T, b14T, b15T):
    # http://rammb.cira.colostate.edu/training/visit/quick_guides/GOES_Ash_RGB.pdf
    # red = band15 - band13; -6.7C to 2.6C rescaled to 0 to 255
    # grn = band14 - band11; -6.0C to 6.3C rescaled to 0 to 255
    # blu = band13; 246.3K to 302.4K rescaled to 0 to 255
    red = rescale(b15T-b13T, -6.7, 2.6, 0, 255)
    grn = rescale(b14T-b11T, -6.0, 6.3, 0, 255)
    blu = rescale(b13T, 246.3, 302.4, 0, 255)
    return combineRGB(red, grn, blu)

# ABI Day Land Cloud RGB
def ABIDayLandCloudRGB(b2A, b3A, b5A):
    # http://rammb.cira.colostate.edu/training/visit/quick_guides/QuickGuide_GOESR_daylandcloudRGB_final.pdf
    # red = band5; 0% to 97.5% rescaled to 0 to 255
    # grn = band3; 0% to 108.6% rescaled to 0 to 255
    # blu = band2; 0% to 100% rescaled to 0 to 255
    red = rescale(b5A, 0, 97.5, 0, 255)
    grn = rescale(b3A, 0, 108.6, 0, 255)
    blu = rescale(b2A, 0, 100, 0, 255)
    return combineRGB(red, grn, blu)

# ABI Day Land Cloud Fire RGB
def ABIDayLandCloudFireRGB(b2A, b3A, b6A):
    # http://rammb.cira.colostate.edu/training/visit/quick_guides/QuickGuide_GOESR_DayLandCloudFireRGB_final.pdf
    # red = band6; 0% to 100% rescaled to 0 to 255
    # grn = band3; 0% to 100% rescaled to 0 to 255
    # blu = band2; 0% to 100% rescaled to 0 to 255
    red = rescale(b6A, 0, 100, 0, 255)
    grn = rescale(b3A, 0, 100, 0, 255)
    blu = rescale(b2A, 0, 100, 0, 255)
    return combineRGB(red, grn, blu)
