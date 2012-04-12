mport types

import java.awt.Color.CYAN
import ucar.unidata.util.Range

from contextlib import contextmanager

# from shell import makeDataSource
from decorators import deprecated

from org.slf4j import Logger
from org.slf4j import LoggerFactory

from java.awt import Dimension
from java.awt import Rectangle
from java.lang import NullPointerException
from java.lang import System
from edu.wisc.ssec.mcidasv.McIDASV import getStaticMcv
from ucar.unidata.idv import DisplayInfo
from ucar.unidata.idv.ui import IdvWindow
from ucar.unidata.geoloc import LatLonPointImpl
from ucar.unidata.ui.colortable import ColorTableDefaults
from ucar.unidata.util import GuiUtils
from ucar.visad import Util

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

def _getNewFont(currentFont, fontName, style, size):
    """Helper class for setLayerLabelFont and setColorScaleFont
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
    else:
        style = currentFont.getStyle()

    if size == None:
        size = currentFont.getSize()
    else:
        size = int(size)

    if fontName != None:
        # check if fontName is valid
        fontList = ucar.unidata.util.GuiUtils.getFontList()
        foundFont = False
        for availableFont in fontList:
            if availableFont.toString().lower() == fontName.lower():
                # need to get properly capitalized font string!
                fontName = availableFont.toString()
                foundFont = True
        if foundFont == False:
            # if fontName is STILL None, then user provided an invalid font name
            raise ValueError("Could not find the following fontName:", fontName, "call allFontNames for valid options")
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

    def toggleFullScreen(self):
        self._JavaProxy__javaObject.toggleFullScreen()

    def getFullScreenSize(self):
        width = self._JavaProxy__javaObject.getFullScreenWidth()
        height = self._JavaProxy__javaObject.getFullScreenHeight()
        return width, height
    
    def setFullScreenSize(self, width, height):
        self._JavaProxy__javaObject.setFullScreenWidth(width)
        self._JavaProxy__javaObject.setFullScreenHeight(height)
    
    def getSize(self):
        size = self._JavaProxy__javaObject.getComponent().getSize()
        return size.getWidth(), size.getHeight()

    def setSize(self, width, height):
        size = Dimension(width, height)
        navigatedComponent = self._JavaProxy__javaObject.getComponent()
        navigatedComponent.setMinimumSize(size)
        navigatedComponent.setMaximumSize(size)
        navigatedComponent.setPreferredSize(size)
        window = GuiUtils.getWindow(navigatedComponent)
        if not window:
            from javax.swing import JFrame
            window = JFrame()
            window.getContentPane().add(navigatedComponent)

        window.pack()
        print 'new: %s\ncur: %s\nmin: %s\nmax: %s\nprf: %s' % (size, navigatedComponent.getSize(), navigatedComponent.getMinimumSize(), navigatedComponent.getMaximumSize(), navigatedComponent.getPreferredSize())

    # @deprecated(self.setSize)
    def setDimensions(self, x, y, width, height):
        self._JavaProxy__javaObject.setDisplayBounds(Rectangle(x, y, width, height))
        self.setSize(width, height)

    # @deprecated(self.getSize)
    def getDimensions(self):
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

    def setProjection(self, projection):
        """ Set the current projection
        
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

        if isinstance(projection, _Layer):
            projObj = projection._JavaProxy__javaObject.getDataProjection()
            return self._JavaProxy__javaObject.getMapDisplay().setMapProjection(projObj)
        
        if isinstance(projection, str):
            projObj = getProjection(projection)._JavaProxy__javaObject
            return self._JavaProxy__javaObject.getMapDisplay().setMapProjection(projObj)

        # if user does something like pass in an int
        raise ValueError('valid arguments to setProjection are (1) a string defining a valid' +
                          ' projection name, or (2) a _Layer object whose data' +
                          ' projection you want to use')
        
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
        #self.setScaleFactor(scale)

    def setCenter(self, latitude, longitude, scale=1.0):
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
        latitude:
        longitude:
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
        validated = LatLonPointImpl(latitude, longitude)
        earthLocation = Util.makeEarthLocation(validated.getLatitude(), validated.getLongitude())
        mapDisplay = self._JavaProxy__javaObject.getMapDisplay()

        #  accept scale keyword as argument.  Seems to be working now  --mike
        mapDisplay.centerAndZoom(earthLocation, False, scale)


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

    def createLayer(self, layerType, data, dataParameter='Data'):
        """Creates a new Layer in this _Display

        Args:
            layerType: ID string that represents a type of layer. The valid names
                       can be determined with the "allLayerTypes()" function.

            data: Data object to associate with the resulting layer.

            dataParameter: Optional...

        Returns:
            The _Layer that was created in this _Display

        Raises:
            ValueError:  if layerType isn't valid
        """
        import ucar.unidata.data.DataDataChoice as DataDataChoice
        import ucar.unidata.data.DataChoice as DataChoice
        import visad.meteorology.NavigatedImage as NavigatedImage

        if isinstance(data, _DataChoice):
            # get DataChoice Java object
            # (Note: createDisplay can handle both flatfile and DataChoice)
            data = data.getJavaInstance()

        # need to get short control description from long name
        mcv = getStaticMcv()
        controlID = None
        for desc in mcv.getControlDescriptors():
            if desc.label == layerType:
                controlID = desc.controlId
        if controlID == None:
            raise ValueError("You did not provide a valid layer type")

        # Set the panel/display that a new DisplayControl will be put into
        # TODO(mike):  set this back to what it was before?
        getStaticMcv().getVMManager().setLastActiveViewManager(self._JavaProxy__javaObject)

        if (isinstance(data, java.util.List)==0):
            tmp = java.util.ArrayList()
            # python lists should work also:
            if isinstance(data, list):
                # this is a python list
                for thing in data:
                    tmp.add(thing)
            else:
                # this will ensure we have a java ArrayList which will make things easier
                tmp.add(data)
            data = tmp

        # this is the guts of the old createDisplay
        dataList = java.util.ArrayList()
        for i in range(data.size()):
            obj = data.get(i)
            if(isinstance(obj, DataChoice)==0):
                label = dataParameter
                if(data.size()>1):
                    label = label +str(i)
                obj = DataDataChoice(label,obj)
            dataList.add(obj)

        # *temporary* fix for doing image sequences with getADDEImage:
        if isinstance(data[0], NavigatedImage) and (controlID == 'imagesequence'):
            # apparently we need a CompositeDataChoice instead of a list of DataDataChoices for
            # Image Sequence Display to work as expected
            # To construct a CompositeDataChoice, we need a legitimate DataSource:
            # (note most data sources keep track of files or URL's - ListDataSource's simply
            #  keep track of DataChoice's)
            listSource = ucar.unidata.data.ListDataSource('tempListDataSource1', 'tempListDataSource1')
            listSource.setDataChoices(dataList)
            # now we can create the CompositeDataChoice:
            comp = ucar.unidata.data.CompositeDataChoice(listSource, listSource, 'compositeDataChoice', None)
            comp.setDataChoices(dataList)
            # need *another* ListDataSource now:
            compSource = ucar.unidata.data.ListDataSource('tempListDataSource2', 'tempListDataSource2')
            compSource.addDataChoice(comp)
            # finally, set dataList appropriately:
            dataList = compSource.getDataChoices()

        newLayer = mcv.doMakeControl(controlID, dataList);

        # createDisplay does it's init in a thread,
        # so wait for it to finish before we hand control back to user!
        pause()

        # TODO(jon): this should behave better if createDisplay fails for some reason.
        return _Layer(newLayer)

    def captureImage(self, filename, quality=1.0):
        """Attempt at a replacement for ISL writeImage

        Args:
            filename
            quality:  float between 0.0 and 1.0 (relevant for JPEG's)
                    0.0 is highest compression / smallest file size / worst quality
                    1.0 is least compression / biggest file size / best qualit

        Raises: 
            ValueError:  if filename is a directory

        """

        # do some sanity checking on filename
        filename = _expandpath(filename)

        isDir = os.path.isdir(filename)

        if isDir:
            # this isn't really good enough.  could be permissions issue, etc.
            raise ValueError(filename, " is a directory")
        
        # the results aren't good if we don't pause first
        pause()

        imageFile = java.io.File(filename)
        # yes, I'm still calling writeImage. But it's a different writeImage!!!
        #  (this is ViewManager.writeImage, not ImageGenerator.writeImage)
        # (2nd arg has something to do with whether image gets written in current thread...)
        self._JavaProxy__javaObject.writeImage(imageFile, True, quality)

        # TODO(mike): catch exceptions resulting from writeImage (e.g., if filename has invalid extension)

# TODO(jon): still not sure what to offer here.
class _Layer(_JavaProxy):
    def __init__(self, javaObject):
        """Creates a proxy for ucar.unidata.idv.DisplayControl objects.
        
        (Mike says:) addDisplayInfo() doesn't seem  necessary here,
                     so I've removed it for the time being...
        """
        
        #_JavaProxy.__init__(self, javaObject).addDisplayInfo()
        _JavaProxy.__init__(self, javaObject)

    def getFrameCount(self):
        # looking like ucar.visad.display.AnimationWidget is the place to be
        pass

    def getFrameDataAtLocation(self, latitude, longitude, frame):
        # just return the value
        pass

    def getDataAtLocation(self, latitude, longitude):
        # should return a dict of timestamp: value ??
        pass

    def setEnhancement(self, name=None, range=None):
        """Wrapper for setEnhancementTable and setDataRange
        """
        if (name != None):  # leave as-is if not specified
            self.setEnhancementTable(name)

        # but 'range' is a Python built-in.........
        if (range != None):
            self.setDataRange(range[0], range[1])

    def getEnhancementTable(self):
        """Get the current enhancement table.
        
        Returns:
            The actual enhancement table object.
        """
        return self._JavaProxy__javaObject.getColorTable()

    def setEnhancementTable(self, ctName):
        """Change the enhancement table.

        Args:
            ctName:  the name of the enhancement table. Unlike setProjection,
                     you don't need to specify "parent" table directories
                     
        Returns: nothing
        """
        
        my_mcv = getStaticMcv()
        ctm = my_mcv.getColorTableManager()
        newct = ctm.getColorTable(ctName)
        return self._JavaProxy__javaObject.setColorTable(newct)

    def setDataRange(self, minRange, maxRange):
        """ Change the range of the displayed data (and enhancement table)

        Args:
            minRange: if min_range evaluates to false, leave as-is
            maxRange: if max_range evaluates to false, leave as-is

        Returns: nothing
        """

        currentRange = self._JavaProxy__javaObject.getRange()

        if (minRange != None):
            currentRange.setMin(minRange)

        if (maxRange != None):
            currentRange.setMax(maxRange)

        self._JavaProxy__javaObject.setRange(currentRange)

    def setColorScale(self, visible=True, placement=None, font=None, style=None, size=None, color=None):
        """Wrapper function for all the color scale manipulation stuff
        """
        # assume user wants color scale visible unless otherwise specified
        self.setColorScaleVisible(visible)

        if (placement != None):
            self.setColorScalePlacement(placement)

        if (font != None):  # let setColorScaleFont handle default
            self.setColorScaleFont(fontName=font)

        if (style != None):
            self.setColorScaleFont(style=style)

        if (size != None):  # let setColorScaleFont handle default
            self.setColorScaleFont(size=size)
        
        if (color != None):
            self.setColorScaleFontColor(color)

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
            style (optional): string containing either PLAIN (default: as-is), BOLD, or ITALIC
                                (case-insensitive)
            size (optional):  font size (default: as-is)

        Returns: nothing
        """

        info = self._JavaProxy__javaObject.getColorScaleInfo()

        currentFont = info.getLabelFont()
        newFont = _getNewFont(currentFont, fontName, style, size)

        info.setLabelFont(newFont)
        self._JavaProxy__javaObject.setColorScaleInfo(info)

    def setColorScaleFontColor(self, color):
        """Set color of color scale labels
        Args:
            color can be rgb list or tuple, or string giving name of a color

        I'm leaning toward keeping this separate from setColorScaleFont since
        it wraps around a different java method (setLabelColor)
        """
        import colorutils
        rgb = colorutils.convertColor(color)
        r = rgb[0].getConstant()
        g = rgb[1].getConstant()
        b = rgb[2].getConstant()
        newColor = java.awt.Color(r, g, b)
        
        info = self._JavaProxy__javaObject.getColorScaleInfo()
        info.setLabelColor(newColor)
        self._JavaProxy__javaObject.setColorScaleInfo(info)

    def setLayerVisible(self, status):
        """Set visibility of this layer

        Args:
            status:  boolean for visibility of layer
        """
        self._JavaProxy__javaObject.setDisplayVisibility(status)

    def getLayerLabel(self):
        """Returns the current layer label text.
        
        Returns:
            string containing a layer label.
        """
        return self._JavaProxy__javaObject.getDisplayListTemplate()

    def setLayerLabel(self, label=None, visible=True, font=None, style=None, size=None, color=None):
        """ Set the layer label (the string of text at the bottom of maps) and other
            properties of layer labels.  Confusingly and not helpful is that properties of layer labels
            are set per panel instead of per layer.  So really, this should be a function of _Display
            instead of _Layer...?

        (In Java-land, Layer Labels are "Display Lists")

        Args:
            label:  a string defining the layer label (default: as-is)
            (Note you can use macros like %displayname% here)

        Returns:  nothing
        """
        if (label != None) and isinstance(label, str):
            self._JavaProxy__javaObject.setDisplayListTemplate(label)

        self.setLayerLabelVisible(visible)

        if (font != None):
            self.setLayerLabelFont(fontName=font)

        if (style != None):
            self.setLayerLabelFont(style=style)

        if (size != None):  # let setColorScaleFont handle default
            self.setLayerLabelFont(size=size)
        
        if (color != None):
            self.setLayerLabelColor(color)

        self._JavaProxy__javaObject.getViewManager().updateDisplayList()

    def getLayerVisible(self):
        """Determine whether or not this layer is visible.
        
        Returns:
            True if visible, False otherwise.
        """
        return self._JavaProxy__javaObject.getDisplayVisibility()

    def setLayerLabelVisible(self, status):
        """Set whether the Display List is shown for this ViewManager

        Args:
            status:  True - visible or False - not visible

        Raises:
            ValueError: if status isn't a boolean
        """
        if isinstance(status, bool):
            self._JavaProxy__javaObject.getViewManager().setShowDisplayList(status)
        else:
            raise ValueError('parameter for setLayerLabelVisible must be boolean (either True or False')

    def setLayerLabelColor(self, color):
        """Set color of Display List labels (confusingly, these are per panel
            and not per layer).

        Args:
            color can be rgb list or tuple, or string giving name of a color
        """
        import colorutils
        rgb = colorutils.convertColor(color)
        r = rgb[0].getConstant()
        g = rgb[1].getConstant()
        b = rgb[2].getConstant()
        newColor = java.awt.Color(r, g, b)
        
        self._JavaProxy__javaObject.getViewManager().setDisplayListColor(newColor)

    def setLayerLabelFont(self, fontName=None, style=None, size=None):
        """ set the font of Display List

        Args:
            fontName (optional): string containing font name (default: leave as-is)
                                    (case-insensitive)
            style (optional): string containing either PLAIN (default: as-is), BOLD, or ITALIC
                                (case-insensitive)
            size (optional):  font size (default: as-is)
        """
        vm = self._JavaProxy__javaObject.getViewManager()
        currentFont = vm.getDisplayListFont()
        newFont = _getNewFont(currentFont, fontName, style, size)
        vm.setDisplayListFont(newFont)

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
    
    def allDataChoices(self):
        """Return a list of strings describing all available data choices
            MIKE
        """
        # return just the strings so that user isn't forced to use the result of allDataChoices
        # in later method calls
        choices = self._JavaProxy__javaObject.getDataChoices()
        return [choice.description for choice in choices]

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

    def allLevels(self):
        """List all levels for this data choice.
        """
        return self._JavaProxy__javaObject.getAllLevels()

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

# def windowDisplays(window):
#     """Returns a list of the McIDAS-V displays within the given window."""
#     pass

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

def createLayer():
    # TODO(mike): remove this method.  (Requires change to console_init.py
    #             which I'm having trouble committing at the moment...)
    pass

def allDataSourceNames():
    """Returns a list of all possible data source types
       (specifically, the verbose descriptions as they appear in the GUI)
       MIKE
    """
    mcv = getStaticMcv()
    dm = mcv.getDataManager()
    # want to return list of labels only, not DataSourceDescriptor's
    return [desc.label for desc in dm.getDescriptors()]

def allLayerTypes():
    """Returns a list of the available layer type names"""
    return getStaticMcv().getAllControlDescriptors()

def allProjections():
    """Returns a list of the available projections."""
    return [_Projection(projection) for projection in getStaticMcv().getIdvProjectionManager().getProjections()]

def allFontNames():
    """Return a list of strings representing all available font names"""
    return [font.toString() for font in ucar.unidata.util.GuiUtils.getFontList()]

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
MAP2D = _NoOp('MAP2D')
GLOBE = _NoOp('GLOBE')
TRANSECT = _NoOp('TRANSECT')

def buildWindow(width=600, height=400, rows=1, cols=1, panelTypes=None):
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
        
        if panelTypes is None:
            panelTypes = [MAP] * (rows * cols)
        elif isinstance(panelTypes, _NoOp):
            panelTypes = [panelTypes] * (rows * cols)
        elif type(panelTypes) is types.ListType:
            if len(panelTypes) != (rows * cols):
                raise ValueError('panelTypes needs to contain rows*cols elements')
        
        from edu.wisc.ssec.mcidasv import PersistenceManager
        
        try:
            window = PersistenceManager.buildDynamicSkin(width, height, rows, cols, panelTypes)
            if width > 0 and height > 0:
                print 'creating window: width=%d height=%d rows=%d cols=%d panelTypes=%s' % (width, height, rows, cols, panelTypes)
            else:
                bounds = window.getBounds()
                print 'creating window: width=%d height=%d rows=%d cols=%d panelTypes=%s' % (bounds.width, bounds.height, rows, cols, panelTypes)
            
            panels = []
            for holder in window.getComponentGroups()[0].getDisplayComponents():
                for viewManager in holder.getViewManagers():
                    panels.append(_Display(viewManager))
            return panels
        except NullPointerException, e:
            raise RuntimeError("could not build window", e)

    def _buildWindowBackground(height=-1, width=-1):
        """
         (1) create a new MapViewManager.  This is the default type of ViewManager
             if (null, null) is passed to createViewManager
         (2) Wrap the MapViewManager in a _Display object
         (3) Wrap the _Display in a list, simply because current calls to buildWindow expect this
              (could very easily do "npanels=rows*cols" and return an array with that many MapViewManagers
        
          Note, this is currently over-simplistic on-purpose, so we can test if this approach
          actually works better.  Eventually, buildWindow could do an ArgsManager.getIsOffScreen()
          and call this method if off screen, and do it's usual thing otherwise...
          Could also be generalized for globe displays, etc.
          
          Default size:  600 x 400 (this is default in MapProjectionDisplayJ3D for offscreen)
        """
        if (height > 0) and (width > 0):
            dim = java.awt.Dimension(width, height)
            # this utilizes the fact that doMakeDisplayMaster in MapViewManager gets it's default
            # dimension from StateManager.getViewSize().  It's slightly hack but much easier
            # than creating my own DisplayMaster and adding it to a new ViewManager.
            # Also, it seems to be much easier to create a ViewManager with a given Dimension
            # than to change it afterward...
            getStaticMcv().getStateManager().setViewSize(dim)
        
        newVM = getStaticMcv().getVMManager().createViewManager(None, None)
        return [_Display(newVM)]
    
    if getStaticMcv().getArgsManager().getIsOffScreen():
        return _buildWindowBackground(height, width)
    else:
        return _buildWindowInternal(width, height, rows, cols, panelTypes)

def makeLogger(name):
    """ """
    return  LoggerFactory.getLogger(name)

# def openBundle(bundle, label="", clear=1, height=-1, width=-1, dataDictionary=None):
def openBundle(bundle, label="", clear=1, dataDictionary=None):
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
    bundle = _expandpath(bundle)

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

    if (dataDictionary != None):
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
    pm.decodeXmlFile(bundle,label,checkToRemove,letUserChangeData,bundleProperties)
    pause()  # this might be controversial...?

    if (dataDictionary != None):
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

    # if (height != -1) and (width != -1):
    #     activeDisplay().setSize(width, height)
    # #    #firstWindow().setSize(width, height)

    return activeDisplay()  # TODO: return list of all displays instead
