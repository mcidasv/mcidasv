import jarray

from visad import ConstantMap, Display
from java.lang import CharSequence
from java.util import Collection

_colorCache = {}

_cnames = {
    'aliceblue'            : '#f0f8ff',
    'antiquewhite'         : '#faebd7',
    'aqua'                 : '#00ffff',
    'aquamarine'           : '#7fffd4',
    'azure'                : '#f0ffff',
    'beige'                : '#f5f5dc',
    'bisque'               : '#ffe4c4',
    'black'                : '#000000',
    'blanchedalmond'       : '#ffebcd',
    'blue'                 : '#0000ff',
    'blueviolet'           : '#8a2be2',
    'brown'                : '#a52a2a',
    'burlywood'            : '#deb887',
    'cadetblue'            : '#5f9ea0',
    'chartreuse'           : '#7fff00',
    'chocolate'            : '#d2691e',
    'coral'                : '#ff7f50',
    'cornflowerblue'       : '#6495ed',
    'cornsilk'             : '#fff8dc',
    'crimson'              : '#dc143c',
    'cyan'                 : '#00ffff',
    'darkblue'             : '#00008b',
    'darkcyan'             : '#008b8b',
    'darkgoldenrod'        : '#b8860b',
    'darkgray'             : '#a9a9a9',
    'darkgrey'             : '#a9a9a9',
    'darkgreen'            : '#006400',
    'darkkhaki'            : '#bdb76b',
    'darkmagenta'          : '#8b008b',
    'darkolivegreen'       : '#556b2f',
    'darkorange'           : '#ff8c00',
    'darkorchid'           : '#9932cc',
    'darkred'              : '#8b0000',
    'darksalmon'           : '#e9967a',
    'darkseagreen'         : '#8fbc8f',
    'darkslateblue'        : '#483d8b',
    'darkslategray'        : '#2f4f4f',
    'darkslategrey'        : '#2f4f4f',
    'darkturquoise'        : '#00ced1',
    'darkviolet'           : '#9400d3',
    'deeppink'             : '#ff1493',
    'deepskyblue'          : '#00bfff',
    'dimgray'              : '#696969',
    'dimgrey'              : '#696969',
    'dodgerblue'           : '#1e90ff',
    'firebrick'            : '#b22222',
    'floralwhite'          : '#fffaf0',
    'forestgreen'          : '#228b22',
    'fuchsia'              : '#ff00ff',
    'gainsboro'            : '#dcdcdc',
    'ghostwhite'           : '#f8f8ff',
    'gold'                 : '#ffd700',
    'goldenrod'            : '#daa520',
    'gray'                 : '#808080',
    'grey'                 : '#808080',
    'green'                : '#008000',
    'greenyellow'          : '#adff2f',
    'honeydew'             : '#f0fff0',
    'hotpink'              : '#ff69b4',
    'indianred'            : '#cd5c5c',
    'indigo'               : '#4b0082',
    'ivory'                : '#fffff0',
    'khaki'                : '#f0e68c',
    'lavender'             : '#e6e6fa',
    'lavenderblush'        : '#fff0f5',
    'lawngreen'            : '#7cfc00',
    'lemonchiffon'         : '#fffacd',
    'lightblue'            : '#add8e6',
    'lightcoral'           : '#f08080',
    'lightcyan'            : '#e0ffff',
    'lightgoldenrodyellow' : '#fafad2',
    'lightgreen'           : '#90ee90',
    'lightgrey'            : '#d3d3d3',
    'lightpink'            : '#ffb6c1',
    'lightsalmon'          : '#ffa07a',
    'lightseagreen'        : '#20b2aa',
    'lightskyblue'         : '#87cefa',
    'lightslategray'       : '#778899',
    'lightslategrey'       : '#778899',
    'lightsteelblue'       : '#b0c4de',
    'lightyellow'          : '#ffffe0',
    'lime'                 : '#00ff00',
    'limegreen'            : '#32cd32',
    'linen'                : '#faf0e6',
    'magenta'              : '#ff00ff',
    'maroon'               : '#800000',
    'mediumaquamarine'     : '#66cdaa',
    'mediumblue'           : '#0000cd',
    'mediumorchid'         : '#ba55d3',
    'mediumpurple'         : '#9370db',
    'mediumseagreen'       : '#3cb371',
    'mediumslateblue'      : '#7b68ee',
    'mediumspringgreen'    : '#00fa9a',
    'mediumturquoise'      : '#48d1cc',
    'mediumvioletred'      : '#c71585',
    'midnightblue'         : '#191970',
    'mintcream'            : '#f5fffa',
    'mistyrose'            : '#ffe4e1',
    'moccasin'             : '#ffe4b5',
    'navajowhite'          : '#ffdead',
    'navy'                 : '#000080',
    'oldlace'              : '#fdf5e6',
    'olive'                : '#808000',
    'olivedrab'            : '#6b8e23',
    'orange'               : '#ffa500',
    'orangered'            : '#ff4500',
    'orchid'               : '#da70d6',
    'palegoldenrod'        : '#eee8aa',
    'palegreen'            : '#98fb98',
    'palevioletred'        : '#afeeee',
    'papayawhip'           : '#ffefd5',
    'peachpuff'            : '#ffdab9',
    'peru'                 : '#cd853f',
    'pink'                 : '#ffc0cb',
    'plum'                 : '#dda0dd',
    'powderblue'           : '#b0e0e6',
    'purple'               : '#800080',
    'red'                  : '#ff0000',
    'rosybrown'            : '#bc8f8f',
    'royalblue'            : '#4169e1',
    'saddlebrown'          : '#8b4513',
    'salmon'               : '#fa8072',
    'sandybrown'           : '#faa460',
    'seagreen'             : '#2e8b57',
    'seashell'             : '#fff5ee',
    'sienna'               : '#a0522d',
    'silver'               : '#c0c0c0',
    'skyblue'              : '#87ceeb',
    'slateblue'            : '#6a5acd',
    'slategray'            : '#708090',
    'slategrey'            : '#708090',
    'snow'                 : '#fffafa',
    'springgreen'          : '#00ff7f',
    'steelblue'            : '#4682b4',
    'tan'                  : '#d2b48c',
    'teal'                 : '#008080',
    'thistle'              : '#d8bfd8',
    'tomato'               : '#ff6347',
    'turquoise'            : '#40e0d0',
    'violet'               : '#ee82ee',
    'wheat'                : '#f5deb3',
    'white'                : '#ffffff',
    'whitesmoke'           : '#f5f5f5',
    'yellow'               : '#ffff00',
    'yellowgreen'          : '#9acd32',
    'r'                    : '#ff0000',
    'g'                    : '#00ff00',
    'b'                    : '#0000ff',
    'c'                    : '#00ffff',
    'm'                    : '#ff00ff',
    'y'                    : '#ffff00',
    'k'                    : '#000000',
    'w'                    : '#ffffff',
}

def _convertHexColor(hex_str):
    t = tuple([int(n, 16) / 255.0 for n in (hex_str[1:3], hex_str[3:5], hex_str[5:7])])
    r = ConstantMap(t[0], Display.Red)
    g = ConstantMap(t[1], Display.Green)
    b = ConstantMap(t[2], Display.Blue)
    return r, g, b

def _convertNamedColor(name):
    if name not in _cnames:
        raise ValueError('Bad color string:', name)
    return _convertHexColor(_cnames[name])

def _convertRgbSeq(seq):
    if len(seq) is not 3:
        raise ValueError('Bad RGB sequence length:', len(seq))
    if [x for x in seq if not 0.0 <= float(x) <= 1.0]:
        raise ValueError('Bad value in RGB sequence; must be 0.0 <= <value> <= 1.0')
    
    r = ConstantMap(seq[0], Display.Red)
    g = ConstantMap(seq[1], Display.Green)
    b = ConstantMap(seq[2], Display.Blue)
    
    return r, g, b

def convertColor(color='green'):
    hash_key = str(color)
    
    if hash_key not in _colorCache:
        color_type = type(color)
        if color_type is type(''):
            if '#' == color[0]:
                r, g, b = _convertHexColor(color.lower())
            else:
                r, g, b = _convertNamedColor(color.lower())
        elif color_type is type([]) or color_type is type(()):
            r, g, b = _convertRgbSeq(color)
        else:
            raise TypeError('Cannot figure out how to convert \"', color, '\" to a color.')
        
        _colorCache[hash_key] = r, g, b
    
    # converts the tuple into a **Java** array of ConstantMaps
    return jarray.array(_colorCache[hash_key], ConstantMap)

