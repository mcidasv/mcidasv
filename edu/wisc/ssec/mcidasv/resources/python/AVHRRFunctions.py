# A collection of functions to create displays of different
# AVHRR RGB products

# AVHRR Night Microphysics RGB
def AVHRRNightMicrophysicsRGB(b3T, b4T, b5T):
    # red = band5 - band4; -4K to 2K rescalled to 0 to 255
    # grn = band4 - band3; -4K to 6K rescalled to 0 to 255
    # blu = band4; 243K to 293K rescalled to 0 to 255
    m3 = b3T.getMetadataMap()
    b3TM = mask(b3T, '>', 0, 1) * b3T
    b3TM.setMetadataMap(m3)
    red = rescale(b5T-b4T, -4, 2, 0, 255)
    grn = rescale(b4T-b3TM, -4, 6, 0, 255)
    blu = rescale(b4T, 243, 293, 0, 255)
    return combineRGB(red, grn, blu)

# AVHRR Day Microphysics RGB
def AVHRRDayMicrophysicsRGB(b2R, b4T, b6R):
    # red = band2; 0% to 100% reflectance rescalled to 0 to 255
    # grn = band6; 0% to 70% reflectance rescalled to 0 to 255
    # blu = band4; 203K to 323K rescalled to 0 to 255
    red = rescale(b2R, 0, 100, 0, 255)
    grn = rescale(b6R, 0, 70, 0, 255)
    blu = rescale(b4T, 203, 323, 0, 255)
    return combineRGB(red, grn, blu)

# AVHRR Day Microphysics RGB
def AVHRRNaturalColorRGB(b1R, b2R, b6R):
    # red = band6; 0% to 100% reflectance rescalled to 0 to 255
    # grn = band2; 0% to 100% reflectance rescalled to 0 to 255
    # blu = band1; 0% to 100% reflectance rescalled to 0 to 255
    red = rescale(b6R, 0, 100, 0, 255)
    grn = rescale(b2R, 0, 100, 0, 255)
    blu = rescale(b1R, 0, 100, 0, 255)
    return combineRGB(red, grn, blu)

# AVHRR Cloud RGB
def AVHRRCloudRGB(b1R, b2R, b4T):
    # red = band1; 0% to 100% reflectance rescalled to 0 to 255
    # grn = band2; 0% to 100% reflectance rescalled to 0 to 255
    # blu = band4 inverted; 323K to 203K rescalled to 0 to 255
    red = rescale(b1R, 0, 100, 0, 255)
    grn = rescale(b2R, 0, 100, 0, 255)
    blu = rescale(b4T, 323, 203, 0, 255)
    return combineRGB(red, grn, blu)
