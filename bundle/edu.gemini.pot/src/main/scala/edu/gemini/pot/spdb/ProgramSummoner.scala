package edu.gemini.pot.spdb

import edu.gemini.pot.sp.{ISPProgram, SPNodeKey}
import edu.gemini.spModel.core.SPProgramID

object ProgramSummoner {
  sealed trait Failure

  case class IdNotFound(id: SPProgramID) extends Failure
  case class IdClash(expected: (SPNodeKey, SPProgramID), found: (SPNodeKey, SPProgramID)) extends Failure {
    assert(expected != found)
  }
  case class KeyAlreadyExists(query: (SPNodeKey, SPProgramID), found: SPProgramID) extends Failure
  case class IdAlreadyExists(query: (SPNodeKey, SPProgramID), found: SPNodeKey) extends Failure

  type Summons = Either[Failure, ISPProgram]

  private def validate(k: SPNodeKey, i: SPProgramID, p: ISPProgram): Summons =
    Either.cond((k == p.getProgramKey) && SPProgramID.same(i, p.getProgramID),
                p, IdClash((k, i), (p.getProgramKey, p.getProgramID)))

  /**
   * An option that stipulates that a program with the given key and id must
   * already exist in the database.
   */
  case object LookupOrFail extends ProgramSummoner {
    def summon(odb: IDBDatabaseService, programKey: SPNodeKey, programId: SPProgramID): Summons =
      for {
        p <- Option(odb.lookupProgramByID(programId)).toRight(IdNotFound(programId)).right
        _ <- validate(programKey, programId, p).right
      } yield p
  }

  /**
   * An option that stipulates that a program with the given key must not
   * already exist in the database.  An empty program with the provided key
   * is created.  The new program is not added to the database.
   */
  case object CreateOrFail extends ProgramSummoner {
    def summon(odb: IDBDatabaseService, programKey: SPNodeKey, programId: SPProgramID): Summons =
      for {
        _ <- Option(odb.lookupProgram(programKey)).toLeft(()).left.map(p => KeyAlreadyExists((programKey, programId), p.getProgramID)).right
        _ <- Option(odb.lookupProgramByID(programId)).toLeft(()).left.map(p => IdAlreadyExists((programKey, programId), p.getProgramKey)).right
      } yield odb.getFactory.createProgram(programKey, programId)
  }

  /**
   * An option that stipulates that a program with the given key may or may
   * not already exist in the database.  An empty program with the provided
   * key is created if a matching program is not found.  If a new program is
   * created, it is not added to the database.
   */
  case object LookupOrCreate extends ProgramSummoner {
    def summon(odb: IDBDatabaseService, programKey: SPNodeKey, programId: SPProgramID): Summons = {
      def newProgram: Summons = Right(odb.getFactory.createProgram(programKey, programId))
      Option(odb.lookupProgram(programKey)).fold(newProgram) { validate(programKey, programId, _) }
    }
  }
}

import ProgramSummoner._

/**
 * Defines options for obtaining a science program from the database given a
 * program key given that the key may or may not refer to a program in the
 * database.  Provides a method that abstracts over the differences.
 */
sealed trait ProgramSummoner {

  /**
   * Summons a program with the matching key, either looking it up in the
   * database or creating an empty one.  See the three implementations.
   *
   * @param odb observing database to consult for the program
   * @param programKey node key of the program to find or create
   *
   * @return a program (potentially empty if created) with the given programKey
   */
  def summon(odb: IDBDatabaseService, programKey: SPNodeKey, programId: SPProgramID): Summons
}