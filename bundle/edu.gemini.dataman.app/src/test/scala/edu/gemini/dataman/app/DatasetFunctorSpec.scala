package edu.gemini.dataman.app

import edu.gemini.spModel.dataset.{DatasetLabel, DatasetExecRecord, DatasetRecord}

import scalaz.{-\/, \/-}


object DatasetFunctorSpec extends TestSupport {

  "DatasetFunctor" should {
    "collect matching datasets" ! forAllPrograms { (odb, progs) =>
      val recs = allDatasets(progs)
      val pf: PartialFunction[DatasetRecord, DatasetLabel] = {
        case DatasetRecord(_, DatasetExecRecord(ds,_,Some(_))) => ds.getLabel
      }

      val expected = recs.collect(pf).toSet
      DatasetFunctor.collect(odb, User)(pf) match {
        case \/-(a) => expected == a.toSet
        case -\/(f) => println(f.explain)
                       false
      }
    }
  }

}
