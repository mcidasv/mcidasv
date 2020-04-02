# MODIS Airmass RGB
def MODISAirmassRGB(b27T, b28T, b30T, b31T):
    # red = band27 - band28; -25K to 0K rescalled to 0 to 255
    # grn = band30 - band31; -40K to 5K rescalled to 0 to 255
    # blu = band27; 243K to 208K rescalled to 0 to 255
    red = rescale(b27T-b28T, -25, 0, 0, 255)
    grn = rescale(b30T-b31T, -40, 5, 0, 255)
    blu = rescale(b27T, 243, 208, 0, 255)
    return combineRGB(red, grn, blu)

# MODIS Dust RGB
def MODISDustRGB(b29T, b31T, b32T):
    # red = band32 - band31; -4 to 2K rescalled to 0 to 255
    # grn = band31 - band29; 0 to 15K rescalled to 0 to 255; gamma 2.5
    # blu = band31
    red = rescale(b32T-b31T, -4, 2, 0, 255)
    grn = 255*(rescale(b31T-b29T, 0, 15, 0, 1)**0.4)
    blu = rescale(b31T, 261, 289, 0, 255)
    return combineRGB(red, grn, blu)

# MODIS Night Microphysics RGB
def MODISNightMicrophysicsRGB(b22T, b31T, b32T):
    # red = band32 - band31; -4K to 2K rescalled to 0 to 255
    # grn = band31 - band21; 0K to 10K rescalled to 0 to 255; gamma 0.4
    # blu = band31; 243K to 293K rescalled to 0 to 255
    red = rescale(b32T-b31T, -4, 2, 0, 255)
    grn = 255*(rescale(b31T-b22T, 0, 10, 0, 1)**0.4)
    blu = rescale(b31T, 243, 293, 0, 255)
    return combineRGB(red, grn, blu)

# MODIS NDVI
def MODISNDVI(b1R,b2R):
    # b1R = Band 1 (0.6465um) Reflectance
    # b2R = Band 2 (0.8567um) Reflectance
    return (b2R-b1R)/(b2R+b1R)

# MODIS EVI
def MODISEVI(b1R,b2R, b3R):
    # b1R = Band 1 (0.6465um) Reflectance : Red band
    # b2R = Band 2 (0.8567um) Reflectance : NIR
    # b3R = Band 3 (0.4656um) Reflectance : Blue band
    # G = Gain factor (2.5)
    # L = Canopy background adjustment (1)
    # C1, C2 = Coefficients of aerosol resistance term (C1=6, C2=7.5)
    G = 2.5
    L = 1
    C1 = 6
    C2 = 7.5
    return G * ((b2R - b1R)/(b2R + (C1 * b1R) - (C2 * b3R) + L))

# MODIS True Color RGB
def MODISTruColRGB(b1B, b4B, b3B):
    # red = band1 BRIT
    # grn = band4 BRIT
    # blu = band3 BRIT
    red = b1B
    grn = b4B
    blu = b3B
    return combineRGB(red, grn, blu)
