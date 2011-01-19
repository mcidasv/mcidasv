import java.awt.Color.CYAN
import ucar.unidata.util.Range

from java.lang import System
from ucar.unidata.idv import DisplayInfo
from ucar.unidata.ui.colortable import ColorTableDefaults

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
        JavaProxy.__init__(self, javaObject)
    
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

class _Tab(_JavaProxy):
    def __init__(self, javaObject):
        """Blank for now. javaObject = McvComponentHolder
        """
        JavaProxy.__init__(self, javaObject)
    
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
    
    def getDataSources(self):
        pass
    
    def getProjection(self):
        """Returns the map projection currently in use."""
        return _Projection(self._JavaProxy__javaObject.getMapDisplay().getMapProjection())
    
    # TODO(jon): still deciding on a decent way to refer to an arbitrary projection...
    #def setProjection(self, projection):
    #    pass
    
    def getVerticalScaleRange(self):
        verticalRange = self._JavaProxy__javaObject.getMapDisplay().getVerticalRange()
        return verticalRange[0], verticalRange[1]
    
    #def getVerticalScaleUnits(self):
    #    pass
    
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
        the display."""
        mapLayer = self._JavaProxy__javaObject.getControls()[0]
        for currentState in mapLayer.getMapStates():
            mapSource = currentState.getSource()
            if mapSource in mapStates:
                currentState.setVisible(mapStates[mapSource])
    
    def center(self, latitude, longitude, scale=1.0):
        """Centers the display over a given latitude and longitude."""
        earthLocation = Util.makeEarthLocation(latitude, longitude)
        mapDisplay = self._JavaProxy__javaObject.getMapDisplay()
        
        # no idea what the problem is here...
        mapDisplay.centerAndZoom(earthLocation, False, scale)
        mapDisplay.centerAndZoom(earthLocation, False, scale)
        mapDisplay.centerAndZoom(earthLocation, False, scale)
    
    def backgroundColor(self, color=java.awt.Color.CYAN):
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
    idv.getStateManager().setViewSize(java.awt.Dimension(width, height))

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
    colorTable = idv.getColorTableManager().getColorTable(name)
    if colorTable:
        return _ColorTable(colorTable)
    else:
        raise LookupError("Couldn't find a ColorTable named ", name, "; try calling 'colorTableNames()' to get the available ColorTables.")

def colorTableNames():
    """Returns a list of the valid color table names."""
    return [colorTable.getName() for colorTable in idv.getColorTableManager().getColorTables()]

def allColorTables():
    """Returns a list of the available color tables."""
    return [_ColorTable(colorTable) for colorTable in idv.getColorTableManager().getColorTables()]

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
    return _Display(idv.getVMManager().getViewManagers().get(0))

def allDisplays():
    """Returns a list of all McIDAS-V displays (aka ViewManagers)"""
    return [_Display(viewManager) for viewManager in idv.getVMManager().getViewManagers()]

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
    return idv.getAllControlDescriptors()

def allProjections():
    """Returns a list of the available projections."""
    return [_Projection(projection) for projection in idv.getIdvProjectionManager().getProjections()]

def projectionNames():
    """Returns a list of the available projection names"""
    return [projection.getName() for projection in idv.getIdvProjectionManager().getProjections()]

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
    if not name:
        return _Projection(idv.getIdvProjectionManager().getDefaultProjection())
    
    for projection in idv.getIdvProjectionManager().getProjections():
        if name == projection.getName():
            return _Projection(projection)
    else:
        raise ValueError("Couldn't find a projection named ", name, "; try calling 'projectionNames()' to get the available projection names.")

# TODO(jon): remove pending jython meeting decision?
def load_enhancement(name=''):
    """Nothing yet."""
    pass

# TODO(jon): remove pending jython meeting decision?
def load_map(name=''):
    """Nothing yet."""
    pass

# TODO(jon): remove pending jython meeting decision?
def annotate(text=''):
    """Nothing yet."""
    pass

# TODO(jon): remove pending jython meeting decision?
def apply_colorbar(name=''):
    """Nothing yet."""
    pass

# TODO(jon): remove pending jython meeting decision?
def write_image(path=''):
    """Nothing yet."""
    pass

# TODO(jon): remove pending jython meeting decision?
def collect_garbage():
    """Signals to Java that it should free any memory that isn't in use."""
    System.gc()


