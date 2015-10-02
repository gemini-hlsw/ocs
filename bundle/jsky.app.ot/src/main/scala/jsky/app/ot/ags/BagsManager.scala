package jsky.app.ot.ags

import java.beans.{PropertyChangeEvent, PropertyChangeListener}
import java.util.concurrent.{LinkedBlockingQueue, Executors}
import java.util.logging.Logger

import edu.gemini.ags.api.{AgsRegistrar, AgsStrategy}
import edu.gemini.pot.sp._
import edu.gemini.spModel.guide.GuideProbe
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.rich.shared.immutable._
import edu.gemini.spModel.target.env.{TargetEnvironment, GuideProbeTargets}
import edu.gemini.spModel.target.obsComp.TargetObsComp
import jsky.app.ot.OT
import jsky.app.ot.gemini.altair.Altair_WFS_Feature
import jsky.app.ot.gemini.inst.OIWFS_Feature
import jsky.app.ot.gemini.tpe.TpePWFSFeature
import jsky.app.ot.tpe.{GemsGuideStarWorker, GuideStarSupport, TpeContext, TpeManager}
import scala.collection.JavaConverters._
import scala.collection.immutable.HashMap
import scala.swing.Swing
import scala.util.{Try, Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global


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
        s"Results=${r.fold("No")(_ => "Yes")}."

      // We must handle GeMS separately from other strategies, and delegate the work to the GemsGuideStarWorker.
      def gemsLookup(obsCtx: ObsContext): Unit = {
        LOG.info(s"BAGS GeMS lookup for observation=${observation.getObservationID} starting...")
        val ggsTry = Try { GemsGuideStarWorker.findGuideStars(obsCtx) }
        LOG.info(s"BAGS GeMS lookup for observation=${observation.getObservationID} ${ggsTry.map(ggs => s"successful. " +
          resultString(ggs.asScalaOpt)).getOrElse("failed.")}")
        if (ggsTry.isFailure)
          LOG.warning(s"EXCEPTION=${ggsTry.failed.get}")
        ggsTry.foreach { ggs =>
          Swing.onEDT {
            muteObservation(observation)
            GemsGuideStarWorker.applyResults(TpeContext(observation), ggs, true)
            unmuteObservation(observation)
          }
        }
        taskComplete(this, success = ggsTry.isSuccess)
      }

      // The mechanism to perform lookups for all other guide probes.
      def otherLookup(obsCtx: ObsContext, strategy: AgsStrategy): Unit = {
        def showTpeFeatures(selOpt: Option[AgsStrategy.Selection]): Unit =
          selOpt.foreach { sel =>
            Option(TpeManager.get()).filter(_.isVisible).foreach { tpe =>
              sel.assignments.foreach { ass =>
                val clazz = ass.guideProbe.getType match {
                  case GuideProbe.Type.AOWFS => classOf[Altair_WFS_Feature]
                  case GuideProbe.Type.OIWFS => classOf[OIWFS_Feature]
                  case GuideProbe.Type.PWFS => classOf[TpePWFSFeature]
                }
                Option(tpe.getFeature(clazz)).foreach {
                  tpe.selectFeature
                }
              }
            }
          }

        def applySelection(selOpt: Option[AgsStrategy.Selection]): Unit = {
          // Make a new TargetEnvironment with the guide probe assignments.
          val ctx = TpeContext(observation)

          // Find out which guide probes previously had assignments, but no longer do.
          val oldEnv = ctx.targets.envOrDefault
          val allProbes = oldEnv.getGuideEnvironment.getReferencedGuiders.asScala.toSet
          val assignedProbes = selOpt.map(_.assignments.map(_.guideProbe)).toList.flatten
          val unassignedProbes = allProbes -- assignedProbes

          // Clear out the guide probes that no longer have a valid assignment.
          val clearedEnv = (oldEnv /: unassignedProbes) { (curEnv, gp) =>
            val oldGpt = curEnv.getPrimaryGuideProbeTargets(gp).asScalaOpt
            val newGpt = oldGpt.getOrElse(GuideProbeTargets.create(gp)).setBagsTarget(GuideProbeTargets.NO_TARGET)
            curEnv.putPrimaryGuideProbeTargets(newGpt)
          }

          // Apply the new selection.
          val newEnv = selOpt.fold(clearedEnv)(_.applyTo(clearedEnv))

          // Update the TargetEnvironment if it is different.
          if (!bagsTargetsMatch(oldEnv, newEnv)) {
            muteObservation(observation)
            ctx.targets.dataObject.foreach { targetComp =>
              targetComp.setTargetEnvironment(newEnv)
              ctx.targets.commit()

              // Update the position angle, if necessary.
              selOpt.foreach { sel =>
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
            unmuteObservation(observation)
          }
        }

        val fut = strategy.select(obsCtx, OT.getMagnitudeTable)
        fut.onComplete {
          case Success(selOpt) =>
            LOG.info(s"BAGS lookup for observation=${observation.getObservationID} successful. ${resultString(selOpt)}")
            Swing.onEDT {
              applySelection(selOpt)
              showTpeFeatures(selOpt)
            }
            taskComplete(this, success = true)
          case Failure(ex) =>
            LOG.warning(s"BAGS lookup for observation=${observation.getObservationID} failed.")
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

  private val NumThreads = 5
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
      p.getObservations.asScala.foreach { obs =>
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

        case prog: ISPProgram => prog.getObservations.asScala.foreach(updateObservation)
        case node: ISPNode    => updateObservation(node.getContextObservation)
        case _                => // Ignore
      }
    }
  }
}
