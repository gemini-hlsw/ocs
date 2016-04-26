package jsky.app.ot.ags

import java.beans.{PropertyChangeEvent, PropertyChangeListener}
import java.time.Instant
import java.util
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

  /** Our state is just a set of programs to watch, and a set of pending keys. */
  private case class BagsState(programs: Set[SPProgramID], keys: Set[SPNodeKey]) {
    def +(key: SPNodeKey): BagsState = copy(keys = keys + key)
    def -(key: SPNodeKey): BagsState = copy(keys = keys - key)
    def +(pid: SPProgramID): BagsState = copy(programs = programs + pid)
    def -(pid: SPProgramID): BagsState = copy(programs = programs - pid)
  }
  @volatile private var state: BagsState = BagsState(Set.empty, Set.empty)

  /** The status of programs and observations being managed by BAGS. */
  @volatile private var bagsStatuses: Map[SPNodeKey, BagsStatus] = Map.empty

  private def setStatus(key: SPNodeKey, statusOpt: Option[BagsStatus]): Unit = {
    println(s"+++ setStatus: $key, $statusOpt")
    bagsStatuses.synchronized {
        val oldStatusOpt = bagsStatuses.get(key)
        if (oldStatusOpt != statusOpt) {
          bagsStatuses = statusOpt match {
            case Some(status) => bagsStatuses + ((key, status))
            case None => bagsStatuses - key
          }
          notifyBagsStatusListeners(key, oldStatusOpt, statusOpt)
        }
      }
    println(s"+++ setStatus done for $key")
  }

  private def clearStatusForProgram(prog: ISPProgram): Unit = {
    bagsStatuses.synchronized {
        prog.getObservations.asScala.foreach { obs =>
          val key = obs.getNodeKey
          bagsStatuses.get(key).foreach { oldStatus =>
            bagsStatuses = bagsStatuses - key
            notifyBagsStatusListeners(key, Some(oldStatus), None)
          }
        }
      }
  }

  def bagsStatus(key: SPNodeKey): Option[BagsStatus] =
    bagsStatuses.synchronized(bagsStatuses.get(key))

  /** Setup an LRU cache with max entries BagsManager.CacheSize. */
  private val hashes = new util.LinkedHashMap[SPNodeKey, Int](math.ceil((BagsManager.CacheSize + 1) * 1.334).toInt, 0.75f, true) {
    override def removeEldestEntry(e: java.util.Map.Entry[SPNodeKey, Int]): Boolean =
      size > BagsManager.CacheSize
  }

  /**
    * Atomically add a program to our watch list and attach listeners.
    * Enqueue all observations for consideration for a BAGS lookup.
    */
  def watch(prog: ISPProgram): Unit = {
    synchronized {
      state += prog.getProgramID
      prog.addStructureChangeListener(StructurePropertyChangeListener)
      prog.addCompositeChangeListener(CompositePropertyChangeListener)
    }
    prog.getAllObservations.asScala.foreach(enqueue(_, 0L))
  }

  /**
    * Atomically remove a program from our watch list and remove listeners. Any queued observations
    * will be discarded as they come up for consideration.
    */
  def unwatch(prog: ISPProgram): Unit = {
    synchronized {
      state -= prog.getProgramID
      clearStatusForProgram(prog)
      prog.removeStructureChangeListener(StructurePropertyChangeListener)
      prog.removeCompositeChangeListener(CompositePropertyChangeListener)
    }
  }

  /**
    * Remove the specified key from the task queue, if present. Return true if the key was present
    * *and* the specified program is still on the watch list.
    */
  private def dequeue(key: SPNodeKey, pid: SPProgramID): Boolean =
    synchronized {
      if (state.keys(key)) {
        state -= key
        state.programs(pid)
      } else false
    }

  /**
    * Atomically enqueue a task that will consider the specified observation for BAGS lookup, after
    * a delay of at least `delay` milliseconds.
    */
  def enqueue(observation: ISPObservation, delay: Long): Unit = {
    // Performs checks to rule out disabled guide groups, instruments without guiding strategies (e.g. GPI),
    // observation classes that do not have guiding (e.g. daytime calibration).
    def isEligibleForBags(ctx: ObsContext): Boolean = {
      val enabledGroup = ctx.getTargets.getGuideEnvironment.guideEnv.auto match {
        case AutomaticGroup.Initial | AutomaticGroup.Active(_, _) => true
        case _ => false
      }
      enabledGroup &&
        AgsRegistrar.validStrategies(ctx).nonEmpty &&
        ObsClassService.lookupObsClass(observation) != ObsClass.DAY_CAL
    }

    // Determine if the observation has been updated since the last successful BAGS lookup.
    // Returns true / false indicating this, as well as the most recent hash value for the observation.
    def hasBeenUpdated(o: ISPObservation, ctx: ObsContext): (Boolean, Int) = {
      val key = o.getNodeKey
      val when = o.getDataObject.asInstanceOf[SPObservation].getSchedulingBlock.asScalaOpt.map(_.start) | Instant.now.toEpochMilli
      val newHash = AgsHash.hash(ctx, when)
      val curHash = Option(hashes.get(key))
      (!curHash.contains(newHash), newHash)
    }

    def updateHash(o: ISPObservation, newHash: Int): Unit =
      hashes.put(o.getNodeKey, newHash)

    def notObserved(o: ISPObservation): Boolean =
      ObservationStatus.computeFor(o) != ObservationStatus.OBSERVED

    Option(observation).foreach { obs =>
      synchronized {
        def applySwingResults(opt: Option[AgsStrategy.Selection]): Unit = {
          Swing.onEDT {
            obs.getProgram.removeCompositeChangeListener(CompositePropertyChangeListener)
            obs.getProgram.removeStructureChangeListener(StructurePropertyChangeListener)
            BagsManager.applyResults(TpeContext(obs), opt)
            obs.getProgram.addStructureChangeListener(StructurePropertyChangeListener)
            obs.getProgram.addCompositeChangeListener(CompositePropertyChangeListener)
          }
        }

        val key = obs.getNodeKey
        state += key
        if (bagsStatus(key).isEmpty)
          setStatus(key, BagsStatus.NewPending.some)

        Future {
          // Wait the delay before running.
          Thread.sleep(delay)

          // If dequeue is false this means that (a) another task scheduled *after* me ended up
          // running before me, so their result is as good as mine would have been and we're done;
          // or (b) we don't care about that program anymore, so we're done.
          if (dequeue(key, obs.getProgramID)) {
            // Otherwise construct an obs context, verify that it's bags-worthy, and go.
            ObsContext.create(obs).asScalaOpt.foreach { ctx =>
              if (isEligibleForBags(ctx)) {
                // Note that we perform these checks AFTER eligibility since they are much more computationally
                // intensive, and we need to store the hash value of hasBeenUpdated.
                val (updated, hash) = hasBeenUpdated(obs, ctx)
                if (updated && notObserved(obs)) {
                  //   do the lookup
                  //   on success {
                  //      if we're in the queue again, it means something changed while this task was
                  //      running, so discard this result and do nothing,
                  //      otherwise update the model
                  //   }
                  //   on failure enqueue again, maybe with a delay depending on the failure
                  val bagsIdMsg = s"BAGS lookup on thread=${Thread.currentThread.getId} for observation=${obs.getObservationID}"
                  LOG.info(s"Performing $bagsIdMsg.")

                  val newStatus = bagsStatus(key).fold(BagsStatus.NewRunning)(_.toRunning)
                  setStatus(key, newStatus.some)
                  Thread.sleep(1000)

                  AgsRegistrar.currentStrategy(ctx).foreach { strategy =>
                    val fut = strategy.select(ctx, OT.getMagnitudeTable)
                    fut onComplete {
                      case Success(opt) =>
                        // If this observation is once again in the queue, then something changed while this task
                        // was running, so discard the result.
                        if (!state.keys(key)) {
                          LOG.info(s"$bagsIdMsg successful. Results=${opt ? "Yes" | "No"}.")
                          applySwingResults(opt)
                          setStatus(key, None)
                          updateHash(obs, hash)
                        }

                      // Note that we have to sleep after reporting a bags failure, as otherwise, there is no time for
                      // the failure status message to be read before we change back to pending.

                      // We don't want to print the stack trace if the host is simply unreachable.
                      // This is reported only as a GenericError in a CatalogException, unfortunately.
                      case Failure(CatalogException((e: GenericError) :: _)) =>
                        LOG.warning(s"$bagsIdMsg failed: ${e.msg}")
                        setStatus(key, BagsStatus.Pending("Catalog lookup failed.".some).some)
                        enqueue(obs, 5000L)

                      // If we timed out, we don't want to delay.
                      case Failure(ex: TimeoutException) =>
                        LOG.warning(s"$bagsIdMsg failed: ${ex.getMessage}")
                        setStatus(key, BagsStatus.Pending("Catalog timed out.".some).some)
                        enqueue(obs, 0L)

                      // For all other exceptions, print the full stack trace.
                      case Failure(ex) =>
                        LOG.log(Level.WARNING, s"$bagsIdMsg} failed.", ex)
                        setStatus(key, BagsStatus.Pending(s"${ex.getMessage}.".some).some)
                        enqueue(obs, 5000L)
                    }
                  }
                } else {
                  // Either not updated or is observed, so just clear the status.
                  setStatus(key, None)
                }
              } else {
                LOG.info(s"${obs.getObservationID} not eligible for BAGS. Clearing auto group.")
                applySwingResults(None)
                setStatus(key, None)
              }
            }
          } else {
            LOG.info(s"${obs.getObservationID} not enqueued for BAGS.")
            setStatus(key, None)
          }
        }
      }
    }
  }

  object CompositePropertyChangeListener extends PropertyChangeListener {
    override def propertyChange(evt: PropertyChangeEvent): Unit =
      evt.getSource match {
        case node: ISPNode => enqueue(node.getContextObservation, 0L)
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
  private var listeners: List[BagsStatusListener] = Nil

  def addBagsStatusListener(l: BagsStatusListener): Unit =
    synchronized {
      if (!listeners.contains(l))
        listeners = l :: listeners
    }

  def removeBagsStatusListener(l: BagsStatusListener): Unit =
    synchronized {
      listeners = listeners.diff(List(l))
    }

  def clearBagsStatusListeners(): Unit =
    synchronized {
      listeners = Nil
    }

  private def notifyBagsStatusListeners(key: SPNodeKey, oldStatus: Option[BagsStatus], newStatus: Option[BagsStatus]): Unit = {
    val immutableListenerList = synchronized(listeners)
    Swing.onEDT {
      immutableListenerList.foreach(l => Try {
        l.bagsStatusChanged(key, oldStatus.asGeminiOpt, newStatus.asGeminiOpt)
      })
    }
  }
}

object BagsManager {
  /** Determines how many observation's AGS hash result will be kept in the
    * LRU cache.
    */
  val CacheSize = 10000

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