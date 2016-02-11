package edu.gemini.seqexec.web.common

import scalaz._
import Scalaz._

case class Comment(author: String, comment: String)

object Comment {
  implicit val equal = Equal.equalA[Comment]
}

