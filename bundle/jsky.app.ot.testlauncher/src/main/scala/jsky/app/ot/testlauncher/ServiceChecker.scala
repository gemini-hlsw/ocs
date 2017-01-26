package jsky.app.ot.testlauncher

import edu.gemini.sp.vcs2.VcsService

import scalaz._
import Scalaz._
import edu.gemini.util.trpc.auth.TrpcKeyChain
import edu.gemini.util.security.auth.keychain.Action._
import java.io.File

import edu.gemini.auxfile.server.AuxFileServer
import edu.gemini.itc.shared.ItcService
import edu.gemini.pot.spdb.IDBQueryRunner
import edu.gemini.services.client.TelescopeScheduleService
import edu.gemini.spModel.core.{Peer, Site}
import edu.gemini.too.event.api.TooService
import edu.gemini.util.security.auth.keychain.KeyService
import edu.gemini.util.trpc.client.TrpcClient

object ServiceChecker extends App {

  // Store everything in /tmp; clean it up by hand as needed.
  val gs    = new Peer("localhost", 8443, Site.GS)
  val dir   = new File("/tmp/ot-servicechecker") <| (_.mkdirs)
  val peers = List(gs)
  val keys  = TrpcKeyChain(new File(dir, "keys.ser"), peers).unsafeRunAndThrow


  def check[A](f: (Int, Int) => Boolean)(implicit ev: Manifest[A]): Unit = {
    val a, b = TrpcClient(gs).withKeyChain(keys)(_[A].hashCode)
    (a tuple b) match {
      case -\/(e)                     => Console.err.println(s">> TRPC check failed for ${ev.runtimeClass.getName}"); e.printStackTrace()
      case \/-((s1, s2)) if f(s1, s2) => Console.out.println(s">> TRPC check ok     for ${ev.runtimeClass.getName}")
      case _                          => Console.err.println(s">> TRPC check failed for ${ev.runtimeClass.getName} - predicate failed")
    }
  }

  // These must be unique for each call
  check[VcsService](_ =/= _)
  check[IDBQueryRunner](_ =/= _)
  check[TooService](_ =/= _)

  // These should be the same for each call
  check[AuxFileServer](_ === _)
  check[ItcService](_ === _)
  check[KeyService](_ === _)

}
