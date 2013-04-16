import sys
import warnings

from functools import wraps

from java.lang import Runnable
from javax.swing import SwingUtilities

class RunnableWrapper(Runnable):
    def __init__(self, func, args, kwargs):
        self._func = func
        self._args = args
        self._kwargs = kwargs
        
    def run(self):
        self._func(*self._args, **self._kwargs)

def _swingRunner(func, *args, **kwargs):
    if SwingUtilities.isEventDispatchThread():
        func(*args, **kwargs)
    else:
        runnable = RunnableWrapper(func, args, kwargs)
        SwingUtilities.invokeLater(runnable)

def swing_thread_required(func):
    @wraps(func)
    def wrapper(*args, **kwargs):
        _swingRunner(func, *args, **kwargs)
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
