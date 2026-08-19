# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build System

This is a large OSGi-based project built with sbt. The project consists of ~100+ OSGi bundles assembled into various applications.

### Essential Build Commands

```bash
# Start sbt shell (recommended for interactive development)
sbt

# From within sbt shell:
compile                    # Compile current project and dependencies
test                      # Run tests (JUnit, ScalaCheck, Specs2, ScalaTest)
testOnly <classname>      # Run a single test class
testQuick                 # Re-run failed tests or tests affected by changes
~test                     # Continuous test mode (re-runs on file changes)
console                   # Start Scala REPL with project code available
clean                     # Clean current project outputs
reload                    # Reload build definition after changing build.sbt
managedSources           # Generate sources (needed before opening in IDEA)

# Bundle-specific commands:
osgiBundle               # Package the bundle as OSGi jar
show osgiBundle          # Show output path for the bundle
ocsBundleDependencies    # Show bundle dependency tree
ocsBundleUsers           # Show bundles that depend on this one

# App-specific commands:
ocsAppIdeaModule <config>  # Generate IDEA project (tab for configs)
ocsDist <platform>         # Build application distribution (tab for platforms)
```

### sbt Workflow Best Practices

**Use sbt shell for development**: Start `sbt` once and use commands within the shell rather than `sbt <command>` for each operation. This avoids JVM startup overhead.

**Parallel operations**: When running multiple independent bash commands (like `git status` and `git diff`), always batch them in a single tool call for optimal performance.

**Testing patterns**: Use `testOnly *ClassName` for focused testing, `testQuick` for iterative development, and `~test` for continuous feedback during implementation.

### Running a Single Test

```bash
# From sbt shell
testOnly *TestClassName
testOnly edu.gemini.package.TestClassName

# Or use testQuick to run only failed/affected tests
testQuick
```

## Architecture Overview

### Project Structure

```
ocs/
├── app/           # Applications (OT, SPDB, QPT, PIT, etc.)
├── bundle/        # OSGi bundles (~100+ modules)
├── lib/bundle/    # Third-party library bundles
└── project/       # Build configuration
```

### Key Applications

- **OT (Observing Tool)**: Main observation planning application
- **SPDB/ODB (Science Program Database)**: Database server for science programs
- **QPT (Queue Planning Tool)**: Queue scheduling and planning
- **PIT (Phase I Tool)**: Proposal submission tool
- **AGS**: Automatic guide star selection service
- **ITC**: Integration time calculator

### Core Architecture Patterns

1. **OSGi Modularity**: Each bundle declares exports; imports are computed by bnd
2. **Shared Core Models**: `edu.gemini.spModel.core` contains domain models used across applications
3. **Service Communication**: TRPC for remote services, servlets for web APIs
4. **Plugin Architecture**: Applications are composed of bundles declared in manifest files

### Key Dependencies Between Modules

- Most bundles depend on `edu.gemini.util.*` for utilities
- UI bundles depend on `jsky.*` components for astronomical visualizations
- Applications aggregate bundles via `Application` manifests in their `build.sbt`
- The `edu.gemini.pot` (Program Object Tree) bundle is central to observation management

### Build Configuration

- Scala 2.11.12 with Java 1.8 target
- OSGi manifest generation via bnd
- Version management: `ocsVersion` and `pitVersion` in root `build.sbt`
- Bundle dependencies declared in `project/OcsBundle.scala`
- App compositions defined in each app's `build.sbt` via `ocsAppManifest`

### Testing Approach

- Test frameworks: JUnit, ScalaCheck, Specs2, ScalaTest
- Tests auto-discovered under `src/test/`
- Fork mode enabled for test isolation
- No explicit test suites needed - sbt finds all tests automatically

### Distribution Building

To build a full distribution:
1. Ensure JREs are available (create `jres.sbt` if needed)
2. Run `ocsDist <platform>` from the app project
3. Output appears in `app/<name>/target/<name>/`

Note: Full OSGi app cannot be bootstrapped from sbt/IDEA - build test distribution and run from there.
