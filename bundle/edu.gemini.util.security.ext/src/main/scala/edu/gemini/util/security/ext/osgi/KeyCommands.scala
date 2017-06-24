package edu.gemini.util.security.ext.osgi

import edu.gemini.util.security.auth.keychain.KeyServer
import edu.gemini.util.security.auth.keychain.Action._
import edu.gemini.util.security.principal._
import edu.gemini.util.security.principal.ProgramPrincipal
import edu.gemini.util.security.principal.UserPrincipal
import edu.gemini.spModel.core.{SPProgramID, Affiliate}
import edu.gemini.spModel.core.SPProgramID.toProgramID
import org.osgi.framework.{BundleContext, ServiceReference => Ref}
import edu.gemini.pot.spdb.{DBAbstractQueryFunctor, IDBDatabaseService}
import edu.gemini.pot.sp.ISPNode
import scalaz.effect.MonadIO
import java.security.Principal
import edu.gemini.util.security.auth.ProgIdHash

trait KeyCommands {
  def key(args: Array[String]): String
}

class KeyCommandsImpl(ks: KeyServer, ctx: BundleContext, user: java.util.Set[Principal], pidHash: ProgIdHash) extends KeyCommands {

  def key(args: Array[String]): String =
    interp(args.toList)

  def interp(ss: List[String]): String =
    ss match {
      case List("try", "password", kind, name, pass)    => withPrincipal(kind, name, tryKey(_, pass))
      case List("try", "version",  kind, name, version) => withPrincipal(kind, name, validateVersion(_, version))
      case List("set",  kind, name, pass)               => withPrincipal(kind, name, setPassword(_, pass))
      case List("set-notify", name, pass)               => setPasswordAndNotify(UserPrincipal(name), pass)
      case List("reset", name)                          => resetPasswordAndNotify(UserPrincipal(name))
      case List("revoke", kind, name)                   => withPrincipal(kind, name, revoke(_))
      case List("default", pid)                         => pidHash.pass(pid)
      case List("batch", "set-program-keys")            => setProgramKeys
      case List("backup", file)                         => backup(file)
      case _ => help
    }

  def withPrincipal(kind: String, name: String, f: GeminiPrincipal => String): String =
    (kind, name) match {
      case ("user", name)      => f(UserPrincipal(name))
      case ("staff", "gemini") => f(StaffPrincipal.Gemini)
      case ("staff", _)        => facilities
      case ("program", name)   => f(ProgramPrincipal(toProgramID(name)))
      case ("affiliate", name) => Option(Affiliate.fromString(name)).map(a => f(new AffiliatePrincipal(a))).getOrElse(affiliates)
      case ("visitor", name)   => f(VisitorPrincipal(toProgramID(name)))
    }

  def tryKey(p: GeminiPrincipal, pass: String): String =
    ks.tryKey(p, pass).map(k => "yep, checks out. the key is " + k).run.unsafePerformIO.toString

  def validateVersion(p: GeminiPrincipal, version: String): String =
    try {
      ks.validateVersion(p, version.toInt).map(_ => "yep, that's the current version").run.unsafePerformIO.toString
    } catch {
      case _: NumberFormatException => "version must be an integer, like 42"
    }

  def setPassword(p: GeminiPrincipal, pass: String): String =
    ks.setPassword(p, pass).map(_ => "ok, set password for " + p).run.unsafePerformIO.toString

  def setPasswordAndNotify(p: UserPrincipal, pass: String): String =
    ks.setPasswordAndNotify(p, pass).map(_ => "ok, set password for " + p + ", will try to send email").run.unsafePerformIO.toString

  def resetPasswordAndNotify(p: UserPrincipal): String =
    ks.resetPasswordAndNotify(p).map(_ => "ok, set password for " + p + ", will try to send email").run.unsafePerformIO.toString

  def revoke(p: GeminiPrincipal): String =
    ks.revokeKey(p).map(_ => "ok, revoked key for " + p).run.unsafePerformIO.toString

  def setProgramKeys: String =
    new ProgramKeySetter(ks, ctx, user, pidHash).run.unsafePerformIO

  def backup(file: String): String = {
    ks.backup(new java.io.File(file)).run.unsafePerformIO
    "Ok then."
  }

  lazy val affiliates =
    s"""
      |Valid affiliates: ${Affiliate.values.mkString(" | ")}
    """.stripMargin

  lazy val facilities =
    s"""
      |Valid facilities: gemini
    """.stripMargin

  lazy val help =
    """
      |KeyServer Help
      |--------------
      |
      |Grammar:
      |
      |  <principal-type> := user | program | staff | affiliate | visitor
      |  <key-version>    := <int>
      |  <notify-option>  := notify
      |
      |Key Commands:
      |
      |  key try password <principal-type> <name> <pass>         Tries a password.
      |  key try version <principal-type> <name> <key-version>   Tries a key version.
      |  key set <principal-type> <name> <pass>                  Sets password.
      |  key set-notify <email> <pass>                           Sets a user password and notifies via email.
      |  key reset <email>                                       Like set-notify, with a random password.
      |  key revoke <principal-type> <name>                      Removes a key from the database.
      |  key default <progid>                                    Returns the default progid key.
      |
      |Admin Tools:
      |
      |  key backup <file-path>                                  Backs up the database to the specified file.
      |
      |Batch updates:
      |
      |  key batch set-program-keys                              Sets default program keys where missing.
      |
      |Notes:
      |
      |  Backups can't be restored online, but you can do it with the h2 tool as described here:
      |    http://www.h2database.com/html/tutorial.html#upgrade_backup_restore
      |  (The h2 bundle jar contains all the tools, so you just need to find it in the app distribution.)
      |
    """.stripMargin


}

class ProgramKeySetter(ks: KeyServer, ctx: BundleContext, user: java.util.Set[Principal], pidHash: ProgIdHash) {
  import scalaz._
  import Scalaz._
  import scalaz.effect.IO

  type PKS[A] = EitherT[IO, String, A]
  object PKS {
    def apply[A](a: => A): PKS[A] = EitherT(IO(a.right))
    def fail[A](s: String): PKS[A] = EitherT.left[IO, String, A](IO(s))
  }

  implicit class KeychainActionOps[A](a: edu.gemini.util.security.auth.keychain.Action[A]) {
    def liftAction: PKS[A] =
      EitherT(a.run.map(_.leftMap(_.toString)))
  }

  def run: IO[String] =
    setKeys.run.map(_.fold(identity, identity))

  def setKeys: PKS[String] =
    for {
      is <- withDB(progids)
      ss <- is.traverseU(setKey)
    } yield ss.mkString("\n")

  def withDB[A](f: IDBDatabaseService => PKS[A]): PKS[A] =
    for {
      r <- EitherT(IO(Option(ctx.getServiceReference(classOf[IDBDatabaseService])) \/> "no db"))
      d <- PKS(ctx.getService(r))
      a <- EitherT(f(d).run.ensuring(IO(ctx.ungetService(r))))
    } yield a

  def progids(d: IDBDatabaseService): PKS[List[SPProgramID]] =
    PKS(d.getQueryRunner(user).queryPrograms(new IdFunctor).ids.sortBy(_.toString))

  def setKey(pid: SPProgramID): PKS[String] =
    for {
      p <- PKS(ProgramPrincipal(pid))
      b <- ks.getVersion(p).liftAction.map(_.isDefined)
      x <- PKS(hash(pid))
      s <- if (b) PKS(s"$pid: already defined")
           else ks.setPassword(p, hash(pid)).liftAction.map(_ => s"$pid: set new key")
    } yield s

  def hash(pid: SPProgramID): String =
    pidHash.pass(pid)

  private class IdFunctor extends DBAbstractQueryFunctor {
    var ids: List[SPProgramID] = Nil
    def execute(db: IDBDatabaseService, node:ISPNode, ps: java.util.Set[Principal]): Unit =
      ids = node.getProgramID :: ids
  }

}
