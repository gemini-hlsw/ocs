package edu.gemini.catalog.image

import edu.gemini.spModel.core.Coordinates

import scalaz._
import Scalaz._
import scalaz.concurrent.Task

protected case class ImagesInProgress(images: List[ImageSearchQuery]) {
  def +(i: ImageSearchQuery): ImagesInProgress = copy(i :: images)
  def -(i: ImageSearchQuery): ImagesInProgress = copy(images.filterNot(_ === i))
}

/**
  * Keeps track of images currently being downloaded or failed
  */
case class KnownImagesSets(inProgress: ImagesInProgress, failed: ImagesInProgress) {
  def start(i: ImageSearchQuery): KnownImagesSets     = copy(inProgress + i, failed - i)
  def completed(i: ImageSearchQuery): KnownImagesSets = copy(inProgress - i, failed - i)
  def failed(i: ImageSearchQuery): KnownImagesSets    = copy(inProgress - i, failed + i)
}

case class CataloguesInUse(inProgress: List[ImageCatalog], failed: List[ImageCatalog])

/**
  * Keep references to images being downloaded and images with failed downloads
  */
object KnownImagesSets {
  val zero = ImagesInProgress(Nil)

  private val imagesSets = TaskRef.newTaskRef[KnownImagesSets](KnownImagesSets(zero, zero)).unsafePerformSync

  /**
    * Mark image as in progress
    */
  def start(i: ImageSearchQuery): Task[KnownImagesSets] = imagesSets.mod(_.start(i)) *> imagesSets.get

  /**
    * Mark image as done
    */
  def completed(i: ImageSearchQuery): Task[KnownImagesSets] = imagesSets.mod(_.completed(i)) *> imagesSets.get

  /**
    * Mark image as failed
    */
  def failed(i: ImageSearchQuery): Task[KnownImagesSets] = imagesSets.mod(_.failed(i)) *> imagesSets.get

  /**
    * Find if the search query is in progress
    */
  def inProgress(query: ImageSearchQuery): Task[Boolean] =
    imagesSets.get.map(_.inProgress.images.contains(query))

  /**
    * Find the catalogues being searched or failed near the given coordinates
    */
  def cataloguesInUse(coordinates: Coordinates): Task[CataloguesInUse] = {
   val u = imagesSets.get.map(_.inProgress.images.filter(_.isNearby(coordinates)).map(_.catalog))
   val v = imagesSets.get.map(_.failed.images.filter(_.isNearby(coordinates)).map(_.catalog))
    u.tuple(v).map(Function.tupled(CataloguesInUse.apply))
  }
}
