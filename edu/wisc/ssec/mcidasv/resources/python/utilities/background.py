import types

import java.awt.Color.CYAN
import ucar.unidata.util.Range

from contextlib import contextmanager

# from shell import makeDataSource

from org.slf4j import Logger
from org.slf4j import LoggerFactory

from java.lang import System
from edu.wisc.ssec.mcidasv.McIDASV import getStaticMcv
from ucar.unidata.idv import DisplayInfo
from ucar.unidata.idv.ui import IdvWindow
from ucar.unidata.geoloc import LatLonPointImpl
from ucar.unidata.ui.colortable import ColorTableDefaults

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
        
        if not self.__dict__.has_key('_JavaProxy__initialized'):
            raise AttributeError(attr)
        else:
            if hasattr(self.__javaObject, attr):
                return getattr(self.__javaObject, attr)
            else:
                raise AttributeError(attr)

    def __setattr__(self, attr, val):
        """Forwards object attribute changes to the internal VisAD/IDV/McIDAS-V object."""
        
        if not self.__dict__.has_key('_JavaProxy__initialized'):
            self.__dict__[attr] = val
            return

        if hasattr(self.__javaObject, attr):
            setattr(self.__javaObject, attr, val)
        else:
            self.__dict__[attr] = val

class _Window(_JavaProxy):
    def __init__(self, javaObject):
        """Blank for now. javaObject = IdvWindow
           tab
        """
        
        _JavaProxy.__init__(self, javaObject)

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
    def getCurrentTab(self):
        """Returns the currently active tab."""

        # mcv windows should only have one component group
        return _Tab(self._JavaProxy__javaObject.getComponentGroups()[0].getActiveComponentHolder())

    def getTabAtIndex(self, index):
        """Returns the tab at the given index."""
        
        return _Tab(self._JavaProxy__javaObject.getComponentGroups()[0].getHolderAt(index))

    def getTabCount(self):
        """Returns the number of tabs."""

        return self._JavaProxy__javaObject.getComponentGroups()[0].getDisplayComponentCount()

    def getTabs(self):
        """Returns a list of the available tabs."""

        return [_Tab(holder) for holder in self._JavaProxy__javaObject.getComponentGroups()[0].getDisplayComponents()]

    def getSize(self):
        """Returns the width and height of the wrapped IdvWindow."""

        dims = self._JavaProxy__javaObject.getSize()
        return dims.getWidth(), dims.getHeight()

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

    def getName(self):
        """Returns the name of this tab."""
        return self._JavaProxy__javaObject.getName()

    def setName(self, newTabName):
        """Set this tab's name to a given string value."""
        self._JavaProxy__javaObject.setName(newTabName)

    def getDisplays(self):
        """Returns a list of the displays contained within this tab."""
        return [_Display(viewManager) for viewManager in self._JavaProxy__javaObject.getViewManagers()]

class _Display(_JavaProxy):
    def __init__(self, javaObject):
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

    def setDimensions(self, x, y, width, height):
        from java.awt import Rectangle
        self._JavaProxy__javaObject.setDisplayBounds(Rectangle(x, y, width, height))

    def getDimensions(self):
        from java.awt import Rectangle
        rect = self._JavaProxy__javaObject.getDisplayBounds()
        return rect.x, rect.y, rect.width, rect.height

    def getDataAtLocation(self, latitude, longitude):
        #earthLocation = Util.makeEarthLocation(latitude, longitude)
        #for layer in self._JavaProxy__javaObject.getControls():
        pass

    def getDataSources(self):
        pass

    def getProjection(self):
        """Returns the map projection currently in use."""
        
        return _Projection(self._JavaProxy__javaObject.getMapDisplay().getMapProjection())

    # TODO(jon): still deciding on a decent way to refer to an arbitrary projection...
    #def setProjection(self, projection):
    #    pass

    def resetProjection(self):
        return self._JavaProxy__javaObject.getMapDisplay().resetProjection()

    def getVerticaleScaleUnit(self):
        return self._JavaProxy__javaObject.getMapDisplay().getVerticalRangeUnit()

    def getVerticalScaleRange(self):
        verticalRange = self._JavaProxy__javaObject.getMapDisplay().getVerticalRange()
        return verticalRange[0], verticalRange[1]

    def getMaps(self):
        """Returns a dictionary of maps and their status for the display."""
        
        # dict of mapName->boolean (describes if a map is enabled or not.)
        # this might fail for transect displays....
        mapLayer = self._JavaProxy__javaObject.getControls()[0]
        mapStates = {}
        for mapState in mapLayer.getMapStates():
            mapStates[mapState.getSource()] = mapState.getVisible()
        return mapStates

    def setMaps(self, mapStates):
        """Allows for controlling the visibility of all available maps for
        the display.
        """
        
        mapLayer = self._JavaProxy__javaObject.getControls()[0]
        for currentState in mapLayer.getMapStates():
            mapSource = currentState.getSource()
            if mapSource in mapStates:
                currentState.setVisible(mapStates[mapSource])

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

    def setScaleFactor(self, scale):
        """ """
        
        self._JavaProxy__javaObject.getMapDisplay().zoom(scale)

    def getScaleFactor(self):
        return self._JavaProxy__javaObject.getMapDisplay().getScale()

    def center(self, latitude, longitude, scale=1.0):
        self.setCenter(latitude, longitude)
        self.setScaleFactor(scale)

    def setCenter(self, latitude, longitude):
        """Centers the display over a given latitude and longitude.

        Please be aware that something like:
        setCenter(lat, long, 1.2)
        setCenter(lat, long, 1.2)
        the second call will rescale the display to be 1.2 times the size of
        the display *after the first call.* Or, those calls are essentially
        the same as "setCenter(lat, long, 2.4)".

        Args:
        latitude:
        longitude:
        scale: Optional parameter for "zooming". Default value (1.0) results in no rescaling; less than 1.0 "zooms out", while greater than 1.0 "zooms in."
        """
        
        # source and dest are arbitrary rectangles.
        # float scaleX = dest.width / source.width;
        # float scaleY = dest.height / source.height;
        # Point sourceCenter = centerPointOfRect(source);
        # Point destCenter = centerPointOfRect(dest);
        # glTranslatef(destCenter.x, destCenter.y, 0.0);
        # glScalef(scaleX, scaleY, 0.0);
        # glTranslatef(sourceCenter.x * -1.0, sourceCenter.y * -1.0, 0.0);
        validated = LatLonPointImpl(latitude, longitude)
        earthLocation = Util.makeEarthLocation(validated.getLatitude(), validated.getLongitude())
        mapDisplay = self._JavaProxy__javaObject.getMapDisplay()

        # no idea what the problem is here...
        mapDisplay.centerAndZoom(earthLocation, False, scale)
        # try to position correctly
        mapDisplay.centerAndZoom(earthLocation, False, 1.0)
        mapDisplay.centerAndZoom(earthLocation, False, 1.0)


    def getBackgroundColor(self):
        """Returns the Java AWT color object of the background color (or None)."""
        
        return self._JavaProxy__javaObject.getMapDisplay().getBackground()

    def setBackgroundColor(self, color=java.awt.Color.CYAN):
        """Sets the display's background color to the given AWT color. Defaults to cyan."""
        
        self._JavaProxy__javaObject.getMapDisplay().setBackground(color)

#    def addLayer(self, newLayer):
#        """Adds a new display layer (display control) to the end of this display's layer list."""
#        self._JavaProxy__javaObject.addDisplayInfo(DisplayInfo(_))

#    def getMapLayer(self):
#        # the map layer will typically be the first layer... still buggy :(
#        return self._JavaProxy__javaObject.getControls()[0]

    def getLayer(self, index):
        """Returns the layer at the given index (zero-based!) for this Display"""
        
        return _Layer(self._JavaProxy__javaObject.getControls()[index])

    def getLayers(self):
        """Returns a list of all layers used by this Display."""
        
        return [_Layer(displayControl) for displayControl in self._JavaProxy__javaObject.getControls()]

# TODO(jon): still not sure what to offer here.
class _Layer(_JavaProxy):
    def __init__(self, javaObject):
        """Creates a proxy for ucar.unidata.idv.DisplayControl objects."""
        
        _JavaProxy.__init__(self, javaObject).addDisplayInfo()

    def getFrameCount(self):
        # looking like ucar.visad.display.AnimationWidget is the place to be
        pass

    def getFrameDataAtLocation(self, latitude, longitude, frame):
        # just return the value
        pass

    def getDataAtLocation(self, latitude, longitude):
        # should return a dict of timestamp: value ??
        pass


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

def setViewSize(width, height):
    """Set the view size to a given width and height.

    Longer description goes here.

    Args:
        width:
        height:
    """
    getStaticMcv().getStateManager().setViewSize(java.awt.Dimension(width, height))

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

def colorTableNames():
    """Returns a list of the valid color table names."""
    return [colorTable.getName() for colorTable in getStaticMcv().getColorTableManager().getColorTables()]

def allColorTables():
    """Returns a list of the available color tables."""
    return [_ColorTable(colorTable) for colorTable in getStaticMcv().getColorTableManager().getColorTables()]

def firstWindow():
    return _Window(IdvWindow.getMainWindows()[0])

def allWindows():
    return [_Window(window) for window in IdvWindow.getMainWindows()]

def firstDisplay():
    """Returns the first display

    Longer description goes here.

    Returns:
         The first Display (aka ViewManager).

    Raises:
        IndexError: If there are no Displays.
    """
    return _Display(getStaticMcv().getVMManager().getViewManagers().get(0))

def allDisplays():
    """Returns a list of all McIDAS-V displays (aka ViewManagers)"""
    return [_Display(viewManager) for viewManager in getStaticMcv().getVMManager().getViewManagers()]

def activeDisplay():
    """Returns the active McIDAS-V display."""
    return _Display(getStaticMcv().getVMManager().getLastActiveViewManager())


def createLayer(layerType, data, dataParameter='Data'):
    """Creates a new Layer in the active Display.

    Args:
        layerType: ID string that represents a type of layer. The valid names
                   can be determined with the "allLayerTypes()" function.

        data: Data object to associate with the resulting layer.

        dataParameter: Optional...

    Returns:
        The Layer that was created in the active display.
    """
    # TODO(jon): this should behave better if createDisplay fails for some reason.
    return _Layer(createDisplay(layerType, data, dataParameter))

def allLayerTypes():
    """Returns a list of the available layer type names"""
    return getStaticMcv().getAllControlDescriptors()

def allProjections():
    """Returns a list of the available projections."""
    return [_Projection(projection) for projection in getStaticMcv().getIdvProjectionManager().getProjections()]

def projectionNames():
    """Returns a list of the available projection names"""
    return [projection.getName() for projection in getStaticMcv().getIdvProjectionManager().getProjections()]

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

def allActions():
    """Returns the available McIDAS-V action identifiers."""
    actions = getStaticMcv().getIdvUIManager().getCachedActions().getAllActions()
    return [action.getId() for action in actions]

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

def removeAllData():
    """Removes all of the current data sources WITHOUT prompting."""
    getStaticMcv().removeAllData(False)

def removeAllLayers():
    """Removes all of the current layers WITHOUT prompting."""
    getStaticMcv().removeAllLayers(False)

def boomstick():
    """ This is [your] BOOOMSTICK! """
    mcv = getStaticMcv()
    mcv.removeAllLayers(False)
    mcv.removeAllData(False)
    System.gc()

class _NoOp(object):
    def __init__(self, description='anything'):
        self.description = description
    def __repr__(self):
        return self.description
        
MAP = _NoOp('MAP')
FLATMAP = _NoOp('FLATMAP')
GLOBE = _NoOp('GLOBE')
TRANSECT = _NoOp('TRANSECT')

def buildWindow(width=0, height=0, rows=1, cols=1, panels=None):
    if panels is None:
        panels = [MAP] * (rows * cols)
    elif isinstance(panels, _NoOp):
        panels = [panels] * (rows * cols)
    elif type(panels) is types.ListType:
        if len(panels) != (rows*cols):
            raise ValueError('panels needs to contain rows*cols elements')
    
    from edu.wisc.ssec.mcidasv import PersistenceManager
    
    print 'creating window: width=%d height=%d rows=%d cols=%d panels=%s' % (width, height, rows, cols, panels)
    
    # window = getStaticMcv().getIdvUIManager().buildEmptyWindow()
    # return PersistenceManager.buildDynamicSkin(window, rows, cols, panels)
    return PersistenceManager.buildDynamicSkin2(rows, cols, panels)

def buildDisplayWindow(title, width=0, height=0):
    """Creates a window using the default McIDAS-V display skin.
    
    Default skin is currently "/edu/wisc/ssec/mcidasv/resources/skins/window/map/onemapview.xml"; 
    
    Args:
        title: Name to give to the window.
        
        width: Sets the window to this width (in pixels). Values less than or 
               equal to zero are considered default values.
               
        height: Sets the window to this height (in pixels). Values less than 
                or equal to zero are considered default values.
    
    Returns:
        A "wrapped" IdvWindow.
    """
    # DEFAULT_SKIN_PATH = '/edu/wisc/ssec/mcidasv/resources/skins/window/map/onemapview.xml'
    from java.awt import Dimension
    mcv = getStaticMcv()
    window = mcv.getIdvUIManager().buildDefaultSkin()
    window.setTitle(title)
    if width > 0 and height > 0:
        window.setSize(Dimension(width, height))
    return _Window(window)

def makeLogger(name):
    """ """
    return  LoggerFactory.getLogger(name)

def openBundle(bundle, label="", clear=1):
    """Open a bundle using the decodeXmlFile from PersistenceManager

    Args:
        bundle: location of bundle to be loaded

        label: Label for bundle?  where is this displayed?

        clear: whether to clear current layers and data (1 or 0)
        Default is to clear.

    Returns:
        Nothing for now.. maybe return activeDisplay()  ?

    Raises:
        ValueError: if bundle doesn't exist
    """
    from edu.wisc.ssec.mcidasv import McIdasPreferenceManager 
    from edu.wisc.ssec.mcidasv import PersistenceManager

    my_mcv = getStaticMcv()
    sm = my_mcv.getStateManager()
    mpm = McIdasPreferenceManager # for some of the PREF constants

    # Allows user to specify file with for example, ~/bundlefile.mcv
    bundle = _expandpath(bundle)

    fileExists = os.path.exists(bundle)
    isDir = os.path.isdir(bundle)


    if (not fileExists) or isDir:
        raise ValueError("File does not exist or is a directory")

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

    # set relevant preferences to values that make sense for non-GUI mode
    sm.putPreference(my_mcv.PREF_ZIDV_ASK, False)
    sm.putPreference(my_mcv.PREF_OPEN_ASK, False)
    # For REMOVE and MERGE, we want to do the same thing as what McIdasPreferenceManager 
    # does for "Replace Session" (set both to true)
    sm.putPreference(my_mcv.PREF_OPEN_REMOVE, True)
    sm.putPreference(my_mcv.PREF_OPEN_MERGE, True)
    sm.putPreference(my_mcv.PREF_ZIDV_SAVETOTMP, True)
    sm.putPreference(mpm.PREF_CONFIRM_REMOVE_DATA, False)
    sm.putPreference(mpm.PREF_CONFIRM_REMOVE_LAYERS, False)
    sm.putPreference(mpm.PREF_CONFIRM_REMOVE_BOTH, False)
    # ZIDV_DIRECTORY should come from keyword
    # (also need to check for existence of this directory, etc.)
    #my_mcv.getStore().put(my_mcv.PREF_ZIDV_DIRECTORY, something??)
    sm.writePreferences()

    pm = my_mcv.getPersistenceManager()
    checkToRemove = clear
    letUserChangeData = 0    # not sure about this
    bundleProperties = None  # not sure what this does..just send it None for now
    pm.decodeXmlFile(bundle,label,checkToRemove,letUserChangeData,bundleProperties)

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

# server = ADDE server
# dataset = ADDE dataset
# day = date of image
# time = time of image
# coordinateType = coordinate system to use
#   AREA
#   LAT/LON
#   Image
# x-coordinate = AREA/Image Line or Latitude
# y-coordinate = AREA/Image Element or Longitude
# position = location of specified coordinate
#   Center
#   Upper-Left
#   Lower-Right
# unit
# navigationType = navigation type used
#   Image
#   LALO
# channel = type and value to display
#   waveLength wavelength
#   waveNumber wavenumber
#   band bandnumber
# relativePosition = relative position number (0, -1, -2)
# numberImages = number of images to load
def getADDEImage(**kwargs):
    if 'server' in kwargs:
        server = kwargs['server']
    else:
        raise TypeError('must provide a server parameter value')
    
    if 'dataset' in kwargs:
        dataset = kwargs['dataset']
    else:
        raise TypeError('must provide a dataset parameter value')
