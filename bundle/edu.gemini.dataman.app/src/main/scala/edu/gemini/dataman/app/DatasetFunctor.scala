package edu.gemini.dataman.app

import edu.gemini.dataman.core._
import edu.gemini.pot.sp.{ISPObservation, ISPProgram, ISPNode}
import edu.gemini.pot.spdb.{DBAbstractQueryFunctor, IDBDatabaseService}
import edu.gemini.spModel.dataset.DatasetExecRecord
import edu.gemini.spModel.obslog.ObsExecLog

import java.security.Principal

import scala.collection.breakOut
import scala.collection.JavaConverters._
import scala.collection.mutable

import scalaz._
import Scalaz._

/** A query for finding all DatasetExecRecords in the database that match a
  * given condition, and then mapping them to the desired type.
  */
final class DatasetFunctor[A](pf: PartialFunction[DatasetExecRecord, A]) extends DBAbstractQueryFunctor {
  private var matches: List[A] = Nil

  def execute(odb: IDBDatabaseService, n: ISPNode, ps: java.util.Set[Principal]): Unit =
    matches = (n match {
      case p: ISPProgram => DatasetFunctor.collect(p)(pf)
      case _             => List.empty[A]
    }) ++ matches
}

object DatasetFunctor {
  def collect[A](p: ISPProgram)(pf: PartialFunction[DatasetExecRecord, A]): List[A] =
    p.getAllObservations.asScala.flatMap { o =>
      (Option(o.getObsExecLog).map(_.getDataObject).collect {
        case log: ObsExecLog => log.getRecord.getAllDatasetExecRecords.asScala
      } | mutable.Buffer.empty).collect(pf)
    }(breakOut[mutable.Buffer[ISPObservation], A, List[A]])

  def exec[A](odb: IDBDatabaseService, user: java.util.Set[Principal])(pf: PartialFunction[DatasetExecRecord, A]): TryDman[List[A]] =
    tryOp {
      val fun = new DatasetFunctor(pf)
      odb.getQueryRunner(user).queryPrograms(fun)
      fun.matches
    }
}
