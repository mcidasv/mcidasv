from collections import namedtuple

from background import _MappedAreaImageFlatField

from edu.wisc.ssec.mcidas import AreaFile
from edu.wisc.ssec.mcidas import AreaFileException
from edu.wisc.ssec.mcidas import AreaFileFactory
from edu.wisc.ssec.mcidas import AreaDirectory
from edu.wisc.ssec.mcidas import AreaDirectoryList
from edu.wisc.ssec.mcidas.adde import AddeURLException

from ucar.unidata.data.imagery import AddeImageDescriptor
from ucar.visad.data import AreaImageFlatField

from edu.wisc.ssec.mcidasv.McIDASV import getStaticMcv
from edu.wisc.ssec.mcidasv.servermanager import EntryStore

from visad.data.mcidas import AreaAdapter

# credit for enum goes to http://stackoverflow.com/a/1695250
def enum(*sequential, **named):
    enums = dict(zip(sequential, range(len(sequential))), **named)
    return type('Enum', (), enums)

def _areaDirectoryToDictionary(areaDirectory):
    d = dict()
    d['bands'] = areaDirectory.getBands()
    d['calinfo'] = areaDirectory.getCalInfo()
    d['calibration-scale-factor'] = areaDirectory.getCalibrationScaleFactor()
    d['calibration-type'] = areaDirectory.getCalibrationType()
    d['calibration-unit-name'] = areaDirectory.getCalibrationUnitName()
    d['center-latitude'] = areaDirectory.getCenterLatitude()
    d['center-latitude-resolution'] = areaDirectory.getCenterLatitudeResolution()
    d['center-longitude'] = areaDirectory.getCenterLongitude()
    d['center-longitude-resolution'] = areaDirectory.getCenterLongitudeResolution()
    d['directory-block'] = areaDirectory.getDirectoryBlock()
    d['elements'] = areaDirectory.getElements()
    d['lines'] = areaDirectory.getLines()
    d['memo-field'] = areaDirectory.getMemoField()
    d['nominal-time'] = areaDirectory.getNominalTime()
    d['band-count'] = areaDirectory.getNumberOfBands()
    d['sensor-id'] = areaDirectory.getSensorID()
    d['sensor-type'] = areaDirectory.getSensorType()
    d['source-type'] = areaDirectory.getSourceType()
    d['start-time'] = areaDirectory.getStartTime()
    return d

DEFAULT_ACCOUNTING = ('idv', '0')

CoordinateSystems = enum('AREA', 'LATLON', 'IMAGE')
AREA = CoordinateSystems.AREA
LATLON = CoordinateSystems.LATLON
IMAGE = CoordinateSystems.IMAGE

Places = enum(ULEFT='Upper Left', CENTER='Center')
ULEFT = Places.ULEFT
CENTER = Places.CENTER

class AddeJythonError(Exception): pass
class AddeJythonInvalidProjectError(AddeJythonError): pass
class AddeJythonInvalidPortError(AddeJythonError): pass
class AddeJythonInvalidUserError(AddeJythonError): pass
class AddeJythonUnknownDataError(AddeJythonError): pass

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

params1 = dict(
    debug=True,
    server='adde.ucar.edu',
    dataset='RTIMAGES',
    descriptor='GE-VIS',
    coordinateSystem=CoordinateSystems.LATLON,
    location=(31.7, -87.4),
    size=(158, 332),
    mag=(-3, -2),
    time=('14:15:00', '14:15:00'),
    band=1,
)

params_area_coords = dict(
    debug=True,
    server='adde.ucar.edu',
    dataset='RTIMAGES',
    descriptor='GE-VIS',
    coordinateSystem=CoordinateSystems.AREA,
    location=(557, 546),
    size=(158, 332),
    mag=(-3, -2),
    time=('14:15:00', '14:15:00'),
    band=1,
)

params_image_coords = dict(
    debug=True,
    server='adde.ucar.edu',
    dataset='RTIMAGES',
    descriptor='GE-VIS',
    coordinateSystem=CoordinateSystems.IMAGE,
    location=(4832, 13384),
    size=(158, 332),
    mag=(-3, -2),
    time=('14:15:00', '14:15:00'),
    band=1,
)

params_sizeall = dict(
    debug=True,
    server='adde.ucar.edu',
    dataset='RTIMAGES',
    descriptor='GE-VIS',
    coordinateSystem=CoordinateSystems.IMAGE,
    location=(4832, 13384),
    size='ALL',
    mag=(-3, -2),
    time=('14:15:00', '14:15:00'),
    band=1,
)


def enableAddeDebug():
    EntryStore.setAddeDebugEnabled(True)


def disableAddeDebug():
    EntryStore.setAddeDebugEnabled(False)


def isAddeDebugEnabled(defaultValue=False):
    return EntryStore.isAddeDebugEnabled(defaultValue)


def getDescriptor(dataset, imageType):
    """Get the descriptor for a local ADDE entry
        
    Args:
        dataset: Dataset field from local ADDE server
        imageType: Image Type field from local ADDE server

    Returns: valid descriptor string or -1 if no match was found
    """
    # get a list of local ADDE server entries
    localEntries = getStaticMcv().getServerManager().getLocalEntries()
    for entry in localEntries:
        if entry.getName() == imageType and entry.getGroup() == dataset:
            # descriptor found; convert to upper case and return it
            desc = str(entry.getDescriptor()).upper()
            return desc
    # no matching descriptor was found so return an error value:
    return -1

def makeLocalADDEDataset(group, mask, format, name=None):
    #isTemp = (name is None)
    pass

def listADDEImages(server, dataset, descriptor,
    accounting=DEFAULT_ACCOUNTING,
    location=None,
    coordinateSystem=CoordinateSystems.LATLON,
    place=Places.CENTER,
    mag=(1, 1),
    position='all',
    unit='BRIT',
    day=None,
    time=None,
    debug=False,
    band=None,
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
    elif coordinateSystem is CoordinateSystems.AREA or coordinateSystem is CoordinateSystems.IMAGE:
        coordSys = 'LINELE'
    else:
        raise ValueError()
    
    if location:
        location = '%s=%s %s' % (coordSys, location[0], location[1])
    
    if day:
        day = '&DAY=%s' % (day)
    
    if size:
        if size == 'ALL':
            size = '99999 99999'
        else:
            size = '%s %s' % (size[0], size[1])
    
    if time:
        time = '%s %s I' % (time[0], time[1])
    
    if band:
        band = '&BAND=%s' % (str(band))
    
    addeUrlFormat = "adde://%s/imagedir?&PORT=112&COMPRESS=gzip&USER=%s&PROJ=%s&VERSION=1&DEBUG=%s&TRACE=0&GROUP=%s&DESCRIPTOR=%s%s&%s&PLACE=%s&SIZE=%s&UNIT=%s&MAG=%s&SPAC=4&NAV=X&AUX=YES&DOC=X%s&TIME=%s&POS=%s"
    url = addeUrlFormat % (server, user, proj, debug, dataset, descriptor, band, location, place, size, unit, mag, day, time, position)
    print url
    adl = AreaDirectoryList(url)
    return adl.getSortedDirs()

def getADDEImage(server, dataset, descriptor,
    accounting=DEFAULT_ACCOUNTING,
    location=None,
    coordinateSystem=CoordinateSystems.LATLON,
    place=Places.CENTER,
    mag=(1, 1),
    position=0,
    unit='BRIT',
    day=None,
    time=None,
    debug=False,
    band=None,
    size=None):
    """Requests data from an ADDE Image server - returns both data and metadata objects

    Args:
        server= ADDE server
        dataset= ADDE dataset group name
        descriptor= ADDE dataset descriptor
        day= day range ('begin date','end date')
        time= ('begin time','end time')
        coordinateSystem= coordinate system to use for retrieving data
                            AREA       AREA file coordinates - zero based
                            LATLON   latitude and longitude coordinates
                            IMAGE     image coordinates - one based
        location=(x,y)
                            x           AREA line, latitude, or IMAGE line
                            y           AREA element, longitude, or IMAGE element
        place = CENTER places specified location (x,y) at center of panel
                            ULEFT places specified location (x,y) at upper-left coordinate of panel
        band= McIDAS band number; must be specified if requesting data from 
              multi-banded image; default=band in image
        unit= calibration unit to request; default = 'BRIT'
        position= time relative (negative values) or absolute (positive values) 
                  position in the dataset; default=0 (most recent image)
        size= number of lines and elements to request; default=(480,640)
        mag= magnification of data (line,element), negative number used for 
            sampling data; default=(1,1)
        accounting= ('user', 'project number') user and project number required 
                    by servers using McIDAS accounting; default = ('idv','0')
    """
    
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
        coordType = 'E'
    elif coordinateSystem is CoordinateSystems.AREA:
        coordSys = 'LINELE'
        coordType = 'A'
    elif coordinateSystem is CoordinateSystems.IMAGE:
        coordSys = 'LINELE'
        coordType = 'I'
    else:
        raise ValueError()
    
    if location:
        location = '&%s=%s %s %s' % (coordSys, location[0], location[1], coordType)
    else:
        location = ''
    
    if day:
        day = '&DAY=%s' % (day)
    else:
        day = ''
    
    if size:
        if size == 'ALL':
            size = '99999 99999'
        else:
            size = '%s %s' % (size[0], size[1])
    
    if time:
        time = '%s %s I' % (time[0], time[1])
    else:
        time = ''
    
    if band:
        band = '&BAND=%s' % (str(band))
    else:
        band = ''
    
    addeUrlFormat = "adde://%s/imagedata?&PORT=112&COMPRESS=gzip&USER=%s&PROJ=%s&VERSION=1&DEBUG=%s&TRACE=0&GROUP=%s&DESCRIPTOR=%s%s%s&PLACE=%s&SIZE=%s&UNIT=%s&MAG=%s&SPAC=4&NAV=X&AUX=YES&DOC=X%s&TIME=%s&POS=%s"
    url = addeUrlFormat % (server, user, proj, debug, dataset, descriptor, band, location, place, size, unit, mag, day, time, position)
    retvals = (-1, -1)
    
    try:
        area = AreaAdapter(url)
        areaDirectory = AreaAdapter.getAreaDirectory(area)
        if debug:
            elements = areaDirectory.getElements()
            lines = areaDirectory.getLines()
            print 'url:', url
            print 'lines=%s elements=%d' % (lines, elements)
        retvals = (_areaDirectoryToDictionary(areaDirectory), area.getData())
    except Exception, err:
        if debug:
            print 'exception: %s\n' % (str(err))
            print 'problem with adde url:', url
    
    return retvals


def testADDEImage(localDataset=None,
    server=None, dataset=None, descriptor=None,
    accounting=DEFAULT_ACCOUNTING,
    location=None,
    coordinateSystem=CoordinateSystems.LATLON,
    place=Places.CENTER,
    mag=(1, 1),
    position=0,
    unit='BRIT',
    day=None,
    time=None,
    debug=False,
    band=None,
    size=None):
    """Requests data from an ADDE Image server - returns both data and metadata objects

    Args:
        server= ADDE server
        dataset= ADDE dataset group name
        descriptor= ADDE dataset descriptor
        day= day range ('begin date','end date')
        time= ('begin time','end time')
        coordinateSystem= coordinate system to use for retrieving data
                            AREA       AREA file coordinates - zero based
                            LATLON   latitude and longitude coordinates
                            IMAGE     image coordinates - one based
        location=(x,y)
                            x           AREA line, latitude, or IMAGE line
                            y           AREA element, longitude, or IMAGE element
        place = CENTER places specified location (x,y) at center of panel
                            ULEFT places specified location (x,y) at upper-left coordinate of panel
        band= McIDAS band number; must be specified if requesting data from
              multi-banded image; default=band in image
        unit= calibration unit to request; default = 'BRIT'
        position= time relative (negative values) or absolute (positive values)
                  position in the dataset; default=0 (most recent image)
        size= number of lines and elements to request; default=(480,640)
        mag= magnification of data (line,element), negative number used for
             sampling data; default=(1,1)
        accounting= ('user', 'project number') user and project number required
                    by servers using McIDAS accounting; default = ('idv','0')
    """
    if localDataset:
        server = localDataset.getAddress()
        dataset = localDataset.getGroup()
        descriptor = localDataset.getDescriptor()
    elif (server is None) or (dataset is None) or (descriptor is None):
        raise TypeError('must provide localDataset or server, dataset, and descriptor values')
    
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
        coordType = 'E'
    elif coordinateSystem is CoordinateSystems.AREA:
        coordSys = 'LINELE'
        coordType = 'A'
    elif coordinateSystem is CoordinateSystems.IMAGE:
        coordSys = 'LINELE'
        coordType = 'I'
    else:
        raise ValueError()
    
    if location:
        location = '&%s=%s %s %s' % (coordSys, location[0], location[1], coordType)
    else:
        location = ''
    
    if day:
        day = '&DAY=%s' % (day)
    else:
        day = ''
    
    if size:
        if size == 'ALL':
            size = '99999 99999'
        else:
            size = '%s %s' % (size[0], size[1])
    
    if time:
        time = '%s %s I' % (time[0], time[1])
    else:
        time = ''
    
    if band:
        band = '&BAND=%s' % (str(band))
    else:
        band = ''
    
    addeUrlFormat = "adde://%s/imagedata?&PORT=112&COMPRESS=gzip&USER=%s&PROJ=%s&VERSION=1&DEBUG=%s&TRACE=0&GROUP=%s&DESCRIPTOR=%s%s%s&PLACE=%s&SIZE=%s&UNIT=%s&MAG=%s&SPAC=4&NAV=X&AUX=YES&DOC=X%s&TIME=%s&POS=%s"
    url = addeUrlFormat % (server, user, proj, debug, dataset, descriptor, band, location, place, size, unit, mag, day, time, position)
    retvals = (-1, -1)
    
    try:
        mapped = _MappedAreaImageFlatField(url)
        retvals = mapped.getDictionary(), mapped
    except AreaFileException, e:
        print 'AreaFileException: url:', url, e
    except AddeURLException, e:
        print 'AddeURLException: url:', url, e
    
    return retvals
