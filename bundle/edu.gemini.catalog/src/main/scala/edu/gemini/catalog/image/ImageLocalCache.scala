package edu.gemini.catalog.image

import scalaz._
import Scalaz._
import scalaz.concurrent.Task

case class StoredImages(images: List[ImageEntry]) {
  def +(i: ImageEntry) = copy(i :: images)
  def -(i: ImageEntry) = copy(images.filterNot(_ === i))

  def find(query: ImageSearchQuery): Option[ImageEntry] = images.find(_.query == query)
}

object StoredImages {
  val zero = StoredImages(Nil)
  implicit val equals = Equal.equalA[StoredImages]
  implicit val monoid = Monoid.instance[StoredImages]((a, b) => StoredImages(a.images ++ b.images), zero)
}

/**
  * In memory cache of images on disk
  */
object ImageLocalCache {
  val cacheRef = TaskRef.newTaskRef[StoredImages](StoredImages.zero).unsafePerformSync

  def add(i: ImageEntry): Task[Unit] = cacheRef.modify(_ + i)

  def remove(i: ImageEntry): Task[Unit] = cacheRef.modify(_ - i)

  def get: Task[StoredImages] = cacheRef.get

  /**
    * Find if the search query is in the cache
    * It will verify that the file actually exists
    */
  def find(query: ImageSearchQuery): Task[Option[ImageEntry]] = {
    cacheRef.get.map(_.find(query)).map(_.filter(_.file.exists()))
  }

}
