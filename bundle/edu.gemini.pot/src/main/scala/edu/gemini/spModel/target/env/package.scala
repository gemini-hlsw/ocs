package edu.gemini.spModel.target

import java.io.{IOException, ObjectInputStream}

package object env {
  def safeRead[A: Manifest](in: ObjectInputStream)(f: PartialFunction[Object, A]): A = {
    val clazz = implicitly[Manifest[A]].runtimeClass.getName
    val o = in.readObject()
    if (f.isDefinedAt(o)) f(o)
    else throw new IOException(s"Expected $clazz, found: $o")
  }
}
