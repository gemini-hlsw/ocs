package jsky.app.ot.ags

import edu.gemini.pot.client.SPDB
import edu.gemini.spModel.core.SPProgramID
import jsky.app.ot.ags.BagsState._
import java.beans.{PropertyChangeEvent, PropertyChangeListener}
import java.time.Instant
import java.util.concurrent.TimeoutException
import java.util.concurrent._
import java.util.logging.Logger
import javax.swing.SwingUtilities

import edu.gemini.ags.api.{AgsHash, AgsRegistrar, AgsStrategy}
import edu.gemini.catalog.votable.{CatalogException, GenericError}
import edu.gemini.pot.sp._
import edu.gemini.spModel.guide.GuideProbe
import edu.gemini.spModel.obs.{ObsClassService, ObservationStatus}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.obsclass.ObsClass
import edu.gemini.spModel.rich.pot.sp._
import edu.gemini.spModel.rich.shared.immutable._
import edu.gemini.spModel.target.env.{AutomaticGroup, GuideEnv, GuideEnvironment}
import jsky.app.ot.OT
import jsky.app.ot.gemini.altair.Altair_WFS_Feature
import jsky.app.ot.gemini.inst.OIWFS_Feature
import jsky.app.ot.gemini.tpe.TpePWFSFeature
import jsky.app.ot.tpe.{TpeContext, TpeManager}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import scala.swing.Swing
import scala.util.{Failure, Success}
import scalaz._
import Scalaz._
import scalaz.effect.IO
import scalaz.effect.IO.ioUnit

/** The state of AGS lookup for a single observation. */
sealed trait BagsState {
  /** Called when an observation is edited to determine what should be the new
    * state.  This is typically called in response to a listener on the
    * science program.
    */
  private[ags] def edit: StateTransition =
    ErrorTransition

  /** Called when a timer goes off to evaluate what should be done next and
    * possibly move to a new state.  Timer events transition out of pending
    * into running or out of failed into pending.  When an AGS lookup fails,
    * a few seconds are added before transitioning in order to provide an
    * opportunity for network issues, etc. to sort themselves out.
    */
  private[ags] def wakeUp: StateTransition =
    (this, ioUnit)

  /** Called with the successful results of an AGS lookup. */
  private[ags] def succeed(results: Option[AgsStrategy.Selection]): StateTransition =
    ErrorTransition

  /** Called to notify that an AGS lookup failed. */
  private[ags] def fail(why: String, delayMs: Long): StateTransition =
    ErrorTransition
}


object BagsState {
  type AgsHashVal      = Long
  type StateTransition = (BagsState, IO[Unit]) // Next BagsState, side-effects
  val ErrorTransition  = (ErrorState, ioUnit)  // Default transition for unexpected events

  def hashObs(ctx: ObsContext): AgsHashVal = {
    val when = ctx.getSchedulingBlockStart.asScalaOpt | Instant.now.toEpochMilli
    AgsHash.hash(ctx, when)
  }

  def hashObs(o: ISPObservation): Option[AgsHashVal] =
    ObsContext.create(o).asScalaOpt.map(hashObs)

  def fetchObs(k: ObsKey): Option[ISPObservation] =
    Option(SPDB.get().lookupProgram(k.prog.key)).flatMap { p =>
      p.allObservations.find(_.getNodeKey === k.key)
    }


  /** Error.  This state represents a logic error.  There are no transitions out
    * of the error state.
    */
  case object ErrorState extends BagsState

  /** Idle.  When idle, there are no pending or running AGS look-ups.  This is
    * where we hang out waiting for something to be edited in order to kick off
    * another AGS search.  We hang on to the last known AGS hash value so that
    * we can discard irrelevant edits if possible.
    */
  case class IdleState(k: ObsKey, hash: Option[AgsHashVal]) extends BagsState {
    private[ags] override val edit: StateTransition =
      (PendingState(k, hash), BagsManager.wakeUpAction(k, 0))
  }

  /** Pending.  When pending, we assume we've been edited and that a new AGS
    * search may be required.  This is a transient state since we're expecting
    * the timer to go off, wake us up and then we'll decide whether to do an
    * AGS lookup, update the AGS hash code, etc.
    */
  case class PendingState(k: ObsKey, hash: Option[AgsHashVal]) extends BagsState {
    private[ags] override val edit: StateTransition =
      (this, ioUnit)

    private[ags] override def wakeUp: StateTransition = {
      def wakeUp(obs: ISPObservation): StateTransition = {
        def notObserved: Boolean =
          ObservationStatus.computeFor(obs) != ObservationStatus.OBSERVED

        // Get the ObsContext and AgsStrategy, if possible.  If not possible,
        // we won't be able to do AGS for this observation.
        val tup = for {
          c <- ObsContext.create(obs).asScalaOpt
          s <- agsStrategy(obs, c)
        } yield (c, s)

        // Compute the new hash value of the observation, if possible.  It may
        // be possible to (re)run AGS but not necessary if the hash hasn't
        // changed.
        val newHash = tup.map { case (ctx, _) => hashObs(ctx) }

        // Here we figure out what the transition to the RunningState should be,
        // assuming we aren't already observed and that the hash differs.
        val runningTransition = for {
          (c, s) <- tup
          h      <- newHash
          if !hash.contains(h) && notObserved
        } yield (RunningState(k, c, h), BagsManager.triggerAgsAction(k, c, s)): StateTransition

        // Returns the new state to switch to, either RunningState if all is well
        // and a new AGS lookup is needed or else IdleState (possibly with an
        // updated hash value) otherwise.
        def idleAction = tup.fold(BagsManager.clearAction(k)) { _ => ioUnit }
        runningTransition | ((IdleState(k, newHash), idleAction))
      }

      fetchObs(k).fold((ErrorState: BagsState, ioUnit)) { wakeUp }
    }
  }

  /** Running.  The running state means we're doing an AGS lookup and haven't
    * received the results yet.  We also know that there have been no edits
    * since the search began since we will transition to RunningEditedState
    * if something is edited in the meantime.
    */
  case class RunningState(k: ObsKey, ctx: ObsContext, hash: AgsHashVal) extends BagsState {
    // When edited while running, we continue running but remember we were
    // edited by moving to RunningEditedState.  When the results eventually
    // come back from the AGS lookup when in RunningEditedState, we store them
    // but move to PendingState to run again.
    private[ags] override val edit: StateTransition =
      (RunningEditedState(k), ioUnit)

    // Successful AGS lookup while running (and not edited).  Apply the update
    // and move to IdleState.
    private[ags] override def succeed(results: Option[AgsStrategy.Selection]): StateTransition = {
      val h = results.fold(hash) { r =>
        if (r.posAngle === ctx.getPositionAngle) hash
        else hashObs(ctx.withPositionAngle(r.posAngle))
      }
      (IdleState(k, Some(h)), BagsManager.applyAction(k, results))
    }

    // Failed AGS lookup.  Move to FailedState for a while and ask the timer to
    // wake us up in a bit.  When the timer eventually goes off we will switch
    // to PendingState (see FailedState.wakeUp).
    private[ags] override def fail(why: String, delayMs: Long): StateTransition =
      (FailureState(k, why), BagsManager.wakeUpAction(k, delayMs))
  }

  /** RunningEdited. This state corresponds to a running AGS search for an
    * observation that was subsequently edited.  Since it has been edited, the
    * results we're expecting may no longer be valid when they arrive.
    */
  case class RunningEditedState(k: ObsKey) extends BagsState {
    // If we're edited again while running, just loop back.  Once the results of
    // the previous edit that got us into RunningState in the first place
    // finish, we'll switch to Pending and go again.
    private[ags] override val edit: StateTransition =
      (this, ioUnit)

    // Success, but now the observation has been edited so the AGS selection
    // may not be valid. We could apply the results in case the edits made to the
    // observation do not change the AGS selection, but in the interests of safety
    // and as this was causing issues for BAGS overwriting PA edits, we only allow
    // BAGS to apply to unedited observations.
    private[ags] override def succeed(results: Option[AgsStrategy.Selection]): StateTransition =
      (PendingState(k, None), BagsManager.wakeUpAction(k, 0))

    // Failed AGS but we've been edited in the meantime anyway.  Go back to
    // PendingState so we can run again.  Here we pass in no AgsHash value to
    // ensure that pending will count this as an AGS-worthy edit.
    private[ags] override def fail(why: String, delayMs: Long): StateTransition =
      (PendingState(k, None), BagsManager.wakeUpAction(k, delayMs))
  }

  /** Failure. This state is used to mark an AGS lookup failure.  It is a
    * transient state since a timer is expected to eventually go off and move
    * us back to pending to retry the search.
    */
  case class FailureState(k: ObsKey, why: String) extends BagsState {
    // If edited while in failed, we just loop back.  When the timer eventually
    // goes off we'll move to pending and redo the AGS lookup anyway.
    private[ags] override val edit: StateTransition =
      (this, ioUnit)

    // When the timer goes off we can go back to pending (with no AgsHash value
    // since we want to ensure that the AGS search runs again).
    private[ags] override val wakeUp: StateTransition =
      (PendingState(k, None), BagsManager.wakeUpAction(k, 0))
  }

  // Performs checks to rule out disabled guide groups, instruments without
  // guiding strategies (e.g. GPI), observation classes that do not have
  // guiding (e.g. daytime calibration).
  private def agsStrategy(obs: ISPObservation, ctx: ObsContext): Option[AgsStrategy] = {
    val groupEnabled = ctx.getTargets.getGuideEnvironment.guideEnv.auto match {
      case AutomaticGroup.Initial | AutomaticGroup.Active(_, _) => true
      case _                                                    => false
    }

    AgsRegistrar.currentStrategy(ctx).filter { _ =>
      groupEnabled && (ObsClassService.lookupObsClass(obs) != ObsClass.DAY_CAL)
    }
  }
}

// Wraps SPNodeKey in a new type to help avoid confusion with observation SPNodeKey.
final case class ProgKey(key: SPNodeKey, pid: Option[SPProgramID])

object ProgKey {
  def apply(o: ISPObservation): ProgKey =
    ProgKey(o.getProgram)

  def apply(p: ISPProgram): ProgKey =
    ProgKey(p.getProgramKey, Option(p.getProgramID))

  implicit val OrderProgKey: Order[ProgKey] =
    Order.orderBy(pk => (pk.key, pk.pid.map(_.toString)))
}

// Wraps SPNodeKey in a new type to help avoid confusion with program SPNodeKey.
final case class ObsKey(prog: ProgKey, key: SPNodeKey, oid: Option[SPObservationID])

object ObsKey {
  def apply(o: ISPObservation): ObsKey =
    ObsKey(ProgKey(o), o.getNodeKey, Option(o.getObservationID))

  implicit val OrderObsKey: Order[ObsKey] =
    Order.orderBy(ok => (ok.prog, ok.key, ok.oid.map(_.toString)))
}


object BagsManager {
  private val Log = Logger.getLogger(getClass.getName)

  // Worker pool running the AGS search and the timer.
  val NumWorkers = math.max(1, Runtime.getRuntime.availableProcessors - 1)
  private val worker = new ScheduledThreadPoolExecutor(NumWorkers, new ThreadFactory() {
    override def newThread(r: Runnable): Thread = {
      val t = new Thread(r, "BagsManager - Worker")
      t.setPriority(Thread.NORM_PRIORITY - 2)
      t.setDaemon(true)
      t
    }
  })
  private implicit val executionContext = ExecutionContext.fromExecutor(worker)

  // Worker pool running the blocking queries.
  private val blockingWorker = new ScheduledThreadPoolExecutor(16, new ThreadFactory() {
    override def newThread(r: Runnable): Thread = {
      val t = new Thread(r, "BagsManager - Blocking Query Worker")
      t.setPriority(Thread.NORM_PRIORITY - 2)
      t.setDaemon(true)
      t
    }
  })
  val blockingExecutionContext = ExecutionContext.fromExecutor(blockingWorker)

  // This is our mutable state.  It is only read/written by the Swing thread.
  private var stateMap  = ==>>.empty[ProgKey, ObsKey ==>> BagsState]

  // Listeners for changes to the Bags Status.
  // Management must be done on the Swing EDT.
  private var listeners: List[BagsStateListener] = Nil

  private def assertingSwingEDT[A](message: String)(block: => A): A =
    if (SwingUtilities.isEventDispatchThread) block
    else throw new IllegalStateException(message)

  def addBagsStateListener(l: BagsStateListener): Unit =
    assertingSwingEDT("BagsStatusListeners can only be added on the Swing EDT.") {
      if (!listeners.contains(l)) listeners = l :: listeners
    }

  def removeBagsStateListener(l: BagsStateListener): Unit =
    assertingSwingEDT("BagsStatusListeners can only be removed on the Swing EDT.") {
      if (listeners.contains(l)) listeners = listeners.diff(List(l))
    }

  private def fireBagsStateChanged(k: SPNodeKey, oldState: BagsState, newState: BagsState): Unit =
    listeners.foreach(_.bagsStateChanged(k, oldState, newState))

  def stateLookup(k: ObsKey): Option[BagsState] =
    assertingSwingEDT("BagsStates can only be queried on the Swing EDT.") {
      for {
        obsMap <- stateMap.lookup(k.prog)
        s      <- obsMap.lookup(k)
      } yield s
    }

  // Handle a state machine transition.  Note we switch to the Swing thread
  // here so that the calling thread continues immediately.  All updates are
  // done from the Swing thread.
  private def transition(obsKey: ObsKey, update: BagsState => StateTransition): Unit = Swing.onEDT {
    for {
      obsMap <- stateMap.lookup(obsKey.prog)
      state  <- obsMap.lookup(obsKey)
    } {
      val (newState, sideEffect) = update(state)
      val newStatemap            = stateMap.insert(obsKey.prog, obsMap.insert(obsKey, newState))

      // Some actions will produce further state transitions when executed and
      // those transitions assume the newState has been reached.  Therefore we
      // make sure to update the state map before running the associated actions.
      val action = for {
        _ <- IO { stateMap = newStatemap }
        _ <- sideEffect
        _ <- IO { fireBagsStateChanged(obsKey.key, state, newState) }
      } yield ()

      action.unsafePerformIO()
    }
  }

  private def edited(o: ObsKey): Unit =
    transition(o, _.edit)

  private def wakeUp(o: ObsKey): Unit =
    transition(o, _.wakeUp)

  private def success(o: ObsKey, results: Option[AgsStrategy.Selection]): Unit =
    transition(o, _.succeed(results))

  private def fail(o: ObsKey, why: String, delay: Long): Unit =
    transition(o, _.fail(why, delay))

  /** Add a program to our watch list and attach listeners. Enqueue modified
    * observations for consideration for a BAGS lookup.
    */
  def watch(prog: ISPProgram): Unit = Swing.onEDT {
    // Remove existing listeners, if any.
    prog.removeStructureChangeListener(StructureListener)
    prog.removeCompositeChangeListener(ChangeListener)

    val progKey = ProgKey(prog)
    val obsList = prog.getAllObservations.asScala.toList
    val obsKeys = obsList.map(ObsKey(_)).toSet

    // Get the initial existing (if any) obs key -> BagsState map, filtered to
    // remove any entries for observations not in the program.
    val obsMap0 = stateMap.lookup(progKey).getOrElse(==>>.empty[ObsKey, BagsState]).filterWithKey { (k, _) =>
      obsKeys(k)
    }

    // Scan the list of observations creating or updating the BagsState as
    // necessary:
    //
    // 1. If an observation is missing in the map, create a new Idle state with
    //    no hash value.
    // 2. Most observations previously in the map will be Idle with a given hash
    //    value.  If the hash value is the same in the updated program there is
    //    no need to redo the lookup.  If the hash value differs, set it to None
    //    so the search will happen again.
    // 3. If currently in the middle of a search, check the hash value. If it
    //    differs the eventual results will no longer apply so mark it as
    //    RunningEdited.
    val obsMap  = (obsMap0/:obsList) { (m,o) =>
      val k = ObsKey(o)
      val s = m.lookup(k).fold(IdleState(k, None): BagsState) {
        case IdleState(_, Some(hash)) => IdleState(k, hashObs(o).filter(_ === hash))
        case RunningState(_, c, h)    => if (hashObs(o).contains(h)) RunningState(k, c, h) else RunningEditedState(k)
        case other                    => other
      }
      m + (k -> s)
    }

    // Add or replace the map associated with the program.
    stateMap = stateMap + (ProgKey(prog) -> obsMap)

    // Now mark all the new and updated observations "edited" in order to kick
    // off AGS searches.  We can tell that they have been updated or else never
    // before checked because the hash value is None.  Unchanged idle
    // observations (which should account for most of an existing program's
    // observations) will have an AgsHashVal that still matches in the new
    // program and so they will be skipped.
    obsMap.values.foreach {
      case IdleState(o, None) => edited(o)
      case _                  => // do nothing
    }

    // Watch the program for future edits.
    prog.addCompositeChangeListener(ChangeListener)
    prog.addStructureChangeListener(StructureListener)
  }

  /** Remove a program from our watch list and remove listeners.
    */
  def unwatch(prog: ISPProgram): Unit = Swing.onEDT {
    prog.removeStructureChangeListener(StructureListener)
    prog.removeCompositeChangeListener(ChangeListener)
    stateMap = stateMap - ProgKey(prog)
  }

  // Watches for changes to existing observations, runs BAGS on them when updated.
  object ChangeListener extends PropertyChangeListener {
    override def propertyChange(evt: PropertyChangeEvent): Unit =
      evt.getSource match {
        case node: ISPNode => Option(node.getContextObservation).foreach { o => Swing.onEDT {
          // Update the state map to add a new IdleState if it is missing the
          // observation that was changed.
          val k       = ObsKey(o)
          val obsMap  = stateMap.lookup(k.prog).getOrElse(==>>.empty[ObsKey, BagsState])
          val obsMap2 = obsMap.alter(k, {
            case None => Some(IdleState(k, None))
            case x    => x
          })
          stateMap = stateMap + (ProgKey(o) -> obsMap2)

          // Now mark it edited.
          BagsManager.edited(ObsKey(o))
        }}
        case _             => // Ignore, not in an observation.
      }
  }

  // Watches for new observations in a program, runs BAGS on them when discovered.
  object StructureListener extends PropertyChangeListener {
    override def propertyChange(evt: PropertyChangeEvent): Unit =
      evt.getSource match {
        case oc: ISPObservationContainer => Swing.onEDT {
          val progKey     = ProgKey(oc.getProgram)
          val existingMap = stateMap.lookup(progKey).getOrElse(==>>.empty[ObsKey, BagsState])

          // Go through all the observations in this container adding any that
          // are missing.
          val allObs      = oc.getAllObservations.asScala.toList
          val missing     = allObs.filter { o => existingMap.lookup(ObsKey(o)).isEmpty }
          val missingMap  = ==>>.fromList(missing.map { o =>
            val k = ObsKey(o)
            k -> (IdleState(k, None): BagsState)
          })
          stateMap = stateMap + (progKey -> existingMap.union(missingMap))

          // Mark any missing observations edited to do the AGS lookup.
          missing.foreach(o => BagsManager.edited(ObsKey(o)))
        }
        case _                           => // ignore, not an observation container
      }
  }

  // -------------------------------------------------------------------------
  // Side effects that accompany the state transitions.
  // -------------------------------------------------------------------------

  private[ags] def wakeUpAction(k: ObsKey, delayMs: Long): IO[Unit] = IO {
    worker.schedule(new Runnable() {
      def run(): Unit = BagsManager.wakeUp(k)
    }, delayMs, TimeUnit.MILLISECONDS)
  }

  private[ags] def triggerAgsAction(k: ObsKey, ctx: ObsContext, ags: AgsStrategy): IO[Unit] = IO {
    ags.select(ctx, OT.getMagnitudeTable)(blockingExecutionContext).onComplete {
      case Success(opt) =>
        Log.info(s"Successful BAGS lookup for observation=${k.oid.getOrElse("?")}; applying on ${Thread.currentThread}")
        BagsManager.success(k, opt)

      case Failure(CatalogException((e: GenericError) :: _)) =>
        BagsManager.fail(k, "Catalog lookup failed.", 5000)

      case Failure(ex: TimeoutException) =>
        BagsManager.fail(k, "Catalog timed out.", 0)

      case Failure(ex) =>
        BagsManager.fail(k, s"Unexpected error ${Option(ex.getMessage).getOrElse("")}", 5000)
    }
  }

  private[ags] def clearAction(k: ObsKey): IO[Unit] =
    applyAction(k, None)

  private[ags] def applyAction(k: ObsKey, selOpt: Option[AgsStrategy.Selection]): IO[Unit] = IO {
    def applySelection(ctx: TpeContext): Unit = {
      val oldEnv = ctx.targets.envOrDefault

      // If AGS results were found, apply them to the target env; otherwise, clear out any existing auto group.
      val newEnv = selOpt.map(_.applyTo(oldEnv)).getOrElse {
        val oldGuideEnv = oldEnv.getGuideEnvironment.guideEnv
        if (oldGuideEnv.auto === AutomaticGroup.Initial || oldGuideEnv.auto === AutomaticGroup.Disabled) oldEnv
        else oldEnv.setGuideEnvironment(GuideEnvironment(GuideEnv(AutomaticGroup.Initial, oldGuideEnv.manual)))
      }

      // Calculate the new target environment and if they are different referentially, apply them.
      ctx.targets.dataObject.foreach { targetComp =>
        // If the env reference hasn't changed, this does nothing.
        if (oldEnv != newEnv) {
          targetComp.setTargetEnvironment(newEnv)
          ctx.targets.commit()
        }

        // Change the pos angle as appropriate if this is the auto group.
        selOpt.foreach { sel =>
          if (newEnv.getPrimaryGuideGroup.isAutomatic && selOpt.isDefined) {
            ctx.instrument.dataObject.foreach { inst =>
              val deg = sel.posAngle.toDegrees
              val old = inst.getPosAngleDegrees
              if (deg != old) {
                inst.setPosAngleDegrees(deg)
                ctx.instrument.commit()
              }
            }
          }
        }
      }
    }

    def doApply(obs: ISPObservation): Unit = {
      obs.getProgram.removeStructureChangeListener(StructureListener)
      obs.getProgram.removeCompositeChangeListener(ChangeListener)
      applySelection(TpeContext(obs))

      // Update the TPE if it is visible
      selOpt.foreach { sel =>
        Option(TpeManager.get()).filter(_.isVisible).foreach { tpe =>
          sel.assignments.foreach { ass =>
            val clazz = ass.guideProbe.getType match {
              case GuideProbe.Type.AOWFS => classOf[Altair_WFS_Feature]
              case GuideProbe.Type.OIWFS => classOf[OIWFS_Feature]
              case GuideProbe.Type.PWFS  => classOf[TpePWFSFeature]
            }
            Option(tpe.getFeature(clazz)).foreach(tpe.selectFeature)
          }
        }
      }

      obs.getProgram.addCompositeChangeListener(ChangeListener)
      obs.getProgram.addStructureChangeListener(StructureListener)
    }

    fetchObs(k).foreach(doApply)
  }
}
