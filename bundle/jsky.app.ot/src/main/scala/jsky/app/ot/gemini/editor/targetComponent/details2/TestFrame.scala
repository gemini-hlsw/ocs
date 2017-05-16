package jsky.app.ot.gemini.editor.targetComponent.details2

import java.awt.{Color, BorderLayout}
import javax.swing.{BorderFactory, WindowConstants, JFrame}

import edu.gemini.pot.spdb.DBLocalDatabase
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.spModel.gemini.gmos.InstGmosSouth
import edu.gemini.spModel.gemini.obscomp.SPProgram
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.obsComp.TargetObsComp
import edu.gemini.spModel.too.TooType

import scala.collection.JavaConverters._
import scala.swing.{Component, MainFrame, Frame, SimpleSwingApplication}

import scalaz._, Scalaz._

object TestFrame extends App {

  // Set up the colors etc., to match the OT
  jsky.util.gui.Theme.install()

  val odb  = DBLocalDatabase.createTransient()
  val fact = odb.getFactory
  val prog = fact.createProgram(null, SPProgramID.toProgramID("GS-2016-B-Q1"))
      prog.setDataObject(new SPProgram <| (_.setTooType(TooType.rapid)))
  val obs  = fact.createObservation(prog, null) <| prog.addObservation
  val gmos = fact.createObsComponent(prog, InstGmosSouth.SP_TYPE, null) <| obs.addObsComponent
  val toc  = obs.getObsComponents.asScala.find(_.getType == TargetObsComp.SP_TYPE).get
  val tenv = toc.getDataObject.asInstanceOf[TargetObsComp].getTargetEnvironment
  val ctx  = ObsContext.create(obs)
  val tdp  = new TargetDetailPanel

  val top = new JFrame {
    setTitle("Target Editor Test UI")
    getContentPane.setLayout(new BorderLayout)
    getContentPane.add(tdp, BorderLayout.NORTH)
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
  }

  tdp.edit(ctx, ctx.getValue.getTargets.getArbitraryTargetFromAsterism, toc)
  top.pack()
  top.setVisible(true)

}
