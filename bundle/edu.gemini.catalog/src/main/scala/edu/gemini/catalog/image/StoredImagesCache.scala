package edu.gemini.catalog.image

import java.io.File
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{FileVisitResult, Files, Path, SimpleFileVisitor}
import java.time.Instant

import jsky.util.Preferences

import scalaz._
import Scalaz._
import scalaz.concurrent.Task

/**
  * Keeps track of images in the file system and access time
  */
protected case class StoredImages(entries: List[(Instant, ImageInFile)]) {
  def images: List[ImageInFile] = entries.map(_._2)

  def +(i: ImageInFile): StoredImages = copy((Instant.now, i) :: entries)
  def +(i: Instant, e: ImageInFile): StoredImages = copy((i, e) :: entries)
  def -(i: ImageInFile): StoredImages = copy(entries.filterNot(_._2 === i))

  /**
    * Indicates the image was used, update the access time
    */
  def touch(i: ImageInFile): StoredImages = copy(entries.collect {
    case x if x._2 === i => (Instant.now, x._2)
    case x               => x
  })

  /**
    * Return imagessorted by access time
    */
  def sortedByAccess: List[ImageInFile] = entries.sortBy(_._1).map(_._2).reverse

  /**
    * Find images near the requested query
    */
  def findNearby(query: ImageSearchQuery): Option[ImageInFile] =
    entries.find(q => q._2.query === query || q._2.query.isNearby(query)).map(_._2)
}

object StoredImages {
  val zero = StoredImages(Nil)
  implicit val equals: Equal[StoredImages] = Equal.equalA[StoredImages]
}

/**
  * In memory cache of images on disk
  */
object StoredImagesCache {
  private val cacheRef = TaskRef.newTaskRef[StoredImages](StoredImages.zero).unsafePerformSync

  def add(i: ImageInFile): Task[StoredImages] = cacheRef.mod(_ + i) *> cacheRef.get

  def addAt(instant: Instant, i: ImageInFile): Task[StoredImages] = cacheRef.mod(_ + (instant, i)) *> cacheRef.get

  def markAsUsed(i: ImageInFile): Task[StoredImages] = cacheRef.mod(_.touch(i)) *> cacheRef.get

  def remove(i: ImageInFile): Task[StoredImages] = cacheRef.mod(_ - i) *> cacheRef.get

  def get: Task[StoredImages] = cacheRef.get

  def clean: Task[StoredImages] = cacheRef.mod(_ => StoredImages.zero) *> cacheRef.get

  /**
    * Find if the search query is in the cache
    * Find allows for nearby images to be reused
    */
  def find(query: ImageSearchQuery): Task[Option[ImageInFile]] =
    cacheRef.get.map(_.findNearby(query))
}

object ImageCacheOnDisk {

  /**
    * Method to prune the cache if we are using to much disk space
    */
  def pruneCache: Task[Unit] = Task.fork {
    // Remove files from the in memory cache and delete from drive
    def deleteOldFiles(files: List[ImageInFile]): Task[Unit] =
      Task.gatherUnordered(files.map(StoredImagesCache.remove)) *> Task.delay(files.foreach(_.file.toFile.delete()))

    // Find the files that should be removed to keep the max size limited
    def filesToRemove(s: StoredImages, maxCacheSize: Long): Task[List[ImageInFile]] = Task.delay {
      val u = s.sortedByAccess.foldLeft((0L, List.empty[ImageInFile])) { (s, e) =>
        val accSize = s._1 + e.fileSize
        if (accSize > maxCacheSize) {
          (accSize, e :: s._2)
        } else {
          (accSize, s._2)
        }
      }
      u._2
    }

    for {
      cache <- StoredImagesCache.get
      pref  <- ImageCatalogPreferences.preferences()
      ftr   <- filesToRemove(cache, pref.imageCacheSize.toBytes.toLong)
      _     <- deleteOldFiles(ftr)
    } yield ()
  }

  /**
    * Clear the image cache, deleting the files
    */
  def clearCache: Task[Unit] = {
    def cacheDir = Task.delay(Preferences.getPreferences.getCacheDir)

    def deleteCacheFiles(cacheDir: File): Task[Path] = Task.delay {
      Files.walkFileTree(cacheDir.toPath, new SimpleFileVisitor[Path] {
        override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
          super.visitFile(file, attrs)
          file.toFile.delete()
          FileVisitResult.CONTINUE
        }
      })
    }

    for {
      cd <- cacheDir
      _  <- deleteCacheFiles(cd)
    } yield ()
  }

}
