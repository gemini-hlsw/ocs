package edu.gemini.spModel.dataset

import scalaz._

object Implicits {

  implicit val EqualDatasetQaState: Equal[DatasetQaState] = Equal.equalA

}
