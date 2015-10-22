package jsky.app.ot.ags

import java.beans.{PropertyChangeEvent, PropertyChangeListener}
import java.util.concurrent.{TimeoutException, TimeUnit, ScheduledThreadPoolExecutor}
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
import edu.gemini.spModel.target.env.{OptionsList, GuideProbeTargets, TargetEnvironment}
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
    def isEligibleForBags: Boolean = {
      val targets = ctx.getTargets
      val prgs    = targets.getGuideEnvironment.getPrimaryReferencedGuiders
      prgs.isEmpty || prgs.asScala.exists { gp =>
        targets.getPrimaryGuideProbeTargets(gp).asScalaOpt.forall(_.getBagsTarget.isEmpty)
      }
    }
  }

  /** Our state is just a set of programs to watch, and a set of pending keys. */
  case class BagsState(programs: Set[SPProgramID], keys: Set[SPNodeKey]) {
    def +(key: SPNodeKey): BagsState = copy(keys = keys + key)
    def -(key: SPNodeKey): BagsState = copy(keys = keys - key)
    def +(pid: SPProgramID): BagsState = copy(programs = programs + pid)
    def -(pid: SPProgramID): BagsState = copy(programs = programs - pid)
  }

  /** Our state. Modifications must be synchronized on `this`. */
  @volatile private var state: BagsState = BagsState(Set.empty, Set.empty)

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
    prog.getAllObservations.asScala.foreach(enqueue(_, 0L))
  }

  /**
   * Atomically remove a program from our watch list and remove listeners. Any queued observations
   * will be discarded as they come up for consideration.
   */
  def unwatch(prog: ISPProgram): Unit =
    synchronized {
      state -= prog.getProgramID
      prog.removeStructureChangeListener(StructurePropertyChangeListener)
      prog.removeCompositeChangeListener(CompositePropertyChangeListener)
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
  def enqueue(observation: ISPObservation, delay: Long): Unit =
    Option(observation).foreach { obs =>
      synchronized {
        val key = obs.getNodeKey
        state += key
        executor.schedule(new Thread {
          setPriority(Thread.NORM_PRIORITY - 1)

          override def run(): Unit =

          // If dequeue is false this means that (a) another task scheduled *after* me ended up
          // running before me, so their result is as good as mine would have been and we're done;
          // or (b) we don't care about that program anymore, so we're done.
            if (dequeue(key, obs.getProgramID)) {
              // Otherwise construct an obs context, verify that it's bagworthy, and go
              ObsContext.create(obs).asScalaOpt.filter(_.isEligibleForBags).foreach { ctx =>

                //   do the lookup
                //   on success {
                //      if we're in the queue again, it means something changed while this task was
                //      running, so discard this result and do nothing,
                //      otherwise update the model
                //   }
                //   on failure enqueue again, maybe with a delay depending on the failure
                val bagsIdMsg = s"BAGS lookup on thread=${Thread.currentThread.getId} for observation=${obs.getObservationID}"

                def lookup[S, T](optExtract: S => Option[T])(worker: (TpeContext, S) => Unit)(results: Try[S]): Unit = {
                  results match {
                    case Success(selOpt) =>
                      // If this observation is once again in the queue, then something changed while this task
                      // was running, so discard the result.
                      if (!state.keys(key)) {
                        LOG.info(s"$bagsIdMsg successful. Results=${optExtract(selOpt) ? "Yes" | "No"}.")
                        if (ObservationStatus.computeFor(obs) != ObservationStatus.OBSERVED) {
                          Swing.onEDT {
                            obs.getProgram.removeCompositeChangeListener(CompositePropertyChangeListener)
                            obs.getProgram.removeStructureChangeListener(StructurePropertyChangeListener)
                            worker(TpeContext(obs), selOpt)
                            obs.getProgram.addStructureChangeListener(StructurePropertyChangeListener)
                            obs.getProgram.addCompositeChangeListener(CompositePropertyChangeListener)
                          }
                        }
                      }

                    // We don't want to print the stack trace if the host is simply unreachable.
                    // This is reported only as a GenericError in a CatalogException, unfortunately.
                    case Failure(CatalogException((e: GenericError) :: _)) =>
                      LOG.warning(s"$bagsIdMsg failed: ${e.msg}")
                      enqueue(obs, 5000L)

                    // If we timed out, we don't want to delay.
                    case Failure(ex: TimeoutException) =>
                      LOG.warning(s"$bagsIdMsg failed: ${ex.getMessage}")
                      enqueue(obs, 0L)

                    // For all other exceptions, print the full stack trace.
                    case Failure(ex) =>
                      LOG.log(Level.WARNING, s"$bagsIdMsg} failed.", ex)
                      enqueue(obs, 5000L)
                  }
                }


                LOG.info(s"Performing $bagsIdMsg.")
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
                    }.isFailure) enqueue(obs, 5000L)
                  }
                }
              }
            }
        }, delay, TimeUnit.MILLISECONDS)
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
}

object BagsManager {
  private val NumThreads = math.max(1, Runtime.getRuntime.availableProcessors - 1)
  val instance = new BagsManager(new ScheduledThreadPoolExecutor(NumThreads))


  // Check two target environments to see if the BAGS targets match exactly between them.
  def bagsTargetsMatch(oldEnv: TargetEnvironment, newEnv: TargetEnvironment): Boolean = {
    // Find a group with a BAGS target in it in the old and new envs.
    val oldGroup = oldEnv.getGroups.asScala.find(gg => gg.getAll.asScala.exists(_.getBagsTarget.isDefined))
    val newGroup = newEnv.getGroups.asScala.find(gg => gg.getAll.asScala.exists(_.getBagsTarget.isDefined))

    // Now compare the two groups to see if they have the same BAGS targets.
    // Filter the GuideProbeTargets of the oldGroup to make sure that we are only looking at GPTs with BAGS.
    val oldGpt = oldGroup.toList.flatMap(gg => gg.getAll.asScala.filter(_.getBagsTarget.isDefined))
    val newGpt = newGroup.toList.flatMap(gg => gg.getAll.asScala.filter(_.getBagsTarget.isDefined))

    // Now compare the two lists to make sure they have the same BAGS targets.
    (oldGpt.size == newGpt.size) &&
      oldGpt.forall(ogpt => newGpt.exists(ngpt => ngpt.getGuider == ogpt.getGuider &&
        ngpt.getBagsTarget.getValue.getTarget.equals(ogpt.getBagsTarget.getValue.getTarget)))
  }

  // Given a target environment, clear all of the BAGS targets from it.
  def clearBagsTargets(oldEnv: TargetEnvironment): TargetEnvironment = {
    val oldGuideEnv = oldEnv.getGuideEnvironment
    val newGuideEnv = oldGuideEnv.getOptions.asScala.foldLeft(oldGuideEnv) { (ge, gg) =>
      // For this guide group, iterate over all GuideProbeTargets and eliminate the BAGS targets. Filter out any
      // GuideProbeTargets that are left empty as a result as we no longer need them.
      val bagslessGpts = gg.getAll.asScala.map(_.withBagsTarget(GuideProbeTargets.NO_TARGET)).filter(_.containsTargets)

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
}