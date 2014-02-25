class ImageFormatting(object):
    def __init__(self):
        pass
        
class Matte(ImageFormatting):
    def __init__(self, background=None, top=None, bottom=None, left=None, right=None, space=None, hspace=None, vspace=None):
        if background:
            self.background = 'background=%s' % (background)
        else:
            self.background = ''
            
        if top:
            self.top = 'top=%s' % (top)
        else:
            self.top = ''
            
        if bottom:
            self.bottom = 'bottom=%s' % (bottom)
        else:
            self.bottom = ''
            
        if left:
            self.left = 'left=%s' % (left)
        else:
            self.left = ''
            
        if right:
            self.right = 'right=%s' % (right)
        else:
            self.right = ''
            
        if space:
            self.space = 'space=%s' % (space)
        else:
            self.space = ''
            
        if hspace:
            self.hspace = 'hspace=%s' % (hspace)
        else:
            self.hspace = ''
            
        if vspace:
            self.vspace = 'vspace=%s' % (vspace)
        else:
            self.vspace = ''
            
    def toIsl(self):
        islString = "matte %s %s %s %s %s %s %s %s" % (self.background, self.top, self.bottom, self.left, self.right, self.space, self.hspace, self.vspace)
        return islString.strip() + '; '
        
class ImageOverlay(ImageFormatting):
    def __init__(self, image=None, place=None, anchor=None, transparency=None, scale=None):
        if image:
            self.image = 'image=%s' % (image)
        else:
            self.image = ''
            
        if place:
            self.place = 'place=%s' % (place)
        else:
            self.place = ''
            
        if anchor:
            self.anchor = 'anchor=%s' % (anchor)
        else:
            self.anchor = ''
            
        if transparency:
            self.transparency = 'transparency=%s' % (transparency)
        else:
            self.transparency = ''
            
        if scale:
            self.scale = 'scale=%s' % (scale)
        else:
            self.scale = ''
            
    def toIsl(self):
        islString = "overlay %s %s %s %s" % (self.image, self.place, self.anchor, self.transparency)
        return islString.strip() + '; '
        
class TextOverlay(ImageFormatting):
    def __init__(self, text=None, place=None, anchor=None, fontSize=None, fontFace=None, color=None, background=None, transparency=None, scale=None):
        if text:
            self.text = 'text=%s' % (text)
        else:
            self.text = ''
            
        if place:
            self.place = 'place=%s' % (place)
        else:
            self.place = ''
            
        if anchor:
            self.anchor = 'anchor=%s' % (anchor)
        else:
            self.anchor = ''
            
        if fontSize:
            self.fontSize = 'fontsize=%s' % (fontSize)
        else:
            self.fontSize = ''
            
        if fontFace:
            self.fontFace = 'fontface=%s' % (fontFace)
        else:
            self.fontFace = ''
            
        if color:
            self.color = 'color=%s' % (color)
        else:
            self.color = ''
            
        if background:
            self.background = 'background=%s' % (background)
        else:
            self.background = ''
            
        if transparency:
            self.transparency = 'transparency=%s' % (transparency)
        else:
            self.transparency = ''
            
        if scale:
            self.scale = 'scale=%s' % (scale)
        else:
            self.scale = ''
            
    def toIsl(self):
        islString = "overlay %s %s %s %s %s %s %s %s" % (self.text, self.place, self.anchor, self.fontSize, self.fontFace, self.color, self.background, self.transparency)
        return islString.strip() + '; '
        
class Clip(ImageFormatting):
    def __init__(self, north, south, east, west, top, bottom, left, right, display, space, hspace, vspace, spaceLeft,spaceRight,spaceTop, spaceBottom):
        if north:
            self.north = 'north=%s' % (north)
        else:
            self.north = ''
            
        if south:
            self.south = 'south=%s' % (south)
        else:
            self.south = ''
            
        if east:
            self.east = 'east=%s' % (east)
        else:
            self.east = ''
            
        if west:
            self.west = 'west=%s' % (west)
        else:
            self.west = ''
            
        if top:
            self.top = 'top=%s' % (top)
        else:
            self.top = ''
            
        if bottom:
            self.bottom = 'bottom=%s' % (bottom)
        else:
            self.bottom = ''
            
        if left:
            self.left = 'left=%s' % (left)
        else:
            self.left = ''
            
        if right:
            self.right = 'right=%s' % (right)
        else:
            self.right = ''
            
        if display:
            self.display = 'display=%s' % (display)
        else:
            self.display = ''
            
        if space:
            self.space = 'space=%s' % (space)
        else:
            self.space = ''
            
        if hspace:
            self.hspace = 'hspace=%s' % (hspace)
        else:
            self.hspace = ''
            
        if vspace:
            self.vspace = 'vspace=%s' % (vspace)
        else:
            self.vspace = ''
            
        if spaceLeft:
            self.spaceLeft = 'space_left=%s' % (spaceLeft)
        else:
            self.spaceLeft = ''
            
        if spaceRight:
            self.spaceRight = 'space_right=%s' % (spaceRight)
        else:
            self.spaceRight = ''
            
        if spaceTop:
            self.spaceTop = 'space_top=%s' % (spaceTop)
        else:
            self.spaceTop = ''
            
        if spaceBottom:
            self.spaceBottom = 'space_bottom=%s' % (spaceBottom)
        else:
            self.spaceBottom = ''
            
    def toIsl(self):
        islString = "clip %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s" % (self.north, self.south, self.east, self.west, self.top, self.bottom, self.left, self.right, self.display, self.space, self.hspace, self.vspace, self.spaceLeft, self.spaceRight, self.spaceTop, self.spaceBottom)
        return islString.strip() + '; '
        
class Colorbar(ImageFormatting):
    def __init__(self, display, width, height, orientation, tickmarks, interval, values, place, anchor, showlines, suffix, suffixfrequency, showunit, transparency, color, lineColor):
        if display:
            self.display = 'display=%s' % (display)
        else:
            self.display = ''
            
        if width:
            self.width = 'width=%s' % (width)
        else:
            self.width = ''
            
        if height:
            self.height = 'height=%s' % (height)
        else:
            self.height = ''
            
        if orientation:
            self.orientation = 'orientation=%s' % (orientation)
        else:
            self.orientation = ''
            
        if tickmarks:
            self.tickmarks = 'tickmarks=%s' % (tickmarks)
        else:
            self.tickmarks = ''
            
        if interval:
            self.interval = 'interval=%s' % (interval)
        else:
            self.interval = ''
            
        if values:
            self.values = 'values=%s' % (values)
        else:
            self.values = ''
            
        if place:
            self.place = 'place=%s' % (place)
        else:
            self.place = ''
            
        if anchor:
            self.anchor = 'anchor=%s' % (anchor)
        else:
            self.anchor = ''
            
        if showlines:
            self.showlines = 'showlines=%s' % (showlines)
        else:
            self.showlines = ''
            
        if suffix:
            self.suffix = 'suffix=%s' % (suffix)
        else:
            self.suffix = ''
            
        if suffixfrequency:
            self.suffixfrequency = 'suffixfrequency=%s' % (suffixfrequency)
        else:
            self.suffixfrequency = ''
            
        if showunit:
            self.showunit = 'showunit=%s' % (showunit)
        else:
            self.showunit = ''
            
        if transparency:
            self.transparency = 'transparency=%s' % (transparency)
        else:
            self.transparency = ''
            
        if color:
            self.color = 'color=%s' % (color)
        else:
            self.color = ''
            
        if lineColor:
            self.lineColor = 'linecolor=%s' % (lineColor)
        else:
            self.lineColor = ''
            
    def toIsl(self):
        islString = "colorbar %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s" % (self.display, self.width, self.height, self.orientation, self.tickmarks, self.interval, self.values, self.place, self.anchor, self.showlines, self.suffix, self.suffixfrequency, self.showunit, self.transparency, self.color, self.lineColor)
        return islString.strip() + '; '
        
class TransparentColor(ImageFormatting):
    # def __init__(self, color, rgbTuple):
    def __init__(self, color=None):
        if color:
            self.color = 'color=%s' % (color)
        else:
            self.color = ''
        # self.red = red
        # self.green = green
        # self.blue = blue
        
    def toIsl(self):
        islString = "transparent %s" % (self.color)
        return islString.strip() + '; '
        
class TransparentBackground(ImageFormatting):
    def __init__(self):
        pass
        
    def toIsl(self):
        return "backgroundtransparent ; "
