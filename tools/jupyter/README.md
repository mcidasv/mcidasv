# McIDAS-V from Jupyter

Drive McIDAS-V's scripting engine from a notebook: write the same Jython you'd put
in a `runMcV -script` file, run it from a cell, see the image inline — and use
numpy/scipy/scikit-learn on the way in and out.

```
mcidasv_jupyter/   package
samples/           example notebooks
samples/output/    generated GIFs (gitignored)
```

## Install

Into the same environment as your Jupyter kernel:

```bash
pip install -e /path/to/mcidasv/tools/jupyter
```

Extras: `[examples]` for the sample notebooks (numpy, scipy, matplotlib,
scikit-learn, scikit-image), `[headless]` on headless Linux (needs Xvfb).

Without installing, add it to the path in the notebook instead:
`import sys; sys.path.insert(0, '/path/to/mcidasv/tools/jupyter')`.

## Quick start

```python
%load_ext mcidasv_jupyter
%mcv_connect /path/to/runMcV
```

```python
%%mcv
data = loadADDEImage(server='adde.ucar.edu', dataset='RTGOESR',
                     descriptor='M1C14', unit='TEMP', size='ALL', mag=(1, 1))
panel = buildWindow(height=600, width=800)
layer = panel[0].createLayer('Image Display', data)
layer.setEnhancement('ABI IR Temperature', range=(220, 300))
```

The image is captured from `panel[0]` and shown inline.

Object API:

```python
import mcidasv_jupyter as mcv
session = mcv.McIDASV('/path/to/runMcV')
result = session.run('''...jython...''')
result.image    # captured PNG path
result.prints   # Jython print output (McIDAS-V logs it, not stdout)
result.log      # full run log
```

Also `session.run_file('script.py')` and `@session.command`.

## `%%mcv` flags

| flag | meaning |
| --- | --- |
| `--capture panel[0]` | capture target (`none` to skip) |
| `--panel panel` / `--index 0` | target used to build the auto-capture call |
| `--size 800x600` | image size |
| `--out path.png` / `--format png` | where/what to write |
| `--push a,b` | inject notebook variables as Jython values |
| `--replay` / `--no-replay` | override the session replay setting |
| `--no-display` | don't render inline |
| `--timeout 900` | per-run timeout (s) |

Line magics: `%mcv_replay on|off`, `%mcv_reset`.

## Replay

Each `runMcV -script` is a fresh, offscreen JVM — nothing persists across cells.
**Replay (default on)** re-runs previous successful cells first, so cell 1 can build
a display and cell 2 can be just an annotation. It restarts the JVM and re-fetches
data each time, so it slows as the chain grows; `%mcv_replay off` makes cells
self-contained and `%mcv_reset` clears history.

## CPython bridge

**Values into Jython** — scalars, lists, dicts, datetimes, small numpy arrays:

```python
%%mcv --push vmin,vmax
layer.setEnhancement('ABI IR Temperature', range=(vmin, vmax))
```

or `session.run(script, values={'vmin': vmin})`.

**Arrays into McIDAS-V** — a numpy field is written to a CF-1.6 netCDF (pure numpy)
and injected as `loadGrid(...)`:

```python
session.run('''
panel = buildWindow(height=600, width=800)
layer = panel[0].createLayer('Color-Shaded Plan View', g)
panel[0].setProjection('US>CONUS')
''', arrays={'g': (field, lats, lons)})
```

`session.write_grid(...)` writes one explicitly and returns a `Grid`.
`session.animate_grid(cube, lats, lons, out='x.gif')` turns a 3-D
`(time, lat, lon)` array into a GIF (`globe=True` for the 3-D globe).

**Data back into numpy** — `extract_field` pulls the layer's VisAD data via
`getData()`: real geophysical values with true per-point navigation.

```python
field = session.extract_field('''
data = loadADDEImage(server='adde.ucar.edu', dataset='EAST',
                     descriptor='CONUSC13', unit='TEMP', size='ALL', mag=(-4, -4))
panel = buildWindow(height=400, width=500)
layer = panel[0].createLayer('Image Display', data)
''')
field.values, field.unit   # e.g. brightness temperature, 'K'
field.lats, field.lons     # navigation for every point
field.masked()             # off-earth + fill → NaN
```

`extract_fields(..., times=range(N))` pulls several time steps in one run. The
`1x`/`2x` analysis notebooks use these.

## Caveats

- **Runtime.** This only shells out to `runMcV`; it doesn't bundle McIDAS-V. The
  launcher needs an assembled runtime (`release/lib/mcidasv*.jar` + deps) — if
  `release/lib/` is empty, point at an installed McIDAS-V. Failures raise
  `McIDASVError` with the log attached rather than producing a blank image.
- **Animation is slow.** `animate_grid` renders one frame per McIDAS-V process
  (~20s/frame): McIDAS-V won't animate a computed time-grid, and grids captured
  within one process come out identical. `slow_render=False` is faster but usually
  yields identical frames (it warns when it does).
- **Extraction cost.** Values are written one float at a time from Jython, so
  subsample at request time (`mag=(-4, -4)`) rather than pulling full resolution.
- **Image-based analysis is approximate.** Reading a capture with `imread` gives
  RGB pixels and assumes a linear lat/lon box over a projected image, so results
  can be off by tens of km. Use `extract_field` when accuracy matters.
- **Display types** (`'Color-Shaded Plan View'`, `'Contour Plan View'`) must match
  an entry in your Field Selector.

## Notebooks

Basics:

| | |
| --- | --- |
| `00_getting_started` | connect, capture, replay |
| `01_realtime_goes_ir` | real-time GOES-R IR (docs `example18.py`) |
| `02_blizzard_1993` | 1993 blizzard, projection + annotation (docs `example15.py`) |
| `03_image_enhancement` | enhancements, annotations, colorbars |

CPython bridge:

| | |
| --- | --- |
| `10_python_parameters` | compute parameters in numpy/pandas, inject them |
| `11_numpy_field_to_mcidasv` | numpy field → grid → display |
| `12_scipy_processing_to_mcidasv` | scipy filters/edges → display |
| `13_analyze_mcidasv_output` | the *image* path: capture → numpy (approximate) |
| `14_numpy_animation` | 3-D numpy stack → animated GIF |
| `15_ml_cloud_classification` | KMeans on brightness temperature |

Visual:

| | |
| --- | --- |
| `16_goes_storm_growth` | cloud-top cooling rate (K/step), animated |
| `17_reaction_diffusion` | Gray-Scott simulation rendered by McIDAS-V |
| `18_spherical_harmonics_globe` | harmonics on the 3-D globe, rotating |
| `19_cloud_motion_optical_flow` | optical-flow cloud motion in km/h |

Weather + ML:

| | |
| --- | --- |
| `20_cloud_type_gmm` | temperature + texture features → Gaussian Mixture Model |
| `21_storm_detection_tracking` | detect cores below 220 K, track them in km |
| `22_eof_pca_ir_sequence` | EOF/PCA of temperature anomalies |
| `23_direct_field_extraction` | real values + navigation, regridded and pushed back |

The `1x`/`2x` notebooks set `%mcv_replay off` (cells are independent).
