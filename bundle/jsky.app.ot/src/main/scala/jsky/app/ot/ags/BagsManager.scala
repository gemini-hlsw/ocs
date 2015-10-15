package jsky.app.ot.ags

import java.beans.{PropertyChangeEvent, PropertyChangeListener}
import java.util.concurrent.{TimeoutException, LinkedBlockingQueue, Executors}
import java.util.logging.{Level, Logger}

import edu.gemini.ags.api.{AgsRegistrar, AgsStrategy}
import edu.gemini.ags.gems.GemsGuideStars
import edu.gemini.catalog.votable.{GenericError, CatalogException}
import edu.gemini.pot.sp._
import edu.gemini.spModel.obs.ObservationStatus
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.rich.shared.immutable._
import edu.gemini.spModel.target.env.TargetEnvironment
import edu.gemini.shared.util.immutable.{Option => GOption}
import jsky.app.ot.OT
import jsky.app.ot.tpe._
import scala.collection.JavaConverters._
import scala.collection.immutable.HashMap
import scala.concurrent.Await
import scala.swing.Swing
import scala.util.{Try, Failure, Success}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import scalaz._, Scalaz._


object BagsManager {
  private val LOG: Logger = Logger.getLogger(getClass.getName)

  private case class BagsTask(observation: ISPObservation, curCtx: Option[ObsContext], nextCtx: Option[ObsContext]) {
    def obsKey: SPNodeKey =
      observation.getNodeKey

    // The state to move to when processing the task.
    def nextTask: Option[BagsTask] =
      nextCtx.map(_ => BagsTask(observation, nextCtx, None))

    def isDone: Boolean =
      nextCtx.isEmpty

    def newContext(ctxOpt: Option[ObsContext]): BagsTask =
      BagsTask(observation, curCtx, ctxOpt)

    def performLookup(bagsThread: BagsThread): Unit = {
      val bagsIdMsg = s"BAGS lookup on thread=${bagsThread.id} for observation=${observation.getObservationID}"

      def lookup[S,T](optExtract: S => Option[T])(worker: (TpeContext, S) => Unit)(results: Try[S]): Unit = {
        results match {
          case Success(selOpt) =>
            LOG.info(s"$bagsIdMsg successful. Results=${optExtract(selOpt) ? "Yes" | "No"}.")
            if (ObservationStatus.computeFor(observation) != ObservationStatus.OBSERVED) {
              Swing.onEDT {
                muteObservation(observation)
                worker(TpeContext(observation), selOpt)
                unmuteObservation(observation)
              }
            }
            taskSuccess(this)

          // We don't want to print the stack trace if the host is simply unreachable.
          // This is reported only as a GenericError in a CatalogException, unfortunately.
          case Failure(CatalogException((e: GenericError) :: _)) =>
            LOG.warning(s"$bagsIdMsg failed: ${e.msg}")
            taskFail(this)

          // If we timed out, we don't want to delay.
          case Failure(ex: TimeoutException) =>
            LOG.warning(s"$bagsIdMsg failed: ${ex.getMessage}")
            taskFail(this, timedOut = true)

          // For all other exceptions, print the full stack trace.
          case Failure(ex) =>
            LOG.log(Level.WARNING, s"$bagsIdMsg} failed.", ex)
            taskFail(this)
        }
      }


      LOG.info(s"Performing $bagsIdMsg.")
      curCtx.foreach { obsCtx =>
        AgsRegistrar.currentStrategy(obsCtx).foreach { strategy =>
          if (GuideStarSupport.hasGemsComponent(observation)) {
            lookup((x: GOption[GemsGuideStars]) => x.asScalaOpt)(GemsGuideStarWorker.applyResults(_, _, true))(Try(GemsGuideStarWorker.findGuideStars(obsCtx)))
          } else {
            // This future always completes, either with results or timeout of 30s, so we wait for it with a long timeout
            // to let it do its thing.
            val fut = strategy.select(obsCtx, OT.getMagnitudeTable)
            val futDone = Await.ready(fut, LookupTimeout)
            futDone.onComplete(lookup(identity[Option[AgsStrategy.Selection]])(GuideStarWorker.applyResults))
          }
        }
      }
    }
  }

  private case class BagsThread(id: Int) extends Thread {
    setPriority(Thread.NORM_PRIORITY - 1)

    override def run() {
      // Get the next task to run. This blocks on the queue.
      val task = nextTask()

      // Execute the task.
      task.foreach(_.performLookup(this))

      // Resubmit self for execution.
      executor.submit(this)
    }
  }

  // A timeout for catalog searches that guarantees they will complete.
  private val LookupTimeout = 1.hour

  // If a task fails due to timeout, a thread takes a brief sleep before requeueing to allow sanity to restore. In ms.
  private val RequeueDelay = 5000

  // We use an executor to allow for a certain number of threads to run, which are created and fixed as per below.
  private val NumThreads = math.max(1, Runtime.getRuntime.availableProcessors()-1)
  private val executor = Executors.newFixedThreadPool(NumThreads)

  // Tasks to lookup are managed in two forms: a blocking FIFO queue contains a list of observations by node key
  // waiting for threads to perform lookups for them, and these node keys are mapped to acutal tasks to perform
  // via the map.
  private val taskQueue = new LinkedBlockingQueue[SPNodeKey]
  private var taskMap = new HashMap[SPNodeKey, BagsTask]

  // The programs we are actively managing. This is to prevent threads who fail from adding their observations
  // back into the queue when the program is no longer active.
  private var activePrograms = Set[SPNodeKey]()

  // Create all of the threads and submit for execution.
  List.tabulate(NumThreads)(i => new BagsThread(i)).foreach(executor.submit)


  // Initial registration of program.
  def registerProgram(prog: ISPProgram) = {
    Option(prog).foreach { p =>
      activePrograms.synchronized(activePrograms += p.getNodeKey)
      p.addStructureChangeListener(structurePropertyChangeListener)
      p.addCompositeChangeListener(compositePropertyChangeListener)

      // Only queue up observations that do not have an auto guide star.
      p.getAllObservations.asScala.foreach { obs =>
        val obsCtx = ObsContext.create(obs)
        obsCtx.asScalaOpt.foreach { ctx =>
          if (ctx.getTargets.getGuideEnvironment.getPrimaryReferencedGuiders.isEmpty ||
            ctx.getTargets.getGuideEnvironment.getPrimaryReferencedGuiders.asScala.exists(gp => {
              ctx.getTargets.getPrimaryGuideProbeTargets(gp).asScalaOpt.forall(_.getBagsTarget.isEmpty)})) {
            updateObservation(obs)
          }
        }
      }
    }
  }

  // Unregister a program.
  def unregisterProgram(prog: ISPProgram) = {
    Option(prog).foreach { p =>
      p.getAllObservations.asScala.foreach(removeObservation)
      p.removeCompositeChangeListener(compositePropertyChangeListener)
      p.removeStructureChangeListener(structurePropertyChangeListener)
      activePrograms.synchronized(activePrograms -= p.getNodeKey)
    }
  }

  // Mute an observation from firing PropertyChangeEvents to BAGS while updating BAGS guide stars.
  private def muteObservation(obs: ISPObservation) = {
    obs.getProgram.removeCompositeChangeListener(compositePropertyChangeListener)
    obs.getProgram.removeStructureChangeListener(structurePropertyChangeListener)
  }

  // Unmute an observation from firing PropertyChangeEvents to BAGS while updating BAGS guide stars.
  private def unmuteObservation(obs: ISPObservation) = {
    obs.getProgram.addStructureChangeListener(structurePropertyChangeListener)
    obs.getProgram.addCompositeChangeListener(compositePropertyChangeListener)
  }

  // Retrieve the next task to perform and set it up, moving it into the processing phase by assigning
  // curCtx to nextCtx.
  private def nextTask(): Option[BagsTask] = {
    val obsKey = taskQueue.take()
    taskMap.synchronized {
      val currTask = taskMap.get(obsKey)
      val tmpTask = currTask.flatMap(_.nextTask)
      tmpTask.foreach(t => taskMap += ((t.obsKey, t)))
      tmpTask
    }
  }

  // If a BAGS task succeeded, process it accordingly. This can comprise one of two things:
  // 1. If the task is marked as done, remove it from the map.
  // 2. If the task is marked as not done, requeue it.
  private def taskSuccess(task: BagsTask): Unit = {
    val obsKey = task.obsKey
    // Check to see if the task is marked as done, and if so, remove.
    val doneWithTask = taskMap.synchronized {
      taskMap.get(obsKey).fold(true) { t =>
        val done = t.isDone
        if (done) taskMap -= obsKey
        done
      }
    }

    // If a task is not complete and the program is still active, we add it back.
    if (!doneWithTask) {
      val isActiveProgram = activePrograms.synchronized(activePrograms.contains(task.observation.getProgramKey))
      if (isActiveProgram) {
        taskQueue.synchronized {
          if (!taskQueue.contains(obsKey))
            taskQueue.add(obsKey)
        }
      }
    }
  }

  // Called when a BAGS task failed to be processed properly, in which case, we must requeue it.
  private def taskFail(task: BagsTask, timedOut: Boolean = false): Unit = {
    val obsKey = task.obsKey

    val isActiveProgram = activePrograms.synchronized(activePrograms.contains(task.observation.getProgramKey))
    if (isActiveProgram) {
      // We only need to do something if the task is not already in the queue.
      // Check to see if the taskQueue contains the key.
      // If the key is added to the taskQueue in the interim, this is fine.
      // It will force another lookup, but that should not be problematic.
      val containsKey = taskQueue.synchronized(taskQueue.contains(obsKey))

      if (!containsKey) {
        // If the task has no next context, revert it back to the unprocessed state.
        // Otherwise, simply process it as is.
        val newTask = task match {
          case BagsTask(obs, Some(ctx), None) => BagsTask(obs, None, Some(ctx))
          case t => t
        }
        taskMap.synchronized(taskMap += ((obsKey, newTask)))
        taskQueue.synchronized(taskQueue.add(obsKey))
      }

      // Pause to give things a chance to right themselves.
      if (!timedOut)
        Thread.sleep(RequeueDelay)
    }
  }

  // Given an observation, either insert it in all data structures or update the existing entry for it.
  private def updateObservation(obs: ISPObservation) =
    Option(obs).foreach { ispObs =>
      val obsKey = ispObs.getNodeKey
      val ctxOpt = ObsContext.create(ispObs).asScalaOpt

      // Update the task map and the task queue.
      taskMap.synchronized {
        val task = taskMap.get(obsKey) match {
          case Some(t) => t.newContext(ctxOpt)
          case None    => BagsTask(ispObs, None, ctxOpt)
        }
        taskMap += ((obsKey, task))
      }
      taskQueue.synchronized {
        if (!taskQueue.contains(obsKey))
          taskQueue.add(obsKey)
      }
    }

  // Remove a given observation from all data structures.
  private def removeObservation(obs: ISPObservation) = {
    Option(obs).foreach { ispObs =>
      val obsKey = ispObs.getNodeKey
      taskQueue.synchronized(taskQueue.remove(obsKey))
      taskMap.synchronized(taskMap -= obsKey)
    }
  }

  // Check two target environments to see if the BAGS targets match exactly between them.
  def bagsTargetsMatch(oldEnv: TargetEnvironment, newEnv: TargetEnvironment): Boolean =
    oldEnv.getGuideEnvironment.getPrimaryReferencedGuiders.equals(newEnv.getGuideEnvironment.getPrimaryReferencedGuiders) &&
      oldEnv.getGuideEnvironment.getPrimaryReferencedGuiders.asScala.forall { gp =>
        oldEnv.getPrimaryGuideProbeTargets(gp).asScalaOpt.forall { oldGpt =>
          val oldBags = oldGpt.getBagsTarget.asScalaOpt
          val newBags = newEnv.getPrimaryGuideProbeTargets(gp).asScalaOpt.flatMap(_.getBagsTarget.asScalaOpt)
          (oldBags.isEmpty && newBags.isEmpty) || oldBags.exists(oldTarget => newBags.exists(_.getTarget.equals(oldTarget.getTarget)))
        }
      }

  private val compositePropertyChangeListener = new PropertyChangeListener {
    override def propertyChange(evt: PropertyChangeEvent): Unit = evt.getSource match {
      case node: ISPNode => updateObservation(node.getContextObservation)
      case _             => // Ignore
    }
  }

  private val structurePropertyChangeListener = new PropertyChangeListener {
    override def propertyChange(evt: PropertyChangeEvent): Unit = evt.getSource match {
      case cont: ISPObservationContainer => cont.getAllObservations.asScala.foreach(updateObservation)
      case _                             => // Ignore
    }
  }
}
