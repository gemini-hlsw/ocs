package edu.gemini.catalog.ui.image

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.util.UUID

import argonaut.Argonaut._
import argonaut._
import edu.gemini.catalog.image.ImageCatalog
import edu.gemini.pot.sp.SPNodeKey
import edu.gemini.spModel.core.Wavelength
import jsky.util.Preferences

import scalaz._
import scalaz.concurrent.Task

/**
  * Handles saving/reading overrides of observation catalogues
  * Basically it keeps a map of observations to catalogue backed up on a file
  */
object ObservationCatalogOverrides {
  val OverridesFileName = "catalogOverrides.json"

  /**
    * Overrides the default catalog for the given key
    */
  case class CatalogOverride(key: SPNodeKey, catalog: ImageCatalog)

  object CatalogOverride {

    // Argonaut codec
    /** @group Typeclass Instances */
    implicit def CatalogOverrideCodecJson: CodecJson[CatalogOverride] =
      CodecJson(
        (p: CatalogOverride) =>
          ("catalog" := p.catalog.id.filePrefix) ->:
          ("uuid" := p.key.uuid.toString) ->:
          jEmptyObject,
        cur =>
          (for {
            uuid <- (cur --\ "uuid").as[String]
            id   <- (cur --\ "catalog").as[String]
          } yield ImageCatalog.byId(id).map((_, UUID.fromString(uuid)))).flatMap {
            case Some((c, u)) => DecodeResult.ok(CatalogOverride(new SPNodeKey(u), c))
            case None => DecodeResult.fail("Unknown catalog id", cur.history)
          }
      )
  }

  case class Overrides(overrides: List[CatalogOverride]) {
    def obsCatalog(key: SPNodeKey): Option[ImageCatalog] = overrides.find(_.key == key).map(_.catalog)
  }

  object Overrides {
    var zero = Overrides(Nil)

    // Argonaut codec
    /** @group Typeclass Instances */
    implicit def OverridesCodecJson: CodecJson[Overrides] =
      casecodec1(Overrides.apply, Overrides.unapply)("overrides")
  }

  /**
    * Find out what's the location of the overrides file
    */
  private def overridesFile: Task[Path] = Task.delay {
    val preferencesDir = Preferences.getPreferences.getDir.toPath
    preferencesDir.resolve(OverridesFileName)
  }

  /**
    * Try to read or use a default if any errors are found
    */
  private def readOverrides(overridesFile: Path): Overrides =
    \/.fromTryCatchNonFatal {
      val lines = new String(Files.readAllBytes(overridesFile), StandardCharsets.UTF_8)
      Parse.decodeOr[Overrides, Overrides](lines, identity, Overrides.zero)
    }.getOrElse(Overrides.zero)

  /**
    * Finds the catalog for the given node and wavelength
    */
  def catalogFor(key: SPNodeKey, wavelength: Option[Wavelength]): Task[ImageCatalog] = {
    this.synchronized {
      overridesFile.map(readOverrides(_).obsCatalog(key).getOrElse(ImageCatalog.catalogForWavelength(wavelength)))
    }
  }

  /**
    * Store the overridden catalog for a given node
    */
  def storeOverride(key: SPNodeKey, c: ImageCatalog): Task[Unit] = {
    def writeOverrides(overridesFile: Path, overrides: Overrides) = Task.delay {
      this.synchronized {
        \/.fromTryCatchNonFatal {
          Files.write(overridesFile, overrides.asJson.spaces2.getBytes(StandardCharsets.UTF_8))
        }
      }
    }

    for {
      overridesFile <- overridesFile
      old           <- Task.delay(readOverrides(overridesFile))
      newOverrides  = old.copy(overrides = CatalogOverride(key, c) :: old.overrides.filter(_.key != key))
      _             <- writeOverrides(overridesFile, newOverrides)
    } yield ()
  }
}
