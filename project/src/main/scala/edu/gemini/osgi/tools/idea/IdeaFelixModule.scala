// package edu.gemini.osgi.tools.idea

// import edu.gemini.osgi.tools.FileUtils.relativePath
// import java.io.File

// /**
//  * Creates the IDEA module file XML for the "felix" module, which is required
//  * for running applications in IDEA.
//  */
// class IdeaFelixModule(idea: Idea) {
//   val imlFile = new File(idea.projDir, "felix.iml")
//   val jarFile = new File(idea.pd.libRoot, felixJarName)

//   def module: xml.Elem =
//     <module type="JAVA_MODULE" version="4">
//       <component name="NewModuleRootManager" inherit-compiler-output="true">
//         <exclude-output />
//         <content url="file://$MODULE_DIR$" />
//         <orderEntry type="inheritedJdk" />
//         <orderEntry type="sourceFolder" forTests="false" />
//         <orderEntry type="module-library">
//           <library>
//             <CLASSES>
//               <root url={"jar://$MODULE_DIR$/%s!/".format(relativePath(idea.projDir, jarFile))} />
//             </CLASSES>
//             <JAVADOC />
//             <SOURCES />
//           </library>
//         </orderEntry>
//         {idea.app.srcBundles map { bnd =>
//           <orderEntry type="module" module-name={IdeaModule.moduleName(bnd)} scope="PROVIDED" />
//         }}
//       </component>
//     </module>


// }