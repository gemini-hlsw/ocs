package edu.gemini.osgi.tools.app

import java.io.{ File, PrintWriter }

import java.util.jar.Manifest

import scala.xml.dtd.{ PublicID, DocType }
import scala.xml.XML

object MacDistHandler extends DistHandler {

  def build(outDir: File, jreDir:Option[File], meta: ApplicationMeta, version:String, config: Configuration, d: Configuration.Distribution, solution: Map[BundleSpec, (File, Manifest)], appProjectBaseDir: File) {

    // Output dirs
    val name = meta.osxVisibleName(version)
    val appDir = mkdir(outDir,  s"$name.app")
    val contentsDir = mkdir(appDir, "Contents")
    val macosDir = mkdir(contentsDir, "MacOS")
    val resourcesDir = mkdir(contentsDir, "Resources")

    // Info.plist and PkgInfo into contents
    val (fwJar, fwMf) = solution(config.framework.bundleSpec)
    val plist = InfoPlist(meta, version, fwJar, fwMf.getMainAttributes.getValue("Main-Class"), config.props, config.vmargsWithApp(name), config.icon)
    write(new File(contentsDir, "PkgInfo")) { _.write(Array[Byte]('A', 'P', 'P', 'L', '?', '?', '?', '?')) }
    write(new File(contentsDir, "Info.plist")) { os => closing(new PrintWriter(os))(plist.write) }

    // JavaApplicationStub into macos
    val jas = new File(macosDir, "JavaApplicationStub")
    write(jas) { os => closing(getClass.getResourceAsStream("JavaApplicationStub")) { stream(_, os) } }
//    jas.setExecutable(true)

    import sys.process._
    val ret = Seq("chmod", "755", jas.getPath).!
    if (ret != 0) sys.error("chmod returned " + ret)

    // Copy the icon file, if it exists
    config.icon foreach { f => copy(f, resourcesDir) }

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

case class InfoPlist(meta: ApplicationMeta, version:String, fwJar: File, mainClass:String, props: Map[String, String], vmOpts: Seq[String], icon: Option[File]) {

  def xml =
    <plist version="1.0">
      <dict>
        <key>CFBundleExecutable</key>
        <string>JavaApplicationStub</string>
          {(icon map { f =>
              <key>CFBundleIconFile</key>
              <string>{f.getName}</string>
          }).toList}
        <key>CFBundleIdentifier</key>
        <string>edu.gemini.{ meta.id }</string>
        <key>CFBundleInfoDictionaryVersion</key>
        <string>6.0</string>
        <key>CFBundleName</key>
        <string>{ meta.name }</string>
        <key>CFBundlePackageType</key>
        <string>APPL</string>
        <key>CFBundleShortVersionString</key>
        <string>{ meta.shortVersion(version) }</string>
        <key>CFBundleVersion</key>
        <string>{ version }</string>
        <key>Java</key>
        <dict>
          <key>ClassPath</key>
          <string>{ fwJar.getName }</string>
          <key>JVMVersion</key>
          <string>1.6*</string>
          <key>JVMArchs</key>
          <array>
            <string>i386</string>
            <string>ppc</string>
          </array>
          <key>MainClass</key>
          <string>{ mainClass }</string>
          <key>Properties</key>
          <dict>
            {
            props map {
              case (k, v) =>
                <key>{ k }</key>
                        <string>{ v }</string>
            }
            }
          </dict>
          <key>VMOptions</key>
          <string>{ vmOpts.mkString(" ") }</string>
          <key>WorkingDirectory</key>
          <string>$APP_PACKAGE/Contents/Resources</string>
        </dict>
      </dict>
      </plist>

  def write(pw: PrintWriter) {
    XML.write(pw, xml, "UTF-8", true, InfoPlist.docType)
  }

}
