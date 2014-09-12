package edu.gemini.osgi.tools.idea

import java.io.File
import edu.gemini.osgi.tools.FileUtils._

object IdeaProject {
  def containsAttribute(n: xml.Node, a: String, v: String): Boolean =
    n.attribute(a).exists(_.text == v)

  def fixedComponents: xml.NodeBuffer =
    <component name="CompilerConfiguration">
      <option name="DEFAULT_COMPILER" value="Javac" />
      <resourceExtensions />
      <wildcardResourcePatterns>
        <entry name="?*.properties" />
        <entry name="?*.xml" />
        <entry name="?*.gif" />
        <entry name="?*.png" />
        <entry name="?*.jpeg" />
        <entry name="?*.jpg" />
        <entry name="?*.html" />
        <entry name="?*.css" />
        <entry name="?*.xsl" />
        <entry name="?*.dtd" />
        <entry name="?*.tld" />
        <entry name="?*.txt" />
        <entry name="?*.ftl" />
        <entry name="?*.xsd" />
        <entry name="?*.cfg" />
        <entry name="?*.conf" />
        <entry name="?*.csv" />
        <entry name="?*.aiff" />
        <entry name="?*.jks" />
        <entry name="?*.vm" />
        <entry name="?*.fits" />
      </wildcardResourcePatterns>
      <annotationProcessing enabled="false" useClasspath="true" />
    </component>
    <component name="DependencyValidationManager">
      <option name="SKIP_IMPORT_STATEMENTS" value="false" />
    </component>
    <component name="Encoding" useUTFGuessing="true" native2AsciiForPropertiesFiles="false" />
    <component name="EntryPointsManager">
      <entry_points version="2.0" />
    </component>
    <component name="InspectionProjectProfileManager">
      <profiles>
        <profile version="1.0" is_locked="false">
          <option name="myName" value="Project Default" />
          <option name="myLocal" value="false" />
          <inspection_tool class="CloneDeclaresCloneNotSupported" enabled="false" level="WARNING" enabled_by_default="false" />
          <inspection_tool class="JavaDoc" enabled="false" level="WARNING" enabled_by_default="false">
            <option name="TOP_LEVEL_CLASS_OPTIONS">
              <value>
                <option name="ACCESS_JAVADOC_REQUIRED_FOR" value="none" />
                <option name="REQUIRED_TAGS" value="" />
              </value>
            </option>
            <option name="INNER_CLASS_OPTIONS">
              <value>
                <option name="ACCESS_JAVADOC_REQUIRED_FOR" value="none" />
                <option name="REQUIRED_TAGS" value="" />
              </value>
            </option>
            <option name="METHOD_OPTIONS">
              <value>
                <option name="ACCESS_JAVADOC_REQUIRED_FOR" value="none" />
                <option name="REQUIRED_TAGS" value="@return@param@throws or @exception" />
              </value>
            </option>
            <option name="FIELD_OPTIONS">
              <value>
                <option name="ACCESS_JAVADOC_REQUIRED_FOR" value="none" />
                <option name="REQUIRED_TAGS" value="" />
              </value>
            </option>
            <option name="IGNORE_DEPRECATED" value="false" />
            <option name="IGNORE_JAVADOC_PERIOD" value="true" />
            <option name="IGNORE_DUPLICATED_THROWS" value="false" />
            <option name="IGNORE_POINT_TO_ITSELF" value="false" />
            <option name="myAdditionalJavadocTags" value="" />
          </inspection_tool>
          <inspection_tool class="NonJREEmulationClassesInClientCode" level="ERROR" enabled="false" />
          <inspection_tool class="SimplifiableIfStatement" enabled="false" level="WARNING" enabled_by_default="false" />
          <inspection_tool class="StringBufferReplaceableByStringBuilder" enabled="true" level="WARNING" enabled_by_default="true" />
          <inspection_tool class="SuspiciousNameCombination" enabled="false" level="WARNING" enabled_by_default="false">
            <group names="x,width,left,right" />
            <group names="y,height,top,bottom" />
          </inspection_tool>
        </profile>
      </profiles>
      <option name="PROJECT_PROFILE" value="Project Default" />
      <option name="USE_PROJECT_PROFILE" value="true" />
      <version value="1.0" />
    </component>
    <component name="JavadocGenerationManager">
      <option name="OUTPUT_DIRECTORY" />
      <option name="OPTION_SCOPE" value="protected" />
      <option name="OPTION_HIERARCHY" value="true" />
      <option name="OPTION_NAVIGATOR" value="true" />
      <option name="OPTION_INDEX" value="true" />
      <option name="OPTION_SEPARATE_INDEX" value="true" />
      <option name="OPTION_DOCUMENT_TAG_USE" value="false" />
      <option name="OPTION_DOCUMENT_TAG_AUTHOR" value="false" />
      <option name="OPTION_DOCUMENT_TAG_VERSION" value="false" />
      <option name="OPTION_DOCUMENT_TAG_DEPRECATED" value="true" />
      <option name="OPTION_DEPRECATED_LIST" value="true" />
      <option name="OTHER_OPTIONS" value="" />
      <option name="HEAP_SIZE" />
      <option name="LOCALE" />
      <option name="OPEN_IN_BROWSER" value="true" />
    </component>
    <component name="NullableNotNullManager">
      <option name="myDefaultNullable" value="org.jetbrains.annotations.Nullable" />
      <option name="myDefaultNotNull" value="org.jetbrains.annotations.NotNull" />
      <option name="myNullables">
        <value>
          <list size="0" />
        </value>
      </option>
      <option name="myNotNulls">
        <value>
          <list size="3">
            <item index="0" class="java.lang.String" itemvalue="org.jetbrains.annotations.NotNull" />
            <item index="1" class="java.lang.String" itemvalue="javax.annotation.Nonnull" />
            <item index="2" class="java.lang.String" itemvalue="edu.umd.cs.findbugs.annotations.NonNull" />
          </list>
        </value>
      </option>
    </component>

}

import IdeaProject._
import sbt.ScalaInstance

class IdeaProject(idea: Idea, scalaInstance: ScalaInstance, imls: List[File]) {
  val name      = "%s.ipr".format(idea.app.id)
  val iprFile   = new File(idea.projDir, name)

  // def imlFile(bl: BundleLoc) = new File(bl.loc, "%s.iml".format(IdeaModule.moduleName(bl)))

  private def emptyProject(javaVersion: String): xml.Elem =
    <project version="4">
      <component name="ArtifactManager"/>
      {fixedComponents}
      <component name="ProjectModuleManager">
        <modules/>
      </component>
      <component name="ProjectResources">
        <default-html-doctype>http://www.w3.org/1999/xhtml</default-html-doctype>
      </component>
      {rootComponent(javaVersion)}
      <component name="ProjectRunConfigurationManager"/>
      <component name="ScalacSettings">
        <option name="SCALAC_BEFORE" value="false" />
      </component>
      <component name="ScalacSettings">
        <option name="COMPILER_LIBRARY_NAME" value="scala-compiler" />
        <option name="COMPILER_LIBRARY_LEVEL" value="Project" />
      </component>
      <component name="SvnBranchConfigurationManager">
        <option name="mySupportsUserInfoFilter" value="true" />
      </component>
      <component name="VcsDirectoryMappings"/>
      {libraryTable}
    </project>

  private def initialProject(javaVersion: String): xml.Elem =
    emptyProject(javaVersion: String)
    // if (iprFile.exists()) xml.XML.loadFile(iprFile) else emptyProject(javaVersion: String)

  private def updatedProject(proj: xml.Elem): xml.Elem = {
    val initMap = components(proj)
    val compMap = initMap ++ List(
      "ArtifactManager"                -> artifactComponent,
      "ProjectModuleManager"           -> moduleComponent,
      "ProjectRunConfigurationManager" -> updatedRunConfigurationComponent(initMap.get("ProjectRunConfigurationManager")) //,
      // "VcsDirectoryMappings"           -> vcsComponent 
    )
    <project version="4">{ compMap.keys.toList.sorted.map(compMap) }</project>
  }

  private def components(proj: xml.Elem): Map[String, xml.Node] =
    (proj.child map { n => (n \ "@name").text -> n }).toMap


  def project(javaVersion: String): xml.Elem = updatedProject(initialProject(javaVersion))

  /*
def project: xml.Elem =
  <project version="4">
    {artifactComponent}
    {IdeaProject.fixedComponents}
    {moduleComponent}
    <component name="ProjectResources">
      <default-html-doctype>http://www.w3.org/1999/xhtml</default-html-doctype>
    </component>
    {rootComponent}
    {runConfigurationComponent}
    <component name="ScalacSettings">
      <option name="SCALAC_BEFORE" value="false" />
    </component>
    <component name="SvnBranchConfigurationManager">
      <option name="mySupportsUserInfoFilter" value="true" />
    </component>
    {vcsComponent}
  </project>
  */

  private def projRelativePath(to: File): String =
    "$PROJECT_DIR$/%s".format(relativePath(idea.projDir, to))

  def artifactComponent: xml.Elem =
    <component name="ArtifactManager">
      {idea.app.srcBundles.map(bl => artifact(bl))}
    </component>

  private def artifact(bl: BundleLoc): xml.Elem =
    <artifact type="jar" build-on-make="true" name={artifactName(bl)}>
      <output-path>{projRelativePath(idea.distBundleDir)}</output-path>
      <root id="archive" name={"%s-%s.jar".format(bl.name, bl.version)}>
        <element id="directory" name="META-INF">
          <element id="file-copy" path={projRelativePath(new File(bl.loc, "META-INF/MANIFEST.MF"))} />
        </element>
        <element id="directory" name="lib">
          {privateLibs(bl).map(lib => <element id="file-copy" path={projRelativePath(lib)} />)}
        </element>
        <element id="module-output" name={IdeaModule.moduleName(bl)} />
      </root>
    </artifact>

  private def privateLibs(bl: BundleLoc): List[File] = {
    val libDir = new File(bl.loc, "lib")
    if (libDir.exists()) libDir.listFiles(jarFilter).toList else List.empty
  }

  // TODO: felix module, see below
  def moduleComponent: xml.Elem =
    <component name="ProjectModuleManager">
      <modules>
        {imls.map(module)}
      </modules>
    </component>

  // def moduleComponent: xml.Elem =
  //   <component name="ProjectModuleManager">
  //     <modules>
  //       {imls.map(module)}
  //       <module fileurl="file://$PROJECT_DIR$/felix.iml" filepath="$PROJECT_DIR$/felix.iml" />
  //     </modules>
  //   </component>

  private def module(iml: File): xml.Elem = {
    val imlPath = projRelativePath(iml)
    <module fileurl={"file://%s".format(imlPath)} filepath={imlPath} />
  }

  def rootComponent(javaVersion: String): xml.Elem =
    <component name="ProjectRootManager" version="2" languageLevel={"JDK_" + javaVersion.replace('.','_') } assert-keyword="true" jdk-15="true" project-jdk-name={ javaVersion } project-jdk-type="JavaSDK">
      <output url={"file://%s".format(projRelativePath(idea.distOutDir))} />
    </component>

  def vcsComponent: xml.Elem =
    <component name="VcsDirectoryMappings">
      {idea.app.srcBundles.map(vcs)}
    </component>

  private def vcs(bl: BundleLoc): xml.Elem =
    <mapping directory={projRelativePath(bl.loc)} vcs="svn" />

  def runConfigurationComponent: xml.Elem =
    <component name="ProjectRunConfigurationManager">
      {appRunConfiguration}
    </component>

  def updatedRunConfigurationComponent(existing: Option[xml.Node]): xml.Elem =
    <component name="ProjectRunConfigurationManager">
      {runConfigurations(existing)}
    </component>

  // Keep all existing run configurations (if any) except the main one, which
  // is updated.
  private def runConfigurations(existing: Option[xml.Node]): Seq[xml.Node] =
    existing map { opt =>
      appRunConfiguration +: opt.child.filterNot(containsAttribute(_, "name", idea.appName))
    } getOrElse Seq(appRunConfiguration)

  private def appRunConfiguration: xml.Elem =
    <configuration default="false" name={idea.appName} type="Application" factoryName="Application">
      <option name="MAIN_CLASS_NAME" value="org.apache.felix.main.Main" />
      <option name="VM_PARAMETERS" value={vmargs} />
      <option name="PROGRAM_PARAMETERS" value="" />
      <option name="WORKING_DIRECTORY" value="file://$PROJECT_DIR$" />
      <option name="ALTERNATIVE_JRE_PATH_ENABLED" value="false" />
      <option name="ALTERNATIVE_JRE_PATH" value="" />
      <option name="ENABLE_SWING_INSPECTOR" value="false" />
      <option name="ENV_VARIABLES" />
      <option name="PASS_PARENT_ENVS" value="true" />
      <module name="felix" />
      <envs />
      <RunnerSettings RunnerId="Run" />
      <ConfigurationWrapper RunnerId="Run" />
      <method>
        {buildArtifacts}
      </method>
    </configuration>

  private def vmargs: String =
    "%s -Dfelix.config.properties=file:felix-config.properties".format(idea.app.vmargs.mkString(" "))

  private def buildArtifacts: xml.Elem =
    <option name="BuildArtifacts" enabled="true">
      {idea.app.srcBundles.map(bl => <artifact name={artifactName(bl)} />)}
    </option>

  private def artifactName(bl: BundleLoc): String = "%s_%s".format(bl.name, bl.version)

  private def libraryTable: xml.Elem =
    <component name="libraryTable">
      <library name="scala-compiler">
        <CLASSES>
          {scalaInstance.allJars.map(jarUrl)}
        </CLASSES>
      </library>
      <library name="scala-library">
        <CLASSES>
          {scalaInstance.allJars.map(jarUrl)}
        </CLASSES>
      </library>
    </component>

  private def jarUrl(jarFile: File): xml.Elem =
    <root url={"jar://%s!/".format(projRelativePath(jarFile)) } />
}

