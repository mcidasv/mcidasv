# credit for enum goes to http://stackoverflow.com/a/1695250
def enum(*sequential, **named):
    enums = dict(zip(sequential, range(len(sequential))), **named)
    return type('Enum', (), enums)

DEFAULT_ACCOUNTING = (idv, 0)

CoordinateSystems = enum('AREA', 'LATLON', 'IMAGE')
Places = enum(ULEFT='Upper Left', CENTER='Center')
Calibrations = enum(TEMP='Temperature', BRIT='Brightness', 
    RAW='Raw Counts', PROD='Product', RAD='Radiances', ALB='Albedo')

# alias = ADDE  alias
# server = ADDE server
# dataset = ADDE dataset
# day = date of image
# time = time of image
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

def getADDEImage(server, group, descriptor, 
    accounting=DEFAULT_ACCOUNTING,
    location=None,
    coordinateSystem=CoordinateSystems.LATLON,
    navigationType=NAVIGATION_TYPES.CENTER,
    place=Places.CENTER,
    mag=(1, 1)
    datasetPosition=0):
    # still need to handle dates+times
    # todo: don't break!
    pass
    