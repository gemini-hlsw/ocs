package edu.gemini.osgi.tools.app

import java.io.{ File, PrintWriter }
import edu.gemini.osgi.tools.app.{copy => recursivecopy} 

import java.util.jar.Manifest

import scala.xml.dtd.{ PublicID, DocType }
import scala.xml.XML

object MacDistHandler {
  val launcher = "JavaAppLauncher"
}

case class MacDistHandler(jre: Option[String], jreName: String) extends DistHandler {

  def build(outDir: File, jreDir: Option[File], meta: ApplicationMeta, version:String, config: Configuration, d: Configuration.Distribution, solution: Map[BundleSpec, (File, Manifest)], appProjectBaseDir: File) {

    // Output dirs
    val name = meta.osxVisibleName(version)
    val appDir = mkdir(outDir,  s"$name.app")
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
      }
    }

    import sys.process._
    val ret = Seq("chmod", "755", jas.getPath).!
    if (ret != 0) sys.error("chmod returned " + ret)

    // Copy the icon file, if it exists
    config.icon foreach { f => recursivecopy(f, resourcesDir) }

    // OSGi app layout
    buildCommon(resourcesDir, meta, version, config, d, solution, appProjectBaseDir)

    // Now create the DMG
    val volname = "%s_%s".format(meta.executableName(version), d.toString.toLowerCase)
    val dmgname = volname + ".dmg"
    val dest = new File(outDir, dmgname).getPath
    val args = Array("hdiutil", "create", "-srcfolder", appDir.getPath, "-volname", volname, dest)
    val result = Runtime.getRuntime.exec(args).waitFor()
    if (result != 0) {
      println("*** " + args.mkString(" "))
      println("*** HDIUTIL RETURNED " + result)
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
