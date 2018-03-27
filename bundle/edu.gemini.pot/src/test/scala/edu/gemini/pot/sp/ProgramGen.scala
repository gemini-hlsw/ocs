package edu.gemini.pot.sp

import edu.gemini.pot.sp.Instrument
import edu.gemini.pot.sp.SPComponentType._
import edu.gemini.pot.sp.validator.{Validator, NodeCardinality, NodeType}
import edu.gemini.spModel.core.ProgramIdGen
import edu.gemini.spModel.obs.{SPObservation, ObsPhase2Status}
import edu.gemini.spModel.rich.pot.sp._
import edu.gemini.spModel.template.{SplitFunctor, TemplateGroup, TemplateParameters}
import edu.gemini.spModel.util.VersionToken

import org.scalacheck._
import org.scalacheck.Gen._

import scala.collection.JavaConverters._
import scala.util.Random

import scalaz._
import Scalaz._

/**
 * ScalaCheck generator for science programs.
 */
object ProgramGen {
  type ProgFun[T] = (ISPFactory, ISPProgram) => T
  type ProgEdit   = ProgFun[Unit]

  val genTitle: Gen[String] = listOfN(5, Gen.alphaChar).map(_.mkString)

  private def decorativeTitle(n: ISPNode, s: String): String = s"$s (${n.key})"

  private def setObsPhase2Status(o: ISPObservation, status: ObsPhase2Status): Unit =
    o.getDataObject.asInstanceOf[SPObservation] <| (_.setPhase2Status(status)) |> (dob => o.setDataObject(dob))

  def genNode[N <: ISPNode](f: (ISPFactory, ISPProgram) => N): Gen[(ISPFactory, ISPProgram) => N] =
    genTitle.map { title => (fact, p) =>
      val n = f(fact, p)
      n.title = decorativeTitle(n, title)
      n
    }

  def genObsComp(t: SPComponentType): Gen[(ISPFactory, ISPProgram) => ISPObsComponent] =
    genNode { case (f, p) => f.createObsComponent(p, t, null) }

  val genNote:           Gen[ProgFun[ISPObsComponent]] = genObsComp(INFO_NOTE)
  val genProgNote:       Gen[ProgFun[ISPObsComponent]] = genObsComp(INFO_PROGRAMNOTE)
  val genSchedulingNote: Gen[ProgFun[ISPObsComponent]] = genObsComp(INFO_SCHEDNOTE)
  val genSomeNote:       Gen[ProgFun[ISPObsComponent]] = oneOf(genNote, genProgNote, genSchedulingNote)

  def genNotes: Gen[List[ProgFun[ISPObsComponent]]] =
    for {
      nc <- Gen.chooseNum(0, 3)
      ls <- listOfN(nc, genSomeNote)
    } yield ls

  private val validInstruments = SPComponentType.values().filter(_.broadType == SPComponentBroadType.INSTRUMENT).filterNot { ct =>
    ct == SPComponentType.QPT_CANOPUS ||
    ct == SPComponentType.QPT_PWFS
  }

  def genInstrument: Gen[ProgFun[ISPObsComponent]] =
    for {
      inst <- oneOf(validInstruments)
      foc  <- genObsComp(inst)
    } yield foc

  val genObs: Gen[ProgFun[ISPObservation]] =
    for {
      st  <- oneOf(ObsPhase2Status.values().toSeq)
      fo  <- genNode(_.createObservation(_, Instrument.none, null))
      fi  <- genInstrument
      fns <- genNotes
    } yield { (f: ISPFactory, p: ISPProgram) =>
      val o    = fo(f, p)
      setObsPhase2Status(o, st)
      val inst = fi(f, p)
      o.children = fns.sequenceU.apply(f, p) ++ List(inst) ++ o.children
      o
    }

  val genGroup: Gen[ProgFun[ISPGroup]] =
    for {
      fg  <- genNode(_.createGroup(_, null))
      fns <- genNotes
      oc  <- chooseNum(0, 3)
      fos <- listOfN(oc, genObs)
    } yield { (f: ISPFactory, p: ISPProgram) =>
      val g = fg(f, p)
      g.children = fns.sequenceU.apply(f, p) ++ fos.sequenceU.apply(f, p)
      g
    }

  val genTemplateGroup: Gen[ProgFun[ISPTemplateGroup]] =
    genNode(_.createTemplateGroup(_, null)).map { ftg => {
      (f: ISPFactory, p: ISPProgram) => ftg.apply(f, p) }
    }

  val genTemplateFolder: Gen[ProgFun[ISPTemplateFolder]] =
    for {
      ftf <- genNode(_.createTemplateFolder(_, null))
      tgc <- chooseNum(0, 3)
      ftg <- listOfN(tgc, genTemplateGroup)
    } yield { (f: ISPFactory, p: ISPProgram) =>
      val tf  = ftf(f, p)
      val tgs = ftg.sequenceU.apply(f, p).zipWithIndex.map { case (tg, i) =>
        val dob = tg.getDataObject.asInstanceOf[TemplateGroup]
        dob.setVersionToken(VersionToken.apply(Array(i+1), 1))
        tg.setDataObject(dob)
        tg
      }
      tf.children = tgs
      tf
    }

  val genProg: Gen[ISPFactory => ISPProgram] =
    for {
      id  <- ProgramIdGen.genSomeId
      ns  <- genNotes
      tfc <- chooseNum(0, 1)
      tf  <- listOfN(tfc, genTemplateFolder)
      oc  <- chooseNum(0, 3)
      os  <- listOfN(oc, genObs)
      gc  <- chooseNum(0, 5)
      gs  <- listOfN(gc, genGroup)
    } yield { (fact: ISPFactory) =>
      val p = fact.createProgram(null, id)
      p.children = ns.sequenceU.apply(fact, p) ++ tf.sequenceU.apply(fact, p) ++
                   os.sequenceU.apply(fact, p) ++ gs.sequenceU.apply(fact, p)
      p
    }

  def pickOne[T](f: ISPNode => NonEmptyList[T]): Gen[ISPNode => T] =
    choose(0, Int.MaxValue).map { i => n => {
      val v = f(n).list.toVector
      v(i % v.length)
    }}


  def maybePickOne[T](f: ISPNode => Seq[T]): Gen[ISPNode => Option[T]] =
    choose(0, Int.MaxValue).map { i => n => {
      val v = f(n).toVector
      (v.length > 0) option v(i % v.length)
    }}

  val pickNode: Gen[ISPProgram => ISPNode] = pickOne(_.nel)

  val maybePickObservation: Gen[ISPProgram => Option[ISPObservation]] =
    maybePickOne(n => new ObservationIterator(n.getProgram).asScala.toList)

  val maybePickTemplateGroup: Gen[ISPProgram => Option[ISPTemplateGroup]] =
    maybePickOne(n => Option(n.getProgram.getTemplateFolder).toList.flatMap { tf =>
      tf.getTemplateGroups.asScala.toList
    })

  def collectOne[T](pf: PartialFunction[ISPNode, T]): Gen[ISPProgram => Option[T]] =
    maybePickOne(_.toStream.collect(pf))

  val pickContainer: Gen[ISPProgram => ISPContainerNode] =
    collectOne { case c: ISPContainerNode => c }.map { f => p =>
      // cheating a bit here to keep it simple.  program is a container so we
      // "know" that the program has at least one container and the option will
      // be defined
      f(p).get
    }

  val genEditDataObject: Gen[ProgEdit] =
    for {
      title <- genTitle
      fn    <- pickNode
    } yield { (_: ISPFactory, p: ISPProgram) =>
      val n = fn(p)
      n.title = decorativeTitle(n, title)
    }

  val genEditSplitTemplateGroup: Gen[ProgEdit] =
    for {
      tgf <- maybePickTemplateGroup
    } yield { (f: ISPFactory, p: ISPProgram) =>
      tgf(p).foreach { tg =>
        val sf    = new SplitFunctor(tg)
        val newTg = sf.split(f)
        tg.getParent.asInstanceOf[ISPTemplateFolder].addTemplateGroup(newTg)
      }
    }

  val genEditObsPhase2Status: Gen[ProgEdit] =
    for {
      st <- oneOf(ObsPhase2Status.values().toSeq)
      fn <- maybePickObservation
    } yield { (_: ISPFactory, p: ISPProgram) =>
      val obs = fn(p)
      obs.foreach { setObsPhase2Status(_, st) }
    }

  val genEditReorderChildren: Gen[ProgEdit] =
    pickContainer.map { fc => (_: ISPFactory, p: ISPProgram) => {
      val container = fc(p)
      container.children = Random.shuffle(container.children)
    }}

  private val validTypes = SPComponentType.values().filterNot { ct =>
    (  ct == SPComponentType.CONFLICT_FOLDER // conflict folders are problematic
    || ct == SPComponentType.OBSERVER_FLAT   // seems to be an abandoned type now (see OBSERVER_GEMFLAT)?
    )
  }

  private def validChildTypes(n: ISPNode): Seq[SPComponentType] =
    validTypes.filter { ct =>
      NodeType.forComponentType(ct).exists { childNt =>
        NodeType.forNode(n).cardinalityOf(childNt) =/= NodeCardinality.Zero
      }
    }

  val genEditAddChild: Gen[ProgEdit] =
    for {
      title      <- genTitle
      fParent    <- pickContainer
      fChildType <- maybePickOne(validChildTypes)
    } yield { (f: ISPFactory, p: ISPProgram) =>
      val parent    = fParent(p)
      val childType = fChildType(parent)

      childType.foreach { ct =>
        NodeFactory.mkNode(f, p, ct, None).foreach { child =>
          // Intercept TemplateParameters since they have to be initialized
          if (ct == SPComponentType.TEMPLATE_PARAMETERS) {
            child.dataObject = TemplateParameters.newEmpty()
          }

          if (Validator.canAdd(p, Array(child), parent, None)) {
            child.title = decorativeTitle(child, title)
            parent.children = child :: parent.children
          }
        }
      }
    }

  val genEditDeleteChild: Gen[ProgEdit] =
    for {
      fParent <- pickContainer
      fChild  <- maybePickOne(_.children)
    } yield { (_: ISPFactory, p: ISPProgram) =>
      val parent = fParent(p)
      val child  = fChild(parent)
      child.foreach { c => parent.children = parent.children.filterNot(_ == c) }
    }

  val genEditMoveChild: Gen[ProgEdit] = {
    def isCurrentParent(parent: ISPNode, child: ISPNode): Boolean =
      parent == child.getParent

    def isDescendant(n: ISPNode, child: ISPNode): Boolean =
      child.toStream.contains(n)

    def potentialParents(child: ISPNode): Seq[ISPNode] = {
      val childType = child.getDataObject.getType
      child.getProgram.toStream.filter { n =>
        (  validChildTypes(n).contains(childType)
        && !isCurrentParent(n, child)   // don't move to the same node
        && !isDescendant(n, child)      // don't create a loop
        )
      }
    }

    for {
      fCurParent <- pickContainer
      fChild     <- maybePickOne(_.children)
      fNewParent <- maybePickOne(potentialParents)
    } yield { (_: ISPFactory, p: ISPProgram) =>
      val parent = fCurParent(p)
      val child  = fChild(parent)
      child.foreach { c =>
        fNewParent(c).foreach { newParent =>
          if (Validator.canAdd(p, Array(c), newParent, None)) {
            parent.children = parent.children.filterNot(_ == c)
            newParent.children = c :: newParent.children
          }
        }
      }
    }
  }

  val genEdit: Gen[ProgEdit] =
    oneOf(
      frequency(6 -> genEditDataObject, 3 -> genEditObsPhase2Status, 1 -> genEditSplitTemplateGroup),
      genEditReorderChildren,
      genEditAddChild,
      genEditDeleteChild,
      genEditMoveChild)

  val genEdits: Gen[List[ProgEdit]] =
    for {
      ec  <- chooseNum(0, 10)
      eds <- listOfN(ec, genEdit)
    } yield eds

  val genEditedProg: Gen[ProgFun[ISPProgram]] = {
    // This copy is a bit different from ISPFactory.copyWithSameKeys in that it
    // creates a new LifespanId for the program copy.
    def copyFrom(fact: ISPFactory, that: ISPProgram): ISPProgram = {
      val sp = fact.createProgram(that.getNodeKey, that.getProgramID)

      def init(src: ISPNode, dest: ISPNode): Unit = {
        dest.dataObject = src.dataObject
        dest.children   = src.children.map(copy)
      }

      def copy(src: ISPNode): ISPNode = {
        val newNode = NodeFactory.mkNode(fact, sp, src)
        init(src, newNode)
        newNode
      }

      init(that, sp)
      sp.setVersions(that.getVersions)
      sp
    }


    genEdits.map { edits => (f: ISPFactory, p: ISPProgram) => {
      val p2 = copyFrom(f, p)
      edits.sequenceU.apply(f, p2)
      p2
    }}
  }
}
