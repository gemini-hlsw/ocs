package edu.gemini.pot.sp

/**
 * Created with IntelliJ IDEA.
 * User: rnorris
 * Date: 3/21/13
 * Time: 10:27 AM
 * To change this template use File | Settings | File Templates.
 */
package object validator {

  def time[A](msg:String)(a: => A):A = {
    val s = System.currentTimeMillis
    try a finally println(msg + ": " + (System.currentTimeMillis - s))
  }

}
