import os
import sys
import types
import urllib

from java.lang import ClassLoader
from java.net import URL
import java.lang.System

try:
    sys.registry['python.security.respectJavaAccessibility'] = False
except:
    print 'could not modify python.security.respectJavaAccessibility; current value:', sys.registry['python.security.respectJavaAccessibility']

mcvPyRoot = 'edu/wisc/ssec/mcidasv/resources/python'
idvPyRoot = 'idv/ucar/unidata/idv/resources/python'
visadPyRoot = 'visad/core/src/visad/python'

# NOTE: ".py" file extension not needed!
mcvModules = {
    'Image Filters': 'imageFilters',
    'Background Scripting': 'background',
    'Interactive Functions': 'interactive',
}
idvModules = {
    'Climate': 'climate',
    'Constants': 'constants',
    'Ensemble': 'ensemble',
    'Grid': 'grid',
    'Grid Diagnostics': 'griddiag',
    'Image': 'image',
    'Maps': 'maps',
    'Misc': 'misc',
    'Shell': 'shell',
    'Test': 'test',
}
visadModules = {
    'VisAD Helpers': 'subs',
}

def _addSysPath(path):
    """Expands ENV variables, fixes things like '~', and then normalizes the
    given path."""
    path = os.path.abspath(os.path.normpath(os.path.expanduser(os.path.expandvars(path))))
    sys.classpath.append(path)

def _joinSysPath(prefix, subpath):
    _addSysPath(os.path.join(prefix, subpath))

# classloader magic comes from the amazing jynx project:
# http://www.fiber-space.de/jynx/doc/index.html
classpathsep = java.lang.System.getProperty("path.separator")
classpath = java.lang.System.getProperty("java.class.path")
javahome  = java.lang.System.getProperty("java.home")

def pathname2url(pth):
    '''
    Transform file system path into URL using urllib.pathname2url().

    Additional changes:

    Undo replacement of ':' by '|' on WinNT. Append '/' to URLs stemming from directories.
    '''
    url = urllib.pathname2url(pth)
    if classpathsep == ";": # NT
        url = url.replace("|", ":", 1)
    if os.path.isdir(pth) and url[-1]!="/":
        url+="/"
    return url

class InstallationError(Exception):pass

class ClassPath(object):
    _instance = None
    def __init__(self):
        if ClassPath._instance:
            self.__dict__.update(ClassPath._instance.__dict__)
        else:
            if 'CLASSPATH' in os.environ:
                self._path = classpath.split(classpathsep)
            else:
                raise InstallationError("Cannot find CLASSPATH environmment variable")
            self._stdloader = ClassLoader.getSystemClassLoader()
            ClassPath._instance = self

    def append(self, pth):
        try:
            self._stdloader.addURL(URL("file:"+pathname2url(pth)))
        except AttributeError:
            raise InstallationError("Make sure that Jython registry file is in the directory of jython.jar\n"
                                    "                   Also set registry option 'python.security.respectJavaAccessibility' to false.")
        self._path.append(pth)
        sys.path.append(pth)

    def __repr__(self):
        return classpathsep.join(self._path)

# define new system variable
sys.classpath = ClassPath()
# end of jclasspath stuff

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

def processModules(modules):
    for title, moduleName in sorted(modules.iteritems()):
        try:
            globals()[moduleName] = __import__(moduleName, globals(), locals(), ['*'], -1)
            print title
            print
            print dumpObj(moduleName)
        except ImportError:
            print '*** could not import', moduleName

def configureSysPath():
    basename = os.path.basename(os.getcwd())
    
    # attempt to work when running from both mcvPath and "mcvPath/tools/" 
    if basename == 'tools':
        mcvPathPrefix = '..'
        idvPathPrefix = '../..'
        visadPathPrefix = '../..'
    elif basename == 'mcv':
        mcvPathPrefix = '.'
        idvPathPrefix = '..'
        visadPathPrefix = '..'
    else:
        raise OSError() # TODO(jon): plz to making better !!
    
    mcvPath = os.path.join(mcvPathPrefix, mcvPyRoot)
    idvPath = os.path.join(idvPathPrefix, idvPyRoot)
    visadPath = os.path.join(visadPathPrefix, visadPyRoot)
    
    print 'visad path prefix:', visadPathPrefix
    print 'visad root       :', visadPyRoot
    print 'visad path       :', visadPath
    print
    print 'idv path prefix  :', idvPathPrefix
    print 'idv root         :', idvPyRoot
    print 'idv path         :', idvPath
    print
    print 'mcv path prefix  :', mcvPathPrefix
    print 'mcv root         :', mcvPyRoot
    print 'mcv path         :', mcvPath
    print
    
    # visad imports
    _joinSysPath(idvPathPrefix, 'idv/lib/visad.jar')
    _addSysPath(visadPath)
    
    # idv imports
    _joinSysPath(idvPathPrefix, 'idv/lib/auxdata.jar')
    _joinSysPath(idvPathPrefix, 'idv/lib/commons-net-1.4.1.jar')
    _joinSysPath(idvPathPrefix, 'idv/lib/doclint.jar')
    _joinSysPath(idvPathPrefix, 'idv/lib/dummy.jar')
    _joinSysPath(idvPathPrefix, 'idv/lib/external.jar')
    _joinSysPath(idvPathPrefix, 'idv/lib/extra.jar')
    _joinSysPath(idvPathPrefix, 'idv/lib/j2h.jar')
    _joinSysPath(idvPathPrefix, 'idv/lib/jcommon.jar')
    _joinSysPath(idvPathPrefix, 'idv/lib/junit.jar')
    _joinSysPath(idvPathPrefix, 'idv/lib/ncIdv.jar')
    _joinSysPath(idvPathPrefix, 'idv/lib/repositorytds.jar')
    _joinSysPath(idvPathPrefix, 'idv/lib/servlet-api.jar')
    _joinSysPath(idvPathPrefix, 'idv/lib/texttonc.jar')
    _joinSysPath(idvPathPrefix, 'idv/lib/unidatacommon.jar')
    _joinSysPath(idvPathPrefix, 'idv/lib/idv.jar')
    _addSysPath(idvPath)
    _joinSysPath(idvPath, '..')
    
    # mcv imports
    _joinSysPath(mcvPathPrefix, 'lib/commons-math-2.2.jar')
    _joinSysPath(mcvPathPrefix, 'lib/eventbus-1.3.jar')
    _joinSysPath(mcvPathPrefix, 'lib/log4j-over-slf4j-1.6.1.jar')
    _joinSysPath(mcvPathPrefix, 'lib/logback-classic-0.9.29.jar')
    _joinSysPath(mcvPathPrefix, 'lib/logback-core-0.9.29.jar')
    _joinSysPath(mcvPathPrefix, 'lib/miglayout-3.7.3.jar')
    _joinSysPath(mcvPathPrefix, 'lib/slf4j-api-1.6.1.jar')
    _joinSysPath(mcvPathPrefix, 'dist/mcidasv.jar')
    _addSysPath(mcvPath)
    _joinSysPath(mcvPath, 'linearcombo')
    _joinSysPath(mcvPath, 'utilities')
    
    for path in sys.path:
        if os.path.exists(path):
            print '(valid)\t', path
        else:
            print '(bad)\t', path
    
    processModules(mcvModules)
    processModules(idvModules)
    processModules(visadModules)

if __name__ == '__main__':
    configureSysPath()