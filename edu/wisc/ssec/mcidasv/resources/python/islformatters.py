"""Classes for 'Pythonic' usage of ISL formatting capabilities."""

from types import MethodType
import os


BAD_COLOR_INPUT = "Input color must be either a single RGB color (e.g. 'color=\"0,255,0\"') " \
                  "or color names (multiple colors may be specified, but must be delimited by commas). " \
                  "Color provided was: %s"


class ImageFormatting(object):
    
    """Base class for ISL formatting objects."""
    def islPrefix(self):
        raise NotImplementedError("'make_isl_string' function requires that this method is implemented")
    
    def toIsl(self):
        return make_isl_string(self)


class Matte(ImageFormatting):
    
    """The Matte formatter allows you to add a space around any of the sides of an image."""
    
    def __init__(self,
                 background=None,
                 top=None,
                 bottom=None,
                 left=None,
                 right=None,
                 space=None,
                 hSpace=None,
                 vSpace=None):
        """Create a new Matte ISL formatting object.
        
        Optional Args:
            background: Color name or red,green,blue values.
            top: Pixel spacing.
            bottom: Pixel spacing.
            left: Pixel spacing.
            right: Pixel spacing.
            space: Pixel spacing.
            hSpace: Pixel spacing.
            vSpace: Pixel spacing.
        """
        self.background = varElse(background, 'background')
        self.top = varElse(top, 'top')
        self.bottom = varElse(bottom, 'bottom')
        self.left = varElse(left, 'left')
        self.right = varElse(right, 'right')
        self.space = varElse(space, 'space')
        self.hSpace = varElse(hSpace, 'hspace')
        self.vSpace = varElse(vSpace, 'vspace')
        
    def islPrefix(self):
        return 'matte'


class ImageOverlay(ImageFormatting):
    
    """The ImageOverlay formatter allows you to add an icon as an image overlay."""
    
    def __init__(self,
                 image=None,
                 place=None,
                 anchor=None,
                 transparency=None,
                 scale=None):
        """Create a new ImageOverlay ISL formatting object.
        
        Optional Args:
            image: Filesystem path or URL to image.
            place: Rectangle point on base image.
            anchor: Rectangle point on overlay
            transparency: Transparency percentage. Values should be 0 - 1.0.
            scale: Not currently functional.
        """
        if image is not None:
            if not os.path.exists(image):
                raise IOError("Image Overlay file '%s' does not exist" % image)
            self.image = 'image=%s' % image
        else:
            from ucar.unidata.util.GuiUtils import MISSING_IMAGE
            self.image = 'image=%s' % MISSING_IMAGE
            
        self.place = varElse(place, 'place')
        self.anchor = varElse(anchor, 'anchor')
        self.transparency = varElse(transparency, 'transparency')
        self.scale = varElse(scale, 'scale')
        
    def islPrefix(self):
        return 'overlay'


class TextOverlay(ImageFormatting):
    
    """The TextOverlay formatter allows you to add text as an overlay. """
    
    def __init__(self,
                 text=None,
                 place=None,
                 anchor=None,
                 fontSize=None,
                 fontFace=None,
                 color=None,
                 background=None,
                 transparency=None,
                 scale=None):
        """Create a new TextOverlay ISL formatting object.
        
        Required Args:
            text: Text to draw.
        
        Optional Args:
            place: Rectangle point on base image.
            anchor: Rectangle point on overlay.
            fontSize: Font size for text.
            fontFace: Font face for text
            color: Color for text.
            background: Color for background.
            transparency: Transparency percentage. Values should be 0-1.0.
            scale: Not currently functional.
        """
        if text is not None:
            self.text = 'text=%s' % text
        else:
            raise TypeError("TextOverlay formatter requires the 'text' parameter")
            
        self.place = varElse(place, 'place')
        self.anchor = varElse(anchor, 'anchor')
        self.fontSize = varElse(fontSize, 'fontsize')
        self.fontFace = varElse(fontFace, 'fontface')
        self.color = varElse(color, 'color')
        self.background = varElse(background, 'background')
        self.transparency = varElse(transparency, 'transparency')
        self.scale = varElse(scale, 'scale')
        
    def islPrefix(self):
        return 'overlay'


class Clip(ImageFormatting):
    
    """The Clip formatter allows you to clip the image."""
    
    def __init__(self,
                 north=None,
                 south=None,
                 east=None,
                 west=None,
                 top=None,
                 bottom=None,
                 left=None,
                 right=None,
                 display=None,
                 space=None,
                 hSpace=None,
                 vSpace=None,
                 spaceLeft=None,
                 spaceRight=None,
                 spaceTop=None,
                 spaceBottom=None):
        """Create a new Clip ISL formatting object.
        
        Optional Args:
            north: Latitude.
            south: Latitude.
            east: Longitude.
            west: Longitude.
            top: Pixels or percentage.
            bottom: Pixels or percentage.
            left: Pixels or percentage.
            right: Pixels or percentage.
            display: ID of a display which we use its data's map projection to clip with.
            space: When clipping at the box pad outwards this number of pixels.
            hSpace: When clipping at the box pad horiz.
            vSpace: When clipping at the box pad vertically.
            spaceLeft: Padding.
            spaceRight: Padding.
            spaceTop: Padding.
            spaceBottom: Padding.
        """
        self.north = varElse(north, 'north')
        self.south = varElse(south, 'south')
        self.east = varElse(east, 'east')
        self.west = varElse(west, 'west')
        self.top = varElse(top, 'top')
        self.bottom = varElse(bottom, 'bottom')
        self.left = varElse(left, 'left')
        self.right = varElse(right, 'right')
        self.display = varElse(display, 'display')
        self.space = varElse(space, 'space')
        self.hSpace = varElse(hSpace, 'hspace')
        self.vSpace = varElse(vSpace, 'vspace')
        self.spaceLeft = varElse(spaceLeft, 'space_left')
        self.spaceRight = varElse(spaceRight, 'space_right')
        self.spaceTop = varElse(spaceTop, 'space_top')
        self.spaceBottom = varElse(spaceBottom, 'space_bottom')
        
    def islPrefix(self):
        return 'clip'


class Colorbar(ImageFormatting):
    
    """The Colorbar formatter allows you to add a color bar from the color tables in the display controls."""
    
    def __init__(self,
                 display=None,
                 width=None,
                 height=None,
                 orientation=None,
                 tickMarks=None,
                 interval=None,
                 values=None,
                 place=None,
                 anchor=None,
                 showLines=None,
                 suffix=None,
                 suffixFrequency=None,
                 showUnit=None,
                 transparency=None,
                 color=None,
                 lineColor=None,
                 fontFace=None,
                 fontSize=None):
        """Create a new Colorbar ISL formatting object.
        
        Optional Args:
        
            display: Optional id of display to use. If not defined then this
                     will use all colorbars found.
            width: Bar width.
            height: Bar height.
            orientation: Tick mark location.
                         Accepted values: "right", "left", "bottom", "top".
            tickMarks: Number of tick marks.
            interval: Interval value.
            values: Comma-separated list of values.
            place: Rectangle location on image.
            anchor: Rectangle location on colorbar.
            showLines: Draw tick lines. Accepted values: True, False
                       "true" or "false" are also accepted, but are considered bad
                       practice.
            suffix: Text to add to tick labels
            suffixFrequency: Frequency of suffix plotting.
                             Accepted values: "all", "first", "last".
            showUnit: Use unit as suffix. Accepted values: True, False
                      "true" or "false" are also accepted, but are considered
                      bad practice.
            transparency: Include the transparency when drawing the colorbar
                          (defaults to True).
                          Accepted values: True, False
                          "true" or "false" are also accepted, but are
                          considered bad practice.
            color: Label color.
            lineColor: Line color.
            fontFace: Name of font to use for labels.
            fontSize Font size for labels.
        """
        if display is not None:
            self.displayObj = display
            display._ensureIslId()
            self.display = 'display=%s' % display.getJavaInstance().getId()
        else:
            self.display = ''
            
        if suffixFrequency is not None:
            self.suffixFrequency = 'suffixfrequency=%s' % suffixFrequency
        else:
            if suffix is not None or showUnit is not None:
                self.suffixFrequency = 'suffixfrequency=last'
            else:
                self.suffixFrequency = ''
                
        self.width = varElse(width, 'width')
        self.height = varElse(height, 'height')
        self.orientation = varElse(orientation, 'orientation')
        self.tickMarks = varElse(tickMarks, 'tickmarks')
        self.interval = varElse(interval, 'interval')
        self.values = varElse(values, 'values')
        self.place = varElse(place, 'place')
        self.anchor = varElse(anchor, 'anchor')
        self.showLines = varElse(showLines, 'showlines')
        self.suffix = varElse(suffix, 'suffix')
        self.showUnit = varElse(showUnit, 'showunit')
        self.transparency = varElse(transparency, 'transparency')
        self.color = varElse(color, 'color')
        self.lineColor = varElse(lineColor, 'linecolor')
        self.fontFace = varElse(fontFace, 'fontface')
        self.fontSize = varElse(fontSize, 'fontsize')
        
    def islPrefix(self):
        return 'colorbar'


class TransparentColor(ImageFormatting):
    
    """The TransparentColor formatter marks a color in the display as transparent."""
    
    def __init__(self, color):
        """Create a new TransparentColor ISL formatting object.
        
        Arbitrarily many TransparentColor objects can be passed 'captureImage'.
        This is especially useful in situations where multiple RGB color values
        must be made transparent.
        
        Required Arg:
            color: Color to set to transparent. Value may either be a single
                   RGB (values from 0 to 255) or named colors. Multiple named
                   color must be separated by commas.
        """
        if not color:
            self.colorList = []
        else:
            # coming in as string:
            # 255,0,0        : a single color specified as rgb!
            # Red            : just the color red
            # Red,Green,Blue : needs to be three sep colors!
            # Red, Green,Blue: needs to be three sep colors!
            # ,              : invalid!
            # Red,, Blue     : also invalid!
            index = color.find(',')
            
            # there was a least a single comma
            if index >= 0:
                vals = [x.strip() for x in color.split(',') if len(x.strip()) != 0]
                
                if check_for_integers(vals):
                    self.colorList = [color]
                elif not any(x.lstrip('-+').isdigit() for x in vals):
                    self.colorList = vals
                else:
                    raise ValueError(BAD_COLOR_INPUT % color)
            else:
                self.colorList = [color]
                
    def toIsl(self):
        """Return the ISL string representing this TransparentColor instance."""
        islString = ''
        for color in self.colorList:
            islString += "transparent color=%s ;" % color
        return islString


class TransparentBackground(ImageFormatting):
    
    """The TransparentBackground formatter marks the display background color as transparent."""
    
    def __init__(self):
        """Create a new TransparentBackground ISL formatting object."""
        pass
        
    def toIsl(self):
        """Return the ISL string representing this TransparentBackground instance."""
        return "backgroundtransparent ; "


class Resize(ImageFormatting):
    
    """The Resize formatter allows you to resize the captured image."""
    
    def __init__(self, dimensions):
        """Create a new Resize ISL formatting object.
        
        Optional Arg:
            dimensions: Tuple representing the desired dimensions (width and
                        height). Values may either be in pixels or percentages.
        """
        if dimensions and len(dimensions) == 2:
            self.width = 'width=%s' % (dimensions[0])
            self.height = 'height=%s' % (dimensions[1])
        else:
            raise ValueError()
            
    def islPrefix(self):
        return 'resize'


def check_for_integers(values):
    return len(values) == 3 and \
           all(x.lstrip('-+').isdigit() for x in values) and \
           all(int(x) >= 0 or x <= 255 for x in values)


def make_isl_string(name, formatter):
    result = '%s ' % formatter.islPrefix()
    for a in dir(formatter):
        val = getattr(formatter, a)
        if (not a.startswith('__')) and (type(val) != MethodType):
            result += '%s ' % val
    return result.strip() + '; '


def varElse(variable, islattr):
    """Return ISL attribute setting string if variable is valid."""
    if variable is not None:
        return '%s=%s' % (islattr, variable)
    return ''
