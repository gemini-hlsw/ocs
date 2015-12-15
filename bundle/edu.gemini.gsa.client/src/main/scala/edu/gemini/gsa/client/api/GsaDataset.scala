package edu.gemini.gsa.client.api

import edu.gemini.spModel.core.Coordinates

/**
 * Relevant information returned for each dataset by the GSA query.
 */
case class GsaDataset(
  ref: GsaDatasetReference,
  coordinates: GsaDatasetTarget,
  instrument: String,
  time: GsaDatasetTime,
  detail: GsaDatasetDetail)

case class GsaDatasetTarget(name: String, coordinates: Coordinates)

case class GsaDatasetReference(id: String, filename: String, progId: String)
case class GsaDatasetTime(obsTime: Long, releaseTime: Long)

case class GsaDatasetDetail(
  integregationMs: Option[Long],
  filters: String,
  wavelengthMicrons: Option[Double])

