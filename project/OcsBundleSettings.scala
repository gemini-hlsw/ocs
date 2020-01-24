import sbt.{ State => _, Configuration => _, Show => _, File => _, _ }
import Keys._
import scalaz._
import Scalaz.{ state => _, _}
import sbt.complete.DefaultParsers._
import sbt.complete._
import edu.gemini.osgi.tools.idea.{ IdeaModule, IdeaProject, IdeaProjectTask }
import edu.gemini.osgi.tools.app.{ Application, Configuration, AppBuilder }
import xml.PrettyPrinter
import java.io.File

trait OcsBundleSettings { this: OcsKey =>

  lazy val osgiEmbeddedJars = TaskKey[Seq[File]]("osgi-embedded-jars")
  lazy val osgiExplodedJars = TaskKey[Seq[File]]("osgi-exploded-jars")

  // Bundle Settings
  lazy val ocsBundleSettings = Seq(

    // // Blow up jarfiles in lib/
    // resourceGenerators in Compile += Def.task[Seq[java.io.File]] {
    //   val n   = name.value
    //   val s   = state.value
    //   val out = (resourceManaged in Compile).value
    //   val ff = FileFunction.cached(target.value / "blowup-cache", FilesInfo.lastModified, FilesInfo.exists) { (fs: Set[File]) =>
    //     fs.filter(_.getName.endsWith(".jar")).flatMap { j =>
    //       s.log.info(s"$n: exploding ${j.getName}")
    //       IO.unzip(j, out, (_: String) != "META-INF/MANIFEST.MF")
    //     }
    //   }
    //   val jars = Option(unmanagedBase.value.listFiles).map(_.toList.filter(_.getName.endsWith(".jar"))).getOrElse(Nil).toSet
    //   ff(jars).toList
    // }.taskValue,

    osgiExplodedJars ++= {
      Option(unmanagedBase.value.listFiles).map(_.toList.filter(_.getName.endsWith(".jar"))).getOrElse(Nil)
    },

    ocsProjectDependencies := thisProject.value.dependencies.collect {
      case ResolvedClasspathDependency(r @ ProjectRef(_, _), _) => r
    },

    ocsUsers := {
      val s = state.value
      val extracted = Project.extract(s)
      val ref = thisProjectRef.value
      ocsAllBundleProjects.value.filter(p => extracted.get(ocsDependencies in p).contains(ref))
    },

    ocsProjectAggregate := thisProject.value.aggregate,

    ocsDependencies := ocsProjectDependencies.value ++ ocsProjectAggregate.value,

    ocsBundleIdeaModuleName := s"${name.value}-${version.value}",

    ocsBundleIdeaModuleAbstractPath := baseDirectory.value / (ocsBundleIdeaModuleName.value + ".iml"),

    ocsBundleIdeaModule := {
      val iml = ocsBundleIdeaModuleAbstractPath.value
      val modules: Seq[String] = {
        val s = state.value
        val extracted = Project.extract(s)
        ocsDependencies.value.map(p => extracted.get(ocsBundleIdeaModuleName in p))
      }
      val classpath = ((managedClasspath in Compile).value ++ (unmanagedJars in Compile).value).map(_.data)
      val testClasspath= ((managedClasspath in Test).value ++ (unmanagedJars in Test).value).map(_.data) filterNot (classpath.contains)
      IO.createDirectory(iml.getParentFile)
      val mod = new IdeaModule(iml.getParentFile, modules, classpath, testClasspath)
      IO.writeLines(iml, List(new PrettyPrinter(132, 2).format(mod.module)), IO.utf8)
      streams.value.log.info("IDEA module: " + iml)
      iml
    },

    ocsClosure := computeOnce(baseDirectory.value + " closure") {
      val s = state.value
      val extracted = Project.extract(s)
      val ds = ocsDependencies.value
      (ds ++ ds.flatMap(p => extracted.runTask(ocsClosure in p, s)._2)).distinct
    },

    ocsBundleDependencies := {
      val extracted = Project.extract(state.value)
      val tree = Tree.unfoldTree(thisProjectRef.value) { p =>
        (extracted.get(name in p), () => extracted.get(ocsDependencies in p).toStream)
      }
      printTree(tree)
    },

    ocsBundleDependencies0 := {
      val extracted = Project.extract(state.value)
      val tree = Tree.Node(name.value, ocsDependencies.value.toStream.map { p =>
        Tree.Node(extracted.get(name in p), Stream.empty)
      })
      printTree(tree)
    },

    ocsBundleUsers := {
      val s = state.value
      val extracted = Project.extract(s)
      val tree = Tree.unfoldTree(thisProjectRef.value) { p =>
        (extracted.get(name in p), () => extracted.runTask(ocsUsers in p, s)._2.toStream)
      }
      printTree(tree)
    },

    ocsBundleUsers0 := {
      val extracted = Project.extract(state.value)
      val tree = Tree.Node(name.value, ocsUsers.value.toStream.map { p =>
        Tree.Node(extracted.get(name in p), Stream.empty)
      })
      printTree(tree)
    },

    ocsBundleInfo := {mkBundleInfo.value} //,

    // incOptions := incOptions.value.withNameHashing(true)

  )

  // A terrible thing, but necessary for some statically-computable properties like the dependency
  // closure that cannot be computed as settings because sbt is terrible
  protected var computeOnceCache: Map[String, Any] = Map.empty // sigh
  protected def computeOnce[A](key: String)(a: => A): A =
    computeOnceCache.get(key) match {
      case Some(x) => x.asInstanceOf[A]
      case None    =>
        val x = a
        synchronized { computeOnceCache += (key -> x) }
        x
    }


  case class OcsBundleInfo(
    embeddedJars: List[File],
    libraryBundleJars: List[File],
    unmooredJars: List[File], // the presence of anything here means the bundle can't be packaged
    bundleProjectRefs: List[ProjectRef])

  val mkBundleInfo: Def.Initialize[Task[OcsBundleInfo]] = {
    Def.task {

      computeOnce(baseDirectory.value + " info") {

        // Setup
        val s   = state.value
        val log = streams.value.log
        val ex  = Project.extract(s)

        s.log.info("Computing bundle info for " + name.value)

        // We need to know how to do this
        def bundleName(jar: File): Option[String] =
          Option(new java.util.jar.JarFile(jar).getManifest.getMainAttributes.getValue("Bundle-SymbolicName"))

        // All class dirs and jars in classpath
        val full: List[File] =
          ((managedClasspath in Compile).value ++ (externalDependencyClasspath in Compile).value).toList.map(_.data.getCanonicalFile).distinct.sortBy(_.toString)
          // (fullClasspath in Compile).value.toList.map(_.data.getCanonicalFile).distinct.sortBy(_.toString)

        // All class dirs and embedded jars from dependent projects
        val proj: Set[File] = ocsDependencies.value.flatMap { p =>
          ex.get(classDirectory in (p, Compile)) +: ex.runTask(osgiEmbeddedJars in p, s)._2
        }.map(_.getCanonicalFile).distinct.toSet

        // Dependencies other than bundle projects. These are library bundles or normal library jars
        val external = full.filterNot(proj)
        val externalJars = external.filter(_.isFile)
        val withName = externalJars.fproduct(bundleName)
        val (bundleJars, libJars) = withName.partition(_._2.isDefined).umap(_.map(_._1))
        val projectRefs = ocsClosure.value.distinct.sorted.toList

        // Done
        OcsBundleInfo(osgiEmbeddedJars.value.toList, bundleJars, libJars, projectRefs)

      }

    }
  }

  // val showBundleInfo = {
  //   lazy val osgiEmbeddedJars = TaskKey[Seq[File]]("osgi-embedded-jars")
  //   Def.task {
  //     val s   = state.value
  //     val log = streams.value.log
  //     val ex  = Project.extract(s)

  //     def bundleName(jar: File): Option[String] =
  //       Option(new java.util.jar.JarFile(jar).getManifest.getMainAttributes.getValue("Bundle-SymbolicName"))

  //     // All class dirs and jars in classpath
  //     val full: List[File] =
  //       (fullClasspath in Compile).value.toList.map(_.data.getCanonicalFile).distinct.sortBy(_.toString)

  //     // All class dirs and embedded jars from dependent projects
  //     val proj: Set[File] = ocsDependencies.value.flatMap { p =>
  //       ex.get(classDirectory in (p, Compile)) +: ex.runTask(osgiEmbeddedJars, s)._2
  //     }.map(_.getCanonicalFile).distinct.toSet

  //     // Dependencies other than bundle projects. These are library bundles or normal library jars
  //     val external = full.filterNot(proj)

  //     // None of these should be dirs
  //     // (external.filter(_.isDirectory)) ...

  //     val externalJars = external.filter(_.isFile)

  //     val withName = externalJars.fproduct(bundleName)

  //     val (bundleJars, libJars) = withName.partition(_._2.isDefined)

  //     val projectRefs = ocsClosure.value.map(p => ex.get(name in p)).distinct.sorted

  //     log.info("")
  //     log.info(s"Bundle Info for ${name.value}")
  //     if (osgiEmbeddedJars.value.nonEmpty) {
  //       log.info("")
  //       log.info("  Embedded Jars:")
  //       osgiEmbeddedJars.value.sorted.foreach(f => log.info(s"    $f"))
  //     }
  //     if (projectRefs.nonEmpty) {
  //       log.info("")
  //       log.info("  Project References:")
  //       projectRefs.map(name => log.info(s"    $name"))
  //     }
  //     if (bundleJars.nonEmpty) {
  //       log.info("")
  //       log.info("  Referenced Library Bundles:")
  //       bundleJars.collect { case (f, Some(n)) => n } .distinct.sorted.foreach(n => log.info(s"    $n"))
  //     }
  //     if (libJars.nonEmpty) {
  //       log.info("")
  //       log.info("  Referenced Library Jars: (MUST be embedded or wrapped)")
  //       libJars.map(_._1).foreach { f =>
  //         log.info(s"    $f")
  //       }
  //     }
  //     log.info("")
  //   }
  // }

  // Tree from a closure, given a root.
  def treeFrom[A](a: A, m: Map[A, Stream[A]], full: Boolean): Tree[A] =
    Tree.Node(a, unfoldForest(~m.get(a))(p => (p, () => if (full) ~m.get(p) else Stream.empty)))

  // Print a tree in a slighly prettier form than default, using toString for elements
  def printTree[A](t: Tree[A]): Unit =
    t.drawTree(Show.showA).zipWithIndex.filter(_._2 % 2 == 0).map(_._1).foreach(println)

}
