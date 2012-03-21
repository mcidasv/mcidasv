# credit for enum goes to http://stackoverflow.com/a/1695250
def enum(*sequential, **named):
    enums = dict(zip(sequential, range(len(sequential))), **named)
    return type('Enum', (), enums)

DEFAULT_ACCOUNTING = ('idv', '0')

CoordinateSystems = enum('AREA', 'LATLON', 'IMAGE')
Places = enum(ULEFT='Upper Left', CENTER='Center')
# Calibrations = enum(TEMP='Temperature', BRIT='Brightness', 
#     RAW='Raw Counts', PROD='Product', RAD='Radiances', ALB='Albedo')

# alias = ADDE  alias
# server = ADDE server
# dataset = ADDE dataset
# day = date of image
# time = tuple (btime, etime)
# coordinateSystem = coordinate system to use
#   AREA (area coords: "LINELE=557 546 F"; 557=line, 546=ele, F=sys)
#   LAT/LON (latlon coords: "LATLON=31.7 -87.4"; 31.7=lat, -87.4=lon)
#   Image (image coords: "LINELE=4832 13384 I"; 4832=line, 13384=ele, I=image)
# x-coordinate = AREA/Image Line or Latitude
# y-coordinate = AREA/Image Element or Longitude
# position = location of specified coordinate [mcx: place? ]
#   Center (default if latlon coords are used)
#   Upper-Left (default if linele coords are used)
#   Lower-Right [ not valid? ]
# navigationType = navigation type used
#   Image [ is this the default value? where NAV=X? ]
#   LALO
#           why not just use a boolean param like "laloNavigation" that defaults to False?
# unit = (corresponds to the Raw=RAW; Brightness=BRIT; Temperature=TEMP entries in field selector)
# channel = type and value to display (corresponds to field selector entries like "10.7 um IR Surface/Cloud-top Temp")
#   waveLength wavelength
#   waveNumber wavenumber
#   band bandnumber
# mag = either an int or a tuple of two ints
# relativePosition = relative position number (0, -1, -2)
# numberImages = number of images to load
# size = default to None; signifies the tuple (1000, 1000)
import datetime

params1 = dict(
    debug=True,
    server='adde.ucar.edu',
    group='RTIMAGES',
    descriptor='GE-VIS',
    coordinateSystem=CoordinateSystems.LATLON,
    location=(31.7, -87.4),
    place=Places.CENTER,
    size=(158, 332),
    mag=(-3, -2),
    time=('14:15:00', '14:15:00'),
)

def listADDEImages(server, group, descriptor, 
    accounting=DEFAULT_ACCOUNTING,
    location=None,
    coordinateSystem=CoordinateSystems.LATLON,
    place=Places.CENTER,
    mag=(1, 1),
    datasetPosition='all',
    unit='BRIT',
    day=None,
    time=None,
    debug=False,
    band=1,
    size=None):
    user = accounting[0]
    proj = accounting[1]
    debug = str(debug).lower()
    mag = '%s %s' % (mag[0], mag[1])
    
    if place is Places.CENTER:
        place = 'CENTER'
    elif place is Places.ULEFT:
        place = 'ULEFT'
    else:
        raise ValueError()
    
    if coordinateSystem is CoordinateSystems.LATLON:
        coordSys = 'LATLON'
    elif coordinateSystem is CoordinateSystems.LINELE or coordinateSystem is CoordinateSystems.IMAGE:
        coordSys = 'LINELE'
    else:
        raise ValueError()
    
    if location:
        location = '%s=%s %s' % (coordSys, location[0], location[1])
    
    if not day:
        day = datetime.datetime.now().strftime('%Y%j')
    
    if size:
        size = '%s %s' % (size[0], size[1])
    
    if time:
        time = '%s %s I' % (time[0], time[1])
    addeUrlFormat = "adde://%s/imagedir?&PORT=112&COMPRESS=gzip&USER=%s&PROJ=%s&DEBUG=%s&GROUP=%s&DESCR=%s&POS=%s"
    url = addeUrlFormat % (server, user, proj, debug, group, descriptor, datasetPosition)
    print url
    return url

def getADDEImage(server, group, descriptor, 
    accounting=DEFAULT_ACCOUNTING,
    location=None,
    coordinateSystem=CoordinateSystems.LATLON,
    place=Places.CENTER,
    mag=(1, 1),
    datasetPosition=0,
    unit='BRIT',
    day=None,
    time=None,
    debug=False,
    band=1,
    size=None):
    # still need to handle dates+times
    # todo: don't break!
    user = accounting[0]
    proj = accounting[1]
    debug = str(debug).lower()
    mag = '%s %s' % (mag[0], mag[1])
    
    if place is Places.CENTER:
        place = 'CENTER'
    elif place is Places.ULEFT:
        place = 'ULEFT'
    else:
        raise ValueError()
    
    if coordinateSystem is CoordinateSystems.LATLON:
        coordSys = 'LATLON'
    elif coordinateSystem is CoordinateSystems.LINELE or coordinateSystem is CoordinateSystems.IMAGE:
        coordSys = 'LINELE'
    else:
        raise ValueError()
    
    if location:
        location = '%s=%s %s' % (coordSys, location[0], location[1])
    
    if not day:
        day = datetime.datetime.now().strftime('%Y%j')
    
    if size:
        size = '%s %s' % (size[0], size[1])
    
    if time:
        time = '%s %s I' % (time[0], time[1])
    
    addeUrlFormat = "adde://%s/imagedata?&PORT=112&COMPRESS=gzip&USER=%s&PROJ=%s&VERSION=1&DEBUG=%s&TRACE=0&GROUP=%s&DESCRIPTOR=%s&BAND=%s&%s&PLACE=%s&SIZE=%s&UNIT=%s&MAG=%s&SPAC=4&NAV=X&AUX=YES&DOC=X&DAY=%s&TIME=%s&POS=%s"
    url = addeUrlFormat % (server, user, proj, debug, group, descriptor, band, location, place, size, unit, mag, day, time, datasetPosition)
    try:
        area = AreaAdapter(url)
        areaDirectory = AreaAdapter.getAreaDirectory(area)
        if debug:
            
            elements = areaDirectory.getElements()
            lines = areaDirectory.getLines()
            print 'url:', url
            print 'lines=%s elements=%d' % (lines, elements)
        # return areaDirectory, area
    except:
        if debug:
            print 'problem with adde url:', url
        area = -1
        areaDirectory =  -1
    finally:
        return areaDirectory, area.getData()

# def getADDEImage(server, group, descriptor, 
#     accounting=DEFAULT_ACCOUNTING,
#     location=None,
#     coordinateSystem=CoordinateSystems.LATLON,
#     # navigationType=NAVIGATION_TYPES.CENTER,
#     place=Places.CENTER,
#     mag=(1, 1),
#     datasetPosition=0,
#     unit='BRIT',
#     day=None,
#     time=None,
#     debug=False,
#     band=1,
#     size=None):