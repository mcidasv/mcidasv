# Note that these functions are still considered to be under development

from edu.wisc.ssec.mcidasv.data.hydra import MultiSpectralDataSource
from ucar.unidata.data.grid import GridUtil


def unpackage(fieldImpl):
    """ Return the first FlatField contained by this FieldImpl.
  
    Args:  
      fieldImpl: the original argument to the VIIRS formula being called

    Returns:  FlatField suitable for sending into MultiSpectralDataSource methods
    """
    if GridUtil.isTimeSequence(fieldImpl):
        return fieldImpl.getSample(0)
    else:
        return fieldImpl


def package(original, result):
    """ Put 'result' back into a FieldImpl using the time domain from 'original'.

    Args:
      original: the original argument to the VIIRS formula being called
      result: the result of the MultiSpectralDataSource methods called by
            the current VIIRS formula

    Returns: FieldImpl with proper time domain (so that e.g. IDV's %timestamp% macro
             will work properly)
    """
    from visad import FunctionType
    from visad import FieldImpl
    from visad import RealType

    if GridUtil.isTimeSequence(original):
        ftype = FunctionType(RealType.Time, result.getType())
        fieldimpl = FieldImpl(ftype, original.getDomainSet())
        fieldimpl.setSample(0, result)
        return fieldimpl
    else:
        # just return the plain flatfield if original wasn't a fieldimpl
        # (needed to make loadJPSSImage work)
        return result


def swathToGrid(fltField, res, mode):
    incoming = fltField
    fltField = unpackage(fltField)

    mp = MultiSpectralDataSource.getDataProjection(fltField)
    grid = MultiSpectralDataSource.makeGrid(mp, float(res))
    stg = MultiSpectralDataSource.swathToGrid(grid, fltField, float(mode))

    return package(incoming, stg)


def makeGrid(fltField, res):

    mp = MultiSpectralDataSource.getDataProjection(fltField)
    grid = MultiSpectralDataSource.makeGrid(mp, float(res))

    return grid


# VIIRS SDR True Color RGB
def VIIRSTrueColorRGB(M5, M4, M3):
    # red = M5 (0.672um) Reflectance
    # grn = M4 (0.555um) Reflectance
    # blu = M3 (0.488um) Reflectance

    inM5 = M5
    M5 = unpackage(M5)
    inM4 = M4
    M4 = unpackage(M4)
    inM3 = M3
    M3 = unpackage(M3)
  
    grd750 = makeGrid(M5, 750)

    rgb = MultiSpectralDataSource.swathToGrid(grd750, [M5, M4, M3], 1.0)

    return package(inM5, rgb)


# VIIRS SDR Natural Color RGB (M-band)
def VIIRSNaturalColorRGB(M10, M7, M5):
    # red = M10 (1.61um) Reflectance
    # grn = M7 (0.865um) Reflectance
    # blu = M5 (0.672um) Reflectance

    inM10 = M10
    M10 = unpackage(M10)
    inM7 = M7
    M7 = unpackage(M7)
    inM5 = M5
    M5 = unpackage(M5)
  
    grd750 = makeGrid(M10, 750)

    rgb = MultiSpectralDataSource.swathToGrid(grd750, [M10, M7, M5], 1.0)

    return package(inM10, rgb)
    

# VIIRS SDR NDVI
def VIIRSNDVI(I1, I2):
    # I1 = 0.64um - visible Reflectance
    # I2 = 0.865um - near IR Reflectance

    inI1 = I1
    I1 = unpackage(I1)
    inI2 = I2
    I2 = unpackage(I2)
    grd375 = makeGrid(I1, 375)

    VISG = MultiSpectralDataSource.swathToGrid(grd375, I1, 1.0)
    NIRG = MultiSpectralDataSource.swathToGrid(grd375, I2, 1.0)

    ndvi = (NIRG - VISG).divide(NIRG + VISG)

    return package(inI1, ndvi)


# VIIRS SDR NDSI
def VIIRSNDSI(I1, I3):
    # I1 = 0.64um - visible Reflectance
    # I3 = 1.61um - shortwave IR Reflectance

    inI1 = I1
    I1 = unpackage(I1)
    inI3 = I3
    I3 = unpackage(I3)
    grd375 = makeGrid(I1, 375)

    VISG = MultiSpectralDataSource.swathToGrid(grd375, I1, 1.0)
    SIRG = MultiSpectralDataSource.swathToGrid(grd375, I3, 1.0)

    ndsi = (VISG - SIRG).divide(VISG + SIRG)

    return package(inI1, ndsi)


# VIIRS SDR Dust RGB
def VIIRSDustRGB(M14, M15, M16):
    # red = M16 - M15 (12.013um - 10.763um); -4C to 2C rescaled to 0 to 255; gamma 1.0
    # grn = M15 - M14 (10.763um - 3.7um); 0C to 15C rescaled to 0 to 255; gamma 2.5
    # blu = M15 (10.763um); 261K to 289K rescaled to 0 to 255; gamma 1.0

    inM14 = M14
    M14 = unpackage(M14)
    inM15 = M15
    M15 = unpackage(M15)
    inM16 = M16
    M16 = unpackage(M16)

    grd750 = makeGrid(M14, 750)

    red = rescale(M16-M15, -4, 2, 0, 255)
    grn = 255*(rescale(M15-M14, 0, 15, 0, 1)**0.4)
    blu = rescale(M15, 261, 289, 0, 255)
  
    rgb = MultiSpectralDataSource.swathToGrid(grd750, [red, grn, blu], 1.0)
    return package(inM15, rgb)


# VIIRS SDR Night Microphysics RGB
def VIIRSNightMicrophysicsRGB(M12, M15, M16):
    # red = M16 - M15 (12.013um - 10.763um); -4C to 2C rescaled to 0 to 255
    # grn = M15 - M12 (10.763um - 3.7um); 0C to 10C rescaled to 0 to 255
    # blu = M15 (10.763um); 243K to 293K rescaled to 0 to 255

    inM12 = M12
    M12 = unpackage(M12)
    inM15 = M15
    M15 = unpackage(M15)
    inM16 = M16
    M16 = unpackage(M16)

    grd750 = makeGrid(M12, 750)

    red = rescale(M16-M15, -4, 2, 0, 255)
    grn = rescale(M15-M12, 0, 10, 0, 255)
    blu = rescale(M15, 243, 293, 0, 255)

    rgb = MultiSpectralDataSource.swathToGrid(grd750, [red, grn, blu], 1.0)
    return package(inM12, rgb)
  

# VIIRS SDR Day Fire RGB
def VIIRSDayFireRGB(I4, I2, I1):
    # http://rammb.cira.colostate.edu/training/visit/quick_guides/VIIRS_Day_Land_Cloud_Fire_RGB_Quick_Guide_10182018.pdf
    # red = I4 (3.7um) - 0C to 60C rescaled to 0 to 255; gamma 0.4
    # grn = I2 (0.86um) - 0% to 100% reflectance rescaled to 0 to 255; gamma 1.0
    # blu = I1 (0.64um) - 0% to 100% reflectance rescaled to 0 to 255; gamma 1.0

    inI4 = I4
    I4 = unpackage(I4)
    inI2 = I2
    I2 = unpackage(I2)
    inI1 = I1
    I1 = unpackage(I1)

    grd375 = makeGrid(I4, 375)

    red = 255*(rescale(I4, 273.15, 333.15, 0, 1)**2.5)
    grn = rescale(I2, 0, 100, 0, 255)
    blu = rescale(I1, 0, 100, 0, 255)

    rgb = MultiSpectralDataSource.swathToGrid(grd375, [red, grn, blu], 1.0)
    return package(inI4, rgb)


# VIIRS SDR Fire Temperature RGB
def VIIRSFireTemperatureRGB(M12, M11, M10):
    # http://rammb.cira.colostate.edu/training/visit/quick_guides/VIIRS_Fire_Temperature_RGB_Quick_Guide_10182018.pdf
    # red = M12 (3.7um) - 0C to 70C rescaled to 0 to 255; gamma 0.4
    # grn = M11 (0.86um) - 0% to 100% reflectance rescaled to 0 to 255; gamma 1.0
    # blu = M10 (0.64um) - 0% to 75% reflectance rescaled to 0 to 255; gamma 1.0

    inM12 = M12
    M12 = unpackage(M12)
    inM11 = M11
    M11 = unpackage(M11)
    inM10 = M10
    M10 = unpackage(M10)

    grd750 = makeGrid(M12, 750)

    red = 255*(rescale(M12, 273.15, 343.15, 0, 1)**2.5)
    grn = rescale(M11, 0, 100, 0, 255)
    blu = rescale(M10, 0, 75, 0, 255)

    rgb = MultiSpectralDataSource.swathToGrid(grd750, [red, grn, blu], 1.0)
    return package(inM12, rgb)


# VIIRS SDR Natural Color RGB (I-band)
def VIIRSNaturalColorIRGB(I3, I2, I1):
    # red = I3 (1.61um) Reflectance
    # grn = I2 (0.86um) Reflectance
    # blu = I1 (0.64um) Reflectance

    inI1 = I1
    I1 = unpackage(I1)
    inI2 = I2
    I2 = unpackage(I2)
    inI3 = I3
    I3 = unpackage(I3)
  
    grd375 = makeGrid(I1, 375)

    rgb = MultiSpectralDataSource.swathToGrid(grd375, [I3, I2, I1], 1.0)
    return package(inI1, rgb)


# VIIRS SDR Cloud Phase RGB
def VIIRSCloudPhaseRGB(M10, M11, M1):
    # red = M10 (1.61um) 0 - 50 reflectance rescaled to 0 to 255; gamma 1.0
    # grn = M11 (2.25um) 0 - 50 reflectance rescaled to 0 to 255; gamma 1.0
    # blu = M1 (0.412um) 0 - 100 reflectance rescaled to 0 to 255; gamma 1.0

    inM10 = M10
    M10 = unpackage(M10)
    inM11 = M11
    M11 = unpackage(M11)
    inM1 = M1
    M1 = unpackage(M1)
  
    grd750 = makeGrid(M10, 750)
    
    red = rescale(M10, 0, 50, 0, 255)
    grn = rescale(M11, 0, 50, 0, 255)
    blu = rescale(M1, 0, 100, 0, 255)

    rgb = MultiSpectralDataSource.swathToGrid(grd750, [red, grn, blu], 1.0)

    return package(inM10, rgb)


# VIIRS SDR Cloud Type RGB
def VIIRSCloudTypeRGB(M9, M5, M10):
    # red = M9 (1.378um) 0 - 10 reflectance rescaled to 0 to 255; gamma 1.5
    # grn = M5 (0.672um) 0 - 80 reflectance rescaled to 0 to 255; gamma 0.75
    # blu = M10 (1.612um) 0 - 80 reflectance rescaled to 0 to 255; gamma 1.0

    inM9 = M9
    M9 = unpackage(M9)
    inM5 = M5
    M5 = unpackage(M5)
    inM10 = M10
    M10 = unpackage(M10)
  
    grd750 = makeGrid(M9, 750)
    
    red = 255*(rescale(M9, 0, 10, 0, 1)**0.66666)
    grn = 255*(rescale(M5, 0, 80, 0, 1)**1.33333)
    blu = rescale(M10, 0, 80, 0, 255)

    rgb = MultiSpectralDataSource.swathToGrid(grd750, [red, grn, blu], 1.0)

    return package(inM9, rgb)

# VIIRS SDR Snowmelt RGB
def VIIRSSnowmeltRGB(M10, M8, M5):
    # https://rammb2.cira.colostate.edu/wp-content/uploads/2021/03/VIIRS_Snowmelt_RGB_Quick_Guide_v3.pdf
    # red = M10 (1.61um)  0 to 100 reflectance rescaled to 0 to 255; gamma 1.0
    # grn = M8 (1.24um)  0 to 100 reflectance rescaled to 0 to 255; gamma 1.0
    # blu = M5 (0.67um)  0 to 100 reflectance rescaled to 0 to 255; gamma 1.0

    inM10 = M10
    M10 = unpackage(M10)
    inM8 = M8
    M8 = unpackage(M8)
    inM5 = M5
    M5 = unpackage(M5)
  
    grd750 = makeGrid(M5, 750)

    red = rescale(M10, 0, 100, 0, 255)
    grn = rescale(M8, 0, 100, 0, 255)
    blu = rescale(M5, 0, 100, 0, 255)

    rgb = MultiSpectralDataSource.swathToGrid(grd750, [red, grn, blu], 1.0)

    return package(inM5, rgb)

# VIIRS SDR Sea Spray RGB
def VIIRSSeaSprayRGB(I1, I2, I4, I5):
    # https://rammb.cira.colostate.edu/training/visit/quick_guides/VIIRS_Sea_Spray_RGB_Quick_Guide_v2.pdf
    # red = I4 (3.7um) - I5 (11.45)  - 0C to 10C rescaled to 0 to 255; gamma 1.0
    # grn = I2 (0.86um) - 1% to 20% reflectance rescaled to 0 to 255; gamma 0.6
    # blu = I1 (0.64um) - 2% to 25% reflectance rescaled to 0 to 255; gamma 0.6

    inI1 = I1
    I1 = unpackage(I1)
    inI2 = I2
    I2 = unpackage(I2)
    inI4 = I4
    I4 = unpackage(I4)
    inI5 = I5
    I5 = unpackage(I5)

    grd375 = makeGrid(I1, 375)

    red = rescale(sub(I4, I5), 0, 10, 0, 255)
    grn = 255*(rescale(I2, .01, .20, 0, 1)**1.66)
    blu = 255*(rescale(I1, .02, .25, 0, 1)**1.66)

    rgb = MultiSpectralDataSource.swathToGrid(grd375, [red, grn, blu], 1.0)
    return package(inI1, rgb)

# The below functions are for VIIRS EDR data.  EDR data
# do not have bowtie deletion lines, so removal is not needed

# VIIRS EDR True Color RGB (M-band)
def VIIRSEdrTrueColorRGB(M5, M4, M3):
    # red = M5 (0.672um) Reflectance
    # grn = M4 (0.555um) Reflectance
    # blu = M3 (0.488um) Reflectance
    return combineRGB(M5, M4, M3)

# VIIRS EDR Natural Color RGB (M-band)
def VIIRSEdrNaturalColorRGB(M10, M7, M5):
    # red = M10 (1.61um) Reflectance
    # grn = M7 (0.865um) Reflectance
    # blu = M5 (0.672um) Reflectance
    return combineRGB(M10, M7, M5)

# VIIRS EDR NDVI
def VIIRSEdrNDVI(I1, I2):
    # I1 = 0.64um - visible Reflectance
    # I2 = 0.865um - near IR Reflectance
    return (I2-I1)/(I1+I2)

# VIIRS EDR NDSI
def VIIRSEdrNDSI(I1, I3):
    # I1 = 0.64um - visible Reflectance
    # I3 = 1.61um - shortwave IR Reflectance
    return (I1-I3)/(I1+I3)

#VIIRS EDR Dust RGB
def VIIRSEdrDustRGB(M14, M15, M16):
    # red = M16 - M15 (12.013um - 10.763um); -4C to 2C rescaled to 0 to 255; gamma 1.0
    # grn = M15 - M14 (10.763um - 3.7um); 0C to 15C rescaled to 0 to 255; gamma 2.5
    # blu = M15 (10.763um); 261K to 289K rescaled to 0 to 255; gamma 1.0
    red = rescale(M16-M15, -4, 2, 0, 255)
    grn = 255*(rescale(M15-M14, 0, 15, 0, 1)**0.4)
    blu = rescale(M15, 261, 289, 0, 255)
    return combineRGB(red, grn, blu)

# VIIRS EDR Night Microphysics RGB
def VIIRSEdrNightMicrophysicsRGB(M12, M15, M16):
    # red = M16 - M15 (12.013um - 10.763um); -4C to 2C rescaled to 0 to 255
    # grn = M15 - M12 (10.763um - 3.7um); 0C to 10C rescaled to 0 to 255
    # blu = M15 (10.763um); 243K to 293K rescaled to 0 to 255
    red = rescale(M16-M15, -4, 2, 0, 255)
    grn = rescale(M15-M12, 0, 10, 0, 255)
    blu = rescale(M15, 243, 293, 0, 255)
    return combineRGB(red, grn, blu)

# VIIRS Night Microphysics RGB
def VIIRSEdrNightMicrophysicsRGB(M12, M15, M16):
    # red = M16 - M15 (12.013um - 10.763um); -4C to 2C rescaled to 0 to 255
    # grn = M15 - M12 (10.763um - 3.7um); 0C to 10C rescaled to 0 to 255
    # blu = M15 (10.763um); 243K to 293K rescaled to 0 to 255
    red = rescale(M16-M15, -4, 2, 0, 255)
    grn = rescale(M15-M12, 0, 10, 0, 255)
    blu = rescale(M15, 243, 293, 0, 255)
    return combineRGB(red, grn, blu)

# VIIRS Edr Day Fire RGB
def VIIRSEdrDayFireRGB(I4, I2, I1):
    # http://rammb.cira.colostate.edu/training/visit/quick_guides/VIIRS_Day_Land_Cloud_Fire_RGB_Quick_Guide_10182018.pdf
    # red = I4 (3.7um) - 0C to 60C rescaled to 0 to 255; gamma 0.4
    # grn = I2 (0.86um) - 0% to 100% reflectance rescaled to 0 to 255; gamma 1.0
    # blu = I1 (0.64um) - 0% to 100% reflectance rescaled to 0 to 255; gamma 1.0
    red = 255*(rescale(I4, 273.15, 333.15, 0, 1)**2.5)
    grn = rescale(I2, 0, 100, 0, 255)
    blu = rescale(I1, 0, 100, 0, 255)
    return combineRGB(red, grn, blu)

# VIIRS EDR Fire Temperature RGB
def VIIRSEdrFireTemperatureRGB(M12, M11, M10):
    # http://rammb.cira.colostate.edu/training/visit/quick_guides/VIIRS_Fire_Temperature_RGB_Quick_Guide_10182018.pdf
    # red = M12 (3.7um) - 0C to 70C rescaled to 0 to 255; gamma 0.4
    # grn = M11 (0.86um) - 0% to 100% reflectance rescaled to 0 to 255; gamma 1.0
    # blu = M10 (0.64um) - 0% to 75% reflectance rescaled to 0 to 255; gamma 1.0
    red = 255*(rescale(M12, 273.15, 343.15, 0, 1)**2.5)
    grn = rescale(M11, 0, 100, 0, 255)
    blu = rescale(M10, 0, 75, 0, 255)
    return combineRGB(red, grn, blu)

# VIIRS EDR Natural Color RGB (I-band)
def VIIRSEdrNaturalColorIRGB(I3, I2, I1):
    # red = I3 (1.61um) Reflectance
    # grn = I2 (0.86um) Reflectance
    # blu = I1 (0.64um) Reflectance
    return combineRGB(I3, I2, I1)

# VIIRS Cloud Phase RGB EDR
def VIIRSEdrCloudPhaseRGB(M10, M11, M1):
    # red = M10 (1.61um) 0 - 50 reflectance rescaled to 0 to 255; gamma 1.0
    # grn = M11 (2.25um) 0 - 50 reflectance rescaled to 0 to 255; gamma 1.0
    # blu = M1 (0.412um) 0 - 100 reflectance rescaled to 0 to 255; gamma 1.0
    red = rescale(M10, 0, 50, 0, 255)
    grn = rescale(M11, 0, 50, 0, 255)
    blu = rescale(M1, 0, 100, 0, 255)
    return combineRGB(red, grn, blu)

# VIIRS EDR Cloud Type RGB
def VIIRSEdrCloudTypeRGB(M9, M5, M10):
    # red = M9 (1.378um) 0 - 10 reflectance rescaled to 0 to 255; gamma 1.5
    # grn = M5 (0.672um) 0 - 80 reflectance rescaled to 0 to 255; gamma 0.75
    # blu = M10 (1.612um) 0 - 80 reflectance rescaled to 0 to 255; gamma 1.0
    red = 255*(rescale(M9, 0, 10, 0, 1)**0.66666)
    grn = 255*(rescale(M5, 0, 80, 0, 1)**1.33333)
    blu = rescale(M10, 0, 80, 0, 255)
    return combineRGB(red, grn, blu)

# VIIRS EDR Snowmelt RGB
def VIIRSEdrSnowmeltRGB(M10, M8, M5):
    # https://rammb2.cira.colostate.edu/wp-content/uploads/2021/03/VIIRS_Snowmelt_RGB_Quick_Guide_v3.pdf
    # red = M10 (1.61um)  0 to 100 reflectance rescaled to 0 to 255; gamma 1.0
    # grn = M8 (1.24um)  0 to 100 reflectance rescaled to 0 to 255; gamma 1.0
    # blu = M5 (0.67um)  0 to 100 reflectance rescaled to 0 to 255; gamma 1.0
    red = rescale(M10, 0, 100, 0, 255)
    grn = rescale(M8, 0, 100, 0, 255)
    blu = rescale(M5, 0, 100, 0, 255)
    return combineRGB(red, grn, blu)

# VIIRS EDR Sea Spray RGB
def VIIRSEdrSeaSprayRGB(I1, I2, I4, I5):
    # https://rammb.cira.colostate.edu/training/visit/quick_guides/VIIRS_Sea_Spray_RGB_Quick_Guide_v2.pdf
    # red = I4 (3.7um) - I5 (11.45)  - 0C to 10C rescaled to 0 to 255; gamma 1.0
    # grn = I2 (0.86um) - 1% to 20% reflectance rescaled to 0 to 255; gamma 0.6
    # blu = I1 (0.64um) - 2% to 25% reflectance rescaled to 0 to 255; gamma 0.6

    red = rescale(sub(I4, I5), 0, 10, 0, 255)
    grn = 255*(rescale(I2, .01, .20, 0, 1)**1.66)
    blu = 255*(rescale(I1, .02, .25, 0, 1)**1.66)
    return mycombineRGB(red, grn, blu)
