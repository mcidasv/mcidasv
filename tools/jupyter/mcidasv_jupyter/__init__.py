"""Drive McIDAS-V from a Jupyter notebook.

Quick start::

    import mcidasv_jupyter as mcv
    session = mcv.McIDASV('/path/to/runMcV')

    session.run('''
    data = loadADDEImage(server='adde.ucar.edu', dataset='RTGOESR',
                         descriptor='M1C14', unit='TEMP', size='ALL', mag=(1, 1))
    panel = buildWindow(height=600, width=800)
    layer = panel[0].createLayer('Image Display', data)
    layer.setEnhancement('ABI IR Temperature', range=(220, 300))
    ''')

Or, with the cell magic::

    %load_ext mcidasv_jupyter
    %mcv_connect /path/to/runMcV

    %%mcv
    ...McIDAS-V Jython...

See ``tools/jupyter/README.md`` and the ``samples/`` notebooks for more.
"""

from __future__ import annotations

from .session import McIDASV, McIDASVError, RunResult
from .bridge import Grid, Field, write_grid
from .magic import set_session, get_session, load_ipython_extension

__all__ = [
    "McIDASV",
    "McIDASVError",
    "RunResult",
    "Grid",
    "Field",
    "write_grid",
    "set_session",
    "get_session",
    "load_ipython_extension",
]

__version__ = "0.1.0"
