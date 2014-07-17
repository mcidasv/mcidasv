import os
import types

import islformatters

import java.awt.Color.CYAN
import java.awt.Dimension

import ucar.unidata.util.Range

from contextlib import contextmanager

from decorators import deprecated
from decorators import gui_invoke_later
from decorators import gui_invoke_now
from interactive import expandpath

from org.slf4j import Logger
from org.slf4j import LoggerFactory

from java.awt import Rectangle

from java.lang import NullPointerException
from java.lang import StringBuffer
from java.lang import System

from java.text import FieldPosition
from java.text import SimpleDateFormat

from java.util import TimeZone
from java.util.concurrent import FutureTask

from edu.wisc.ssec.mcidasv.McIDASV import getStaticMcv
from ucar.unidata.idv import DisplayInfo
from ucar.unidata.idv.ui import IdvWindow
from ucar.unidata.idv.control import ValuePlanViewControl
from ucar.unidata.idv.control.drawing import TextGlyph
from ucar.unidata.geoloc import LatLonPointImpl
from ucar.unidata.ui.colortable import ColorTableDefaults
from ucar.unidata.util import GuiUtils
from ucar.visad import Util
from ucar.visad.data import GeoGridFlatField
from visad import FieldImpl

# from collections import namedtuple

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
from edu.wisc.ssec.mcidasv.util import ErrorCodeAreaAdapter

def pause():
    getStaticMcv().waitUntilDisplaysAreDone()

@contextmanager
def managedDataSource(path, cleanup=True, dataType=None):
    """Loads a data source and performs automatic resource cleanup.

    Attempts to create and load an IDV DataSource object using a given file.
    This function works as a part of a Python "with statement". By default
    this function will attempt to "guess" the IDV data source type of the given
    file and call the "boomstick" (TODO: better name) resource cleanup function
    if any errors are encountered.

    Args:
        path: Required string value that must be a valid file path or URL.

        cleanup: Option boolean value that allows control over whether or not
        automatic resource cleanup is performed. Default value is True.

        dataType: Optional string value that must be a valid IDV
        "data source type" ID and should correspond to the file type of the "path"
        argument. Default value is None.

    Returns:
        If McIDAS-V was able to load the file, a "ucar.unidata.data.DataSource" is
        returned. Otherwise None is returned.
    """
    # setup step
    # the problem here is that makeDataSource returns a boolean
    # how do i grab the ref to the actual datasource that got
    # created?
    dataSource = getStaticMcv().makeOneDataSource(path, dataType, None)
    # TODO(jon): perhaps write another generator that takes a varname?
    #actualData = getData(dataSource.getName(), variableName)
    try:
        # hand control back to the code "inside" the "with" statement
        yield dataSource
    except:
        # hmm...
        raise
    finally:
        # the "with" block has relinquished control; time to clean up!
        if cleanup:
            boomstick()
            
class _MappedData(object):
    """ 'Abstract' class for combined VisAD Data / Python dictionary objects
    
    Subclasses should override the _getDirValue method.
    """
    def __init__(self, keys):
        self._keys = keys

    def initMetadataMap(self):
        if not isinstance(self, FieldImpl):
            return
        hm = self.getMetadataMap()
        for key in self._keys:
            value = self[key]
            # ConcurrentHashMap doesn't allow null values
            if value:
                hm.put(key, value)
        for key in self.getMacrosDict().keys():
            hm.put(key, self.getMacrosDict()[key])
        hm.put('defaultlayerlabel', self.getDefaultLayerLabel())
        
    def _getDirValue(self, key):
        # subclasses should override!
        raise NotImplementedError()
        
    def getDictionary(self):
        return dict(self.iteritems())
        
    def __repr__(self):
        return repr(dict(self.iteritems()))
        
    def __len__(self):
        return len(self._keys)
        
    def __getitem__(self, key):
        try:
            return self._getDirValue(key)
        except KeyError:
            raise KeyError()
            
    def __iter__(self):
        for x in self._keys:
            yield x
            
    def __contains__(self, item):
        for value in self.itervalues():
            if item == value:
                return True
        return False
        
    def keys(self):
        return list(self._keys)
        
    def items(self):
        mappedItems = []
        for key in self._keys:
            mappedItems.append((key, self._getDirValue(key)))
        return mappedItems
        
    def iteritems(self):
        for key in self._keys:
            yield (key, self._getDirValue(key))
            
    def iterkeys(self):
        return iter(self._keys)
        
    def itervalues(self):
        for key in self._keys:
            yield self._getDirValue(key)
            
    def values(self):
        return [self._getDirValue(key) for key in self._keys]
        
    def has_key(self, key):
        return key in self._keys
        
    def get(self, key, default=None):
        try:
            return self._getDirValue(key)
        except KeyError:
            return default

    def getMacrosDict(self):
        """return a dictionary mapping IDV macro strings to reasonable defaults
        for this object
        """
        # subclasses should override!
        raise NotImplementedError()

    def getDefaultLayerLabel(self):
        """return a reasonable default layer label for this class
        """
        # subclasses should override!
        raise NotImplementedError()

    def clone(self):
        # i'm so sorry :(
        return self * 1
            
    def __reversed__(self):
        raise NotImplementedError()
        
    def __setitem__(self, key, value):
        raise NotImplementedError()
        
    def __delitem__(self, key):
        raise NotImplementedError()
        
    def setdefault(self, key, failobj=None):
        raise NotImplementedError()
        
    def pop(self, key, *args):
        raise NotImplementedError()
        
    def popitem(self):
        raise NotImplementedError()
        
    def update(self, newDict=None, **kwargs):
        raise NotImplementedError()
        
class _MappedAreaImageFlatField(_MappedData, AreaImageFlatField):
    def __init__(self, aiff, areaFile, areaDirectory, addeDescriptor, startTime):
        """
        Make a _MappedAreaImageFlatField from an existing AreaImageFlatField
        """
        # self.__mappedObject = AreaImageFlatField.createImmediate(areaDirectory, imageUrl)
        keys = [ 'band-count', 'bandList', 'bandNumber', 'bands',
                 'calibration-scale-factor', 'calibration-type',
                 'calibration-unit-name', 'calinfo', 'center-latitude',
                 'center-latitude-resolution', 'center-longitude',
                 'center-longitude-resolution', 'day', 'directory-block',
                 'elements', 'lines', 'memo-field', 'nominal-time',
                 'sensor-id', 'sensor-type', 'source-type', 'start-time',
                 'url','satband-band-label', ]
                 
        _MappedData.__init__(self, keys)
        self.areaFile = areaFile
        self.areaDirectory = areaDirectory
        self.addeDescriptor = addeDescriptor
        self.addeSatBands = None
        # call the copy constructor
        AreaImageFlatField.__init__(self, aiff, False, aiff.getType(),
            aiff.getDomainSet(), aiff.RangeCoordinateSystem,
            aiff.RangeCoordinateSystems, aiff.RangeSet,
            aiff.RangeUnits, aiff.readLabel)
        self.startTime = startTime
        self.initMetadataMap()
        
    # http://stackoverflow.com/questions/141545/overloading-init-in-python
    @classmethod
    def fromUrl(cls, imageUrl):
        """
        Create an AreaImageFlatField from a URL, then make a
        _MappedAreaImageFlatField
        """
        aa = ErrorCodeAreaAdapter.createAreaAdapter(imageUrl)
        areaFile = aa.getAreaFile()
        areaDirectory = aa.getAreaDirectory()
        addeDescriptor = AddeImageDescriptor(areaDirectory, imageUrl)
        ff = aa.getImage()
        samples = ff.unpackFloats()
        ftype = ff.getType()
        domainSet = ff.getDomainSet()
        rangeCoordSys = ff.getRangeCoordinateSystem()[0]
        rangeSets = ff.getRangeSets()
        units = ff.getRangeUnits()[0]
        aiff = AreaImageFlatField(addeDescriptor, ftype, domainSet,
                rangeCoordSys, rangeSets, units, samples, "READLABEL")
        areaFile.close()
        return cls(aiff, areaFile, areaDirectory, addeDescriptor,
                ff.getStartTime())
                
    def test(self):
        return self.aid
        
    def _getCalInfo(self):
        calInfo = self.areaDirectory.getCalInfo()
        if calInfo:
            return map(str, list(calInfo[0]))
        else:
            return []
            
    def _getDay(self):
        nominal = self.areaDirectory.getNominalTime()
        dateFmt = SimpleDateFormat()
        dateFmt.setTimeZone(TimeZone.getTimeZone('Z'))
        dateFmt.applyPattern('yyyyDDD')
        return str(dateFmt.format(nominal, StringBuffer(), FieldPosition(0)))
        
    def _getBand(self):
        bands = self._getDirValue('bands')
        if len(bands) == 1:
            return bands[0]
        else:
            return bands
            
    def _handleSatBand(self):
        # grab result if we haven't already done so
        if isinstance(self.addeSatBands, FutureTask):
            self.addeSatBands = self.addeSatBands.get()
            
        if self.addeSatBands:
            bandDescr = self.addeSatBands.getBandDescr(
                self.areaDirectory.getSensorID(),
                self.areaDirectory.getSourceType())
            if bandDescr:
                return bandDescr[self._getBand()]

        return ''
            
    def _getDirValue(self, key):
        from visad import DateTime
        
        if key not in self._keys:
            raise KeyError('unknown key: %s' % key)
        if key == 'bands' or key == 'bandList':
            return list(self.areaDirectory.getBands())
        elif key == 'bandNumber':
            return self._getBand()
        elif key == 'day':
            return self._getDay()
        elif key == 'calinfo':
            return self._getCalInfo()
        elif key == 'calibration-scale-factor':
            return self.areaDirectory.getCalibrationScaleFactor()
        elif key == 'calibration-type':
            return str(self.areaDirectory.getCalibrationType())
        elif key == 'calibration-unit-name':
            return self.areaDirectory.getCalibrationUnitName()
        elif key == 'center-latitude':
            return self.areaDirectory.getCenterLatitude()
        elif key == 'center-latitude-resolution':
            return self.areaDirectory.getCenterLatitudeResolution()
        elif key == 'center-longitude':
            return self.areaDirectory.getCenterLongitude()
        elif key == 'center-longitude-resolution':
            return self.areaDirectory.getCenterLongitudeResolution()
        elif key == 'directory-block':
            return list(self.areaDirectory.getDirectoryBlock())
        elif key == 'elements':
            return self.areaDirectory.getElements()
        elif key == 'lines':
            return self.areaDirectory.getLines()
        elif key == 'memo-field':
            return str(self.areaDirectory.getMemoField())
        elif key == 'nominal-time':
            return DateTime(self.areaDirectory.getNominalTime())
        elif key == 'band-count':
            return self.areaDirectory.getNumberOfBands()
        elif key == 'sensor-id':
            return self.areaDirectory.getSensorID()
        elif key == 'sensor-type':
            return str(self.areaDirectory.getSensorType())
        elif key == 'source-type':
            return str(self.areaDirectory.getSourceType())
        elif key == 'start-time':
            return DateTime(self.areaDirectory.getStartTime())
        elif key == 'url':
            return str(self.aid.getSource())
        elif key == 'satband-band-label':
            return self._handleSatBand()
        else:
            raise KeyError('should not be capable of reaching here: %s')

    def getMacrosDict(self):
        """return a dictionary mapping IDV macro strings to reasonable defaults
        for this object
        """
        #longname = '%s band %s %s' % (self['sensor-type'], self['bands'][0], self['calibration-type'])
        # use SATBAND string now that we have it:
        longname = '%s %s' % (self['sensor-type'], self['satband-band-label'])
        shortname = '%s_Band%s_%s' % (self['sensor-id'], self['bands'][0],  self['calibration-type'])
        macros = {'longname': longname, 'shortname': shortname}
        return macros

    def getDefaultLayerLabel(self):
        """return a reasonable default layer label for this class
        """
        # note the double percent sign- we are 'escaping' the percent signs
        #defaultLabel = '%s band %s %s %%timestamp%%' % (self['sensor-type'], self['bands'][0], self['calibration-type'])
        # use %longname% now that it is getting set w/ SATBAND string:
        defaultLabel = '%longname% %timestamp%'
        return defaultLabel

class _MappedGeoGridFlatField(_MappedData, GeoGridFlatField):
    """ implements the 'mega-object' class for grids read with loadFile
    """
    def __init__(self, ggff, geogrid, filename, field):
        self.geogrid = geogrid
        self.filename = filename
        self.field = field
        self.levelReal = None
        keys = ['attributes', 'datatype', 'description', 'info',
                'levels', 'units', 'times', 'projection', 'field', 'filename']
        _MappedData.__init__(self, keys)
        # call the "copy constructor", not copying values
        GeoGridFlatField.__init__(self, ggff, False, ggff.getType(), 
                ggff.getDomainSet(), ggff.RangeCoordinateSystem, 
                ggff.RangeCoordinateSystems, ggff.RangeSet,
                ggff.RangeUnits)
        self.initMetadataMap()
    
    def _getDirValue(self, key):
        if key not in self._keys:
            raise KeyError('unknown key: %s' % key)
        if key == 'attributes':
            return self.geogrid.getAttributes()
        if key == 'datatype':
            return self.geogrid.getDataType()
        if key == 'description':
            return self.geogrid.getDescription()
        if key == 'info':
            return self.geogrid.getInfo()
        if key == 'levels':
            return self.geogrid.getLevels()
        if key == 'units':
            return self.geogrid.getUnitsString()
        if key == 'times':
            return self.geogrid.getTimes()
        if key == 'projection':
            return self.geogrid.getProjection()
        if key == 'field':
            return self.field
        if key == 'filename':
            return self.filename
        else:
            raise KeyError('should not be capable of reaching here: %s')

    def getMacrosDict(self):
        """return a dictionary mapping IDV macro strings to reasonable defaults
        for this object
        """
        from os.path import basename
        longname = self['description']
        shortname = self['field']
        #TODO: figure out how to actually set this macro (in createLayer)
        datasourcename = basename(self['filename'])
        macros = {'longname': longname, 'shortname': shortname,
                  'datasourcename': datasourcename 
                  }
        return macros

    def getDefaultLayerLabel(self):
        """return a reasonable default layer label for this class
        """
        defaultLabel = '%shortname% %level% - %timestamp%'
        return defaultLabel


class _JavaProxy(object):
    """One sentence description goes here
    
    This is where a more complete description of the class would go.
    
    Attributes:
        attr_one: Blurb about attr_one goes here.
        foo: Blurb about foo.
    """
    def __init__(self, javaObject):
        """Stores a given java instance and flags the proxy as being initialized."""
        
        self.__javaObject = javaObject
        self.__initialized = True
        
    def getJavaInstance(self):
        """Returns the actual VisAD/IDV/McIDAS-V object being proxied."""
        
        return self.__javaObject
        
    def __str__(self):
        """Returns the results of running the proxied object's toString() method."""
        
        return self.__javaObject.toString()
        
    def __getattr__(self, attr):
        """Forwards object attribute lookups to the internal VisAD/IDV/McIDAS-V object."""
        
        # if not self.__dict__.has_key('_JavaProxy__initialized'):
        if not '_JavaProxy__initialized' in self.__dict__:
            raise AttributeError(attr)
        else:
            if hasattr(self.__javaObject, attr):
                return getattr(self.__javaObject, attr)
            elif hasattr(self, attr):
                # return getattr(self, attr)
                return self.__dict__[attr]
            else:
                raise AttributeError(attr)
                
    def __setattr__(self, attr, val):
        """Forwards object attribute changes to the internal VisAD/IDV/McIDAS-V object."""
        
        if not '_JavaProxy__initialized' in self.__dict__:
            self.__dict__[attr] = val
            return
            
        if hasattr(self.__javaObject, attr):
            setattr(self.__javaObject, attr, val)
        else:
            self.__dict__[attr] = val
            
@gui_invoke_later
def _getNewFont(currentFont, fontName, style, size):
    """Helper function for setLayerLabelFont and setColorScaleFont
       since they need to accomplish the same task
       (see those functions for more details)
       
    Args:
        currentFont: an existing font to use for "default" font properties
        fontName: new fontName
        style: new style
        size: new size

    Raises:
        ValueError: if fontName doesn't exist
    """
    if isinstance(style, str):
        # we need all caps
        style = style.upper()
        
    if style == "BOLD":
        style = java.awt.Font.BOLD
    elif style == "ITALIC":
        style = java.awt.Font.ITALIC
    elif style == "NONE":
        style = java.awt.Font.PLAIN
    else:
        style = currentFont.getStyle()
        
    if size is None:
        size = currentFont.getSize()
    else:
        size = int(size)
        
    if fontName is not None:
        # check if fontName is valid
        fontList = list(ucar.unidata.util.GuiUtils.getFontList())
        # Add Java Platform required fonts to this list. This avoids issues 
        # where getFontList() returns e.g. Serif.plain instead of just Serif.
        fontList.extend(
                ['Serif', 'SansSerif', 'Monospaced', 'Dialog', 'DialogInput'])
        foundFont = False
        for availableFont in fontList:
            # Note, Font constructor will just use some default font if passed
            # a non-existent font name.  So need to check for existence of
            # user-specified font rather than catch exception.
            if str(availableFont).lower() == fontName.lower():
                fontName = str(availableFont)
                foundFont = True
        if not foundFont:
            # if fontName is STILL None, then user provided an invalid font name
            raise ValueError(
                    "Could not find the following fontName:", fontName, 
                    "call allFontNames for valid options")
    else:
        # leave as-is if fontName is None
        fontName = currentFont.getFontName()
        
    return java.awt.Font(fontName, style, size)
    
class _Window(_JavaProxy):
    def __init__(self, javaObject):
        """Blank for now. javaObject = IdvWindow
           tab
        """
        
        _JavaProxy.__init__(self, javaObject)
        
    @gui_invoke_later
    def createTab(self, skinId='idv.skin.oneview.map'):
        from ucar.unidata.idv import IdvResourceManager
        from edu.wisc.ssec.mcidasv.util.McVGuiUtils import idvGroupsToMcv
        skins = getStaticMcv().getResourceManager().getXmlResources(IdvResourceManager.RSC_SKIN)
        
        skinToIdx = {}
        for x in range(skins.size()):
            skinToIdx[skins.getProperty('skinid', x)] = x
            
        if not skinId in skinToIdx:
            raise LookupError()
        else:
            window = self._JavaProxy__javaObject
            group = idvGroupsToMcv(window)
            holder = group[0].makeSkinAtIndex(skinToIdx[skinId])
            return _Tab(holder)
            
    #def setCurrentTabIndex(self, index):
    #    """Sets the tab at the given index to be the active tab."""
    #    # TODO(jon): remove this method?
    #    self._JavaProxy__javaObject.getComponentGroups()[0].setActiveIndex(index)
    #
    @gui_invoke_later
    def getCurrentTab(self):
        """Returns the currently active tab."""
        
        # mcv windows should only have one component group
        return _Tab(self._JavaProxy__javaObject.getComponentGroups()[0].getActiveComponentHolder())
        
    @gui_invoke_later
    def getTabAtIndex(self, index):
        """Returns the tab at the given index."""
        
        return _Tab(self._JavaProxy__javaObject.getComponentGroups()[0].getHolderAt(index))
        
    @gui_invoke_later
    def getTabCount(self):
        """Returns the number of tabs."""
        
        return self._JavaProxy__javaObject.getComponentGroups()[0].getDisplayComponentCount()
        
    @gui_invoke_later
    def getTabs(self):
        """Returns a list of the available tabs."""
        
        return [_Tab(holder) for holder in self._JavaProxy__javaObject.getComponentGroups()[0].getDisplayComponents()]
        
    @gui_invoke_later
    def getSize(self):
        """Returns the width and height of the wrapped IdvWindow."""
        
        dims = self._JavaProxy__javaObject.getSize()
        return dims.getWidth(), dims.getHeight()
        
    @gui_invoke_later
    def getBounds(self):
        """Returns the xy-coords of the upper left corner, as well as the width
        and height of the wrapped IdvWindow.
        """
        rect = self._JavaProxy__javaObject.getBounds()
        return rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight()


class _Tab(_JavaProxy):
    def __init__(self, javaObject):
        """Blank for now. javaObject = McvComponentHolder
        """
        _JavaProxy.__init__(self, javaObject)
        
    @gui_invoke_later
    def getName(self):
        """Returns the name of this tab."""
        return self._JavaProxy__javaObject.getName()
        
    @gui_invoke_later
    def setName(self, newTabName):
        """Set this tab's name to a given string value."""
        self._JavaProxy__javaObject.setName(newTabName)
        
    @gui_invoke_later
    def getDisplays(self):
        """Returns a list of the displays contained within this tab."""
        return [_Display(viewManager) for viewManager in self._JavaProxy__javaObject.getViewManagers()]

class _Display(_JavaProxy):
    
    # this allows a _Layer to find it's associated _Display
    displayWrappers = []
    
    def __init__(self, javaObject, labelDict=None):
        """Blank for now. javaObject = ViewManager
           displayType
           width
           height
           panel ?
           dataSource
           wireBox(boolean)
           colortable(string)
           colorBar(boolean)
           projection(string)
           minValue ?
           maxValue ?
           minVerticalScale
           maxVerticalScale
           map(list)
           x-rotate
           y-rotate
           z-rotate
        """
        _JavaProxy.__init__(self, javaObject)
        if labelDict is None:
            # DisplayList / layer label properties
            self.labelDict = dict(
                font=javaObject.getDisplayListFont(),
                color=javaObject.getDisplayListColor(),
                visible=javaObject.getShowDisplayList(),
            )
        _Display.displayWrappers.append(self)
        
    def _getDisplayIndex(self):
        mcv = getStaticMcv()
        index = -1
        if mcv:
            currentDescriptor = self._JavaProxy__javaObject.getViewDescriptor()
            vmManager = mcv.getVMManager()
            for i, vm in enumerate(vmManager.getViewManagers()):
                if currentDescriptor == vm.getViewDescriptor():
                    index = i
                    break
                    
        return index
        
    @gui_invoke_later
    def getDisplayType(self):
        # TODO(jon): how to refer to 2d map displays?
        # MapViewManager, IdvUIManager.COMP_MAPVIEW
        # MapViewManager.getUseGlobeDisplay(), IdvUIManager.COMP_GLOBEVIEW
        # TransectViewManager, IdvUIManager.COMP_TRANSECTVIEW
        from ucar.unidata.idv.ui import IdvUIManager
        
        className = self._JavaProxy__javaObject.getClass().getCanonicalName()
        if className == 'ucar.unidata.idv.MapViewManager':
            if self._JavaProxy__javaObject.getUseGlobeDisplay():
                return IdvUIManager.COMP_GLOBEVIEW
            else:
                return IdvUIManager.COMP_MAPVIEW
        elif className == 'ucar.unidata.idv.TransectViewManager':
            return IdvUIManager.COMP_TRANSECTVIEW
        else:
            return IdvUIManager.COMP_VIEW
            
    @gui_invoke_later
    def toggleFullScreen(self):
        self._JavaProxy__javaObject.toggleFullScreen()
        
    @gui_invoke_later
    def getFullScreenSize(self):
        width = self._JavaProxy__javaObject.getFullScreenWidth()
        height = self._JavaProxy__javaObject.getFullScreenHeight()
        return width, height
        
    @gui_invoke_later
    def setFullScreenSize(self, width, height):
        self._JavaProxy__javaObject.setFullScreenWidth(width)
        self._JavaProxy__javaObject.setFullScreenHeight(height)
        
    @gui_invoke_later
    def getSize(self):
        size = self._JavaProxy__javaObject.getComponent().getSize()
        return size.getWidth(), size.getHeight()
        
    @gui_invoke_later
    def setSize(self, width, height):
        if getStaticMcv().getArgsManager().getIsOffScreen():
            self.setSizeBackground(width, height)
            return
        size = java.awt.Dimension(width, height)
        #me = self._JavaProxy__javaObject
        navigatedComponent = self._JavaProxy__javaObject.getComponent()
        navigatedComponent.setMinimumSize(size)
        navigatedComponent.setMaximumSize(size)
        navigatedComponent.setPreferredSize(size)
        window = GuiUtils.getWindow(navigatedComponent)
        #me.getMaster().getDisplay().getComponent().setPreferredSize(size)
        #me.getMaster().getDisplay().getComponent().setMaximumSize(size)
        #me.getMaster().getDisplay().getComponent().setMinimumSize(size)
        #me.getMaster().getDisplay().getDisplayRenderer().getCanvas().setPreferredSize(size)
        #me.getMaster().getDisplay().getDisplayRenderer().getCanvas().setMaximumSize(size)
        #me.getMaster().getDisplay().getDisplayRenderer().getCanvas().setMinimumSize(size)
        if not window:
            from javax.swing import JFrame
            window = JFrame()
            window.getContentPane().add(navigatedComponent)
            
        window.pack()
        # print 'new: %s\ncur: %s\nmin: %s\nmax: %s\nprf: %s' % (size, navigatedComponent.getSize(), navigatedComponent.getMinimumSize(), navigatedComponent.getMaximumSize(), navigatedComponent.getPreferredSize())
        
    @gui_invoke_later
    def setSizeBackground(self, width, height):
        curWindowObj = self._JavaProxy__javaObject
        # get some properties of the current window
        # get the current, um, viewpoint?
        displayMatrix = curWindowObj.getDisplayMatrix()
        # get the current projection
        projection = curWindowObj.getMapDisplay().getMapProjection()
        # other stuff.. wireframe, DisplayList properties... more?
        wireframe = curWindowObj.getWireframe()
        
        newWindow = buildWindow(width, height)[0]
        newWindowObj = newWindow._JavaProxy__javaObject
        
        # this is somewhat akin to dragging layers in the GUI
        layers = self.getLayers()
        for layer in layers:
            layerObj = layer._JavaProxy__javaObject
            if (layerObj.toString() != 'Default Background Maps'):
                # this does just a part of what ViewManager.moveTo does
                displayList = layerObj.getDisplayInfos()
                for info in displayList:
                    info.moveTo(newWindowObj)
                # this makes sure _Layer.getViewManager returns the right thing:
                layerObj.setInitialViewManager(newWindowObj)
                # Note, the following pops up a window in background!!!
                # (ViewManager.controlMoved eventually leads to a
                #  McIDASVViewPanel.addControlTab which does component stuff):
                #layerObj.moveTo(newWindow._JavaProxy__javaObject)
                
        # set the new window's viewpoint, projection, etc.
        newWindowObj.getMapDisplay().setMapProjection(projection)
        newWindowObj.setDisplayMatrix(displayMatrix)
        newWindowObj.setWireframe(wireframe)
        
        # DisplayList/layer label stuff
        newWindowObj.setShowDisplayList(self.labelDict['visible'])
        newWindowObj.setDisplayListColor(self.labelDict['color'])
        newWindowObj.setDisplayListFont(self.labelDict['font'])
        newWindowObj.updateDisplayList()
        
        # note, can't just do 'self = newWindow' since self is local
        self._JavaProxy__javaObject = newWindow._JavaProxy__javaObject
        
    # @deprecated(self.setSize)
    @gui_invoke_later
    def setDimensions(self, x, y, width, height):
        self._JavaProxy__javaObject.setDisplayBounds(Rectangle(x, y, width, height))
        self.setSize(width, height)
        
    # @deprecated(self.getSize)
    @gui_invoke_later
    def getDimensions(self):
        rect = self._JavaProxy__javaObject.getDisplayBounds()
        return rect.x, rect.y, rect.width, rect.height
        
    def getDataAtLocation(self, lat, lon):
        #earthLocation = Util.makeEarthLocation(lat, lon)
        #for layer in self._JavaProxy__javaObject.getControls():
        pass
        
    def getDataSources(self):
        pass
        
    @gui_invoke_later
    def getProjection(self):
        """Returns the map projection currently in use."""
        return _Projection(self._JavaProxy__javaObject.getMapDisplay().getMapProjection())
        
    @gui_invoke_later
    def setProjection(self, projection):
        """ Set the current projection.
        
        Args:
            projection: can be either:
                (1) a string that specifies the desired projection in the format:
                'US>States>West>Texas'
                
                or
                
                (2) a _Layer object.  Projection will get set to the 'native'
                projection for that layer
                
        Raises:
            ValueError:  if projection isn't a valid projection name or existing layer
        """
        # TODO(mike): catch a NameError if projection isn't defined.
        # Currently able to catch AttributeError but not NameError, hmm..
        
        # if isinstance(projection, _Layer):
        #     projObj = projection._JavaProxy__javaObject.getDataProjection()
        #     self._JavaProxy__javaObject.getMapDisplay().resetMapParameters(True)
        #     return self._JavaProxy__javaObject.getMapDisplay().setMapProjection(projObj)
        
        # if isinstance(projection, str):
        #     projObj = getProjection(projection)._JavaProxy__javaObject
        #     self._JavaProxy__javaObject.getMapDisplay().resetMapParameters(True)
        #     return self._JavaProxy__javaObject.getMapDisplay().setMapProjection(projObj)
        
        if isinstance(projection, _Layer):
            projObj = projection._JavaProxy__javaObject.getDataProjection()
        elif isinstance(projection, str):
            projObj = getProjection(projection)._JavaProxy__javaObject
        else:
            # if user does something like pass in an int
            raise ValueError('valid arguments to setProjection are (1) a string defining a valid' +
                             ' projection name, or (2) a _Layer object with the desired data projection.')
            
        currentProj = self._JavaProxy__javaObject.getMapDisplay().getMapProjection()
        if projObj == currentProj:
            # print 'projections match!'
            self._JavaProxy__javaObject.getMapDisplay().resetMapParameters(True)
        else:
            #print 'projections differ: %s %s' % (projObj, currentProj)
            #print 'types: %s %s' % (type(projObj), type(currentProj))
            self._JavaProxy__javaObject.getMapDisplay().setMapProjection(projObj)
            
    @gui_invoke_later
    def resetProjection(self):
        return self._JavaProxy__javaObject.getMapDisplay().resetProjection()
        
    @gui_invoke_later
    def getVerticaleScaleUnit(self):
        return self._JavaProxy__javaObject.getMapDisplay().getVerticalRangeUnit()
        
    @gui_invoke_later
    def getVerticalScaleRange(self):
        verticalRange = self._JavaProxy__javaObject.getMapDisplay().getVerticalRange()
        return verticalRange[0], verticalRange[1]
        
    @gui_invoke_later
    def getMaps(self):
        """Returns a dictionary of maps and their status for the display."""
        
        # dict of mapName->boolean (describes if a map is enabled or not.)
        # this might fail for transect displays....
        mapLayer = self.getMapLayer()
        mapStates = {}
        for mapState in mapLayer.getMapStates():
            mapStates[mapState.getSource()] = mapState.getVisible()
        return mapStates
        
    @gui_invoke_later
    def setMaps(self, mapStates):
        """Allows for controlling the visibility of all available maps for
        the display.
        """
        
        mapLayer = self.getMapLayer()
        for currentState in mapLayer.getMapStates():
            mapSource = currentState.getSource()
            if mapSource in mapStates:
                currentState.setVisible(mapStates[mapSource])
                
    @gui_invoke_later
    def getMapVisibilityByDescription(self, description):
        """Return the visibility of the map matching the given description.
        
        Args:
            description: Corresponds to the label used by the GUI. Case does
                         not matter.
                         
        Raises:
            KeyError: if no maps matching the given description could be found.
        """
        originalDescription = description
        description = str(description).lower()
        for mapState in self.getMapLayer().getMapStates():
            mapDescription = str(mapState.getDescription()).lower()
            if description == mapDescription:
                return mapState.getVisible()
        raise KeyError("No map matching '%s'" % originalDescription)
        
    @gui_invoke_later
    def setMapVisibilityByDescription(self, description, visibility):
        """Return the visibility of the map matching the given description.
        
        Args:
            description: Corresponds to the label used by the GUI. Case does
                         not matter.
                         
            visibility: Boolean value for the new map visibility.
                        
        Raises:
            KeyError: if no maps matching the given description could be found.
        """
        originalDescription = description
        description = str(description).lower()
        for mapState in self.getMapLayer().getMapStates():
            mapDescription = str(mapState.getDescription()).lower()
            if description == mapDescription:
                mapState.setVisible(visibility)
                return
        raise KeyError("No map matching '%s'" % originalDescription)
        
    @gui_invoke_later
    def getCenter(self, includeScale=False):
        """Returns the latitude and longitude at the display's center."""
        
        position = self._JavaProxy__javaObject.getScreenCenter()
        latitude = position.getLatitude().getValue()
        longitude = position.getLongitude().getValue()
        
        # validate! (visad's EarthLocation allows for bad values!)
        llp = LatLonPointImpl(latitude, longitude)
        
        if includeScale:
            result = llp.getLatitude(), llp.getLongitude(), self.getScaleFactor()
        else:
            result = llp.getLatitude(), llp.getLongitude()
            
        return result
        
    @gui_invoke_later
    def setScaleFactor(self, scale):
        """ """
        
        self._JavaProxy__javaObject.getMapDisplay().zoom(scale)
        
    @gui_invoke_later
    def getScaleFactor(self):
        return self._JavaProxy__javaObject.getMapDisplay().getScale()
        
    @gui_invoke_later
    def center(self, lat, lon, scale=1.0):
        self.setCenter(lat, lon)
        #self.setScaleFactor(scale)
        
    @gui_invoke_later
    def setCenter(self, lat, lon, scale=1.0):
        """Centers the display over a given latitude and longitude.
        
        Please be aware that something like:
        activeDisplay().setCenter(lat, lon, 1.2)
        activeDisplay().setCenter(lat, lon, 1.2)
        the second call will rescale the display to be 1.2 times the size of
        the display *after the first call.* Or, those calls are essentially
        the same as "activeDisplay().setCenter(lat, lon, 2.4)".
        
        Note on above issue: it might be useful if this does a "resetProjection" every time,
        so that "scale" behaves more predicatbly   --mike
        
        Args:
        lat:
        lon:
        scale: Optional parameter for "zooming". Default value (1.0) results in no rescaling;
            greater than 1.0 "zooms in", less than 1.0 "zooms out"
        """
        validated = LatLonPointImpl(lat, lon)
        earthLocation = Util.makeEarthLocation(validated.getLatitude(), validated.getLongitude())
        mapDisplay = self._JavaProxy__javaObject.getMapDisplay()
        
        # no idea what the problem is here...
        mapDisplay.centerAndZoom(earthLocation, False, scale)
        # try to position correctly
        mapDisplay.centerAndZoom(earthLocation, False, 1.0)
        mapDisplay.centerAndZoom(earthLocation, False, 1.0)
        
    @gui_invoke_later
    def testCenter(self, lat, lon, scale=1.0):
        """Centers the display over a given latitude and longitude.
        
        Please be aware that something like:
        setCenter(lat, long, 1.2)
        setCenter(lat, long, 1.2)
        the second call will rescale the display to be 1.2 times the size of
        the display *after the first call.* Or, those calls are essentially
        the same as "setCenter(lat, long, 2.4)".
        
        Note on above issue: it might be useful if this does a "resetProjection" every time,
        so that "scale" behaves more predicatbly   --mike
        
        Args:
        lat:
        lon:
        scale: Optional parameter for "zooming". Default value (1.0) results in no rescaling;
            greater than 1.0 "zooms in", less than 1.0 "zooms out"
        """
        
        # source and dest are arbitrary rectangles.
        # float scaleX = dest.width / source.width;
        # float scaleY = dest.height / source.height;
        # Point sourceCenter = centerPointOfRect(source);
        # Point destCenter = centerPointOfRect(dest);
        # glTranslatef(destCenter.x, destCenter.y, 0.0);
        # glScalef(scaleX, scaleY, 0.0);
        # glTranslatef(sourceCenter.x * -1.0, sourceCenter.y * -1.0, 0.0);
        validated = LatLonPointImpl(lat, lon)
        earthLocation = Util.makeEarthLocation(validated.getLatitude(), validated.getLongitude())
        mapDisplay = self._JavaProxy__javaObject.getMapDisplay()
        
        #  accept scale keyword as argument.  Seems to be working now  --mike
        mapDisplay.centerAndZoom(earthLocation, False, scale)
        
    @gui_invoke_later
    def getBackgroundColor(self):
        """Returns the Java AWT color object of the background color (or None)."""
        
        return self._JavaProxy__javaObject.getMapDisplay().getBackground()
        
    def _translateLogoPosition(self, position, xoff, yoff):
        position = position.lower()
        translateTable = {
            'lower left': 'll', 'll': 'll',
            'upper left': 'ul', 'ul': 'ul',
            'upper right': 'ur', 'ur': 'ur',
            'lower right': 'lr', 'lr': 'lr',
            'center': 'mm', 'mm': 'mm',
        }
        return '%s,%s,%s' % (translateTable[position], xoff, yoff)
        
    @gui_invoke_later
    def setLogo(self, image, position='Lower Left', xOffset=0, yOffset=0, visibility=True, scale=1.0):
        """Set a logo for the display.
        
        If individual logo "properties" need to be manipulated after calling
        this method, there are the following _Display methods available:
            setLogoFile
            setLogoPosition
            setLogoVisibility
            setLogoScale
            
        Required Args:
            image: Path to the image to use as a logo.
            
        Optional Args:
            position: Section of the screen where the logo should be placed. 
                      Acceptable values are "Lower Left", "Upper Left", 
                      "Upper Right", "Lower Right", and "Center". Case does not
                      matter. Default value is "Lower Left".
            xOffset: Integer value for x-axis offset from position. Default 
                     value is 0.
            yOffset: Integer value for y-axis offset from position. Default 
                     value is 0.
            visibility: Boolean value for whether or not the logo is 
                        immediately visible. Default value is True.
            scale: Default value is 1.0.
        
        Raises:
            IOError: if image does not exist.
            
            ValueError: if the position was invalid.
        """
        if not os.path.exists(image):
            raise IOError("'%s' does not exist." % (image))
            
        try:
            idvPos = self._translateLogoPosition(position, xOffset, yOffset)
            self._JavaProxy__javaObject.setLogoFile(image)
            self._JavaProxy__javaObject.setLogoPosition(idvPos)
            self._JavaProxy__javaObject.setLogoVisibility(visibility)
            self._JavaProxy__javaObject.setLogoScale(scale)
        except KeyError:
            raise ValueError("Position '%s' is not valid. Please provide one of 'Lower Left', 'Upper Left', 'Upper Right', 'Lower Right', or 'Center'." % (position))
    
    @gui_invoke_later
    def setLogoFile(self, image):
        """Set the image to use as the display's logo.
        
        Required Args:
            image: Path to the image to use as a logo.
        
        Raises:
            IOError: if image does not exist.
        """
        if not os.path.exists(image):
            raise IOError("'%s' does not exist." % (image))
            
        vis = self._JavaProxy__javaObject.getLogoVisibility()
        self._JavaProxy__javaObject.setLogoFile(image)
        # display doesn't update without this step.
        self._JavaProxy__javaObject.setLogoVisibility(vis)
        
    @gui_invoke_later
    def setLogoScale(self, scale):
        """Set the scale of the display's logo.
        
        Required Args:
            scale: New logo scale.
        """
        vis = self._JavaProxy__javaObject.getLogoVisibility()
        self._JavaProxy__javaObject.setLogoScale(scale)
        # display doesn't update without this step.
        self._JavaProxy__javaObject.setLogoVisibility(vis)
        
    @gui_invoke_later
    def setLogoPosition(self, position, xOffset=0, yOffset=0):
        """Set the position of the display's logo.
        
        Required Args:
            position: Section of the screen where the logo should be placed. 
                      Acceptable values are "Lower Left", "Upper Left", 
                      "Upper Right", "Lower Right", and "Center". Case does not
                      matter.
        Optional Args:
            xOffset: Integer value for x-axis offset from position. Default 
                     value is 0.
            yOffset: Integer value for y-axis offset from position. Default 
                     value is 0.
        Raises:
            ValueError: if the position was invalid.
        """
        try:
            idvPos = self._translateLogoPosition(position, xOffset, yOffset)
            vis = self._JavaProxy__javaObject.getLogoVisibility()
            self._JavaProxy__javaObject.setLogoPosition(idvPos)
            # display doesn't update without this step.
            self._JavaProxy__javaObject.setLogoVisibility(vis)
        except KeyError:
            raise ValueError("Position '%s' is not valid. Please provide one of 'Lower Left', 'Upper Left', 'Upper Right', 'Lower Right', or 'Center'." % (position))
            
    @gui_invoke_later
    def setBackgroundColor(self, color=java.awt.Color.CYAN):
        """Sets the display's background color to the given AWT color. Defaults to cyan."""
        
        self._JavaProxy__javaObject.getMapDisplay().setBackground(color)
        
    @gui_invoke_later
    def getMapLayer(self):
        """Returns the map layer for this display, or None if no map layer could be found."""
        # TODO(jon): can there be multiple MapDisplayControls per ViewManager?
        from ucar.unidata.idv.control import MapDisplayControl
        controls = self._JavaProxy__javaObject.getControls()
        mapLayer = None
        for control in controls:
            if isinstance(control, MapDisplayControl):
                mapLayer = _Layer(control)
                break
        return mapLayer
        
    @gui_invoke_later
    def getLayer(self, index):
        """Returns the layer at the given index (zero-based!) for this Display"""
        
        return _Layer(self._JavaProxy__javaObject.getControls()[index])
        
    @gui_invoke_later
    def getLayers(self):
        """Returns a list of all layers used by this Display."""
        
        return [_Layer(displayControl) for displayControl in self._JavaProxy__javaObject.getControls()]

    @gui_invoke_later
    def createLayer(self, layerType, data):
        return self.createLayerWithShortname(layerType, data, 'unable to set shortname macro')
        
    @gui_invoke_later
    def createLayerWithShortname(self, layerType, data, shortname):
        """Creates a new _Layer in this _Display
        
        Args:
            layerType: ID string that represents a type of layer. The valid names
                       can be determined with the "allLayerTypes()" function.
                       
            data: a VisAD Data object to be displayed, or a list of them
            
        Returns:
            The _Layer that was created in this _Display
            
        Raises:
            ValueError:  if layerType isn't valid
        """
        from ucar.unidata.data import DataDataChoice
        from visad.meteorology import ImageSequenceImpl
        from visad.meteorology import SingleBandedImage
        from visad.meteorology import SingleBandedImageImpl
        from visad import FlatField
        from visad import DateTime
        
        # need to get short control description from long name
        mcv = getStaticMcv()
        controlID = None
        for desc in mcv.getControlDescriptors():
            if desc.label == layerType:
                controlID = desc.controlId
        if controlID is None:
            raise ValueError("Layer type '%s' is invalid; please see output of allLayerTypes() for available layer types." % (layerType))
        if controlID == 'imagesequence':
            # hack for backward compatibility: don't let user do an
            # imagesequence since it requires a strange DataChoice and 
            # imagedisplay can handle loops anyway.
            controlID = 'imagedisplay'
            
        # Set the panel/display that a new DisplayControl will be put into
        # TODO(mike):  set this back to what it was before?
        mcv.getVMManager().setLastActiveViewManager(self._JavaProxy__javaObject)

        firstData = data  # keep a ref to the first image in the list
                           # for layer labeling, etc.

        # make this into a list if it isn't already
        if isinstance(data, FlatField):
            # if it is a Visad FlatField..it's not a Python list or java ArrayList, etc.
            # and also not ALREADY a time sequence.
            data = [data]
        else:
            firstData = data[0]

        if isinstance(data[0], _MappedGeoGridFlatField):
            data = makeFlatFieldSequence(data)
        elif isinstance(data[0], SingleBandedImage):
            data = ImageSequenceImpl(data)
        elif isinstance(data[0], FlatField):
            # this is plain flatfield...need to re-attach time somehow and
            # make into some kind of time sequence.
            if isinstance(data[0].getMetadataMap().get('nominal-time'), DateTime):
                # this was a _MappedAreaImageFlatField
                newData = []
                for step in data:
                    # should be a visad.DateTime:
                    theTime = step.getMetadataMap().get('nominal-time') 
                    newData.append(SingleBandedImageImpl(step, theTime,
                        theTime.toString()))
                data = ImageSequenceImpl(newData)
            elif data[0].getMetadataMap().get('times'):
                # this was a _MappedGeoGridFlatField
                data = makeFlatFieldSequence(data)
            else:
                # data might already be a proper time sequence and will just work!
                #print 'DEBUG: cant get a timestamp to make an ImageSequenceImpl...'
                pass

        # figure out the shortname and longname macros if possible,
        # and default layer label
        longname = 'unable to set longname macro'
        defaultLabel = ''
        try:
            hm = firstData.getMetadataMap()
            if hm.containsKey('longname'):
                longname = hm.get('longname')
            if hm.containsKey('shortname'):
                shortname = hm.get('shortname')
            if hm.containsKey('defaultlayerlabel'):
                defaultLabel = hm.get('defaultlayerlabel')
        except AttributeError:
            # no metadataMap
            pass

        # first param of DataDataChoice constructor is %shortname% macro
        try: 
            ddc = DataDataChoice(shortname, data)
        except TypeError:
            # try one more time; this is to make a plain FlatField w/no time field still work
            ddc = DataDataChoice(shortname, data[0])
        if isinstance(firstData, _MappedGeoGridFlatField):
            # this will get the %level% macro filled in,
            # and the GUI will reflect the level in other places.
            ddc.setLevelSelection(firstData.levelReal)
        # setting the description should set the %longname% macro
        ddc.setDescription(longname)
        newLayer = mcv.doMakeControl( 
                [ddc],
                getStaticMcv().getControlDescriptor(controlID),
                None, ddc.getDataSelection(), False)
        
        wrappedLayer = _Layer(newLayer)
            
        wrappedLayer.setLayerLabel(label=defaultLabel)
        
        return wrappedLayer
        
    def _captureAsVector(self, filename):
        from ucar.unidata.idv import VectorGraphicsRenderer
        
        renderer = VectorGraphicsRenderer(self._JavaProxy__javaObject)
        if renderer.showConfigDialog():
            renderer.renderTo(filename)
        
    def _captureAsKml(self, filename, quality):
        from java.awt import Color
        from java.util import ArrayList
        from ucar.unidata.data.gis import KmlDataSource
        from ucar.unidata.ui import ImageUtils
        
        bounds = self._JavaProxy__javaObject.getVisibleGeoBounds()
        if not bounds:
            raise RuntimeError("Could not determine geographic bounds of display; is the display navigable?")
            
        fileRoot, fileExt = os.path.splitext(filename)
        kmlImagePath = fileRoot + '.png'
        views = ArrayList(1)
        views.add(self._JavaProxy__javaObject.getMaster().getComponent())
        columns = ImageUtils.getColumnCountFromComps(views)
        whichComponent = "main display"
        
        images = self._JavaProxy__javaObject.makeBufferedImages(views, whichComponent)
        
        image = ImageUtils.gridImages2(images, 0, Color.GRAY, columns)
        
        KmlDataSource.writeToFile(filename, bounds, kmlImagePath)
        
        ImageUtils.writeImageToFile(image, kmlImagePath, quality)
        
    def _testCaptureImage(self, filename, quality=1.0, formatting=None, ignoreLogo=False):
        """Don't use this method for anything other than testing!"""
        
        from ucar.unidata.idv.ui import ImageGenerator
        from ucar.unidata.xml import XmlUtil
        
        formatting = formatting or []
        
        # this pause is apparently critical
        pause()
        
        # do some sanity checking on filename
        filename = expandpath(filename)
        
        isDir = os.path.isdir(filename)
        
        if isDir:
            # this isn't really good enough.  could be permissions issue, etc.
            raise ValueError(filename, " is a directory")
            
        isl = ''
        
        for formatter in formatting:
            isl += formatter.toIsl()
            
        if not ignoreLogo and self._JavaProxy__javaObject.getLogoVisibility():
            logoFile = self._JavaProxy__javaObject.getLogoFile()
            logoPosition = self._JavaProxy__javaObject.getLogoPosition().upper()
            logoAnchor = logoPosition[0:2]
            # logoScale = self._JavaProxy__javaObject.getLogoScale()
            logoIsl = 'overlay image=%s anchor=%s place=%s; ' % (logoFile, logoAnchor, logoPosition)
            isl += logoIsl
            
        # print 'isl=%s' % (isl[:-2])
        
        islAsXml = ImageGenerator.makeXmlFromString(isl[:-2])
        index = self._getDisplayIndex()
        if index:
            xml = '%s\n<image file="%s" quality="%s" view="#%s">%s</image>' % (XmlUtil.getHeader(), filename, quality, index, islAsXml)
        else:
            xml = '%s\n<image file="%s" quality="%s">%s</image>' % (XmlUtil.getHeader(), filename, quality, islAsXml)
        print 'isl2xml=%s' % (xml)
        islInterpreter.captureImage(islInterpreter.applyMacros(filename), islInterpreter.makeElement(xml))
        
        # not terribly happy about this approach...
        for formatter in formatting:
            if isinstance(formatter, islformatters.Colorbar) and formatter.displayObj:
                layer = formatter.displayObj
                if layer.usedTemporaryId:
                    layer.getJavaInstance().setId(None)
                    layer.usedTemporaryId = False
                    
    def captureImage(self, filename, quality=1.0, height=-1, width=-1):
        """Attempt at a replacement for ISL writeImage.
        
        Args:
            filename: 
            quality:  float between 0.0 and 1.0 (relevant for JPEG's)
                    0.0 is highest compression / smallest file size / worst quality
                    1.0 is least compression / biggest file size / best quality
            height, width: size of image
            
        Raises:
            ValueError:  if filename is a directory
            RuntimeError: if height and width specified here after an annotate
            
        """
        from ucar.unidata.idv.ViewManager import isVectorGraphicsFile
        from visad import DisplayException
        
        # this pause is apparently critical
        pause()
        
        # do some sanity checking on filename
        filename = expandpath(filename)
        
        isDir = os.path.isdir(filename)
        
        if isDir:
            # this isn't really good enough.  could be permissions issue, etc.
            raise ValueError(filename, " is a directory")
            
        if height != -1 and width != -1:
            try:
                self.setSize(width, height)
            except DisplayException, target:
                if "ScalarMap cannot belong to two Displays" in target.getMessage():
                    # this should only happen if captureImage is called
                    # with a height and width after an annotate() in the background
                    raise RuntimeError("Height/width for captureImage is currently not supported after a text annotation. Height/Width can be specified with buildWindow or openBundle instead, then leave height/width out of subsequent calls to captureImage.")
                    
        imageFile = java.io.File(filename)
        fileRoot, fileExt = os.path.splitext(filename.lower())
        extensions = ['.pdf', '.kmz', '.svg']
        if isVectorGraphicsFile(filename):
            self._captureAsVector(filename)
        elif fileExt == '.kml':
            self._captureAsKml(filename, quality)
        elif fileExt in extensions:
            islInterpreter.writeImage(filename, "", quality)
        else:
            # 2nd arg is whether image is written in current thread
            self._JavaProxy__javaObject.writeImage(imageFile, True, quality)
            
        # TODO(mike): catch exceptions resulting from writeImage (e.g., if filename has invalid extension)
        
    #@gui_invoke_later
    def annotate(self, text, lat=None, lon=None, line=None, element=None,
        font=None, color='red', size=None, style=None, alignment=None):
        """Put a text annotation on this panel
        
        Can specify location by a lat/lon point or number of pixels
        from upper left corner of screen (lines from top, elements from left).
        (but not both!).
        
        The location specifies the *bottom left* point of the text string.
        
        Args:  (need text and one of lat/lon or line/element). rest are optional.
           text: the text for annotation
           lat, lon:  need to be specified together.  (required)
                      Specifies ottom left point of text.
           line, element: need to be specified together.
                      Line is number of pixels from top, element is number
                      of pixels from left, for bottom left point of text.
                      Or, can do element="CENTER" which centers the text
                      horizontally.
           font: name of a font.   (optional)
           size: size of font. (optional)
           style:  'NONE', 'BOLD', or 'ITALIC'  (optional)
               Font defaults come from ViewManager.getDisplayListFont()
           color: text color. Default red, for now I guess. this is GUI default.
                 (optional)
           alignment: 2-element tuple representing the (horizontal, vertical)
                text alignment wrt to the given point.  "center" is valid
                in both elements, "left" and "right" are valid for horizontal,
                "top" and "bottom" are valid for vertical.  Can also just pass
                single string "center" which would be equivalent to
                ("center", "center").  Default is ("center", "center"),
                so "alignment" is optional.
                
        Returns:
           a _Layer wrapping a DrawingControl
           
        Raises:
            ValueError: if didn't get proper lat/lon or line/element combo,
            or invalid strings in alignment
        """
        import colorutils
        from visad.georef import EarthLocationTuple
        from ucar.unidata.idv.control.drawing import DrawingGlyph
        
        alignment = alignment or ('center', 'center')
        
        # Force into offscreen mode for the moment so drawing control
        # properties window doesn't flash
        # (see DisplayControlImpl.createIdvWindow for why this works)
        initOffScreen = getStaticMcv().getArgsManager().getIsOffScreen()
        getStaticMcv().getArgsManager().setIsOffScreen(True)
        
        # "False" here means do not "initDisplayInThread":
        drawCtl = getStaticMcv().doMakeControl([],
            getStaticMcv().getControlDescriptor('drawingcontrol'),
            None, None, False)
        drawCtl.setName(text)
        drawCtl.setLegendLabelTemplate(text)
        drawCtl.setShowInDisplayList(False)
        
        pause()
        
        # set offscreen mode back to whatever it was
        getStaticMcv().getArgsManager().setIsOffScreen(initOffScreen)
        
        glyph = TextGlyph(drawCtl, None, text)
        
        horAlign = str(alignment[0]).lower()
        vertAlign = str(alignment[1]).lower()
        if str(alignment).lower() != "center":
            if horAlign != "left" and horAlign != "center" and horAlign != "right":
                raise ValueError('First element of alignment keyword must be "left", "center", or "right".')
                
            if vertAlign != "top" and vertAlign != "center" and vertAlign != "bottom":
                raise ValueError('Second element of alignment keyword must be "top", "center", or "bottom".')
                
        # deal with horizontal/vertical justification keywords.
        # Unfortunately, we need to "reverse" the justification w.r.t.
        # IDV terminology ("right" becomes "left"), because we've decided
        # on the opposite model of alignment as the IDV...
        if horAlign == "center":
            glyph.setHorizontalJustification(TextGlyph.JUST_CENTER)
        if horAlign == "left":
            glyph.setHorizontalJustification(TextGlyph.JUST_RIGHT)
        if horAlign == "right":
            glyph.setHorizontalJustification(TextGlyph.JUST_LEFT)
            
        if vertAlign == "center":
            glyph.setVerticalJustification(TextGlyph.JUST_CENTER)
        if vertAlign == "top":
            glyph.setVerticalJustification(TextGlyph.JUST_BOTTOM)
        if vertAlign == "bottom":
            glyph.setVerticalJustification(TextGlyph.JUST_TOP)
            
        if str(alignment).lower() == "center":
            glyph.setHorizontalJustification(TextGlyph.JUST_CENTER)
            glyph.setVerticalJustification(TextGlyph.JUST_CENTER)
            
        if (lat is not None) and (lon is not None) and ((line is None) and (element is None)):
            # lat lon point
            point = EarthLocationTuple(lat, lon, 0.0)  # TODO: not sure about altitude
            glyph.setCoordType(DrawingGlyph.COORD_LATLONALT)
        elif (line is not None) and (element is not None) and ((lat is None) and (lon is None)):
            if (str(element).lower() == "center"):
                dims = self.getDimensions()
                element = dims[2] / 2  # should be middle pixel
                glyph.setHorizontalJustification(TextGlyph.JUST_CENTER)
            # screen coordinates
            glyph.setCoordType(DrawingGlyph.COORD_XYZ)
            mapDisplay = self._JavaProxy__javaObject.getMapDisplay()
            # note: (element, line) note (line, element):
            point = mapDisplay.getSpatialCoordinatesFromScreen(element, line)
        else:
            raise ValueError("No location was provided; please specify either lat AND lon or line AND element.")
            
        newColor = colorutils.convertColorToJava(color)
        
        currentFont = self._JavaProxy__javaObject.getDisplayListFont()
        newFont = _getNewFont(currentFont, fontName=font, size=size, style=style)
        
        glyph.setName(text)  # not visible after drawCtl.close()
        glyph.setColor(newColor)
        glyph.setFont(newFont)
        pointList = java.util.ArrayList()
        pointList.add(point)
        glyph.setPoints(pointList)
        drawCtl.addGlyph(glyph)
        return _Layer(drawCtl)
        
    @gui_invoke_later
    def setViewpoint(self, viewpointName):
        """Convenience method for changing to a saved "Viewpoint"
        
        Note, a user can define viewpoints via the "Projections -> Viewpoints"
        menu in the main McV window.
        
        Args:
               viewpointName:  the name given to the viewpoint by the user
                               when saving.
                               
        Raises:
               valueError:   if viewpointName isn't a saved viewpoint.
        """
        # Get list of saved viewpoints... These are actually "ViewState" objects
        viewpoints = getStaticMcv().getVMManager().getVMState()
        
        # Pick the desired viewpoint out of the list
        desiredViewpoint = None
        for viewpoint in viewpoints:
            if viewpoint.getName() == viewpointName:
                desiredViewpoint = viewpoint
                break
                
        if desiredViewpoint is None:
            raise ValueError("No viewpoint with the name %s could be found" %
                    viewpointName)
                    
        # change the display to the saved viewpoint
        self._JavaProxy__javaObject.initWith(desiredViewpoint)
        
        
# TODO(jon): still not sure what to offer here.
class _Layer(_JavaProxy):
    def __init__(self, javaObject):
        """Creates a proxy for ucar.unidata.idv.DisplayControl objects.
        
        (Mike says:) addDisplayInfo() doesn't seem  necessary here,
                     so I've removed it for the time being...
        """
        
        #_JavaProxy.__init__(self, javaObject).addDisplayInfo()
        _JavaProxy.__init__(self, javaObject)
        self.usedTemporaryId = False
        
    @gui_invoke_later
    def _getDisplayWrapper(self):
        """Helper method for layer label setters
        
        Returns: _Display associated with this _Layer
        
        Raises: LookupError if no _Display is found
        """
        for wrapper in _Display.displayWrappers:
            if (wrapper._JavaProxy__javaObject.getUniqueId() ==
                    self._JavaProxy__javaObject.getViewManager().getUniqueId()):
                return wrapper
        raise LookupError('Couldnt find a _Display for this _Layer')
        
    def _ensureIslId(self):
        if not self._JavaProxy__javaObject.getId():
            self.usedTemporaryId = True
            self._JavaProxy__javaObject.setId(self._JavaProxy__javaObject.getUniqueId())
            
    def getFrameCount(self):
        # looking like ucar.visad.display.AnimationWidget is the place to be
        pass
        
    def getFrameDataAtLocation(self, lat, lon, frame):
        # just return the value
        pass
        
    def getDataAtLocation(self, lat, lon):
        # should return a dict of timestamp: value ??
        pass
        
    @gui_invoke_later
    def setEnhancement(self, name=None, range=None):
        """Wrapper for setEnhancementTable and setDataRange
        Args:
           Name: the name of the enhancement table.  Don't need to specify
                 "parent" directories like setProjection, but will work if you 
                 do.  Case sensitive!
           Range: 2-element list specifying min and max data range
        """
        if name is not None:  # leave as-is if not specified
            self.setEnhancementTable(name)
            
        # but 'range' is a Python built-in.........
        if range is not None:
            self.setDataRange(range[0], range[1])
            
    @gui_invoke_later
    def getEnhancementTable(self):
        """Get the current enhancement table.
        
        Returns:
            The actual enhancement table object.
        """
        return self._JavaProxy__javaObject.getColorTable()
        
    @gui_invoke_later
    def setEnhancementTable(self, ctName, transparency=None):
        """Change the enhancement table.
        
        Args:
            ctName:  the name of the enhancement table. Unlike setProjection,
                     you don't NEED to specify "parent" table directories,
                     (but it will work if you do).  However, this is
                     CASE SENSITIVE!  Can't really help this because IDV stores
                     color table names case-sensitively.
            transparency:  set the overall transparency of the color table
                     
        Raises:
            ValueError:  couldn't find ctName or transparency invalid value
        Returns: nothing
        """
        
        my_mcv = getStaticMcv()
        ctm = my_mcv.getColorTableManager()
        newct = ctm.getColorTable(ctName)
        
        # if that one didn't work, keep trying hard to figure out what
        # the user meant.
        if newct is None:
            # In case user specifies 'full path' to color table like getProjection,
            # e.g. 'System>Temperature'
            shortName = (ctName.split('>'))[-1]
            newct = ctm.getColorTable(shortName)

        if transparency:
            if (transparency > 1.0) or (transparency < 0.0):
                raise ValueError('transparency must be between 0 and 1')
            newct.setTransparency(transparency)
            
        if newct is not None:
            return self._JavaProxy__javaObject.setColorTable(newct)
        else:
            raise ValueError(
                'setEnhancementTable could not find the enhancement table called %s, note: enhancement table names are CASE SENSITIVE!' 
                    % ctName)
            
    @gui_invoke_later
    def setDataRange(self, minRange, maxRange):
        """ Change the range of the displayed data (and enhancement table)
        
        Args:
            minRange: if min_range evaluates to false, leave as-is
            maxRange: if max_range evaluates to false, leave as-is
            
        Returns: nothing
        """
        from ucar.unidata.util import Range
        
        currentRange = Range(minRange, maxRange)
        # currentRange = self._JavaProxy__javaObject.getRange()
        
        # if (minRange != None):
        #     currentRange.setMin(minRange)
        
        # if (maxRange != None):
        #     currentRange.setMax(maxRange)
        
        self._JavaProxy__javaObject.setRange(currentRange)
        
    @gui_invoke_later
    def setColorScale(self, visible=True, placement=None, font=None, style=None, size=None, color=None, showUnit=None):
        """Wrapper function for all the color scale manipulation stuff
        
        Args:
            visible: boolean whether to display color scale (default True)
            placement: location of color scale. valid strings are
                'Top', 'Bottom', 'Left', 'Right'
            font: name of font. default defined in user preferences.
               Valid options are 'bold', 'italic', 'none'
            size: size of font. default defined in user preferences.
            color: 'colorname' string or [R, G, B] list
        """
        # assume user wants color scale visible unless otherwise specified
        self.setColorScaleVisible(visible)
        
        if placement is not None:
            self.setColorScalePlacement(placement)
            
        if font is not None:  # let setColorScaleFont handle default
            self.setColorScaleFont(fontName=font)
            
        if style is not None:
            self.setColorScaleFont(style=style)
            
        if size is not None:  # let setColorScaleFont handle default
            self.setColorScaleFont(size=size)
            
        if color is not None:
            self.setColorScaleFontColor(color)

        if showUnit is not None:
            self.setColorScaleShowUnit(showUnit)
            
    @gui_invoke_later
    def setColorScaleVisible(self, status):
        """Set visibility of Color Scale (the legend thing that actually shows
           up overlaid on the map)
        
        Args:
            status:  boolean for whether to show color scale
        """
        
        if isinstance(status, bool):
            self._JavaProxy__javaObject.setColorScaleVisible(status)
        else:
            raise ValueError('parameter for setColorScaleVisible must be boolean (either True or False')
            
    @gui_invoke_later
    def setColorScalePlacement(self, pos):
        """Set the placement of the color scale on the map.
        
        Args:
            pos: string that can be either "Left", "Top", "Bottom", or "Right"
                 (NOT case sensitive!)
                 
        Raises:
            ValueError:  if pos is not one of the four valid choices
        """
        if isinstance(pos, str):
            # handy string method that does exactly what we need:
            # (first letter capitalized, the rest are small)
            pos = pos.capitalize()
            
        if (pos == 'Left') or (pos == 'Top') or (pos == 'Bottom') or (pos == 'Right'):
            info = self._JavaProxy__javaObject.getColorScaleInfo()
            info.setPlacement(pos)
            # this will call the (protected) applyColorScaleInfo(),
            # which is necessary to update the display:
            self._JavaProxy__javaObject.setColorScaleInfo(info)
        else:
            raise ValueError(pos, 'is not valid. The only valid strings are:    '+
                                   'Top  |  Bottom  |  Left  |  Right')
            
    def setColorScaleFont(self, fontName=None, style=None, size=None):
        """For the color scale, change the font, font style, and/or font size
        
        Args:
            fontName (optional): string containing font name (default: leave as-is)
                                    (case-insensitive)
            style (optional): string containing either NONE (default: as-is), BOLD, or ITALIC
                                (case-insensitive)
            size (optional):  font size (default: as-is)
            
        Returns: nothing
        """
        
        info = self._JavaProxy__javaObject.getColorScaleInfo()
        
        currentFont = info.getLabelFont()
        newFont = _getNewFont(currentFont, fontName, style, size)
        
        info.setLabelFont(newFont)
        self._JavaProxy__javaObject.setColorScaleInfo(info)
        
    @gui_invoke_later
    def setColorScaleFontColor(self, color):
        """Set color of color scale labels
        Args:
            color can be rgb list or tuple, or string giving name of a color
            
        I'm leaning toward keeping this separate from setColorScaleFont since
        it wraps around a different java method (setLabelColor)
        """
        import colorutils
        # rgb = colorutils.convertColor(color)
        # r = rgb[0].getConstant()
        # g = rgb[1].getConstant()
        # b = rgb[2].getConstant()
        # newColor = java.awt.Color(r, g, b)
        newColor = colorutils.convertColorToJava(color)
        
        info = self._JavaProxy__javaObject.getColorScaleInfo()
        info.setLabelColor(newColor)
        self._JavaProxy__javaObject.setColorScaleInfo(info)

    @gui_invoke_later
    def setColorScaleShowUnit(self, showUnit):
        """Set whether the unit is shown at the end of the color scale
        Args:
            showUnit:  boolean, to set visibility of unit label
        """
        info = self._JavaProxy__javaObject.getColorScaleInfo()
        info.setUnitVisible(showUnit)
        self._JavaProxy__javaObject.setColorScaleInfo(info)
        
    @gui_invoke_later
    def setLayerVisible(self, status):
        """Set visibility of this layer
        
        Args:
            status:  boolean for visibility of layer
        """
        self._JavaProxy__javaObject.setDisplayVisibility(status)
        
    @gui_invoke_later
    def getLayerLabel(self):
        """Returns the current layer label text.
        
        Returns:
            string containing a layer label.
        """
        return self._JavaProxy__javaObject.getDisplayListTemplate()
        
    @gui_invoke_later
    def setLayerLabel(self, label=None, visible=True, font=None, style=None, size=None, color=None):
        """ Set the layer label (the string of text at the bottom of maps) and other
            properties of layer labels.  Confusingly and not helpful is that properties of layer labels
            are set per panel instead of per layer.  So really, this should be a function of _Display
            instead of _Layer...?
            
        (In Java-land, Layer Labels are "Display Lists")
        
        Args:
            label:  a string defining the layer label (default: as-is)
                  Note, macros (eg %datasourcename%) will get expanded but
                  often get expanded to empty strings (especially with
                  data from getADDEImage)
            visible: boolean whether to display color scale (default True)
            placement: location of color scale. valid strings are
                'Top', 'Bottom', 'Left', 'Right'
            font: name of font. default defined in user preferences.
               Valid options are 'bold', 'italic', 'none'
            size: size of font. default defined in user preferences.
            color: 'colorname' string or [R, G, B] list
            
        Returns:  nothing
        """
        if label is not None:
            label = str(label)  # convert to str if possible
            self._JavaProxy__javaObject.setDisplayListTemplate(label)
            
        self.setLayerLabelVisible(visible)
        self._getDisplayWrapper().labelDict['visible'] = visible
        
        if font is not None:
            self.setLayerLabelFont(fontName=font)
            
        if style is not None:
            self.setLayerLabelFont(style=style)
            
        if size is not None:  # let setColorScaleFont handle default
            self.setLayerLabelFont(size=size)
            
        if color is not None:
            self.setLayerLabelColor(color)
            
        self._JavaProxy__javaObject.getViewManager().updateDisplayList()
        
    @gui_invoke_later
    def getLayerVisible(self):
        """Determine whether or not this layer is visible.
        
        Returns:
            True if visible, False otherwise.
        """
        return self._JavaProxy__javaObject.getDisplayVisibility()
        
    @gui_invoke_later
    def setLayerLabelVisible(self, status):
        """Set whether the Display List is shown for this ViewManager

        Args:
            status:  True - visible or False - not visible

        Raises:
            ValueError: if status isn't a boolean
        """
        if isinstance(status, bool):
            self._JavaProxy__javaObject.getViewManager().setShowDisplayList(status)
            self._getDisplayWrapper().labelDict['visible'] = status
        else:
            raise ValueError('parameter for setLayerLabelVisible must be boolean (either True or False')
            
    @gui_invoke_later
    def setLayerLabelColor(self, color):
        """Set color of Display List labels (confusingly, these are per panel
            and not per layer).
    
        Args:
            color can be rgb list or tuple, or string giving name of a color
        """
        import colorutils
        # rgb = colorutils.convertColor(color)
        # r = rgb[0].getConstant()
        # g = rgb[1].getConstant()
        # b = rgb[2].getConstant()
        # newColor = java.awt.Color(r, g, b)
        newColor = colorutils.convertColorToJava(color)
        
        self._JavaProxy__javaObject.getViewManager().setDisplayListColor(newColor)
        self._getDisplayWrapper().labelDict['color'] = newColor
        
    @gui_invoke_later
    def setLayerLabelFont(self, fontName=None, style=None, size=None):
        """ set the font of Display List
        
        Args:
            fontName (optional): string containing font name (default: leave as-is)
                                    (case-insensitive)
            style (optional): string containing either NONE (default: as-is), BOLD, or ITALIC
                                (case-insensitive)
            size (optional):  font size (default: as-is)
        """
        vm = self._JavaProxy__javaObject.getViewManager()
        currentFont = vm.getDisplayListFont()
        newFont = _getNewFont(currentFont, fontName, style, size)
        vm.setDisplayListFont(newFont)
        self._getDisplayWrapper().labelDict['font'] = newFont

    @gui_invoke_later
    def setVerticalPosition(self, verticalPosition):
        """ wrapper around DisplayControlImpl.setZPosition

        Args:
            verticalPosition: float value between -1 and 1

        Raises:
            ValueError: if verticalPosition is not valid
        """
        if (verticalPosition < -1.0 or verticalPosition > 1.0):
            raise ValueError('verticalPosition must be between -1.0 and 1.0')
        self._JavaProxy__javaObject.setZPosition(verticalPosition)
        
    @gui_invoke_later
    def getLayoutModelName(self):
        if isinstance(self._JavaProxy__javaObject, ValuePlanViewControl):
            return str(self._JavaProxy__javaObject.getLayoutModel().getName())
        else:
            return str(self._JavaProxy__javaObject.getStationModel().getName())
            
    @gui_invoke_later
    def getLayoutModel(self):
        if isinstance(self._JavaProxy__javaObject, ValuePlanViewControl):
            return self._JavaProxy__javaObject.getLayoutModel()
        else:
            return self._JavaProxy__javaObject.getStationModel()
        
    @gui_invoke_later
    def setLayoutModel(self, model=None):
        """Change the station layout model for the current layer.
        
        If the type of the given model is a string, this method will attempt to
        find the first StationModel object whose name is an exact match. If there
        was not an exact match, the method will try to find a partial match
        (partial matches occur when the given model string is a
        case-insensitive substring of a StationModel object's name).
        
        More than one partial match is considered ambiguous and will result in
        a ValueError exception being raised.
        
        Optional Args:
            model: If provided, this can be a string value representing the name
            of a station model or an actual StationModel object. Default behavior
            is to use the StationModel object returned by defaultLayoutModel.
            
        Raises:
            ValueError: if model was a string and there was neither an exact nor
                        partial matches.
                        
            ValueError: if model was a string without an exact match, but more
                        than one partial match.
                        
            TypeError: if model's type was neither a string nor StationModel.
        """
        from java.lang import String
        from ucar.unidata.ui.symbol import StationModel
        
        if not model:
            model = defaultLayoutModel()
        elif isinstance(model, (str, unicode, String)):
            match, partialMatches = _getLayoutModelByName(model)
            # print "exact match: '%s'; partials='%s'" % (match, partialMatches)
            if match is not None:
                model = match
            elif len(partialMatches) <= 0:
                raise ValueError("Could not find an exact or partial match for station layout name '%s'. Call allLayoutModelNames for valid options." % (model))
            elif len(partialMatches) > 1:
                raise ValueError("Station layout name '%s' has no exact matches, and resulted in multiple partially matching station model layouts (%s). Please try a more specific name or call allLayoutModelNames for valid options." % (model, partialMatches))
            else:
                model = partialMatches[0]
        elif not isinstance(model, StationModel):
            raise TypeError("Invalid 'model' parameter type: %s" % (type))
            
        if isinstance(self._JavaProxy__javaObject, ValuePlanViewControl):
            self._JavaProxy__javaObject.setLayoutModel(model)
        else:
            self._JavaProxy__javaObject.setStationModel(model)
            
    @gui_invoke_later
    def enableDeclutter(self):
        """Enable decluttering of value plot displays.
        
        This method will only force a change if the underlying layer has 
        decluttering disabled.
        
        Raises:
            NotImplementedError: if the underlying layer is not a 
                                 ValuePlanViewControl.
        """
        if not isinstance(self._JavaProxy__javaObject, ValuePlanViewControl):
            raise NotImplementedError('decluttering not support for this layer type')
            
        if not self._JavaProxy__javaObject.getDeclutter():
            self._JavaProxy__javaObject.setDeclutter(True)
            self._JavaProxy__javaObject.loadDataInThread()
        
    @gui_invoke_later
    def disableDeclutter(self):
        """Disable decluttering of value plot displays.
        
        This method will only force a change if the underlying layer has 
        decluttering enabled.
        
        Raises:
            NotImplementedError: if the underlying layer is not a 
                                 ValuePlanViewControl.
        """
        if not isinstance(self._JavaProxy__javaObject, ValuePlanViewControl):
            raise NotImplementedError('decluttering not support for this layer type')
            
        if self._JavaProxy__javaObject.getDeclutter():
            self._JavaProxy__javaObject.setDeclutter(False)
            self._JavaProxy__javaObject.loadDataInThread()
        
    @gui_invoke_later
    def isDeclutterEnabled(self):
        """Determine whether or not decluttering is enabled.
        
        Returns:
            True if decluttering is enabled, False otherwise.
            
        Raises:
            NotImplementedError: if the underlying layer is not a 
                                 ValuePlanViewControl.
        """
        if not isinstance(self._JavaProxy__javaObject, ValuePlanViewControl):
            raise NotImplementedError('decluttering not support for this layer type')
            
        return self._JavaProxy__javaObject.getDeclutter()
        
    @gui_invoke_later
    def getDeclutterFilter(self):
        """Determine the decluttering filter factor.
        
        Returns:
            The decluttering filter factor.
            
        Raises:
            NotImplementedError: if the underlying layer is not a 
                                 ValuePlanViewControl.
        """
        if not isinstance(self._JavaProxy__javaObject, ValuePlanViewControl):
            raise NotImplementedError('decluttering not support for this layer type')
            
        return self._JavaProxy__javaObject.getDeclutterFilter()
        
    @gui_invoke_later
    def setDeclutterFilter(self, filterFactor):
        """Set the decluttering filter factor.
        
        Args:
            filterFactor: a floating point value that controls the number of 
                          displayed values. Lower means more values, higher 
                          means less.
                          
        Raises:
            NotImplementedError: if the underlying layer is not a 
                                 ValuePlanViewControl.
        """
        if not isinstance(self._JavaProxy__javaObject, ValuePlanViewControl):
            raise NotImplementedError('decluttering not support for this layer type')
            
        if self._JavaProxy__javaObject.getDeclutterFilter() != filterFactor:
            self._JavaProxy__javaObject.setDeclutterFilter(filterFactor)
            self._JavaProxy__javaObject.loadDataInThread()
            
# TODO(jon): this (and its accompanying subclasses) are a productivity rabbit
# hole!
class _DataSource(_JavaProxy):
    def __init__(self, javaObject):
        """Blank for now.
           server
           dataset
           imageType
           coordinateType
           xcoordinate
           ycoordinate
           xyLocation
           unit
           magnification
           lineSize
           elementSize
        """
        _JavaProxy.__init__(self, javaObject)
        
    @gui_invoke_later
    def allDataChoices(self):
        """Return a list of strings describing all available data choices
            MIKE
        """
        # return just the strings so that user isn't forced to use the result of allDataChoices
        # in later method calls
        choices = self._JavaProxy__javaObject.getDataChoices()
        return [choice.description for choice in choices]
        
    @gui_invoke_later
    def getDataChoice(self, dataChoiceName):
        """Return a _DataChoice associated with this _DataSource
           
        Args:
            dataChoiceName: name of data choice

        Returns:  appropriate _DataChoice

        Raises:
            ValueError:  if dataChoiceName doesn't exist int his data source
        MIKE
        """
        choices = self._JavaProxy__javaObject.getDataChoices()
        for choice in choices:
            if choice.description == dataChoiceName:
                return _DataChoice(choice)
        raise ValueError("There is no data choice by that name for this data source")
        
class _DataChoice(_JavaProxy):
    def __init__(self, javaObject):
        """Represents a specific field within a data source
           I don't know if "DataChoice" is the best name here
           but that is how it is called in Java code
           MIKE
        """
        _JavaProxy.__init__(self, javaObject)
        
    @gui_invoke_later
    def allLevels(self):
        """List all levels for this data choice.
        """
        return self._JavaProxy__javaObject.getAllLevels()
        
    @gui_invoke_later
    def setLevel(self, level):
        """Set which level you want from this data choice before plotting.
            TODO(mike): This is extremely experimental at the moment...
            
            Works for some data sources (model grids) but not others (radar)
            
        Args:
            level: one of the elements in the list returned by getLevels()
        """
        self._JavaProxy__javaObject.setLevelSelection(level)
        return
        
# TODO(jon): still not sure what people want to see in here
class _Projection(_JavaProxy):
    def __init__(self, javaObject):
        """Creates a proxy for ucar.unidata.geoloc.Projection objects."""
        _JavaProxy.__init__(self, javaObject)
        
# TODO(jon): a *LOT* of this functionality isn't currently offered by colortables...
class _ColorTable(_JavaProxy):
    def __init__(self, javaObject):
        """Creates a proxy for ucar.unidata.util.ColorTable objects.
           width
           height
           xLocation
           yLocation
           minValue
           maxValue
           majorInterval
           minorInterval
        """
        _JavaProxy.__init__(self, javaObject)
        
# TODO(jon): "annotation" is ambiguous...does it refer to the layer description
# or a drawing control?
class _Annotation(_JavaProxy):
    def __init__(self, javaObject):
        """Blank for now.
           font
           fontColor
           fontSize
           value(string)
           xLocation
           yLocation
        """
        _JavaProxy.__init__(self, javaObject)
        
    def getFontName(self):
        pass
        
    def getFontColor(self):
        pass
        
    def getFontSize(self):
        pass
        
    def getFontStyle(self):
        pass
        
    def getFontInfo(self):
        # return a tuple: name, size, color, style? (like bold, etc)
        # would REALLY like to have named tuples here...
        pass
        
    def getText(self):
        pass
        
    def setText(self, text):
        pass
        
    def getCoordinates(self):
        # (x,y) tuple
        pass
        
@gui_invoke_later
def setViewSize(width, height):
    """Set the view size to a given width and height.
    
    Longer description goes here.
    
    Args:
        width:
        height:
    """
    getStaticMcv().getStateManager().setViewSize(java.awt.Dimension(width, height))
    
@gui_invoke_later
def getColorTable(name=ColorTableDefaults.NAME_DEFAULT):
    """Return the ColorTable associated with the given name.
    
    Longer description goes here.
    
    Args:
        name: The name of the desired ColorTable. If no name was given, the
              name of the IDV's default ColorTable will be used.
              
    Returns:
        The first ColorTable with a matching name.
        
    Raises:
        LookupError: If there was no ColorTable with the given name.
    """
    colorTable = getStaticMcv().getColorTableManager().getColorTable(name)
    if colorTable:
        return _ColorTable(colorTable)
    else:
        raise LookupError("Couldn't find a ColorTable named ", name, "; try calling 'colorTableNames()' to get the available ColorTables.")
        
@gui_invoke_later
def colorTableNames():
    """Returns a list of the valid color table names."""
    return [colorTable.getName() for colorTable in getStaticMcv().getColorTableManager().getColorTables()]
    
@gui_invoke_later
def allColorTables():
    """Returns a list of the available color tables."""
    return [_ColorTable(colorTable) for colorTable in getStaticMcv().getColorTableManager().getColorTables()]
    
@gui_invoke_later
def firstWindow():
    return _Window(IdvWindow.getMainWindows()[0])
    
@gui_invoke_later
def allWindows():
    return [_Window(window) for window in IdvWindow.getMainWindows()]
    
@gui_invoke_later
def firstDisplay():
    """Returns the first display
    
    Longer description goes here.
    
    Returns:
         The first Display (aka ViewManager).
         
    Raises:
        IndexError: If there are no Displays.
    """
    return _Display(getStaticMcv().getVMManager().getViewManagers().get(0))
    
@gui_invoke_later
def allDisplays():
    """Returns a list of all McIDAS-V displays (aka ViewManagers)"""
    return [_Display(viewManager) for viewManager in getStaticMcv().getVMManager().getViewManagers()]
    
@gui_invoke_later
def activeDisplay():
    """Returns the active McIDAS-V display."""
    return _Display(getStaticMcv().getVMManager().getLastActiveViewManager())

# def windowDisplays(window):
#     """Returns a list of the McIDAS-V displays within the given window."""
#     pass

@gui_invoke_later
def createDataSource(path, filetype):
    """Currently just a wrapper around makeDataSource in shell.py
       
    Args:
        path:  path to local file
        filetype:  type of data source (one of the strings given by dataSourcesNames() )

    Returns:
        the DataSource that was created

    Raises:
        ValueError:  if filetype is not a valid data source type
    MIKE
    """
    mcv = getStaticMcv()
    dm = mcv.getMcvDataManager()
    for desc in dm.getDescriptors():
        if desc.label == filetype:
            return _DataSource(makeDataSource(path, type=desc.id))
    raise ValueError("Couldn't find that data source type")
    
@gui_invoke_later
def allDataSourceNames():
    """Returns a list of all possible data source types
       (specifically, the verbose descriptions as they appear in the GUI)
       MIKE
    """
    mcv = getStaticMcv()
    dm = mcv.getDataManager()
    # want to return list of labels only, not DataSourceDescriptor's
    return [desc.label for desc in dm.getDescriptors()]
    
@gui_invoke_later
def allLayerTypes():
    """Returns a list of the available layer type names"""
    return getStaticMcv().getAllControlDescriptors()
    
@gui_invoke_later
def allProjections():
    """Returns a list of the available projections."""
    return [_Projection(projection) for projection in getStaticMcv().getIdvProjectionManager().getProjections()]
    
@gui_invoke_later
def allFontNames():
    """Return a list of strings representing all available font names"""
    return [font.toString() for font in ucar.unidata.util.GuiUtils.getFontList()]
    
@gui_invoke_later
def projectionNames():
    """Returns a list of the available projection names"""
    return [projection.getName() for projection in getStaticMcv().getIdvProjectionManager().getProjections()]
    
@gui_invoke_later
def getProjection(name=''):
    """Returns the projection associated with the given name.
    
    Longer description here.
    
    Args:
        name: Name of the desired projection.
        
    Returns:
        The first projection whose name matches the given name. If the given
        name is empty (or None), McIDAS-V's default projection is returned.
        (does that make sense!?)
        
    Raises:
        ValueError: If there was no projection with the given name.
    """
    mcv = getStaticMcv()
    if not name:
        return _Projection(mcv.getIdvProjectionManager().getDefaultProjection())
        
    for projection in mcv.getIdvProjectionManager().getProjections():
        if name == projection.getName():
            return _Projection(projection)
    else:
        raise ValueError("Couldn't find a projection named ", name, "; try calling 'projectionNames()' to get the available projection names.")
        
def allLayoutModelNames():
    """Returns a list of the available station model layout names."""
    return [str(stationModel.getName()) for stationModel in getStaticMcv().getStationModelManager().getStationModels()]
    
def allLayoutModels():
    """Returns a list of the available station model layout names."""
    return [stationModel for stationModel in getStaticMcv().getStationModelManager().getStationModels()]
    
def defaultLayoutModelName():
    """Returns the name of the default station model layout."""
    return str(getStaticMcv().getStationModelManager().getDefaultStationModel().getName())
    
def defaultLayoutModel():
    """Returns the default station model layout."""
    return getStaticMcv().getStationModelManager().getDefaultStationModel()
    
def _getLayoutModelByName(name):
    """Find station model layouts by name.
    
    Args:
        name: Name of the desired station model layout.
    
    Returns:
        Tuple containing two elements. The first is either a StationModel
        object or None. The second is (always) a list of StationModel objects
        whose name is a partial match for the given name.
    """
    name = unicode(name)
    lowered = name.lower()
    modelManager = getStaticMcv().getStationModelManager()
    model = None
    partials = []
    # if an exact match is found, the contents of partials is irrelevant
    for tempModel in modelManager.getStationModels():
        if name == tempModel.getName():
            model = tempModel
            break
        elif lowered in tempModel.getName().lower():
            partials.append(tempModel)
    return model, partials
    
@gui_invoke_later
def allActions():
    """Returns the available McIDAS-V action identifiers."""
    actions = getStaticMcv().getIdvUIManager().getCachedActions().getAllActions()
    return [action.getId() for action in actions]
    
@gui_invoke_later
def performAction(action):
    # not terribly different from "idv.handleAction('action:edit.paramdefaults')"
    # key diffs:
    # *only* handles actions
    # does not require you to prepend everything with 'action:' (but you can if you must)
    available = allActions()
    if not action.startswith('action:'):
        prefixedId = 'action:' + action
    else:
        prefixedId = action
        action = action.replace('action:', '')
        
    if action in available:
        getStaticMcv().handleAction(prefixedId)
    else:
        raise ValueError("Couldn't find the action ID ", action, "; try calling 'allActions()' to get the available action IDs.")
        
# def load_enhancement(name=''):
#     """Nothing yet."""
#     pass
#
# def load_map(name=''):
#     """Nothing yet."""
#     pass
#
# def annotate(text=''):
#     """Nothing yet."""
#     pass
#
# def apply_colorbar(name=''):
#     """Nothing yet."""
#     pass
#
# def write_image(path=''):
#     """Nothing yet."""
#     pass

def collect_garbage():
    """Signals to Java that it should free any memory that isn't in use."""
    print '* WARNING: please use the new name for this function:\n\'collectGarbage()\''
    collectGarbage()

def collectGarbage():
    """Signals to Java that it should free any memory that isn't in use."""
    System.gc()
    
@gui_invoke_later
def removeAllData():
    """Removes all of the current data sources WITHOUT prompting."""
    getStaticMcv().removeAllData(False)
    
@gui_invoke_later
def removeAllLayers():
    """Removes all of the current layers WITHOUT prompting."""
    getStaticMcv().removeAllLayers(False)
    
@gui_invoke_later
def boomstick():
    """ This is [your] BOOOMSTICK! """
    mcv = getStaticMcv()
    mcv.removeAllLayers(False)
    mcv.removeAllData(False)
    System.gc()
    
def setJythonShellMaxHistoryLength(newHistoryLength):
    """Set the number of commands remembered in the Jython Shell History"""
    from ucar.unidata.idv.ui import JythonShell
    JythonShell.saveMaxHistoryLength(getStaticMcv().getStore(), newHistoryLength)

def getJythonShellMaxHistoryLength():
    """Returns the number of commands remembered in the Jython Shell History"""
    from ucar.unidata.idv.ui import JythonShell
    return JythonShell.loadMaxHistoryLength(
            getStaticMcv().getStore(), JythonShell.DEFAULT_MAX_HISTORY_LENGTH)

class _NoOp(object):

    def __init__(self, description='anything'):
        self.description = description

    def __repr__(self):
        return self.description

MAP = _NoOp('MAP')
MAP2D = _NoOp('MAP2D')
GLOBE = _NoOp('GLOBE')
TRANSECT = _NoOp('TRANSECT')

def buildWindow(width=600, height=400, rows=1, cols=1, panelTypes=None):
    """Call _buildWindowInternal (from Jython Shell) or _buildWindowBackground (from background)
    """
    def _buildWindowInternal(width, height, rows, cols, panelTypes):
        """Creates a window with a user-specified layout of displays.
        
        This function will attempt to create a grid of displays with the dimensions
        determined by rows * cols. Simply calling buildWindow() will result in a
        1x1 grid containing a single map.
        
        Args:
            width: Optional parameter; default value is zero. Sets the window to
                   this width (in pixels). Values less than or equal to zero are
                   considered default values.
            
            height: Optional parameter; default value is zero. Sets the window to
                    this height (in pixels). Values less than or equal to zero are
                    considered default values.
            
            rows: Optional parameter; default value is one.
            
            cols: Optional parameter; default value is one.
            
            panelTypes: Optional parameter; default value is None (creates a single
                Map Display).
            
        Returns:
            A "wrapped" IdvWindow.
        """
        
        from edu.wisc.ssec.mcidasv import PersistenceManager
        
        try:
            window = PersistenceManager.buildDynamicSkin(width, height, rows, cols, panelTypes)
            if width > 0 and height > 0:
                # print 'creating window: width=%d height=%d rows=%d cols=%d panelTypes=%s' % (width, height, rows, cols, panelTypes)
                pass
            else:
                bounds = window.getBounds()
                # print 'creating window: width=%d height=%d rows=%d cols=%d panelTypes=%s' % (bounds.width, bounds.height, rows, cols, panelTypes)
                
            panels = []
            for holder in window.getComponentGroups()[0].getDisplayComponents():
                for viewManager in holder.getViewManagers():
                    wrapped = _Display(viewManager)
                    wrapped.setSize(width, height)
                    panels.append(wrapped)
            return panels
        except NullPointerException, e:
            raise RuntimeError("could not build window", e)
            
    def _buildWindowBackground(height, width, panelTypes):
        """
         (1) create a new MapViewManager.  This is the default type of ViewManager
             if (null, null) is passed to createViewManager
         (2) Wrap the MapViewManager in a _Display object
         (3) Wrap the _Display in a list, simply because current calls to buildWindow expect this
          
          Default size:  600 x 400
        """
        if height > 0 and width > 0:
            dim = java.awt.Dimension(width, height)
            # this utilizes the fact that doMakeDisplayMaster in MapViewManager gets it's default
            # dimension from StateManager.getViewSize().  It's slightly hack but much easier
            # than creating my own DisplayMaster and adding it to a new ViewManager.
            # Also, it seems to be much easier to create a ViewManager with a given Dimension
            # than to change it afterward...
            getStaticMcv().getStateManager().setViewSize(dim)
        
        if panelTypes[0] is GLOBE:
            propString = 'useGlobeDisplay=true'
        elif panelTypes[0] is MAP2D:
            propString = 'use3D=false'
        else:
            propString = ''
        
        newVM = getStaticMcv().getVMManager().createViewManager(None, propString)
        return [_Display(newVM)]
        
    # end of internal method definitions..this is buildWindow now.
    if panelTypes is None:
        panelTypes = [MAP] * (rows * cols)
    elif isinstance(panelTypes, _NoOp):
        panelTypes = [panelTypes] * (rows * cols)
    elif isinstance(panelTypes, types.ListType):
        if len(panelTypes) != (rows * cols):
            raise ValueError('panelTypes needs to contain rows*cols elements')
            
    if getStaticMcv().getArgsManager().getIsOffScreen():
        return _buildWindowBackground(height, width, panelTypes)
    else:
        if len(panelTypes) > 1:
            print '* WARNING: buildWindow will only build one panel when run from the background'
        return _buildWindowInternal(width, height, rows, cols, panelTypes)
        
def makeLogger(name):
    """ """
    return  LoggerFactory.getLogger(name)
    
def openBundle(bundle, label="", clear=1, height=-1, width=-1, dataDictionary=None, mode=None):
    """Open a bundle using the decodeXmlFile from PersistenceManager

    Args:
        bundle: location of bundle to be loaded

        label: Label for bundle?  where is this displayed?

        clear: whether to clear current layers and data (1 or 0)
        Default is to clear.

        height, width: specify size of window (not size of display!)

        dataDictionary: allows you to override what files are used for a
        given datasource.  (This was known as setfiles in ISL).
        The keys specify the name of the data source (as shown in e.g.,
        the Field Selector tab).  The values can be either a single
        file or a list of files to use for the given datasource.

        mode method used to load the bundle
            newWindow - opens bundle in a new window w/o removing any previously existing layers and data
            merge - merges the bundle's layers and data with the current tab in your existing display window
            newTab - adds a new tab(s) to your current display window for the bundle's data w/o removing any previously existing layers and data
            replace - replaces the current session in place of the bundle.  This removes any previously loaded layers and data as well as your existing window/tab/panel configuration (default).

    Returns:
        the result of activeDisplay()

    Raises:
        ValueError: if bundle doesn't exist
        ValueError: if height is specified but not width, or vice verse
    """
    from edu.wisc.ssec.mcidasv import McIdasPreferenceManager
    from edu.wisc.ssec.mcidasv import PersistenceManager
    
    my_mcv = getStaticMcv()
    sm = my_mcv.getStateManager()
    mpm = McIdasPreferenceManager # for some of the PREF constants
    
    # Allows user to specify file with for example, ~/bundlefile.mcv
    bundle = expandpath(bundle)
    
    fileExists = os.path.exists(bundle)
    isDir = os.path.isdir(bundle)
    
    if (not fileExists) or isDir:
        raise ValueError("File does not exist or is a directory")
        
    #if ((height == -1) and (width != -1)) or ((height != -1) and (width == -1)):
    #    raise ValueError("Please specify both a width and height")
    
    # get current relevant user preferences so we can override them
    #   and then change them back
    # careful about the second argument here...this is default if preference
    #   hasn't already been written.  Might be important for fresh installs?
    #   (for now, I set these to what I believe to be McV defaults on fresh install
    pref_zidv_ask_user = sm.getPreference(my_mcv.PREF_ZIDV_ASK, True)
    pref_open_ask_user = sm.getPreference(my_mcv.PREF_OPEN_ASK, True)
    pref_open_remove_user = sm.getPreference(my_mcv.PREF_OPEN_REMOVE, False)
    pref_open_merge_user = sm.getPreference(my_mcv.PREF_OPEN_MERGE, False)
    pref_zidv_savetotmp_user = sm.getPreference(my_mcv.PREF_ZIDV_SAVETOTMP, True)
    pref_zidv_directory_user = sm.getPreference(my_mcv.PREF_ZIDV_DIRECTORY, '')
    pref_confirm_data = sm.getPreference(mpm.PREF_CONFIRM_REMOVE_DATA, True)
    pref_confirm_layers = sm.getPreference(mpm.PREF_CONFIRM_REMOVE_LAYERS, True)
    pref_confirm_both = sm.getPreference(mpm.PREF_CONFIRM_REMOVE_BOTH, True)
    
    # see McIdasPreferenceManager:1360 for what these should get set to
    if str(mode).lower() == 'newwindow':
        sm.putPreference(my_mcv.PREF_OPEN_REMOVE, False)
        sm.putPreference(my_mcv.PREF_OPEN_MERGE, False)
    elif str(mode).lower() == 'merge':
        sm.putPreference(my_mcv.PREF_OPEN_REMOVE, True)
        sm.putPreference(my_mcv.PREF_OPEN_MERGE, False)
    elif str(mode).lower() == 'newtab':
        sm.putPreference(my_mcv.PREF_OPEN_REMOVE, False)
        sm.putPreference(my_mcv.PREF_OPEN_MERGE, True)
    elif str(mode).lower() == 'replace':
        sm.putPreference(my_mcv.PREF_OPEN_REMOVE, True)
        sm.putPreference(my_mcv.PREF_OPEN_MERGE, True)
    else:
        # do "replace" by default"
        sm.putPreference(my_mcv.PREF_OPEN_REMOVE, True)
        sm.putPreference(my_mcv.PREF_OPEN_MERGE, True)
        
    # set relevant preferences to values that make sense for non-GUI mode
    sm.putPreference(my_mcv.PREF_ZIDV_ASK, False)
    sm.putPreference(my_mcv.PREF_OPEN_ASK, False)
    # For REMOVE and MERGE, we want to do the same thing as what McIdasPreferenceManager
    # does for "Replace Session" (set both to true)
    sm.putPreference(my_mcv.PREF_ZIDV_SAVETOTMP, True)
    sm.putPreference(mpm.PREF_CONFIRM_REMOVE_DATA, False)
    sm.putPreference(mpm.PREF_CONFIRM_REMOVE_LAYERS, False)
    sm.putPreference(mpm.PREF_CONFIRM_REMOVE_BOTH, False)


    # ZIDV_DIRECTORY should come from keyword
    # (also need to check for existence of this directory, etc.)
    #my_mcv.getStore().put(my_mcv.PREF_ZIDV_DIRECTORY, something??)
    sm.writePreferences()
    
    pm = my_mcv.getPersistenceManager()
    
    if dataDictionary is not None:
        # It turns out the whole dictionary thing boils down to a call to
        # PersistenceManager.setFileMapping which takes a list of ids and
        # a list containing lists of files for each datasource id.  Then we
        # call clearFileMapping to clean up.
        # So, make datasource ids list and list of file lists:
        ids = java.util.ArrayList()
        fileLists = java.util.ArrayList()
        for key in dataDictionary.keys():
            ids.add(key)
            fileList = java.util.ArrayList()
            value = dataDictionary[key]
            if isinstance(value, list):
                for element in value:
                    fileList.add(element)
            else:
                fileList.add(value)
            fileLists.add(fileList)
        pm.setFileMapping(ids, fileLists)
        
    checkToRemove = clear
    letUserChangeData = 0    # not sure about this
    bundleProperties = None  # not sure what this does..just send it None for now
    pm.decodeXmlFile(bundle, label, checkToRemove, letUserChangeData, bundleProperties)
    pause()  # MJH 2013-05-15 not sure about this one...
    
    if dataDictionary is not None:
        pm.clearFileMapping()
        
    # change relevant preferences back to original values
    sm.putPreference(my_mcv.PREF_ZIDV_ASK, pref_zidv_ask_user)
    sm.putPreference(my_mcv.PREF_OPEN_ASK, pref_open_ask_user)
    sm.putPreference(my_mcv.PREF_OPEN_REMOVE, pref_open_remove_user)
    sm.putPreference(my_mcv.PREF_OPEN_MERGE, pref_open_merge_user)
    sm.putPreference(my_mcv.PREF_ZIDV_SAVETOTMP, pref_zidv_savetotmp_user)
    sm.putPreference(my_mcv.PREF_ZIDV_DIRECTORY, pref_zidv_directory_user)
    sm.putPreference(mpm.PREF_CONFIRM_REMOVE_DATA, pref_confirm_data)
    sm.putPreference(mpm.PREF_CONFIRM_REMOVE_LAYERS, pref_confirm_layers)
    sm.putPreference(mpm.PREF_CONFIRM_REMOVE_BOTH, pref_confirm_both)
    sm.writePreferences()
    
    display = activeDisplay()
    
    if height != -1 and width != -1:
        display.setSize(width, height)
        
    return display  # TODO: return list of all displays instead
    
def writeImageAtIndex(fname, idx, params='', quality=1.0):
    """Captures a particular animation step from the active display.
    
    Args:
        fname: Filename for the captured image.
        
        idx: Zero-based index of the desired animation step.
        
        params: IDV ISL directives. Default value is "".
        
        quality: Image quality (for formats like JPEG). Values may be 
        between 0.0 and 1.0. Default value is 1.0.
        
    Returns:
        Nothing.
    """
    from ucar.unidata.idv.ui import ImageGenerator
    isl = ImageGenerator.makeXmlFromString(params)
    xml = '<image file="%s" animation_index="%s" quality="%s">%s</image>' % \
        (fname, idx, quality, isl)
    elem = islInterpreter.makeElement(xml)
    macros = islInterpreter.applyMacros(fname)
    islInterpreter.captureImage(macros, elem)


def loadFile(filename=None, field=None, level=None,
        time=None, xStride=1, yStride=1, 
        xRange=None, yRange=None, latLonBounds=None, **kwargs): 
    """method for loading anything handled by the netCDF-java library
       (netCDF, HDF, GRIB...)

    Args:
        filename
        field
        level
        time
        xStride
        yStride
        xRange
        yRange
        latLonBounds

    Returns:
        _MappedGeoGridFlatField with the requested data
    """
    from ucar.nc2.dt.grid import GridDataset
    from ucar.unidata.data import DataUtil
    from ucar.unidata.data.grid import GridUtil
    from ucar.unidata.data.grid import GeoGridAdapter
    from ucar.unidata.geoloc import LatLonPointImpl
    from ucar.unidata.geoloc import LatLonRect
    from ucar.ma2 import Range
    from ucar.visad import Util
    from visad import DateTime

    if (xStride < 1) or (yStride < 1):
        raise ValueError("xStride and yStride must be 1 or greater")

    dataType = 'Grid files (netCDF/GRIB/OPeNDAP/GEMPAK)'

    if not filename:
        raise ValueError('no filename provided')

    if not os.path.isfile(filename):
        raise ValueError('filename does not exist or is a directory: ' + filename)

    dataSource = createDataSource(filename, dataType)
    gridDataset = GridDataset.open(filename)

    if field:
        geogrid = gridDataset.findGridByName(field)
        if not geogrid:
            raise ValueError('Failed to create geogrid.  Make sure the field you specified exists in the file (use loadFileListFieldsInFile): ' + field)
    else:
        raise ValueError('no field name provided')
    
    levelReal = None
    if level:
        # TODO: type checking on level
        # expecting string specifying value and units, e.g. "1000 hPa"
        levelWanted = Util.toReal(level)
        levels = geogrid.getLevels()
        for i, levelToTest in enumerate(levels):
            levelString = '%s %s' % (levelToTest.getName(), levelToTest.getDescription())
            curLevelReal = Util.toReal(levelString)
            # actually utilize visad comparison magic!
            if levelWanted == curLevelReal:
                # slice the desired level
                geogrid = geogrid.subset(None, Range(i, i), None, None)
                levelReal = curLevelReal
                break

    if time: 
        if isinstance(time, str):
            foundTime = False
            # make a visad DateTime out of the string, and compare to the 
            # times in the file by creating visad DateTime's out of those.
            dateTime = DateTime.createDateTime(time)
            dateTimesInFile = DataUtil.makeDateTimes(geogrid.getCoordinateSystem().getTimeAxis1D())
            for i, timeInFile in enumerate(dateTimesInFile):
                if dateTime.equals(timeInFile):
                    geogrid = geogrid.subset(Range(i, i), None, None, None)
                    foundTime = True
                    break
            if not foundTime:
                raise ValueError('Could not find time specified: %s' % time)
        else:
            # assume time is integer specifying time step index
            geogrid = geogrid.subset(Range(time, time), None, None, None)
    else:
        # default to first time step...
        geogrid = geogrid.subset(Range(0, 0), None, None, None)

    if xRange or yRange: 
        if xRange:
            xRange = Range(xRange[0], xRange[1])
        if yRange:
            yRange = Range(yRange[0], yRange[1])
        geogrid = geogrid.subset(None, None, yRange, xRange)

    if latLonBounds:
        # TODO: type checking
        a = latLonBounds
        left = LatLonPointImpl(a[0], a[1]) # lat, lon
        right = LatLonPointImpl(a[2], a[3]) # lat, lon
        latLonBounds = LatLonRect(left, right)

    # TODO: type checking
    # TODO: if inputs == 1, eliminate this call?
    geogrid = geogrid.subset(None, None, latLonBounds, 1, yStride, xStride)

    adapter = GeoGridAdapter(dataSource.getJavaInstance(), geogrid)

    adapterData = adapter.getData()
    if (GridUtil.isTimeSequence(adapterData)):
        ff = adapterData.getSample(0)
    else:
        ff = adapterData

    # make the 'mega-object'
    mapped = _MappedGeoGridFlatField(ff, geogrid, filename, field)
    mapped.levelReal = levelReal

    return mapped

def makeFlatFieldSequence(sequence):
    """turn a list of _MappedGeoGridFlatField's into a FieldImpl with time
       domain that is suitable for displaying. 
       For now this is "dumb" ... assume input is:
       - in temporal order
       - a list of _MappedGeoGridFlatField's read from loadFile
    """
    from ucar.unidata.data import DataUtil
    from ucar.visad import Util
    from visad import FunctionType
    from visad import RealType
    from visad import DateTime
    dateTimes = []
    try:
        for ff in sequence:
            if ff.geogrid.getCoordinateSystem().hasTimeAxis1D():
                timeAxis = ff.geogrid.getCoordinateSystem().getTimeAxis1D()
                dateTimes.append(DataUtil.makeDateTimes(timeAxis)[0])
            else:
                # fix for ABI data / data with no time coord: just return plain FF
                # this will allow data to get displayed, but w/o time info
                return ff
    except AttributeError:
        # no geogrid ... try to read from getMetadataMap
        if sequence[0].getMetadataMap().get('times'):
            # this was a _MappedGeoGridFlatField
            for ff in sequence:
                # should be a visad.DateTime:
                timeStr = ff.getMetadataMap().get('times')[0].toString() 
                dateTimes.append(DateTime.createDateTime(timeStr))
    timeSet = Util.makeTimeSet(dateTimes)
    ftype = FunctionType(RealType.Time, ff.getType())
    fi = FieldImpl(ftype, timeSet)
    for i, ff in enumerate(sequence):
        fi.setSample(i, ff)
    return fi

def loadFileListFieldsInFile(filename):
    from ucar.nc2.dt.grid import GridDataset
    gridDataset = GridDataset.open(filename)
    for grid in gridDataset.getGrids():
        print '%s ; %s' % (grid.getName(), grid.getDescription())

def loadFileListLevelsInField(filename, field):
    from ucar.nc2.dt.grid import GridDataset
    gridDataset = GridDataset.open(filename)
    geogrid = gridDataset.findGridByName(field)
    for level in geogrid.getLevels():
        print '%s %s' % (level.getName(), level.getDescription())

def loadFileListTimesInField(filename, field):
    from ucar.nc2.dt.grid import GridDataset
    gridDataset = GridDataset.open(filename)
    geogrid = gridDataset.findGridByName(field)
    for time in geogrid.getTimes():
        print time
