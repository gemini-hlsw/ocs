package jsky.app.ot.gemini.editor.targetComponent.details2

import edu.gemini.pot.sp.ISPFactory
import edu.gemini.pot.spdb.DBLocalDatabase
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.shared.util.immutable.{ None => GNone }

import scala.swing.{Component, MainFrame, Frame, SimpleSwingApplication}

/**
  * Created by rnorris on 1/15/16.
  */
object TestFrame extends SimpleSwingApplication {

  val tdp = new TargetDetailPanel
  val odb = DBLocalDatabase.createTransient()

  def top: Frame = new MainFrame {
    title = "Target Editor Test UI"
    contents = Component.wrap(tdp)
  }

  val prog = odb.getFactory.createProgram(null, SPProgramID.toProgramID("GS-2016-B-Q1"))

  tdp.edit(GNone.instance(), new SPTarget(), prog)

}
