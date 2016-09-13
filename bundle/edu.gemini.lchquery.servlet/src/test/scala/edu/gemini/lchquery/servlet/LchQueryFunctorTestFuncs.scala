package edu.gemini.lchquery.servlet

import edu.gemini.pot.sp.{ISPObservation, ISPProgram}

import scala.collection.mutable.ListBuffer

object LchQueryFunctorTestFuncs {
  val obsQuery = LchQueryFunctor.QueryType.ObservationQuery

  def progParamsToList(programSemester: String,
                       programTitle: String,
                       programReference: String,
                       programActive: String,
                       programCompleted: String,
                       programNotifyPi: String,
                       programRollover: String): List[(LchQueryParam[ISPProgram],String)] = {
    val lb = ListBuffer[(LchQueryParam[ISPProgram],String)]()
    Option(programSemester).foreach(s => lb += ((LchQueryParam.ProgramSemesterParam, s)))
    Option(programTitle).foreach(s => lb += ((LchQueryParam.ProgramTitleParam, s)))
    Option(programReference).foreach(s => lb += ((LchQueryParam.ProgramReferenceParam, s)))
    Option(programActive).foreach(s => lb += ((LchQueryParam.ProgramActiveParam, s)))
    Option(programCompleted).foreach(s => lb += ((LchQueryParam.ProgramCompletedParam, s)))
    Option(programNotifyPi).foreach(s => lb += ((LchQueryParam.ProgramNotifyPIParam, s)))
    Option(programRollover).foreach(s => lb += ((LchQueryParam.ProgramRolloverParam, s)))
    lb.toList
  }

  def obsParamsToList(observationTooStatus: String,
                      observationName: String,
                      observationStatus: String,
                      observationInstrument: String,
                      observationAo: String,
                      observationClass: String): List[(LchQueryParam[ISPObservation],String)] = {
    val lb = ListBuffer[(LchQueryParam[ISPObservation],String)]()
    Option(observationTooStatus).foreach(s => lb += ((LchQueryParam.ObservationTOOStatusParam, s)))
    Option(observationName).foreach(s => lb += ((LchQueryParam.ObservationNameParam, s)))
    Option(observationStatus).foreach(s => lb += ((LchQueryParam.ObservationStatusParam, s)))
    Option(observationInstrument).foreach(s => lb += ((LchQueryParam.ObservationInstrumentParam, s)))
    Option(observationAo).foreach(s => lb += ((LchQueryParam.ObservationAOParam, s)))
    Option(observationClass).foreach(s => lb += ((LchQueryParam.ObservationClassParam, s)))
    lb.toList
  }
}