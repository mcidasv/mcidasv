"""Utility functions to ease working in the Jython Shell."""

# python imports
import datetime
import fnmatch
import inspect
import re
import os
import shutil
import sys
import textwrap
import types

from decorators import deprecated

# heinous java imports! boo! hiss!
from ch.qos.logback.core import FileAppender
from ch.qos.logback.classic import LoggerContext

from ucar.unidata.idv.ui.JythonShell import PROP_JYTHON_SHELL_TRUNCATE

from edu.wisc.ssec.mcidasv.McIDASV import getStaticMcv
from edu.wisc.ssec.mcidasv.util import JythonObjectStore

from java.lang import Class
from java.lang import Object
from java.lang.reflect import Modifier

from org.python.core import PyReflectedFunction

from org.slf4j import Logger
from org.slf4j import LoggerFactory

_CONTEXT_ASSERT_MSG = "expected 'default' context; got '%s'"
_APPENDER_ASSERT_MSG = "expected appender to be a subclass of FileAppender; got '%s'"
_CONTEXT_FIND_LOGGER = "expected logger '%s' to be added to default context"
_BAD_LOGGERNAME = "Cannot save log level if loggerName is not 'ROOT' (given loggerName is '%s')."

def checkLocalServerStatus():
    """Check the status of mcservl.
    
    Returns:
        True if things are working as expected, False if mcservl does not have
        write access to the userpath.
        
    Raises:
        RuntimeError: if getStaticMcv() fails (which should not happen).
    """
    mcv = getStaticMcv()
    if mcv:
        return mcv.getServerManager().testLocalServer()
    raise RuntimeError('Could not get reference to McIDAS-V!')
        
def editFile(path, encoding=None, cleanup=False):
    """Import file contents into the Jython Shell input field.
    
    Args:
        path: Required string value that represents a path to a file. The string
              is validated with expandpath, so paths like "~/test.py" will work.
              
        encoding: Optional string value that defaults to None. If given a value,
                  no attempt will be made to check the encoding of the given
                  file.
              
        cleanup: Optional boolean value that defaults to False. If set to True,
                 calls to removeAllData() and removeAllLayers() are added to the 
                 beginning of the Jython Shell input text field.
    """
    from edu.wisc.ssec.mcidasv.util import DetectCharset
    import codecs
    
    filepath = expandpath(path)
    encoding = encoding or DetectCharset.detect(filepath)
    
    # detection may have failed
    if not encoding:
        raise RuntimeError('Could not detect encoding of "%s". Please verify the file\'s encoding and then run "editFile(\'%s\', encoding=\'ENCODING_HERE\', cleanup=%s)".' % (path, path, cleanup))
        
    fp = codecs.open(filepath, 'r', encoding=encoding)
    try:
        shell = getStaticMcv().getJythonManager().getShell()
        lines = u''
        if cleanup:
            lines += u'# removeAllData and removeAllLayers were added because editFile was called with "cleanup" set to True.\nremoveAllData()\nremoveAllLayers()\n\n'
        once = False
        for line in fp:
            lines += line
        shell.setMultilineText(lines)
    except UnicodeDecodeError:
        detected = DetectCharset.detect(filepath)
        raise RuntimeError('Could not decode contents of "%s" using "%s"! You may want to retry by running "editFile(\'%s\', encoding=\'%s\', cleanup=%s)".' % (path, encoding, path, detected, cleanup))
    finally:
        fp.close()
        
def detectEncoding(path):
    """Attempt automatic detection of the given file's encoding.
    
    Returns:
        Either the encoding as a string (suitable for passing to editFile or 
        codecs.open), or None if the automatic detection failed.
    """
    from edu.wisc.ssec.mcidasv.util import DetectCharset
    return str(DetectCharset.detect(expandpath(path)))
        
def today(dateFormat=None):
    """Return today's date in either the user's specified format, or YYYYDDD (default)."""
    dateFormat = dateFormat or '%Y%j'
    return datetime.date.today().strftime(dateFormat)
    
def tomorrow(dateFormat=None):
    """Return tomorrow's date in either the user's specified format, or YYYYDDD (default)."""
    dateFormat = dateFormat or '%Y%j'
    return (datetime.date.today() + datetime.timedelta(days=1)).strftime(dateFormat)
    
def yesterday(dateFormat=None):
    """Return yesterday's date in either the user's specified format, or YYYYDDD (default)."""
    dateFormat = dateFormat or '%Y%j'
    return (datetime.date.today() - datetime.timedelta(days=1)).strftime(dateFormat)
    
def expandPath(path, *paths):
    """Expand ENV variables, fixes things like '~', and then normalizes the given path."""
    return os.path.normpath(os.path.join(os.path.expanduser(os.path.expandvars(path)), *paths))
    
@deprecated(today)
def _today(dateFormat=None):
    return today(dateFormat)
    
@deprecated(tomorrow)
def _tomorrow(dateFormat=None):
    return _tomorrow(dateFormat)
    
@deprecated(yesterday)
def _yesterday(dateFormat=None):
    return _yesterday(dateFormat)
    
@deprecated(expandPath)
def expandpath(path, *paths):
    return expandPath
    
@deprecated(expandPath)
def _expandpath(path, *paths):
    return expandPath
    
def getUserPath():
    """Return path to the user's McIDAS-V directory."""
    return getStaticMcv().getStore().getUserDirectory().getPath()
    
def getMouseEarthLocation():
    """Return position of the mouse pointer as a lat/lon tuple."""
    display = getStaticMcv().getVMManager().getLastActiveViewManager()
    master = display.getMaster()
    visadLat, visadLon = master.getCursorLatitude(), master.getCursorLongitude()
    return visadLat.getValue(), visadLon.getValue()
    
def describeActions(pattern=None):
    """Print out a list of the McIDAS-V actions.
    
    The output is ordered alphabetically and grouped by functionality. Each
    identifier can be "run" like so:
    
    performAction(identifier)
    performAction('edit.paramdefaults')
    
    Args:
        pattern: Searches for the given pattern within the action identifier
                 strings as well as action descriptions.
    """
    # actions = _mcv.getIdvUIManager().getCachedActions().getAllActions()
    actions = getStaticMcv().getIdvUIManager().getCachedActions().getAllActions()
    print sorted([action.getId() for action in actions])
    
def getLogFile():
    """Return the file path of the McIDAS-V log file."""
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
    
def getLogLevel(loggerName='ROOT'):
    """Return log level.
    
    Optional Args:
        loggerName: Name of a specific 'logger'. Default value is 'ROOT'.
        
    Returns:
        Dictionary containing both the 'level' and 'effectiveLevel' of the 
        given logger.
    """
    logger = LoggerFactory.getLogger(loggerName)
    if logger.getLevel():
        level = str(logger.getLevel())
    else:
        level = None
        
    if logger.getEffectiveLevel():
        effectiveLevel = str(logger.getEffectiveLevel())
    else:
        effectiveLevel = None
        
    return { 'level': level, 'effectiveLevel': effectiveLevel }
    
def setLogLevel(level, loggerName='ROOT', temporary=True):
    """Set the log level for the given logger.
    
    Args:
        level: Logging level to set. Valid levels are 'TRACE', 'DEBUG', 'INFO',
               'WARN', 'ERROR', and 'OFF'.
                
    Optional Args:
        loggerName: Name of a specific 'logger'. Default value is 'ROOT'.
        temporary: Whether or not the logging level should be saved between
                   McIDAS-V sessions. Be aware that if set to True, loggerName
                   must be 'ROOT'. Default value is True.
                   
    Raises:
        ValueError: if temporary is True and loggerName is not 'ROOT'.
    """
    if not temporary:
        if loggerName != 'ROOT':
            raise ValueError(_BAD_LOGGERNAME % (loggerName))
        
        from edu.wisc.ssec.mcidasv.startupmanager.options import OptionMaster
        optMaster = OptionMaster.getInstance()
        optMaster.getLoggerLevelOption("LOG_LEVEL").setValue(level)
        optMaster.writeStartup()
        
    context = LoggerFactory.getILoggerFactory()
    logger = context.exists(loggerName)
    if not logger:
        logger = context.getLogger(loggerName)
    currentLevel = logger.getLevel()
    if not currentLevel:
        currentLevel = logger.getEffectiveLevel()
    convertedLevel = currentLevel.toLevel(level, currentLevel.INFO)
    logger.setLevel(convertedLevel)
    
def deleteLogFile():
    """Remove active log file."""
    os.remove(getLogFile())
    
def moveLogFile(destination):
    """Move active log file to a given destination."""
    shutil.move(getLogFile(), _expandpath(destination))
    
def copyLogFile(destination):
    """Copy active log files to a given destination."""
    shutil.copy2(getLogFile(), _expandpath(destination))
    
def ncdump(path, output_format='cdl', show_values='c', vars=None):
    """Print contents of a given netCDF file.
    
    Please be aware that this output CANNOT (currently) be used as input for
    ncgen due to netCDF-Java limitations.
    
    Args:
        path: Path to an existing netCDF file.
        
    Optional Args:
        output_format: Understands 'cdl' and 'ncml'. Defaults to 'cdl'.
        show_values: Understands 'c' and 'vall'. Defaults to 'c'.
        vars: Allows you to dump specified variable(s) or variable section(s).
        
    Returns:
        Nothing. However, you may want to try the 'ncdumpToString' function.
    """
    results = ncdumpToString(path, output_format, show_values, vars)
    if results:
        print results
        
def ncdumpToString(path, output_format='cdl', show_values='c', vars=None):
    """Return contents of a given netCDF file as a string.
    
    Please be aware that the resulting string CANNOT (currently) be used as
    input for ncgen due to netCDF-Java limitations.
    
    Args:
        path: Path to an existing netCDF file.
        
    Optional Args:
        output_format: Understands 'cdl' and 'ncml'. Defaults to 'cdl'.
        show_values: Understands 'c' and 'vall'. Defaults to 'c'.
        vars: Allows you to dump specified variable(s) or variable section(s).
        
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
    
# TODO(jon): consider making a JythonObjectStore context manager!
def disableTruncation():
    """Disable Jython Shell truncation."""
    store = JythonObjectStore.newInstance(getStaticMcv())
    store.putBoolean(PROP_JYTHON_SHELL_TRUNCATE, False)
    store = None
    
def enableTruncation():
    """Enable Jython Shell truncation."""
    store = JythonObjectStore.newInstance(getStaticMcv())
    store.putBoolean(PROP_JYTHON_SHELL_TRUNCATE, True)
    store = None
    
def truncation():
    """Return Jython Shell truncation status."""
    store = JythonObjectStore.newInstance(getStaticMcv())
    value = store.getBoolean(PROP_JYTHON_SHELL_TRUNCATE, True)
    store = None
    return value
    
def dump_active_display():
    """Return currently active display object."""
    pass
    
# The dumpObj code has been adapted from
# http://code.activestate.com/recipes/137951/
def printDict(di, format="%-25s %s"):
    """Print the contents of the given dictionary.
    
    Args:
        di: Dictionary to print.
        
    Optional Args:
        format: Formatting string to use when printing. Default value is '%-25s %s'.
    """
    for (key, val) in di.items():
        print format % (str(key)+':', val)
        
def dumpObj(obj, maxlen=77, lindent=24, maxspew=600):
    """Print a nicely formatted overview of an object.

    The output lines will be wrapped at maxlen, with lindent of space for 
    names of attributes.  A maximum of maxspew characters will be printed for 
    each attribute value.

    You can hand dumpObj any data type -- a module, class, instance, new class.

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
        elif (isinstance(attr, types.BuiltinMethodType) or isinstance(attr, MethodWrapperType)):
            builtins.append(slot)
        elif (isinstance(attr, types.MethodType) or isinstance(attr, types.FunctionType)):
            methods.append((slot, attr))
        elif isinstance(attr, types.TypeType):
            classes.append((slot, attr))
        else:
            attrs.append((slot, attr))
            
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
        objdoc = ('"""' + objdoc.strip() + '"""')
    print
    print prettyPrintCols(('Documentation string:',
                            truncstring(objdoc, maxspew)),
                            normalwidths, ' ')
    # Built-in methods
    if builtins:
        bi_str = delchars(str(builtins), "[']") or str(None)
        print
        print prettyPrintCols(('Built-in Methods:',
                               truncstring(bi_str, maxspew)),
                               normalwidths, ', ')
    # Classes
    if classes:
        print
        print 'Classes:'
    for (classname, classtype) in classes:
        classdoc = getattr(classtype, '__doc__', None) or '<No documentation>'
        print prettyPrintCols(('',
                                classname,
                                truncstring(classdoc, maxspew)),
                                tabbedwidths, ' ')
    # User methods
    if methods:
        print
        print 'Methods:'
    for (methodname, method) in methods:
        methoddoc = getattr(method, '__doc__', None) or '<No documentation>'
        print prettyPrintCols(('',
                                methodname,
                                truncstring(methoddoc, maxspew)),
                                tabbedwidths, ' ')
    # Attributes
    if attrs:
        print
        print 'Attributes:'
    for (attr, val) in attrs:
        print prettyPrintCols(('',
                                attr,
                                truncstring(str(val), maxspew)),
                                tabbedwidths, ' ')
                                
def prettyPrintCols(strings, widths, split=' '):
    """Pretty prints text.
    
    Resulting output is organized into columns, with each string breaking at
    split according to prettyPrint. Margins gives the corresponding right 
    breaking point.
    """
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
    """Pretty print the given string.
    
    Breaks at an occurrence of split where necessary to avoid lines longer 
    than maxlen.
    
    This will overflow the line if no convenient occurrence of split
    is found.
    """
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
    """Strip newlines and any trailing/following whitespace.
    
    Rejoins with a single space where the newlines were.
    
    Bug: This routine will completely butcher any whitespace-formatted text.
    """
    if not string:
        return ''
    lines = string.splitlines()
    return ' '.join([line.strip() for line in lines])
    
def delchars(str, chars):
    """Return string for which all occurrences of characters in chars have been removed."""
    # Translate demands a mapping string of 256 characters;
    # whip up a string that will leave all characters unmolested.
    identity = ''.join([chr(x) for x in range(256)])
    return str.translate(identity, chars)
    
def javaInstanceMethods(clazz):
    """Return names of instance methods for a given Java class."""
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
    """Return names of static methods for a given Java class."""
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
    """Return names of static fields for a given Java class."""
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
    elif isinstance(object, types.MethodType):
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
    elif isinstance(object, types.MethodType) and not ispython(object.im_class):
        python = False
    else:
        python = True
    return python
    
if sys.version.startswith('2.5'):
    ispython = ispython25
else:
    ispython = ispython22
