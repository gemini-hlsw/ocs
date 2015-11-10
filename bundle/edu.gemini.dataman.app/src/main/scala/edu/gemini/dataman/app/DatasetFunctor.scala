package edu.gemini.dataman.app

import edu.gemini.dataman.core._
import edu.gemini.pot.sp.{ISPObservation, ISPProgram, ISPNode}
import edu.gemini.pot.spdb.{DBAbstractQueryFunctor, IDBDatabaseService}
import edu.gemini.spModel.dataset.{DatasetRecord, DatasetExecRecord}
import edu.gemini.spModel.obslog.ObsLog

import java.security.Principal

import scala.collection.breakOut
import scala.collection.JavaConverters._
import scala.collection.mutable

import scalaz._
import Scalaz._

/** A query for finding all DatasetExecRecords in the database that match a
  * given condition, and then mapping them to the desired type.
  */
final class DatasetFunctor[A](pf: PartialFunction[DatasetRecord, A]) extends DBAbstractQueryFunctor {
  private val matches = List.newBuilder[A]

  def execute(odb: IDBDatabaseService, n: ISPNode, ps: java.util.Set[Principal]): Unit = {
    def collect(p: ISPProgram)(pf: PartialFunction[DatasetRecord, A]): List[A] =
      p.getAllObservations.asScala.flatMap { o =>
        Option(ObsLog.getIfExists(o)).map(_.getAllDatasetRecords.asScala.collect(pf)) | mutable.Buffer.empty
      }(breakOut[mutable.Buffer[ISPObservation], A, List[A]])

    matches ++= (n match {
      case p: ISPProgram => collect(p)(pf)
      case _             => List.empty[A]
    })
  }
}

object DatasetFunctor {

  def collect[A](odb: IDBDatabaseService, user: java.util.Set[Principal])(pf: PartialFunction[DatasetRecord, A]): TryDman[List[A]] =
    tryOp {
      val fun = new DatasetFunctor(pf)
      odb.getQueryRunner(user).queryPrograms(fun)
      fun.matches.result()
    }

  def collectExec[A](odb: IDBDatabaseService, user: java.util.Set[Principal])(pf: PartialFunction[DatasetExecRecord, A]): TryDman[List[A]] =
    collect(odb, user) {
      case dr if pf.isDefinedAt(dr.exec) => pf(dr.exec)
    }
}
