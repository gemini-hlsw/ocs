package edu.gemini.spModel.dataset

import scalaz._

object Implicits {

  implicit val EqualDatasetLabel: Equal[DatasetLabel]     = Equal.equalA
  implicit val EqualDatasetQaState: Equal[DatasetQaState] = Equal.equalA

}
