package edu.gemini.catalog.image

import java.time.Instant

import scalaz._
import Scalaz._
import scalaz.concurrent.Task

case class StoredImages(entries: List[(Instant, ImageEntry)]) {
  def images = entries.map(_._2)

  def +(i: ImageEntry) = copy((Instant.now, i) :: entries)
  def +(i: Instant, e: ImageEntry) = copy((i, e) :: entries)

  def -(i: ImageEntry) = copy(entries.filterNot(_._2 === i))

  def mark(i: ImageEntry) = copy(entries.collect {
    case x if x._2 === i => (Instant.now, x._2)
    case x               => x
  })

  def sortedByAccess: List[ImageEntry] = entries.sortBy(_._1).map(_._2).reverse

  def findNearby(query: ImageSearchQuery): Option[ImageEntry] =
    entries.find(q => q._2.query === query || (q._2.query.catalog === query.catalog && q._2.query.isNearby(query))).map(_._2)
}

object StoredImages {
  val zero = StoredImages(Nil)
  implicit val equals = Equal.equalA[StoredImages]
  implicit val monoid = Monoid.instance[StoredImages]((a, b) => StoredImages(a.entries ++ b.entries), zero)
}

/**
  * In memory cache of images on disk
  */
object StoredImagesCache {
  private val cacheRef = TaskRef.newTaskRef[StoredImages](StoredImages.zero).unsafePerformSync

  def add(i: ImageEntry): Task[StoredImages] = cacheRef.modify(_ + i) *> cacheRef.get

  def addAt(instant: Instant, i: ImageEntry): Task[StoredImages] = cacheRef.modify(_ + (instant, i)) *> cacheRef.get

  def markAsUsed(i: ImageEntry): Task[StoredImages] = cacheRef.modify(_.mark(i)) *> cacheRef.get

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
