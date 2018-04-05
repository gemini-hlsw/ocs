package edu.gemini.pot.sp

import edu.gemini.spModel.rich.pot.sp._
import org.scalacheck.Gen

import scala.collection.JavaConverters._
import scala.util.Random

object DuplicateSpec extends ProgramTestSupport {

  val genTestProg: Gen[ISPFactory => ISPProgram] =
    ProgramGen.genProg

  def randomInstance[A](as: Vector[A]): Option[A] = {
    val s = as.size
    if (s == 0) None else Some(as(Random.nextInt(s)))
  }

  def randomObs(p: ISPProgram): Option[ISPObservation] =
    randomInstance(ObservationIterator.apply(p).asScala.toVector)

  def randomObsNumber(p: ISPProgram): Option[Int] =
    randomObs(p).map(_.getObservationNumber)

  def randomContainer(p: ISPProgram): Option[ISPObservationContainer] =
    randomInstance(p.toStream.collect {
      case oc: ISPObservationContainer => oc
    }.toVector)

  def expectTreeStateException(block: => Unit): Boolean =
    try {
      block
      false
    } catch {
      case _: SPTreeStateException => true
    }


  "the obs number duplication assertion" should {
    "prevent a duplicate when directly adding an observation" ! forAllPrograms { (odb, progs) =>
      progs.forall { p =>
        val setup =
          for {
            oc <- randomContainer(p)
            on <- randomObsNumber(p)
          } yield (oc, on)

        setup.forall { case (obsContainer, obsNum) =>
          val obs = odb.getFactory.createObservation(p, obsNum, Instrument.none, null)
          expectTreeStateException { obsContainer.addObservation(obs) }
        }
      }
    }

    "prevent a duplicate when adding a group" ! forAllPrograms { (odb, progs) =>
      progs.forall { p =>
        randomObsNumber(p).forall { obsNum =>
          val f = odb.getFactory
          val g = f.createGroup(p, null)
          val o = f.createObservation(p, obsNum, Instrument.none, null)
          g.addObservation(o)
          expectTreeStateException { p.addGroup(g) }
        }
      }
    }

    "prevent a duplicate when adding a template group" ! forAllPrograms { (odb, progs) =>
      progs.forall { p =>
        randomObsNumber(p).forall { obsNum =>
          Option(p.getTemplateFolder).forall { tf =>
            val f = odb.getFactory
            val g = f.createTemplateGroup(p, null)
            val o = f.createObservation(p, obsNum, Instrument.none, null)
            g.addObservation(o)
            expectTreeStateException { tf.addTemplateGroup(g) }
          }
        }
      }
    }

    "prevent a duplicate when setting obs children" ! forAllPrograms { (odb, progs) =>
      progs.forall { p =>
        val setup =
          for {
            oc <- randomContainer(p)
            on <- randomObsNumber(p)
          } yield (oc, on)

        setup.forall { case (obsContainer, obsNum) =>
          val obs = odb.getFactory.createObservation(p, obsNum, Instrument.none, null)
          expectTreeStateException {
            obsContainer.children = obs :: obsContainer.children
          }
        }
      }
    }

    "prevent a duplicate when setting group children" ! forAllPrograms { (odb, progs) =>
      progs.forall { p =>
        randomObsNumber(p).forall { obsNum =>
          val f = odb.getFactory
          val g = f.createGroup(p, null)
          val o = f.createObservation(p, obsNum, Instrument.none, null)
          g.addObservation(o)

          val gs = p.getGroups
          gs.add(g)

          expectTreeStateException { p.setGroups(gs) }
        }
      }
    }

    "prevent a duplicate when setting template group children" ! forAllPrograms { (odb, progs) =>
      progs.forall { p =>
        val setup =
          for {
            tf <- Option(p.getTemplateFolder)
            on <- randomObsNumber(p)
          } yield (tf, on)

        setup.forall { case (templateFolder, obsNum) =>
          val f = odb.getFactory
          val g = f.createTemplateGroup(p, null)
          val o = f.createObservation(p, obsNum, Instrument.none, null)
          g.addObservation(o)

          val gs = templateFolder.getTemplateGroups
          gs.add(g)

          expectTreeStateException { templateFolder.setTemplateGroups(gs) }
        }
      }
    }

    "prevent a duplicate when setting template folder" ! forAllPrograms { (odb, progs) =>
      progs.forall { p =>
        randomObsNumber(p).forall { obsNum =>
          val f   = odb.getFactory
          val tf  = f.createTemplateFolder(p, null)
          val tgs = Option(p.getTemplateFolder).map(_.getTemplateGroups).getOrElse(new java.util.ArrayList[ISPTemplateGroup]())

          val tg  = f.createTemplateGroup(p, null)
          val o   = f.createObservation(p, obsNum, Instrument.none, null)
          tg.addObservation(o)
          tgs.add(tg)

          expectTreeStateException {
            // One of the two calls should fail, depending upon which random
            // observation number we picked up.
            tf.setTemplateGroups(tgs)
            p.setTemplateFolder(tf)
          }
        }
      }
    }

  }


}
