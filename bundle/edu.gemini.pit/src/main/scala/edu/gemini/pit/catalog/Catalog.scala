package edu.gemini.pit.catalog

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scalaz._
import Scalaz._

object Catalog {

  // An empty catalog that always returns an error
  def empty(msg: String) = new Catalog {

    override def find(id: String)(implicit ex: ExecutionContext): Future[Result] =
      Future.successful(Error(new RuntimeException(msg)))

    override def ||(outer: Catalog): Catalog =
      outer

  }

}

trait Catalog { inner =>

  /** Given an id (a search string), do a lookup and invoke the callback at some later time. */
  def find(id: String)(implicit ex: ExecutionContext): Future[Result]

  /** Return a new catalog that returns the first result from LHS *or* RHS. */
  def ||(outer: Catalog): Catalog = new Catalog {

    def find(id: String)(implicit ex: ExecutionContext): Future[Result] = {
      val fs = List(inner, outer).map(_.find(id))

      Future.find(fs) {
        case edu.gemini.pit.catalog.Success(_, _) => true
        case _                                    => false
      }.flatMap {
        case Some(s) => Future.successful(s)
        case _       => fs.head
      }

    }
  }

}