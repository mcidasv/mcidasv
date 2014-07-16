import os
import hashlib
import threading
import urllib2

import java.lang.Exception

from collections import namedtuple

from background import _MappedAreaImageFlatField

from java.util.concurrent import Callable
from java.util.concurrent import Executors
from java.util.concurrent import ExecutorCompletionService


from edu.wisc.ssec.mcidas import AreaFile
from edu.wisc.ssec.mcidas import AreaFileException
from edu.wisc.ssec.mcidas import AreaFileFactory
from edu.wisc.ssec.mcidas import AreaDirectory
from edu.wisc.ssec.mcidas import AreaDirectoryList

from edu.wisc.ssec.mcidas.adde import AddeException
from edu.wisc.ssec.mcidas.adde import AddeURLException
from edu.wisc.ssec.mcidas.adde import AddeTextReader
from edu.wisc.ssec.mcidas.adde import AddeSatBands

from ucar.unidata.data.imagery import AddeImageDescriptor
from ucar.visad.data import AreaImageFlatField
from ucar.unidata.util import StringUtil

from edu.wisc.ssec.mcidasv.McIDASV import getStaticMcv

from edu.wisc.ssec.mcidasv.servermanager import EntryStore
from edu.wisc.ssec.mcidasv.servermanager import LocalAddeEntry
from edu.wisc.ssec.mcidasv.servermanager.AddeEntry import EntryStatus
from edu.wisc.ssec.mcidasv.servermanager.EntryTransforms import addeFormatToStr
from edu.wisc.ssec.mcidasv.servermanager.EntryTransforms import serverNameToStr
from edu.wisc.ssec.mcidasv.servermanager.EntryTransforms import strToAddeFormat
from edu.wisc.ssec.mcidasv.servermanager.EntryTransforms import strToServerName
from edu.wisc.ssec.mcidasv.servermanager.LocalAddeEntry import AddeFormat
from edu.wisc.ssec.mcidasv.servermanager.LocalAddeEntry import ServerName

from visad import DateTime

from visad.data.mcidas import AreaAdapter

from java.lang import String
from java.lang import StringBuffer
from java.text import FieldPosition
from java.text import SimpleDateFormat
from java.util import Calendar
from java.util import Date
from java.util import GregorianCalendar
from java.util import TimeZone

# credit for enum goes to http://stackoverflow.com/a/1695250
def enum(*sequential, **named):
    enums = dict(zip(sequential, range(len(sequential))), **named)
    return type('Enum', (), enums)
    
def _checkADDEParameters(hasCoordSys, hasPlace, hasLocation, size):
    retval = True
    
    if isinstance(size, str):
        s = size.upper()
    else:
        s = size
        
    if (hasCoordSys or hasPlace or hasLocation) and (s == 'ALL' or s == 'SAME'):
        retval = False
    return retval
    
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
    
def _normalizeDates(dates):
    # string only ever signifies single day
    # list or set signifies at least one day
    # tuple of two items signifies range of days
    normalized = None
    if dates is None:
        normalized = ['']
    elif isinstance(dates, (str, unicode, String)):
        normalized = [str('&DAY='+dates)]
    elif isinstance(dates, int):
        normalized = ['&DAY='+str(dates)]
    elif len(dates) == 2:
        start, stop = int(dates[0]), int(dates[1])
        normalized = ['&DAY=%s %s' % (str(start), str(stop))]
    else:
        normalized = ['']
    return normalized
    
def _normalizeUnits(units):
    # how to handle units='ALL'?
    normalized = None
    if isinstance(units, str):
        if units == 'ALL':
            normalized = []
        else:
            normalized = [units]
    elif isinstance(units, list) or isinstance(units, tuple) or isinstance(units, set):
        normalized = [unit for unit in units]
    elif not units:
        normalized = ['BRIT']
    return normalized
    
_formats = {
    "AMSR-E Rain Product":                                     AddeFormat.AMSRE_RAIN_PRODUCT,
    "AMRR":                                                    AddeFormat.AMSRE_RAIN_PRODUCT,
    "AMSR-E L 1b":                                             AddeFormat.AMSRE_L1B,
    "AMSR":                                                    AddeFormat.AMSRE_L1B,
    "LRIT GOES-9":                                             AddeFormat.LRIT_GOES9,
    "FSDX_G9":                                                 AddeFormat.LRIT_GOES9,
    "LRIT GOES-10":                                            AddeFormat.LRIT_GOES10,
    "FSDX_G10":                                                AddeFormat.LRIT_GOES10,
    "LRIT GOES-11":                                            AddeFormat.LRIT_GOES11,
    "FSDX_G11":                                                AddeFormat.LRIT_GOES11,
    "LRIT GOES-12":                                            AddeFormat.LRIT_GOES12,
    "FSDX_G12":                                                AddeFormat.LRIT_GOES12,
    "LRIT MET-5":                                              AddeFormat.LRIT_MET5,
    "FSDX_M5":                                                 AddeFormat.LRIT_MET5,
    "LRIT MET-7":                                              AddeFormat.LRIT_MET7,
    "FSDX_M7":                                                 AddeFormat.LRIT_MET7,
    "LRIT MTSAT-1R":                                           AddeFormat.LRIT_MTSAT1R,
    "FSDX_MT":                                                 AddeFormat.LRIT_MTSAT1R,
    "McIDAS Area":                                             AddeFormat.MCIDAS_AREA,
    "AREA":                                                    AddeFormat.MCIDAS_AREA,
    "Meteosat OpenMTP":                                        AddeFormat.METEOSAT_OPENMTP,
    "OMTP":                                                    AddeFormat.METEOSAT_OPENMTP,
    "Metop AVHRR L 1b":                                        AddeFormat.METOP_AVHRR_L1B,
    "LV1B_METOP":                                              AddeFormat.METOP_AVHRR_L1B,
    "MODIS MOD 02 - Level-1B Calibrated Geolocated Radiances": AddeFormat.MODIS_L1B_MOD02,
    "MODS":                                                    AddeFormat.MODIS_L1B_MOD02,
    "MODIS MOD 04 - Aerosol Product":                          AddeFormat.MODIS_L2_MOD04,
    "MOD4":                                                    AddeFormat.MODIS_L2_MOD04,
    "MODIS MOD 06 - Cloud Product":                            AddeFormat.MODIS_L2_MOD06,
    "MODX_06":                                                 AddeFormat.MODIS_L2_MOD06,
    "MODIS MOD 07 - Atmospheric Profiles":                     AddeFormat.MODIS_L2_MOD07,
    "MODX_07":                                                 AddeFormat.MODIS_L2_MOD07,
    "MODIS MOD 28 - Sea Surface Temperature":                  AddeFormat.MODIS_L2_MOD28,
    "MOD8":                                                    AddeFormat.MODIS_L2_MOD28,
    "MODIS MOD 35 - Cloud Mask":                               AddeFormat.MODIS_L2_MOD35,
    "MODX_35":                                                 AddeFormat.MODIS_L2_MOD35,
    "MODIS MOD R - Corrected Reflectance":                     AddeFormat.MODIS_L2_MODR,
    "MODR":                                                    AddeFormat.MODIS_L2_MODR,
    "MSG HRIT FD":                                             AddeFormat.MSG_HRIT_FD,
    "MSGT_FD":                                                 AddeFormat.MSG_HRIT_FD,
    "MSG HRIT HRV":                                            AddeFormat.MSG_HRIT_HRV,
    "MSGT_HRV":                                                AddeFormat.MSG_HRIT_HRV,
    "MTSAT HRIT":                                              AddeFormat.MTSAT_HRIT,
    "MTST":                                                    AddeFormat.MTSAT_HRIT,
    "NOAA AVHRR L 1b":                                         AddeFormat.NOAA_AVHRR_L1B,
    "LV1B_NOAA":                                               AddeFormat.NOAA_AVHRR_L1B,
    "SSMI":                                                    AddeFormat.SSMI,
    "SMIN":                                                    AddeFormat.SSMI,
    "TRMM":                                                    AddeFormat.TRMM,
    "TMIN":                                                    AddeFormat.TRMM,
    "GINI":                                                    AddeFormat.GINI,
}

DEFAULT_ACCOUNTING = ('idv', '0')
DEFAULT_SIZE = (480, 640)

CoordinateSystems = enum(AREA='AREA', LATLON='LATLON', IMAGE='IMAGE')
AREA = CoordinateSystems.AREA
LATLON = CoordinateSystems.LATLON
IMAGE = CoordinateSystems.IMAGE

Places = enum(ULEFT='Upper Left', CENTER='Center')
ULEFT = Places.ULEFT
CENTER = Places.CENTER

MAX_CONCURRENT = 5

pool = Executors.newFixedThreadPool(MAX_CONCURRENT)

ecs = ExecutorCompletionService(pool)

def _satBandUrl(**kwargs):
    # needs at least server, port, debug, user, and proj
    # follow AddeImageChooser.appendMiscKeyValues in determining which extra keys to add
    satbandUrlFormat = "adde://%(server)s/text?&FILE=SATBAND&COMPRESS=gzip&PORT=%(port)s&DEBUG=%(debug)s&VERSION=1&USER=%(user)s&PROJ=%(proj)s"
    return satbandUrlFormat % kwargs
    
# NOTE: remember that Callable means that the "task" returns some kind of
# result from CallableObj.get()!
# RunnableObj.get() just returns null.
class _SatBandReq(Callable):
    def __init__(self, url):
        self.url = url
        self.result = None
        self.thread_used = None
        self.exception = None
        
    def __str__(self):
        if self.exception:
             return "[%s] %s download error %s" % \
                (self.thread_used, self.url, self.exception)
        elif self.completed:
            return "[%s] %s downloaded %dK" % \
                (self.thread_used, self.url, len(self.result)/1024)
                 
        elif self.started:
            return "[%s] %s started" % \
                (self.thread_used, self.url)
        else:
            return "[%s] %s not yet scheduled" % \
                (self.thread_used, self.url)
                
    # needed to implement the Callable interface;
    # any exceptions will be wrapped as either ExecutionException
    # or InterruptedException
    def call(self):
        self.thread_used = threading.currentThread().getName()
        try:
            reader = AddeTextReader(self.url)
            if reader.getStatusCode() > 0:
                lines = StringUtil.listToStringArray(reader.getLinesOfText())
                self.result = AddeSatBands(lines)
        except Exception, ex:
            self.exception = ex
        return self.result
        
class _AreaDirectoryList(object):
    def __init__(self, values=None):
        self.values = values or []
        
    def __len__(self):
        return len(self.values)
        
    def __getitem__(self, key):
        return self.values[key]
        
    def __setitem__(self, key, value):
        self.values[key] = value
        
    def __delitem__(self, key):
        del self.values[key]
        
    def __iter__(self):
        return iter(self.values)
        
    def __reversed__(self):
        return _AreaDirectoryList(reversed(self.values))
        
    def append(self, value):
        self.values.append(value)
        
    def __repr__(self):
        return repr(self.values)
        
    def __str__(self):
        return str(self.values)
        
class AddeJythonError(Exception, java.lang.Exception):
    def __init__(self, ex):
        Exception.__init__(self, ex)
        
        if isinstance(ex, AddeException):
            self.hasErrorCode = ex.hasAddeErrorCode()
            self.addeErrorCode = ex.getAddeErrorCode()
        else:
            self.hasAddeErrorCode = False
            self.addeErrorCode = 0
            
    def hasAddeErrorCode(self):
        return self.hasAddeErrorCode
        
    def getAddeErrorCode(self):
        return self.addeErrorCode
        
class AddeJythonInvalidAccountingError(AddeJythonError):
    pass
    
class AddeJythonAccountingRequiredError(AddeJythonError):
    pass
    
class AddeJythonInvalidDatasetError(AddeJythonError):
    pass
    
class AddeJythonInvalidProjectError(AddeJythonError):
    pass
    
class AddeJythonInvalidPortError(AddeJythonError):
    pass
    
class AddeJythonInvalidUserError(AddeJythonError):
    pass
    
class AddeJythonUnknownDataError(AddeJythonError):
    pass
    
class AddeJythonBandRequiredError(AddeJythonError):
    pass
    
class AddeJythonInvalidUnitError(AddeJythonError):
    pass
    
class AddeJythonInvalidPortionError(AddeJythonError):
    pass
    
class AddeJythonNavigationError(AddeJythonError):
    pass
    
class AddeJythonBandNotPresent(AddeJythonError):
    pass
    
# whew!
class AddeJythonBandNotPresentInSpecifiedUnits(AddeJythonError):
    pass
    
# class AddeJythonUnknownFormatError(AddeJythonError): pass

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
    
    (this wasn't included in the 1.2 release, but enough people are using it
    that we'll want to keep it for backward compatibility.)
        
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
    
def getLocalADDEEntry(dataset, imageType):
    """Get the local ADDE entry matching the given dataset and imageType.
    
    Args:
        dataset: Local ADDE entry dataset name.
        
        imageType: Image type name of local ADDE entry.
        
    Returns: 
        Valid local ADDE entry or None if no match was found.
    """
    # get a list of local ADDE server entries
    localEntries = getStaticMcv().getServerManager().getLocalEntries()
    for entry in localEntries:
        if entry.getName() == imageType and entry.getGroup() == dataset:
            return entry
    # no matching descriptor was found so return an error value:
    return None
    
def makeLocalADDEEntry(dataset, mask, format, imageType=None, save=False):
    """Creates a local ADDE entry in the server table.
    
    Required Args:
        dataset: Name of the group associated with the created dataset.
        imageType: Image type name for local server entry. The image type name is limited to twelve characters or less. (default=format_dataset)
        mask: Directory containing the files used by the created dataset.
        save: True saves entry into the server table. False will cause the entry to be removed at the end of this McIDAS-V session. (default=False)
        format: Data format of the files within the dataset. Either the Full Name or Short Name can be used as valid options:
            
            =========================================================  ============
            Full Name                                                  Short Name  
            =========================================================  ============
            "AMSR-E Rain Product"                                      "AMRR"
            "AMSR-E L 1b"                                              "AMSR"
            "LRIT GOES-9"                                              "FSDX_G9"
            "LRIT GOES-10"                                             "FSDX_G10"
            "LRIT GOES-11"                                             "FSDX_G11"
            "LRIT GOES-12"                                             "FSDX_G12"
            "LRIT MET-5"                                               "FSDX_M5"
            "LRIT MET-7"                                               "FSDX_M7"
            "LRIT MTSAT-1R"                                            "FSDX_MT"
            "McIDAS Area"                                              "AREA"
            "Meteosat OpenMTP"                                         "OMTP"
            "Metop AVHRR L 1b"                                         "LV1B_METOP"
            "MODIS MOD 02 - Level-1B Calibrated Geolocated Radiances"  "MODS"
            "MODIS MOD 04 - Aerosol Product"                           "MOD4"
            "MODIS MOD 06 - Cloud Product"                             "MODX_06"
            "MODIS MOD 07 - Atmospheric Profiles"                      "MODX_07"
            "MODIS MOD 28 - Sea Surface Temperature"                   "MOD8"
            "MODIS MOD 35 - Cloud Mask"                                "MODX_35"
            "MODIS MOD R - Corrected Reflectance"                      "MODR"
            "MSG HRIT FD"                                              "MSGT_FD"
            "MSG HRIT HRV"                                             "MSGT_HRV"
            "MTSAT HRIT"                                               "MTST"
            "NOAA AVHRR L 1b"                                          "LV1B_NOAA"
            "SSMI"                                                     "SMIN"
            "TRMM"                                                     "TMIN"
            "GINI"                                                     "GINI"
            =========================================================  ============
                    
    Returns:
        The newly created local ADDE dataset.
    """
    
    if len(dataset) > 8 or not dataset.isupper() or any(c in dataset for c in "/. []%"):
        raise AddeJythonInvalidDatasetError("Dataset '%s' is not valid." % (dataset))
        
    convertedFormat = _formats.get(format, AddeFormat.INVALID)
    
    if convertedFormat is AddeFormat.INVALID:
        raise AddeJythonError("Unknown format '%s' specified." % (format))
        
    if not imageType:
        imageType = "%s_%s" % (format, dataset)
        
    localEntry = LocalAddeEntry.Builder(imageType, dataset, mask, convertedFormat).status(EntryStatus.ENABLED).temporary((not save)).build()
    getStaticMcv().getServerManager().addEntry(localEntry)
    return localEntry
    
def listADDEImageTimes(localEntry=None,
    server=None, dataset=None, descriptor=None,
    accounting=DEFAULT_ACCOUNTING,
    location=None,
    coordinateSystem=None,
    place=None,
    mag=None,
    position=None,
    unit=None,
    day=None,
    time=None,
    debug=False,
    band=None,
    size=None,
    showUrls=True):
    
    if localEntry:
        server = localEntry.getAddress()
        dataset = localEntry.getGroup()
        descriptor = localEntry.getDescriptor().upper()
    elif (server is None) or (dataset is None) or (descriptor is None):
        raise TypeError("must provide localEntry or server, dataset, and descriptor values.")
        
    if server == "localhost" or server == "127.0.0.1":
        port = EntryStore.getLocalPort()
    else:
        port = "112"
        
    # server = '%s:%s' % (server, port)
    
    user = accounting[0]
    proj = accounting[1]
    debug = str(debug).lower()
    
    hasCoordSys = coordinateSystem or False
    hasPlace = place or False
    hasLocation = location or False
    
    if not _checkADDEParameters(hasCoordSys, hasPlace, hasLocation, size):
        raise ValueError("Cannot specify coordinate system, place, or location while also providing a size of '%s'." % (size))
        
    if mag:
        mag = '&MAG=%s %s' % (mag[0], mag[1])
    else:
        mag = ''
        
    if unit:
        origUnit = unit
        unit = '&UNIT=%s' % (unit)
    else:
        # origUnit = None
        unit = ''
        
    if place is Places.CENTER:
        place = '&PLACE=CENTER'
    elif place is Places.ULEFT:
        place = '&PLACE=ULEFT'
    else:
        # raise ValueError()
        place = ''
        
    if coordinateSystem is CoordinateSystems.LATLON or coordinateSystem is None:
        coordSys = 'LATLON'
    elif coordinateSystem is CoordinateSystems.AREA or coordinateSystem is CoordinateSystems.IMAGE:
        coordSys = 'LINELE'
    else:
        raise ValueError("Invalid coordinateSystem value.")
        
    if location:
        location = '&%s=%s %s' % (coordSys, location[0], location[1])
    else:
        location = '%s=0 0' % (coordSys)
        
    if size:
        if size == 'ALL':
            size = '&SIZE=99999 99999'
        else:
            size = '&SIZE=%s %s' % (size[0], size[1])
    else:
        size = ''
        
    if time:
        time = '&TIME=%s %s I' % (time[0], time[1])
    else:
        time = ''
        
    if band:
        band = '&BAND=%s' % (str(band))
    else:
        band = '&BAND=ALL'
        
    if position is not None:
        if isinstance(position, int):
            position = '&POS=%s' % (position)
        elif isinstance(position, tuple):
            if len(position) != 2:
                raise ValueError('position range may only contain values for the beginning and end of a range.')
            position = '&POS=%s %s' % (str(position[0]), str(position[1]))
        else:
            position = '&POS=%s' % (str(position).upper())
    else:
        position = '&POS=0'
        
    tz = TimeZone.getTimeZone('Z')
    
    dateFormat = SimpleDateFormat()
    dateFormat.setTimeZone(tz)
    dateFormat.applyPattern('yyyyDDD')
    
    timeFormat = SimpleDateFormat();
    timeFormat.setTimeZone(tz)
    timeFormat.applyPattern('HH:mm:ss')
    
    addeUrlFormat = "adde://%(server)s/imagedirectory?&PORT=%(port)s&COMPRESS=gzip&USER=%(user)s&PROJ=%(proj)s&VERSION=1&DEBUG=%(debug)s&TRACE=0&GROUP=%(dataset)s&DESCRIPTOR=%(descriptor)s%(band)s%(location)s%(place)s%(size)s%(unit)s%(mag)s%(day)s%(time)s%(position)s"
    
    urls = []
    areaDirectories = []
    
    dates = _normalizeDates(day)
    for date in dates:
        formatValues = {
            'server': server,
            'port': port,
            'user': user,
            'proj': proj,
            'debug': debug,
            'dataset': dataset,
            'descriptor': descriptor,
            'band': band,
            'location': location,
            'place': place,
            'size': size,
            'unit': unit,
            'mag': mag,
            'day': date,
            'time': time,
            'position': position,
        }
        url = addeUrlFormat % formatValues
        if showUrls:
            print url
        adl = AreaDirectoryList(url)
        results = adl.getSortedDirs()
        for imageTimes in results:
            for areaDirectory in imageTimes:
                urls.append(url)
                areaDirectories.append(areaDirectory)
                
    uniques = set()
    times = []
    for d in areaDirectories:
        dt = DateTime(d.getNominalTime())
        if dt not in uniques:
            d = { 
                'day': str(dt.formattedString('yyyyDDD', tz)), 
                'time': str(dt.formattedString('HH:mm:ss', tz)),
            }
            times.append(d)
            uniques.add(dt)
    uniques = None
    return sorted(times)
    
def listADDEImages(localEntry=None,
    server=None, dataset=None, descriptor=None,
    accounting=DEFAULT_ACCOUNTING,
    location=None,
    coordinateSystem=None,
    place=None,
    mag=None,
    position=None,
    unit=None,
    day=None,
    time=None,
    debug=False,
    band=None,
    size=None,
    showUrls=True):
    """Creates a list of ADDE images.
    
    Args:
        localEntry: Local ADDE dataset.
        server: ADDE server.
        dataset: ADDE dataset group name.
        descriptor: ADDE dataset descriptor.
        day: Day range. ('begin date', 'end date')
        time: ('begin time', 'end time')
        position: Position number. Values may be integers or the string "ALL". (default=0)
        band: McIDAS band number; only images that have matching band number will be returned.
        accounting: ('user', 'project number') User and project number required by servers using McIDAS accounting. default = ('idv','0')
        
    Returns:
        ADDE image matching the given criteria, if any.
    """
    if localEntry:
        server = localEntry.getAddress()
        dataset = localEntry.getGroup()
        descriptor = localEntry.getDescriptor().upper()
    elif (server is None) or (dataset is None) or (descriptor is None):
        raise TypeError("must provide localEntry or server, dataset, and descriptor values.")
        
    if server == "localhost" or server == "127.0.0.1":
        port = EntryStore.getLocalPort()
    else:
        port = "112"
        
    # server = '%s:%s' % (server, port)
    
    user = accounting[0]
    proj = accounting[1]
    debug = str(debug).lower()
    
    hasCoordSys = coordinateSystem or False
    hasPlace = place or False
    hasLocation = location or False
    
    if not _checkADDEParameters(hasCoordSys, hasPlace, hasLocation, size):
        raise ValueError("Cannot specify coordinate system, place, or location while also providing a size of '%s'." % (size))
        
    if mag:
        mag = '&MAG=%s %s' % (mag[0], mag[1])
    else:
        mag = ''
        
    if unit:
        origUnit = unit
        unit = '&UNIT=%s' % (unit)
    else:
        # origUnit = None
        unit = ''
        
    if place is Places.CENTER:
        place = '&PLACE=CENTER'
    elif place is Places.ULEFT:
        place = '&PLACE=ULEFT'
    else:
        # raise ValueError()
        place = ''
        
    if coordinateSystem is CoordinateSystems.LATLON or coordinateSystem is None:
        coordSys = 'LATLON'
    elif coordinateSystem is CoordinateSystems.AREA or coordinateSystem is CoordinateSystems.IMAGE:
        coordSys = 'LINELE'
    else:
        raise ValueError("Invalid coordinateSystem value.")
        
    if location:
        location = '&%s=%s %s' % (coordSys, location[0], location[1])
    else:
        location = '&%s=0 0' % (coordSys)
        
    if size:
        if size == 'ALL':
            size = '&SIZE=99999 99999'
        else:
            size = '&SIZE=%s %s' % (size[0], size[1])
    else:
        size = ''
        
    if time:
        time = '&TIME=%s %s I' % (time[0], time[1])
    else:
        time = ''
        
    if band:
        band = '&BAND=%s' % (str(band))
    else:
        band = '&BAND=ALL'
        
    if position is not None:
        if isinstance(position, int):
            position = '&POS=%s' % (position)
        elif isinstance(position, tuple):
            if len(position) != 2:
                raise ValueError('position range may only contain values for the beginning and end of a range.')
            position = '&POS=%s %s' % (str(position[0]), str(position[1]))
        else:
            position = '&POS=%s' % (str(position).upper())
    else:
        position = '&POS=0'
        
    tz = TimeZone.getTimeZone('Z')
    
    dateFormat = SimpleDateFormat()
    dateFormat.setTimeZone(tz)
    dateFormat.applyPattern('yyyyDDD')
    
    timeFormat = SimpleDateFormat()
    timeFormat.setTimeZone(tz)
    timeFormat.applyPattern('HH:mm:ss')
    
    addeUrlFormat = "adde://%(server)s/imagedirectory?&PORT=%(port)s&COMPRESS=gzip&USER=%(user)s&PROJ=%(proj)s&VERSION=1&DEBUG=%(debug)s&TRACE=0&GROUP=%(dataset)s&DESCRIPTOR=%(descriptor)s%(band)s%(location)s%(place)s%(size)s%(unit)s%(mag)s%(day)s%(time)s%(position)s"
    
    urls = []
    areaDirectories = []
    
    dates = _normalizeDates(day)
    for date in dates:
        formatValues = {
            'server': server,
            'port': port,
            'user': user,
            'proj': proj,
            'debug': debug,
            'dataset': dataset,
            'descriptor': descriptor,
            'band': band,
            'location': location,
            'place': place,
            'size': size,
            'unit': unit,
            'mag': mag,
            'day': date,
            'time': time,
            'position': position,
        }
        url = addeUrlFormat % formatValues
        if showUrls:
            print url
        adl = AreaDirectoryList(url)
        results = adl.getSortedDirs()
        for imageTimes in results:
            for areaDirectory in imageTimes:
                urls.append(url)
                areaDirectories.append(areaDirectory)
                
    temp = _AreaDirectoryList()
    for i, d in enumerate(areaDirectories):
        nominalTime = d.getNominalTime()
        tempDay = str(dateFormat.format(nominalTime, StringBuffer(), FieldPosition(0)))
        tempTime = str(timeFormat.format(nominalTime, StringBuffer(), FieldPosition(0)))
        
        bandList = list(d.getBands())
        # tempUnitList = list(d.getCalInfo()[0])
        # unitList = tempUnitList[::2]
        # unitDescList = tempUnitList[1::2]
        # calInfo = dict(zip(unitList, unitDescList))
        if unit:
            unitList = [origUnit]
        else:
            unitList = map(str, list(d.getCalInfo()[0])[::2])
            
        for band in bandList:
            for calUnit in unitList:
                dt = {
                    'server': server,
                    'dataset': dataset,
                    'descriptor': descriptor,
                    'bandNumber': band,
                    'bandList': bandList,
                    'debug': debug,
                    'accounting': accounting,
                    'day': tempDay,
                    'time': tempTime,
                    'imageSize': (d.getLines(), d.getElements()),
                    'centerLocation': (d.getCenterLatitude(), d.getCenterLongitude()),
                    'resolution': (d.getCenterLatitudeResolution(), d.getCenterLongitudeResolution()),
                    'unitList': unitList,
                    'unitType': calUnit,
                    'bands': bandList,
                    'band-count': d.getNumberOfBands(),
                    'calinfo': map(str, list(d.getCalInfo()[0])),
                    'calibration-scale-factor': d.getCalibrationScaleFactor(),
                    'calibration-type': str(d.getCalibrationType()),
                    'calibration-unit-name': d.getCalibrationUnitName(),
                    'center-latitude': d.getCenterLatitude(),
                    'center-latitude-resolution': d.getCenterLatitudeResolution(),
                    'center-longitude': d.getCenterLongitude(),
                    'center-longitude-resolution': d.getCenterLongitudeResolution(),
                    'directory-block': list(d.getDirectoryBlock()),
                    'elements': d.getElements(),
                    'lines': d.getLines(),
                    'memo-field': str(d.getMemoField()),
                    'nominal-time': DateTime(d.getNominalTime()),
                    'sensor-id': d.getSensorID(),
                    'sensor-type': str(d.getSensorType()),
                    'source-type': str(d.getSourceType()),
                    'start-time': DateTime(d.getStartTime()),
                    'url': urls[i],
                }
            temp.append(dt)
    return temp
    
def oldADDEImage(localEntry=None, server=None, dataset=None, descriptor=None,
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
    track=False,
    band=None,
    size=DEFAULT_SIZE):
    """Requests data from an ADDE Image server - returns both data and metadata objects.

    An ADDE request must include values for either localEntry or the combination of server, dataset and descriptor.

    Required Args:
        localEntry: Local data set defined by makeLocalADDEEntry. 
        server: ADDE server.
        dataset: ADDE dataset group name.
        descriptor: ADDE dataset descriptor.
        
        
    Optional Args:
        day: Day range ('begin date','end date')
        time: ('begin time', 'end time')
        coordinateSystem: coordinate system to use for retrieving data
                            AREA       AREA file coordinates - zero based
                            LATLON   latitude and longitude coordinates
                            IMAGE     image coordinates - one based
        location: (x,y)
                            x           AREA line, latitude, or IMAGE line
                            y           AREA element, longitude, or IMAGE element
        place: CENTER places specified location (x,y) at center of panel
                            ULEFT places specified location (x,y) at upper-left coordinate of panel
        band: McIDAS band number; must be specified if requesting data from
              multi-banded image; default=band in image
        unit: calibration unit to request; default = 'BRIT'
        position: time relative (negative values) or absolute (positive values)
                  position in the dataset; default=0 (most recent image)
        size: number of lines and elements to request; default=(480,640)
        mag: magnification of data (line,element), negative number used for
             sampling data; default=(1,1)
        accounting: ('user', 'project number') user and project number required
                    by servers using McIDAS accounting; default = ('idv','0')
        debug: send debug information to file; default=False
        track: default=False
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
        
    addeUrlFormat = "adde://%s/imagedata?&PORT=112&COMPRESS=gzip&USER=%s&PROJ=%s&VERSION=1&DEBUG=%s&TRACE=0&GROUP=%s&DESCRIPTOR=%s%s%s&PLACE=%s&SIZE=%s&UNIT=%s&MAG=%s&SPAC=4&NAV=X&AUX=YES&DOC=X%s&TIME=%s&POS=%s&TRACK=%d"
    url = addeUrlFormat % (server, user, proj, debug, dataset, descriptor, band, location, place, size, unit, mag, day, time, position, track)
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
    
def getADDEImage(localEntry=None,
    server=None, dataset=None, descriptor=None,
    accounting=DEFAULT_ACCOUNTING,
    location=None,
    coordinateSystem=None,
    place=None,
    mag=(1, 1),
    position=0,
    unit='BRIT',
    day=None,
    time=None,
    debug=False,
    track=False,
    band=None,
    size=DEFAULT_SIZE,
    showUrls=True,
    **kwargs):
    """Requests data from an ADDE Image server - returns both data and metadata objects.
    
    An ADDE request must include values for either localEntry or the combination of server, dataset and descriptor.
    
    Required Args:
        localEntry: Local data set defined by makeLocalADDEEntry.
        server: ADDE server.
        dataset: ADDE dataset group name.
        descriptor: ADDE dataset descriptor.
        
        
    Optional Args:
        day: Day range ('begin date','end date')
        time: ('begin time', 'end time')
        coordinateSystem: coordinate system to use for retrieving data
                            AREA       AREA file coordinates - zero based
                            LATLON   latitude and longitude coordinates
                            IMAGE     image coordinates - one based
        location: (x,y)
                            x           AREA line, latitude, or IMAGE line
                            y           AREA element, longitude, or IMAGE element
        place: CENTER places specified location (x,y) at center of panel
                            ULEFT places specified location (x,y) at upper-left coordinate of panel
        band: McIDAS band number; must be specified if requesting data from
              multi-banded image; default=band in image
        unit: calibration unit to request; default = 'BRIT'
        position: time relative (negative values) or absolute (positive values)
                  position in the dataset; default=0 (most recent image)
        size: number of lines and elements to request; default=(480,640)
        mag: magnification of data (line,element), negative number used for
             sampling data; default=(1,1)
        accounting: ('user', 'project number') user and project number required
                    by servers using McIDAS accounting; default = ('idv','0')
        debug: send debug information to file; default=False
        track: default=False.
    """
    
    if localEntry:
        server = localEntry.getAddress()
        dataset = localEntry.getGroup()
        descriptor = localEntry.getDescriptor().upper()
    elif (server is None) or (dataset is None) or (descriptor is None):
        raise TypeError("must provide localEntry or server, dataset, and descriptor values.")
        
    if server == "localhost" or server == "127.0.0.1":
        port = EntryStore.getLocalPort()
    else:
        port = "112"
        
    # server = '%s:%s' % (server, port)
    
    # still need to handle dates+times
    # todo: don't break!
    user = accounting[0]
    proj = accounting[1]
    debug = str(debug).lower()
    if mag and hasattr(mag, '__getitem__'):
        mag = '%s %s' % (mag[0], mag[1])
    else:
        raise ValueError("Mag keyword must be a tuple or list of at least two elements.")
        
    hasCoordSys = coordinateSystem or False
    hasPlace = place or False
    hasLocation = location or False
    
    # if hasCoordSys and not hasLocation:
    #     raise ValueError('Please provide location when specifying coordinate system.')
    # elif not hasCoordSys and hasLocation:
    #     raise ValueError('Please provide coordinate system when specifying location.')
    
    if not _checkADDEParameters(hasCoordSys, hasPlace, hasLocation, size):
        raise ValueError("Cannot specify coordinate system, place, or location while also providing a size of '%s'." % (size))
        
    if place is Places.CENTER:
        place = '&PLACE=CENTER'
    elif place is Places.ULEFT:
        place = '&PLACE=ULEFT'
    else:
        # raise ValueError("Invalid place value.")
        place = ''
        
    if coordinateSystem is CoordinateSystems.LATLON:
        coordSys = 'LATLON'
        coordType = 'E'
    elif coordinateSystem is CoordinateSystems.AREA:
        coordSys = 'LINELE'
        coordType = 'A'
    elif coordinateSystem is CoordinateSystems.IMAGE:
        coordSys = 'LINELE'
        coordType = 'I'
    # else:
        # raise ValueError("Invalid coordinateSystem value.")
        
    if location and isinstance(location, tuple) and len(location) == 2:
        location = '&%s=%s %s %s' % (coordSys, location[0], location[1], coordType)
    else:
        location = ''
        
    if day:
        if isinstance(day, tuple):
            day = '&DAY=%s %s' % (day[0], day[1])
        else:
            day = '&DAY=%s %s' % (day, day)
    else:
        day = ''
        
    if size:
        if size == 'ALL':
            size = '&SIZE=99999 99999'
        else:
            size = '&SIZE=%s %s' % (size[0], size[1])
    else:
        size = ''
        
    if time:
        if isinstance(time, (str, unicode, String)):
            time = '&TIME=%s %s I' % (str(time), str(time))
        elif len(time) == 2:
            time = '&TIME=%s %s I' % (str(time[0]), str(time[1]))
        else:
            raise ValueError("could not understand the given time value: %s" % (time))
    else:
        time = ''
        
    if band:
        try:
            band = int(band)
            band = '&BAND=%s' % (str(band))
        except:
            raise ValueError("band must be a single integer value; could not convert '%s' to an integer." % (band))
    else:
        band = ''
        
    addeUrlFormat = "adde://%(server)s/imagedata?&PORT=%(port)s&COMPRESS=gzip&USER=%(user)s&PROJ=%(proj)s&VERSION=1&DEBUG=%(debug)s&TRACE=0&GROUP=%(dataset)s&DESCRIPTOR=%(descriptor)s%(band)s%(location)s%(place)s%(size)s&UNIT=%(unit)s&MAG=%(mag)s&SPAC=4&NAV=X&AUX=YES&DOC=X%(day)s%(time)s&POS=%(position)s&TRACKING=%(track)d"
    formatValues = {
        'server': server,
        'port': port,
        'user': user,
        'proj': proj,
        'debug': debug,
        'dataset': dataset,
        'descriptor': descriptor,
        'band': band,
        'location': location,
        'place': place,
        'size': size,
        'unit': unit,
        'mag': mag,
        'day': day,
        'time': time,
        'position': position,
        'track': track,
    }
    url = addeUrlFormat % formatValues
    if showUrls:
        print url
    
    # build an object that returns the SATBAND file.
    satBandRequest = _SatBandReq(_satBandUrl(**formatValues))
    
    # submit the request
    futureSatband = ecs.submit(satBandRequest)
    
    try:
        mapped = _MappedAreaImageFlatField.fromUrl(url)
        # the commented code will immediately attempt to grab the SATBAND file;
        # the uncommented code *should* be lazy-loaded.
        # mapped.addeSatBands = futureSatband.get()
        mapped.addeSatBands = futureSatband
        return mapped.getDictionary(), mapped
    except AreaFileException, e:
        raise AddeJythonError(e)
    except AddeURLException, e:
        raise AddeJythonError(e)
    except AddeException, e:
        if e.hasAddeErrorCode():
            if e.getAddeErrorCode() == -5000:
                raise AddeJythonUnknownDataError(e)
            elif e.getAddeErrorCode() == -11011:
                raise AddeJythonBandRequiredError(e)
            elif e.getAddeErrorCode() == -11007:
                raise AddeJythonInvalidUnitError(e)
            elif e.getAddeErrorCode() == -11010:
                raise AddeJythonInvalidPortionError(e)
            elif e.getAddeErrorCode() == -11001:
                raise AddeJythonNavigationError(e)
            elif e.getAddeErrorCode() == -11003:
                raise AddeJythonBandNotPresent(e)
            elif e.getAddeErrorCode() == -7000:
                raise AddeJythonBandNotPresentInSpecifiedUnits(e)
            elif e.getAddeErrorCode() == -6000:
                if accounting == DEFAULT_ACCOUNTING:
                    raise AddeJythonAccountingRequiredError(e)
                else:
                    raise AddeJythonInvalidAccountingError(e)
        raise AddeJythonError(e)
