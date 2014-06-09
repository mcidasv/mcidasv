import sys
import warnings

from functools import wraps

from java.util.concurrent import Callable
from java.util.concurrent import FutureTask

from javax.swing import SwingUtilities

from visad import Field
from visad import FlatField
from visad.meteorology import ImageSequenceImpl
from visad.meteorology import SingleBandedImage

from ucar.unidata.data.grid import GridUtil

from java.util.concurrent import ExecutionException

class _JythonCallable(Callable):
    def __init__(self, func, args, kwargs):
        self._func = func
        self._args = args
        self._kwargs = kwargs
        
    def call(self):
        return self._func(*self._args, **self._kwargs)
        
def transform_flatfields(func, *args, **kwargs):
    @wraps(func)
    def wrapper(*args, **kwargs):
        wrappedArgs = []
        for i, arg in enumerate(args):
            # print '%s: %s' % (i, arg)
            if isinstance(arg, SingleBandedImage):
                arg = ImageSequenceImpl([arg])
            wrappedArgs.append(arg)
        # print [type(a) for a in wrappedArgs]
        wrappedKwargs = {}
        for keyword in kwargs:
            keywordValue = kwargs[keyword]
            if isinstance(keywordValue, SingleBandedImage):
                keywordValue = ImageSequenceImpl([keywordValue])
            wrappedKwargs[keyword] = keywordValue
        # print [type(wrappedKwargs[a]) for a in wrappedKwargs]
        result = func(*wrappedArgs, **wrappedKwargs)
        # print 'result type=%s' % (type(result))
        if GridUtil.isTimeSequence(result) and len(result) == 1:
            # print 'attempting conversion...'
            result = result.getImage(0)
        # else:
            # print 'not a time sequence! isseq=%s seqtype=%s' % (GridUtil.isSequence(result), GridUtil.getSequenceType(result))
        # print 'returning type=%s' % (type(result))
        return result
    return wrapper

def keepMetadata(func, *args, **kwargs):
    @wraps(func)
    def wrapper(*args, **kwargs):
        print 'inside wrapper'
        result = func(*args, **kwargs)
        try:
            result.setMetadataMap(args[0].getMetadataMap())
        except IndexError:
            print 'caught IndexError; length of args is apparently zero'
        except AttributeError:
            print 'caught AttributeError; args[0] is not a FlatField'
        return result
    return wrapper
    
def _swingRunner(func, *args, **kwargs):
    if SwingUtilities.isEventDispatchThread():
        return func(*args, **kwargs)
    else:
        wrappedCode = _JythonCallable(func, args, kwargs)
        task = FutureTask(wrappedCode)
        SwingUtilities.invokeLater(task)
        return task.get()
        
def _swingWaitForResult(func, *args, **kwargs):
    if SwingUtilities.isEventDispatchThread():
        return func(*args, **kwargs)
        
    wrappedCode = _JythonCallable(func, args, kwargs)
    task = FutureTask(wrappedCode)
    SwingUtilities.invokeAndWait(task)
    return task.get()
    
def gui_invoke_later(func):
    @wraps(func)
    def wrapper(*args, **kwargs):
        return _swingRunner(func, *args, **kwargs)
    return wrapper
    
def gui_invoke_now(func):
    @wraps(func)
    def wrapper(*args, **kwargs):
        return _swingWaitForResult(func, *args, **kwargs)
    return wrapper
    
def deprecated(replacement=None):
    """A decorator which can be used to mark functions as deprecated.
    replacement is a callable that will be called with the same args
    as the decorated function.

    >>> @deprecated()
    ... def foo(x):
    ...     return x
    ...
    >>> ret = foo(1)
    DeprecationWarning: foo is deprecated
    >>> ret
    1
    >>>
    >>>
    >>> def newfun(x):
    ...     return 0
    ...
    >>> @deprecated(newfun)
    ... def foo(x):
    ...     return x
    ...
    >>> ret = foo(1)
    DeprecationWarning: foo is deprecated; use newfun instead
    >>> ret
    0
    >>>
    """
    def outer(oldfun):
        def inner(*args, **kwargs):
            msg = "%s is deprecated" % oldfun.__name__
            if replacement is not None:
                msg += "; use %s instead" % (replacement.__name__)
            warnings.warn(msg, DeprecationWarning, stacklevel=2)
            if replacement is not None:
                return replacement(*args, **kwargs)
            else:
                return oldfun(*args, **kwargs)
        return inner
    return outer
    
def default_import(f):
    """A decorator that automatically adds whatever is being decorated to __all__."""
    # Taken from:
    # http://code.activestate.com/recipes/576993-public-decorator-adds-an-item-to-__all__/
    all = sys.modules[f.__module__].__dict__.setdefault('__all__', [])
    if f.__name__ not in all:  # Prevent duplicates if run from an IDE.
        all.append(f.__name__)
    return f
    
default_import(default_import)
