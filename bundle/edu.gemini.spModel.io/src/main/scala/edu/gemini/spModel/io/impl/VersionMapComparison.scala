package edu.gemini.spModel.io.impl

import edu.gemini.spModel.pio.xml.PioXmlUtil
import edu.gemini.spModel.pio.{Container, Document, PioNode}
import edu.gemini.pot.sp.version._

import scala.collection.JavaConverters._
import java.io.File


/**
 * A utility for comparing version map information in two programs
 */
object VersionMapComparison extends App {

  private def versionMap(n: PioNode): Option[VersionMap] = {
    n match {
      case d: Document =>
        d.getContainers.asScala.collectFirst {
          case c: Container if "versions" == c.getKind => VersionVectorPio.toVersions(c)
        }
      case _ => None
    }
  }

  def compare(vm0: VersionMap, vm1: VersionMap): Unit = {
    (vm0.keySet ++ vm1.keySet).foreach {
      key =>
        val nv0 = nodeVersions(vm0, key)
        val nv1 = nodeVersions(vm1, key)

        if (nv0 != nv1) {
          println("key = " + key)
          println("\t" + nv0)
          println("\t" + nv1)
        }
    }
  }

  if (args.length != 2) {
    sys.error("expected: prog1.xml prog2.xml")
  }

  val f0 = new File(args(0))
  val f1 = new File(args(1))
  val vm0O = versionMap(PioXmlUtil.read(f0))
  val vm1O = versionMap(PioXmlUtil.read(f1))

  for {
    vm0 <- vm0O
    vm1 <- vm1O
  } if (vm0 == vm1) println("identical")
  else compare(vm0, vm1)
}
