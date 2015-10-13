package jsky.app.ot.ags

import java.beans.{PropertyChangeEvent, PropertyChangeListener}
import java.util.concurrent.{LinkedBlockingQueue, Executors}
import java.util.logging.Logger

import edu.gemini.ags.api.{AgsRegistrar, AgsStrategy}
import edu.gemini.pot.sp._
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.rich.shared.immutable._
import edu.gemini.spModel.target.env.TargetEnvironment
import edu.gemini.spModel.target.obsComp.TargetObsComp
import jsky.app.ot.OT
import jsky.app.ot.tpe._
import scala.collection.JavaConverters._
import scala.collection.immutable.HashMap
import scala.swing.Swing
import scala.util.{Try, Failure, Success}
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

    def performLookup(): Unit = {
      LOG.info(s"Performing BAGS lookup for observation=${observation.getObservationID}")

      def resultString[A](r: Option[A]): String =
        s"Results=${r ? "No" | "Yes"}."

      // We must handle GeMS separately from other strategies, and delegate the work to the GemsGuideStarWorker.
      def gemsLookup(obsCtx: ObsContext): Unit = {
        LOG.info(s"BAGS GeMS lookup for observation=${observation.getObservationID} starting...")
        Try { GemsGuideStarWorker.findGuideStars(obsCtx) } match {
          case Success(ggs) =>
            LOG.info(s"BAGS GeMS lookup for observation=${observation.getObservationID} successful. ${resultString(ggs.asScalaOpt)}")
            Swing.onEDT {
              muteObservation(observation)
              GemsGuideStarWorker.applyResults(TpeContext(observation), ggs, true)
              unmuteObservation(observation)
            }
            taskComplete(this, success = true)
          case Failure(ex) =>
            LOG.warning(s"BAGS GeMS lookup for observation=${observation.getObservationID} failed. Exception=$ex")
            taskComplete(this, success = false)
        }
      }

      // The mechanism to perform lookups for all other guide probes.
      def otherLookup(obsCtx: ObsContext, strategy: AgsStrategy): Unit = {
        val fut = strategy.select(obsCtx, OT.getMagnitudeTable)
        fut.onComplete {
          case Success(selOpt) =>
            LOG.info(s"BAGS lookup for observation=${observation.getObservationID} successful. ${resultString(selOpt)}")
            Swing.onEDT {
              muteObservation(observation)
              GuideStarWorker.applyResults(TpeContext(observation), selOpt)
              unmuteObservation(observation)
            }
            taskComplete(this, success = true)
          case Failure(ex) =>
            LOG.warning(s"BAGS lookup for observation=${observation.getObservationID} failed. Exception=$ex")
            taskComplete(this, success = false)
        }
      }


      curCtx.foreach { obsCtx =>
        AgsRegistrar.currentStrategy(obsCtx).foreach { strategy =>
          if (GuideStarSupport.hasGemsComponent(observation)) {
            gemsLookup(obsCtx)
          } else {
            otherLookup(obsCtx, strategy)
          }
        }
      }
    }
  }

  private class BagsThread(id: Int) extends Runnable {
    override def run() {
      // Get the next task to run. This blocks on the queue.
      val task = nextTask()

      // Execute the task.
      task.foreach(_.performLookup())

      // Resubmit self for execution.
      executor.submit(this)
    }
  }

  private val NumThreads = math.min(1, Runtime.getRuntime.availableProcessors())
  private val RequeueDelay = 3000
  private val executor = Executors.newFixedThreadPool(NumThreads)
  private var taskMap = new HashMap[SPNodeKey, BagsTask]
  private val taskQueue = new LinkedBlockingQueue[SPNodeKey]
  List.tabulate(NumThreads)(i => new BagsThread(i)).foreach(executor.submit)


  // Initial registration of program.
  def registerProgram(prog: ISPProgram) = {
    Option(prog).foreach { p =>
      p.addCompositeChangeListener(propertyChangeListener)

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
      p.getObservations.asScala.foreach(removeObservation)
      p.removeCompositeChangeListener(propertyChangeListener)
    }
  }

  // Mute an observation from firing PropertyChangeEvents to BAGS while updating BAGS guide stars.
  private def muteObservation(obs: ISPObservation) =
    obs.getProgram.removeCompositeChangeListener(propertyChangeListener)

  // Unmute an observation from firing PropertyChangeEvents to BAGS while updating BAGS guide stars.
  private def unmuteObservation(obs: ISPObservation) =
    obs.getProgram.addCompositeChangeListener(propertyChangeListener)

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

  // If a BAGS task failed to be processed properly, we should requeue it.
  private def taskComplete(task: BagsTask, success: Boolean) = {
    val obsKey = task.obsKey
    if (success) {
      // Check to see if the task is marked as done, and if so, remove.
      val doneWithTask = taskMap.synchronized {
        taskMap.get(obsKey).fold(true){t =>
          val done = t.isDone
          if (done) taskMap -= obsKey
          done
        }
      }
      if (!doneWithTask)
        taskQueue.synchronized {
          if (!taskQueue.contains(obsKey))
            taskQueue.add(obsKey)
        }
    }
    else {
      Thread.sleep(RequeueDelay)

      // We only need to do something if the task is not already in the queue.
      taskQueue.synchronized {
        if (!taskQueue.contains(obsKey)) {
          val newTask = task match {
            case BagsTask(obs, Some(ctx), None) => BagsTask(obs, None, Some(ctx))
            case t => t
          }
          taskMap.synchronized(taskMap += ((obsKey, newTask)))
          taskQueue.add(obsKey)
        }
      }
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

  private val propertyChangeListener = new PropertyChangeListener {
    override def propertyChange(evt: PropertyChangeEvent): Unit = {
      evt.getSource match {
        case obsComp: ISPObsComponent =>
          // Check to see if anything has changed.
          val sameEnv = evt.getOldValue.isInstanceOf[TargetObsComp] && evt.getNewValue.isInstanceOf[TargetObsComp] && {
            val oldEnv = evt.getOldValue.asInstanceOf[TargetObsComp].getTargetEnvironment
            val newEnv = evt.getNewValue.asInstanceOf[TargetObsComp].getTargetEnvironment

            // Same base, same primary guiders, same BAGS targets.
            oldEnv.getBase.getTarget.equals(newEnv.getBase.getTarget) && bagsTargetsMatch(oldEnv, newEnv)
          }
          if (!sameEnv)
            updateObservation(obsComp.getContextObservation)

        case prog: ISPProgram =>
          prog.getAllObservations.asScala.foreach(updateObservation)
        case group: ISPGroup =>
          group.getObservations.asScala.foreach(updateObservation)
        case node: ISPNode    =>
          updateObservation(node.getContextObservation)
        case _                =>
          // Ignore
      }
    }
  }
}
