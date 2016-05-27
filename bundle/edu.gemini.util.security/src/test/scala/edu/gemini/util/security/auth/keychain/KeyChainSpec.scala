package edu.gemini.util.security.auth.keychain

import org.specs2.mutable.Specification
import scalaz._, Scalaz._, effect.IO
import java.io.File
import java.nio.file.{ Files, StandardCopyOption }
import edu.gemini.spModel.core.{ Peer, Site }

object KeyChainSpec extends Specification {
  import Action._

  /** Test peers. */
  val testPeers: Set[Peer] = Set(
    new Peer("foo", 123, Site.GN), 
    new Peer("bar", 456, null)
  )

  /** Construct a KeyChain with a copy of the resource at the provided path. */
  def keychain(resourcePath: String): Action[KeyChain] = {
    val path = Files.createTempFile("KeyChainSpec", ".ser")
    val file = path.toFile <| (_.deleteOnExit())
    val in   = getClass.getResourceAsStream(resourcePath)
    Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING)
    in.close()
    KeyChain(file, KeyChain.KeyFetcher.forTesting(null), testPeers.toList)
  }

  /** Test that the keychain at the given path, after preparation, has peers = testPeers. */
  def testKeyChain(path: String, prep: KeyChain => Action[Unit] = _ => Action.unit) =
    (keychain(path) >>! prep >>= (_.peers)).unsafeRunAndThrow must_== testPeers

  "corrupted keychain file" should {
    "reset" in testKeyChain("/corrupted.ser")
  }

  "corrupted kernel" should {
    "reset if unlocked" in testKeyChain("/old-unlocked.ser")
    "reset after unlocking, if locked" in testKeyChain("/old-locked.ser", _.unlock("poi"))
  }  

}