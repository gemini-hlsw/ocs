package edu.gemini.spModel.core

import java.io._

trait Helpers {

  // N.B. we need to subclass the inputstream, otherwise we can't deserialize List[A] for any A
  // that we define. No idea. Just trust me here.
  def canSerialize[A](t: A): Boolean = {
    val baos = new ByteArrayOutputStream
    val oos  = new ObjectOutputStream(baos)
    oos.writeObject(t)
    oos.close
    val bais = new ByteArrayInputStream(baos.toByteArray)
    val ois = new ObjectInputStream(bais) {
      override def resolveClass(desc: java.io.ObjectStreamClass): Class[_] = {
        try { Class.forName(desc.getName, false, getClass.getClassLoader) }
        catch { case ex: ClassNotFoundException => super.resolveClass(desc) }
      }
    }
    ois.readObject == t
  }

}
