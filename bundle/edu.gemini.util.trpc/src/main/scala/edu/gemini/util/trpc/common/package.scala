package edu.gemini.util.trpc

import scalaz._
import Scalaz._
import java.io._

package object common {

  val BufSize = 1024 * 16 // ok?

  type Try[A] = Exception \/ A

  def lift[A](a: => A):Try[A] = catching(a.right[Exception])

  def catching[A](a: => Try[A]) = try {
    a
  } catch {
    case e:Exception => e.left[A]
  }

  implicit class TryOps[A](ta:Try[A]) {
    def get:A = ta.fold(e => throw e, identity)
  }

  implicit class OutputStreamOps(os:OutputStream) {
    def writeRaw(as: Any*): Unit =
      closing(new BufferedOutputStream(os, BufSize)) { os =>
        closing(new ObjectOutputStream(os)) {
          oos => as.foreach(oos.writeObject)
        }
      }
  }

  implicit class InputStreamOps(is:InputStream) {

    def readRaw:ObjectInputStream = {
      new ObjectInputStream(new BufferedInputStream(is, BufSize)) {

        // Override to fall back on the current classloader, since ObjectInputStream pulls
        // one out of the ether and it's not really possible to know what to expect.
        override def resolveClass(desc:ObjectStreamClass):Class[_] =
          try {
            super.resolveClass(desc);
          } catch {
            case cnfe: ClassNotFoundException =>
              Class.forName(desc.getName, false, getClass.getClassLoader)
          }
      }
    }

  }

  implicit class ObjectInputStreamOps(ois:ObjectInputStream) {
    def next[A]:A = {
      val obj = ois.readObject()
      obj.asInstanceOf[A]
    }
  }

  def closing[A <: { def close():Unit }, B](a:A)(f: A => B):B = try {
    f(a)
  } finally {
    a.close()
  }

}

