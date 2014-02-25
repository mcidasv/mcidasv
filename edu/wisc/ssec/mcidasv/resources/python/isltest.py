class ImageFormatting(object):
    def __init__(self):
        pass
        
class Matte(ImageFormatting):
    def __init__(self, background=None, top=None, bottom=None, left=None, right=None, space=None, hspace=None, vspace=None):
        self.background = 'background=%s' % (background) or ''
        self.top = 'top=%s' % (top) or ''
        self.bottom = 'bottom=%s' % (bottom) or ''
        self.left = 'left=%s' % (left) or ''
        self.right = 'right=%s' % (right) or ''
        self.space = 'space=%s' % (space) or ''
        self.hspace = 'hspace=%s' % (hspace) or ''
        self.vspace = 'vspace=%s' % (vspace) or ''
        
    def toIsl(self):
        return "matte %s %s %s %s %s %s %s %s; " % (self.background, self.top, self.bottom, self.left, self.right, self.space, self.hspace, self.vspace)
        
class ImageOverlay(ImageFormatting):
    def __init__(self, image=None, place=None, anchor=None, transparency=None, scale=None):
        self.image = 'image=%s' % (image) or ''
        self.place = 'place=%s' % (place) or ''
        self.anchor = 'anchor=%s' % (anchor) or ''
        self.transparency = 'transparency=%s' % (transparency) or ''
        self.scale = 'scale=%s' % (scale) or ''
        
    def toIsl(self):
        return "overlay %s %s %s %s; " % (self.image, self.place, self.anchor, self.transparency)
        
class TextOverlay(ImageFormatting):
    def __init__(self, text=None, place=None, anchor=None, fontSize=None, fontFace=None, color=None, background=None, transparency=None, scale=None):
        self.text = 'text=%s' % (text) or ''
        self.place = 'place=%s' % (place) or ''
        self.anchor = 'anchor=%s' % (anchor) or ''
        self.fontSize = 'fontsize=%s' % (fontSize) or ''
        self.fontFace = 'fontface=%s' % (fontFace) or ''
        self.color = 'color=%s' % (color) or ''
        self.background = 'background=%s' % (background) or ''
        self.transparency = 'transparency=%s' % (transparency) or ''
        self.scale = 'scale=%s' % (scale) or ''
        
    def toIsl(self):
        return "overlay %s %s %s %s %s %s %s %s; " % (self.text, self.place, self.anchor, self.fontSize, self.fontFace, self.color, self.background, self.transparency)
        
class Clip(ImageFormatting):
    def __init__(self, north, south, east, west, top, bottom, left, right, display, space, hspace, vspace, spaceLeft,spaceRight,spaceTop, spaceBottom):
        self.north = 'north=%s' % (north) or ''
        self.south = 'south=%s' % (south) or ''
        self.east = 'east=%s' % (east) or ''
        self.west = 'west=%s' % (west) or ''
        self.top = 'top=%s' % (top) or ''
        self.bottom = 'bottom=%s' % (bottom) or ''
        self.left = 'left=%s' % (left) or ''
        self.right = 'right=%s' % (right) or ''
        self.display = 'display=%s' % (display) or ''
        self.space = 'space=%s' % (space) or ''
        self.hspace = 'hspace=%s' % (hspace) or ''
        self.vspace = 'vspace=%s' % (vspace) or ''
        self.spaceLeft = 'space_left=%s' % (spaceLeft) or ''
        self.spaceRight = 'space_right=%s' % (spaceRight) or ''
        self.spaceTop = 'space_top=%s' % (spaceTop) or ''
        self.spaceBottom = 'space_bottom=%s' % (spaceBottom) or ''
        
    def toIsl(self):
        return "clip %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s; " % (self.north, self.south, self.east, self.west, self.top, self.bottom, self.left, self.right, self.display, self.space, self.hspace, self.vspace, self.spaceLeft, self.spaceRight, self.spaceTop, self.spaceBottom)
        
class Colorbar(ImageFormatting):
    def __init__(self, display, width, height, orientation, tickmarks, interval, values, place, anchor, showlines, suffix, suffixfrequency, showunit, transparency, color, lineColor):
        self.display = 'display=%s' % (display) or ''
        self.width = 'width=%s' % (width) or ''
        self.height = 'height=%s' % (height) or ''
        self.orientation = 'orientation=%s' % (orientation) or ''
        self.tickmarks = 'tickmarks=%s' % (tickmarks) or ''
        self.interval = 'interval=%s' % (interval) or ''
        self.values = 'values=%s' % (values) or ''
        self.place = 'place=%s' % (place) or ''
        self.anchor = 'anchor=%s' % (anchor) or ''
        self.showlines = 'showlines=%s' % (showlines) or ''
        self.suffix = 'suffix=%s' % (suffix) or ''
        self.suffixfrequency = 'suffixfrequency=%s' % (suffixfrequency) or ''
        self.showunit = 'showunit=%s' % (showunit) or ''
        self.transparency = 'transparency=%s' % (transparency) or ''
        self.color = 'color=%s' % (color) or ''
        self.lineColor = 'linecolor=%s' % (lineColor) or ''
        
    def toIsl(self):
        return "colorbar %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s; " % (self.display, self.width, self.height, self.orientation, self.tickmarks, self.interval, self.values, self.place, self.anchor, self.showlines, self.suffix, self.suffixfrequency, self.showunit, self.transparency, self.color, self.lineColor)
        
class TransparentColor(ImageFormatting):
    # def __init__(self, color, rgbTuple):
    def __init__(self, color=None):
        self.color = 'color=%s' % (color) or ''
        # self.red = red
        # self.green = green
        # self.blue = blue
        
    def toIsl(self):
        return "transparent %s; " % (self.color)
        
class TransparentBackground(ImageFormatting):
    def __init__(self):
        pass
        
    def toIsl(self):
        return "backgroundtransparent ; "
