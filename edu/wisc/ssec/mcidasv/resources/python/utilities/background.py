import ucar.unidata.util.Range

from java.lang import System
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

class _ColorTable(_JavaProxy):
    def __init__(self, javaObject):
        """Creates a proxy for ucar.unidata.util.ColorTable objects."""
        _JavaProxy.__init__(self, javaObject)

class _Display(_JavaProxy):
    def __init__(self, javaObject):
        """ """
        _JavaProxy.__init__(self, javaObject)
    
    def center(self, latitude, longitude, scale=1.0):
        """ """
        earthLocation = Util.makeEarthLocation(latitude, longitude)
        mapDisplay = self._JavaProxy__javaObject.getMapDisplay()
        
        # no idea what the problem is here...
        mapDisplay.centerAndZoom(earthLocation, False, scale)
        mapDisplay.centerAndZoom(earthLocation, False, scale)
        mapDisplay.centerAndZoom(earthLocation, False, scale)
    
    def getLayer(self, index):
        """Returns the layer at the given index (zero-based!) for this Display"""
        return _Layer(self._JavaProxy__javaObject.getControls()[index])
    
    def getLayers(self):
        """Returns a list of all layers used by this Display."""
        return [_Layer(displayControl) for displayControl in self._JavaProxy__javaObject.getControls()]

class _Layer(_JavaProxy):
    def __init__(self, javaObject):
        """Creates a proxy for ucar.unidata.idv.DisplayControl objects."""
        _JavaProxy.__init__(self, javaObject)

class _Projection(_JavaProxy):
    def __init__(self, javaObject):
        """Creates a proxy for ucar.unidata.geoloc.Projection objects."""
        _JavaProxy.__init__(self, javaObject)

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
        LookupError: If there was no Project with the given name.
    """
    if not name:
        return _Projection(idv.getIdvProjectionManager().getDefaultProjection())
    
    for projection in idv.getIdvProjectionManager().getProjections():
        if name == projection.getName():
            return _Projection(projection)
    else:
        raise LookupError("Couldn't find a projection named ", name, "; try calling 'projectionNames()' to get the available projection names.")

def load_enhancement(name=''):
    """Nothing yet."""
    pass

def load_map(name=''):
    """Nothing yet."""
    pass

def annotate(text=''):
    """Nothing yet."""
    pass

def apply_colorbar(name=''):
    """Nothing yet."""
    pass

def write_image(path=''):
    """Nothing yet."""
    pass

def collect_garbage():
    """Signals to Java that it should free any memory that isn't in use."""
    System.gc()


