# python imports 
import fnmatch
import inspect
import re
import os
import shutil
import sys
import textwrap
import types

# heinous java imports! boo! hiss!
from ch.qos.logback.core import FileAppender
from ch.qos.logback.classic import LoggerContext

from edu.wisc.ssec.mcidasv.McIDASV import getStaticMcv

from java.lang import Class
from java.lang import Object
from java.lang.reflect import Modifier

from org.python.core import PyReflectedFunction

from org.slf4j import Logger
from org.slf4j import LoggerFactory

_CONTEXT_ASSERT_MSG = "expected 'default' context; got '%s'"
_APPENDER_ASSERT_MSG = "expected appender to be a subclass of FileAppender; got '%s'"

def _expandpath(path):
    """Expands ENV variables, fixes things like '~', and then normalizes the
    given path."""
    return os.path.normpath(os.path.expanduser(os.path.expandvars(path)))

def getMouseEarthLocation():
    display = getStaticMcv().getVMManager().getLastActiveViewManager()
    master = display.getMaster()
    visadLat, visadLon = master.getCursorLatitude(), master.getCursorLongitude()
    return visadLat.getValue(), visadLon.getValue()

def describeActions(pattern=None):
    # """Prints out a list of the McIDAS-V actions.

    # The output is ordered alphabetically and grouped by functionality. Each
    # identifier can be "run" like so:

    # performAction(identifier)
    # performAction('edit.paramdefaults')

    # Args:
    #     pattern: Searches for the given pattern within the action identifier
    #              strings as well as action descriptions.
    # """
    # actions = _mcv.getIdvUIManager().getCachedActions().getAllActions()
    actions = getStaticMcv().getIdvUIManager().getCachedActions().getAllActions()
    print sorted([action.getId() for action in actions])

def getLogFile():
    # TODO(jon): this will likely have to change as the complexity of 
    #            logback.xml increases. :(
    # should return the "default" logging context
    context = LoggerFactory.getILoggerFactory()
    assert context.getName() == 'default', _CONTEXT_ASSERT_MSG % context.getName()
    logger = context.getLogger(Logger.ROOT_LOGGER_NAME)
    # for now I'll assume that there's only ONE appender per logger
    appender = [x for x in logger.iteratorForAppenders()].pop()
    assert isinstance(appender, FileAppender), _APPENDER_ASSERT_MSG % type(appender).getCanonicalName()
    return appender.getFile()

def deleteLogFile():
    """Removes the active log file."""
    os.remove(getLogFile())

def moveLogFile(destination):
    """Move the active log file to a given destination."""
    shutil.move(getLogFile(), _expandpath(destination))

def copyLogFile(destination):
    """Copies the active log files to a given destination."""
    shutil.copy2(getLogFile(), _expandpath(destination))

def ncdump(path, output_format='cdl', show_values='c', vars=None):
    """Print contents of a given netCDF file. 
    
    Please be aware that this output CANNOT (currently) be used as input for
    ncgen due to netCDF-Java limitations.
    
    Args:
        path: Path to an existing netCDF file.
        
        output_format: Optional. Understands 'cdl' and 'ncml'. Defaults to 'cdl'
        
        show_values: Optional. Understands 'c' and 'vall'. Defaults to 'c'.
        
        vars: Optional. Allows you to dump specified variable(s) or variable section(s).
        
    Returns:
        Nothing. However, you may want to try the 'ncdumpToString' function.
    """
    results = ncdumpToString(path, output_format, show_values, vars)
    if results:
        print results

def ncdumpToString(path, output_format='cdl', show_values='c', vars=None):
    """Returns contents of a given netCDF file as a string. 
    
    Please be aware that the resulting string CANNOT (currently) be used as 
    input for ncgen due to netCDF-Java limitations.
    
    Args:
        path: Path to an existing netCDF file.
        
        output_format: Optional. Understands 'cdl' and 'ncml'. Defaults to 'cdl'
        
        show_values: Optional. Understands 'c' and 'vall'. Defaults to 'c'.
        
        vars: Optional. Allows you to dump specified variable(s) or variable section(s).
        
    Returns:
        String representation of "ncdump" output (or an empty string if there
        was a problem within netCDF).
    """
    
    from jarray import array
    from java.io import IOException
    from java.io import StringWriter
    from ucar.nc2 import NCdumpW

    # build up commandline args to send off to netCDF-land (it wants 'em as a 
    # string for some reason)
    args = '%s -%s -%s ' % (path, output_format, show_values)
    if vars:
        for var in vars:
            args = '%s -v %s' % (args, var)

    writer = StringWriter()
    
    try:
        NCdumpW.print(args, writer, None)
    except IOException, ioe:
        print 'Error attempting to list contents of', path
        print ioe
    
    return writer.toString()

def dump_active_display():
    """ Returns the currently-active display object."""
    pass

# The dumpObj code has been adapted from 
# http://code.activestate.com/recipes/137951/
def printDict(di, format="%-25s %s"):
    for (key, val) in di.items():
        print format % (str(key)+':', val)

def dumpObj(obj, maxlen=77, lindent=24, maxspew=600):
    """Print a nicely formatted overview of an object.

    The output lines will be wrapped at maxlen, with lindent of space
    for names of attributes.  A maximum of maxspew characters will be
    printed for each attribute value.

    You can hand dumpObj any data type -- a module, class, instance,
    new class.

    Note that in reformatting for compactness the routine trashes any
    formatting in the docstrings it prints.

    Example:
       >>> class Foo(object):
               a = 30
               def bar(self, b):
                   "A silly method"
                   return a*b
       ... ... ... ... 
       >>> foo = Foo()
       >>> dumpObj(foo)
       Instance of class 'Foo' as defined in module __main__ with id 136863308
       Documentation string:   None
       Built-in Methods:       __delattr__, __getattribute__, __hash__, __init__
                               __new__, __reduce__, __repr__, __setattr__,       
                               __str__
       Methods:
         bar                   "A silly method"
       Attributes:
         __dict__              {}
         __weakref__           None
         a                     30
    """
    # Formatting parameters.
    ltab = 2    # initial tab in front of level 2 text
    
    # There seem to be a couple of other types; gather templates of them
    MethodWrapperType = type(object().__hash__)
    
    # Gather all the attributes of the object
    objclass = None
    objdoc = None
    objmodule = '<None defined>'
    
    methods = []
    builtins = []
    classes = []
    attrs = []
    for slot in dir(obj):
        attr = getattr(obj, slot)
        if slot == '__class__':
            objclass = attr.__name__
        elif slot == '__doc__':
            objdoc = attr
        elif slot == '__module__':
            objmodule = attr
        elif (isinstance(attr, types.BuiltinMethodType) or 
              isinstance(attr, MethodWrapperType)):
            builtins.append( slot )
        elif (isinstance(attr, types.MethodType) or
              isinstance(attr, types.FunctionType)):
            methods.append( (slot, attr) )
        elif isinstance(attr, types.TypeType):
            classes.append( (slot, attr) )
        else:
            attrs.append( (slot, attr) )
    
    # Organize them
    methods.sort()
    builtins.sort()
    classes.sort()
    attrs.sort()
    
    # Print a readable summary of those attributes
    normalwidths = [lindent, maxlen - lindent]
    tabbedwidths = [ltab, lindent-ltab, maxlen - lindent - ltab]
    
    def truncstring(s, maxlen):
        if len(s) > maxlen:
            return s[0:maxlen] + ' ...(%d more chars)...' % (len(s) - maxlen)
        else:
            return s
    
    # Summary of introspection attributes
    if objclass == '':
        objclass = type(obj).__name__
    if objclass is None:
        objclass = obj.__class__.__name__
    
    intro = "Instance of class '%s' as defined in module %s with id %d" % \
            (objclass, objmodule, id(obj))
    print '\n'.join(prettyPrint(intro, maxlen))
    
    # Object's Docstring
    if objdoc is None:
        objdoc = str(objdoc)
    else:
        objdoc = ('"""' + objdoc.strip()  + '"""')
    print
    print prettyPrintCols( ('Documentation string:',
                            truncstring(objdoc, maxspew)),
                          normalwidths, ' ')
    
    # Built-in methods
    if builtins:
        bi_str   = delchars(str(builtins), "[']") or str(None)
        print
        print prettyPrintCols( ('Built-in Methods:',
                                truncstring(bi_str, maxspew)),
                              normalwidths, ', ')
    
    # Classes
    if classes:
        print
        print 'Classes:'
    for (classname, classtype) in classes:
        classdoc = getattr(classtype, '__doc__', None) or '<No documentation>'
        print prettyPrintCols( ('',
                                classname,
                                truncstring(classdoc, maxspew)),
                              tabbedwidths, ' ')
    
    # User methods
    if methods:
        print
        print 'Methods:'
    for (methodname, method) in methods:
        methoddoc = getattr(method, '__doc__', None) or '<No documentation>'
        print prettyPrintCols( ('',
                                methodname,
                                truncstring(methoddoc, maxspew)),
                              tabbedwidths, ' ')
    
    # Attributes
    if attrs:
        print
        print 'Attributes:'
    for (attr, val) in attrs:
        print prettyPrintCols( ('',
                                attr,
                                truncstring(str(val), maxspew)),
                              tabbedwidths, ' ')

def prettyPrintCols(strings, widths, split=' '):
    """Pretty prints text in colums, with each string breaking at
    split according to prettyPrint.  margins gives the corresponding
    right breaking point."""
    
    assert len(strings) == len(widths)
    
    strings = map(nukenewlines, strings)
    
    # pretty print each column
    cols = [''] * len(strings)
    for i in range(len(strings)):
        cols[i] = prettyPrint(strings[i], widths[i], split)
    
    # prepare a format line
    format = ''.join(['%%-%ds' % width for width in widths[0:-1]]) + '%s'
    
    def formatline(*cols):
        return format % tuple(map(lambda s: (s or ''), cols))
    
    # generate the formatted text
    return '\n'.join(map(formatline, *cols))

def prettyPrint(string, maxlen=75, split=' '):
    """Pretty prints the given string to break at an occurrence of
    split where necessary to avoid lines longer than maxlen.

    This will overflow the line if no convenient occurrence of split
    is found"""
    
    # Tack on the splitting character to guarantee a final match
    string += split
    
    lines = []
    oldeol = 0
    eol = 0
    while not (eol == -1 or eol == len(string)-1):
        eol = string.rfind(split, oldeol, oldeol+maxlen+len(split))
        lines.append(string[oldeol:eol])
        oldeol = eol + len(split)
    
    return lines

def nukenewlines(string):
    """Strip newlines and any trailing/following whitespace; rejoin
    with a single space where the newlines were.
    
    Bug: This routine will completely butcher any whitespace-formatted
    text."""
    if not string:
        return ''
    lines = string.splitlines()
    return ' '.join([line.strip() for line in lines])

def delchars(str, chars):
    """Returns a string for which all occurrences of characters in
    chars have been removed."""
    # Translate demands a mapping string of 256 characters;
    # whip up a string that will leave all characters unmolested.
    identity = ''.join([chr(x) for x in range(256)])
    return str.translate(identity, chars)

def javaInstanceMethods(clazz):
    """Returns names of instance methods for a given Java class."""
    names = set()
    for method in Class.getDeclaredMethods(clazz):
        modifiers = method.getModifiers()
        if not Modifier.isStatic(modifiers) and Modifier.isPublic(modifiers):
            name = method.name
            names.add(name)
            if name.startswith('get') and len(name) > 3 and not method.getParameterTypes():
                property_name = name[3].lower() + name[4:]
                names.add(property_name)
    for base in clazz.__bases__:
        if not ispython(base):
            names = names | javaInstanceMethods(base)
    return names

def javaStaticMethods(clazz):
    """Returns names of static methods for a given Java class."""
    static_methods = {}
    for method in Class.getDeclaredMethods(clazz):
        modifiers = method.getModifiers()
        if Modifier.isStatic(modifiers) and Modifier.isPublic(modifiers):
            static_methods[method.name] = method
    methods = static_methods.keys()
    for base in clazz.__bases__:
        if not ispython(base):
            methods.extend(javaStaticMethods(base))
    return methods

def javaStaticFields(clazz):
    """Returns names of static fields for a given Java class."""
    static_fields = {}
    for field in Class.getDeclaredFields(clazz):
        modifiers = field.getModifiers()
        if Modifier.isStatic(modifiers) and Modifier.isPublic(modifiers):
            static_fields[field.name] = field
    fields = static_fields.keys()
    for base in clazz.__bases__:
        if not ispython(base):
            fields.extend(javaStaticFields(base))
    return fields

def ispython22(object):
    """Determine whether or not the object is Python (2.2.*) code."""
    object_type = type(object)
    if object_type.__name__.startswith('java') or isinstance(object, PyReflectedFunction):
        python = False
    elif object_type is types.MethodType:
        try:
            object.__dict__
            python = True
        except AttributeError:
            python = False
    else:
        # assume rest is python
        python = True
    
    return python

def ispython25(object):
    """Determine whether or not the object is Python (2.5.*) code."""
    if isinstance(object, Class):
        python = False
    elif isinstance(object, Object):
        python = False
    elif isinstance(object, PyReflectedFunction):
        python = False
    elif type(object) == types.MethodType and not ispython(object.im_class):
        python = False
    else:
        python = True
    return python

if sys.version.startswith('2.5'):
    ispython = ispython25
else:
    ispython = ispython22

# start of code lifted (and modified) from the wonderful see.py 
# original: https://github.com/inky/see
# modifications consist of Python 2.2 compatibilty and Java enhancements

def regex_filter(names, pat):
    pat = re.compile(pat)
    def match(name, fn=pat.search):
        return fn(name) is not None
    return tuple(filter(match, names))


def fn_filter(names, pat):
    def match(name, fn=fnmatch.fnmatch, pat=pat):
        return fn(name, pat)
    return tuple(filter(match, names))

class _SeeOutput(tuple):
    """Tuple-like object with a pretty string representation."""

    def __new__(self, actions=None):
        return tuple.__new__(self, actions or [])

    def __repr__(self):
        def sorted_lengths(t):
            tmp = map(len, list(t))
            tmp.sort()
            return tmp
        
        #lens = sorted(map(len, self)) or [0]
        lens = sorted_lengths(self)
        most = lens[int(len(lens)*0.9)]
        if lens[-1] + 6 > most:
            max_len = most
        else:
            max_len = lens[-1]

        def justify(i):
            if len(i) <= max_len + 2:
                return i.ljust(max_len + 4)
            else:
                return i.ljust(max_len * 2 + 8)

        padded = [justify(i) for i in self]
        if 'ps1' in dir(sys):
            indent = ' ' * len(sys.ps1)
        else:
            indent = '    '
        return textwrap.fill(''.join(padded), 78,
                             initial_indent=indent,
                             subsequent_indent=indent)


class _SeeDefault(object):
    def __repr__(self):
        return 'anything'

_LOCALS = _SeeDefault()

def see(obj=_LOCALS, pattern=None, r=None):
    """
    Inspect an object. Like the dir() builtin, but easier on the eyes.

    Keyword arguments (all optional):
    obj -- object to be inspected
    pattern -- shell-style search pattern (e.g. '*len*')
    r -- regular expression

    If obj is omitted, objects in the current scope are listed instead.

    Some unique symbols are used:

        .*      implements obj.anything
        []      implements obj[key]
        in      implements membership tests (e.g. x in obj)
        +obj    unary positive operator (e.g. +2)
        -obj    unary negative operator (e.g. -2)

    """
    use_locals = obj is _LOCALS
    actions = []
    dot = not use_locals and '.' or ''
    func = lambda f: hasattr(f, '__call__') and '()' or ''
    name = lambda a, f: ''.join((dot, a, func(f)))

    if use_locals:
        obj.__dict__ = inspect.currentframe().f_back.f_locals
    attrs = dir(obj)
    if not use_locals:
        for var, symbol in SYMBOLS:
            if var not in attrs or symbol in actions:
                continue
            elif var == '__doc__':
                if not obj.__doc__ or not obj.__doc__.strip():
                    continue
            actions.append(symbol)

    for attr in filter(lambda a: not a.startswith('_'), attrs):
        try:
            prop = getattr(obj, attr)
        except AttributeError:
            continue
        except TypeError:
            continue
        actions.append(name(attr, prop))

    if pattern is not None:
        actions = fn_filter(actions, pattern)
    if r is not None:
        actions = regex_filter(actions, r)

    return _SeeOutput(actions)


PY_300 = sys.version_info >= (3, 0)
PY_301 = sys.version_info >= (3, 0, 1)

SYMBOLS = tuple(filter(lambda x: x[0], (
    # callable
    ('__call__', '()'),

    # element/attribute access
    ('__getattr__', '.*'),
    ('__getitem__', '[]'),
    ('__setitem__', '[]'),
    ('__delitem__', '[]'),

    # iteration
    ('__enter__', 'with'),
    ('__exit__', 'with'),
    ('__contains__', 'in'),

    # operators
    ('__add__', '+'),
    ('__radd__', '+'),
    ('__iadd__', '+='),
    ('__sub__', '-'),
    ('__rsub__', '-'),
    ('__isub__', '-='),
    ('__mul__', '*'),
    ('__rmul__', '*'),
    ('__imul__', '*='),
    (not PY_300 and '__div__', '/'),
    (not PY_301 and '__rdiv__', '/'),
    ('__truediv__', '/'),
    ('__rtruediv__', '/'),
    ('__floordiv__', '//'),
    ('__rfloordiv__', '//'),
    (not PY_300 and '__idiv__', '/='),
    ('__itruediv__', '/='),
    ('__ifloordiv__', '//='),
    ('__mod__', '%'),
    ('__rmod__', '%'),
    ('__divmod__', '%'),
    ('__imod__', '%='),
    ('__pow__', '**'),
    ('__rpow__', '**'),
    ('__ipow__', '**='),
    ('__lshift__', '<<'),
    ('__rlshift__', '<<'),
    ('__ilshift__', '<<='),
    ('__rshift__', '>>'),
    ('__rrshift__', '>>'),
    ('__irshift__', '>>='),
    ('__and__', '&'),
    ('__rand__', '&'),
    ('__iand__', '&='),
    ('__xor__', '^'),
    ('__rxor__', '^'),
    ('__ixor__', '^='),
    ('__or__', '|'),
    ('__ror__', '|'),
    ('__ior__', '|='),
    ('__pos__', '+obj'),
    ('__neg__', '-obj'),
    ('__invert__', '~'),
    ('__lt__', '<'),
    (not PY_301 and '__cmp__', '<'),
    ('__le__', '<='),
    (not PY_301 and '__cmp__', '<='),
    ('__eq__', '=='),
    (not PY_301 and '__cmp__', '=='),
    ('__ne__', '!='),
    (not PY_301 and '__cmp__', '!='),
    ('__gt__', '>'),
    (not PY_301 and '__cmp__', '>'),
    ('__ge__', '>='),
    (not PY_301 and '__cmp__', '>='),

    # built-in functions
    ('__abs__', 'abs()'),
    (PY_300 and '__bool__' or '__nonzero__', 'bool()'),
    ('__complex__', 'complex()'),
    (PY_300 and '__dir__', 'dir()'),
    ('__divmod__', 'divmod()'),
    ('__rdivmod__', 'divmod()'),
    ('__float__', 'float()'),
    ('__hash__', 'hash()'),
    ('__doc__', 'help()'),
    (PY_300 and '__index__' or '__hex__', 'hex()'),
    ('__int__', 'int()'),
    ('__iter__', 'iter()'),
    ('__len__', 'len()'),
    (not PY_300 and '__long__', 'long()'),
    (PY_300 and '__index__' or '__oct__', 'oct()'),
    ('__repr__', 'repr()'),
    ('__reversed__', 'reversed()'),
    (PY_300 and '__round__', 'round()'),
    ('__str__', 'str()'),
    (PY_300 and '__unicode__', 'unicode()'),
)))
# end of see.py

# all the junk that *currently* gets automatically placed into the namespace 
# by the idv shell:
# 
# ArrayList ConstantMap Data DataChoice DataSelection DataUtil 
# DerivedGridFactory Display EARTH_RADIUS EntryStore FFFffff FieldImpl 
# FileOutputStream GRAVITY GeoLocationInfo GeoSelection GridMath GridUtil 
# Integer JyVars MapMaker MultiSpectralData RemoteAddeEntry Selector 
# StreamResult StreamSource StringUtil TWO_OMEGA Transformer 
# TransformerFactory Util _KWARG_ALIASES __doc__ __name__ _cnames _color_cache 
# _convertHexColor _convertNamedColor _convertRgbSeq _extract_kwarg _idv 
# _initWavenumber _myip _namedBands _s abs abs_data acos acosDegrees add adv 
# age annotate applyFunctionToValuesInField applyFunctionToValuesInRange 
# applyToIndices applyToRange applyToRangeValues apply_colorbar array asin 
# asinDegrees atan atan2 atan2Degrees atanDegrees atn2 averageFromMap 
# averageFromMapAndClip averageOverTime avg avor bands ceil chol cleanFilter 
# clear clearplot cloneArray cloudFilter collect_garbage colorutils combine 
# combineFields combineRGB cond contrast convertColor coreFilter corl cos 
# cosDegrees createDisplay cros datamanager ddx ddy default defr det 
# differenceFromBaseTime dirn dirr disableAddeDebug discriminateFilter div 
# domainDimension domainFactor domainType dot dumpType dumpTypes 
# dump_active_display dvdx dvdy eig enableAddeDebug evaluate exp 
# exportGridToNetcdf extract extractLatitudeFromNWPGrid 
# extractPressureFromNWPGrid fft field filterMaps filters find findDataSource 
# findDisplayControl floor frnt geo geolocate getAltitude getData 
# getDataChoice getDomain getDomainDimension getDomainSet getDomainSizes 
# getDomainType getImage getMapProperty getMapsWithProperty getMinMax 
# getNetcdfData getNthTimeGrid getPointDataAdapter getRangeDimension 
# getRangeType getRealType getScale getSliceAtAltitude getSliceAtLevel getType 
# getValue getValues getValuesLengths grad gradientFilter gwfs gwfv 
# highPass2DFilter hist holeFilter horizontalAdvection horizontalDivergence 
# idv idveval idvutils ifft inad installmanager interpreter inverse 
# islInterpreter java jcbn lap latr lav layerAverage layerDiff ldf listVars 
# load loadBundle load_enhancement load_map log lowPass2DFilter lu mag make2D 
# make3DMap makeDataSource makeDomain makeField makeFieldFromMapBounds 
# makeFloatArray makeFlowField makeNavigatedImage makePairedLines makeRealType 
# makeTimeSequence makeTopographyFromField makeTrueVector makeType makeUnit 
# makeVector mapsAbsoluteValue mapsApplyToField mapsApplyToRange mapsAverage 
# mapsMax mapsMin mapsSetValue mask maskGrid matrixMultiply matteImage 
# maxOverTime max_data mergeFilter mergeTimeSequences minOverTime min_data 
# mixr mod modify mul myCloudzz mycombineRGB newName newUnit noUnit norm1 
# norm2 normF normInf oldaverageOverTime os passFilter pause plot 
# printSounding printSoundings printType printValueDiff pvor qr quo qvcl qvec 
# rangeDimension rangeType rank recenter relh replace replaceFilter 
# replaceMissing resample resampleGrid rescale resizeImage rint round 
# saveNetcdf saveplot savg savs sdiv selectData selectDataChoice selector 
# setAxesScalesFont setAxesScalesLabel setDataChoices setDataSources setDebug 
# setOffScreen setValues shell shotFilter showAxesScales showLib shr sin 
# sinDegrees sm5s sm5v sm9s sm9v solve spotFilter sqrt strd sub subsetFromMap 
# subsetRangeFromMap subsetRangeWithProperty subsetWithProperty substitute 
# sumFromBaseTime sumOverTime svd sys tan tanDegrees testIndexToLatLon 
# testIndexToLatLon2 thrm thta thte timeStepDifference timeStepSum tmpBundle 
# tmpOut tmpXsl trace transform transpose un uniqueID ur vadd vecn vecr 
# verifyServer vlav vldf vmul vn vor vquo vr vsub windShear windShearVector 
# writeGridToXls writeImage writeImageToFile writeMovie write_image wshr xav 
# xsum yav ysum

# Mike's junk comment to test CVS commit notification
