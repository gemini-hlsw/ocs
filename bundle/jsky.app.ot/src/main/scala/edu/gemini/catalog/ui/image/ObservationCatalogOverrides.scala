package edu.gemini.catalog.ui.image

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.UUID

import argonaut.Argonaut._
import argonaut._
import edu.gemini.catalog.image.ImageCatalog
import edu.gemini.pot.sp.SPNodeKey
import edu.gemini.spModel.core.Wavelength
import jsky.util.Preferences

import scalaz._
import Scalaz._
import scalaz.concurrent.Task

/**
  * Handles saving/reading overrides of observation catalogues
  * Basically it keeps a map of observations to catalogue backed up on a file
  */
object ObservationCatalogOverrides {
  // TODO Convert these to Task
  private val preferencesDir = Preferences.getPreferences.getDir
  private val overridesFile = new File(preferencesDir, "catalogOverrides.json")

  case class CatalogOverride(key: SPNodeKey, catalog: ImageCatalog)

  object CatalogOverride {

    // Argonaut codec
    implicit def CatalogOverrideCodecJson: CodecJson[CatalogOverride] =
      CodecJson(
        (p: CatalogOverride) =>
          ("catalog" := p.catalog.id) ->:
          ("uuid" := p.key.uuid.toString) ->:
          jEmptyObject,
        cur =>
          (for {
            uuid <- (cur --\ "uuid").as[String]
            id   <- (cur --\ "catalog").as[String]
          } yield ImageCatalog.byName(id).map((_, UUID.fromString(uuid)))).flatMap {
            case Some((c, u)) => DecodeResult.ok(CatalogOverride(new SPNodeKey(u), c))
            case None => DecodeResult.fail("Unknown catalog id", cur.history)
          }
      )
  }

  case class Overrides(overrides: List[CatalogOverride]) {
    /**
      * Finds the catalog for the key or goes to the default user catalog
      */
    def obsCatalog(key: SPNodeKey): Option[ImageCatalog] = overrides.find(_.key == key).map(_.catalog)
  }

  object Overrides {
    var zero = Overrides(Nil)

    implicit def OverridesCodecJson: CodecJson[Overrides] =
      casecodec1(Overrides.apply, Overrides.unapply)("overrides")
  }

  // Try to read or use a default if any errors are found
  private def readOverrides: Overrides = \/.fromTryCatchNonFatal {
        val lines = new String(Files.readAllBytes(overridesFile.toPath), StandardCharsets.UTF_8)
        Parse.decodeOr[Overrides, Overrides](lines, identity, Overrides.zero)
      }.getOrElse(Overrides.zero)

  def catalogFor(key: SPNodeKey, wavelength: Option[Wavelength]): Task[ImageCatalog] = {
    this.synchronized {
      ImageCatalog.preferences().map { p => readOverrides.obsCatalog(key).getOrElse(ImageCatalog.catalogForWavelength(wavelength))}
    }
  }

  def storeOverride(key: SPNodeKey, c: ImageCatalog): Task[Unit] = Task.delay {
    this.synchronized {
      def writeOverrides(overrides: Overrides) =
        \/.fromTryCatchNonFatal {
          Files.write(overridesFile.toPath, overrides.asJson.spaces2.getBytes(StandardCharsets.UTF_8))
        }

      for {
        old         <- \/.right(readOverrides)
        newOverrides = old.copy(overrides = CatalogOverride(key, c) :: old.overrides.filter(_.key != key))
        _           <- writeOverrides(newOverrides)
      } yield ()
    }
  }
}
