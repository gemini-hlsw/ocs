import sbt.{ State => _, Configuration => _, Show => _, _ }
import Keys._
import scalaz._
import Scalaz.{ state => _, _}
import sbt.complete.DefaultParsers._
import sbt.complete._
import edu.gemini.osgi.tools.idea.{ IdeaModule, IdeaProject, IdeaProjectTask }
import edu.gemini.osgi.tools.app.{ Application, Configuration, AppBuilder }
import java.util.jar.{ JarFile, Manifest }
import edu.gemini.osgi.tools.app.BundleSpec
import edu.gemini.osgi.tools.Version

trait OcsAppSettings { this: OcsKey =>

  lazy val ocsAppSettings = Seq(
    ocsAppInfo := showAppInfo(ocsAppManifest.value, streams.value.log),
    commands ++= Seq(
      mkIdeaCommand(ocsAppManifest.value, target.value),
      mkDistCommand(ocsAppManifest.value, target.value)
    ).flatten //,
    // incOptions := incOptions.value.withNameHashing(true)
  )

  def showAppInfo(app: Application, log: Logger): Unit = {
    def showConfig(c: Configuration): Unit = {
      val s = s"  ${c.id}" +
        (if (c.distribution.isEmpty) " «abstract»" else s" for ${c.distribution.mkString(", ")}") +
        (if (c.extending.nonEmpty) s" extending ${c.extending.map(_.id).mkString(", ")}" else "")
      log.info(s)
    }
    log.info(s"OCS Application Summary:")
    log.info(s"          id: ${app.id}")
    log.info(s"        name: ${app.name}")
    log.info(s"       label: ${app.label.getOrElse("«none»")}")
    log.info(s"     version: ${app.version}")
    log.info(s"  executable: ${app.meta.executableName(app.version)}")
    log.info(s"     configs: total ${app.configs.length} in ${app.configs.map(_.distribution).distinct.length} distributions")
    log.info("")
    app.configs.foreach(showConfig)
    log.info("")
  }    

  // // A map of lazily computed bundle jarfiles
  // def allBundleProjectArtifacts(e: Extracted, s: sbt.State): Map[ProjectRef, Need[File]] = 
  //   e.get(ocsAllBundleProjects).fproduct(p => Need {
  //     s.log.info("packaging " + e.get(name in p))
  //     e.runTask(ocsBundleIdeaModule in p, s)._2
  //   }).toMap 

  // Resolve a configuration to a complete map of all bundles needed.
  def resolveConfig(e: Extracted, s: sbt.State): Configuration => Map[BundleSpec, (File, Manifest)] = {

    // lazy map of all bundle artifacts
    val abp: Map[ProjectRef, Need[File]] =
      e.get(ocsAllBundleProjects).fproduct(p => Need {
        s.log.info("packaging " + e.get(name in p))
        e.runTask(osgiBundle in p, s)._2
      }).toMap 

    Memo.immutableHashMapMemo { c =>

      s.log.info("resolveconfig: " + c.id)

      // Compute a BundleSpec for a ProjectRef
      def spec(ref: ProjectRef): BundleSpec = {
        val n = e.get(name in ref)
        val v = e.get(version in ref)
        BundleSpec(n, Version.parse(v))
      }

      // Compute a BundleSpec for a jarfile
      def jspec(f: File): BundleSpec = 
        BundleSpec(new JarFile(f).getManifest)

      // A map of all bundle projects, keyed by spec
      val bmap: Map[BundleSpec, ProjectRef] =
        e.get(ocsAllBundleProjects).map(p => (spec(p), p)).toMap

      // A map of all library bundles, keyed by spec
      val lmap: Map[BundleSpec, File] =
        e.get(ocsLibraryBundles).map(f => (jspec(f), f)).toMap

      // partition config specs into projects and library bundles
      val (ps, bs): (List[(BundleSpec, ProjectRef)], List[(BundleSpec, (File, Manifest))]) = {
        val (ps0, bs0) = c.bundles.partition(bmap.keySet)
        (ps0.fproduct(bmap), bs0.fproduct { s => 
          val jar = lmap.get(s).getOrElse(sys.error(s"config ${c.id} references unknown bundle specified as $s"))
          (jar, new JarFile(jar).getManifest)
        })
      }

      // Expand our project list to its closure
      val cps: List[(BundleSpec, ProjectRef)] = 
        ps ++ ps.flatMap { case (_, ref) =>
          e.runTask(ocsClosure in ref, s)._2.toList.map(r => (spec(r), r))
        }

      // Expand our library bundle list to include references from all closures
      val cbs: List[(BundleSpec, (File, Manifest))] = 
        bs ++ ps.flatMap { case (_, ref) =>
          e.runTask(ocsBundleInfo in ref, s)._2.libraryBundleJars.map { f => 
            val mf = new JarFile(f).getManifest
            (BundleSpec(mf), (f, mf))
          }
        }

      // Force packaging. Done.
      val x = cbs.toMap ++ cps.map(_.rightMap(p => abp(p).fproduct(f => new JarFile(f).getManifest).value)).toMap

      s.log.info("resolveconfig: " + c.id + " [done]")

      x

    }
  }

  // /** Construct a function that returns exactly the bundles required for the given Configuration. */
  // def bsClosure(e: Extracted, s: sbt.State)(c: Configuration): Set[BundleSpec] = 
  //   referencedProjects(c, e, s).flatMap { ref =>
  //     val info = e.runTask(ocsBundleInfo in ref, s)._2
  //     val n = e.get(name in ref)
  //     val v = e.get(version in ref)
  //     println("Examining " + n + " " + v)
  //     val libs = info.libraryBundleJars.map(f => new java.util.jar.JarFile(f).getManifest).map(BundleSpec.apply)
  //     BundleSpec(n, Version.parse(v)) :: libs
  //   } .toSet

  /** 
   * Compute the closure of all bundle *projects* required by the passed Configuration by inspecting
   * its list of BundleSpecs.
   */
  def referencedProjects(c: Configuration, e: Extracted, s: sbt.State): List[ProjectRef] = {
    val log = s.log
    val allProjects: Seq[ProjectRef] = e.get(thisProject in LocalRootProject).aggregate 
    val ps = c.bundles.toList.flatMap { b => 
      allProjects.find { p =>
        val n = e.get(name in p)
        val v = e.get(version in p)
        b.name == n && b.version.toString == v
      }
    }
    val minimalSet = ps.foldLeft(ps) { (ps, p) =>
      val closure = e.runTask(ocsClosure in p, s)._2
      ps.filter(closure.contains).foreach { x =>
        log.warn(s"ignoring redundant bundle reference ${e.get(name in x)}")
      }
      ps.filterNot(closure.contains)
    }
    val closure = (minimalSet ++ minimalSet.flatMap(p => e.runTask(ocsClosure in p, s)._2)).distinct
    minimalSet.foreach(p => log.info(s"+ ${e.get(name in p)}"))
    log.info(s"total ${minimalSet.size} bundles (${closure.size} in closure)")
    // TODO: warn if the set computed here is not a subset of the projects referenced by the 
    // app project itself. Need to define ocsClosure in all projects
    closure
  }

  def mkIdeaCommand(a: Application, target: File): Option[Command] =
    a.configs.toList.toNel map { cs =>
      val config: Parser[Configuration] = cs.toList.map(c => c.id.id ^^^ c).reduce(_ | _)
      Command("ocsAppIdeaModule")(_ => Space ~> config) { (s, c) =>
        val extracted = Project.extract(s)
        val ps = referencedProjects(c, extracted, s)
        val cross = extracted.get(crossTarget)
        val top = extracted.get(baseDirectory in LocalRootProject)
        val jv = extracted.get(javaVersion)
        val (s0, si) = extracted.runTask(scalaInstance, s)
        def iml(p: ProjectRef): State[sbt.State, File] = State { s =>
          extracted.runTask(ocsBundleIdeaModule in p, s)
        }
        val (s1, imls) = ps.traverseU(iml).run(s0)
        IdeaProjectTask.go(top, cross, imls, a, c.id, si, jv, s.log)
        s1
      }
    }

  lazy val osgiBundleSymbolicName = SettingKey[String]("osgi-bundle-symbolic-name")
  lazy val osgiBundle = TaskKey[File]("osgi-bundle")

  def mkDistCommand(a: Application, target: File): Option[Command] =
    a.configs.flatMap(_.distribution).distinct.toList.toNel map { xs =>
      val dist: Parser[Configuration.Distribution] = xs.toList.map(e => e.toString.id ^^^ e).reduce(_ | _)
      Command("ocsDist")(_ => Space ~> dist) { (s, d) => 
        def now = System.currentTimeMillis
        val start = now
        val extracted = Project.extract(s)
        new AppBuilder(target, resolveConfig(extracted, s), extracted.getOpt(ocsJreDir), Set(d), s.log, extracted.get(baseDirectory)).build(a)
        s.log.info(f"Elapsed time: ${(now - start) / 1000f}%3.2f")
        s
      }
    }

}







