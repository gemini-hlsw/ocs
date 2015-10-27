package edu.gemini.phase2.template.factory.impl.phoenix

import edu.gemini.phase2.template.factory.impl.{TemplateDsl2, GroupInitializer}
import edu.gemini.spModel.gemini.phoenix.{PhoenixParams, SeqConfigPhoenix, InstPhoenix}
import edu.gemini.spModel.gemini.phoenix.blueprint.SpPhoenixBlueprint

/*
Instrument : PHOENIX
Blueprints : PHOENIX_BP.xml
Version July 6, 2015 - updated by Bryan M

Observations identified by LibraryIDs indicated with {}.

Template library:
{1} Science observation
{2} Telluric observation
{3} Dark observation
{4} Flat observation


INCLUDE {1} {2} {3} {4} IN target-specific Scheduling Group (in order)
    INCLUDE the notes "How to use the observations in this folder" and "Darks, Flats and Arcs" IN the target-specific Scheduling Group
    SET FOCAL PLANE MASK from Phase-I (in all observations)
    SET FILTER from Phase-I (in all observations)
    SET the Exposure Time for the science observation (from {1}) based on the filter:
        J*,H*,K* -> 900
        L* -> 120
        M* -> 30
    SET the number of Coadds based on the filter
        J*,H*,K* -> 1
        L* -> 3
        M* -> 4
*/
case class Phoenix(blueprint: SpPhoenixBlueprint) extends PhoenixBase {

  val filterGroup = PhoenixFilterGroup.forFilter(blueprint.filter)

  include(1, 2, 3, 4) in TargetGroup
  addNote("How to use the observations in this folder", "Darks, Flats and Arcs") in TargetGroup
  forGroup(TargetGroup)(setFilter(blueprint.filter), setFpu(blueprint.fpu))
  forObs(1)(
    setExposureTime(
      filterGroup match {
        case PhoenixFilterGroup.JHK => 900
        case PhoenixFilterGroup.L   => 120
        case PhoenixFilterGroup.M   => 30
      }
    ),
    setCoadds(
      filterGroup match {
        case PhoenixFilterGroup.JHK => 1
        case PhoenixFilterGroup.L   => 3
        case PhoenixFilterGroup.M   => 4
      }
    ))

}
