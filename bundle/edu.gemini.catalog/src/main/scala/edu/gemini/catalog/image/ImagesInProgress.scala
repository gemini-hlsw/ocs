package edu.gemini.catalog.image

import java.io.File

import scalaz._
import Scalaz._
import scalaz.concurrent.Task

case class ImagesInProgress (images: List[ImageSearchQuery]) {
  def +(i: ImageSearchQuery) = copy(i :: images)
  def -(i: ImageSearchQuery) = copy(images.filterNot(_ == i))
}

object ImagesInProgress  {
  val zero = ImagesInProgress (Nil)
  implicit val equals = Equal.equalA[ImagesInProgress ]

  private val cacheRef = TaskRef.newTaskRef[ImagesInProgress](ImagesInProgress.zero).unsafePerformSync

  def add(i: ImageSearchQuery): Task[ImagesInProgress] = cacheRef.modify(_ + i) *> cacheRef.get

  def remove(i: ImageSearchQuery): Task[ImagesInProgress] = cacheRef.modify(_ - i) *> cacheRef.get

  /**
    * Find if the search query is in the cache
    */
  def contains(file: ImageSearchQuery): Task[Boolean] =
    cacheRef.get.map(_.images.contains(file))

}
