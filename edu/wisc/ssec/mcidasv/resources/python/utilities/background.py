import ucar.unidata.util.Range

from java.lang import System
from ucar.unidata.ui.colortable import ColorTableDefaults

class _JavaProxy(object):
    def __init__(self, javaObject):
        self.__javaObject = javaObject
        self.__initialized = True
    
    def getJavaInstance(self):
        return self.__javaObject
    
    def __getattr__(self, attr):
        if not self.__dict__.has_key('_JavaProxy__initialized'):
                raise AttributeError(attr)
        else:
            if hasattr(self.__javaObject, attr):
                return getattr(self.__javaObject, attr)
            else:
                raise AttributeError(attr)
    
    def __setattr__(self, attr, val):
        if not self.__dict__.has_key('_JavaProxy__initialized'):
            self.__dict__[attr] = val
            return
        
        if hasattr(self.__javaObject, attr):
            setattr(self.__javaObject, attr, val)
        else:
            self.__dict__[attr] = val

class _ColorTable(_JavaProxy):
    def __init__(self, javaObject):
        _JavaProxy.__init__(self, javaObject)

class _Display(_JavaProxy):
    def __init__(self, javaObject):
        _JavaProxy.__init__(self, javaObject)
    
    def center(self, latitude, longitude, scale=1.0):
        earthLocation = Util.makeEarthLocation(latitude, longitude)
        mapDisplay = self.__javaObj.getMapDisplay()
        
        # no idea what the problem is here...
        mapDisplay.centerAndZoom(earthLocation, False, scale)
        mapDisplay.centerAndZoom(earthLocation, False, scale)
        mapDisplay.centerAndZoom(earthLocation, False, scale)
    
    def getLayer(self, index):
        return _Layer(self.__javaObj.getControls()[index])
    
    def getLayers(self):
        return [_Layer(displayControl) for displayControl in self.__javaObj.getControls()]

class _Layer(_JavaProxy):
    def __init__(self, javaObj):
        _JavaProxy.__init__(self, javaObject)

class _Projection(_JavaProxy):
    def __init__(self, javaObject):
        _JavaProxy.__init__(self, javaObject)

def setViewSize(width=0, height=0):
    """Set the view size to a given width and height."""
    idv.getStateManager().setViewSize(java.awt.Dimension(width, height))

def getColorTable(name=ColorTableDefaults.NAME_DEFAULT):
    colorTable = idv.getColorTableManager().getColorTable(name)
    if colorTable:
        return _ColorTable(colorTable)
    else:
        raise LookupError("Couldn't find a ColorTable named ", name, "; try calling 'colorTableNames()' to get the available ColorTables.")

def colorTableNames():
    """Returns a list of the valid color table names."""
    return [colorTable.getName() for colorTable in idv.getColorTableManager().getColorTables()]

def allColorTables():
    """Returns a list of the available color tables"""
    return [_ColorTable(colorTable) for colorTable in idv.getColorTableManager().getColorTables()]

def firstDisplay():
    """Returns the first display"""
    return _Display(idv.getVMManager().getViewManagers().get(0))

def allDisplays():
    """Returns a list of all displays"""
    return [_Display(viewManager) for viewManager in idv.getVMManager().getViewManagers()]

def createLayer(layerType, data, dataParameter='Data'):
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
    if not name:
        return _Projection(idv.getIdvProjectionManager().getDefaultProjection())
    
    for projection in idv.getIdvProjectionManager().getProjections():
        if name == projection.getName():
            return _Projection(projection)
    else:
        raise LookupError("Couldn't find a projection named ", name, "; try calling 'projectionNames()' to get the available projection names.")

def load_enhancement(name=''):
    pass

def load_map(name=''):
    pass

def annotate(text=''):
    pass

def apply_colorbar(name=''):
    pass

def write_image(path=''):
    pass

def collect_garbage():
    System.gc()


