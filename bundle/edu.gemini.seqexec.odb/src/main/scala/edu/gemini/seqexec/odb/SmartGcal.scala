package edu.gemini.seqexec.odb

import edu.gemini.spModel.core.Peer
import edu.gemini.spModel.gemini.calunit.smartgcal.{CalibrationProviderHolder, CalibrationProvider, CalibrationRepository}
import edu.gemini.spModel.smartgcal.UpdatableCalibrationRepository
import edu.gemini.spModel.smartgcal.provider.CalibrationProviderImpl
import edu.gemini.spModel.smartgcal.repository.{CalibrationUpdater, CalibrationFileCache, CalibrationRemoteRepository}

import java.nio.file.Path
import java.time.Duration

object SmartGcal {
  def initialize(peer: Peer, dir: Path): TrySeq[Unit] =
    catchingAll {
      val service    = new CalibrationRemoteRepository(peer.host, peer.port)
      val cachedRepo = new CalibrationFileCache(dir.toFile)
      val provider   = new CalibrationProviderImpl(cachedRepo)
      CalibrationProviderHolder.setProvider(provider)
      CalibrationUpdater.instance.addListener(provider)
      CalibrationUpdater.instance.start(cachedRepo, service, Duration.ofHours(1).toMillis)
    }
}
