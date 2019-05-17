package edu.gemini.phase2.template.factory.impl.flamingos2

import edu.gemini.phase2.template.factory.impl.{TemplateDb, Maybe}
import edu.gemini.pot.sp.{ISPObservation, ISPGroup}
import edu.gemini.spModel.gemini.flamingos2.blueprint.SpFlamingos2BlueprintMos

import scala.collection.JavaConverters._


final case class Flamingos2Mos(blueprint:SpFlamingos2BlueprintMos) extends Flamingos2Base[SpFlamingos2BlueprintMos] {

//  IF PRE-IMAGING REQUIRED = YES
//      INCLUDE {21}
//
//  INCLUDE {29}                           # Mask daytime image
//
//  INCLUDE {22,23}                           # Telluric std
//  INCLUDE {24,25,28}                           # Science
//  INCLUDE {26,27}                           # Telluric std
//
//  FOR {23,25,27,28}:                           # Science and Tellurics
//      SET DISPERSER FROM PI
//          Put FILTERS from PI into F2 ITERATOR
//
//  FOR {21,22,23,24,25,26,27,28,29}:
//      SET CONDITIONS FROM PI
//
//  FOR {24,25,28,29}:                              # MOS science
//      SET "Custom MDF" = G(N/S)YYYYS(Q/C/DD/SV/LP/FT)XXX-NN
//          where:
//          (N/S) is the site
//          YYYYS is the semester, e.g. 2015A
//          (Q/C/DD/SV/LP/FT) is the program type
//          XXX is the program number, e.g. 001, or 012, or 123
//          NN should be the string "NN" since the mask number is unknown

  def preImaging: Option[Int] =
    if (blueprint.preImaging) Some(21) else None

  override val targetGroup: Seq[Int] =
    preImaging.foldRight(Seq(29,22,23,24,25,28,26,27)) { _ +: _ }

  override val baselineFolder: Seq[Int] =
    Seq.empty

  override val notes: Seq[String] =
    Seq(
      "F2 MOS Notes",
      "Use the same PA for the MOS science target and telluric",
      "Detector readout modes",
      "Libraries",
      "MOS Arcs and flats",
      "MOS slits: only use slit widths of 4 pixels (0.72 arcsec) or larger. Slit length no less than 5 arcsec."
    )

  val scienceAndTellurics: Seq[Int] =
    Seq(23,25,27,28)

  override def initialize(grp:ISPGroup, db:TemplateDb): Maybe[Unit] =
    forObservations(grp, scienceAndTellurics, forScienceAndTellurics)

  /**
    REL-3661 This will (almost) implement the feature of setting the custom
    mask name from the program id.  The problem is that the program id is the
    program id of the library program here (F2-BP) not the resulting science
    program.  To fix it we need to pass the eventual program id down into
    the `initialize` method. Basically add an SPProgramID to the parameter list
    and bubble up as necessary.  I think there is one case where we won't have
    it (TemplateServlet -- so it may need to be an argument to the servlet there)

  val mosScience: Seq[Int] =
    Seq(24,25,28,29)

  override def initialize(grp:ISPGroup, db:TemplateDb): Maybe[Unit] =
    for {
      _ <- forObservations(grp, scienceAndTellurics, forScienceAndTellurics).right
      _ <- forObservations(grp, mosScience,          forMosScience         ).right
    } yield ()

  def forScienceAndTellurics(obs:ISPObservation): Maybe[Unit] =
    for {
      _ <- obs.setDisperser(blueprint.disperser).right
      _ <- obs.setFilters(blueprint.filters.asScala).right
    } yield ()

  def forMosScience(obs: ISPObservation): Maybe[Unit] =
    obs.ed.updateInstrument { f2 =>
      // Compute the mask name, if there is a program id.
      val maskName = Option(obs.getProgramID).map { id =>
        s"${id.toString.replaceAll("-", "")}-NN"
      }.toRight("Missing program id.")

      // Update the custom mask field in f2, if possible.
      maskName.right.foreach(f2.setFpuCustomMask)
    }
  */
}
