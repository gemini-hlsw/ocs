package edu.gemini.pit.catalog

import scala.actors.Actor._

object Catalog {

  // An empty catalog that always returns an error
  def empty(msg: String) = new Catalog {
    def find(id: String)(callback: Callback) {
      actor {
        callback.safe(Error(new RuntimeException(msg)))
      }
    }
    override def ||(outer: Catalog): Catalog = outer
    override def &&(outer: Catalog): Catalog = outer
  }

}

trait Catalog { inner =>

  /** Given an id (a search string), do a lookup and invoke the callback at some later time. */
  def find(id: String)(callback: Callback)

  /** Return a new catalog that returns the first result from LHS *or* RHS. */
  def ||(outer: Catalog): Catalog = new Catalog {

    def find(id: String)(callback: Callback) {
      inner.find(id) { case m => accum ! m }
      outer.find(id) { case m => accum ! m }
      lazy val accum = actor {
        react {
          case s: Success => callback.safe(s)
          case _ => react {
            case r: Result => callback.safe(r)
          }
        }
      }
    }
  }

  /** Return a new catalog that returns the merged results from both LHS *and* RHS. */
  def &&(outer: Catalog): Catalog = new Catalog {

    def find(id: String)(callback: Callback) {
      inner.find(id) { case (crA) => accum ! (('a, crA)) }
      outer.find(id) { case (crB) => accum ! (('b, crB)) }
      lazy val accum = actor {
        await {
          case ('a, a: Result) => await {
            case ('b, b: Result) =>
              callback.safe(merge(a, b))
              exit()
          }
        }
      }
    }

    def await(f: PartialFunction[Any, Unit]) {
      loop {
        react {
          case m if f.isDefinedAt(m) => f(m)
        }
      }
    }

    /**
     * Merge two results. If both sides succeed, add them together. Otherwise if the first one
     * succeeds return it, otherwise return the other; we're assuming that any failure is as good
     * as any other, so we just pick one.
     */
    def merge: ((Result, Result) => Result) = {
      case (Success(a, ac), Success(b, bc)) => Success(a ++ b, ac ++ bc)
      case (a @ Success(_, _), _)   => a
      case (_, b)                   => b
    }

  }

}