package edu.gemini.spModel.core

import java.io._
import java.util.Arrays
import java.util.zip.{ Deflater, Inflater }

import scalaz.Functor

/** A value that has been serialized and compressed. */
final class Deflated[A] private (
  private val blob: Array[Byte]
) extends Serializable {

  /** Return a *reference* to the deflated blob. */
  def unsafeToByteArray: Array[Byte] =
    blob

  /** Return a *copy* of the deflated blob. */
  def toByteArray: Array[Byte] =
    blob.clone

  /** Size of the compressed blob in bytes. */
  def size: Int =
    blob.length

  /** 
   * Inflate and return the value. Note that this operation is not memoized; each call will
   * re-inflate the compressed blob.
   */
  def inflate: A = {

    // First inflate
    val inflater = new Inflater();
    inflater.setInput(blob);
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
      case d: Deflated[_] => Arrays.equals(blob, d.blob)
      case _              => false
    }

  override def hashCode: Int =
    Arrays.hashCode(blob)

}

object Deflated {
  private val BUF_SIZE = 1024 * 16

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
    // If you don't call end() the deflater leaks system heap on Linux. This is documented in 
    // 6293787. It still leaks slowly but running finalizers seems to fix it.
    deflater.end()
    System.runFinalization()
    
    // Done
    unsafeFromByteArray(baos.toByteArray)
  
  }

  /** Construct a deflated object using a *reference* to the passed byte array. */
  def unsafeFromByteArray[A](blob: Array[Byte]): Deflated[A] =
    new Deflated(blob)

  /** Construct a deflated object using a *copy* of the passed byte array. */
  def fromByteArray[A](blob: Array[Byte]): Deflated[A] =
    unsafeFromByteArray(blob.clone)

}