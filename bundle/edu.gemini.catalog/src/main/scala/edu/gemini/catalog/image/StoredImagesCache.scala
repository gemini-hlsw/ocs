package edu.gemini.catalog.image

import edu.gemini.spModel.core.Coordinates

import scalaz._
import Scalaz._
import scalaz.concurrent.Task

case class StoredImages(images: List[ImageEntry]) {
  def +(i: ImageEntry) = copy(i :: images)
  def -(i: ImageEntry) = copy(images.filterNot(_ === i))

  def find(query: ImageSearchQuery): Option[ImageEntry] = images.find(_.query == query)

  def findNearby(query: ImageSearchQuery): Option[ImageEntry] =
    images.find(q => q.query === query || (q.query.catalog === query.catalog && q.query.isNearby(query)))
}

object StoredImages {
  val zero = StoredImages(Nil)
  implicit val equals = Equal.equalA[StoredImages]
  implicit val monoid = Monoid.instance[StoredImages]((a, b) => StoredImages(a.images ++ b.images), zero)
}

/**
  * In memory cache of images on disk
  */
object StoredImagesCache {
  private val cacheRef = TaskRef.newTaskRef[StoredImages](StoredImages.zero).unsafePerformSync

  def add(i: ImageEntry): Task[StoredImages] = cacheRef.modify(_ + i) *> cacheRef.get

  def remove(i: ImageEntry): Task[StoredImages] = cacheRef.modify(_ - i) *> cacheRef.get

  def get: Task[StoredImages] = cacheRef.get

  def clean: Task[StoredImages] = cacheRef.modify(_ => StoredImages.zero) *> cacheRef.get

  /**
    * Find if the search query is in the cache
    * Find allows for nearby images to be reused
    */
  def find(query: ImageSearchQuery): Task[Option[ImageEntry]] =
    cacheRef.get.map(_.findNearby(query))
}
