package edu.gemini.dbTools.ephemeris

import edu.gemini.spModel.core.NonSiderealTarget
import edu.gemini.spModel.target.obsComp.TargetObsComp
import edu.gemini.spModel.util.SPTreeUtil

import scala.collection.JavaConverters._

object NonSiderealObservationTest extends TestSupport {

  "NonSiderealObservation.findScheduleable" should {
    "include every ready nonsidereal observation in an active program" ! forAllPrograms { (odb, progs) =>
      progs.forall { prog =>
        val obsList = NonSiderealObservation.findRelevantIn(prog)
        obsList.size == prog.getAllObservations.size
      }
    }

    "include no observations in an inactive program" ! forAllPrograms { (odb, progs) =>
      progs.forall { prog =>
        genInactiveProgram.sample.get.apply(odb.getFactory, prog)
        NonSiderealObservation.findRelevantIn(prog).isEmpty
      }
    }

    "find matching observation information" ! forAllPrograms { (odb, progs) =>
      progs.forall { prog =>
        val expected = NonSiderealObservation.findRelevantIn(prog).toSet
        val actual   = (Set.empty[NonSiderealObservation]/:prog.getAllObservations.asScala) { (s, o) =>
          val oid = o.getObservationID
          val tc  = SPTreeUtil.findTargetEnvNode(o)
          val toc = tc.getDataObject.asInstanceOf[TargetObsComp]
          toc.getBase.getTarget match {
            case NonSiderealTarget(n, _, Some(hid), _, _, _) =>
              s + NonSiderealObservation(oid, hid, n)
            case _                                           =>
              s
          }
        }
        expected == actual
      }
    }

    "skip sidereal targets" ! forAllPrograms { (odb, progs) =>
      progs.forall { prog =>
        genSiderealEdit.sample.get.apply(odb.getFactory, prog)
        val nsSize  = NonSiderealObservation.findRelevantIn(prog).size
        val allSize = prog.getAllObservations.size
        ((allSize == 0) && (nsSize == 0)) || (allSize - 1 == nsSize)
      }
    }

    "skip inactive observations" ! forAllPrograms { (odb, progs) =>
      progs.forall { prog =>
        genInactiveObsStatus.sample.get.apply(odb.getFactory, prog)
        val nsSize  = NonSiderealObservation.findRelevantIn(prog).size
        val allSize = prog.getAllObservations.size
        ((allSize == 0) && (nsSize == 0)) || (allSize - 1 == nsSize)
      }
    }
  }
}
