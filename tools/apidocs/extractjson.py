import sys
try:
    sys.registry['python.security.respectJavaAccessibility'] = 'false'
except:
    print 'could not modify python.security.respectJavaAccessibility; current value:', sys.registry['python.security.respectJavaAccessibility']

import os
import types
import urllib

import StringIO

from java.lang import ClassLoader
from java.net import URL

import java.lang.Class
import java.lang.System

mcvPyRoot = 'edu/wisc/ssec/mcidasv/resources/python'
idvPyRoot = 'idv/ucar/unidata/idv/resources/python'
visadPyRoot = 'visad/core/src/visad/python'

mcvJars = ['lib/commons-math-2.2.jar', 'lib/eventbus-1.3.jar', 'lib/log4j-over-slf4j-1.6.1.jar', 'lib/logback-classic-0.9.29.jar', 'lib/logback-core-0.9.29.jar', 'lib/miglayout-3.7.3.jar', 'lib/slf4j-api-1.6.1.jar', 'dist/mcidasv.jar']
idvJars = ['idv/lib/auxdata.jar', 'idv/lib/commons-net-1.4.1.jar', 'idv/lib/doclint.jar', 'idv/lib/dummy.jar', 'idv/lib/external.jar', 'idv/lib/extra.jar', 'idv/lib/j2h.jar', 'idv/lib/jcommon.jar', 'idv/lib/junit.jar', 'idv/lib/ncIdv.jar', 'idv/lib/repositorytds.jar', 'idv/lib/servlet-api.jar', 'idv/lib/texttonc.jar', 'idv/lib/unidatacommon.jar', 'idv/lib/idv.jar']

# NOTE: ".py" file extension not needed!
mcvModules = {
    # 'Image Filters': 'imageFilters',
    # 'Background Scripting': 'background',
    # 'Interactive Functions': 'interactive',
    'Background Scripting': 'background',
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

def _expandpath(path):
    return os.path.abspath(os.path.normpath(os.path.expanduser(os.path.expandvars(path))))

def _addJarFiles(path, jarFiles):
    path = _expandpath(path)
    for jarFile in jarFiles:
        jarPath = os.path.join(path, jarFile)
        if os.path.exists(jarPath):
            sys.classpath.append(jarPath)
        else:
            print '*** warning: invalid path to jar:', jarPath

def _addSysPath(path):
    """Expands ENV variables, fixes things like '~', and then normalizes the
    given path."""
    path = _expandpath(path)
    if not os.path.exists(path):
        print '*** warning: invalid path:', path
    else:
        sys.classpath.append(path)

def _joinSysPath(prefix, subpath):
    _addSysPath(os.path.join(prefix, subpath))

# classloader magic comes from the amazing jynx project:
# http://www.fiber-space.de/jynx/doc/index.html
classpathsep = java.lang.System.getProperty("path.separator")
classpath = java.lang.System.getProperty("java.class.path")
javahome  = java.lang.System.getProperty("java.home")

def pathname2url(pth):
    """
    Transform file system path into URL using urllib.pathname2url().
    
    Additional changes:

    Undo replacement of ':' by '|' on WinNT. Append '/' to URLs stemming from directories.
    """
    url = urllib.pathname2url(pth)
    if classpathsep == ";": # NT
        url = url.replace("|", ":", 1)
    if os.path.isdir(pth) and url[-1]!="/":
        url+="/"
    return url

class InstallationError(Exception):
    pass

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
                                    "                            Also set registry option 'python.security.respectJavaAccessibility' to false.\n"
                                    "                            Consider running this script like so:\n"
                                    "                            jython -Dpython.security.respectJavaAccessibility=false extractjson.py")
        self._path.append(pth)
        sys.path.append(pth)
    
    def __repr__(self):
        return classpathsep.join(self._path)

# define new system variable
sys.classpath = ClassPath()
# end of jclasspath stuff

class _NoOp(object):
    def __repr__(self):
        return 'anything'
_NOOP = _NoOp()

def dumpClass(clazz):
    if not isinstance(clazz, types.TypeType):
        raise TypeError()
    
    docCollection = {}
    for slot in dir(clazz):
        attr = getattr(clazz, slot)
        if isinstance(attr, types.MethodType):
            methodName = getattr(attr, '__name__')
            methodDoc = getattr(clazz, '__doc__', None) or ''
            methodDoc = methodDoc.replace('\n', '<br/>').replace('\"', '&quot;')
            docCollection[methodName] = methodDoc
    
    return docCollection

def dumpObj(obj):
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
    
    
    # There seem to be a couple of other types; gather templates of them
    MethodWrapperType = type(object().__hash__)
    
    # Gather all the attributes of the object
    objclass = None
    objdoc = None
    objmodule = '<None defined>'
    
    functions = []
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
        elif slot == '__module__' or isinstance(attr, types.ModuleType):
            objmodule = attr.__name__
        elif isinstance(attr, types.BuiltinMethodType) or isinstance(attr, MethodWrapperType):
            builtins.append(slot)
        elif isinstance(attr, types.MethodType):
            methods.append((slot, attr))
        elif isinstance(attr, types.FunctionType):
            functions.append((slot, attr))
        elif isinstance(attr, types.TypeType) and not isinstance(attr, java.lang.Class):
            classes.append((slot, attr))
        else:
            attrs.append((slot, attr))
    
    # Summary of introspection attributes
    if objclass == '':
        objclass = type(obj).__name__
    if objclass is None:
        objclass = obj.__class__.__name__
    print '"namespace": "%s",' % (obj.__name__),
    
    classOutput = StringIO.StringIO()
    for className, classType in sorted(classes):
        classDoc = getattr(classType, '__doc__', None) or ''
        methodDocs = dumpClass(classType)
        methodOutput = StringIO.StringIO()
        for methodName in sorted(methodDocs):
            methodOutput.write('{"title": "%s", "value": "%s"},' %  (methodName, methodDocs[methodName]))
        methodOutput = methodOutput.getvalue()[:-1]
        classOutput.write('{"title":"%s","value":"%s","method": [ %s ], "attribute": [ ] },' % (className, classDoc.replace('\n', '<br/>').replace('\"', '&quot;'), methodOutput))
    print '"class": [ %s ],' % (classOutput.getvalue()[:-1])
    classOutput.close()
    
    functionOutput = StringIO.StringIO()
    for funcName, func in sorted(functions):
        funcDoc = getattr(func, '__doc__', None) or ''
        functionOutput.write('{"title":"%s","value":"%s"},' % (funcName, funcDoc.replace('\n', '<br/>').replace('\"', '&quot;')))
    print '"function": [ %s ],' % (functionOutput.getvalue()[:-1])
    functionOutput.close()
    
    # Attributes
    attrOutput = StringIO.StringIO()
    for attr, val in sorted(attrs):
        attrOutput.write('{"title":"%s","value":"%s"},' % (attr, val))
    print '"attribute": [ %s ]' % (attrOutput.getvalue()[:-1])
    attrOutput.close()

def processModules(modules):
    for title, moduleName in sorted(modules.iteritems()):
        try:
            globals()[moduleName] = __import__(moduleName, globals(), locals(), ['*'], -1)
            dumpObj(globals()[moduleName])
        except ImportError:
            print '*** could not import', moduleName

def configureSysPath():
    argvpath, script = os.path.split(sys.argv[0])
    if argvpath.endswith('tools/apidocs'):
        mcvPathPrefix = '../..'
        idvPathPrefix = '../../..'
        visadPathPrefix = '../../..'
    else:
        raise OSError() # TODO(jon): plz to making better !!
    
    mcvPath = os.path.join(mcvPathPrefix, mcvPyRoot)
    idvPath = os.path.join(idvPathPrefix, idvPyRoot)
    visadPath = os.path.join(visadPathPrefix, visadPyRoot)
    
    # print 'visad path prefix:', visadPathPrefix
    # print 'visad root       :', visadPyRoot
    # print 'visad path       :', visadPath
    # print
    # print 'idv path prefix  :', idvPathPrefix
    # print 'idv root         :', idvPyRoot
    # print 'idv path         :', idvPath
    # print
    # print 'mcv path prefix  :', mcvPathPrefix
    # print 'mcv root         :', mcvPyRoot
    # print 'mcv path         :', mcvPath
    # print
    
    # visad imports
    _joinSysPath(idvPathPrefix, 'idv/lib/visad.jar')
    _addSysPath(visadPath)
    
    # idv imports
    _addJarFiles(idvPathPrefix, idvJars)
    _addSysPath(idvPath)
    _joinSysPath(idvPath, '..')
    
    # mcv imports
    _addJarFiles(mcvPathPrefix, mcvJars)
    _addSysPath(mcvPath)
    _joinSysPath(mcvPath, 'linearcombo')
    _joinSysPath(mcvPath, 'utilities')
    
    # for path in sys.path:
    #     if os.path.exists(path):
    #         print '(valid)\t', path
    #     else:
    #         print '(bad)\t', path
    
    print '{ "contents": [ {'
    processModules(mcvModules)
    print '} ] }'
    #processModules(idvModules)
    #processModules(visadModules)

if __name__ == '__main__':
    configureSysPath()