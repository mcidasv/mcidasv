"""Core McIDAS-V session object for driving ``runMcV -script`` from Jupyter.

Each ``runMcV -script <file>`` invocation is a *fresh, offscreen* McIDAS-V JVM --
there is no persistent Jython interpreter to talk to across cells.  This module
embraces that reality and offers two ways to build up a display:

* **self-contained** (default) -- each :meth:`McIDASV.run` call is a complete
  script, exactly like a hand-written ``runMcV -script`` file.
* **replay** (``replay=True``) -- successfully executed scripts are remembered and
  re-run together on the next call so state (loaded data, layers, projection)
  is rebuilt.  This is slower (the JVM restarts and ADDE data is re-fetched
  every time) but gives an incremental, shell-like feel.

The key improvement over a naive wrapper is that stdout/stderr and the exit
code are captured and inspected: McIDAS-V can exit ``0`` even when a Jython
script raises, so the output is scanned for tracebacks and a
:class:`McIDASVError` is raised with the full log attached.
"""

from __future__ import annotations

import hashlib
import os
import platform
import shutil
import subprocess
import tempfile
import time
from dataclasses import dataclass, field
from typing import Iterable, List, Optional, Sequence, Tuple, Union

__all__ = ["McIDASV", "McIDASVError", "RunResult"]


class McIDASVError(RuntimeError):
    """Raised when a McIDAS-V script fails.

    The captured ``stdout`` and ``stderr`` from the ``runMcV`` process are kept
    on the exception so the failure is visible in the notebook rather than being
    silently swallowed.
    """

    def __init__(self, message: str, *, stdout: str = "", stderr: str = "",
                 returncode: Optional[int] = None, script: str = ""):
        self.stdout = stdout
        self.stderr = stderr
        self.returncode = returncode
        self.script = script
        super().__init__(message)


@dataclass
class RunResult:
    """Result of a single :meth:`McIDASV.run` call."""

    image: Optional[str]
    stdout: str
    stderr: str
    returncode: int
    script_path: str
    #: McIDAS-V log for this run -- this is where Jython ``print`` output goes.
    log: str = ""
    script: str = field(repr=False, default="")

    @property
    def prints(self) -> str:
        """Just the ``print`` lines from the Jython script (from the log)."""
        out = []
        for line in self.log.splitlines():
            marker = "jython - print:"
            if marker in line:
                out.append(line.split(marker, 1)[1].strip())
        return "\n".join(out)

    def _repr_mimebundle_(self, include=None, exclude=None):
        # When a RunResult is the value of a notebook cell, render its image.
        if self.image and os.path.exists(self.image):
            from .display import image_mimebundle
            return image_mimebundle(self.image)
        return {"text/plain": repr(self)}


# Signatures of a *script* failure that can occur even when runMcV exits 0.
#
# We deliberately look ONLY for a Jython traceback header. McIDAS-V logs plenty
# of benign Java stack traces at startup (e.g. the macOS ``com.apple.eawt``
# ClassNotFoundException from OSXAdapter, or the PolarOrbitTrackChooser
# IllegalAccessException) -- matching on ``at edu.wisc``/``java.lang.`` etc. would
# flag every successful run on macOS as a failure. A failing ``-script`` Jython
# script, by contrast, prints a Python traceback header.
_ERROR_MARKERS: Tuple[str, ...] = (
    "Traceback (most recent call last)",
    "Traceback (innermost last)",
)


def _looks_like_failure(text: str) -> Optional[str]:
    """Return the offending marker if *text* contains a Jython traceback, else None."""
    for marker in _ERROR_MARKERS:
        if marker in text:
            return marker.strip()
    return None


class McIDASV:
    """A notebook-friendly handle on a ``runMcV`` launcher.

    Parameters
    ----------
    mcv_path:
        Path to the ``runMcV`` (or ``runMcV.bat``) launcher, or any executable
        that accepts ``-script <file>``.
    workdir:
        Directory for generated scripts and captured images.  Defaults to a
        fresh temp directory that is cleaned up with the process.
    default_size:
        ``(width, height)`` used for captures when none is given.
    timeout:
        Default per-run timeout in seconds.
    headless:
        ``"auto"`` (default) starts an Xvfb virtual display only on Linux when no
        ``$DISPLAY`` is set (requires ``pyvirtualdisplay``); ``True``/``False``
        force it on/off.  A no-op on macOS/Windows.
    replay:
        Default replay behaviour for :meth:`run` when its own ``replay`` argument
        is left as ``None``.  Defaults to ``True`` so cells accumulate into a
        shell-like session; set ``False`` (or toggle ``session.replay``) for
        self-contained runs.
    extra_args:
        Additional command-line arguments passed to ``runMcV`` on every run
        (e.g. ``("-userpath", "/path")``).
    """

    def __init__(self, mcv_path: Union[str, os.PathLike], *,
                 workdir: Optional[Union[str, os.PathLike]] = None,
                 default_size: Tuple[int, int] = (800, 600),
                 timeout: float = 600,
                 headless: Union[str, bool] = "auto",
                 replay: bool = True,
                 extra_args: Sequence[str] = ()):
        self.mcv_path = os.path.abspath(os.path.expanduser(str(mcv_path)))
        if not os.path.exists(self.mcv_path):
            raise FileNotFoundError(
                "runMcV launcher not found: {}".format(self.mcv_path))
        if not os.access(self.mcv_path, os.X_OK):
            raise PermissionError(
                "runMcV launcher is not executable: {0}\n"
                "Make it runnable with:  chmod +x '{0}'".format(self.mcv_path))

        if workdir is None:
            self.workdir = tempfile.mkdtemp(prefix="mcv-jupyter-")
            self._owns_workdir = True
        else:
            self.workdir = os.path.abspath(os.path.expanduser(str(workdir)))
            os.makedirs(self.workdir, exist_ok=True)
            self._owns_workdir = False

        self.default_size = default_size
        self.timeout = timeout
        self.replay = replay
        self.extra_args = list(extra_args)
        self._history: List[str] = []
        self._display = None  # started Xvfb, if any

        # A known userpath so we can read McIDAS-V's log file after each run --
        # McIDAS-V writes Jython/ADDE errors there, not to stdout/stderr.
        self.userpath = os.path.join(self.workdir, "userpath")
        os.makedirs(self.userpath, exist_ok=True)
        self._logpath = os.path.join(self.userpath, "mcidasv.log")

        self._maybe_start_display(headless)

    # -- lifecycle -----------------------------------------------------------

    def _maybe_start_display(self, headless: Union[str, bool]) -> None:
        if headless is False:
            return
        want = headless is True or (
            headless == "auto"
            and platform.system() == "Linux"
            and not os.environ.get("DISPLAY"))
        if not want:
            return
        try:
            from pyvirtualdisplay import Display
        except ImportError as exc:  # pragma: no cover - environment dependent
            raise McIDASVError(
                "A virtual display is required (headless Linux with no "
                "$DISPLAY) but pyvirtualdisplay is not installed. Install it "
                "with `pip install pyvirtualdisplay` and ensure Xvfb is "
                "available.") from exc
        self._display = Display(visible=False, size=(1400, 1000))
        self._display.start()

    def close(self) -> None:
        """Stop any virtual display and remove the owned temp directory."""
        if self._display is not None:
            try:
                self._display.stop()
            finally:
                self._display = None
        if self._owns_workdir and os.path.isdir(self.workdir):
            shutil.rmtree(self.workdir, ignore_errors=True)

    def __enter__(self) -> "McIDASV":
        return self

    def __exit__(self, *exc) -> None:
        self.close()

    # -- history / replay ----------------------------------------------------

    @property
    def history(self) -> List[str]:
        """Scripts that have executed successfully (used by ``replay=True``)."""
        return list(self._history)

    def reset(self) -> None:
        """Forget replay history so the next run starts from a clean session."""
        self._history = []

    # -- running -------------------------------------------------------------

    def run(self, script: str, *,
            values: Optional[dict] = None,
            arrays: Optional[dict] = None,
            capture: Optional[str] = "auto",
            panel: str = "panel",
            index: int = 0,
            size: Optional[Tuple[int, int]] = None,
            out: Optional[Union[str, os.PathLike]] = None,
            fmt: str = "png",
            replay: Optional[bool] = None,
            display: bool = True,
            timeout: Optional[float] = None,
            userpath: Optional[str] = None) -> RunResult:
        """Execute *script* in a fresh McIDAS-V and return a :class:`RunResult`.

        Parameters
        ----------
        script:
            McIDAS-V Jython, identical to the body of a ``runMcV -script`` file.
        values:
            ``{name: python_value}`` injected as Jython assignments *before* your
            script, so you can compute parameters in Python/numpy and use them in
            McIDAS-V.  Scalars, strings, lists/tuples/dicts, datetimes and small
            numpy arrays (serialised via ``tolist()``) are supported.
        arrays:
            ``{name: grid}`` where each *grid* is a :class:`~mcidasv_jupyter.bridge.Grid`
            (from :meth:`write_grid`/:meth:`array_to_grid`), a ``(data, lats, lons)``
            tuple, or a dict of :func:`~mcidasv_jupyter.bridge.write_grid` keyword
            arguments.  Each becomes ``name = loadGrid(filename=..., field=...)`` so
            a numpy field can be displayed in McIDAS-V.
        capture:
            ``"auto"`` appends a ``captureImage`` for *panel*/*index*; pass an
            explicit target expression (e.g. ``"panel[0]"``) to capture that; or
            ``None`` to skip auto-capture (use when the script writes its own
            image or movie).
        panel, index:
            Panel variable name and frame index used to build the auto-capture
            ``<panel>[<index>].captureImage(...)`` call.
        size:
            ``(width, height)`` for the captured image; defaults to
            ``default_size``.
        out:
            Where to write the image; defaults to a hashed name in ``workdir``.
        fmt:
            Image extension when *out* is not given (``png``, ``jpg``, ``gif``...).
        replay:
            Re-run previously successful scripts before this one to rebuild
            state.  ``None`` (default) uses the session's ``replay`` setting
            (on by default); pass ``True``/``False`` to override for this call.
        display:
            Render the captured image inline (in a notebook).
        timeout:
            Override the default per-run timeout (seconds).
        """
        size = size or self.default_size
        capture_target = self._capture_target(capture, panel, index)

        # Injected Python values / numpy grids become a prelude that is part of
        # the script (and therefore replayed with it).
        prelude = self._build_prelude(values, arrays)
        effective_script = prelude + script if prelude else script

        image_path: Optional[str] = None
        capture_line = ""
        if capture_target is not None:
            image_path = (os.path.abspath(os.path.expanduser(str(out)))
                          if out is not None else self._new_path(fmt))
            capture_line = "{target}.captureImage('{path}', width={w}, height={h})".format(
                target=capture_target, path=_escape(image_path),
                w=size[0], h=size[1])

        use_replay = self.replay if replay is None else replay
        pieces: List[str] = []
        if use_replay and self._history:
            pieces.extend(self._history)
        pieces.append(effective_script)
        if capture_line:
            pieces.append(capture_line)
        full_script = "\n".join(pieces) + "\n"

        if userpath is not None:
            os.makedirs(userpath, exist_ok=True)
        eff_userpath = userpath or self.userpath

        script_path = self._write_script(full_script)
        result = self._invoke(script_path, full_script, userpath=eff_userpath,
                              timeout=timeout if timeout is not None else self.timeout)

        # Only remember scripts that ran cleanly (with their injected prelude).
        self._history.append(effective_script)

        run_result = RunResult(
            image=image_path if (image_path and os.path.exists(image_path)) else None,
            stdout=result.stdout or "",
            stderr=result.stderr or "",
            returncode=result.returncode,
            script_path=script_path,
            log=self._read_log(os.path.join(eff_userpath, "mcidasv.log")),
            script=full_script,
        )
        if display and run_result.image:
            from .display import show_image
            show_image(run_result.image)
        return run_result

    def run_file(self, path: Union[str, os.PathLike], **kwargs) -> RunResult:
        """Run an existing ``.py`` McIDAS-V script file through :meth:`run`."""
        with open(path, "r") as handle:
            return self.run(handle.read(), **kwargs)

    # -- numpy -> McIDAS-V grid bridge ---------------------------------------

    def write_grid(self, data, lats, lons, *, name: str = "data", **kwargs):
        """Write a numpy field to a CF netCDF grid in ``workdir`` for ``loadGrid``.

        Returns a :class:`~mcidasv_jupyter.bridge.Grid` (``.path``, ``.field``).
        Extra keyword arguments are forwarded to
        :func:`mcidasv_jupyter.bridge.write_grid` (e.g. ``times=``, ``attrs=``).
        """
        from .bridge import write_grid as _write_grid
        path = os.path.join(self.workdir, "grid-{}.nc".format(self._hash()))
        return _write_grid(path, data, lats, lons, name=name, **kwargs)

    #: alias -- reads a little better at some call sites
    array_to_grid = write_grid

    # -- McIDAS-V -> numpy (real values, not pixels) --------------------------

    def extract_field(self, script: str, *, layer: str = "layer", time: int = 0,
                      timeout: Optional[float] = None, **run_kwargs):
        """Run *script* and return a displayed layer's data as numpy arrays.

        *script* must create a layer (by default named ``layer``), e.g. via
        ``createLayer``. The layer's VisAD data is then pulled out with
        ``getData()`` and written to disk, giving a :class:`~mcidasv_jupyter.bridge.Field`
        with the **actual geophysical values** (e.g. brightness temperature in K)
        and a latitude/longitude for every point -- as opposed to reading a
        rendered image, where values are RGB and navigation is only approximate.

        Parameters
        ----------
        layer:
            Name of the layer variable defined in *script*.
        time:
            Time index to extract when the layer holds a sequence.

        Large images produce a lot of points; subsample at request time with
        ``mag=(-4, -4)`` (or similar) in ``loadADDEImage`` to keep it quick.
        """
        return self.extract_fields(script, layer=layer, times=(time,),
                                   timeout=timeout, **run_kwargs)[0]

    def extract_fields(self, script: str, *, layer: str = "layer",
                       times: Sequence[int] = (0,),
                       timeout: Optional[float] = None, **run_kwargs) -> "List":
        """Like :meth:`extract_field`, but pulls several time steps in one run.

        Returns a list of :class:`~mcidasv_jupyter.bridge.Field`, one per entry in
        *times*. All steps share the same navigation, so this is much faster than
        calling :meth:`extract_field` repeatedly (one McIDAS-V start, not N).
        """
        import numpy as np
        from .bridge import Field

        times = list(times)
        tag = self._hash()
        base = os.path.join(self.workdir, "field-{}".format(tag))
        dump = (
            "\nfrom java.io import DataOutputStream, FileOutputStream, BufferedOutputStream\n"
            "def _dump(_path, _arr):\n"
            "    _o = DataOutputStream(BufferedOutputStream(FileOutputStream(_path)))\n"
            "    for _v in _arr:\n"
            "        _o.writeFloat(float(_v))\n"
            "    _o.close()\n"
            "_d = {layer}.getData()\n"
            "_first = None\n"
            "for _t in TIMES:\n"
            "    try:\n"
            "        _ff = _d.getSample(_t)\n"
            "    except Exception:\n"
            "        _ff = _d\n"
            "    if _first is None:\n"
            "        _first = _ff\n"
            "    _dump(BASE + '.%d.val' % _t, _ff.getFloats(0)[0])\n"
            "_dom = _first.getDomainSet()\n"
            "_nx, _ny = _dom.getLengths()[0], _dom.getLengths()[1]\n"
            "_ll = _dom.getCoordinateSystem().toReference(_dom.getSamples())\n"
            "_dump(BASE + '.lat', _ll[0])\n"
            "_dump(BASE + '.lon', _ll[1])\n"
            "_m = open(BASE + '.meta', 'w')\n"
            "_m.write('%d %d\\n' % (_nx, _ny))\n"
            "try:\n"
            "    _m.write('%s\\n' % {layer}.getDisplayUnit())\n"
            "except Exception:\n"
            "    _m.write('\\n')\n"
            "_m.write('%s\\n' % str(_first.getType()))\n"
            "_m.close()\n"
        ).format(layer=layer)

        run_kwargs.setdefault("replay", False)
        values = dict(run_kwargs.pop("values", None) or {})
        values["BASE"] = base
        values["TIMES"] = times
        self.run(script + dump, values=values, capture=None, display=False,
                 timeout=timeout, **run_kwargs)

        with open(base + ".meta") as fh:
            meta = fh.read().split("\n")
        nx, ny = (int(x) for x in meta[0].split())
        unit = meta[1].strip() if len(meta) > 1 else ""
        name = meta[2].strip() if len(meta) > 2 else ""

        def _read(ext):
            # DataOutputStream writes big-endian floats.
            return np.fromfile(base + ext, dtype=">f4").astype("f4").reshape(ny, nx)

        lats, lons = _read(".lat"), _read(".lon")
        return [Field(values=_read(".{}.val".format(t)), lats=lats, lons=lons,
                      unit=unit, name=name) for t in times]

    def animate_grid(self, cube, lats, lons, *,
                     name: str = "data",
                     out: Optional[Union[str, os.PathLike]] = None,
                     display_type: str = "Color-Shaded Plan View",
                     globe: bool = False,
                     projection: str = "US>CONUS",
                     size: Tuple[int, int] = (760, 500),
                     setup: str = "",
                     fps: float = 5,
                     slow_render: bool = True,
                     display: bool = True) -> str:
        """Animate a 3-D ``(time, lat, lon)`` numpy cube as a GIF.

        Two rendering strategies, chosen by *slow_render*:

        * ``slow_render=True`` (default) renders each frame in its **own**
          McIDAS-V process. This is the only *reliable* way to animate computed
          grids -- McIDAS-V's offscreen scripting caches data within a run, so
          multiple grids captured in one process come out identical -- but it
          costs roughly one JVM startup (~20s) per frame. Keep the frame count
          modest (say 8-16).
        * ``slow_render=False`` renders all frames in a **single** process
          (much faster). This hits the caching bug and usually produces
          identical frames; when it does, a warning is printed telling you to
          re-run with ``slow_render=True``. Useful only for a quick try.

        Parameters
        ----------
        cube:
            3-D array shaped ``(time, lat, lon)``.
        lats, lons:
            1-D coordinate arrays.
        globe:
            Display on the 3-D GLOBE panel instead of a map.
        projection:
            Map projection (ignored when ``globe=True``).
        setup:
            Jython run on ``panel`` before each frame is captured, e.g.
            ``"panel[0].annotate('title', line=470, element=380, size=16)"``.
        fps:
            Frames per second in the output GIF.
        slow_render:
            ``True`` (default) = reliable one-process-per-frame; ``False`` =
            fast single-process (may yield identical frames -- see above).

        Returns the path to the written GIF.
        """
        import numpy as np
        cube = np.asarray(cube)
        if cube.ndim != 3:
            raise ValueError("cube must be 3-D (time, lat, lon)")
        nt = cube.shape[0]

        if slow_render:
            frame_paths = self._render_frames_slow(
                cube, lats, lons, name, display_type, globe, projection, size, setup)
        else:
            frame_paths = self._render_frames_fast(
                cube, lats, lons, name, display_type, globe, projection, size, setup)
            self._warn_if_identical(frame_paths)

        out = (os.path.abspath(os.path.expanduser(str(out))) if out is not None
               else os.path.join(self.workdir, "{}.gif".format(name)))
        from .display import frames_to_gif
        frames_to_gif(frame_paths, out, fps=fps)
        if display:
            from .display import show_image
            show_image(out)
        return out

    def _frame_head(self, panel_expr, display_type, globe, projection, setup):
        # createLayer must come immediately after buildWindow, or the data does
        # not render (blank frame). The display type is inlined and the grid is a
        # plain tuple so each frame renders its own data.
        head = ["panel = {}".format(panel_expr),
                "layer = panel[0].createLayer({!r}, g)".format(display_type)]
        if not globe:
            head.append("panel[0].setProjection({!r})".format(projection))
        head.append("panel[0].setWireframe(False)")
        head.extend(setup.splitlines())
        return head

    def _render_frames_slow(self, cube, lats, lons, name, display_type, globe,
                            projection, size, setup):
        w, h = size
        panel_expr = ("buildWindow(height={h}, width={w}, panelTypes=GLOBE)"
                      if globe else "buildWindow(height={h}, width={w})").format(w=w, h=h)
        frame_script = "\n".join(
            self._frame_head(panel_expr, display_type, globe, projection, setup))
        tag = self._hash()
        tmpl = os.path.join(self.workdir, "aniframe-{}-%03d.png".format(tag))
        frame_paths = []
        for i in range(len(cube)):
            png = tmpl % i
            self.run(frame_script, arrays={"g": (cube[i], lats, lons)},
                     capture="panel[0]", out=png, replay=False, display=False)
            frame_paths.append(png)
        return frame_paths

    def _render_frames_fast(self, cube, lats, lons, name, display_type, globe,
                            projection, size, setup):
        # All frames in one McIDAS-V process: write each frame as a 2-D grid and
        # loop over them, rebuilding a fresh window and clearing data each time.
        import numpy as np
        from .bridge import write_grid as _wg
        nt = len(cube)
        tag = self._hash()
        files = []
        for i in range(nt):
            fp = os.path.join(self.workdir, "fastframe-{}-{:03d}.nc".format(tag, i))
            _wg(fp, cube[i], lats, lons, name=name)
            files.append(fp)
        w, h = size
        panel_expr = ("buildWindow(height={h}, width={w}, panelTypes=GLOBE)"
                      if globe else "buildWindow(height={h}, width={w})").format(w=w, h=h)
        tmpl = os.path.join(self.workdir, "aniframe-{}-%03d.png".format(tag))
        body = ["removeAllData()",
                "g = loadGrid(filename=FILES[_i], field={!r})".format(name)]
        body.extend(self._frame_head(panel_expr, display_type, globe, projection, setup))
        body.append("panel[0].captureImage(TMPL % _i, width=WIDTH, height=HEIGHT)")
        loop = "".join("    {}\n".format(ln) for ln in body)
        script = "for _i in range(NT):\n" + loop
        self.run(script, capture=None, display=False,
                 values={"NT": nt, "FILES": files, "TMPL": tmpl, "WIDTH": w, "HEIGHT": h})
        return [tmpl % i for i in range(nt)]

    @staticmethod
    def _warn_if_identical(frame_paths):
        import hashlib
        seen = set()
        for p in frame_paths:
            if os.path.exists(p):
                with open(p, "rb") as fh:
                    seen.add(hashlib.md5(fh.read()).hexdigest())
        if len(seen) <= 1 and len(frame_paths) > 1:
            import warnings
            warnings.warn(
                "animate_grid(slow_render=False) produced identical frames "
                "(McIDAS-V cached the data within one process). Re-run with "
                "slow_render=True for a real animation.", stacklevel=3)

    def _grid_for(self, value):
        """Normalise an ``arrays=`` value into a :class:`Grid`."""
        from .bridge import Grid
        if isinstance(value, Grid):
            return value
        if isinstance(value, dict):
            return self.write_grid(**value)
        if isinstance(value, (tuple, list)) and len(value) == 3:
            data, lats, lons = value
            return self.write_grid(data, lats, lons)
        raise TypeError(
            "arrays values must be a Grid, a (data, lats, lons) tuple, or a "
            "dict of write_grid kwargs; got {}".format(type(value).__name__))

    def _build_prelude(self, values: Optional[dict], arrays: Optional[dict]) -> str:
        lines: List[str] = []
        if values:
            from ._capture import _safe_literal
            for name, value in values.items():
                lines.append("{} = {}".format(name, _safe_literal(value)))
        if arrays:
            for name, value in arrays.items():
                grid = self._grid_for(value)
                # loadGrid loads a single time; for a time animation use
                # McIDASV.animate_grid (McIDAS-V does not animate a list of grids
                # passed to one createLayer).
                lines.append("{} = {}".format(name, grid.loadgrid_call()))
        return ("\n".join(lines) + "\n") if lines else ""

    # -- decorator sugar -----------------------------------------------------

    def command(self, func):
        """Decorator: run a function's body as a McIDAS-V script.

        Python literal values referenced from the enclosing scope (closures and
        module globals) are captured and emitted as assignments at the top of the
        script, so simple parameters can be shared between Python and Jython.
        Modules, functions and classes are skipped.
        """
        from ._capture import build_script_from_function
        script = build_script_from_function(func)
        self.run(script)
        return func

    # -- internals -----------------------------------------------------------

    @staticmethod
    def _capture_target(capture: Optional[str], panel: str, index: int) -> Optional[str]:
        if capture is None:
            return None
        if capture == "auto":
            return "{panel}[{index}]".format(panel=panel, index=index)
        return capture

    def _new_path(self, fmt: str) -> str:
        return os.path.join(self.workdir, "{}.{}".format(self._hash(), fmt.lstrip(".")))

    @staticmethod
    def _hash() -> str:
        return hashlib.sha256(str(time.time()).encode()).hexdigest()[:16]

    def _write_script(self, full_script: str) -> str:
        path = os.path.join(self.workdir, "{}.py".format(self._hash()))
        with open(path, "w") as handle:
            handle.write(full_script)
        return path

    def _invoke(self, script_path: str, full_script: str,
                *, timeout: float,
                userpath: Optional[str] = None) -> subprocess.CompletedProcess:
        userpath = userpath or self.userpath
        logpath = os.path.join(userpath, "mcidasv.log")
        # Start each run with a fresh log so we read only this run's messages.
        try:
            if os.path.exists(logpath):
                os.remove(logpath)
        except OSError:
            pass

        cmd = [self.mcv_path, "-script", script_path,
               "-userpath", userpath, *self.extra_args]
        try:
            result = subprocess.run(
                cmd, capture_output=True, text=True, timeout=timeout)
        except subprocess.TimeoutExpired as exc:
            raise McIDASVError(
                "McIDAS-V timed out after {:g}s.".format(timeout),
                stdout=exc.stdout or "", stderr=exc.stderr or "",
                script=full_script) from exc

        # McIDAS-V logs Jython/ADDE errors to mcidasv.log, not stdout/stderr.
        log = self._read_log(logpath)
        combined = "\n".join([result.stdout or "", result.stderr or "", log])
        marker = _looks_like_failure(combined)
        if result.returncode != 0 or marker is not None:
            reason = ("exit code {}".format(result.returncode)
                      if result.returncode != 0
                      else "error in output ({})".format(marker))
            raise McIDASVError(
                "McIDAS-V script failed ({}). See output below.\n"
                "--- stdout ---\n{}\n--- stderr ---\n{}\n--- mcidasv.log ---\n{}".format(
                    reason, result.stdout, result.stderr, log or "(empty)"),
                stdout=result.stdout or "", stderr=(result.stderr or "") + log,
                returncode=result.returncode, script=full_script)
        return result

    def _read_log(self, logpath: Optional[str] = None, max_lines: int = 200) -> str:
        """Return the tail of McIDAS-V's log file, if it exists."""
        try:
            with open(logpath or self._logpath, "r", errors="replace") as handle:
                lines = handle.readlines()
        except OSError:
            return ""
        return "".join(lines[-max_lines:]).strip()


def _escape(path: str) -> str:
    """Escape a filesystem path for embedding inside a single-quoted string."""
    return path.replace("\\", "\\\\").replace("'", "\\'")
