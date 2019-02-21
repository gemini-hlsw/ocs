package edu.gemini.spModel.io

import edu.gemini.pot.sp._
import edu.gemini.pot.sp.version._
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.pot.util.POTUtil
import edu.gemini.spModel.io.impl.PioSpXmlParser

import scala.collection.JavaConverters._
import scala.util.Try

object SpImportService {
  // Option for what to do if the program being imported already exists.
  sealed trait ImportDirective

  case object Copy extends ImportDirective
  case object Replace extends ImportDirective
  case object Skip extends ImportDirective

//  type Ask[T] = (T, T) => ImportDirective
  trait DuplicateQuery[T <: ISPRootNode] {
    def ask(importNode: T, existingNode: T): ImportDirective
  }

  def alwaysAnswer[T <: ISPRootNode](d: ImportDirective): DuplicateQuery[T] = new DuplicateQuery[T] {
    def ask(im: T, ex: T): ImportDirective = d
  }

  private trait ImportOps[N <: ISPRootNode] {
    def copy(db: IDBDatabaseService, im: N): N
    def put(db: IDBDatabaseService, p: N): Unit
  }

  private val progOps = new ImportOps[ISPProgram] {
    def copy(db: IDBDatabaseService, im: ISPProgram): ISPProgram = {
      def inc(vm: VersionMap, n: ISPNode): VersionMap = {
        val key = n.getNodeKey
        val vm1 = vm.updated(key, nodeVersions(vm, key).incr(n.getLifespanId))
        n match {
          case c: ISPContainerNode => (vm1/:c.getChildren.asScala) { (vm2, child) =>
            inc(vm2, child)
          }
          case _ => vm1
        }
      }

      val pid = Option(im.getProgramID).map(POTUtil.getUnusedProgramId(_, db)).orNull
      val cp  = db.getFactory.copyWithNewKeys(im, pid)
      cp.setVersions(inc(EmptyVersionMap, cp))
      cp
    }

    def put(db: IDBDatabaseService, p: ISPProgram): Unit = db.put(p)
  }

  private val planOps = new ImportOps[ISPNightlyRecord] {
    def copy(db: IDBDatabaseService, im: ISPNightlyRecord): ISPNightlyRecord = {
      val pid = Option(im.getProgramID).map(POTUtil.getUnusedProgramId(_, db)).orNull
      val cp  = db.getFactory.createNightlyRecord(null, pid)
      cp.setDataObject(im.getDataObject)
      cp
    }

    def put(db: IDBDatabaseService, p: ISPNightlyRecord): Unit = db.put(p)
  }

  private val rootOps = new ImportOps[ISPRootNode] {
    def copy(db: IDBDatabaseService, im: ISPRootNode): ISPRootNode =
      im match {
        case p: ISPProgram     => progOps.copy(db, p)
        case p: ISPNightlyRecord => planOps.copy(db, p)
      }

    def put(db: IDBDatabaseService, r: ISPRootNode): Unit =
      r match {
        case p: ISPProgram     => db.put(p)
        case p: ISPNightlyRecord => db.put(p)
      }
  }
}

import edu.gemini.spModel.io.SpImportService._

class SpImportService(db: IDBDatabaseService) {
  private val parser = new PioSpXmlParser(db.getFactory)

  def importProgramXml(rdr: java.io.Reader, query: DuplicateQuery[ISPProgram] = alwaysAnswer(Skip)): Try[ISPProgram] =
    importXml(rdr, query, progOps)

  def importPlanXml(rdr: java.io.Reader, query: DuplicateQuery[ISPNightlyRecord] = alwaysAnswer(Skip)): Try[ISPNightlyRecord] =
    importXml(rdr, query, planOps)

  def importRootNodeXml(rdr: java.io.Reader, query: DuplicateQuery[ISPRootNode] = alwaysAnswer(Skip)): Try[ISPRootNode] =
    importXml(rdr, query, rootOps)

  private def importXml[N <: ISPRootNode : Manifest](rdr: java.io.Reader, query: DuplicateQuery[N], ops: ImportOps[N]): Try[N] = {
    val clazz = implicitly[Manifest[N]].runtimeClass

    def lookup(k: SPNodeKey): Option[ISPRootNode] =
      Option(db.lookupProgram(k)) orElse Option(db.lookupNightlyPlan(k))

    def compatible(im: N, exOpt: Option[ISPRootNode]): Boolean = {
      def matches(ex: ISPRootNode): Boolean = {
        def same[T](f: ISPRootNode => T) = f(im) == f(ex)
        clazz.isInstance(ex) && same(_.getProgramID) && same(_.getNodeKey) && same(_.getDataObject.getType)
      }
      exOpt.forall(matches)
    }

    // imported program: Try[N]
    val tryIm = Try(parser.parseDocument(rdr)).filter(clazz.isInstance).map(_.asInstanceOf[N])

    // existing program: Try[Option[N]]
    val tryEx = tryIm.map(im => (im, lookup(im.getNodeKey))).filter {
      case (im, optEx) => compatible(im, optEx)
    }.map {
      case (_, optEx)  => optEx.map(_.asInstanceOf[N])
    }

    val result = for {
      im    <- tryIm
      exOpt <- tryEx
    } yield exOpt.fold(im) { ex =>
      query.ask(im, ex) match {
        case Copy    => ops.copy(db, im)
        case Replace => im
        case Skip    => ex
      }
    }

    result.foreach { ops.put(db, _) }
    result
  }
}
