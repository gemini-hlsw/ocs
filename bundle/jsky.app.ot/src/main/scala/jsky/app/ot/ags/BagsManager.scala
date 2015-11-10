package jsky.app.ot.ags

import java.beans.{PropertyChangeEvent, PropertyChangeListener}
import java.util.concurrent.{ThreadFactory, TimeoutException, TimeUnit, ScheduledThreadPoolExecutor}
import java.util.logging.{Level, Logger}

import edu.gemini.ags.api.{AgsRegistrar, AgsStrategy}
import edu.gemini.ags.gems.GemsGuideStars
import edu.gemini.catalog.votable.{CatalogException, GenericError}
import edu.gemini.pot.sp.{ISPObservationContainer, ISPNode, ISPProgram, ISPObservation, SPNodeKey}
import edu.gemini.shared.util.immutable.{Option => GOption, DefaultImList}
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.spModel.obs.ObservationStatus
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.rich.shared.immutable._
import edu.gemini.spModel.target.env._
import jsky.app.ot.OT
import jsky.app.ot.tpe.{TpeContext, GuideStarSupport, GemsGuideStarWorker, GuideStarWorker}

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.swing.Swing
import scala.util.{Success, Failure, Try}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import scalaz._, Scalaz._


final class BagsManager(executor: ScheduledThreadPoolExecutor) {
  private val LOG: Logger = Logger.getLogger(getClass.getName)

  /** Syntax for ObsContext. */
  implicit class ObsContextOps(ctx: ObsContext) {
    private def extractPrimaryGuideGroupTargets(): List[GuideProbeTargets] = {
      ctx.getTargets.getGuideEnvironment.getPrimary.asScalaOpt.fold(List.empty[GuideProbeTargets])(_.getAll.asScalaList)
    }

    def hasManualPrimary: Boolean = extractPrimaryGuideGroupTargets().exists(_.primaryIsManual)
    def isMissingSearch:  Boolean = {
      val gpts = extractPrimaryGuideGroupTargets()
      gpts.isEmpty || gpts.exists(_.getBagsResult == BagsResult.NoSearchPerformed)
    }
  }

  /** Our state is just a set of programs to watch, a set of pending keys, and a map of keys to BAGS status. */
  case class BagsState(programs: Set[SPProgramID], keys: Set[SPNodeKey], statuses: Map[SPNodeKey,BagsStatus]) {
    def +(key: SPNodeKey): BagsState = copy(keys = keys + key)
    def -(key: SPNodeKey): BagsState = copy(keys = keys - key)
    def +(pid: SPProgramID): BagsState = copy(programs = programs + pid)
    def -(pid: SPProgramID): BagsState = copy(programs = programs - pid)
    def setStatus(key: SPNodeKey, status: BagsStatus): BagsState = copy(statuses = statuses + ((key, status)))
    def clearStatus(key: SPNodeKey): BagsState = copy(statuses = statuses - key)
  }

  /** Our state. Modifications must be synchronized on `this`. */
  @volatile private var state: BagsState = BagsState(Set.empty, Set.empty, Map.empty)

  /**
   * Atomically add a program to our watch list and attach listeners. Enqueue all observations for
   * consideration for a BAGS lookup.
   */
  def watch(prog: ISPProgram): Unit = {
        synchronized {
          state += prog.getProgramID
          prog.addStructureChangeListener(StructurePropertyChangeListener)
          prog.addCompositeChangeListener(CompositePropertyChangeListener)
        }
        prog.getAllObservations.asScala.foreach(enqueue(_, 0L, initialEnqueue = true))
  }

  /**
   * Atomically remove a program from our watch list and remove listeners. Any queued observations
   * will be discarded as they come up for consideration.
   */
  def unwatch(prog: ISPProgram): Unit = {
        synchronized {
          state -= prog.getProgramID
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
  def enqueue(observation: ISPObservation, delay: Long, initialEnqueue: Boolean = false): Unit =
    Option(observation).foreach { obs =>
      synchronized {
        val key = obs.getNodeKey
        state += key

        def bagsStatus(status: BagsStatus): Unit = {
          val oldState = state.statuses.get(key)
          state = state.setStatus(key, status)
          notifyBagsStatusListeners(observation, oldState, Some(status))
        }
        def bagsClearStatus(): Unit = {
          val oldState = state.statuses.get(key)
          state = state.clearStatus(key)
          notifyBagsStatusListeners(observation, oldState, None)
        }

        // The criteria to perform a BAGS lookup is that:
        // 1. There are no manual primary targets in the primary guide group;
        // 2a. This is not the initial call to enqueue (i.e. the call was triggered by a change to the TargetEnv); or
        // 2b. The primary guide group has some active guide probe for which a BAGS search has not been done; and
        // 3. The observation has not been executed.
        def obsCtxFilter(obsCtx: ObsContext): Boolean =
          !obsCtx.hasManualPrimary &&
            (!initialEnqueue || obsCtx.isMissingSearch) &&
            ObservationStatus.computeFor(obs) != ObservationStatus.OBSERVED

        // If dequeue is false this means that (a) another task scheduled *after* me ended up
        // running before me, so their result is as good as mine would have been and we're done;
        // or (b) we don't care about that program anymore, so we're done.
        if (dequeue(key, obs.getProgramID)) {
          // Otherwise construct an obs context, verify that it's bagworthy, and go
          ObsContext.create(obs).asScalaOpt.filter(obsCtxFilter).foreach { ctx =>
            // Reset the BAGS result to NoSearchPerformed.
            // We do this so if the OT is closed before the search is completed, it knows to start the search
            // again on startup.
            def resetBagsResult(): Unit = {
              obs.getProgram.removeCompositeChangeListener(CompositePropertyChangeListener)
              obs.getProgram.removeStructureChangeListener(StructurePropertyChangeListener)
              val tpeContext = TpeContext(obs)
              val newEnv = BagsManager.clearBagsTargets(tpeContext.targets.envOrDefault, BagsResult.NoSearchPerformed)
              tpeContext.targets.dataObject.foreach { targetComp =>
                targetComp.setTargetEnvironment(newEnv)
                tpeContext.targets.commit()
              }
              obs.getProgram.addStructureChangeListener(StructurePropertyChangeListener)
              obs.getProgram.addCompositeChangeListener(CompositePropertyChangeListener)
            }
            resetBagsResult()

            bagsStatus(BagsStatus.Pending)
            executor.schedule(new Runnable {

              override def run(): Unit = {
                //   do the lookup
                //   on success {
                //      if we're in the queue again, it means something changed while this task was
                //      running, so discard this result and do nothing,
                //      otherwise update the model
                //   }
                //   on failure enqueue again
                val bagsIdMsg = s"BAGS lookup on thread=${Thread.currentThread.getId} for observation=${obs.getObservationID}"

                def lookup[S, T](optExtract: S => Option[T])(worker: (TpeContext, S) => Unit)(results: Try[S]): Unit = {
                  results match {
                    case Success(selOpt) =>
                      // If this observation is once again in the queue, then something changed while this task
                      // was running, so discard the result.
                      if (!state.keys(key)) {
                        LOG.info(s"$bagsIdMsg successful. Results=${optExtract(selOpt) ? "Yes" | "No"}.")
                        Swing.onEDT {
                          // Unfortunately, we need a fresh TpeContext here in case any changes were made since scheduling
                          // with the executor.
                          val tpeContext = TpeContext(obs)
                          obs.getProgram.removeCompositeChangeListener(CompositePropertyChangeListener)
                          obs.getProgram.removeStructureChangeListener(StructurePropertyChangeListener)
                          worker(tpeContext, selOpt)
                          obs.getProgram.addStructureChangeListener(StructurePropertyChangeListener)
                          obs.getProgram.addCompositeChangeListener(CompositePropertyChangeListener)
                        }
                        bagsClearStatus()
                      }

                    // NOTE: We sleep for a brief duration BEFORE enqueue to allow the error message to be readable.
                    // We don't want to print the stack trace if the host is simply unreachable.
                    // This is reported only as a GenericError in a CatalogException, unfortunately.
                    case Failure(CatalogException((e: GenericError) :: _)) =>
                      LOG.warning(s"$bagsIdMsg failed: ${e.msg}")
                      bagsStatus(BagsStatus.Failed("Catalog lookup failed."))
                      Thread.sleep(3000)
                      enqueue(obs, 5000L)

                    // If we timed out, we don't want to delay.
                    case Failure(e: TimeoutException) =>
                      LOG.warning(s"$bagsIdMsg failed: ${e.getMessage}")
                      bagsStatus(BagsStatus.Failed("Catalog could not be contacted."))
                      Thread.sleep(3000)
                      enqueue(obs, 5000L)

                    // For all other exceptions, print the full stack trace.
                    case Failure(e) =>
                      LOG.log(Level.WARNING, s"$bagsIdMsg} failed.", e)
                      bagsStatus(BagsStatus.Failed(s"${e.getMessage}."))
                      Thread.sleep(3000)
                      enqueue(obs, 5000L)
                  }
                }

                LOG.info(s"Performing $bagsIdMsg.")
                bagsStatus(BagsStatus.Running)

                AgsRegistrar.currentStrategy(ctx).foreach { strategy =>
                  if (GuideStarSupport.hasGemsComponent(obs)) {
                    lookup((x: GOption[GemsGuideStars]) => x.asScalaOpt)(GemsGuideStarWorker.applyResults(_, _, true))(Try(GemsGuideStarWorker.findGuideStars(ctx)))
                  } else {
                    val fut = strategy.select(ctx, OT.getMagnitudeTable)

                    // This is a hideous abuse of Futures, but I don't see a way around it when combining AGS lookups
                    // with GeMS lookups. We do not want the thread to spawn a Future and return immediately, because
                    // then a huge number of tasks will be delegated to a single thread instead of being evenly
                    // dispersed amongst the available threads, which will cause serious slowdown later.
                    if (Try {
                      val futDone = Await.ready(fut, 30.seconds)
                      futDone.onComplete(lookup(identity[Option[AgsStrategy.Selection]])(GuideStarWorker.applyResults))
                    }.isFailure) {
                      bagsStatus(BagsStatus.Failed("Lookup timed out."))
                      enqueue(obs, 5000L)
                    }
                  }
                }
              }
            }, delay, TimeUnit.MILLISECONDS)
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

  /** BAGS status changes. **/
  def bagsStatus(key: SPNodeKey): Option[BagsStatus] =
    synchronized(state.statuses.get(key))

  /** Listeners for BAGS status changes. **/
  private var listeners: List[BagsStatusListener] = Nil

  def addBagsStatusListener(l: BagsStatusListener): Unit =
    if (!listeners.contains(l))
      listeners = l :: listeners

  def removeBagsStatusListener(l: BagsStatusListener): Unit =
    listeners = listeners.filterNot(_ == l)

  def clearBagsStatusListeners(): Unit =
    listeners = Nil

  private def notifyBagsStatusListeners(obs: ISPObservation, oldStatus: Option[BagsStatus], newStatus: Option[BagsStatus]): Unit = {
    listeners.foreach(l => Try{l.bagsStatusChanged(obs, oldStatus.asGeminiOpt, newStatus.asGeminiOpt)})
  }
}

object BagsManager {
  private val NumThreads = math.max(1, Runtime.getRuntime.availableProcessors - 1)
  val instance = new BagsManager(new ScheduledThreadPoolExecutor(NumThreads, new ThreadFactory {
    override def newThread(r: Runnable): Thread = {
      val thread = new Thread(r)
      thread.setPriority(Thread.NORM_PRIORITY-1)
      thread
    }
  }))

  // Check two target environments to see if the BAGS targets match exactly between them.
  def bagsTargetsMatch(oldEnv: TargetEnvironment, newEnv: TargetEnvironment): Boolean = {
    // Find a group with a BAGS target in it in the old and new envs.
    val oldGroup = oldEnv.getGroups.asScala.find(gg => gg.getAll.asScala.exists(_.getBagsResult.target.isDefined))
    val newGroup = newEnv.getGroups.asScala.find(gg => gg.getAll.asScala.exists(_.getBagsResult.target.isDefined))

    // Now compare the two groups to see if they have the same BAGS targets.
    // Filter the GuideProbeTargets of the oldGroup to make sure that we are only looking at GPTs with BAGS.
    val oldGpt = oldGroup.toList.flatMap(gg => gg.getAll.asScala.filter(_.getBagsResult.target.isDefined))
    val newGpt = newGroup.toList.flatMap(gg => gg.getAll.asScala.filter(_.getBagsResult.target.isDefined))

    // Now compare the two lists to make sure they have the same BAGS targets.
    (oldGpt.size == newGpt.size) &&
      oldGpt.forall(ogpt => newGpt.exists(ngpt => ngpt.getGuider == ogpt.getGuider &&
        ngpt.getBagsResult.target.get.getTarget.equals(ogpt.getBagsResult.target.get.getTarget)))
  }

  // Given a target environment, clear all of the BAGS targets from it.
  // Set the BAGS results to the given result, which is that no search has been performed by default.
  def clearBagsTargets(oldEnv: TargetEnvironment, clearedBagsResult: BagsResult = GuideProbeTargets.DEFAULT_BAGS_RESULT): TargetEnvironment = {
    val oldGuideEnv = oldEnv.getGuideEnvironment
    val newGuideEnv = oldGuideEnv.getOptions.asScala.foldLeft(oldGuideEnv) { (ge, gg) =>
      // For this guide group, iterate over all GuideProbeTargets and eliminate the BAGS targets. Filter out any
      // GuideProbeTargets that are left empty as a result as we no longer need them.
      val bagslessGpts = gg.getAll.asScala.map(_.withBagsResult(clearedBagsResult)).filter(_.containsTargets)

      // If there are still any targets left, replace gg in the guide environment with a new guide group.
      // If there are no targets left, remove gg from the guide environment.
      if (bagslessGpts.nonEmpty) {
        val newGG = gg.setAll(DefaultImList.create(bagslessGpts.asJavaCollection))
        val idx = ge.getOptions.indexOf(gg)
        ge.setOptions(ge.getOptions.updated(idx, newGG))
      } else {
        ge.update(OptionsList.UpdateOps.remove(gg))
      }
    }
    oldEnv.setGuideEnvironment(newGuideEnv)
  }

  // Determine if an ObsContext has a primary guide group with a primary guide star.
  def hasPrimary(ctx: ObsContext): Boolean =
    ctx.getTargets.getGuideEnvironment.getPrimary.asScalaOpt.exists(_.getAll.asScalaList.forall(_.getPrimary.isDefined))
}