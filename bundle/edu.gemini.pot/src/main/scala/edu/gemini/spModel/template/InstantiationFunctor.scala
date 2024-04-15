package edu.gemini.spModel.template

import com.sun.jmx.mbeanserver.MBeanInstantiator
import edu.gemini.pot.sp._
import edu.gemini.pot.spdb.DBAbstractFunctor
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.shared.util.immutable.ImOption
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
import edu.gemini.spModel.obs.ObsClassService
import edu.gemini.spModel.obs.SPObservation
import edu.gemini.spModel.obsclass.ObsClass
import edu.gemini.spModel.obscomp.SPGroup
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.Asterism
import edu.gemini.spModel.target.env.AsterismType
import edu.gemini.spModel.target.obsComp.TargetObsComp
import edu.gemini.spModel.util.AsterismEditUtil
import edu.gemini.spModel.util.DefaultSchedulingBlock

import scala.collection.immutable.Iterable
import scala.collection.immutable._
import scala.collection.JavaConverters._
import java.security.Principal
import java.util.ArrayList
import java.util.List
import java.util.Set
import java.util.logging.Level
import java.util.logging.Logger
import java.util.stream.IntStream
import edu.gemini.spModel.template.FunctorHelpers.addIfNotPresent

/**
 * Template instantiation functor that runs on a program, instantiating a set of TemplateGroup/TemplateParameters
 * pairs. The group and parameters need not come from the same program as that to which the functor is applied, nor
 * do the template parameters need to exist in the template group. We can tighten this down if needed.
 */
object InstantiationFunctor {
  val LOGGER: Logger = Logger.getLogger(classOf[InstantiationFunctor].getName)

  final class Instantiator(
    fact: ISPFactory,
    prog: ISPProgram,
    templateGroup: ISPTemplateGroup,
    siteQualityData: SPSiteQuality,
    targetData: SPTarget
  ) {

    def instantiate: ISPGroup = {
      val templateGroupData = templateGroup.getDataObject.asInstanceOf[TemplateGroup]
      val group = createGroup(templateGroupData)
      copyNotes(group)
      copyObservations(group)
      group
    }

    // Create a scheduling group based on the specified targetData
    private def createGroup(templateGroupData: TemplateGroup): ISPGroup = {
      val group     = fact.createGroup(prog, null)
      val groupData = group.getDataObject.asInstanceOf[SPGroup]
      groupData.setTitle(createGroupTitle(templateGroupData, targetData))
      groupData.setGroupType(templateGroupData.getGroupType)
      group.setDataObject(groupData)
      group
    }

    // Copy all notes from the template group into the target group
    private def copyNotes(group: ISPGroup): Unit =
      templateGroup.getObsComponents.asScala.foreach { oc =>
        group.addObsComponent(fact.createObsComponentCopy(prog, oc, false))
      }

    // Copy observations from the template group into the destination group
    private def copyObservations(group: ISPGroup): Unit =
      templateGroup.getAllObservations.asScala.foreach { templateObs =>

        // Clone the template obs
        val newObs     = fact.createObservationCopy(prog, templateObs, false)
        val newObsData = newObs.getDataObject.asInstanceOf[SPObservation]

        // Add a default scheduling block
        newObsData.setSchedulingBlock(ImOption.apply(DefaultSchedulingBlock.forProgram(prog)))

        // Save a pointer to the template
        newObsData.setOriginatingTemplate(templateObs.getNodeKey)
        newObs.setDataObject(newObsData)

        // Copy target/conds for relevant obs classes.  Note, if the
        // template obs has an explicitly defined target or conditions node,
        // it is not replaced with template values.
        val newObsClass = ObsClassService.lookupObsClass(newObs)
        if (newObsClass != ObsClass.DAY_CAL) copySiteQuality(newObs)
        if (newObsClass == ObsClass.SCIENCE || newObsClass == ObsClass.ACQ) copyTarget(newObs)

        group.addObservation(newObs)
      }

    // Copy the specified target into the specified observation
    def copyTarget(newObs: ISPObservation): Unit = {
      val comp    = fact.createObsComponent(prog, TargetObsComp.SP_TYPE, null)
      val toc     = comp.getDataObject.asInstanceOf[TargetObsComp]
      val astType = AsterismType.forObservation(newObs)
      val ast     = Asterism.fromTypeAndTemplateTarget(astType, targetData)
      toc.setTargetEnvironment(toc.getTargetEnvironment.setAsterism(ast))
      comp.setDataObject(toc)
      addIfNotPresent(newObs, comp)
      AsterismEditUtil.matchAsterismToInstrument(newObs)
    }

    // Copy the specified site quality into the specified observation
    def copySiteQuality(newObs: ISPObservation): Unit = {
      val comp = fact.createObsComponent(prog, SPSiteQuality.SP_TYPE, null)
      comp.setDataObject(siteQualityData)
      addIfNotPresent(newObs, comp)
    }

    // Get a group name from the given config, conditions, target triple
    def createGroupTitle(templateGroupData: TemplateGroup, targetData: SPTarget): String =
      s"${targetData.getName} - [${templateGroupData.getVersionToken}] ${templateGroupData.getTitle}"
  }
}

final class InstantiationFunctor extends DBAbstractFunctor {
  import InstantiationFunctor._

  var selection: ListMap[ISPTemplateGroup, ListSet[ISPTemplateParameters]] = ListMap.empty

  /**
   * Add a group/params pair to the set of instantiations to be performed.
   * @param group a template group
   * @param params a set of params
   */
  def add(group: ISPTemplateGroup, params: ISPTemplateParameters): Unit =
    selection = selection.updated(group, selection.getOrElse(group, ListSet.empty[ISPTemplateParameters]) + params)

  override def execute(db: IDBDatabaseService, node: ISPNode, principals: Set[Principal]): Unit = {
    try {
      val prog: ISPProgram = db.lookupProgram(node.getProgramKey)

      val newGroups = new ArrayList[ISPGroup]

      for {
        ps <- selection
        p  <- ps._2.toList.reverse
      } yield {
        val templateGroup          = ps._1
        val templateParametersData = p.getDataObject.asInstanceOf[TemplateParameters]
        val siteQualityData        = templateParametersData.getSiteQuality
        val targetData             = templateParametersData.getTarget
        val instantiator = new Instantiator(db.getFactory, prog, templateGroup, siteQualityData, targetData)
        newGroups.add(instantiator.instantiate)
      }

      // Update the group list all at once.  This way, only one event
      // is sent to the clients with all the changes.
      val allGroups = prog.getGroups
      allGroups.addAll(newGroups)
      prog.setGroups(allGroups)

    } catch {
      case e: Exception =>
        LOGGER.log(Level.WARNING, "Trouble during template instantiation.", e)
        setException(e)
    }
  }
}
