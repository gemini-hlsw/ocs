# AGENTS.md — OCS Coding Agent Guide

## Project Overview

OCS (Observing Control System) is the Gemini Observatory science operations software. It is a **large OSGi bundle-based monorepo** built with sbt (Scala 2.11 / Java 8), producing four main applications:

| App | sbt project | Description |
|-----|------------|-------------|
| SPDB (ODB) | `app_spdb` | Science Program Database — the central server |
| OT | `app_ot` | Observing Tool — desktop GUI for astronomers |
| QPT | `app_qpt` | Queue Planning Tool |
| PIT | `app_pit` | Phase I Proposal Tool |
| ITC | `app_itc` | Integration Time Calculator service |
| AGS | `app_ags` | Automated Guide Star servlet |
| P1 Monitor | `app_p1_monitor` | Phase I proposal monitor |
| P1 PDF Maker | `app_p1pdfmaker` | Phase I PDF generation utility |
| Weather | `app_weather` | Weather reporting service |
| EPICS ACM | `app_epics_acm` | EPICS Action/Configuration/Monitor interface |

## Repository Structure

```
bundle/edu.gemini.<name>/   ← OSGi bundles (one per directory)
  build.sbt                 ← bundle-specific OSGi settings & exports
  src/main/{java,scala}/    ← mixed Java/Scala sources
  src/test/{java,scala}/    ← JUnit 4, Specs2, ScalaCheck tests
  lib/                      ← jars exploded into this bundle at package time
app/<name>/                 ← application assembly definitions
  build.sbt                 ← ocsAppManifest: Application(...) declaration
lib/bundle/                 ← unmanaged third-party OSGi jar dependencies
project/
  OcsBuild.scala            ← root Build object mixing in all traits
  OcsBundle.scala           ← ALL inter-bundle dependencies declared here
  OcsApp.scala              ← application-to-bundle wiring
  OcsAppSettings.scala      ← ocsDist / ocsAppIdeaModule command implementations
  OcsBundleSettings.scala   ← custom sbt tasks (ocsBundleDependencies, etc.)
  OcsKey.scala              ← custom sbt keys (ocsVersion, pitVersion, etc.)
  OcsCredentials.scala.template ← copy to OcsCredentials.scala for Artifactory creds
```

## Key Domain Bundles

- **`edu.gemini.spModel.core`** — Scala ADTs for astronomical domain: `Target` (sealed `TooTarget | SiderealTarget | NonSiderealTarget`), `Coordinates`, `Angle`, `ProgramId`, `Magnitude`, etc. Uses scalaz heavily (`\/`, `Order`, lenses).
- **`edu.gemini.pot`** — Java `ISP*` interfaces (Science Program tree): `ISPProgram → ISPObservation → ISPObsComponent | ISPSeqComponent`. This is the core in-memory SP tree. Also contains all `spModel.gemini.*` instrument data objects.
- **`edu.gemini.spModel.io`** — XML serialization/deserialization of the SP tree (PIO layer via `edu.gemini.spModel.pio`).
- **`edu.gemini.sp.vcs`** — Version Control System for science programs; three-way merge of SP trees between OT and SPDB.
- **`edu.gemini.util.trpc`** — Typed RPC mechanism: Java dynamic proxies over HTTPS; services are interfaces declared in one bundle and implementations registered in OSGi by another. Clients call `TrpcClient` pointing at a `Peer` (host/port).
- **`edu.gemini.catalog`** — VOTable-based guide star catalog access (Simbad, UCAC4, etc.). Provides `CatalogQuery` and `CatalogResult` types. Depended on by `edu.gemini.ags`.
- **`edu.gemini.ags`** — Automated Guide Star selection logic. Pure Scala; depends on `catalog` and `pot` (instrument configurations drive constraints).
- **`edu.gemini.model.p1`** — Phase I proposal model. Scala generated from XML schema at build time via `managedSources`. PIT and P1 monitor depend on this; it does **not** depend on `pot`.
- **`edu.gemini.wdba.xmlrpc.server`** — Workflow DataBase Adapter: XML-RPC bridge between the SPDB and the TCS/sequence executor. Paired with `edu.gemini.wdba.xmlrpc.api` (interface) and `edu.gemini.wdba.session.client` (OT-side client).
- **`jsky.app.ot`** — Main OT GUI bundle (Swing); depends on virtually the entire bundle graph. Changes here ripple broadly.

## Build & Development Workflows

**Required:** Java 8 (`1.8`), sbt. Set JVM options:
```sh
export SBT_OPTS='-XX:ReservedCodeCacheSize=512M -Xmx3500M -Xss2M -Dfile.encoding=UTF-8'
```

**sbt interactive session (preferred):**
```
sbt                               # start interactive shell
project bundle_edu_gemini_pot     # focus on one bundle (faster compilation)
compile                           # incremental compile
test                              # run JUnit/Specs2/ScalaCheck tests
~test                             # watch mode
testOnly edu.gemini.pot.SomeSpec  # single test
testQuick                         # re-run failed or source-changed tests only
console                           # Scala REPL with project code and deps
consoleQuick                      # Scala REPL with deps only (when code won't compile)
reload                            # reload after build.sbt or project/ changes
managedSources                    # force code generation (needed for IDEA)
osgiBundle                        # build the OSGi jar
ocsBundleDependencies             # show full dependency tree
ocsBundleUsers                    # show who depends on this bundle
```

**Build a distributable app:**
```
project app_ot
ocsDist Test           # produces app/ot/target/ot/... (no JRE required)
ocsDist MacOS          # macOS distribution (no JRE required)
```
Platforms requiring a JRE directory (`jres.sbt` at repo root with `ocsJreDir in ThisBuild := file("../jres")`): `Linux`, `Windows`. Windows also requires `makensis` (available via homebrew).

**IDEA project generation (after managedSources):**
```
project app_ot
ocsAppIdeaModule Test   # generates .ipr under app/ot/idea/
```

> **Note:** OSGi applications cannot be launched from sbt or IDEA directly. Build a distribution first and run from there.

## OSGi Bundle Conventions

Every bundle's `build.sbt` must include `osgiSettings` then `ocsBundleSettings` (in that order), declare `OsgiKeys.bundleSymbolicName`, and list only its **exported** packages in `OsgiKeys.exportPackage`. Imports are computed by bnd from bytecode — do **not** list imports manually.

Bundle jar dependencies (non-OSGi third-party libs) go in the bundle's `lib/` directory and are **exploded** into the bundle jar at package time — not added to the bundle classpath.

Inter-bundle dependencies are **only** declared in `project/OcsBundle.scala`. Adding a bundle to the project requires a new `lazy val bundle_...` entry there and a corresponding entry in `app/all-bundles/build.sbt`.

When a bundle's tests need types from another bundle's test sources, use the scope modifier in `dependsOn`: `bundle_edu_gemini_pot % "test->test;compile->compile"`. The `compile->compile` clause must be included alongside `test->test` when main-scope code is also needed.

## Cross-Cutting Patterns

- **Functional style:** Scala code uses scalaz 7.2.27 (`\/` for errors, `scalaz.concurrent.Task`, `State`, `Lens`). Avoid `throw`; use `TrpcException` or `\/` at service boundaries.
- **Mixed Java/Scala:** Older domain objects (instruments, SP nodes) are Java; newer domain types (`spModel.core.*`) are Scala. Both live side-by-side in the same bundle.
- **OSGi services:** Bundle activators (`OsgiKeys.bundleActivator`) register/unregister services in the OSGi registry. Consumers look up services via the registry or receive them via dependency injection from the OSGi container.
- **Versioning:** `ocsVersion` and `pitVersion` in the root `build.sbt` drive the OSGi bundle version and generate `CurrentVersion.java` via `sourceGenerators` in `edu.gemini.spModel.core`.
- **Code generation:** P1 model (`edu.gemini.model.p1`) generates Scala from XML schema. Run `managedSources` before opening in an IDE.

## Publishing

Artifacts are published to Gemini Artifactory. For cross-Scala-version publishing (e.g., 2.13), follow `PUBLISH.txt` using `++2.13.1` prefix in sbt.

