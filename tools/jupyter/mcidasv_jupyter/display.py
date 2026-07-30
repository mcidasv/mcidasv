"""Inline rendering helpers for images and movies produced by McIDAS-V."""

from __future__ import annotations

import os
from typing import Dict

__all__ = ["show_image", "image_mimebundle", "frames_to_gif"]


def frames_to_gif(frame_paths, out_path, *, fps: float = 5, loop: int = 0) -> str:
    """Assemble PNG frames into an animated GIF (needs Pillow).

    McIDAS-V's ``writeMovie`` does not animate a list of grids, so
    :meth:`McIDASV.animate_grid` captures each frame separately and stitches
    them together here.
    """
    try:
        from PIL import Image
    except ImportError as exc:  # pragma: no cover - environment dependent
        raise ImportError(
            "Assembling an animated GIF needs Pillow: pip install pillow "
            "(it also ships with matplotlib).") from exc
    frames = [Image.open(p).convert("RGB") for p in frame_paths if os.path.exists(p)]
    if not frames:
        raise RuntimeError("no frames were captured to assemble into a GIF")
    duration = max(1, int(round(1000.0 / fps)))
    frames[0].save(out_path, save_all=True, append_images=frames[1:],
                   duration=duration, loop=loop, optimize=True)
    return out_path

_MIME_BY_EXT = {
    ".png": "image/png",
    ".jpg": "image/jpeg",
    ".jpeg": "image/jpeg",
    ".gif": "image/gif",
    ".svg": "image/svg+xml",
}


def _mime_for(path: str) -> str:
    return _MIME_BY_EXT.get(os.path.splitext(path)[1].lower(), "image/png")


def image_mimebundle(path: str) -> Dict[str, object]:
    """Return an IPython mimebundle dict for the image at *path*."""
    with open(path, "rb") as handle:
        data = handle.read()
    mime = _mime_for(path)
    if mime == "image/svg+xml":
        return {mime: data.decode("utf-8")}
    return {mime: data}


def show_image(path: str) -> None:
    """Display the image (or animated GIF) at *path* inline in a notebook.

    Falls back to a plain message if the file is missing or IPython is not
    available (e.g. running under plain Python).
    """
    if not path or not os.path.exists(path):
        print("[mcidasv] no image was produced at {!r}".format(path))
        return
    try:
        from IPython.display import Image, display
    except ImportError:
        print("[mcidasv] image written to {}".format(path))
        return
    display(Image(filename=path))
