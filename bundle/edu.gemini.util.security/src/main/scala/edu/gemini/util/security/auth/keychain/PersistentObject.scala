package edu.gemini.util.security.auth.keychain

import edu.gemini.spModel.core.Peer
import edu.gemini.spModel.core.Site
import java.io._
import java.security.Principal
import scalaz._
import Scalaz._
import scalaz.effect.IO
import scalaz.effect.IO.ioUnit
import Close.close

/** 
 * A mutable cell that serializes its contents to disk. Callers must trap exceptions.
 */
class PersistentObject[A] private (val file: File, initial: => A) {

  def get: IO[A] =
    cache >>= (_.fold((IO(new FileInputStream(file)) >>= close(readIs)) >>= putCache)(IO(_)))

  def put(a: A): IO[Unit] =
    (IO(new FileOutputStream(file)) >>= close(putOs(a))) >> putCache(a) >> ioUnit

  def modify(f: A => A): IO[Unit] =
    get.map(f) >>= put

  ////// HELPERS

  private var myCache: Option[A] = None

  private def cache: IO[Option[A]] =
    IO(myCache)

  private def putCache(a: A): IO[A] =
    IO(myCache = Some(a)) >> IO(a)

  private def putOos(a: A)(o: ObjectOutputStream): IO[Unit] =
    IO(o.writeObject(a))

  private def putOs(a: A)(s: OutputStream): IO[Unit] =
    IO(new ObjectOutputStream(s)) >>= close(putOos(a))

  private def readIs(s: InputStream): IO[A] =
    IO(new ObjectInputStream(s)) >>= close(readOis)

  private def readOis(o: ObjectInputStream): IO[A] =
    IO(o.readObject.asInstanceOf[A])

}

object PersistentObject {

  def apply[A](f: File, a: => A): IO[PersistentObject[A]] =
    for {
      o <- IO(new PersistentObject(f, a))
      _ <- !f.isFile whenM o.put(a)
    } yield o

}

