package edu.gemini.pot.spdb

import edu.gemini.pot.sp.{ISPProgram, SPNodeKeyLocks, SPNodeKey}
import edu.gemini.spModel.core.SPProgramID

object Locking {
  case class TransactionalResult[T](commit: Boolean, value: T)

  def commit[T](value: T): TransactionalResult[T] = TransactionalResult(commit = true, value)
  def discard[T](value: T): TransactionalResult[T] = TransactionalResult(commit = false, value)

  // Keeps up with how many times the current thread has entered a locking
  // read or write operation without leaving it.  We need a count instead
  // of a simple boolean because these methods (like the locks they use) are
  // reentrant.  This is useful for determining at any time whether the
  // current thread is involved in a locking read or write offered by this
  // class as opposed to just holding the read or write lock.
  private val lockCount = new ThreadLocal[Map[SPNodeKey, Int]] {
    protected override def initialValue: Map[SPNodeKey, Int] = Map.empty[SPNodeKey, Int] withDefaultValue 0
  }

  private def incLockCount(key: SPNodeKey, i: Integer): Unit = {
    val m = lockCount.get()
    val c = m(key) + i
    lockCount.set(if (c > 0) m.updated(key, c) else m - key)
  }


  /**
   * Returns <code>true</code> if the current thread is inside an operation
   * protected by this Locking class, <code>false</code> otherwise.
   */
  def insideLockingOperation(key: SPNodeKey): Boolean = lockCount.get()(key) > 0
}

import Locking._
import ProgramSummoner._

/**
 * Provides a way to perform multi-step reads and writes of science programs
 * with the program lock held.
 */
class Locking(odb: IDBDatabaseService) {
  private def readLock(key: SPNodeKey): Unit = {
      SPNodeKeyLocks.instance.readLock(key)
      incLockCount(key, 1)
  }

  private def readUnlock(key: SPNodeKey): Unit = {
      incLockCount(key, -1)
      SPNodeKeyLocks.instance.readUnlock(key)
  }

  private def writeLock(key: SPNodeKey) {
      SPNodeKeyLocks.instance.writeLock(key)
      incLockCount(key, 1)
  }

  private def writeUnlock(key: SPNodeKey) {
      incLockCount(key, -1)
      SPNodeKeyLocks.instance.writeUnlock(key)
  }

  private def lookupKey(id: SPProgramID): Either[IdNotFound, SPNodeKey] =
    Option(odb.lookupProgramByID(id)).toRight(IdNotFound(id)).right.map(_.getProgramKey)

  private def readLocking[T](key: SPNodeKey)(body: => Either[Failure, T]): Either[Failure, T] = {
    readLock(key)
    try {
      body
    } finally {
      readUnlock(key)
    }
  }

  private def writeLocking[T](key: SPNodeKey)(body: => Either[Failure, T]): Either[Failure, T] = {
    writeLock(key)
    try {
      body
    } finally {
      writeUnlock(key)
    }
  }

  /**
   * Copies the program with the given id with the write lock held. The
   * program returned will not be in the ODB and may be independently
   * modified without impacting the version in the ODB.
   * @param id program id associated with the program to copy
   * @return copy of the indicated program
   */
  def copy(id: SPProgramID): Summons =
    for {
      k <- lookupKey(id).right
      p <- copy(k, id).right
    } yield p

  /**
   * Copies the program with the given key with the write lock held.  The
   * program returned will not be in the ODB and may be independently
   * modified without impacting the version in the ODB.
   * @param key program key associated with the program to copy
   * @return copy of the indicated program
   */
  def copy(key: SPNodeKey, id: SPProgramID): Summons =
    writeLocking(key) {
      LookupOrFail.summon(odb, key, id).right.map(p => odb.getFactory.copyWithSameKeys(p))
    }


  /**
   * Perform a read operation on the program associated with the given
   * program id, which must exist in the database.  A read lock is held
   * during the execution of this method to prevent changes to the program.
   *
   * @param id id of the science program that will be read
   * @param f read operation to perform
   * @return the result of applying the matching program to <code>op</code>
   */
  def read[T](id: SPProgramID, f: ISPProgram => T): Either[Failure, T] =
    for {
      k <- lookupKey(id).right
      t <- read(k, id, f).right
    } yield t

  /**
   * Perform a read operation on the program associated with the given key,
   * which must exist in the database.  A read lock is held during the
   * execution of this method to prevent changes to the program.
   *
   * @param key key of the science program that will be read
   * @param f read operation to perform
   * @return the result of applying the matching program to <code>op</code>
   */
  def read[T](key: SPNodeKey, id: SPProgramID, f: ISPProgram => T): Either[Failure, T] =
    readLocking(key) { LookupOrFail.summon(odb, key, id).right.map(f) }

  private def put[T](p: ISPProgram, t: T): Either[IdClash, T] =
    try {
      odb.put(p)
      Right(t)
    } catch {
      case clash: DBIDClashException => Left(IdClash((p.getProgramKey, p.getProgramID), (clash.existingKey, clash.id)))
    }

  /**
   * Performs a write operation on the program in the ODB associated with the
   * given id, if there is one in the database. Unlike writeCopy this method
   * updates the program in place rather than working on a copy.  If the write
   * operation fails before it terminates, any changes it has made are kept.
   *
   * @param id the id of the program to update
   * @param f the operation to perform on the program
   *
   * @return result of the update operation
   */
  def write[T](sum: ProgramSummoner, id: SPProgramID, f: ISPProgram => T): Either[Failure, T] =
    for {
      k <-lookupKey(id).right
      t <- write(sum, k, id, f).right
    } yield t


  /**
   * Performs a write operation on the program in the ODB associated with the
   * given key (or upon a newly created empty program if not found depending
   * upon the <code>summoner</code> argument).  Unlike writeCopy, this method
   * updates the program in place rather than working on a copy.  If the write
   * operation fails before it terminates, any changes it has made are kept.
   *
   * @param sum defines the caller's expectation for whether the program
   *            identified with key should be in the ODB and what to
   *            do if it isn't
   * @param key the key of the program to update
   * @param f   the operation to perform on the program
   *
   * @return result of the update operation
   */
  def write[T](sum: ProgramSummoner, key: SPNodeKey, id: SPProgramID, f: ISPProgram => T): Either[Failure, T] =
    writeLocking(key) {
      sum.summon(odb, key, id).right.flatMap { prog =>
        Option(odb.lookupProgram(key)).fold(put(prog, f(prog))) { _ => Right(f(prog)) }
      }
    }

  /**
   * Performs a write operation on a copy of the program associated with the
   * given id, replacing the program in the ODB if successful.
   *
   * @param id the id of the program to update
   * @param f the operation to perform on a copy of the program
   *
   * @return result of the update operation
   */
  def writeCopy[T](sum: ProgramSummoner, id: SPProgramID, f: ISPProgram => TransactionalResult[T]): Either[Failure, T] =
    for {
      k <- lookupKey(id).right
      t <- writeCopy(sum, k, id, f).right
    } yield t

  /**
   * Performs a write operation on a copy of the program associated with the
   * given key, replacing the program in or adding it to the ODB if
   * successful.
   *
   * @param sum defines the caller's expectation for whether the program
   *            identified with key should be in the ODB and what to
   *            do if it isn't
   * @param key the key of the program to update
   * @param f   the operation to perform on a copy of the program
   *
   * @return result of the update operation
   */
  def writeCopy[T](sum: ProgramSummoner, key: SPNodeKey, id: SPProgramID, f: ISPProgram => TransactionalResult[T]): Either[Failure, T] =
    writeLocking(key) {
      sum.summon(odb, key, id).right.flatMap { prog =>
        val progCopy = odb.getFactory.copyWithSameKeys(prog)
        val res = f(progCopy)
        if (res.commit) put(progCopy, res.value)
        else Right(res.value)
      }
    }
}