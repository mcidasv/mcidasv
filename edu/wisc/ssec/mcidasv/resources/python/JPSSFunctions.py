# Note that these functions are still considered to be under development

from edu.wisc.ssec.mcidasv.control import RGBCompositeControl
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
    # https://gina.alaska.edu/wp-content/uploads/2023/02/QuickGuide_True_Color_Final.pdf
    # red = M5 (0.672um) Reflectance; 0% to 100% reflectance rescaled to 0 to 255; gamma 1.0
    # grn = M4 (0.555um) Reflectance; 0% to 100% reflectance rescaled to 0 to 255; gamma 1.0
    # blu = M3 (0.488um) Reflectance; 0% to 100% reflectance rescaled to 0 to 255; gamma 1.0

    inM5 = M5
    M5 = unpackage(M5)
    inM4 = M4
    M4 = unpackage(M4)
    inM3 = M3
    M3 = unpackage(M3)
  
    grd750 = makeGrid(M5, 750)
    
    red = rescale(M5, 0, 1, 0, 255)
    grn = rescale(M4, 0, 1, 0, 255)
    blu = rescale(M3, 0, 1, 0, 255)

    rgb = MultiSpectralDataSource.swathToGrid(grd750, [red, grn, blu], 1.0)

    return package(inM5, rgb)

# VIIRS SDR True Color RGB, Rayleigh Corrected
def VIIRSTrueColorRGBRayleighCorrected(M5, M4, M3, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # https://gina.alaska.edu/wp-content/uploads/2023/02/QuickGuide_True_Color_Final.pdf
    # red = M5 (0.672um) Reflectance; 0% to 100% reflectance rescaled to 0 to 255; gamma 1.0
    # grn = M4 (0.555um) Reflectance; 0% to 100% reflectance rescaled to 0 to 255; gamma 1.0
    # blu = M3 (0.488um) Reflectance; 0% to 100% reflectance rescaled to 0 to 255; gamma 1.0

    # azDiff = SOL_AA - SAT_AA

    inM5 = M5
    M5 = unpackage(M5)
    inM4 = M4
    M4 = unpackage(M4)
    inM3 = M3
    M3 = unpackage(M3)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)

    M5 = RGBCompositeControl.correctRayleighVisible(M5, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.672, 1013.25)
    M4 = RGBCompositeControl.correctRayleighVisible(M4, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.555, 1013.25)
    M3 = RGBCompositeControl.correctRayleighVisible(M3, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.488, 1013.25)

    grd750 = makeGrid(M5, 750)

    red = rescale(M5, 0, 1, 0, 255)
    grn = rescale(M4, 0, 1, 0, 255)
    blu = rescale(M3, 0, 1, 0, 255)

    rgb = MultiSpectralDataSource.swathToGrid(grd750, [red, grn, blu], 1.0)

    return package(inM5, rgb)


# VIIRS SDR Natural Color RGB (M-band)
def VIIRSNaturalColorRGB(M10, M7, M5):
    # red = M10 (1.61um) Reflectance; 0% to 100% reflectance rescaled to 0 to 255; gamma 1.0
    # grn = M7 (0.865um) Reflectance; 0% to 100% reflectance rescaled to 0 to 255; gamma 1.0
    # blu = M5 (0.672um) Reflectance; 0% to 100% reflectance rescaled to 0 to 255; gamma 1.0

    inM10 = M10
    M10 = unpackage(M10)
    inM7 = M7
    M7 = unpackage(M7)
    inM5 = M5
    M5 = unpackage(M5)
  
    grd750 = makeGrid(M10, 750)
    
    red = rescale(M10, 0, 1, 0, 255)
    grn = rescale(M7, 0, 1, 0, 255)
    blu = rescale(M5, 0, 1, 0, 255)

    rgb = MultiSpectralDataSource.swathToGrid(grd750, [red, grn, blu], 1.0)

    return package(inM10, rgb)


# VIIRS SDR Dust RGB
def VIIRSDustRGB(M14, M15, M16):
    # https://rammb2.cira.colostate.edu/wp-content/uploads/2020/01/Dust_RGB_Quick_Guide-1.pdf
    # red = M16 - M15 (12.013um - 10.763um); -6.7C to 2.6C rescaled to 0 to 255; gamma 1.0
    # grn = M15 - M14 (10.763um - 8.55um);   -0.5C to 20C rescaled to 0 to 255; gamma 2.5
    # blu = M15 (10.763um);                  261.2K to 288.7K rescaled to 0 to 255; gamma 1.0

    inM14 = M14
    M14 = unpackage(M14)
    inM15 = M15
    M15 = unpackage(M15)
    inM16 = M16
    M16 = unpackage(M16)

    grd750 = makeGrid(M14, 750)

    red = rescale(M16-M15, -6.7, 2.6, 0, 255)
    grn = 255*(rescale(M15-M14, -0.5, 20, 0, 1)**(1/2.5))
    blu = rescale(M15, 261.2, 288.7, 0, 255)
  
    rgb = MultiSpectralDataSource.swathToGrid(grd750, [red, grn, blu], 1.0)
    return package(inM15, rgb)


# VIIRS SDR Night Microphysics RGB
def VIIRSNightMicrophysicsRGB(M12, M15, M16):
    # https://rammb2.cira.colostate.edu/wp-content/uploads/2020/01/QuickGuide_GOESR_NtMicroRGB_Final_20191206-1.pdf
    # red = M16 - M15 (12.013um - 10.763um); -6.7C to 2.6C rescaled to 0 to 255
    # grn = M15 - M12 (10.763um - 3.7um);    -3.1C to 5.2C rescaled to 0 to 255
    # blu = M15 (10.763um);                  243.55K to 292.65K rescaled to 0 to 255

    inM12 = M12
    M12 = unpackage(M12)
    inM15 = M15
    M15 = unpackage(M15)
    inM16 = M16
    M16 = unpackage(M16)

    grd750 = makeGrid(M12, 750)

    red = rescale(M16-M15, -6.7, 2.6, 0, 255)
    grn = rescale(M15-M12, -3.1, 5.2, 0, 255)
    blu = rescale(M15, 243.55, 292.65, 0, 255)

    rgb = MultiSpectralDataSource.swathToGrid(grd750, [red, grn, blu], 1.0)
    return package(inM12, rgb)
  

# VIIRS SDR Day Fire RGB
def VIIRSDayFireRGB(I4, I2, I1):
    # http://rammb.cira.colostate.edu/training/visit/quick_guides/VIIRS_Day_Land_Cloud_Fire_RGB_Quick_Guide_10182018.pdf
    # red = I4 (3.74um);  0C to 60C rescaled to 0 to 255; gamma 0.4
    # grn = I2 (0.865um); 0% to 100% reflectance rescaled to 0 to 255; gamma 1.0
    # blu = I1 (0.64um);  0% to 100% reflectance rescaled to 0 to 255; gamma 1.0

    inI4 = I4
    I4 = unpackage(I4)
    inI2 = I2
    I2 = unpackage(I2)
    inI1 = I1
    I1 = unpackage(I1)

    grd375 = makeGrid(I4, 375)

    red = 255*(rescale(I4, 273.15, 333.15, 0, 1)**(1/0.4))
    grn = rescale(I2, 0, 1, 0, 255)
    blu = rescale(I1, 0, 1, 0, 255)

    rgb = MultiSpectralDataSource.swathToGrid(grd375, [red, grn, blu], 1.0)
    return package(inI4, rgb)


# VIIRS SDR Fire Temperature RGB
def VIIRSFireTemperatureRGB(M12, M11, M10):
    # http://rammb.cira.colostate.edu/training/visit/quick_guides/VIIRS_Fire_Temperature_RGB_Quick_Guide_10182018.pdf
    # red = M12 (3.7um);  0C to 70C rescaled to 0 to 255; gamma 0.4
    # grn = M11 (2.25um); 0% to 100% reflectance rescaled to 0 to 255; gamma 1.0
    # blu = M10 (1.61um); 0% to 75% reflectance rescaled to 0 to 255; gamma 1.0

    inM12 = M12
    M12 = unpackage(M12)
    inM11 = M11
    M11 = unpackage(M11)
    inM10 = M10
    M10 = unpackage(M10)

    grd750 = makeGrid(M12, 750)

    red = 255*(rescale(M12, 273.15, 343.15, 0, 1)**(1/.4))
    grn = rescale(M11, 0, 1, 0, 255)
    blu = rescale(M10, 0, 0.75, 0, 255)

    rgb = MultiSpectralDataSource.swathToGrid(grd750, [red, grn, blu], 1.0)
    return package(inM12, rgb)


# VIIRS SDR Natural Color RGB (I-band)
def VIIRSNaturalColorIRGB(I3, I2, I1):
    # red = I3 (1.61um);  0% to 100% reflectance rescaled to 0 to 255; gamma 1.0
    # grn = I2 (0.865um); 0% to 100% reflectance rescaled to 0 to 255; gamma 1.0
    # blu = I1 (0.64um);  0% to 100% reflectance rescaled to 0 to 255; gamma 1.0

    inI1 = I1
    I1 = unpackage(I1)
    inI2 = I2
    I2 = unpackage(I2)
    inI3 = I3
    I3 = unpackage(I3)
  
    grd375 = makeGrid(I1, 375)
    
    red = rescale(I3, 0, 1, 0, 255)
    grn = rescale(I2, 0, 1, 0, 255)
    blu = rescale(I1, 0, 1, 0, 255)

    rgb = MultiSpectralDataSource.swathToGrid(grd375, [red, grn, blu], 1.0)
    return package(inI1, rgb)


# VIIRS SDR Day Cloud Phase Distinction M-Band RGB
def VIIRSDayCloudPhaseMRGB(M5, M10, M15):
    # https://rammb.cira.colostate.edu/training/visit/quick_guides/QuickGuide_DayCloudPhaseDistinction_final_v2.pdf
    # red = M15 (10.763um); 7.5C to -53.5C (280.65K to 219.65) rescaled to 0 to 255; gamma 1.0
    # grn = M5 (0.672um);   0 to 78% reflectance rescaled to 0 to 255; gamma 1.0
    # blu = M10 (1.61um);   1 to 59% reflectance rescaled to 0 to 255; gamma 1.0

    inM5 = M5
    M5 = unpackage(M5)
    inM10 = M10
    M10 = unpackage(M10)
    inM15 = M15
    M15 = unpackage(M15)
  
    grd750 = makeGrid(M5, 750)
    
    red = rescale(M15, 280.65, 219.65, 0, 255)
    grn = rescale(M5, 0, 0.78, 0, 255)
    blu = rescale(M10, 0.01, 0.59, 0, 255)

    rgb = MultiSpectralDataSource.swathToGrid(grd750, [red, grn, blu], 1.0)

    return package(inM10, rgb)


# VIIRS SDR Day Cloud Phase Distinction I-Band RGB
def VIIRSDayCloudPhaseIRGB(I1, I3, I5):
    # https://rammb.cira.colostate.edu/training/visit/quick_guides/QuickGuide_DayCloudPhaseDistinction_final_v2.pdf
    # red = I5 (11.45um); 7.5C to -53.5C (280.65K to 219.65) rescaled to 0 to 255; gamma 1.0
    # grn = I1 (0.64um);  0 to 78% reflectance rescaled to 0 to 255; gamma 1.0
    # blu = I3 (1.61um);  1 to 59% reflectance rescaled to 0 to 255; gamma 1.0
    
    inI1 = I1
    I1 = unpackage(I1)
    inI3 = I3
    I3 = unpackage(I3)
    inI5 = I5
    I5 = unpackage(I5)
  
    grd375 = makeGrid(I1, 375)
    
    red = rescale(I5, 280.65, 219.65, 0, 255)
    grn = rescale(I1, 0, 0.78, 0, 255)
    blu = rescale(I3, 0.01, 0.59, 0, 255)
    
    rgb = MultiSpectralDataSource.swathToGrid(grd375, [red, grn, blu], 1.0)

    return package(inI1, rgb)


# VIIRS SDR Cloud Type RGB
def VIIRSCloudTypeRGB(M9, M5, M10):
    # red = M9 (1.378um); 0 - 10 reflectance rescaled to 0 to 255; gamma 0.66
    # grn = M5 (0.672um); 0 - 78 reflectance rescaled to 0 to 255; gamma 1.0
    # blu = M10 (1.61um); 0 - 59 reflectance rescaled to 0 to 255; gamma 1.0

    inM9 = M9
    M9 = unpackage(M9)
    inM5 = M5
    M5 = unpackage(M5)
    inM10 = M10
    M10 = unpackage(M10)
  
    grd750 = makeGrid(M9, 750)
    
    red = 255*(rescale(M9, 0, 0.10, 0, 1)**(1/0.66))
    grn = rescale(M5, 0, 0.78, 0, 255)
    blu = rescale(M10, 0, 0.59, 0, 255)

    rgb = MultiSpectralDataSource.swathToGrid(grd750, [red, grn, blu], 1.0)

    return package(inM9, rgb)


# VIIRS SDR Snowmelt RGB
def VIIRSSnowmeltRGB(M10, M8, M5):
    # https://rammb2.cira.colostate.edu/wp-content/uploads/2021/03/VIIRS_Snowmelt_RGB_Quick_Guide_v3.pdf
    # red = M10 (1.61um); 0 to 100 reflectance rescaled to 0 to 255; gamma 1.0
    # grn = M8 (1.24um);  0 to 100 reflectance rescaled to 0 to 255; gamma 1.0
    # blu = M5 (0.672um); 0 to 100 reflectance rescaled to 0 to 255; gamma 1.0

    inM10 = M10
    M10 = unpackage(M10)
    inM8 = M8
    M8 = unpackage(M8)
    inM5 = M5
    M5 = unpackage(M5)
  
    grd750 = makeGrid(M5, 750)

    red = rescale(M10, 0, 1, 0, 255)
    grn = rescale(M8, 0, 1, 0, 255)
    blu = rescale(M5, 0, 1, 0, 255)

    rgb = MultiSpectralDataSource.swathToGrid(grd750, [red, grn, blu], 1.0)

    return package(inM5, rgb)


# VIIRS SDR Sea Spray RGB
def VIIRSSeaSprayRGB(I1, I2, I4, I5):
    # https://rammb.cira.colostate.edu/training/visit/quick_guides/VIIRS_Sea_Spray_RGB_Quick_Guide_v2.pdf
    # red = I4 (3.74um) - I5 (11.45); 0C to 10C rescaled to 0 to 255; gamma 1.0
    # grn = I2 (0.865um);             1% to 20% reflectance rescaled to 0 to 255; gamma 0.6
    # blu = I1 (0.64um);              2% to 25% reflectance rescaled to 0 to 255; gamma 0.6

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
    grn = 255*(rescale(I2, 0.01, .20, 0, 1)**(1/0.6))
    blu = 255*(rescale(I1, 0.02, .25, 0, 1)**(1/0.6))

    rgb = MultiSpectralDataSource.swathToGrid(grd375, [red, grn, blu], 1.0)
    return package(inI1, rgb)


# VIIRS SDR Ash RGB
def VIIRSAshRGB(M14, M15, M16):
    # https://rammb2.cira.colostate.edu/wp-content/uploads/2025/12/QuickGuide_VIIRS_Ash_RGB.pdf
    # red = M16 - M15 (12.013um - 10.763um); -4C to 2C rescaled to 0 to 255
    # grn = M15 - M14 (10.763um - 8.55um);   -4C to 5C rescaled to 0 to 255
    # blu = M15 (10.763um);                  242.95K to 303.05K rescaled to 0 to 255
    
    inM14 = M14
    M14 = unpackage(M14)
    inM15 = M15
    M15 = unpackage(M15)
    inM16 = M16
    M16 = unpackage(M16)
    
    grd750 = makeGrid(M14, 750)
    
    red = rescale(M16-M15, -4, 2, 0, 255)
    grn = rescale(M15-M14, -4, 5, 0, 255)
    blu = rescale(M15, 242.95, 303.05, 0, 255)
    
    rgb = MultiSpectralDataSource.swathToGrid(grd750, [red, grn, blu], 1.0)

    return package(inM14, rgb)


# VIIRS SDR Day Snow-Fog M-Band RGB
def VIIRSDaySnowFogMRGB(M7, M10, M12, M15):
    # https://rammb.cira.colostate.edu/training/visit/quick_guides/QuickGuide_DaySnowFogRGB_final_v2.pdf
    # red = M7 (0.865um);           0 - 100 reflectance rescaled to 0 to 255; gamma 1.7
    # grn = M10 (1.61um);           0 - 70 reflectance rescaled to 0 to 255; gamma 1.7
    # blu = M12-M15 (3.7um-10.763); 0C - 30C rescaled to 0 to 255; gamma 1.7
    
    inM7 = M7
    M7 = unpackage(M7)
    inM10 = M10
    M10 = unpackage(M10)
    inM12 = M12
    M12 = unpackage(M12)
    inM15 = M15
    M15 = unpackage(M15)
  
    grd750 = makeGrid(M7, 750)
    
    red = 255*(rescale(M7, 0, 1, 0, 1)**(1/1.7))
    grn = 255*(rescale(M10, 0, 0.7, 0, 1)**(1/1.7))
    blu = 255*(rescale(M12-M15, 0, 30, 0, 1)**(1/1.7))
    
    rgb = MultiSpectralDataSource.swathToGrid(grd750, [red, grn, blu], 1.0)

    return package(inM7, rgb)


# VIIRS SDR Day Snow-Fog I-Band RGB
def VIIRSDaySnowFogIRGB(I2, I3, I4, I5):
    # https://rammb.cira.colostate.edu/training/visit/quick_guides/QuickGuide_DaySnowFogRGB_final_v2.pdf
    # red = I2 (0.865um);         0 - 100 reflectance rescaled to 0 to 255; gamma 1.7
    # grn = I3 (1.61um);          0 - 70 reflectance rescaled to 0 to 255; gamma 1.7
    # blu = I4-I5 (3.74um-11.45); 0C - 30C rescaled to 0 to 255; gamma 1.7
    
    inI2 = I2
    I2 = unpackage(I2)
    inI3 = I3
    I3 = unpackage(I3)
    inI4 = I4
    I4 = unpackage(I4)
    inI5 = I5
    I5 = unpackage(I5)
  
    grd375 = makeGrid(I2, 375)
    
    red = 255*(rescale(I2, 0, 1, 0, 1)**(1/1.7))
    grn = 255*(rescale(I3, 0, 0.7, 0, 1)**(1/1.7))
    blu = 255*(rescale(I4-I5, 0, 30, 0, 1)**(1/1.7))
    
    rgb = MultiSpectralDataSource.swathToGrid(grd375, [red, grn, blu], 1.0)

    return package(inI2, rgb)


# VIIRS SDR Cloud Phase RGB
def VIIRSCloudPhaseRGB(M5, M10, M11):
    # https://resources.eumetrain.org/data/7/726/navmenu.php?tab=3&page=1.0.0
    # red = M5  (0.672um) Reflectance; 0% to 50% reflectance rescaled to 0 to 255; gamma 1.0
    # grn = M10 (1.61um)  Reflectance; 0% to 50% reflectance rescaled to 0 to 255; gamma 1.0
    # blu = M11 (2.25um)  Reflectance; 0% to 100% reflectance rescaled to 0 to 255; gamma 1.0

    inM5 = M5
    M5 = unpackage(M5)
    inM10 = M10
    M10 = unpackage(M10)
    inM11 = M11
    M11 = unpackage(M11)
  
    grd750 = makeGrid(M5, 750)
    
    red = rescale(M10, 0, 0.5, 0, 255)
    grn = rescale(M11, 0, 0.5, 0, 255)
    blu = rescale(M5, 0, 1, 0, 255)

    rgb = MultiSpectralDataSource.swathToGrid(grd750, [red, grn, blu], 1.0)

    return package(inM5, rgb)


# VIIRS SDR NGFS Microphysics M-Band RGB
def VIIRSNgfsMicrophysicsMRGB(M13, M15, M16):
    # https://cimss.ssec.wisc.edu/ngfs/images/documentation/QuickGuide_NGFS_Microphysics_jao.pdf
    # red = M15 - M16 (10.763um - 12.013um); -2C to 4C rescaled to 0 to 255 inverted
    # grn = M13 - M15 (4.05um - 10.763um);   -5C to 30C rescaled to 0 to 255 inverted
    # blu = M15 (10.763um);                  243K to 293K rescaled to 0 to 255

    inM13 = M13
    M13 = unpackage(M13)
    inM15 = M15
    M15 = unpackage(M15)
    inM16 = M16
    M16 = unpackage(M16)

    grd750 = makeGrid(M13, 750)

    red = rescale(M15-M16, 4, -2, 0, 255)
    grn = rescale(M13-M15, 30, -5, 0, 255)
    blu = rescale(M15, 243, 293, 0, 255)

    rgb = MultiSpectralDataSource.swathToGrid(grd750, [red, grn, blu], 1.0)
    
    return package(inM13, rgb)


# VIIRS SDR NGFS Microphysics M and I-Band RGB
def VIIRSSDRNgfsMicrophysicsMIRGB(I4, I5, M15, M16):
    # https://cimss.ssec.wisc.edu/ngfs/images/documentation/QuickGuide_NGFS_Microphysics_jao.pdf
    # red = M15 - M16 (10.763um - 12.013um); -2C to 4C rescaled to 0 to 255 inverted
    # grn = I4 - I5 (3.74um - 11.45um);    5C to 40C rescaled to 0 to 255 inverted
    # blu = I5 (11.45um);                  243K to 293K rescaled to 0 to 255
    I4stg = swathToGrid(I4, 375, 1)
    I5stg = swathToGrid(I5, 375, 1)
    M15stg = swathToGrid(M15, 375, 1)
    M16stg = swathToGrid(M16, 375, 1)
    red = rescale(M15stg-M16stg, 4, -2, 0, 255)
    grn = rescale(I4stg-I5stg, 40, 5, 0, 255)
    blu = rescale(I5stg, 243, 293, 0, 255)
    return mycombineRGB(red, grn, blu)


# VIIRS SDR Blowing Snow RGB
def VIIRSBlowingSnowRGB(I1, I3, I4, I5):
    # https://rammb2.cira.colostate.edu/wp-content/uploads/2025/02/VIIRS_Blowing_Snow_RGB_Quick_Guide_v1.pdf
    # red = I1 (0.64um); 10% to 110% rescaled to 0 to 255; gamma 1.0
    # grn = I3 (1.61um); 5% to 40% reflectance rescaled to 0 to 255; gamma 1.0
    # blu = I4 (3.74um) - I5 (11.45um); 0C to 15C temperature rescaled to 0 to 255; gamma 1.0

    inI1 = I1
    I1 = unpackage(I1)
    inI3 = I3
    I3 = unpackage(I3)
    inI4 = I4
    I4 = unpackage(I4)
    inI5 = I5
    I5 = unpackage(I5)

    grd375 = makeGrid(I1, 375)

    red = rescale(I1, 0.1, 1.1, 0, 255)
    grn = rescale(I3, 0.05, 0.4, 0, 255)
    blu = rescale(I4-I5, 0, 15, 0, 255)

    rgb = MultiSpectralDataSource.swathToGrid(grd375, [red, grn, blu], 1.0)
    return package(inI4, rgb)


# VIIRS SDR Sandwich RGB
def sandwichSDR(imgIR, imgVIS, minIR=180, maxIR=280, resolution=750, colorTable='Sandwich', useNaN=False):
    """McIDAS-V implementation of Martin Setvak's "sandwich" imagery.
    This function is designed to work with VIIRS SDR data
    Bowtie deletion lines will be removed by swathToGrid()
    
    Args:
        imgIR: IR image being displayed.
        imgVIS: VIS image being displayed.
        minIR, maxIR: outside these bounds, no IR layer will be visible.
        resolution: resolution of the output display in meters
        colorTable: Name of chosen enhancement table for IR image.
        useNaN: if True, the VIS image won't be visible outside of minIR/maxIR.
        
    Returns:
       rgbImg: An RGB "sandwich".
    """

    imgIR = swathToGrid(imgIR, resolution, 1)
    imgVIS = swathToGrid(imgVIS, resolution, 1)
    
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
    
    scaledIR = (imgIR - int(minIR)) / (int(maxIR) - int(minIR))
    
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
        int(minIR),
        int(maxIR),
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


# VIIRS SDR NDVI
def VIIRSNDVI(I1, I2):
    # I1 = 0.64um;  visible Reflectance
    # I2 = 0.865um; near IR Reflectance

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
    # I1 = 0.64um; visible Reflectance
    # I3 = 1.61um; shortwave IR Reflectance

    inI1 = I1
    I1 = unpackage(I1)
    inI3 = I3
    I3 = unpackage(I3)
    grd375 = makeGrid(I1, 375)

    VISG = MultiSpectralDataSource.swathToGrid(grd375, I1, 1.0)
    SIRG = MultiSpectralDataSource.swathToGrid(grd375, I3, 1.0)

    ndsi = (VISG - SIRG).divide(VISG + SIRG)

    return package(inI1, ndsi)


# VIIRS SDR Burn Area Index
def VIIRSBAI(I1, I2):
    # I1 = 0.64um;  visible Reflectance
    # I2 = 0.865um; near IR Reflectance
    
    inI1 = I1
    I1 = unpackage(I1)
    inI2 = I2
    I2 = unpackage(I2)
    grd375 = makeGrid(I1, 375)
    
    VISG = MultiSpectralDataSource.swathToGrid(grd375, I1, 1.0)
    NIRG = MultiSpectralDataSource.swathToGrid(grd375, I2, 1.0)
    
    bai =  1/((0.1 - NIRG)**2 + (0.06 - VISG)**2)
    
    return package(inI1, bai)


# VIIRS SDR Normalized Burn Ratio
def VIIRSNBR(I2, I3):
    # I2 = 0.865um; near IR Reflectance
    # I3 = 1.61um;  shortwave IR Reflectance
    inI2 = I2
    
    I2 = unpackage(I2)
    inI3 = I3
    I3 = unpackage(I3)
    grd375 = makeGrid(I2, 375)
    
    NIRG = MultiSpectralDataSource.swathToGrid(grd375, I2, 1.0)
    SIRG = MultiSpectralDataSource.swathToGrid(grd375, I3, 1.0)
    nbr = (NIRG - SIRG) / (NIRG + SIRG)
    
    return package(inI2, nbr)


# VIIRS SDR VARI
def VIIRSVARI(M5, M4, M3):
    # VARI = Visible Atmospherically Resistant Index
    # Makes vegetation stand out from surrounding areas
    # red = M5 (0.672um); visible reflectance
    # grn = M4 (0.555um); visible reflectance
    # blu = M3 (0.488um); visible reflectance
    
    inM5 = M5
    M5 = unpackage(M5)
    inM4 = M4
    M4 = unpackage(M4)
    inM3 = M3
    M3 = unpackage(M3)
    grd750 = makeGrid(M5, 750)
    
    M5S = MultiSpectralDataSource.swathToGrid(grd750, M5, 1.0)
    M4S = MultiSpectralDataSource.swathToGrid(grd750, M4, 1.0)
    M3S = MultiSpectralDataSource.swathToGrid(grd750, M3, 1.0)
    
    vari = (M4S - M5S) / (M4S + M5S - M3S)
    
    return package(inM5, vari)


# VIIRS SDR Normalized Difference Built-up Index
def VIIRSNDBI(I2, I3):
    # I2 = 0.865um; near IR Reflectance
    # I3 = 1.61um;  shortwave IR Reflectance
    
    inI2 = I2
    I2 = unpackage(I2)
    inI3 = I3
    I3 = unpackage(I3)
    grd375 = makeGrid(I2, 375)
    
    NIRG = MultiSpectralDataSource.swathToGrid(grd375, I2, 1.0)
    SIRG = MultiSpectralDataSource.swathToGrid(grd375, I3, 1.0)
    
    ndbi = (SIRG - NIRG) / (SIRG + NIRG)
    
    return package(inI2, ndbi)


# The below functions are for VIIRS Imagery EDR data.  EDR data
# do not have bowtie deletion lines, so removal is not needed.


# VIIRS EDR True Color RGB (M-band)
def VIIRSEdrTrueColorRGB(M5, M4, M3):
    # https://gina.alaska.edu/wp-content/uploads/2023/02/QuickGuide_True_Color_Final.pdf
    # red = M5 (0.672um); 0 to 100 reflectance rescaled to 0 to 255; gamma 1.0
    # grn = M4 (0.555um); 0 to 100 reflectance rescaled to 0 to 255; gamma 1.0
    # blu = M3 (0.488um); 0 to 100 reflectance rescaled to 0 to 255; gamma 1.0
    red = rescale(M5, 0, 1, 0, 255)
    grn = rescale(M4, 0, 1, 0, 255)
    blu = rescale(M3, 0, 1, 0, 255)
    return mycombineRGB(red, grn, blu)


# VIIRS EDR Natural Color RGB (M-band)
def VIIRSEdrNaturalColorRGB(M10, M7, M5):
    # red = M10 (1.61um); 0 to 100 reflectance rescaled to 0 to 255; gamma 1.0
    # grn = M7 (0.865um); 0 to 100 reflectance rescaled to 0 to 255; gamma 1.0
    # blu = M5 (0.672um); 0 to 100 reflectance rescaled to 0 to 255; gamma 1.0
    red = rescale(M10, 0, 1, 0, 255)
    grn = rescale(M7, 0, 1, 0, 255)
    blu = rescale(M5, 0, 1, 0, 255)
    return mycombineRGB(red, grn, blu)


#VIIRS EDR Dust RGB
def VIIRSEdrDustRGB(M14, M15, M16):
    # https://rammb2.cira.colostate.edu/wp-content/uploads/2020/01/Dust_RGB_Quick_Guide-1.pdf
    # red = M16 - M15 (12.013um - 10.763um); -6.7C to 2.6C rescaled to 0 to 255; gamma 1.0
    # grn = M15 - M14 (10.763um - 8.55um);   -0.5C to 20C rescaled to 0 to 255; gamma 2.5
    # blu = M15 (10.763um);                  261.2K to 288.7K rescaled to 0 to 255; gamma 1.0
    red = rescale(M16-M15,  -6.7, 2.6, 0, 255)
    grn = 255*(rescale(M15-M14, -0.5, 20, 0, 1)**(1/2.5))
    blu = rescale(M15, 261.2, 288.7, 0, 255)
    return mycombineRGB(red, grn, blu)


# VIIRS EDR Night Microphysics RGB
def VIIRSEdrNightMicrophysicsRGB(M12, M15, M16):
    # https://rammb2.cira.colostate.edu/wp-content/uploads/2020/01/QuickGuide_GOESR_NtMicroRGB_Final_20191206-1.pdf
    # red = M16 - M15 (12.013um - 10.763um); -6.7C to 2.6C rescaled to 0 to 255
    # grn = M15 - M12 (10.763um - 3.7um);    -3.1C to 5.2C rescaled to 0 to 255
    # blu = M15 (10.763um);                  243.55K to 292.65K rescaled to 0 to 255
    red = rescale(M16-M15, -6.7, 2.6, 0, 255)
    grn = rescale(M15-M12, -3.1, 5.2, 0, 255)
    blu = rescale(M15, 243.55, 292.65, 0, 255)
    return mycombineRGB(red, grn, blu)


# VIIRS Edr Day Fire RGB
def VIIRSEdrDayFireRGB(I4, I2, I1):
    # http://rammb.cira.colostate.edu/training/visit/quick_guides/VIIRS_Day_Land_Cloud_Fire_RGB_Quick_Guide_10182018.pdf
    # red = I4 (3.74um);  0C to 60C rescaled to 0 to 255; gamma 0.4
    # grn = I2 (0.865um); 0 to 100 reflectance rescaled to 0 to 255; gamma 1.0
    # blu = I1 (0.64um);  0 to 100 reflectance rescaled to 0 to 255; gamma 1.0
    red = 255*(rescale(I4, 273.15, 333.15, 0, 1)**(1/.4))
    grn = rescale(I2, 0, 1, 0, 255)
    blu = rescale(I1, 0, 1, 0, 255)
    return mycombineRGB(red, grn, blu)


# VIIRS EDR Fire Temperature RGB
def VIIRSEdrFireTemperatureRGB(M12, M11, M10):
    # http://rammb.cira.colostate.edu/training/visit/quick_guides/VIIRS_Fire_Temperature_RGB_Quick_Guide_10182018.pdf
    # red = M12 (3.7um);  0C to 70C rescaled to 0 to 255; gamma 0.4
    # grn = M11 (2.25um); 0 to 100 reflectance rescaled to 0 to 255; gamma 1.0
    # blu = M10 (1.61um); 0 to 75 reflectance rescaled to 0 to 255; gamma 1.0
    red = 255*(rescale(M12, 273.15, 343.15, 0, 1)**(1/.4))
    grn = rescale(M11, 0, 1, 0, 255)
    blu = rescale(M10, 0, 0.75, 0, 255)
    return mycombineRGB(red, grn, blu)


# VIIRS EDR Natural Color RGB (I-band)
def VIIRSEdrNaturalColorIRGB(I3, I2, I1):
    # red = I3 (1.61um);  0 to 100 reflectance rescaled to 0 to 255; gamma 1.0
    # grn = I2 (0.865um); 0 to 100 reflectance rescaled to 0 to 255; gamma 1.0
    # blu = I1 (0.64um);  0 to 100 reflectance rescaled to 0 to 255; gamma 1.0
    red = rescale(I3, 0, 1, 0, 255)
    grn = rescale(I2, 0, 1, 0, 255)
    blu = rescale(I1, 0, 1, 0, 255)
    return mycombineRGB(red, grn, blu)


# VIIRS EDR Day Cloud Type RGB
def VIIRSEdrCloudTypeRGB(M9, M5, M10):
    # red = M9 (1.378um); 0 to 10 reflectance rescaled to 0 to 255; gamma 0.66
    # grn = M5 (0.672um); 0 to 78 reflectance rescaled to 0 to 255; gamma 1.0
    # blu = M10 (1.61um); 0 to 59 reflectance rescaled to 0 to 255; gamma 1.0
    red = 255*(rescale(M9, 0, .10, 0, 1)**(1/0.66))
    grn = rescale(M5, 0, 0.78, 0, 255)
    blu = rescale(M10, 0, 0.59, 0, 255)
    return mycombineRGB(red, grn, blu)


# VIIRS EDR Snowmelt RGB
def VIIRSEdrSnowmeltRGB(M10, M8, M5):
    # https://rammb2.cira.colostate.edu/wp-content/uploads/2021/03/VIIRS_Snowmelt_RGB_Quick_Guide_v3.pdf
    # red = M10 (1.61um); 0 to 100 reflectance rescaled to 0 to 255; gamma 1.0
    # grn = M8 (1.24um);  0 to 100 reflectance rescaled to 0 to 255; gamma 1.0
    # blu = M5 (0.672um); 0 to 100 reflectance rescaled to 0 to 255; gamma 1.0
    red = rescale(M10, 0, 1, 0, 255)
    grn = rescale(M8, 0, 1, 0, 255)
    blu = rescale(M5, 0, 1, 0, 255)
    return mycombineRGB(red, grn, blu)


# VIIRS EDR Sea Spray RGB
def VIIRSEdrSeaSprayRGB(I1, I2, I4, I5):
    # https://rammb.cira.colostate.edu/training/visit/quick_guides/VIIRS_Sea_Spray_RGB_Quick_Guide_v2.pdf
    # red = I4 (3.74um) - I5 (11.45); 0C to 10C rescaled to 0 to 255; gamma 1.0
    # grn = I2 (0.865um);             1 to 20 reflectance rescaled to 0 to 255; gamma 0.6
    # blu = I1 (0.64um);              2 to 25 reflectance rescaled to 0 to 255; gamma 0.6
    red = rescale(sub(I4, I5), 0, 10, 0, 255)
    grn = 255*(rescale(I2, 0.01, 0.20, 0, 1)**(1/.6))
    blu = 255*(rescale(I1, 0.02, 0.25, 0, 1)**(1/.6))
    return mycombineRGB(red, grn, blu)


# VIIRS EDR Ash RGB
def VIIRSEdrAshRGB(M14, M15, M16):
    # https://rammb2.cira.colostate.edu/wp-content/uploads/2025/12/QuickGuide_VIIRS_Ash_RGB.pdf
    # red = M16 - M15 (12.013um - 10.763um); -4C to 2C rescaled to 0 to 255
    # grn = M15 - M14 (10.763um - 8.55um);   -4C to 5C rescaled to 0 to 255
    # blu = M15 (10.763um);                  242.95K to 303.05K rescaled to 0 to 255
    red = rescale(M16-M15, -4, 2.6, 0, 255)
    grn = rescale(M15-M14, -4, 6.3, 0, 255)
    blu = rescale(M15, 242.95, 303.05, 0, 255)
    return mycombineRGB(red, grn, blu)


# VIIRS EDR Day Cloud Phase Distinction M-Band RGB
def VIIRSEdrDayCloudPhaseMRGB(M5, M10, M15):
    # https://rammb.cira.colostate.edu/training/visit/quick_guides/QuickGuide_DayCloudPhaseDistinction_final_v2.pdf
    # red = M15 (10.763um); 7.5C to -53.5C (280.65K to 219.65) rescaled to 0 to 255; gamma 1.0
    # grn = M5 (0.672um);   0 to 78 reflectance rescaled to 0 to 255; gamma 1.0
    # blu = M10 (1.61um);   1 to 59 reflectance rescaled to 0 to 255; gamma 1.0
    red = rescale(M15, 280.65, 219.65, 0, 255)
    grn = rescale(M5, 0, 0.78, 0, 255)
    blu = rescale(M10, 0.01, 0.59, 0, 255)
    return mycombineRGB(red, grn, blu)


# VIIRS EDR Day Cloud Phase Distinction I-Band RGB
def VIIRSEdrDayCloudPhaseIRGB(I1, I3, I5):
    # https://rammb.cira.colostate.edu/training/visit/quick_guides/QuickGuide_DayCloudPhaseDistinction_final_v2.pdf
    # red = I5 (11.45um); 7.5C to -53.5C (280.65K to 219.65) rescaled to 0 to 255; gamma 1.0
    # grn = I1 (0.64um);  0 to 78 reflectance rescaled to 0 to 255; gamma 1.0
    # blu = I3 (1.61um);  1 to 59 reflectance rescaled to 0 to 255; gamma 1.0
    red = rescale(I5, 280.65, 219.65, 0, 255)
    grn = rescale(I1, 0, 0.78, 0, 255)
    blu = rescale(I3, 0.01, 0.59, 0, 255)
    return mycombineRGB(red, grn, blu)


# VIIRS EDR Day Snow-Fog M-Band RGB
def VIIRSEdrDaySnowFogMRGB(M7, M10, M12, M15):
    # https://rammb.cira.colostate.edu/training/visit/quick_guides/QuickGuide_DaySnowFogRGB_final_v2.pdf
    # red = M7 (0.865um);           0 - 100 reflectance rescaled to 0 to 255; gamma 1.7
    # grn = M10 (1.61um);           0 - 70 reflectance rescaled to 0 to 255; gamma 1.7
    # blu = M12-M15 (3.7um-10.763); 0C - 30C rescaled to 0 to 255; gamma 1.7
    red = 255*(rescale(M7, 0, 1, 0, 1)**(1/1.7))
    grn = 255*(rescale(M10, 0, 0.7, 0, 1)**(1/1.7))
    blu = 255*(rescale(M12-M15, 0, 30, 0, 1)**(1/1.7))
    return mycombineRGB(red, grn, blu)


# VIIRS EDR Day Snow-Fog I-Band RGB
def VIIRSEdrDaySnowFogIRGB(I2, I3, I4, I5):
    # https://rammb.cira.colostate.edu/training/visit/quick_guides/QuickGuide_DaySnowFogRGB_final_v2.pdf
    # red = I2 (0.865um);         0 to 100 reflectance rescaled to 0 to 255; gamma 1.7
    # grn = I3 (1.61um);          0 to 70 reflectance rescaled to 0 to 255; gamma 1.7
    # blu = I4-I5 (3.74um-11.45); 0C to 30C rescaled to 0 to 255; gamma 1.7
    red = 255*(rescale(I2, 0, 1, 0, 1)**(1/1.7))
    grn = 255*(rescale(I3, 0, 0.7, 0, 1)**(1/1.7))
    blu = 255*(rescale(I4-I5, 0, 30, 0, 1)**(1/1.7))
    return mycombineRGB(red, grn, blu)


# VIIRS EDR Cloud Phase RGB
def VIIRSEdrCloudPhaseRGB(M5, M10, M11):
    # https://resources.eumetrain.org/data/7/726/navmenu.php?tab=3&page=1.0.0
    # red = M5  (0.672um) Reflectance; 0% to 50% reflectance rescaled to 0 to 255; gamma 1.0
    # grn = M10 (1.61um)  Reflectance; 0% to 50% reflectance rescaled to 0 to 255; gamma 1.0
    # blu = M11 (2.25um)  Reflectance; 0% to 100% reflectance rescaled to 0 to 255; gamma 1.0
    red = rescale(M10, 0, 0.5, 0, 255)
    grn = rescale(M11, 0, 0.5, 0, 255)
    blu = rescale(M5, 0, 1, 0, 255)
    return mycombineRGB(red, grn, blu)


# VIIRS EDR NGFS Microphysics M-Band RGB
def VIIRSEDRNgfsMicrophysicsMRGB(M13, M15, M16):
    # https://cimss.ssec.wisc.edu/ngfs/images/documentation/QuickGuide_NGFS_Microphysics_jao.pdf
    # red = M15 - M16 (10.763um - 12.013um); -2C to 4C rescaled to 0 to 255 inverted
    # grn = M13 - M15 (4.05um - 10.763um);   -5C to 30C rescaled to 0 to 255 inverted
    # blu = M15 (10.763um);                  243K to 293K rescaled to 0 to 255
    red = rescale(M15-M16, 4, -2, 0, 255)
    grn = rescale(M13-M15, 30, -5, 0, 255)
    blu = rescale(M15, 243, 293, 0, 255)
    return mycombineRGB(red, grn, blu)


# VIIRS EDR NGFS Microphysics M and I-Band RGB
def VIIRSEDRNgfsMicrophysicsMIRGB(I4, I5, M15, M16):
    # https://cimss.ssec.wisc.edu/ngfs/images/documentation/QuickGuide_NGFS_Microphysics_jao.pdf
    # red = M15 - M16 (10.763um - 12.013um); -2C to 4C rescaled to 0 to 255 inverted
    # grn = I4 - I5 (3.74um - 11.45um);    5C to 40C rescaled to 0 to 255 inverted
    # blu = I5 (11.45um);                  243K to 293K rescaled to 0 to 255
    red = rescale(resampleGrid(M15-M16,I4), 4, -2, 0, 255)
    grn = rescale(I4-I5, 40, 5, 0, 255)
    blu = rescale(I5, 243, 293, 0, 255)
    return mycombineRGB(red, grn, blu)


# VIIRS EDR Blowing Snow RGB
def VIIRSEdrBlowingSnowRGB(I1, I3, I4, I5):
    # https://rammb2.cira.colostate.edu/wp-content/uploads/2025/02/VIIRS_Blowing_Snow_RGB_Quick_Guide_v1.pdf
    # red = I1 (0.64um); 10% to 110% rescaled to 0 to 255; gamma 1.0
    # grn = I3 (1.61um); 5% to 40% reflectance rescaled to 0 to 255; gamma 1.0
    # blu = I4 (3.74um) - I5 (11.45um); 0C to 15C temperature rescaled to 0 to 255; gamma 1.0
    red = rescale(I1, 0.1, 1.1, 0, 255)
    grn = rescale(I3, 0.05, 0.4, 0, 255)
    blu = rescale(I4-I5, 0, 15, 0, 255)
    return mycombineRGB(red, grn, blu)


# VIIRS EDR Sandwich RGB
def sandwichEDR(imgIR, imgVIS, minIR=180, maxIR=280, colorTable='Sandwich', useNaN=False):
    """McIDAS-V implementation of Martin Setvak's "sandwich" imagery.
    This function is designed to work with VIIRS Imagery EDR data
    
    Args:
        imgIR: IR image being displayed.
        imgVIS: VIS image being displayed.
        minIR, maxIR: outside these bounds, no IR layer will be visible.
        colorTable: Name of chosen enhancement table for IR image.
        useNaN: if True, the VIS image won't be visible outside of minIR/maxIR.
        
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

    a = type(imgIR)
    b = type(imgVIS)
    
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
    
    scaledIR = (imgIR - int(minIR)) / (int(maxIR) - int(minIR))
    
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
        int(minIR),
        int(maxIR),
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


# VIIRS EDR NDVI
def VIIRSEdrNDVI(I1, I2):
    # I1 = 0.64um;  visible Reflectance
    # I2 = 0.865um; near IR Reflectance
    return (I2-I1)/(I1+I2)


# VIIRS EDR NDSI
def VIIRSEdrNDSI(I1, I3):
    # I1 = 0.64um; visible Reflectance
    # I3 = 1.61um; shortwave IR Reflectance
    return (I1-I3)/(I1+I3)


# VIIRS EDR Burn Area Index
def VIIRSEdrBAI(I1, I2):
    # I1 = 0.64um;  visible Reflectance
    # I2 = 0.865um; near IR Reflectance
    return 1/((0.1 - I2)**2 + (0.06 - I1)**2)


# VIIRS EDR Normalized Burn Ratio
def VIIRSEdrNBR(I2, I3):
    # I2 = 0.865um; near IR Reflectance
    # I3 = 1.61um;  shortwave IR Reflectance
    return (I2-I3) / (I2+I3)


# VIIRS EDR VARI
def VIIRSEdrVARI(M5, M4, M3):
    # VARI = Visible Atmospherically Resistant Index
    # Makes vegetation stand out from surrounding areas
    # red = M5 (0.672um) reflectance
    # grn = M4 (0.555um) reflectance
    # blu = M3 (0.488um) reflectance
    return (M4 - M5) / (M4 + M5 - M3)


# VIIRS EDR Normalized Difference Built-up Index
def VIIRSEdrNDBI(I2, I3):
    # I2 = 0.865um; near IR Reflectance
    # I3 = 1.61um;  shortwave IR Reflectance
    return (I3-I2) / (I3+I2)

# Below are functions for performing a rayleigh correction on visible
# and near infrared SDR and imagery EDR data - RMC 04/2025 - Inq 3158

def VIIRSEDRTrueColorRGBRayleighCorrected(M5, M4, M3, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # https://gina.alaska.edu/wp-content/uploads/2023/02/QuickGuide_True_Color_Final.pdf
    # red = M5 (0.672um) Reflectance; 0% to 100% reflectance rescaled to 0 to 255; gamma 1.0
    # grn = M4 (0.555um) Reflectance; 0% to 100% reflectance rescaled to 0 to 255; gamma 1.0
    # blu = M3 (0.488um) Reflectance; 0% to 100% reflectance rescaled to 0 to 255; gamma 1.0
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inM5 = M5
    M5 = unpackage(M5)
    inM4 = M4
    M4 = unpackage(M4)
    inM3 = M3
    M3 = unpackage(M3)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    M5 = RGBCompositeControl.correctRayleighVisible(M5, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.672, 1013.25)
    M4 = RGBCompositeControl.correctRayleighVisible(M4, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.555, 1013.25)
    M3 = RGBCompositeControl.correctRayleighVisible(M3, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.488, 1013.25)
    red = rescale(M5, 0, 1, 0, 255)
    grn = rescale(M4, 0, 1, 0, 255)
    blu = rescale(M3, 0, 1, 0, 255)
    return mycombineRGB(red, grn, blu)

# VIIRS EDR Natural Color RGB (M-band) Rayleigh Corrected
def VIIRSEdrNaturalColorRGBRayleighCorrected(M10, M7, M5, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # red = M10 (1.61um); 0 to 100 reflectance rescaled to 0 to 255; gamma 1.0
    # grn = M7 (0.865um); 0 to 100 reflectance rescaled to 0 to 255; gamma 1.0
    # blu = M5 (0.672um); 0 to 100 reflectance rescaled to 0 to 255; gamma 1.0
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inM5 = M5
    M5 = unpackage(M5)
    inM7 = M7
    M7 = unpackage(M7)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    M5 = RGBCompositeControl.correctRayleighVisible(M5, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.672, 1013.25)
    M7 = RGBCompositeControl.correctRayleighVisible(M7, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.865, 1013.25)
    red = rescale(M10, 0, 1, 0, 255)
    grn = rescale(M7, 0, 1, 0, 255)
    blu = rescale(M5, 0, 1, 0, 255)
    return mycombineRGB(red, grn, blu)

# VIIRS SDR Natural Color RGB (M-Band), Rayleigh Corrected
def VIIRSSdrNaturalColorRGBRayleighCorrected(M10, M7, M5, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # red = M10 (1.61um); 0 to 100 reflectance rescaled to 0 to 255; gamma 1.0
    # grn = M7 (0.865um); 0 to 100 reflectance rescaled to 0 to 255; gamma 1.0
    # blu = M5 (0.672um); 0 to 100 reflectance rescaled to 0 to 255; gamma 1.0
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inM7 = M7
    M7 = unpackage(M7)
    inM5 = M5
    M5 = unpackage(M5)
    inM10 = M10
    M10 = unpackage(M10)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    M7 = RGBCompositeControl.correctRayleighVisible(M7, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.865, 1013.25)
    M5 = RGBCompositeControl.correctRayleighVisible(M5, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.672, 1013.25)
    grd750 = makeGrid(M5, 750)
    red = rescale(M10, 0, 1, 0, 255)
    grn = rescale(M7, 0, 1, 0, 255)
    blu = rescale(M5, 0, 1, 0, 255)
    rgb = MultiSpectralDataSource.swathToGrid(grd750, [red, grn, blu], 1.0)
    return package(inM5, rgb)

# VIIRS Edr Day Fire RGB Rayleigh Corrected
def VIIRSEdrDayFireRGBRayleighCorrected(I4, I2, I1, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # http://rammb.cira.colostate.edu/training/visit/quick_guides/VIIRS_Day_Land_Cloud_Fire_RGB_Quick_Guide_10182018.pdf
    # red = I4 (3.74um);  0C to 60C rescaled to 0 to 255; gamma 0.4
    # grn = I2 (0.865um); 0 to 100 reflectance rescaled to 0 to 255; gamma 1.0
    # blu = I1 (0.64um);  0 to 100 reflectance rescaled to 0 to 255; gamma 1.0
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inI2 = I2
    I2 = unpackage(I2)
    inI1 = I1
    I1 = unpackage(I1)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    I1 = RGBCompositeControl.correctRayleighVisible(I1, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.64, 1013.25)
    I2 = RGBCompositeControl.correctRayleighVisible(I2, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.865, 1013.25)
    red = 255*(rescale(I4, 273.15, 333.15, 0, 1)**(1/.4))
    grn = rescale(I2, 0, 1, 0, 255)
    blu = rescale(I1, 0, 1, 0, 255)
    return mycombineRGB(red, grn, blu)

# VIIRS SDR Day Fire RGB Rayleigh Corrected
def VIIRSSdrDayFireRGBRayleighCorrected(I4, I2, I1, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # http://rammb.cira.colostate.edu/training/visit/quick_guides/VIIRS_Day_Land_Cloud_Fire_RGB_Quick_Guide_10182018.pdf
    # red = I4 (3.74um);  0C to 60C rescaled to 0 to 255; gamma 0.4
    # grn = I2 (0.865um); 0 to 100 reflectance rescaled to 0 to 255; gamma 1.0
    # blu = I1 (0.64um);  0 to 100 reflectance rescaled to 0 to 255; gamma 1.0
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inI4 = I4
    I4 = unpackage(I4)
    inI2 = I2
    I2 = unpackage(I2)
    inI1 = I1
    I1 = unpackage(I1)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    I1 = RGBCompositeControl.correctRayleighVisible(I1, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.64, 1013.25)
    I2 = RGBCompositeControl.correctRayleighVisible(I2, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.865, 1013.25)
    grd375 = makeGrid(I4, 375)
    red = 255*(rescale(I4, 273.15, 333.15, 0, 1)**(1/.4))
    grn = rescale(I2, 0, 1, 0, 255)
    blu = rescale(I1, 0, 1, 0, 255)
    rgb = MultiSpectralDataSource.swathToGrid(grd375, [red, grn, blu], 1.0)
    return package(inI4, rgb)

# VIIRS EDR Natural Color RGB (I-band) Rayleigh Corrected
def VIIRSEdrNaturalColorIRGBRayleighCorrected(I3, I2, I1, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # red = I3 (1.61um);  0 to 100 reflectance rescaled to 0 to 255; gamma 1.0
    # grn = I2 (0.865um); 0 to 100 reflectance rescaled to 0 to 255; gamma 1.0
    # blu = I1 (0.64um);  0 to 100 reflectance rescaled to 0 to 255; gamma 1.0
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inI2 = I2
    I2 = unpackage(I2)
    inI1 = I1
    I1 = unpackage(I1)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    I1 = RGBCompositeControl.correctRayleighVisible(I1, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.64, 1013.25)
    I2 = RGBCompositeControl.correctRayleighVisible(I2, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.865, 1013.25)
    red = rescale(I3, 0, 1, 0, 255)
    grn = rescale(I2, 0, 1, 0, 255)
    blu = rescale(I1, 0, 1, 0, 255)
    return mycombineRGB(red, grn, blu)

# VIIRS SDR Natural Color RGB (I-band) Rayleigh Corrected
def VIIRSSdrNaturalColorIRGBRayleighCorrected(I3, I2, I1, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # red = I3 (1.61um);  0 to 100 reflectance rescaled to 0 to 255; gamma 1.0
    # grn = I2 (0.865um); 0 to 100 reflectance rescaled to 0 to 255; gamma 1.0
    # blu = I1 (0.64um);  0 to 100 reflectance rescaled to 0 to 255; gamma 1.0
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inI3 = I3
    I3 = unpackage(I3)
    inI2 = I2
    I2 = unpackage(I2)
    inI1 = I1
    I1 = unpackage(I1)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    I1 = RGBCompositeControl.correctRayleighVisible(I1, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.64, 1013.25)
    I2 = RGBCompositeControl.correctRayleighVisible(I2, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.865, 1013.25)
    grd375 = makeGrid(I3, 375)
    red = rescale(I3, 0, 1, 0, 255)
    grn = rescale(I2, 0, 1, 0, 255)
    blu = rescale(I1, 0, 1, 0, 255)
    rgb = MultiSpectralDataSource.swathToGrid(grd375, [red, grn, blu], 1.0)
    return package(inI3, rgb)

# VIIRS EDR Day Cloud Phase Distinction M-Band RGB Rayleigh Corrected
def VIIRSEdrDayCloudPhaseMRGBRayleighCorrected(M5, M10, M15, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # https://rammb.cira.colostate.edu/training/visit/quick_guides/QuickGuide_DayCloudPhaseDistinction_final_v2.pdf
    # red = M15 (10.763um); 7.5C to -53.5C (280.65K to 219.65) rescaled to 0 to 255; gamma 1.0
    # grn = M5 (0.672um);   0 to 78 reflectance rescaled to 0 to 255; gamma 1.0
    # blu = M10 (1.61um);   1 to 59 reflectance rescaled to 0 to 255; gamma 1.0
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inM5 = M5
    M5 = unpackage(M5)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    M5 = RGBCompositeControl.correctRayleighVisible(M5, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.672, 1013.25)
    red = rescale(M15, 280.65, 219.65, 0, 255)
    grn = rescale(M5, 0, 0.78, 0, 255)
    blu = rescale(M10, 0.01, 0.59, 0, 255)
    return mycombineRGB(red, grn, blu)

# VIIRS SDR Day Cloud Phase Distinction M-Band RGB Rayleigh Corrected
def VIIRSSdrDayCloudPhaseMRGBRayleighCorrected(M5, M10, M15, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # https://rammb.cira.colostate.edu/training/visit/quick_guides/QuickGuide_DayCloudPhaseDistinction_final_v2.pdf
    # red = M15 (10.763um); 7.5C to -53.5C (280.65K to 219.65) rescaled to 0 to 255; gamma 1.0
    # grn = M5 (0.672um);   0 to 78 reflectance rescaled to 0 to 255; gamma 1.0
    # blu = M10 (1.61um);   1 to 59 reflectance rescaled to 0 to 255; gamma 1.0
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inM5 = M5
    M5 = unpackage(M5)
    inM10 = M10
    M10 = unpackage(M10)
    inM15 = M15
    M15 = unpackage(M15)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    M5 = RGBCompositeControl.correctRayleighVisible(M5, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.672, 1013.25)
    grd750 = makeGrid(M5, 750)
    red = rescale(M15, 280.65, 219.65, 0, 255)
    grn = rescale(M5, 0, 0.78, 0, 255)
    blu = rescale(M10, 0.01, 0.59, 0, 255)
    rgb = MultiSpectralDataSource.swathToGrid(grd750, [red, grn, blu], 1.0)
    return package(inM5, rgb)

# VIIRS EDR Day Cloud Phase Distinction I-Band RGB Rayleigh Corrected
def VIIRSEdrDayCloudPhaseIRGBRayleighCorrected(I1, I3, I5, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # https://rammb.cira.colostate.edu/training/visit/quick_guides/QuickGuide_DayCloudPhaseDistinction_final_v2.pdf
    # red = I5 (11.45um); 7.5C to -53.5C (280.65K to 219.65) rescaled to 0 to 255; gamma 1.0
    # grn = I1 (0.64um);  0 to 78 reflectance rescaled to 0 to 255; gamma 1.0
    # blu = I3 (1.61um);  1 to 59 reflectance rescaled to 0 to 255; gamma 1.0
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inI1 = I1
    I1 = unpackage(I1)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    I1 = RGBCompositeControl.correctRayleighVisible(I1, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.64, 1013.25)
    red = rescale(I5, 280.65, 219.65, 0, 255)
    grn = rescale(I1, 0, 0.78, 0, 255)
    blu = rescale(I3, 0.01, 0.59, 0, 255)
    return mycombineRGB(red, grn, blu)

# VIIRS SDR Day Cloud Phase Distinction I-Band RGB Rayleigh Corrected
def VIIRSSdrDayCloudPhaseIRGBRayleighCorrected(I1, I3, I5, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # https://rammb.cira.colostate.edu/training/visit/quick_guides/QuickGuide_DayCloudPhaseDistinction_final_v2.pdf
    # red = I5 (11.45um); 7.5C to -53.5C (280.65K to 219.65) rescaled to 0 to 255; gamma 1.0
    # grn = I1 (0.64um);  0 to 78 reflectance rescaled to 0 to 255; gamma 1.0
    # blu = I3 (1.61um);  1 to 59 reflectance rescaled to 0 to 255; gamma 1.0
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inI1 = I1
    I1 = unpackage(I1)
    inI3 = I3
    I3 = unpackage(I3)
    inI5 = I5
    I5 = unpackage(I5)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    I1 = RGBCompositeControl.correctRayleighVisible(I1, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.64, 1013.25)
    grd375 = makeGrid(I1, 375)
    red = rescale(I5, 280.65, 219.65, 0, 255)
    grn = rescale(I1, 0, 0.78, 0, 255)
    blu = rescale(I3, 0.01, 0.59, 0, 255)
    rgb = MultiSpectralDataSource.swathToGrid(grd375, [red, grn, blu], 1.0)
    return package(inI1, rgb)

# VIIRS EDR Day Cloud Type RGB Rayleigh Corrected
def VIIRSEdrCloudTypeRGBRayleighCorrected(M9, M5, M10, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # red = M9 (1.378um); 0 to 10 reflectance rescaled to 0 to 255; gamma 0.66
    # grn = M5 (0.672um); 0 to 78 reflectance rescaled to 0 to 255; gamma 1.0
    # blu = M10 (1.61um); 0 to 59 reflectance rescaled to 0 to 255; gamma 1.0
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inM5 = M5
    M5 = unpackage(M5)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    M5 = RGBCompositeControl.correctRayleighVisible(M5, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.672, 1013.25)
    red = 255*(rescale(M9, 0, .10, 0, 1)**(1/0.66))
    grn = rescale(M5, 0, 0.78, 0, 255)
    blu = rescale(M10, 0, 0.59, 0, 255)
    return mycombineRGB(red, grn, blu)

# VIIRS SDR Day Cloud Type RGB Rayleigh Corrected
def VIIRSSdrCloudTypeRGBRayleighCorrected(M9, M5, M10, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # red = M9 (1.378um); 0 to 10 reflectance rescaled to 0 to 255; gamma 0.66
    # grn = M5 (0.672um); 0 to 78 reflectance rescaled to 0 to 255; gamma 1.0
    # blu = M10 (1.61um); 0 to 59 reflectance rescaled to 0 to 255; gamma 1.0
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inM9 = M9
    M9 = unpackage(M9)
    inM5 = M5
    M5 = unpackage(M5)
    inM10 = M10
    M10 = unpackage(M10)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    M5 = RGBCompositeControl.correctRayleighVisible(M5, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.672, 1013.25)
    grd750 = makeGrid(M5, 750)
    red = 255*(rescale(M9, 0, .10, 0, 1)**(1/0.66))
    grn = rescale(M5, 0, 0.78, 0, 255)
    blu = rescale(M10, 0, 0.59, 0, 255)
    rgb = MultiSpectralDataSource.swathToGrid(grd750, [red, grn, blu], 1.0)
    return package(inM5, rgb)

# VIIRS EDR Snowmelt RGB RayleighCorrected
def VIIRSEdrSnowmeltRGBRayleighCorrected(M10, M8, M5, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # https://rammb2.cira.colostate.edu/wp-content/uploads/2021/03/VIIRS_Snowmelt_RGB_Quick_Guide_v3.pdf
    # red = M10 (1.61um); 0 to 100 reflectance rescaled to 0 to 255; gamma 1.0
    # grn = M8 (1.24um);  0 to 100 reflectance rescaled to 0 to 255; gamma 1.0
    # blu = M5 (0.672um); 0 to 100 reflectance rescaled to 0 to 255; gamma 1.0
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inM5 = M5
    M5 = unpackage(M5)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    M5 = RGBCompositeControl.correctRayleighVisible(M5, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.672, 1013.25)
    red = rescale(M10, 0, 1, 0, 255)
    grn = rescale(M8, 0, 1, 0, 255)
    blu = rescale(M5, 0, 1, 0, 255)
    return mycombineRGB(red, grn, blu)

# VIIRS SDR Snowmelt RGB RayleighCorrected
def VIIRSSdrSnowmeltRGBRayleighCorrected(M10, M8, M5, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # https://rammb2.cira.colostate.edu/wp-content/uploads/2021/03/VIIRS_Snowmelt_RGB_Quick_Guide_v3.pdf
    # red = M10 (1.61um); 0 to 100 reflectance rescaled to 0 to 255; gamma 1.0
    # grn = M8 (1.24um);  0 to 100 reflectance rescaled to 0 to 255; gamma 1.0
    # blu = M5 (0.672um); 0 to 100 reflectance rescaled to 0 to 255; gamma 1.0
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inM10 = M10
    M10 = unpackage(M10)
    inM8 = M8
    M8 = unpackage(M8)
    inM5 = M5
    M5 = unpackage(M5)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    M5 = RGBCompositeControl.correctRayleighVisible(M5, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.672, 1013.25)
    grd750 = makeGrid(M5, 750)
    red = rescale(M10, 0, 1, 0, 255)
    grn = rescale(M8, 0, 1, 0, 255)
    blu = rescale(M5, 0, 1, 0, 255)
    rgb = MultiSpectralDataSource.swathToGrid(grd750, [red, grn, blu], 1.0)
    return package(inM5, rgb)

# VIIRS EDR Sea Spray RGB Rayleigh Corrected
def VIIRSEdrSeaSprayRGBRayleighCorrected(I1, I2, I4, I5, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # https://rammb.cira.colostate.edu/training/visit/quick_guides/VIIRS_Sea_Spray_RGB_Quick_Guide_v2.pdf
    # red = I4 (3.74um) - I5 (11.45); 0C to 10C rescaled to 0 to 255; gamma 1.0
    # grn = I2 (0.865um);             1 to 20 reflectance rescaled to 0 to 255; gamma 0.6
    # blu = I1 (0.64um);              2 to 25 reflectance rescaled to 0 to 255; gamma 0.6
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inI2 = I2
    I2 = unpackage(I2)
    inI1 = I1
    I1 = unpackage(I1)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    I1 = RGBCompositeControl.correctRayleighVisible(I1, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.64, 1013.25)
    I2 = RGBCompositeControl.correctRayleighVisible(I2, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.865, 1013.25)
    red = rescale(sub(I4, I5), 0, 10, 0, 255)
    grn = 255*(rescale(I2, 0.01, 0.20, 0, 1)**(1/.6))
    blu = 255*(rescale(I1, 0.02, 0.25, 0, 1)**(1/.6))
    return mycombineRGB(red, grn, blu)

# VIIRS SDR Sea Spray RGB Rayleigh Corrected
def VIIRSSdrSeaSprayRGBRayleighCorrected(I1, I2, I4, I5, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # https://rammb.cira.colostate.edu/training/visit/quick_guides/VIIRS_Sea_Spray_RGB_Quick_Guide_v2.pdf
    # red = I4 (3.74um) - I5 (11.45); 0C to 10C rescaled to 0 to 255; gamma 1.0
    # grn = I2 (0.865um);             1 to 20 reflectance rescaled to 0 to 255; gamma 0.6
    # blu = I1 (0.64um);              2 to 25 reflectance rescaled to 0 to 255; gamma 0.6
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inI1 = I1
    I1 = unpackage(I1)
    inI2 = I2
    I2 = unpackage(I2)
    inI4 = I4
    I4 = unpackage(I4)
    inI5 = I5
    I5 = unpackage(I5)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    I1 = RGBCompositeControl.correctRayleighVisible(I1, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.64, 1013.25)
    I2 = RGBCompositeControl.correctRayleighVisible(I2, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.865, 1013.25)
    grd375 = makeGrid(I2, 375)
    red = rescale(sub(I4, I5), 0, 10, 0, 255)
    grn = 255*(rescale(I2, 0.01, 0.20, 0, 1)**(1/.6))
    blu = 255*(rescale(I1, 0.02, 0.25, 0, 1)**(1/.6))
    rgb = MultiSpectralDataSource.swathToGrid(grd375, [red, grn, blu], 1.0)
    return package(inI1, rgb)

# VIIRS EDR Cloud Phase RGB Rayleigh Corrected
def VIIRSEdrCloudPhaseRGBRayleighCorrected(M5, M10, M11, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # https://resources.eumetrain.org/data/7/726/navmenu.php?tab=3&page=1.0.0
    # red = M5  (0.672um) Reflectance; 0% to 50% reflectance rescaled to 0 to 255; gamma 1.0
    # grn = M10 (1.61um)  Reflectance; 0% to 50% reflectance rescaled to 0 to 255; gamma 1.0
    # blu = M11 (2.25um)  Reflectance; 0% to 100% reflectance rescaled to 0 to 255; gamma 1.0
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inM5 = M5
    M5 = unpackage(M5)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    M5 = RGBCompositeControl.correctRayleighVisible(M5, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.672, 1013.25)
    red = rescale(M10, 0, 0.5, 0, 255)
    grn = rescale(M11, 0, 0.5, 0, 255)
    blu = rescale(M5, 0, 1, 0, 255)
    return mycombineRGB(red, grn, blu)

# VIIRS SDR Cloud Phase RGB Rayleigh Corrected
def VIIRSSdrCloudPhaseRGBRayleighCorrected(M5, M10, M11, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # https://resources.eumetrain.org/data/7/726/navmenu.php?tab=3&page=1.0.0
    # red = M5  (0.672um) Reflectance; 0% to 50% reflectance rescaled to 0 to 255; gamma 1.0
    # grn = M10 (1.61um)  Reflectance; 0% to 50% reflectance rescaled to 0 to 255; gamma 1.0
    # blu = M11 (2.25um)  Reflectance; 0% to 100% reflectance rescaled to 0 to 255; gamma 1.0
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inM5 = M5
    M5 = unpackage(M5)
    inM10 = M10
    M10 = unpackage(M10)
    inM11 = M11
    M11 = unpackage(M11)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    M5 = RGBCompositeControl.correctRayleighVisible(M5, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.672, 1013.25)
    grd750 = makeGrid(M5, 750)
    red = rescale(M10, 0, 0.5, 0, 255)
    grn = rescale(M11, 0, 0.5, 0, 255)
    blu = rescale(M5, 0, 1, 0, 255)
    rgb = MultiSpectralDataSource.swathToGrid(grd750, [red, grn, blu], 1.0)
    return package(inM5, rgb)

# VIIRS EDR Blowing Snow RGB Rayleigh Corrected
def VIIRSEdrBlowingSnowRGBRayleighCorrected(I1, I3, I4, I5, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # https://rammb2.cira.colostate.edu/wp-content/uploads/2025/02/VIIRS_Blowing_Snow_RGB_Quick_Guide_v1.pdf
    # red = I1 (0.64um); 10% to 110% rescaled to 0 to 255; gamma 1.0
    # grn = I3 (1.61um); 5% to 40% reflectance rescaled to 0 to 255; gamma 1.0
    # blu = I4 (3.74um) - I5 (11.45um); 0C to 15C temperature rescaled to 0 to 255; gamma 1.0
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inI1 = I1
    I1 = unpackage(I1)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    I1 = RGBCompositeControl.correctRayleighVisible(I1, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.64, 1013.25)
    red = rescale(I1, 0.1, 1.1, 0, 255)
    grn = rescale(I3, 0.05, 0.4, 0, 255)
    blu = rescale(I4-I5, 0, 15, 0, 255)
    return mycombineRGB(red, grn, blu)

# VIIRS SDR Blowing Snow RGB Rayleigh Corrected
def VIIRSSdrBlowingSnowRGBRayleighCorrected(I1, I3, I4, I5, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # https://rammb2.cira.colostate.edu/wp-content/uploads/2025/02/VIIRS_Blowing_Snow_RGB_Quick_Guide_v1.pdf
    # red = I1 (0.64um); 10% to 110% rescaled to 0 to 255; gamma 1.0
    # grn = I3 (1.61um); 5% to 40% reflectance rescaled to 0 to 255; gamma 1.0
    # blu = I4 (3.74um) - I5 (11.45um); 0C to 15C temperature rescaled to 0 to 255; gamma 1.0
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inI1 = I1
    I1 = unpackage(I1)
    inI3 = I3
    I3 = unpackage(I3)
    inI4 = I4
    I4 = unpackage(I4)
    inI5 = I5
    I5 = unpackage(I5)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    I1 = RGBCompositeControl.correctRayleighVisible(I1, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.64, 1013.25)
    grd375 = makeGrid(I1, 375)
    red = rescale(I1, 0.1, 1.1, 0, 255)
    grn = rescale(I3, 0.05, 0.4, 0, 255)
    blu = rescale(I4-I5, 0, 15, 0, 255)
    rgb = MultiSpectralDataSource.swathToGrid(grd375, [red, grn, blu], 1.0)
    return package(inI1, rgb)

# VIIRS EDR Day Snow-Fog M-Band RGB Rayleigh Corrected
def VIIRSEdrDaySnowFogMRGBRayleighCorrected(M7, M10, M12, M15, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # https://rammb.cira.colostate.edu/training/visit/quick_guides/QuickGuide_DaySnowFogRGB_final_v2.pdf
    # red = M7 (0.865um);           0 - 100 reflectance rescaled to 0 to 255; gamma 1.7
    # grn = M10 (1.61um);           0 - 70 reflectance rescaled to 0 to 255; gamma 1.7
    # blu = M12-M15 (3.7um-10.763); 0C - 30C rescaled to 0 to 255; gamma 1.7
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inM7 = M7
    M7 = unpackage(M7)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    M7 = RGBCompositeControl.correctRayleighVisible(M7, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.865, 1013.25)
    red = 255*(rescale(M7, 0, 1, 0, 1)**(1/1.7))
    grn = 255*(rescale(M10, 0, 0.7, 0, 1)**(1/1.7))
    blu = 255*(rescale(M12-M15, 0, 30, 0, 1)**(1/1.7))
    return mycombineRGB(red, grn, blu)

# VIIRS SDR Day Snow-Fog M-Band RGB Rayleigh Corrected
def VIIRSSdrDaySnowFogMRGBRayleighCorrected(M7, M10, M12, M15, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # https://rammb.cira.colostate.edu/training/visit/quick_guides/QuickGuide_DaySnowFogRGB_final_v2.pdf
    # red = M7 (0.865um);           0 - 100 reflectance rescaled to 0 to 255; gamma 1.7
    # grn = M10 (1.61um);           0 - 70 reflectance rescaled to 0 to 255; gamma 1.7
    # blu = M12-M15 (3.7um-10.763); 0C - 30C rescaled to 0 to 255; gamma 1.7
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inM7 = M7
    M7 = unpackage(M7)
    inM10 = M10
    M10 = unpackage(M10)
    inM12 = M12
    M12 = unpackage(M12)
    inM15 = M15
    M15 = unpackage(M15)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    M7 = RGBCompositeControl.correctRayleighVisible(M7, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.865, 1013.25)
    grd750 = makeGrid(M7, 750)
    red = 255*(rescale(M7, 0, 1, 0, 1)**(1/1.7))
    grn = 255*(rescale(M10, 0, 0.7, 0, 1)**(1/1.7))
    blu = 255*(rescale(M12-M15, 0, 30, 0, 1)**(1/1.7))
    rgb = MultiSpectralDataSource.swathToGrid(grd750, [red, grn, blu], 1.0)
    return package(inM7, rgb)

# VIIRS EDR Day Snow-Fog I-Band RGB Rayleigh Corrected
def VIIRSEdrDaySnowFogIRGBRayleighCorrected(I2, I3, I4, I5, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # https://rammb.cira.colostate.edu/training/visit/quick_guides/QuickGuide_DaySnowFogRGB_final_v2.pdf
    # red = I2 (0.865um);         0 to 100 reflectance rescaled to 0 to 255; gamma 1.7
    # grn = I3 (1.61um);          0 to 70 reflectance rescaled to 0 to 255; gamma 1.7
    # blu = I4-I5 (3.74um-11.45); 0C to 30C rescaled to 0 to 255; gamma 1.7
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inI2 = I2
    I2 = unpackage(I2)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    I2 = RGBCompositeControl.correctRayleighVisible(I2, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.865, 1013.25)
    red = 255*(rescale(I2, 0, 1, 0, 1)**(1/1.7))
    grn = 255*(rescale(I3, 0, 0.7, 0, 1)**(1/1.7))
    blu = 255*(rescale(I4-I5, 0, 30, 0, 1)**(1/1.7))
    return mycombineRGB(red, grn, blu)

# VIIRS SDR Day Snow-Fog I-Band RGB Rayleigh Corrected
def VIIRSEdrDaySnowFogIRGBRayleighCorrected(I2, I3, I4, I5, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # https://rammb.cira.colostate.edu/training/visit/quick_guides/QuickGuide_DaySnowFogRGB_final_v2.pdf
    # red = I2 (0.865um);         0 to 100 reflectance rescaled to 0 to 255; gamma 1.7
    # grn = I3 (1.61um);          0 to 70 reflectance rescaled to 0 to 255; gamma 1.7
    # blu = I4-I5 (3.74um-11.45); 0C to 30C rescaled to 0 to 255; gamma 1.7
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inI2 = I2
    I2 = unpackage(I2)
    inI3 = I3
    I3 = unpackage(I3)
    inI4 = I4
    I4 = unpackage(I4)
    inI5 = I5
    I5 = unpackage(I5)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    I2 = RGBCompositeControl.correctRayleighVisible(I2, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.865, 1013.25)
    grd375 = makeGrid(I2, 375)
    red = 255*(rescale(I2, 0, 1, 0, 1)**(1/1.7))
    grn = 255*(rescale(I3, 0, 0.7, 0, 1)**(1/1.7))
    blu = 255*(rescale(I4-I5, 0, 30, 0, 1)**(1/1.7))
    rgb = MultiSpectralDataSource.swathToGrid(grd375, [red, grn, blu], 1.0)
    return package(inI2, rgb)

# VIIRS EDR NDVI Rayleigh Corrected
def VIIRSEdrNDVIRayleighCorrected(I1, I2, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # I1 = 0.64um;  visible Reflectance
    # I2 = 0.865um; near IR Reflectance
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inI2 = I2
    I2 = unpackage(I2)
    inI1 = I1
    I1 = unpackage(I1)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    I1 = RGBCompositeControl.correctRayleighVisible(I1, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.64, 1013.25)
    I2 = RGBCompositeControl.correctRayleighVisible(I2, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.865, 1013.25)
    return (I2-I1)/(I1+I2)

# VIIRS SDR NDVI Rayleigh Corrected
def VIIRSSdrNDVIRayleighCorrected(I1, I2, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # I1 = 0.64um;  visible Reflectance
    # I2 = 0.865um; near IR Reflectance
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inI1 = I1
    I1 = unpackage(I1)
    inI2 = I2
    I2 = unpackage(I2)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    grd375 = makeGrid(I1, 375)
    I1 = RGBCompositeControl.correctRayleighVisible(I1, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.64, 1013.25)
    I1 = MultiSpectralDataSource.swathToGrid(grd375, I1, 1.0)
    I2 = RGBCompositeControl.correctRayleighVisible(I2, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.865, 1013.25)
    I2 = MultiSpectralDataSource.swathToGrid(grd375, I2, 1.0)
    return (I2-I1)/(I1+I2)

# VIIRS EDR NDSI Rayleigh Corrected
def VIIRSEdrNDSIRayleighCorrected(I1, I3, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # I1 = 0.64um; visible Reflectance
    # I3 = 1.61um; shortwave IR Reflectance
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inI1 = I1
    I1 = unpackage(I1)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    I1 = RGBCompositeControl.correctRayleighVisible(I1, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.64, 1013.25)
    return (I1-I3)/(I1+I3)

# VIIRS SDR NDSI Rayleigh Corrected
def VIIRSSdrNDSIRayleighCorrected(I1, I3, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # I1 = 0.64um;  visible Reflectance
    # I2 = 0.865um; near IR Reflectance
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inI1 = I1
    I1 = unpackage(I1)
    inI3 = I3
    I3 = unpackage(I3)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    grd375 = makeGrid(I1, 375)
    I1 = RGBCompositeControl.correctRayleighVisible(I1, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.64, 1013.25)
    I1 = MultiSpectralDataSource.swathToGrid(grd375, I1, 1.0)
    I3 = MultiSpectralDataSource.swathToGrid(grd375, I3, 1.0)
    return (I1-I3)/(I1+I3)

# VIIRS EDR Burn Area Index Rayleigh Corrected
def VIIRSEdrBAIRayleighCorrected(I1, I2, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # I1 = 0.64um;  visible Reflectance
    # I2 = 0.865um; near IR Reflectance
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inI2 = I2
    I2 = unpackage(I2)
    inI1 = I1
    I1 = unpackage(I1)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    I1 = RGBCompositeControl.correctRayleighVisible(I1, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.64, 1013.25)
    I2 = RGBCompositeControl.correctRayleighVisible(I2, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.865, 1013.25)
    return 1/((0.1 - I2)**2 + (0.06 - I1)**2)

# VIIRS SDR Burn Area Index Rayleigh Corrected
def VIIRSSdrBAIRayleighCorrected(I1, I2, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # I1 = 0.64um;  visible Reflectance
    # I2 = 0.865um; near IR Reflectance
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inI1 = I1
    I1 = unpackage(I1)
    inI2 = I2
    I2 = unpackage(I2)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    grd375 = makeGrid(I1, 375)
    I1 = RGBCompositeControl.correctRayleighVisible(I1, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.64, 1013.25)
    I1 = MultiSpectralDataSource.swathToGrid(grd375, I1, 1.0)
    I2 = RGBCompositeControl.correctRayleighVisible(I2, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.865, 1013.25)
    I2 = MultiSpectralDataSource.swathToGrid(grd375, I2, 1.0)
    return 1/((0.1 - I2)**2 + (0.06 - I1)**2)

# VIIRS EDR Normalized Burn Ratio Rayleigh Corrected
def VIIRSEdrNBRRayleighCorrected(I2, I3, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # I2 = 0.865um; near IR Reflectance
    # I3 = 1.61um;  shortwave IR Reflectance
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inI2 = I2
    I2 = unpackage(I2)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    I2 = RGBCompositeControl.correctRayleighVisible(I2, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.865, 1013.25)
    return (I2-I3) / (I2+I3)

# VIIRS SDR Normalized Burn Ratio Rayleigh Corrected
def VIIRSSdrNBRRayleighCorrected(I2, I3, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # I1 = 0.64um;  visible Reflectance
    # I2 = 0.865um; near IR Reflectance
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inI2 = I2
    I2 = unpackage(I2)
    inI3 = I3
    I3 = unpackage(I3)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    grd375 = makeGrid(I2, 375)
    I2 = RGBCompositeControl.correctRayleighVisible(I2, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.865, 1013.25)
    I2 = MultiSpectralDataSource.swathToGrid(grd375, I2, 1.0)
    I3 = MultiSpectralDataSource.swathToGrid(grd375, I3, 1.0)
    return (I2-I3) / (I2+I3)

# VIIRS EDR VARI Rayleigh Corrected
def VIIRSEdrVARIRayleighCorrected(M5, M4, M3, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # VARI = Visible Atmospherically Resistant Index
    # Makes vegetation stand out from surrounding areas
    # red = M5 (0.672um) reflectance
    # grn = M4 (0.555um) reflectance
    # blu = M3 (0.488um) reflectance
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inM5 = M5
    M5 = unpackage(M5)
    inM4 = M4
    M4 = unpackage(M4)
    inM3 = M3
    M3 = unpackage(M3)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    M5 = RGBCompositeControl.correctRayleighVisible(M5, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.672, 1013.25)
    M4 = RGBCompositeControl.correctRayleighVisible(M4, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.555, 1013.25)
    M3 = RGBCompositeControl.correctRayleighVisible(M3, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.488, 1013.25)
    return (M4 - M5) / (M4 + M5 - M3)

# VIIRS SDR VARI Rayleigh Corrected
def VIIRSSdrVARIRayleighCorrected(M5, M4, M3, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # VARI = Visible Atmospherically Resistant Index
    # Makes vegetation stand out from surrounding areas
    # red = M5 (0.672um) reflectance
    # grn = M4 (0.555um) reflectance
    # blu = M3 (0.488um) reflectance
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inM5 = M5
    M5 = unpackage(M5)
    inM4 = M4
    M4 = unpackage(M4)
    inM3 = M3
    M3 = unpackage(M3)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    grd750 = makeGrid(M5, 750)
    M5 = RGBCompositeControl.correctRayleighVisible(M5, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.672, 1013.25)
    M5 = MultiSpectralDataSource.swathToGrid(grd750, M5, 1.0)
    M4 = RGBCompositeControl.correctRayleighVisible(M4, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.555, 1013.25)
    M4 = MultiSpectralDataSource.swathToGrid(grd750, M4, 1.0)
    M3 = RGBCompositeControl.correctRayleighVisible(M3, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.488, 1013.25)
    M3 = MultiSpectralDataSource.swathToGrid(grd750, M3, 1.0)
    return (M4 - M5) / (M4 + M5 - M3)

# VIIRS EDR Normalized Difference Built-up Index Rayleigh Corrected
def VIIRSEdrNDBIRayleighCorrected(I2, I3, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # I2 = 0.865um; near IR Reflectance
    # I3 = 1.61um;  shortwave IR Reflectance
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inI2 = I2
    I2 = unpackage(I2)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    I2 = RGBCompositeControl.correctRayleighVisible(I2, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.865, 1013.25)
    return (I3-I2) / (I3+I2)

# VIIRS SDR Normalized Difference Built-up Index Rayleigh Corrected
def VIIRSSdrNDBIRayleighCorrected(I2, I3, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # I2 = 0.865um; near IR Reflectance
    # I3 = 1.61um;  shortwave IR Reflectance
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inI2 = I2
    I2 = unpackage(I2)
    inI3 = I3
    I3 = unpackage(I3)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    grd375 = makeGrid(I2, 375)
    I2 = RGBCompositeControl.correctRayleighVisible(I2, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.865, 1013.25)
    I2 = MultiSpectralDataSource.swathToGrid(grd375, I2, 1.0)
    I3 = MultiSpectralDataSource.swathToGrid(grd375, I3, 1.0)
    return (I3-I2) / (I3+I2)

# VIIRS EDR M1 Reflectance Rayleigh Corrected
def VIIRSEDRCorrectM1(M1, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # M1 (0.412um) Reflectance
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inM1 = M1
    M1 = unpackage(M1)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    M1 = RGBCompositeControl.correctRayleighVisible(M1, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.412, 1013.25)
    return M1

# VIIRS SDR M1 Reflectance Rayleigh Corrected
def VIIRSSDRCorrectM1(M1, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # M1 (0.412um) Reflectance
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inM1 = M1
    M1 = unpackage(M1)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    grd750 = makeGrid(M1, 750)
    M1 = RGBCompositeControl.correctRayleighVisible(M1, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.412, 1013.25)
    M1 = MultiSpectralDataSource.swathToGrid(grd750, M1, 1.0)
    return M1

# VIIRS EDR M2 Reflectance Rayleigh Corrected
def VIIRSEDRCorrectM2(M2, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # M2 (0.445um) Reflectance
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inM2 = M2
    M2 = unpackage(M2)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    M2 = RGBCompositeControl.correctRayleighVisible(M2, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.445, 1013.25)
    return M2

# VIIRS SDR M2 Reflectance Rayleigh Corrected
def VIIRSSDRCorrectM2(M2, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # M2 (0.445um) Reflectance
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inM2 = M2
    M2 = unpackage(M2)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    grd750 = makeGrid(M2, 750)
    M2 = RGBCompositeControl.correctRayleighVisible(M2, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.445, 1013.25)
    M2 = MultiSpectralDataSource.swathToGrid(grd750, M2, 1.0)
    return M2

# VIIRS EDR M3 Reflectance Rayleigh Corrected
def VIIRSEDRCorrectM3(M3, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # M3 (0.488um) Reflectance
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inM3 = M3
    M3 = unpackage(M3)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    M3 = RGBCompositeControl.correctRayleighVisible(M3, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.488, 1013.25)
    return M3

# VIIRS SDR M3 Reflectance Rayleigh Corrected
def VIIRSSDRCorrectM3(M3, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # M3 (0.488um) Reflectance
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inM3 = M3
    M3 = unpackage(M3)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    grd750 = makeGrid(M3, 750)
    M3 = RGBCompositeControl.correctRayleighVisible(M3, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.488, 1013.25)
    M3 = MultiSpectralDataSource.swathToGrid(grd750, M3, 1.0)
    return M3

# VIIRS EDR M4 Reflectance Rayleigh Corrected
def VIIRSEDRCorrectM4(M4, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # M4 (0.555um) Reflectance
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inM4 = M2
    M4 = unpackage(M4)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    M4 = RGBCompositeControl.correctRayleighVisible(M4, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.555, 1013.25)
    return M4

# VIIRS SDR M4 Reflectance Rayleigh Corrected
def VIIRSSDRCorrectM4(M4, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # M4 (0.555um) Reflectance
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inM4 = M4
    M4 = unpackage(M4)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    grd750 = makeGrid(M4, 750)
    M4 = RGBCompositeControl.correctRayleighVisible(M4, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.555, 1013.25)
    M4 = MultiSpectralDataSource.swathToGrid(grd750, M4, 1.0)
    return M4

# VIIRS EDR M5 Reflectance Rayleigh Corrected
def VIIRSEDRCorrectM5(M5, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # M5 (0.672um) Reflectance
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inM5 = M5
    M5 = unpackage(M5)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    M5 = RGBCompositeControl.correctRayleighVisible(M5, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.672, 1013.25)
    return M5

# VIIRS SDR M5 Reflectance Rayleigh Corrected
def VIIRSSDRCorrectM5(M5, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # M5 (0.672um) Reflectance
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inM5 = M5
    M5 = unpackage(M5)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    grd750 = makeGrid(M5, 750)
    M5 = RGBCompositeControl.correctRayleighVisible(M5, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.672, 1013.25)
    M5 = MultiSpectralDataSource.swathToGrid(grd750, M5, 1.0)
    return M5

# VIIRS EDR M6 Reflectance Rayleigh Corrected
def VIIRSEDRCorrectM6(M6, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # M6 (0.746um) Reflectance
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inM6 = M6
    M6 = unpackage(M6)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    M6 = RGBCompositeControl.correctRayleighVisible(M6, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.746, 1013.25)
    return M6

# VIIRS SDR M6 Reflectance Rayleigh Corrected
def VIIRSSDRCorrectM6(M6, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # M6 (0.746um) Reflectance
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inM6 = M6
    M6 = unpackage(M6)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    grd750 = makeGrid(M6, 750)
    M6 = RGBCompositeControl.correctRayleighVisible(M6, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.746, 1013.25)
    M6 = MultiSpectralDataSource.swathToGrid(grd750, M6, 1.0)
    return M6

# VIIRS EDR M7 Reflectance Rayleigh Corrected
def VIIRSEDRCorrectM7(M7, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # M7 (0.865um) Reflectance
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inM7 = M7
    M7 = unpackage(M7)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    M7 = RGBCompositeControl.correctRayleighVisible(M7, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.865, 1013.25)
    return M7

# VIIRS SDR M7 Reflectance Rayleigh Corrected
def VIIRSSDRCorrectM7(M7, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # M7 (0.865um) Reflectance
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inM7 = M7
    M7 = unpackage(M7)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    grd750 = makeGrid(M7, 750)
    M7 = RGBCompositeControl.correctRayleighVisible(M7, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.865, 1013.25)
    M7 = MultiSpectralDataSource.swathToGrid(grd750, M7, 1.0)
    return M7

# VIIRS EDR I1 Reflectance Rayleigh Corrected
def VIIRSEDRCorrectI1(I1, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # I1 (0.64um) Reflectance
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inI1 = I1
    I1 = unpackage(I1)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    I1 = RGBCompositeControl.correctRayleighVisible(I1, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.64, 1013.25)
    return I1

# VIIRS SDR I1 Reflectance Rayleigh Corrected
def VIIRSSDRCorrectI1(I1, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # I1 (0.64um) Reflectance
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inI1 = I1
    I1 = unpackage(I1)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    grd375 = makeGrid(I1, 375)
    I1 = RGBCompositeControl.correctRayleighVisible(I1, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.64, 1013.25)
    I1 = MultiSpectralDataSource.swathToGrid(grd375, I1, 1.0)
    return I1

# VIIRS EDR I2 Reflectance Rayleigh Corrected
def VIIRSEDRCorrectI2(I2, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # I2 (0.865um) Reflectance
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inI2 = I2
    I2 = unpackage(I2)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    I2 = RGBCompositeControl.correctRayleighVisible(I2, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.865, 1013.25)
    return I2

# VIIRS SDR I2 Reflectance Rayleigh Corrected
def VIIRSSDRCorrectI2(I2, SOL_ZA, SAT_ZA, SOL_AA, SAT_AA):
    # I2 (0.865um) Reflectance
    # SOL_ZA = Solar Zenith Angle
    # SAT_ZA = Satellite Zenith Angle
    # SOL_AA = Solar Azimuth Angle
    # SAT_AA = Satellite Azimuth Angle
    inI2 = I2
    I2 = unpackage(I2)
    inSOL_ZA = SOL_ZA
    SOL_ZA = unpackage(SOL_ZA)
    inSAT_ZA = SAT_ZA
    SAT_ZA = unpackage(SAT_ZA)
    inSOL_AA = SOL_AA
    SOL_AA = unpackage(SOL_AA)
    inSAT_AA = SAT_AA
    SAT_AA = unpackage(SAT_AA)
    grd375 = makeGrid(I2, 375)
    I2 = RGBCompositeControl.correctRayleighVisible(I2, SAT_ZA, SOL_ZA, SAT_AA, SOL_AA, 0.865, 1013.25)
    I2 = MultiSpectralDataSource.swathToGrid(grd375, I2, 1.0)
    return I2
