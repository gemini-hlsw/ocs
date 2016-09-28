package edu.gemini.catalog.image

import edu.gemini.spModel.core.Coordinates

import scalaz._
import Scalaz._
import scalaz.concurrent.Task

case class ImagesInProgress(images: List[ImageSearchQuery]) {
  def +(i: ImageSearchQuery): ImagesInProgress = copy(i :: images)
  def -(i: ImageSearchQuery): ImagesInProgress = copy(images.filterNot(_ === i))
}

case class CataloguesInUse(inProgress: List[ImageCatalog], failed: List[ImageCatalog])

object ImagesInProgress  {
  val zero = ImagesInProgress(Nil)
  implicit val equals: Equal[ImagesInProgress] = Equal.equalA[ImagesInProgress]

  // Contains the images being downloaded
  private val inUseRef = TaskRef.newTaskRef[ImagesInProgress](ImagesInProgress.zero).unsafePerformSync
  // Contains images that have failed
  private val failedRef = TaskRef.newTaskRef[ImagesInProgress](ImagesInProgress.zero).unsafePerformSync

  def start(i: ImageSearchQuery): Task[ImagesInProgress] = inUseRef.modify(_ + i) *> failedRef.modify(_ - i) *> inUseRef.get

  def completed(i: ImageSearchQuery): Task[ImagesInProgress] = inUseRef.modify(_ - i) *> failedRef.modify(_ - i) *> inUseRef.get

  def failed(i: ImageSearchQuery): Task[ImagesInProgress] = inUseRef.modify(_ - i) *> failedRef.modify(_ + i) *> failedRef.get

  /**
    * Find if the search query is in progress
    */
  def inProgress(query: ImageSearchQuery): Task[Boolean] =
    inUseRef.get.map(_.images.contains(query))

  /**
    * Find the catalogues being searched or failed near the given coordinates
    */
  def cataloguesInUse(coordinates: Coordinates): Task[CataloguesInUse] = {
   val u = inUseRef.get.map(_.images.filter(_.isNearby(coordinates)).map(_.catalog))
   val v = failedRef.get.map(_.images.filter(_.isNearby(coordinates)).map(_.catalog))
    u.tuple(v).map(Function.tupled(CataloguesInUse.apply))
  }

}
