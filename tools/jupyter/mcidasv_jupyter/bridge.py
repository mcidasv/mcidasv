"""Move numpy arrays into McIDAS-V as gridded data.

McIDAS-V's ``loadGrid`` scripting function reads local netCDF/HDF/GRIB files, so
the robust way to display a numpy/scipy result in McIDAS-V is:

    numpy array  --(write_grid)-->  CF netCDF file  --(loadGrid)-->  McIDAS-V

This module writes a minimal, CF-compliant **netCDF-3 classic** file using only
numpy -- no ``netCDF4``/``scipy`` dependency -- so ``mcidasv_jupyter`` can push a
field with nothing more than what you already have installed. The resulting file
has ``lat``/``lon`` (and optional ``time``) coordinate variables and one or more
2-D/3-D data variables, which netCDF-Java (bundled with McIDAS-V) recognises as a
lat/lon grid.

See :meth:`mcidasv_jupyter.McIDASV.write_grid` /
:meth:`~mcidasv_jupyter.McIDASV.array_to_grid` for the notebook-facing helpers.
"""

from __future__ import annotations

import os
import struct
from dataclasses import dataclass
from typing import Dict, List, Optional, Sequence, Tuple, Union

import numpy as np

__all__ = ["Grid", "Field", "write_grid", "write_netcdf"]


@dataclass
class Field:
    """Real data values pulled out of a McIDAS-V layer, with navigation.

    Returned by :meth:`mcidasv_jupyter.McIDASV.extract_field`. Unlike reading a
    captured image, these are the layer's actual geophysical values (e.g.
    brightness temperature in K) with a latitude/longitude for every point.

    Attributes
    ----------
    values, lats, lons:
        ``(ny, nx)`` arrays; row 0 is the northern edge.
    unit, name:
        Display unit and parameter name, when McIDAS-V reports them.
    """

    values: "np.ndarray"
    lats: "np.ndarray"
    lons: "np.ndarray"
    unit: str = ""
    name: str = ""

    @property
    def valid(self) -> "np.ndarray":
        """Mask of on-earth points (off-earth pixels have no navigation)."""
        return np.isfinite(self.lats) & np.isfinite(self.lons)

    @property
    def shape(self):
        return self.values.shape

    def masked(self, missing: Optional[float] = 0.0) -> "np.ndarray":
        """``values`` as float with unusable points set to NaN.

        Masks points with no navigation (off the earth) and, by default, points
        exactly equal to *missing* -- McIDAS-V image fields use ``0`` as a fill
        value, and a 0 K brightness temperature would otherwise look like the
        coldest pixel in the scene. Pass ``missing=None`` to keep raw values.
        """
        out = self.values.astype("f4", copy=True)
        bad = ~self.valid
        if missing is not None:
            bad |= (out == missing)
        out[bad] = np.nan
        return out


# netCDF-3 type tags: name -> (nc_type code, numpy big-endian dtype)
_NC_TYPES = {
    "byte": (1, np.dtype(">i1")),
    "char": (2, np.dtype("S1")),
    "short": (3, np.dtype(">i2")),
    "int": (4, np.dtype(">i4")),
    "float": (5, np.dtype(">f4")),
    "double": (6, np.dtype(">f8")),
}
_NC_DIMENSION = 0x0A
_NC_VARIABLE = 0x0B
_NC_ATTRIBUTE = 0x0C


@dataclass
class Grid:
    """A netCDF grid written for McIDAS-V's ``loadGrid``.

    Attributes
    ----------
    path:
        Path to the netCDF file on disk.
    field:
        Default field (variable) short name to hand to ``loadGrid``.
    """

    path: str
    field: str
    #: number of time steps (for a 3-D grid), else None
    ntimes: Optional[int] = None

    def loadgrid_call(self, **extra) -> str:
        """Return a Jython ``loadGrid(...)`` expression for a single time.

        ``loadGrid`` loads one time step; animating a 3-D grid needs
        :meth:`McIDASV.animate_grid`, which renders a frame per process.
        """
        args = ["filename={!r}".format(self.path), "field={!r}".format(self.field)]
        for key, value in extra.items():
            args.append("{}={!r}".format(key, value))
        return "loadGrid({})".format(", ".join(args))


def _pad4(n: int) -> int:
    return (n + 3) & ~3


def _pack_string(name: str) -> bytes:
    raw = name.encode("utf-8")
    out = struct.pack(">i", len(raw)) + raw
    return out + b"\x00" * (_pad4(len(raw)) - len(raw))


def _pack_values(nc_code: int, dtype: np.dtype, values) -> bytes:
    if nc_code == 2:  # char / text attribute
        raw = values.encode("utf-8") if isinstance(values, str) else bytes(values)
        return raw + b"\x00" * (_pad4(len(raw)) - len(raw))
    arr = np.asarray(values, dtype=dtype)
    raw = arr.tobytes()
    return raw + b"\x00" * (_pad4(len(raw)) - len(raw))


def _pack_attr(name: str, value) -> bytes:
    if isinstance(value, str):
        nc_code, dtype = _NC_TYPES["char"]
        nelems = len(value.encode("utf-8"))
    elif isinstance(value, (int, np.integer)):
        nc_code, dtype = _NC_TYPES["int"]
        nelems = 1
    else:
        # float / sequence of floats
        nc_code, dtype = _NC_TYPES["float"]
        seq = np.atleast_1d(np.asarray(value, dtype=dtype))
        nelems = seq.size
        value = seq
    body = _pack_string(name) + struct.pack(">ii", nc_code, nelems)
    body += _pack_values(nc_code, dtype, value)
    return body


def _pack_attr_list(attrs: Optional[Dict[str, object]]) -> bytes:
    if not attrs:
        return struct.pack(">ii", 0, 0)  # ABSENT
    out = struct.pack(">ii", _NC_ATTRIBUTE, len(attrs))
    for name, value in attrs.items():
        out += _pack_attr(name, value)
    return out


class _Var:
    def __init__(self, name, dimids, nc_code, dtype, shape, attrs):
        self.name = name
        self.dimids = dimids
        self.nc_code = nc_code
        self.dtype = dtype
        self.shape = shape
        self.attrs = attrs
        nbytes = int(np.prod(shape)) * dtype.itemsize if shape else dtype.itemsize
        self.vsize = _pad4(nbytes)
        self.begin = 0

    def header(self) -> bytes:
        out = _pack_string(self.name)
        out += struct.pack(">i", len(self.dimids))
        for d in self.dimids:
            out += struct.pack(">i", d)
        out += _pack_attr_list(self.attrs)
        out += struct.pack(">iii", self.nc_code, self.vsize, self.begin)
        return out


def write_netcdf(path: str,
                 dimensions: "List[Tuple[str, int]]",
                 variables: "List[dict]",
                 global_attrs: Optional[Dict[str, object]] = None) -> str:
    """Write a netCDF-3 classic file.

    Low-level; most callers want :func:`write_grid`.

    Parameters
    ----------
    dimensions:
        Ordered ``[(name, length), ...]``.
    variables:
        Each a dict with keys ``name``, ``dims`` (tuple of dim names),
        ``data`` (array), optional ``dtype`` (``"float"``/``"double"``/...),
        and optional ``attrs`` (dict).
    global_attrs:
        File-level attributes.
    """
    dim_index = {name: i for i, (name, _) in enumerate(dimensions)}
    dim_len = dict(dimensions)

    built: List[_Var] = []
    for spec in variables:
        typename = spec.get("dtype", "float")
        nc_code, dtype = _NC_TYPES[typename]
        dims = tuple(spec["dims"])
        dimids = [dim_index[d] for d in dims]
        shape = tuple(dim_len[d] for d in dims)
        data = np.asarray(spec["data"], dtype=dtype).reshape(shape)
        spec["_data"] = data
        built.append(_Var(spec["name"], dimids, nc_code, dtype, shape, spec.get("attrs")))

    # dim_list
    dim_bytes = struct.pack(">ii", _NC_DIMENSION, len(dimensions))
    for name, length in dimensions:
        dim_bytes += _pack_string(name) + struct.pack(">i", length)

    gatt_bytes = _pack_attr_list(global_attrs)

    def var_list_bytes() -> bytes:
        out = struct.pack(">ii", _NC_VARIABLE, len(built))
        for v in built:
            out += v.header()
        return out

    header_len = 4 + 4 + len(dim_bytes) + len(gatt_bytes) + len(var_list_bytes())

    offset = header_len
    for v in built:
        v.begin = offset
        offset += v.vsize

    header = b"CDF\x01" + struct.pack(">i", 0)  # numrecs = 0 (no record dim)
    header += dim_bytes + gatt_bytes + var_list_bytes()

    with open(path, "wb") as f:
        f.write(header)
        for spec, v in zip(variables, built):
            raw = spec["_data"].astype(v.dtype).tobytes()
            f.write(raw + b"\x00" * (v.vsize - len(raw)))
    return path


def write_grid(path: str,
               data,
               lats: Sequence[float],
               lons: Sequence[float],
               *,
               name: str = "data",
               times: Optional[Sequence[float]] = None,
               time_units: str = "seconds since 1970-01-01 00:00:00",
               dtype: str = "float",
               attrs: Optional[Dict[str, object]] = None,
               global_attrs: Optional[Dict[str, object]] = None) -> Grid:
    """Write *data* as a CF lat/lon grid netCDF for ``loadGrid``.

    Parameters
    ----------
    data:
        2-D ``(lat, lon)`` array, or 3-D ``(time, lat, lon)`` when *times* is given.
    lats, lons:
        1-D coordinate arrays (degrees north / east).
    name:
        Variable short name (what you pass to ``loadGrid(field=...)``).
    times:
        Optional 1-D time coordinate (numeric, in *time_units*).
    attrs:
        Extra attributes for the data variable (e.g. ``{'units': 'K',
        'long_name': 'brightness temperature'}``).
    """
    data = np.asarray(data)
    lats = np.asarray(lats, dtype="f8")
    lons = np.asarray(lons, dtype="f8")

    dims: List[Tuple[str, int]] = []
    variables: List[dict] = []
    var_dims: Tuple[str, ...]

    if times is not None:
        times = np.asarray(times, dtype="f8")
        if data.ndim != 3:
            raise ValueError("with times, data must be 3-D (time, lat, lon)")
        dims = [("time", len(times)), ("lat", len(lats)), ("lon", len(lons))]
        variables.append(dict(name="time", dims=("time",), data=times, dtype="double",
                              attrs={"units": time_units, "standard_name": "time",
                                     "long_name": "time", "axis": "T"}))
        var_dims = ("time", "lat", "lon")
    else:
        if data.ndim != 2:
            raise ValueError("data must be 2-D (lat, lon); pass times for 3-D")
        dims = [("lat", len(lats)), ("lon", len(lons))]
        var_dims = ("lat", "lon")

    if data.shape[-2:] != (len(lats), len(lons)):
        raise ValueError(
            "data trailing shape {} does not match (len(lats), len(lons)) = {}"
            .format(data.shape[-2:], (len(lats), len(lons))))

    variables.append(dict(name="lat", dims=("lat",), data=lats, dtype="double",
                          attrs={"units": "degrees_north", "standard_name": "latitude",
                                 "long_name": "latitude", "axis": "Y"}))
    variables.append(dict(name="lon", dims=("lon",), data=lons, dtype="double",
                          attrs={"units": "degrees_east", "standard_name": "longitude",
                                 "long_name": "longitude", "axis": "X"}))

    data_attrs = {"long_name": name}
    if attrs:
        data_attrs.update(attrs)
    variables.append(dict(name=name, dims=var_dims, data=data, dtype=dtype,
                          attrs=data_attrs))

    gatts = {"Conventions": "CF-1.6",
             "history": "written by mcidasv_jupyter"}
    if global_attrs:
        gatts.update(global_attrs)

    write_netcdf(path, dims, variables, gatts)
    return Grid(path=os.path.abspath(path), field=name,
                ntimes=(len(times) if times is not None else None))
