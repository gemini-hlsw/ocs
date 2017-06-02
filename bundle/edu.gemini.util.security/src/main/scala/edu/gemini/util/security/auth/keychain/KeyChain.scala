package edu.gemini.util.security.auth.keychain

import java.util.logging.{Level, Logger}

import edu.gemini.util.security.principal.GeminiPrincipal
import edu.gemini.spModel.core.Peer
import edu.gemini.spModel.core.Site
import java.io.{IOException, File}
import java.security.Principal
import javax.security.auth.Subject
import scalaz._
import Scalaz._
import scalaz.effect.IO

/** A KeyChain manages a set of peers and keys they have allocated. */
abstract class KeyChain(kCell: KeyChain.KCell, pCell: KeyChain.PCell, lCell: KeyChain.LCell, initialPeers: List[Peer]) extends KeyChain.KeyFetcher {
  import KeyChain._

  ////// LISTENERS

  /**
   * Add an action to be executed when the key or lock state changes. Returns a token that can be
   * used to unregister early.
   */
  def addListener(a: IO[Boolean]): Action[Int] =
    for {
      n <- lCell.get.map {
        case Nil => 0
        case as  => as.map(_._2).max + 1
      }
      _ <- lCell.modify((a, n) :: _)
      _ <- notify1(a)
    } yield n

  def removeListener(n: Int): Action[Unit] =
    lCell.modify(_.filter(_._2 =/= n))

  ////// PEER MANAGEMENT

  /** Action to return a set of known peers.*/
  def peers: Action[Set[Peer]] =
    keys.map(_.keySet)

  /** Action to add a peer (checking the connection first). */
  def addPeer(peer: Peer): Action[Unit] =
    for {
      _ <- checkConnection(peer)
      _ <- modify(_ + peer)
      _ <- notifyListeners
    } yield ()

  /** Action to remove a peer (and all associated keys) from the keychain. */
  def removePeer(peer: Peer): Action[Unit] =
    for {
      _ <- modify(_ - peer)
      _ <- updateSubject
      _ <- notifyListeners
    } yield ()

  /** Action to retrieve a peer by Site. */
  def peerForSite(site: Site): Action[Option[Peer]] =
    peers.map(_.find(_.site === site))

  ////// KEY MANAGEMENT

  /** Action to return a map of all peers and keys. */
  def keys: Action[Map[Peer, Set[Key]]] =
    kernel.map(_.keys)

  /** Action to retrieve a key, add it to the keychain, and return it. */
  def addKey(peer: Peer, principal: GeminiPrincipal, pass: Array[Char]): Action[Key] =
    for {
      _ <- checkLock
      k <- retrieveKey(peer, principal, pass)
      _ <- modify(_ + (peer, k))
      _ <- notifyListeners
    } yield k

  /** Action to return a list of all unique `GeminiPrincipal`s in the KeyChain. */
  def principals: Action[List[GeminiPrincipal]] =
    keys.map(_.values.flatten.toList.map(_.get._1).distinct)

  /** Action to remove a key from the keychain. */
  def removeKey(key: Key): Action[Unit] =
    for {
      _ <- modify(_ - key)
      _ <- updateSubject
    } yield ()

  ////// LOCK MANAGEMENT

  /** Action to compute the current lock state. */
  def lockState: Action[LockState] =
    for {
      p <- pCell.get
      b <- tryPass(p getOrElse Password.default)
    } yield b.fold(p.fold[LockState](LockState.Empty)(_ => LockState.Unlocked), LockState.Locked)

  def isLocked: Action[Boolean] =
    lockState.map(_ == LockState.Locked)

  def hasLock: Action[Boolean] =
    lockState.map(_ != LockState.Empty)

  /** Action to unlock the current lock (if it's locked). */
  def unlock(pass: Password): Action[Unit] =
    for {
      _ <- verifyLock(LockState.Locked)
      b <- tryPass(pass).map(!_)
      _ <- b.whenM(Action.fail(KeyFailure.BadPassword))
      _ <- pCell.put(Some(pass))
      _ <- updateSubject
    } yield ()

  /** Action to lock the current lock (if it's unlocked). */
  def lock: Action[Unit] =
    for {
      _ <- verifyLock(LockState.Unlocked)
      _ <- pCell.put(None)
      _ <- updateSubject
    } yield ()

  /** Action to remove the current lock (if it's unlocked). */
  def removeLock(pass: Password): Action[Unit] =
    for {
      _ <- verifyLock(LockState.Unlocked)
      b <- tryPass(pass).map(!_)
      _ <- b.whenM(Action.fail(KeyFailure.BadPassword))
      _ <- setPass(None)
      _ <- updateSubject
    } yield ()

  /** Action to set the current lock (if it's empty). */
  def setLock(pass: Password): Action[Unit] =
    for {
      _ <- verifyLock(LockState.Empty)
      _ <- setPass(Some(pass))
      _ <- updateSubject
    } yield ()

  /** Action to change the current lock. */
  def changeLock(oldPass: Password, newPass: Password): Action[Unit] =
    for {
      b <- isLocked
      _ <- b.whenM(unlock(oldPass))
      _ <- removeLock(oldPass)
      _ <- setLock(newPass)
      _ <- b.whenM(lock)
    } yield ()

  /** Action to reset keychain to empty. */
  def reset(p: Option[Password]): Action[Unit] =
    for {
      _ <- KeyChain.warn("Resetting keychain. Initial peers are " + initialPeers)
      _ <- pCell.put(p)
      _ <- commit(Kernel.empty(initialPeers))
      _ <- updateSubject
    } yield ()

  ////// KEY SELECTION

  def select(pk: Option[(Peer, Key)]): Action[Unit] =
    for {
      _ <- modify(_.copy(selection = pk))
      _ <- updateSubject
    } yield()

  def selection: Action[Option[(Peer, Key)]] =
    kernel.map(_.selection)

  ////// SUBJECT

  // We provide a public Subject that is updated to reflect the status of the KeyChain selection.
  // In order to get up and running we have to be sure to `updateSubject` from the factory method
  // in the companion object, otherwise the Subject will be empty initially.
  val subject = new Subject

  protected def updateSubject: Action[Unit] =
    for {
      l <- kernel.map(_.selection.map(_._2.get._1)) ||| Action(None)
      s <- Action(subject.getPrincipals <| (_.clear))
      _ <- l.traverseU(p => Action(s.add(p)))
      _ <- notifyListeners
    } yield ()

  ////// HELPERS

  private def kernel: Action[Kernel] =
    for {
      p <- pCell.get.map(_.getOrElse(Password.default))
      v <- kCell.get.map(_.get(p))
      k <- v match {
             case -\/(_: IOException) =>

               // Kernel is corrupted; reset and retry. See comment in tryPass below.
               KeyChain.warn("Discarding corrupted kernel.") *> reset(Some(p)) *> kernel

             case -\/(_) => Action.fail(KeyFailure.KeychainLocked)
             case \/-(x) => x.point[Action]
           }
    } yield k

  private def checkLock: Action[Unit] =
    for {
      s <- lockState.map(_ === LockState.Locked)
      _ <- s.whenM(Action.fail(KeyFailure.KeychainLocked))
    } yield ()

  private def verifyLock(s: LockState): Action[Unit] =
    for {
      b <- lockState.map(_ =/= s)
      _ <- b.whenM(Action.fail(KeyFailure.IllegalLockState))
    } yield ()

  private def setPass(p: Option[Password]): Action[Unit] =
    for {
      k <- kernel
      _ <- pCell.put(p) // ordering is important here
      _ <- commit(k)
    } yield ()

  // Discard the kernel and replace with a new one
  private def discardCorruptedKernel(p: Password): Action[Unit] =
    KeyChain.warn("Discarding corrupted kernel.") *> reset(Some(p)) *> setPass(Some(p))

  protected def tryPass(p: Password): Action[Boolean] =
    kCell.get.map(_.get(p)).flatMap {
      case -\/(_: IOException) =>

        // This means the password is correct but the kernel can't be deserialized, so throw it
        // away and replace it with a new kernel and set the password. To the user it will seem
        // like their keys just disappeared, which is irritating but unavoidable in this case.
        discardCorruptedKernel(p).as(true)

      case e => e.isRight.point[Action]
    }

  protected def modify(f: Kernel => Kernel): Action[Unit] =
    for {
      k <- kernel.map(f)
      _ <- commit(k)
    } yield ()

  protected def commit(k: Kernel): Action[Unit] =
    for {
      p <- pCell.get.map(_.getOrElse(Password.default))
      _ <- kCell.put(Sealed.seal(k, p))
    } yield ()

  private def notify1(l: IO[Boolean]): Action[Boolean] =
    l.catchLeft.map(_.fold(_ => false, identity)).liftIO[Action] // TODO: log failure

  private def notifyListeners: Action[Unit] =
    for {
      ls <- lCell.get
      bs <- ls.map(_._1).traverse(notify1)
      _  <- lCell.put((ls zip bs).filter(_._2).map(_._1))
    } yield ()

  /** Unsafe interface for use from Java. */
  def asJava = asJavaStub
  object asJavaStub {
    import collection.JavaConverters._
    import Action._

    def peer(s: Site): Peer =
      KeyChain.this.peerForSite(s).map(_.orNull).unsafeRunAndThrow

    def peers(): java.util.Set[Peer] =
      KeyChain.this.peers.map(_.asJava).unsafeRunAndThrow

    /** possibly null */
    def selection(): (Peer, Principal) = {
      val o: Option[(Peer, Key)] = KeyChain.this.selection.unsafeRun.fold(_ => None, identity)
      o.map(_.rightMap(_.principal)).orNull
    }

    def addListener(callback: Runnable): Int =
      KeyChain.this.addListener(IO { callback.run(); true } ).unsafeRunAndThrow

    def isLocked: Boolean =
      KeyChain.this.isLocked.unsafeRunAndThrow

  }

}

abstract class PersistentKeyChain(
  kCell: KeyChain.KCell,
  pCell: KeyChain.PCell,
  lCell: KeyChain.LCell,
  initialPeers: List[Peer],
  o: PersistentObject[Sealed[KeyChain.Kernel]]) extends KeyChain(kCell, pCell, lCell, initialPeers) {
  import KeyChain._

  override def commit(k: Kernel): Action[Unit] =
    for {
      _ <- super.commit(k)
      _ <- kCell.get >>= (s => o.put(s).liftIO[Action])
    } yield ()

}

object KeyChain {
  private val Log = Logger.getLogger(getClass.getName)

  type Listener = IO[Boolean]

  sealed trait LockState
  object LockState {
    case object Empty extends LockState
    case object Unlocked extends LockState
    case object Locked extends LockState
    implicit val equal: Equal[LockState] = Equal.equalA
  }

  case class Kernel(selection: Option[(Peer, Key)], keyMap: Map[Peer, Set[Key]]) {

    def keys: Map[Peer, Set[Key]] =
      keyMap |+| selection.map(_.rightMap(Set(_))).foldMap(Map(_))

    def +(p: Peer): Kernel =
      copy(keyMap = keyMap |+| Map(p -> Set()))

    def +(p: Peer, k: Key) = {
      // filter out existing versions of the same key, if any
      val ks: Set[Key] = keyMap.get(p).map(_.filterNot(_.principal == k.principal) + k).getOrElse(Set(k))
      copy(keyMap = keyMap + (p -> ks))
    }

    def -(p: Peer): Kernel =
      copy(selection = selection.filterNot(_._1 == p), keyMap = keyMap - p)

    def -(k: Key): Kernel = {
      val newKeys = keyMap.mapValues(_.filterNot(_ == k)).map(identity /* SI-7005 */)
      copy(selection = selection.filterNot(_._2 == k), keyMap = newKeys)
    }

    def -(p: Peer, u: GeminiPrincipal) = {
      val sel: Option[(Peer, Key)] = selection.filterNot { case (p0, k) => p0 == p && k.principal == u }
      val ks: Set[Key] = keyMap.get(p).map(_.filterNot(_.principal == u)).getOrElse(Set())
      copy(selection = sel, keyMap = keyMap + (p -> ks))
    }

    def reset: Kernel =
      Kernel(None, keyMap.mapValues(_ => Set[Key]()).map(identity /* SI-7005 */ ))

  }
  object Kernel {
    def empty(initialPeers: List[Peer]): Kernel =
      Kernel(None, initialPeers.map(p => (p, Set[Key]())).toMap)
  }

  type Password = String
  object Password {
    val default: Password = "21A233D7B7E243589C644745C00509C7"
  }

  type KCell = Cell[Sealed[Kernel]]
  object KCell {
    def newEmptyInstance(initialPeers: List[Peer]): Action[KCell] = Cell(Sealed.seal(Kernel.empty(initialPeers), Password.default))
    def newInstance(k: Sealed[Kernel]): Action[KCell] = Cell(k)
  }

  type PCell = Cell[Option[Password]]
  object PCell {
    def newInstance: Action[PCell] = Cell(None)
  }

  type LCell = Cell[List[(Listener, Int)]]
  object LCell {
    def newInstance: Action[LCell] = Cell(Nil)
  }

  trait KeyFetcher {
    def checkConnection(peer: Peer): Action[Unit]
    def retrieveKey(peer: Peer, principal: GeminiPrincipal, pass: Array[Char]): Action[Key]
    def validateKey(peer: Peer, key: Key): Action[Unit]
    def resetPasswordAndNotify(peer: Peer, u: UserPrincipal): Action[Unit]
  }

  object KeyFetcher {
    def forTesting(keyPair: java.security.KeyPair): KeyFetcher =
      new KeyFetcher {

        def retrieveKey(peer: Peer, principal: GeminiPrincipal, pass: Array[Char]): Action[Key] =
          pass.mkString match {
            case "foo" => Signed.sign((principal, 1), keyPair.getPrivate).fold(t => Action.fail(KeyFailure.InvalidSignature(t)), Action(_))
            case "bar" => Action[Key](throw new Exception("oh noes"))
            case _     => Action.fail(KeyFailure.InvalidPassword)
          }

        def validateKey(peer: Peer, key: Key): Action[Unit] =
          Action(()) // for now

        def checkConnection(peer: Peer): Action[Unit] =
          if (peer.port == 666) Action(throw new Exception("oh noes")) else Action(())

        def resetPasswordAndNotify(peer: Peer, u: UserPrincipal): Action[Unit] =
          if (u.getName == "foo") Action(sys.error("woo!"))
          else IO.putStrLn("RESET PASSWORD FOR " + u).liftIO[Action]

      }
  }

  def apply(f: File, kf: KeyFetcher, initialPeers: List[Peer]): Action[KeyChain] =
    for {
      k <- KCell.newEmptyInstance(initialPeers)
      p <- PCell.newInstance
      l <- LCell.newInstance
      o <- k.get >>= (s => PersistentObject(f, s).liftIO[Action])
      _ <- replaceKernelFromPersistentObject(k, o)

      keychain = new PersistentKeyChain(k, p, l, initialPeers, o) {

        def retrieveKey(peer: Peer, principal: GeminiPrincipal, pass: Array[Char]): Action[Key] =
          kf.retrieveKey(peer, principal, pass)

        def validateKey(peer: Peer, key: Key): Action[Unit] =
          kf.validateKey(peer, key)

        def checkConnection(peer: Peer): Action[Unit] =
          kf.checkConnection(peer)

        def resetPasswordAndNotify(peer: Peer, u: UserPrincipal): Action[Unit] =
          for {
            _ <- modify(_ - (peer, u))
            _ <- kf.resetPasswordAndNotify(peer, u)
            _ <- updateSubject
          } yield ()

      }

      _ <- keychain.updateSubject // IMPORTANT, see note at declaration of subject above
    } yield keychain

  def log(level: Level, s: String): Action[Unit] =
    IO(Log.log(level, s)).liftIO[Action]

  def warn(s: String): Action[Unit] =
    log(Level.WARNING, s)

  /**
   * Replace the kernel in the given cell with one from the PersistentObject, or do nothing (other
   * than logging) if the PersistentObject is corrupted.
   */
  def replaceKernelFromPersistentObject(k: KCell, o: PersistentObject[Sealed[Kernel]]): Action[Unit] =
    for {
      v <- k.get
      e <- o.get.catchLeft.liftIO[Action]
      _ <- e match {
             case -\/(_: IOException) => warn("Ignoring corrupted keychain: " + o.file.getAbsolutePath)
             case -\/(t)              => IO.throwIO(t).liftIO[Action]
             case \/-(s)              => k.put(s)
           }
    } yield ()

}
