package edu.gemini.spModel.core

import java.io._
import java.util.Arrays
import java.util.zip.{ Deflater, Inflater }

/**
 * A compressed value.
 * @param data the underlying byte array
 */
final class Deflated[A] private (val data: Array[Byte]) extends Serializable {

  /** Size of the compressed blob in bytes. */
  def size: Int =
    data.length

  /** 
   * Inflate and return the value. Note that this operation is not memoized; each call will
   * require re-inflating the compressed blob.
   */
  def inflate: A = {

    // First inflate
    val inflater = new Inflater();
    inflater.setInput(data);
    val baos = new ByteArrayOutputStream();
    val buf = new Array[Byte](Deflated.BUF_SIZE);
    while (!inflater.needsInput) {
      val len = inflater.inflate(buf);
      baos.write(buf, 0, len);
    }
    val bs = baos.toByteArray

    // Now deserialize
    val bais = new ByteArrayInputStream(bs)
    val ois  = new ObjectInputStream(bais)

    // Done
    ois.readObject.asInstanceOf[A]

  }

  override def equals(a: Any): Boolean =
    a match {
      case d: Deflated[_] => Arrays.equals(data, d.data)
      case _              => false
    }

  override def hashCode: Int =
    Arrays.hashCode(data)

}

object Deflated {
  val BUF_SIZE = 1024 * 16

  /** 
   * Construct a deflated object by serializing and compressing the passed value. This method will
   * throw if the value is not serializable.
   */
  def apply[A](a: A): Deflated[A] = {

    // First serialize
    val bs: Array[Byte] = {
      val baos = new ByteArrayOutputStream
      val oos  = new ObjectOutputStream(baos)
      oos.writeObject(a)
      oos.close
      baos.close
      baos.toByteArray
    }

    // Now deflate
    val deflater = new Deflater(Deflater.BEST_COMPRESSION)
    deflater.setInput(bs)
    deflater.finish()
    val baos = new ByteArrayOutputStream()
    val buf = new Array[Byte](BUF_SIZE)
    while (!deflater.finished) {
      val len = deflater.deflate(buf)
      baos.write(buf, 0, len)
    }
    deflater.end()           // (1) these two lines are important; see note in CompressedString.java
    System.runFinalization() // (2)
    
    // Done
    new Deflated(baos.toByteArray)
  
  }

  /**
   * Construct a deflated object from the given byte array, which is *not* copied, under the
   * assertion that its can be decompressed into a value of type A.
   */
  def unsafeFromBytes[A](data: Array[Byte]): Deflated[A] =
    new Deflated[A](data)

}