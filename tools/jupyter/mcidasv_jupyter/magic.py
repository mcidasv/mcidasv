"""IPython magics for running McIDAS-V scripts from notebook cells.

Usage in a notebook::

    %load_ext mcidasv_jupyter
    %mcv_connect /path/to/runMcV

    %%mcv
    data = loadADDEImage(server='adde.ucar.edu', dataset='RTGOESR',
                         descriptor='M1C14', unit='TEMP', size='ALL', mag=(1, 1))
    panel = buildWindow(height=600, width=800)
    layer = panel[0].createLayer('Image Display', data)
    layer.setEnhancement('ABI IR Temperature', range=(220, 300))

The cell body is the same Jython you would put in a ``runMcV -script`` file; the
image is captured and shown inline automatically.
"""

from __future__ import annotations

import shlex
from typing import Optional

from .session import McIDASV, McIDASVError

__all__ = ["McIDASVMagics", "load_ipython_extension", "set_session", "get_session"]

# A single module-level session shared by the magics, set via %mcv_connect or
# set_session().  Keeps notebook cells terse.
_SESSION: Optional[McIDASV] = None


def set_session(session: McIDASV) -> None:
    """Make *session* the one used by ``%%mcv``."""
    global _SESSION
    _SESSION = session


def get_session() -> Optional[McIDASV]:
    return _SESSION


def _parse_size(text: str):
    lowered = text.lower().replace("x", " ").replace(",", " ").split()
    if len(lowered) != 2:
        raise ValueError("--size must look like 800x600")
    return int(lowered[0]), int(lowered[1])


try:
    from IPython.core.magic import Magics, magics_class, line_magic, cell_magic
    from IPython.core.magic_arguments import (argument, magic_arguments,
                                              parse_argstring)
except ImportError:  # pragma: no cover - IPython not installed
    Magics = object

    def magics_class(cls):
        return cls
else:
    @magics_class
    class McIDASVMagics(Magics):
        """Provides ``%mcv_connect``, ``%mcv_reset`` and ``%%mcv``."""

        @line_magic("mcv_connect")
        def mcv_connect(self, line):
            """``%mcv_connect <path-to-runMcV> [--headless auto|on|off]``"""
            args = shlex.split(line)
            if not args:
                raise UsageError("usage: %mcv_connect <path-to-runMcV>")
            path = args[0]
            headless = "auto"
            if "--headless" in args:
                value = args[args.index("--headless") + 1]
                headless = {"on": True, "off": False}.get(value, value)
            set_session(McIDASV(path, headless=headless))
            print("[mcidasv] connected to {}".format(path))

        @line_magic("mcv_reset")
        def mcv_reset(self, line):
            """Clear replay history on the active session."""
            session = _require_session()
            session.reset()
            print("[mcidasv] replay history cleared")

        @line_magic("mcv_replay")
        def mcv_replay(self, line):
            """``%mcv_replay on|off`` -- toggle sticky replay for ``%%mcv``.

            With no argument, prints the current setting.
            """
            session = _require_session()
            arg = line.strip().lower()
            if not arg:
                print("[mcidasv] replay is {}".format(
                    "on" if session.replay else "off"))
                return
            if arg in ("on", "true", "1", "yes"):
                session.replay = True
            elif arg in ("off", "false", "0", "no"):
                session.replay = False
            else:
                raise UsageError("usage: %mcv_replay on|off")
            print("[mcidasv] replay is now {}".format(
                "on" if session.replay else "off"))

        @magic_arguments()
        @argument("--capture", default="auto",
                  help="capture target expression, e.g. panel[0]; 'none' to skip")
        @argument("--panel", default="panel",
                  help="panel variable name for auto-capture (default: panel)")
        @argument("--index", type=int, default=0,
                  help="frame index for auto-capture (default: 0)")
        @argument("--size", default=None, help="image size, e.g. 800x600")
        @argument("--out", default=None, help="output image path")
        @argument("--format", dest="fmt", default="png",
                  help="image format when --out is omitted (default: png)")
        @argument("--push", default=None,
                  help="comma-separated notebook variable names to inject as "
                       "Jython values (e.g. --push levels,center_lat)")
        @argument("--replay", action="store_true",
                  help="force replay for this cell (re-run previous cells)")
        @argument("--no-replay", dest="no_replay", action="store_true",
                  help="force self-contained for this cell (ignore history)")
        @argument("--no-display", dest="no_display", action="store_true",
                  help="do not render the image inline")
        @argument("--timeout", type=float, default=None,
                  help="per-run timeout in seconds")
        @cell_magic("mcv")
        def mcv(self, line, cell):
            """Run the cell body as a McIDAS-V script and show the image."""
            args = parse_argstring(self.mcv, line)
            session = _require_session()

            capture = None if str(args.capture).lower() == "none" else args.capture
            size = _parse_size(args.size) if args.size else None

            if args.replay and args.no_replay:
                raise UsageError("pass either --replay or --no-replay, not both")
            # None => use the session's sticky default (%mcv_replay / session.replay)
            replay = True if args.replay else (False if args.no_replay else None)

            values = None
            if args.push:
                ns = self.shell.user_ns
                names = [n.strip() for n in args.push.split(",") if n.strip()]
                missing = [n for n in names if n not in ns]
                if missing:
                    raise UsageError(
                        "--push: name(s) not defined in the notebook: {}".format(
                            ", ".join(missing)))
                values = {n: ns[n] for n in names}

            try:
                return session.run(
                    cell,
                    values=values,
                    capture=capture,
                    panel=args.panel,
                    index=args.index,
                    size=size,
                    out=args.out,
                    fmt=args.fmt,
                    replay=replay,
                    display=not args.no_display,
                    timeout=args.timeout,
                )
            except McIDASVError as exc:
                # Surface the McIDAS-V log in the cell rather than a bare trace.
                print(str(exc))
                raise

    from IPython.core.error import UsageError


def _require_session() -> McIDASV:
    if _SESSION is None:
        raise RuntimeError(
            "No McIDAS-V session. Run `%mcv_connect <path-to-runMcV>` first, "
            "or call mcidasv_jupyter.set_session(McIDASV(path)).")
    return _SESSION


def load_ipython_extension(ipython) -> None:
    """Entry point for ``%load_ext mcidasv_jupyter``."""
    ipython.register_magics(McIDASVMagics)
