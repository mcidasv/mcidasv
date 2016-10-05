"""Classes for 'Pythonic' usage of ISL formatting capabilities."""

class ImageFormatting(object):
    
    """Base class for ISL formatting objects."""
    
    def __init__(self):
        """Override this stubbed out method."""
        pass
        
class Matte(ImageFormatting):
    
    """The Matte formatter allows you to add a space around any of the sides of an image."""
    
    def __init__(self, background=None, top=None, bottom=None, left=None, right=None, space=None, hSpace=None, vSpace=None):
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
        if background is not None:
            self.background = 'background=%s' % (background)
        else:
            self.background = ''
            
        if top is not None:
            self.top = 'top=%s' % (top)
        else:
            self.top = ''
            
        if bottom is not None:
            self.bottom = 'bottom=%s' % (bottom)
        else:
            self.bottom = ''
            
        if left is not None:
            self.left = 'left=%s' % (left)
        else:
            self.left = ''
            
        if right is not None:
            self.right = 'right=%s' % (right)
        else:
            self.right = ''
            
        if space is not None:
            self.space = 'space=%s' % (space)
        else:
            self.space = ''
            
        if hSpace is not None:
            self.hSpace = 'hspace=%s' % (hSpace)
        else:
            self.hSpace = ''
            
        if vSpace is not None:
            self.vSpace = 'vspace=%s' % (vSpace)
        else:
            self.vSpace = ''
            
    def toIsl(self):
        """Return the ISL string representing this Matte instance."""
        islString = "matte %s %s %s %s %s %s %s %s" % (self.background, self.top, self.bottom, self.left, self.right, self.space, self.hSpace, self.vSpace)
        return islString.strip() + '; '
        
class ImageOverlay(ImageFormatting):
    
    """The ImageOverlay formatter allows you to add an icon as an image overlay."""
    
    def __init__(self, image=None, place=None, anchor=None, transparency=None, scale=None):
        """Create a new ImageOverlay ISL formatting object.
        
        Optional Args:
            image: Filesystem path or URL to image.
            place: Rectangle point on base image.
            anchor: Rectangle point on overlay
            transparency: Transparency percentage. Values should be 0 - 1.0.
            scale: Not currently functional.
        """
        if image is not None:
            self.image = 'image=%s' % (image)
        else:
            from ucar.unidata.util.GuiUtils import MISSING_IMAGE
            self.image = 'image=%s' % (MISSING_IMAGE)
            
        if place is not None:
            self.place = 'place=%s' % (place)
        else:
            self.place = ''
            
        if anchor is not None:
            self.anchor = 'anchor=%s' % (anchor)
        else:
            self.anchor = ''
            
        if transparency is not None:
            self.transparency = 'transparency=%s' % (transparency)
        else:
            self.transparency = ''
            
        if scale is not None:
            self.scale = 'scale=%s' % (scale)
        else:
            self.scale = ''
            
    def toIsl(self):
        """Return the ISL string representing this ImageOverlay instance."""
        islString = "overlay %s %s %s %s" % (self.image, self.place, self.anchor, self.transparency)
        return islString.strip() + '; '
        
class TextOverlay(ImageFormatting):
    
    """The TextOverlay formatter allows you to add text as an overlay. """
    
    def __init__(self, text=None, place=None, anchor=None, fontSize=None, fontFace=None, color=None, background=None, transparency=None, scale=None):
        """Create a new TextOverlay ISL formatting object.
        
        Optional Args:
            text: Text to draw.
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
            self.text = 'text=%s' % (text)
        else:
            self.text = ''
            
        if place is not None:
            self.place = 'place=%s' % (place)
        else:
            self.place = ''
            
        if anchor is not None:
            self.anchor = 'anchor=%s' % (anchor)
        else:
            self.anchor = ''
            
        if fontSize is not None:
            self.fontSize = 'fontsize=%s' % (fontSize)
        else:
            self.fontSize = ''
            
        if fontFace is not None:
            self.fontFace = 'fontface=%s' % (fontFace)
        else:
            self.fontFace = ''
            
        if color is not None:
            self.color = 'color=%s' % (color)
        else:
            self.color = ''
            
        if background is not None:
            self.background = 'background=%s' % (background)
        else:
            self.background = ''
            
        if transparency is not None:
            self.transparency = 'transparency=%s' % (transparency)
        else:
            self.transparency = ''
            
        if scale is not None:
            self.scale = 'scale=%s' % (scale)
        else:
            self.scale = ''
            
    def toIsl(self):
        """Return the ISL string representing this TextOverlay instance."""
        islString = "overlay %s %s %s %s %s %s %s %s" % (self.text, self.place, self.anchor, self.fontSize, self.fontFace, self.color, self.background, self.transparency)
        return islString.strip() + '; '
        
class Clip(ImageFormatting):
    
    """The Clip formatter allows you to clip the image."""
    
    def __init__(self, north=None, south=None, east=None, west=None, top=None, bottom=None, left=None, right=None, display=None, space=None, hSpace=None, vSpace=None, spaceLeft=None, spaceRight=None, spaceTop=None, spaceBottom=None):
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
        if north is not None:
            self.north = 'north=%s' % (north)
        else:
            self.north = ''
            
        if south is not None:
            self.south = 'south=%s' % (south)
        else:
            self.south = ''
            
        if east is not None:
            self.east = 'east=%s' % (east)
        else:
            self.east = ''
            
        if west is not None:
            self.west = 'west=%s' % (west)
        else:
            self.west = ''
            
        if top is not None:
            self.top = 'top=%s' % (top)
        else:
            self.top = ''
            
        if bottom is not None:
            self.bottom = 'bottom=%s' % (bottom)
        else:
            self.bottom = ''
            
        if left is not None:
            self.left = 'left=%s' % (left)
        else:
            self.left = ''
            
        if right is not None:
            self.right = 'right=%s' % (right)
        else:
            self.right = ''
            
        if display is not None:
            self.display = 'display=%s' % (display)
        else:
            self.display = ''
            
        if space is not None:
            self.space = 'space=%s' % (space)
        else:
            self.space = ''
            
        if hSpace is not None:
            self.hSpace = 'hspace=%s' % (hSpace)
        else:
            self.hSpace = ''
            
        if vSpace is not None:
            self.vSpace = 'vspace=%s' % (vSpace)
        else:
            self.vSpace = ''
            
        if spaceLeft is not None:
            self.spaceLeft = 'space_left=%s' % (spaceLeft)
        else:
            self.spaceLeft = ''
            
        if spaceRight is not None:
            self.spaceRight = 'space_right=%s' % (spaceRight)
        else:
            self.spaceRight = ''
            
        if spaceTop is not None:
            self.spaceTop = 'space_top=%s' % (spaceTop)
        else:
            self.spaceTop = ''
            
        if spaceBottom is not None:
            self.spaceBottom = 'space_bottom=%s' % (spaceBottom)
        else:
            self.spaceBottom = ''
            
    def toIsl(self):
        """Return the ISL string representing this Clip instance."""
        islString = "clip %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s" % (self.north, self.south, self.east, self.west, self.top, self.bottom, self.left, self.right, self.display, self.space, self.hSpace, self.vSpace, self.spaceLeft, self.spaceRight, self.spaceTop, self.spaceBottom)
        return islString.strip() + '; '
        
class Colorbar(ImageFormatting):
    
    """The Colorbar formatter allows you to add a color bar from the color tables in the display controls."""
    
    def __init__(self, display=None, width=None, height=None, orientation=None, tickMarks=None, interval=None, values=None, place=None, anchor=None, showLines=None, suffix=None, suffixFrequency=None, showUnit=None, transparency=None, color=None, lineColor=None, fontFace=None, fontSize=None):
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
            self.display = 'display=%s' % (display.getJavaInstance().getId())
        else:
            self.display = ''
            
        if width is not None:
            self.width = 'width=%s' % (width)
        else:
            self.width = ''
            
        if height is not None:
            self.height = 'height=%s' % (height)
        else:
            self.height = ''
            
        if orientation is not None:
            self.orientation = 'orientation=%s' % (orientation)
        else:
            self.orientation = ''
            
        if tickMarks is not None:
            self.tickMarks = 'tickmarks=%s' % (tickMarks)
        else:
            self.tickMarks = ''
            
        if interval is not None:
            self.interval = 'interval=%s' % (interval)
        else:
            self.interval = ''
            
        if values is not None:
            self.values = 'values=%s' % (values)
        else:
            self.values = ''
            
        if place is not None:
            self.place = 'place=%s' % (place)
        else:
            self.place = ''
            
        if anchor is not None:
            self.anchor = 'anchor=%s' % (anchor)
        else:
            self.anchor = ''
            
        if showLines is not None:
            self.showLines = 'showlines=%s' % (showLines)
        else:
            self.showLines = ''
            
        if suffix is not None:
            self.suffix = 'suffix=%s' % (suffix)
        else:
            self.suffix = ''
            
        if suffixFrequency is not None:
            self.suffixFrequency = 'suffixfrequency=%s' % (suffixFrequency)
        else:
            if suffix is not None or showUnit is not None:
                self.suffixFrequency = 'suffixfrequency=last'
            else:
                self.suffixFrequency = ''
            
        if showUnit is not None:
            self.showUnit = 'showunit=%s' % (showUnit)
        else:
            self.showUnit = ''
            
        if transparency is not None:
            self.transparency = 'transparency=%s' % (transparency)
        else:
            self.transparency = ''
            
        if color is not None:
            self.color = 'color=%s' % (color)
        else:
            self.color = ''
            
        if lineColor is not None:
            self.lineColor = 'linecolor=%s' % (lineColor)
        else:
            self.lineColor = ''

        if fontFace is not None:
            self.fontFace = 'fontface=%s' % (fontFace)
        else:
            self.fontFace = ''

        if fontSize is not None:
            self.fontSize = 'fontsize=%s' % (fontSize)
        else:
            self.fontSize = ''

    def toIsl(self):
        """Return the ISL string representing this Colorbar instance."""
        islString = "colorbar %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s" % (self.display, self.width, self.height, self.orientation, self.tickMarks, self.interval, self.values, self.place, self.anchor, self.showLines, self.suffix, self.suffixFrequency, self.showUnit, self.transparency, self.color, self.lineColor, self.fontFace, self.fontSize)
        return islString.strip() + '; '
        
class TransparentColor(ImageFormatting):
    
    """The TransparentColor formatter marks a color in the display as transparent."""
    
    # def __init__(self, color, rgbTuple):
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
                
                if len(vals) == 3 and all(x.lstrip('-+').isdigit() for x in vals) and all(int(x) >= 0 or x <= 255 for x in vals):
                    self.colorList = [ color ]
                elif not any(x.lstrip('-+').isdigit() for x in vals):
                    self.colorList = vals
                else:
                    raise ValueError("Input color must be either a single RGB color (e.g. 'color=\"0,255,0\"') or color names (multiple colors may be specified, but must be delimited by commas). Color provided was: %s" % (color))
            else:
                self.colorList = [ color ]
                
    def toIsl(self):
        """Return the ISL string representing this TransparentColor instance."""
        islString = ''
        for color in self.colorList:
            islString += "transparent color=%s ;" % (color)
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
            
    def toIsl(self):
        """Return the ISL string representing this Resize instance."""
        islString = "resize %s %s" % (self.width, self.height)
        return islString.strip() + '; '
