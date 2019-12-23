package edu.gemini.osgi.tools.app

import java.io.{File, PrintWriter, IOException}
import java.nio.file.{Files, SimpleFileVisitor, FileVisitResult, Path, FileSystems}
import java.nio.file.attribute.BasicFileAttributes
import edu.gemini.osgi.tools.app.{copy => recursivecopy}

import java.util.jar.Manifest

import scala.xml.dtd.{ PublicID, DocType }
import scala.xml.XML

object MacDistHandler {
  val launcher = "JavaAppLauncher"
  val jdkInfo = "Info.plist"

  // These will change when the certifcate expires but that's likely ok
  // Note that these ids are not very useful without the actual certificate installed
  val certificateHash = "60464455CE099B3293643BC0021D1F25D2F52A59"
  val signatureID = "T87F4ZD75E"

  val jarMatcher = FileSystems.getDefault().getPathMatcher("glob:.jar")
  val dylibMatcher = FileSystems.getDefault().getPathMatcher("glob:.dylib")
}

case class MacDistHandler(jre: Option[String], jreName: String) extends DistHandler {

  def build(outDir: File, jreDir: Option[File], meta: ApplicationMeta, version:String, config: Configuration, d: Configuration.Distribution, solution: Map[BundleSpec, (File, Manifest)], log: sbt.Logger, appProjectBaseDir: File) {

    // Output dirs
    val name = meta.osxVisibleName(version)
    val appDir = mkdir(outDir,  s"$name.app")
    val trashDir = mkdir(appDir, ".Trash")  // http://stackoverflow.com/questions/18621467/error-creating-disk-image-using-hdutil
    val contentsDir = mkdir(appDir, "Contents")
    val macosDir = mkdir(contentsDir, "MacOS")
    val resourcesDir = mkdir(contentsDir, "Resources")
    val jrePluginDir = mkdir(contentsDir, "PlugIns")

    // Info.plist and PkgInfo into contents
    val (fwJar, fwMf) = solution(config.framework.bundleSpec)
    val plist = InfoPlist(meta, version, fwJar, fwMf.getMainAttributes.getValue("Main-Class"), config.props, config.vmargsWithApp(name), config.icon, jreName)
    write(new File(contentsDir, "PkgInfo")) { _.write(Array[Byte]('A', 'P', 'P', 'L', '?', '?', '?', '?')) }
    write(new File(contentsDir, "Info.plist")) { os => closing(new PrintWriter(os))(plist.write) }

    // JavaApplicationStub into macos
    val jas = new File(macosDir, MacDistHandler.launcher)
    write(jas) { os => closing(getClass.getResourceAsStream(MacDistHandler.launcher)) { stream(_, os) } }

    // Copy the JRE (if any)
    jre foreach { path =>
      jreDir match {
        case None => sys.error("No JRE dir was specified.")
        case Some(jreDir) =>
          val jrePath = new File(jreDir, path)
          if (!jrePath.isDirectory) sys.error("No JRE was found at " + jrePath)
          jrePath.listFiles.foreach{f: File => recursivecopy(f, mkdir(jrePluginDir, jreName))}

          // JDK's Info.plist must be in the jre path even though it is not in the standard distribution
          // It must be updated when we change JDK version
          val jdkInfo = new File(new File(jrePluginDir, jreName), MacDistHandler.jdkInfo)
          Files.copy(getClass.getResourceAsStream(MacDistHandler.jdkInfo), jdkInfo.toPath)
      }
    }

    import sys.process._
    val ret = Seq("chmod", "755", jas.getPath).!
    if (ret != 0) sys.error("chmod returned " + ret)

    // Copy the icon file, if it exists
    config.icon.foreach { f => recursivecopy(f, resourcesDir) }

    // OSGi app layout
    buildCommon(resourcesDir, meta, version, config, d, solution, appProjectBaseDir)

    // Check if we can sign
    val cmd = Seq("security", "find-identity", "-v", "-p", "codesigning")
    val canSign = cmd.lines.exists(_.contains(MacDistHandler.signatureID))
    if (canSign) {
      println("Signing code with developer ID")
      // Sign each jar and dylib file
      Files.walkFileTree(new File(appDir.getPath).toPath, new SimpleFileVisitor[Path]() {
        override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
          if (MacDistHandler.jarMatcher.matches(file) || MacDistHandler.dylibMatcher.matches(file)) {
            Seq("codesign", "-f", "--options", "runtime", "-v", "-s", MacDistHandler.certificateHash, file.toFile.getAbsolutePath).!
          }
          FileVisitResult.CONTINUE
        }
      })

      // Now sign the jre
      if (Seq("codesign", "-f", "--options", "runtime", "-v", "-s", MacDistHandler.certificateHash, new File(jrePluginDir, jreName).getAbsolutePath).! != 0) {
        log.error("*** Codesign error ")
      }

      // The .Trash dir must be deleted
      Files.walkFileTree(new File(appDir.getPath, ".Trash").toPath, new SimpleFileVisitor[Path]() {
        override def postVisitDirectory(dir: Path, ex: IOException): FileVisitResult = {
          Files.delete(dir);
          FileVisitResult.CONTINUE
        }
      })

      // Finally sign the app
      if (Seq("codesign", "-f", "--options", "runtime", "-v", "-s", MacDistHandler.certificateHash, appDir.getPath).! != 0) {
        log.error("*** Codesign error")
      }
    }

    // Now create the DMG
    val volname = "%s_%s".format(meta.executableName(version), d.toString.toLowerCase)
    val dmgname = volname + ".dmg"
    val dest = new File(outDir, dmgname).getPath
    val args = Array("hdiutil", "create", "-size", "500m", "-srcfolder", appDir.getPath, "-volname", volname, dest)
    val result = Runtime.getRuntime.exec(args).waitFor()
    if (result != 0) {
      log.error("*** " + args.mkString(" "))
      log.error("*** HDIUTIL RETURNED " + result)
    }

    // And remove the app
    rm(appDir)

  }

}

object InfoPlist {
  val extId = PublicID("-//Apple Computer//DTD PLIST 1.0//EN", "http://www.apple.com/DTDs/PropertyList-1.0.dtd")
  val docType = DocType("plist", extId, Nil)
}

case class InfoPlist(meta: ApplicationMeta, version:String, fwJar: File, mainClass:String, props: Map[String, String], vmOpts: Seq[String], icon: Option[File], jreDir: String) {

  def xml =
    <plist version="1.0">
      <dict>
        <key>CFBundleExecutable</key>
        <string>{MacDistHandler.launcher}</string>
          {(icon map { f =>
              <key>CFBundleIconFile</key>
              <string>{f.getName}</string>
          }).toList}
        <key>CFBundleIdentifier</key>
        <string>edu.gemini.{ meta.id }</string>
        <key>CFBundleInfoDictionaryVersion</key>
        <string>6.0</string>
        <key>CFBundleName</key>
        <string>{ meta.osxVisibleName(version) }</string>
        <key>CFBundleDisplayName</key>
        <string>{ meta.osxVisibleName(version) }</string>
        <key>CFBundlePackageType</key>
        <string>APPL</string>
        <key>CFBundleShortVersionString</key>
        <string>{ meta.shortVersion(version) }</string>
        <key>CFBundleVersion</key>
        <string>{ version }</string>
        <key>JVMRuntime</key>
        <string>{jreDir}</string>
        <key>JVMMainClassName</key>
        <string>{ mainClass }</string>
        <key>JVMOptions</key>
        <array>
          {
            vmOpts.map { x =>
              <string>{ x }</string>
            }
          }
          {
            props.map {
              case (k, v) =>
                <string>{s"-D$k=$v"}</string>
            }
          }
          <string>-Xdock:name={meta.osxVisibleName(version)}</string>
        </array>
        <key>WorkingDirectory</key>
        <string>$APP_ROOT/Contents/Resources</string>
        <key>NSHighResolutionCapable</key>
        <true/>
      </dict>
    </plist>

  def write(pw: PrintWriter) {
    XML.write(pw, xml, "UTF-8", true, InfoPlist.docType)
  }

}
