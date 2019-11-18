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


def VIIRSTrueColorRGB(M5, M4, M3):
    # red = M5 (0.672um)
    # grn = M4 (0.555um)
    # blu = M3 (0.488um)

    inM5 = M5
    M5 = unpackage(M5)
    inM4 = M4
    M4 = unpackage(M4)
    inM3 = M3
    M3 = unpackage(M3)
  
    grd750 = makeGrid(M5, 750)

    rgb = MultiSpectralDataSource.swathToGrid(grd750, [M5, M4, M3], 1.0)

    return package(inM5, rgb)


def VIIRSNaturalColorRGB(M10, M7, M5):
    # red = M10 (1.61um)
    # grn = M7 (0.865um)
    # blu = M5 (0.672um)

    inM10 = M10
    M10 = unpackage(M10)
    inM7 = M7
    M7 = unpackage(M7)
    inM5 = M5
    M5 = unpackage(M5)
  
    grd750 = makeGrid(M10, 750)

    rgb = MultiSpectralDataSource.swathToGrid(grd750, [M10, M7, M5], 1.0)

    return package(inM10, rgb)
    

def VIIRSNDVI(I1, I2):
    # I1 = 0.64um - visible
    # I1 = 0.865um - near IR

    inI1 = I1
    I1 = unpackage(I1)
    inI2 = I2
    I2 = unpackage(I2)
    grd375 = makeGrid(I1, 375)

    VISG = MultiSpectralDataSource.swathToGrid(grd375, I1, 1.0)
    NIRG = MultiSpectralDataSource.swathToGrid(grd375, I2, 1.0)

    ndvi = (NIRG - VISG).divide(NIRG + VISG)

    return package(inI1, ndvi)


def VIIRSDustRGB(M14, M15, M16):
    # red = M16 - M15 (12.013um - 10.763um); -4C to 2C rescalled to 0 to 255; gamma 1.0
    # grn = M15 - M14 (10.763um - 3.7um); 0C to 15C rescalled to 0 to 255; gamma 2.5
    # blu = M15 (10.763um); 261K to 289K rescalled to 0 to 255; gamma 1.0

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


def VIIRSNightMicrophysicsRGB(M12, M15, M16):
    # red = M16 - M15 (12.013um - 10.763um); -4C to 2C rescalled to 0 to 255
    # grn = M15 - M12 (10.763um - 3.7um); 0C to 10C rescalled to 0 to 255
    # blu = M15 (10.763um); 243K to 293K rescalled to 0 to 255

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
  

def VIIRSDayLandCloudFireRGB(I4, I2, I1):
    # http://rammb.cira.colostate.edu/training/visit/quick_guides/VIIRS_Day_Land_Cloud_Fire_RGB_Quick_Guide_10182018.pdf
    # red = I4 (3.7um) - 0C to 60C rescalled to 0 to 255; gamma 0.4
    # grn = I2 (0.86um) - 0% to 100% reflectance rescalled to 0 to 255; gamma 1.0
    # blu = I1 (0.64um) - 0% to 100% reflectance rescalled to 0 to 255; gamma 1.0

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


def VIIRSFireTemperatureRGB(M12, M11, M10):
    # http://rammb.cira.colostate.edu/training/visit/quick_guides/VIIRS_Fire_Temperature_RGB_Quick_Guide_10182018.pdf
    # red = M12 (3.7um) - 0C to 60C rescalled to 0 to 255; gamma 0.4
    # grn = M11 (0.86um) - 0% to 100% reflectance rescalled to 0 to 255; gamma 1.0
    # blu = M10 (0.64um) - 0% to 100% reflectance rescalled to 0 to 255; gamma 1.0

    inM12 = M12
    M12 = unpackage(M12)
    inM11 = M11
    M11 = unpackage(M11)
    inM10 = M10
    M10 = unpackage(M10)

    grd750 = makeGrid(M12, 750)

    red = 255*(rescale(M12, 273.15, 333.15, 0, 1)**2.5)
    grn = rescale(M11, 0, 100, 0, 255)
    blu = rescale(M10, 0, 75, 0, 255)

    rgb = MultiSpectralDataSource.swathToGrid(grd750, [red, grn, blu], 1.0)
    return package(inM12, rgb)
