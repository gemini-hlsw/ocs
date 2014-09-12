package edu.gemini.osgi.tools.app

import java.io._

import java.util.jar.Manifest


object WinDistHandler extends DistHandler {

  def build(wd: File, jreDir: Option[File], meta: ApplicationMeta, version:String, config: Configuration, d: Configuration.Distribution, solution: Map[BundleSpec, (File, Manifest)], appProjectBaseDir: File) {

    // Vals in this class attempt to mirror those in proj/ot/appbuild.xml as closely as is reasonable

    val winstage = mkdir(wd, "staging")
    val winjre = jreDir.map(jreDir => new File(new File(jreDir, "windows"), "JRE1.6"))

    // Some helpers
    def exec(as: String*) {
      //println("> " + as.mkString(" "))
      val ret = Runtime.getRuntime.exec(as.toArray, null, wd).waitFor()
      if (ret != 0)
        sys.error("Process returned %d: %s".format(ret, as.mkString(" ")))
    }

    // Copy the icon, if it exists
    config.icon foreach { f => copy(f, winstage) }

    // OSGi app layout
    buildCommon(winstage, meta, version, config, d, solution, appProjectBaseDir)

    // JRE
    winjre match {
      case None => sys.error("No JRE dir was specified.")
      case Some(winjre) => 
        if (!winjre.isDirectory) sys.error("No JRE was found at " + winjre)
        winjre.listFiles.foreach(copy(_, mkdir(winstage, "jre")))
    }
    
    // Nsis file
    var nsis = new File(winstage, "%s.nsi".format(meta.id))
    write(nsis) { os =>

      // Add our defines at the top
      val pw = new PrintWriter(os)
      def define(k: String, v: Any) { pw.println("!define %s \"%s\"".format(k, v)) }
      define("NAME", meta.winVisibleName(version))
      define("ID", meta.id)
      define("VERSION", meta.shortVersion(version))
      
      define("DIST", d.toString.toLowerCase)
      define("EXE", "%s_%s".format(meta.executableName(version), d.toString.toLowerCase))
      config.icon foreach { f => define("ICON", f.getName) }
      define("APP_VM_ARGS", config.vmargsWithApp(meta.executableName(version)).mkString(" ", " ", ""))
      define("APP_JAR", solution(config.framework.bundleSpec)._1.getName)
      define("APP_ARGS", config.args.mkString(" ", " ", ""))
      pw.flush()

      // And stream the remainder
      stream(getClass.getResourceAsStream("template.nsis"), os)

    }

    // Run NSIS
    exec("makensis", "-V0", nsis.getPath)

    // Retrieve the .exe
    val exe = "%s_%s.exe".format(meta.executableName(version), d.toString.toLowerCase) // NB must match what gets generated in the NSI file
    exec("mv", new File(winstage, exe).getPath, wd.getPath);

    // Clean up
    rm(winstage)

  }

}

class Blah {

  <target name="-win-internal">
    <antcall target="-win-up"/>
    <!-- The remote host -->
    <property name="remote.host" value="172.16.90.152"/>
    <property name="remote.user" value="Software Group"/>
    <property name="remote.password" value="S0ftware"/>
    <!-- Set up the windows dist in a temp dir. Start clean every time. -->
    <exec executable="sh" outputproperty="temp.dir">
      <arg line="-c 'echo build-dir-`whoami`-at-`hostname -s`'"/>
    </exec>
    <property name="winstage" value="${DIST_BASE}/${win}/${temp.dir}"/>
    <property name="winjre" value="JRE1.6"/>
    <!-- Set up directories -->
    <echo/>
    <echo message="Staging windows build in ${winstage}/"/>
    <echo/>
    <delete dir="${DIST_BASE}/${win}"/>
    <mkdir dir="${winstage}"/>
    <!-- Copy the shared common install into the distribution. -->
    <copy todir="${winstage}">
      <fileset dir="${UP_BASE}/${win}"/>
    </copy>
    <!-- Copy the launcher into the distribution. -->
    <antcall target="-common-launcher">
      <param name="launcherdir" value="${winstage}/launcher"/>
    </antcall>
    <!-- copy additional windows files. -->
    <copy todir="${winstage}">
      <fileset dir="${APP_BASE}/distfiles/${win}"/>
      <fileset refid="xml-resources"/>
      <fileset file="${APP_BASE}/distfiles/release-notes.txt"/>
    </copy>
    <!-- copy jre -->
    <copy todir="${winstage}/jre">
      <fileset dir="${JRE_BASE}/${win}/${winjre}">
        <patternset refid="exclude.jre.patterns"/>
      </fileset>
    </copy>
    <!-- Get a list of all jarfiles to use as our classpath. -->
    <pathconvert pathsep=";" property="jarfiles">
      <path>
        <fileset dir="${winstage}/launcher" includes="*.jar"/>
      </path>
      <mapper type="regexp" from=".*/(launcher/.*\.jar)" to="\1"/>
    </pathconvert>
    <!-- Do some search and replace in the Info.plist so the version, etc., is up to date. -->
    <replace file="${winstage}/ot.nsis" token="@version@" value="${hlpg.project.version.public}"/>
    <replace file="${winstage}/ot.bat" token="@classpath@" value="${jarfiles}"/>
    <replace file="${winstage}/ot-north.bat" token="@classpath@" value="${jarfiles}"/>
    <replace file="${winstage}/ot-south.bat" token="@classpath@" value="${jarfiles}"/>
    <replace file="${winstage}/ot-test.bat" token="@classpath@" value="${jarfiles}"/>
    <replace file="${winstage}/ot-north-test.bat" token="@classpath@" value="${jarfiles}"/>
    <replace file="${winstage}/ot-south-test.bat" token="@classpath@" value="${jarfiles}"/>
    <!-- Tar up the build. -->
    <tar destfile="${DIST_BASE}/${win}/${temp.dir}.tar.gz" compression="gzip" basedir="${DIST_BASE}/${win}" includes="${temp.dir}/**"/>
    <delete dir="${winstage}"/>
    <!-- Stage on remote box. -->
    <echo/>
    <echo message="Staging remote install build on ${remote.host} at ~${remote.user}/${temp.dir}/"/>
    <echo/>
    <echo message="The following command may fail. This is fine."/>
    <sshexec trust="true" host="${remote.host}" username="${remote.user}" password="${remote.password}" command="rm -r ${temp.dir}*" failonerror="false"/>
    <echo message="Sending build to remote box. May take a moment..."/>
    <scp trust="true" file="${DIST_BASE}/${win}/${temp.dir}.tar.gz" todir="${remote.user}:${remote.password}@${remote.host}:"/>
    <delete file="${DIST_BASE}/${win}/${temp.dir}.tar.gz"/>
    <echo message="Exploding build on remote box... NOTE: if this fails, just try again. Sometimes it doesn't work. Blah."/>
    <sshexec trust="true" host="${remote.host}" username="${remote.user}" password="${remote.password}" command="gzip -d ${temp.dir}.tar.gz"/>
    <sshexec trust="true" host="${remote.host}" username="${remote.user}" password="${remote.password}" command="tar -xf ${temp.dir}.tar"/>
    <!-- Build on remote box -->
    <echo/>
    <echo message="Building installer remotely..."/>
    <echo/>
    <sshexec trust="true" host="${remote.host}" username="${remote.user}" password="${remote.password}" command="makensis ${temp.dir}/ot.nsis"/>
    <!-- Copy the install back over. -->
    <echo/>
    <echo message="Retrieving installer..."/>
    <echo/>
    <scp trust="true" file="${remote.user}:${remote.password}@${remote.host}:${temp.dir}/ot_${hlpg.project.version.public}_win.exe" todir="${DIST_BASE}/${win}"/>
    <!-- Clean up -->
    <echo/>
    <echo message="Cleaning up ..."/>
    <echo/>
    <delete file="${DIST_BASE}/${win}/${temp.dir}.tar"/>
    <sshexec trust="true" host="${remote.host}" username="${remote.user}" password="${remote.password}" command="rm -r ${temp.dir}*" failonerror="false"/>
  </target>

}