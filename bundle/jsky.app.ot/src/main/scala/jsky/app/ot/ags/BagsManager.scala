package jsky.app.ot.ags

import java.beans.{PropertyChangeEvent, PropertyChangeListener}
import java.time.Instant
import java.util.concurrent.TimeoutException
import java.util.concurrent._
import java.util.logging.{Level, Logger}

import edu.gemini.ags.api.{AgsHash, AgsRegistrar, AgsStrategy}
import edu.gemini.catalog.votable.{CatalogException, GenericError}
import edu.gemini.pot.sp._
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.spModel.guide.GuideProbe
import edu.gemini.spModel.obs.{ObsClassService, ObservationStatus, SPObservation}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.obsclass.ObsClass
import edu.gemini.spModel.rich.shared.immutable._
import edu.gemini.spModel.target.env.{AutomaticGroup, GuideEnv, GuideEnvironment}
import jsky.app.ot.OT
import jsky.app.ot.gemini.altair.Altair_WFS_Feature
import jsky.app.ot.gemini.inst.OIWFS_Feature
import jsky.app.ot.gemini.tpe.TpePWFSFeature
import jsky.app.ot.tpe.{TpeContext, TpeManager}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.swing.Swing
import scala.util.{Failure, Success, Try}
import scalaz._
import Scalaz._


final class BagsManager(executorService: ExecutorService) {
  private val LOG: Logger = Logger.getLogger(getClass.getName)

  private implicit val executionContext = new ExecutionContext {
    def shutdown() = executorService.shutdown()
    override def reportFailure(t: Throwable): Unit = throw t
    override def execute(runnable: Runnable): Unit = {
      executorService.submit(new Thread(runnable) {
        setPriority(Thread.NORM_PRIORITY - 1)
      })
    }
  }

  type ProgramKey = (SPProgramID, SPNodeKey)
  type AgsHash    = Int
  type ObsStatus  = (BagsStatus, AgsHash)

  implicit val spNodeKeyOrder: Order[SPNodeKey] = Order.orderBy(_.uuid.toString)
  implicit val programKeyOrder: Order[ProgramKey] = Order.orderBy(Option(_).map(_.toString))

  private def extractProgKey(prog: ISPProgram): ProgramKey =
    (prog.getProgramID, prog.getNodeKey)
  private def extractProgKey(obs: ISPObservation): ProgramKey =
    extractProgKey(obs.getProgram)

  private case class BagsData(statuses: ProgramKey ==>> (SPNodeKey ==>> ObsStatus),
                              pendingObservations: Set[SPNodeKey],
                              listeners: List[BagsStatusListener]) {
    def +(obsKey: SPNodeKey): BagsData = copy(pendingObservations = pendingObservations + obsKey)
    def -(obsKey: SPNodeKey): BagsData = copy(pendingObservations = pendingObservations - obsKey)

    def +(listener: BagsStatusListener): BagsData = copy(listeners = listener :: listeners)
    def -(listener: BagsStatusListener): BagsData = copy(listeners = listeners.diff(List(listener)))

//    private def notify(obs: ISPObservation, oldStatus: Option[ObsStatus], newStatus: Option[ObsStatus]): Unit =
//      listeners.foreach(_.bagsStatusChanged(obs, oldStatus.map(_._1).asGeminiOpt, newStatus.map(_._1).asGeminiOpt))

    private def notifyListeners(obs: ISPObservation, oldStatus: Option[ObsStatus], newStatus: Option[ObsStatus]): Unit = {
      val immutableListenerList = synchronized(listeners)
      Swing.onEDT {
        immutableListenerList.foreach(l => Try {
          l.bagsStatusChanged(obs, oldStatus.map(_._1).asGeminiOpt, newStatus.map(_._1).asGeminiOpt)
        })
      }
    }

    def updateObsStatus(obs: ISPObservation, newStatus: Option[ObsStatus]): BagsData = {
      val progKey   = extractProgKey(obs)
      val obsKey    = obs.getNodeKey
      val obsMap    = statuses.lookup(progKey).getOrElse(==>>.empty)
      val oldStatus = obsMap.lookup(obsKey)

      if (oldStatus != newStatus) {
        val newStatuses = newStatus match {
          case Some(status) => statuses + (progKey, obsMap + ((obsKey, status)))
          case None         => statuses + (progKey, obsMap - obsKey)
        }
        notifyListeners(obs, oldStatus, newStatus)
        copy(statuses = newStatuses)
      } else this
    }

    def obsStatus(obs: ISPObservation): Option[ObsStatus] =
      for {
        m <- statuses.lookup(extractProgKey(obs))
        s <- m.lookup(obs.getNodeKey)
      } yield s

    def clearStatusForProgram(prog: ISPProgram): BagsData = {
      // TODO: Do we really need to do this notification? Do we care?
      val progKey = extractProgKey(prog)
      for {
        m     <- statuses.lookup(progKey)
        (k,s) <- m.toList
        obs   <- prog.getAllObservations.asScala.find(_.getNodeKey == k)
      } notifyListeners(obs, s.some, None)
      copy(statuses = statuses - progKey)
    }

    def hash(obs: ISPObservation): Option[AgsHash] =
      obsStatus(obs).map(_._2)

    def updateHash(obs: ISPObservation, newHash: Option[AgsHash]): BagsData = {
      val status = obsStatus(obs).map(_._1) | BagsStatus.NewPending
      updateObsStatus(obs, newHash.map((status, _)))
    }

    def bagsStatus(obs: ISPObservation): Option[BagsStatus] =
      obsStatus(obs).map(_._1)

    def updateBagsStatus(obs: ISPObservation, newStatus: Option[BagsStatus]): BagsData = {
      val obsStatus = {
        newStatus.map((_, hash(obs) | {
          val when = obs.getDataObject.asInstanceOf[SPObservation].getSchedulingBlock.asScalaOpt.map(_.start) | Instant.now.toEpochMilli
          ObsContext.create(obs).asScalaOpt.map(AgsHash.hash(_, when)) | 0
        }))
      }
      updateObsStatus(obs, obsStatus)
    }
  }

  private var data: BagsData = BagsData(==>>.empty, Set.empty, List.empty)

  // Synchronized access to Bags status information.
  def bagsStatus(obs: ISPObservation): Option[BagsStatus] =
    synchronized(data.bagsStatus(obs))

  /**
    * Atomically add a program to our watch list and attach listeners.
    * Enqueue all observations for consideration for a BAGS lookup.
    */
  def watch(prog: ISPProgram): Unit =
    prog <|
      (_.addStructureChangeListener(StructurePropertyChangeListener)) <|
      (_.addCompositeChangeListener(CompositePropertyChangeListener)) <|
      (_.getAllObservations.asScala.foreach(enqueue(_, 0L)))

  /**
    * Atomically remove a program from our watch list and remove listeners. Any queued observations
    * will be discarded as they come up for consideration.
    */
  def unwatch(prog: ISPProgram): Unit = {
    synchronized(data = data.clearStatusForProgram(prog))
    prog.removeStructureChangeListener(StructurePropertyChangeListener)
    prog.removeCompositeChangeListener(CompositePropertyChangeListener)
  }

  /**
    * Remove the specified key from the task queue, if present. Return true if the key was present
    * *and* the specified program is still on the watch list.
    */
  private def dequeue(obs: ISPObservation): Boolean =
    synchronized {
      val obsKey = obs.getNodeKey
      if (data.pendingObservations(obsKey)) {
        data -= obsKey
        data.statuses.keys.contains(extractProgKey(obs.getProgram))
      } else false
    }

  /**
    * Atomically enqueue a task that will consider the specified observation for BAGS lookup, after
    * a delay of at least `delay` milliseconds.
    */
  def enqueue(obs: ISPObservation, delay: Long): Unit = {
    val key = obs.getNodeKey

    // Performs checks to rule out disabled guide groups, instruments without guiding strategies (e.g. GPI),
    // observation classes that do not have guiding (e.g. daytime calibration).
    // NOTE: Only called in synchronized code, so does not require synchronization.
    def isEligibleForBags(ctx: ObsContext): Boolean = {
      val enabledGroup = ctx.getTargets.getGuideEnvironment.guideEnv.auto match {
        case AutomaticGroup.Initial | AutomaticGroup.Active(_, _) => true
        case _ => false
      }
      enabledGroup &&
        AgsRegistrar.currentStrategy(ctx).nonEmpty &&
        ObsClassService.lookupObsClass(obs) != ObsClass.DAY_CAL
    }

    // Determine if the observation has been updated since the last successful BAGS lookup.
    // Returns true / false indicating this, as well as the most recent hash value for the observation.
    // NOTE: Only called in synchronized code, so does not require synchronization.
    def hasBeenUpdated(ctx: ObsContext): (Boolean, AgsHash) = {
      val when    = obs.getDataObject.asInstanceOf[SPObservation].getSchedulingBlock.asScalaOpt.map(_.start) | Instant.now.toEpochMilli
      val curHash = data.hash(obs)
      val newHash = AgsHash.hash(ctx, when)

      (!curHash.contains(newHash), newHash)
    }

    // NOTE: Only called in synchronized code, so does not require synchronization.
    def notObserved(): Boolean =
      ObservationStatus.computeFor(obs) != ObservationStatus.OBSERVED

    // NOTE: Only called in synchronized code, so does not require synchronization.
    def applySwingResults(opt: Option[AgsStrategy.Selection]): Unit = {
      Swing.onEDT {
        obs.getProgram.removeCompositeChangeListener(CompositePropertyChangeListener)
        obs.getProgram.removeStructureChangeListener(StructurePropertyChangeListener)
        BagsManager.applyResults(TpeContext(obs), opt)
        obs.getProgram.addStructureChangeListener(StructurePropertyChangeListener)
        obs.getProgram.addCompositeChangeListener(CompositePropertyChangeListener)
      }
    }

    synchronized {
      // Add the observation to the list of observations to process, and place it in NewPending mode.
      data += key
      data = data.updateBagsStatus(obs, BagsStatus.NewPending.some)

      // If dequeue is false this means that (a) another task scheduled *after* me ended up
      // running before me, so their result is as good as mine would have been and we're done;
      // or (b) we don't care about that program anymore, so we're done.
      if (dequeue(obs)) {
        // Otherwise construct an obs context, verify that it's bags-worthy, and go.
        ObsContext.create(obs).asScalaOpt.foreach { ctx =>
          val eligible = isEligibleForBags(ctx)
          val (updated, hash) = hasBeenUpdated(ctx)
          if (eligible && updated && notObserved()) {
            //   do the lookup
            //   on success {
            //      if we're in the queue again, it means something changed while this task was
            //      running, so discard this result and do nothing,
            //      otherwise update the model
            //   }
            //   on failure enqueue again, maybe with a delay depending on the failure
            val bagsIdMsg = s"BAGS lookup on thread=${Thread.currentThread.getId} for observation=${obs.getObservationID}"
            LOG.info(s"Performing $bagsIdMsg.")

            // TODO: This will ALWAYS return true as we filtered on this in isEligible.
            // TODO: Do we need to do it or can we get away with something else?
            AgsRegistrar.currentStrategy(ctx).foreach { strategy =>
              val newStatus = data.bagsStatus(obs).fold(BagsStatus.NewRunning)(_.toRunning)
              data = data.updateBagsStatus(obs, newStatus.some)

              // Sleep for the required delay, and then run the AGS strategy selection, which already runs in a
              // separate thread.
              val fut = Future(Thread.sleep(delay)).flatMap(_ => strategy.select(ctx, OT.getMagnitudeTable))
              fut onComplete {
                case Success(opt) =>
                  // If this observation is once again in the queue, then something changed while this task
                  // was running, so discard the result.
                  if (!data.pendingObservations(key)) {
                    LOG.info(s"$bagsIdMsg successful. Results=${opt ? "Yes" | "No"}.")
                    applySwingResults(opt)
                    data = data.updateObsStatus(obs, (BagsStatus.Success, hash).some)
                  }

                // We don't want to print the stack trace if the host is simply unreachable.
                // This is reported only as a GenericError in a CatalogException, unfortunately.
                case Failure(CatalogException((e: GenericError) :: _)) =>
                  LOG.warning(s"$bagsIdMsg failed: ${e.msg}")
                  data = data.updateBagsStatus(obs, BagsStatus.Pending("Catalog lookup failed.".some).some)
                  enqueue(obs, BagsManager.delay)

                // If we timed out, we don't want to delay.
                case Failure(ex: TimeoutException) =>
                  LOG.warning(s"$bagsIdMsg failed: ${ex.getMessage}")
                  data = data.updateBagsStatus(obs, BagsStatus.Pending("Catalog timed out.".some).some)
                  enqueue(obs, BagsManager.delay)

                // For all other exceptions, print the full stack trace.
                case Failure(ex) =>
                  LOG.log(Level.WARNING, s"$bagsIdMsg} failed.", ex)
                  data = data.updateBagsStatus(obs, BagsStatus.Pending(s"${ex.getMessage}.".some).some)
                  enqueue(obs, BagsManager.delay)
              }
            }
          } else if (!eligible) {
            LOG.info(s"${obs.getObservationID} not eligible for BAGS. Clearing auto group.")
            applySwingResults(None)
            data = data.updateObsStatus(obs, None)
          }
        }
      } else {
        LOG.info(s"${obs.getObservationID} not enqueued for BAGS.")
        data = data.updateObsStatus(obs, None)
      }
    }
  }

  object CompositePropertyChangeListener extends PropertyChangeListener {
    override def propertyChange(evt: PropertyChangeEvent): Unit =
      evt.getSource match {
        case node: ISPNode => Option(node.getContextObservation).foreach(enqueue(_, 0L))
        case _             => // Ignore
      }
  }

  object StructurePropertyChangeListener extends PropertyChangeListener {
    override def propertyChange(evt: PropertyChangeEvent): Unit =
      evt.getSource match {
        case cont: ISPObservationContainer => cont.getAllObservations.asScala.foreach(enqueue(_, 0L))
        case _                             => // Ignore
      }
  }

  /** Listeners for BAGS status changes. **/
  def addBagsStatusListener(listener: BagsStatusListener): Unit = synchronized {
      if (!data.listeners.contains(listener))
        data += listener
    }

  def removeBagsStatusListener(listener: BagsStatusListener): Unit = synchronized {
      if (data.listeners.contains(listener))
        data -= listener
    }
}

object BagsManager {
  val delay = 5000L

  val instance = {
    // Limit execution of futures in the BagsManager instance to an execution context with NumWorkers threads.
    val NumWorkers = math.max(1, Runtime.getRuntime.availableProcessors - 1)
    val executor = Executors.newFixedThreadPool(NumWorkers)
    new BagsManager(executor)
  }

  private def applyResults(ctx: TpeContext, selOpt: Option[AgsStrategy.Selection]): Unit = {
    applySelection(ctx, selOpt)
    showTpeFeatures(selOpt)
  }

  /** Update the TPE if it is visible */
  private def showTpeFeatures(selOpt: Option[AgsStrategy.Selection]): Unit =
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

  private def applySelection(ctx: TpeContext, selOpt: Option[AgsStrategy.Selection]): Unit = {
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
}