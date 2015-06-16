
**NOTE**: This doc is written in Markdown format. You can view it locally using https://github.com/joeyespo/grip or paste it into a Gist or just read the source.

## OCS 2.1

[![Join the chat at https://gitter.im/gemini-hlsw/ocs](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/gemini-hlsw/ocs?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

This is a single rather large project that comprises a collection of **OSGi Bundles** that are assembled in various combinations to produce the following end-user applications:

- **Science Program Database** (SPDB or ODB)
- **Observing Tool**  (OT)
- **Queue Planning Tool** (QPT)
- **Phase I Tool** (PIT) and associated **p1-monitor** and **p1pdfmaker** utilities

Main differences with older builds:

- The project is built using [**sbt**](http://www.scala-sbt.org/), which provides a very nice interactive environment and fast incremental compilation. The obviates our reliance on IDEs somewhat; many tasks can be performed quite reasonably with a text editor, with sbt running alongside in a shell.
- App and bundle **versioning** is at the project level; with OCS and PIT versions defined at the top and inherited by their bundles. Branching and merging will be much more common with this strategy, so we plan to switch to **git** which makes this much easier.
- OSGi manifests are computed by [**bnd**](http://www.aqute.biz/Bnd/Bnd) based on project imports; bundles now only declare their exports. This simplifies things considerably but slows down packaging somewhat, as there is an additional bytecode analysis step to determine imports.

## Quick Start

For the impatient, here is how to get up and running:

- You need [**sbt**](http://www.scala-sbt.org/), which is most easily installed via [`homebrew`](http://brew.sh/).
- Add the following (or similar) to your shell profile:
  ```export SBT_OPTS='-XX:ReservedCodeCacheSize=256M -XX:MaxPermSize=256M -Xmx3500M -Xss2M -Dfile.encoding=UTF-8'``` 

Now, From the root of the project, run `sbt`. It will download the internet the first time you do this, and will eventually drop you at a `>` prompt. From here you can do many things. Here are some basic commands:

| sbt cmmand          | explanation 
|------------------|-------------
| `projects`       | List all projects in the build. All projects other than the root `ocs2-1` will be prefixed with `app_` or `bundle_`.
| `project <name>` | Change to the given project. *Note that tab completion works here (and everywhere).* If you're working on a specific bundle, switch to that bundle; if you're working on several bundles in the same application, switch to the application project. This will limit the extent of compilation. If you're doing a very large refactoring job or just want to be sure the whole world builds, you can do this from the top. To see where you are just type `project`.
| `project /`      | Go to the "top"; same as `project ocs2-1`.
| `compile`        | Compile the main source for the current project and its dependencies, as needed. This is always an incremental compilation; if you wish to do a full compile, `clean` first (see below).
| `test:compile`   | Compile the test sources in the current project, which implies normal `compile` as well.
| `test`           | Compile (if needed) and run tests. There is no need to define suites or anything; sbt will find and run any JUnit, Specs2, or ScalaCheck tests found under `/src/test/`. You can use `testOnly <classname>` to run a single test, or `testQuick` to re-run test that failed the previous run and/or depend directly or indirectly on code that has changed.
| `console`        | Start up a Scala REPL in the current project, with the project code and all of its dependencies available. This is very very useful for testing and experimentation. `consoleQuick` will start up with *only* your dependencies, which can be useful if your code doesn't quite compile.
| `run`            | Find any runnable classes and prompt to select one (if needed). Use `runMain <classname>` to run a specific one.
| `clean`          | Clean all output for the current project (but **not** for its dependencies; do `clean` from the top if you want a totally clean slate).
| `reload`         | Reloads the project definition. This is necessary if you change a `build.sbt` file or anything under `project/`. This is somewhat faster than restarting sbt.
| `managedSources` | Forces the build to perform code generation in this project (or everywhere if done at the top), which is necessary if you want to see generated source in IDEA (which you do).

Note that you can precede any command with `~` to repeat the command when a relevant source file changes. So `~test` will cause sbt to sit in a loop, recompiling and re-running tests as you change things.

In addition to the built-in sbt tasks, the OCS build adds some additional commands for **bundle projects**:

| command | explanation
|-------------|-------------
| `ocsBundleDependencies` | Draw a tree of bundles you depend on. To only see immediate dependencies use `ocsBundleDependencies0`.
| `ocsBundleUsers`        | Draw a tree of bundles that depend on you. To only see immediate dependencies use `ocsBundleUsers0`.
| `osgiBundle`            | Compile (if needed) and packages the bundle jarfile. To see the output path for the bundle, say `show osgiBundle`.

For **app projects** the following additional commands are available:

| command     | explanation
|-------------|-------------
| `ocsAppIdeaModule <config>` | Generate an IDEA project for the application (hit tab to see available configs). The project `.ipr` will appear under `app/<name>/idea` and the `.iml` for each bundle will appear under `bundle/<name>/`. Note that you need to build from sbt or at least run `managedSources` to ensure that the P1 and SP model generated source is available.
| `ocsDist <platform>`        | Assemble the application distribution for the given platform (hit tab to see them). The build product will appear under `app/<name>/target/<name>/...` with the same layout as in OCS1.5. Note that the `clean` command will remove this build product. If you want to build a distribution that requires a JRE or special pakaging (anything other than Test or MacOS) then you need some additional stuff. See the discussion under *Application Projects* below.

At the moment **it is not possible to bootstrap a full OSGi app from within sbt or IDEA**. You need to build the test distribution and run from there.

## Project Structure

#### Library Bundles

Library bundles in `lib/bundle/` are treated as unmanaged dependencies in the sbt build, and are referenced directly by the bundles that use them. Some of these libraries are distributed as bundles, and others were packaged manually with bnd. If you need to add a third-party library that is likely to be reused, make it into a bundle (if it is not already) and put it here. For specialized libraries that are unlikely to be needed outside a bundle, just embed the jar (described below).

#### Bundle Projects

Each project in `bundle/` produces a single OSGi bundle jar. These bundles are **not** detected by the build automatically; they are all declared explicitly in `project/OcsBundle.scala`. The structure of each bundle is as follows:

- Each bundle's `build.sbt` defines settings specific to the project (but note that settings applied to `ThisBuild` in the top-level `build.sbt` are also applied unless overridden here).
  - `name` will end up being used in many places, most notably for the bundle symbolic name.
  - `unmanagedJars` automatically includes anything in the bundle's `lib/` directory, but any other references (in general library bundles) will be given explicitly. These are added to the project's dependency classpath. *Managed* dependencies are not quite supported yet, but will be.
  - `osgiSettings` and `ocsBundleSettings` expand to a set of default settings for all bundles. Note that they *must* appear in this order. Both are defined by plugins in the top-level `project/`.
  - `OsgiKeys.*` settings are instructions for bnd.
- Library jars in a bundle project's `lib/` directory are automatically exploded into the bundle jar when it is created. This is faster and more reliable than the embedded jars and bundle-classpath used in the old build. Note that you should *not* put bundle jars here. Put them in the top-level library.
- The source in each bundle is organized conventionally, using the standard sbt layout (which is the same as Maven's).

#### Application Projects

Each project in `app/` defines an application, which is a collection of bundles plus some configuration information and potentially extra resources like scripts or icons. They are all declared in `project/OcsApp.scala`.

- The structure and implementation of the app build (the `ocsDist` and `ocsAppIdeaModule` targets, specifically) is based on OCS-1.5 and in fact delegates to the same application packaging code. The build product structure should be identical.
- Each app's `build.sbt` defines an `ocsAppManifest` setting whose value is an `Application` object. This object defines the app's name, version, etc., as well as a set of configurations, each of which may be built for one or more target platforms. This structure is essentially identical to the `manifest.xml` declarations in OCS-1.5, expressed as a Scala expression.
- If you need to build a distribution that requires a JRE, you need to tell the build where your JREs are. To do this, add a `jres.sbt` file at the top level of the build with the content `ocsJreDir in ThisBuild := file("../jres")` such that the given path has JREs arranged like those at `~software/dev/jres` on the build machine. You will need to `reload` the project to pick up this setting.
- If you want to build the Windows distribution you also need to install `makensis`, which is available via homebrew.
- If you don't feel like doing either of these things locally, you can log into `build.cl.gemini.edu` as `software` and find the project in `dev/ocs2.1/`.

**Note** the following issues with the declaration of application projects:

- The additional work done by bnd results in longer packaging times for the distribution task than what you might be used to from OCS-1.5. 
- The build dependencies declared in `project/OcsApp.scala` and the bundle dependencies declared (and calculated) by the `Application` object in each app's `build.sbt` are independent; the former is simply a convenience that allows you to build the app from sbt, and the latter is used for generating IDEA projects and app distributions.
- The `all-bundles` app is not computed; if you add a new bundle to the OCS project you will need to add it manually to `all-bundles`.






