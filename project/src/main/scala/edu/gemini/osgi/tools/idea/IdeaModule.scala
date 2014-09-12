package edu.gemini.osgi.tools.idea

import edu.gemini.osgi.tools.FileUtils._
import IdeaModule._
import java.io.{FileFilter, File}
import edu.gemini.osgi.tools.{Version, BundleVersion}

class IdeaModule(
  dir: File,          // folder the IML file will be in
  depMods: Seq[String], // modules we need to refer to
  depJars: Seq[File], // project-local jars
  testJars: Seq[File] // jars only used for testing
  ) {

  def moduleRelativePath(to: File): String =
    "$MODULE_DIR$/%s".format(relativePath(dir, to))

  def module: xml.Elem =
    <module type="JAVA_MODULE" version="4">
      {scalaComponent}
      {rootComponent}
    </module>

  def scalaComponent: xml.Elem =
    <component name="FacetManager">
      <facet type="scala" name="Scala">
        <configuration>
          <option name="compilerLibraryLevel" value="Project" />
          <option name="compilerLibraryName" value={"scala-compiler"} />
          <option name="languageLevel" value="Scala 2.10" />
        </configuration>
      </facet>
    </component>

  def rootComponent: xml.Elem =
    <component name="NewModuleRootManager" inherit-compiler-output="true">
      <content url="file://$MODULE_DIR$">
        {srcDirs(false).map(d => sourceFolder(d, false))}
        {mgdSrcDirs.map(d => sourceFolder(d, false))}
        {srcDirs(true).map(d => sourceFolder(d, true))}
        {excludeDirs.map(excludeFolder)}
      </content>
      <orderEntry type="inheritedJdk" />
      <orderEntry type="sourceFolder" forTests="false" />
      {depMods.map(bv => bundleDependency(bv))}
      {depJars.map(j => libraryDependency(j))}
      {testJars.map(j => libraryDependency(j, isTest = true))}
      {/*testScopeDependencies(imlFile)*/}
    </component>

  def srcDirs(isTest: Boolean): List[File]= {
    val srcMain = new File(dir, "src/" + (if (isTest) "test" else "main"))
    if (srcMain.isDirectory) srcMain.listFiles.toList else Nil
  }

  def mgdSrcDirs: List[File] =
    findFile(new File(dir, "target"), "src_managed").toList.flatMap(childDirs)

  def excludeDirs: List[File] = {
    def exclude(fs: List[File], path: String, res: List[File]): List[File] = fs match {
      case Nil      => res
      case (h :: t) =>
        if (h.getPath == path) exclude(t, path, res)
        else if (path.startsWith(h.getPath)) exclude(childDirs(h) ++ t, path, res)
        else exclude(t, path, h :: res)
    }

    val root = new File(dir, "target")
    val mgd  = findFile(root, "src_managed")
    mgd.fold(List(root)) { m => exclude(List(root), m.getPath, Nil) }
  }

  def sourceFolder(f: File, isTest: Boolean): xml.Elem =
    <sourceFolder url={"file://%s".format(moduleRelativePath(f))} isTestSource={isTest.toString} />

  def excludeFolder(f: File): xml.Elem =
    <excludeFolder url={"file://%s".format(moduleRelativePath(f))} />

  def bundleDependency(mod: String): xml.Elem =
    <orderEntry type="module" module-name={mod} exported="" />

  def libraryDependency(jar: File, isTest: Boolean = false): xml.Elem =
    <orderEntry type="module-library" scope={if (isTest) "TEST" else ""} exported="" >
      <library>
        <CLASSES>
          <root url={"jar://%s!/".format(moduleRelativePath(jar))} />
        </CLASSES>
      </library>
    </orderEntry>

  /**
   * Keep any already defined test-scoped deps.  If none, add given deps.
  def testLibraryDependencies(iml: File): xml.NodeSeq = {
    val existing = testScopeDependencies(iml)
    if (existing.isEmpty) testJars.map(j => libraryDependency(j, true)) else existing
  }
   */
}

object IdeaModule {

  val srcDirFileFilter = new FileFilter {
    def accept(f: File) =
      f.isDirectory && f.getName.startsWith("src") && f.getName != "src-test"
  }

  /**
   * Returns the orderEntry elements for all "TEST" scoped dependencies in the
   * given module file, if it exists.
   */
  def testScopeDependencies(iml: File): xml.NodeSeq =
    if (!iml.exists()) xml.NodeSeq.Empty else extractTestEntries(xml.XML.loadFile(iml))

  private def extractTestEntries(root: xml.Elem): xml.NodeSeq =
    (root \\ "orderEntry") filter {
      oe => oe.attribute("scope").exists(_.text == "TEST")
    }

  def moduleName(bl: BundleLoc): String = moduleName(bl.name, bl.version)
  def moduleName(bv: BundleVersion): String = moduleName(bv.manifest.symbolicName, bv.manifest.version)
  def moduleName(root: String, v: Version): String = "%s-%s".format(root, v)
}
