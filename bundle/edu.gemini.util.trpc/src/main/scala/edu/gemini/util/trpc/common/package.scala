package edu.gemini.util.trpc

import scalaz._
import Scalaz._
import java.io._
import sun.misc.{BASE64Decoder, BASE64Encoder}

package object common {

  type Try[A] = Exception \/ A

  def lift[A](a: => A):Try[A] = catching(a.right[Exception])

  def catching[A](a: => Try[A]) = try {
    a
  } catch {
    case e:Exception => e.left[A]
  }

  implicit def pimpTry[A](ta:Try[A]) = new {
    def get:A = ta.fold(e => throw e, identity)
  }

  implicit def pimpOutputStream(os:OutputStream) = new {
    def writeBase64(as: Any*) {
      val bos = new ByteArrayOutputStream
      val oos = new ObjectOutputStream(bos)
      as.foreach(oos.writeObject)
      oos.close
      bos.close
      new BASE64Encoder().encode(bos.toByteArray, os)
    }
  }

  implicit def pimpInputStream(is:InputStream) = new {

    def readBase64:ObjectInputStream = {
      new ObjectInputStream(new ByteArrayInputStream(new BASE64Decoder().decodeBuffer(is))) {

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

  implicit def pimpObjectInputStream(ois:ObjectInputStream) = new {
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

