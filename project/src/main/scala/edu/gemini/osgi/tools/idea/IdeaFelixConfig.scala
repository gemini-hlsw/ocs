// package edu.gemini.osgi.tools.idea

// import java.io.File

// import IdeaFelixConfig._

// /**
//  * Support for creating the IdeaFelixConfig
//  */
// class IdeaFelixConfig(idea: Idea) {
//   val configFile = new File(idea.projDir, "felix-config.properties")

//   def formattedProps: List[String] =
//     (augmentedProps map {
//       case (key, value) => "%s=%s".format(key, value)
//     }).toList

//   private def augmentedProps: Map[String, String] =
//     (idea.app.props/:propertyDefaults(idea)) {
//       (map, propDef) => map.updated(propDef._1, map.getOrElse(propDef._1, propDef._2))
//     }

//   private def formattedBundlePath(bl: BundleLoc): String =
//     "file://%s".format((if (bl.loc.isFile) bl.loc else idea.distBundleFile(bl)).getAbsolutePath)

//   private def formattedStartBlock(startLevel: Int, bundles: Set[BundleLoc]): String =
//     bundles.map(formattedBundlePath).mkString("felix.auto.start.%d= \\\n ".format(startLevel), " \\\n ", "")

//   private def formattedStartBlocks: List[String] =
//     idea.app.startBlocks filter {
//       case (startLevel, _)       => startLevel > 0
//     } map {
//       case (startLevel, bundles) => formattedStartBlock(startLevel, bundles)
//     }

//   def formattedConfig: String =
//     (formattedProps ++ formattedStartBlocks).mkString("\n\n")
// }

// object IdeaFelixConfig {
//   def propertyDefaults(idea: Idea) = List(
//     "org.osgi.framework.storage.clean" -> "onFirstInit",
//     "org.osgi.framework.storage"       -> new File(idea.distDir, "felix-cache").getAbsolutePath
//   )
// }

