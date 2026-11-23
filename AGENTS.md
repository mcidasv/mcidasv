# AGENTS.md

This file provides guidance to AI coding agents working with code in this repository.

## What this is

McIDAS-V is SSEC's Java Swing/Java3D desktop application for visualizing geophysical data. It is built as a layer on top of Unidata's IDV (`ucar.unidata.idv.*`) and SSEC's VisAD (`visad.*`), both consumed as prebuilt JARs in `lib/share`. Most work here means subclassing or overriding IDV behavior, so expect to need IDV/VisAD knowledge that is not in this repo's source (only in `idv-*.jar` / `visad-*.jar`).

## Build and run

The build is Ant only (`build.xml`, ~2400 lines). There is no Maven/Gradle, no unit test suite, and no linter. Verification is: compile, then run the app.

Compiles with `source`/`target` 21 (the README's "Java 8+" is out of date). The code uses JavaFX (`util/nativepathchooser`, `servermanager/LocalEntryEditor`), so the JDK must bundle it: use Azul Zulu 21 "JDK FX" (check with `java --list-modules | grep javafx`). No separate JavaFX module path is configured in `build.xml`.

On Apple Silicon the build picks up `lib/native/macos-aarch64`, but the local ADDE server binaries (`lib/build/adde/macos-intel`) are x86_64 and need Rosetta.

```
ant build                 # compile everything into build/antbuild, copy lib jars + natives into dist/
ant jar                   # build + create dist/mcidasv-<ver>.jar and dist/local-idv-<ver>.jar
ant jar.runlarge.jpms     # build and run the app (the run target to use on modern JDKs; has the --add-opens/--add-exports flags)
ant debug.runlarge.jpms   # same, but the JVM suspends until a JDWP debugger attaches on port 8001
ant clean
ant info                  # print resolved property values
ant javadoc
ant dist                  # full release build (clean, jars, startup manager, javadoc + userguide jars)
ant nightly               # what the 4:00 AM cron job runs; does a git stash/pull --rebase first
```

`jar.runlarge` / `jar.run` / `jar.runscript` are the legacy non-JPMS variants and pass JVM flags removed from newer JDKs.

Useful `-D` overrides:

- `-Duserpath.dir=/tmp/mcidasv` — use a scratch userpath instead of the real one (`~/Documents/McIDAS-V` on macOS, `~/McIDAS-V` elsewhere). The run targets read `logback.xml` from the userpath.
- `-Daddeservers.dir=...` — location of local ADDE server binaries (defaults to `lib/build/adde/<platform>`).
- `-Duiscale=2`, `-Ddarkmode=true` — JPMS run targets only.
- `-Dpyfile=script.py` with `jar.runscript.jpms` — run a Jython script in the app.

`build-customized.xml` is a scratch file for personal Ant experiments (`ant -f build-customized.xml ...`); official build changes go in `build.xml`.

### Dependencies

All dependencies are checked-in JARs in `lib/share` (platform natives in `lib/native/<platform>`). Adding or upgrading one means touching `build.xml` in several places: the `*.version` property, the `jar.base.classpath` property, and the `mcv.libs` fileset includes. `tools/get_idv_latest.sh` and `tools/get_visad_latest.sh` (`ant getidvjars` / `ant getvisadjar`) refresh the IDV and VisAD jars.

## Architecture

### Source roots

The repo root is itself the Java source root; `javac` runs over all of it. Three package trees matter:

- `edu/wisc/ssec/mcidasv/` — McIDAS-V proper. Packaged into `mcidasv-<ver>.jar`.
- `ucar/` and `visad/` — **patched copies of individual IDV and VisAD classes**, packaged into `local-idv-<ver>.jar`, which is placed ahead of `idv.jar`/`visad.jar` on the classpath so these versions win. When a fix has to be made inside an IDV/VisAD class, the convention is to copy that class here and modify it. Before editing an IDV/VisAD class's behavior, check whether an override already exists in these trees. A stale override can also silently mask changes after an IDV/VisAD jar upgrade.
- `edu/wisc/ssec/mcidas/adde/` — ADDE protocol overrides.

### How McIDAS-V plugs into IDV

`edu.wisc.ssec.mcidasv.McIDASV` (the main class) extends `ucar.unidata.idv.IntegratedDataViewer` and overrides the `doMake*Manager()` factory methods to substitute McIDAS-V subclasses of IDV's managers: `ArgumentManager`, `StateManager`, `ResourceManager`, `PersistenceManager` (bundles), `McIdasPreferenceManager`, `JythonManager`, `McvPluginManager`, `ViewManagerManager`, `ui.UIManager`, `chooser.McIdasChooserManager`, etc. Cross-cutting behavior changes generally belong in one of these manager subclasses.

### Resource-driven registration

Much of the application is wired by XML in `edu/wisc/ssec/mcidasv/resources/`, not by code. `mcidasv.properties` sets `idv.resourcefiles` to `mcidasv.rbi`, the resource bundle index that points at every other resource file (layered: user path, site path, app path, then IDV's own defaults). Adding a new feature class typically requires a matching entry:

- data sources → `datasource.xml` (factory class + file patterns)
- choosers → `choosers.xml`
- display controls → `controls.xml`
- menus/actions/toolbar → `defaultmenu.xml`, `actions.xml`, `toolbar.xml`
- window layouts → `skins/`
- derived quantities/formulas, parameter defaults, color tables, projections, stations → the similarly named XML files

Non-code resources belong under `resources/` (`images/` is deprecated).

### Main packages under `edu.wisc.ssec.mcidasv`

- `chooser` (and `chooser.adde`) — data chooser UIs; `data` — data sources and adapters; `control` — display controls; `display` — VisAD displayables. These follow IDV's chooser → DataSource → DataChoice → DisplayControl pipeline.
- `data/hydra`, `display/hydra`, `control` (MultiSpectral, etc.) — HYDRA multi/hyperspectral satellite support (largest subsystem in `data`).
- `servermanager` — local and remote ADDE dataset management, including launching the bundled local ADDE server (`mcservl`) binaries.
- `startupmanager` — a separate small app (own JAR, `ant startupmanager`) that edits JVM/startup options consumed by the `release/runMcV*` launch scripts. It is deliberately packaged with minimal dependencies; avoid pulling main-app classes into it.
- `jython` — the interactive Jython console (also built as its own JAR).
- `ui` — `UIManager`, tabbed/window management, the Jython text components.
- `adt` — Advanced Dvorak Technique tropical cyclone analysis.

### Jython scripting layer

`resources/python/` holds the Jython (2.7) library that implements the user-facing scripting API (`background.py`, `interactive.py`, `mcvadde.py`, sensor-specific `*Functions.py`, `utilities/`). These files are loaded via the `idv.resource.jython` section of `mcidasv.rbi`, so a new library file needs an entry there. This is Jython 2.7 code running inside the JVM — Python 2 syntax, Java classes imported directly, no CPython-only packages. `ant jythondocs` generates HTML API docs from the docstrings via `tools/apidocs`.

### Other directories

- `docs/userguide/processed/` — user guide HTML, bundled into the app as a JavaHelp jar. It is synced from the web server by automated commits ("Dreamweaver updates synced from web server"); don't hand-edit as part of code changes unless asked.
- `release/` — install4j project (`mcidasv.install4j`), launcher scripts (`runMcV`, `runMcV.bat`, ...), custom JRE jmods, installer custom code. Installer/app-bundle/signing targets in `build.xml` need install4j and signing credentials passed via `-D` properties.
- `tools/` — release and maintenance scripts (`update_versions.sh`, `set_copyright.sh`, `make_new_release.sh`, ...).

## Conventions

- Commit messages: a short imperative summary followed by the McIDAS-V Inquiry number in brackets, e.g. `Fixes for Go to Address [3261]`.
- Every Java source file starts with the McIDAS-V LGPL license header (see any file under `edu/wisc/ssec/mcidasv`); the copyright year range is maintained by `tools/set_copyright.sh`.
- Version lives in `resources/version.properties`; `build.properties` is generated from it at build time and is gitignored.
- Logging is SLF4J/logback (`logback.xml` at the repo root); other logging APIs are bridged to SLF4J.
