package edu.gemini.osgi.tools.app

import java.io.File
import java.util.jar.Manifest

sealed trait Framework {
  val bundleSpec:BundleSpec
  val BundleDirName:String
  val ConfigDirName:String
  val ConfigFileName:String

  def bundleProps(bs:Seq[BundleSpec], resolver: BundleSpec => (File, Manifest), startLevel: BundleSpec => Int):Map[String,String]

}

object Framework {

  def forBundle(b:BundleSpec):Framework = b match {
    // case Equinox.bundleSpec => Equinox
    case Felix.bundleSpec => Felix
    case Gemini.bundleSpec => Gemini
    case _ => sys.error(b + ": not a framework bundle")
  }

}

// case object Equinox extends Framework {
//   val bundleSpec = BundleSpec("org.eclipse.osgi", Version(3,5,1))
//   val BundleDirName = "plugins"
//   val ConfigDirName = "configuration"
//   val ConfigFileName = "config.ini"

//   def bundleProps(bs:Seq[BundleSpec], resolver: BundleSpec => (File, Manifest)) = {
//     val bundleExpression = {
//       def expr(b: BundleSpec): String = {
//         val (f, mf) = resolver(b)
//         val s = "plugins/%s@%d".format(f.getName, b.startLevel)
//         if (isFragment(mf)) s else s + ":start"
//       }
//       bs.map(expr).mkString(",")
//     }
//     Map("osgi.bundles" -> bundleExpression)
//   }

// }

trait FelixLike extends Framework {
  val BundleDirName = "bundle"
  val ConfigDirName = "conf"
  val ConfigFileName = "config.properties"

  def bundleProps(bs:Seq[BundleSpec], resolver: BundleSpec => (File, Manifest), startLevel: BundleSpec => Int) = {

    def blist(includeFragments: Boolean) = bs
      .filter(startLevel(_) > 0)
      .filter(b => includeFragments || !isFragment(resolver(b)._2))
      .groupBy(startLevel)
      .mapValues(_.map("file:bundle/" + resolver(_)._1.getName).mkString(" "))

    val install = for ((n, e) <- blist(includeFragments = true)) yield ("felix.auto.install." + n, e)
    val start = for ((n, e) <- blist(includeFragments = false)) yield ("felix.auto.start." + n, e)

    install ++ start

  }

}

case object Felix extends FelixLike {
  val bundleSpec = BundleSpec("org.apache.felix.main", Version(4,2,1))
}

case object Gemini extends FelixLike {
  val bundleSpec = BundleSpec("edu.gemini.osgi.main", Version(4,2,1))
}


