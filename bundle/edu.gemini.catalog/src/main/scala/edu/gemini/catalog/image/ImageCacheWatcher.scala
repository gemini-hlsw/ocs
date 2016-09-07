package edu.gemini.catalog.image

import java.io.File
import java.nio.file._
import java.nio.file.StandardWatchEventKinds._
import java.nio.file.attribute.BasicFileAttributes

import scalaz.concurrent.Task
import scala.collection.JavaConverters._
import scalaz._
import Scalaz._

/**
  * Observes the file system to keep the cache populated
  */
// Fill the in-memory cache with references to the existing images
object ImageCacheWatcher {
  // TODO Support more file extensions?
  // TODO Should we somehow validate the files?
  def initStream(cacheDir: File): Task[DirectoryStream[Path]] = Task.delay(Files.newDirectoryStream(cacheDir.toPath, "img_*.fits.gz"))

  def populateInitialCache(cacheDir: File): Task[StoredImages] = {

    def closeStream(stream: DirectoryStream[Path]): Task[Unit] = Task.delay(stream.close())

    def readFiles(stream: DirectoryStream[Path]): Task[StoredImages] =
      Task.delay {
        val u = stream.iterator().asScala.toList
        val p = u.flatMap(f => ImageEntry.entryFromFile(f.toFile)).map(StoredImagesCache.add)
        (p.sequenceU *> StoredImagesCache.get).unsafePerformSync
      }.onFinish(f => Task.delay(f.foreach(u => stream.close()))) // Make sure the stream is closed

    for {
      ds <- initStream(cacheDir)
      ab <- readFiles(ds)
      _  <- closeStream(ds)
    } yield ab
  }

  def trimStoredImages(cacheDir: File): Task[Unit] = {
    def findObsoleteFiles(stream: DirectoryStream[Path]): Task[List[Any]] =
      Task.delay {
        val u = stream.iterator().asScala.toList
        u.sortBy(f => {Files.readAttributes(f, "lastAccessTime");f.toFile.lastModified()}).map(f => (new java.util.Date(f.toFile.lastModified()), f.toFile.getName))
      }

    for {
      ds <- initStream(cacheDir)
      f <- findObsoleteFiles(ds)
    } yield ()
  }

  def watch(cacheDir: File)(implicit B: Bind[Task]): Task[Unit] = {
    def waitForWatcher(watcher: WatchService) : Task[Unit] = Task.delay {
      val watchKey = watcher.take()
      val tasks = watchKey.pollEvents().asScala.toList.collect {
          case ev: WatchEvent[Path] if ev.kind() == ENTRY_DELETE =>
            val p = ev.context()
            val task = for {
                e <- ImageEntry.entryFromFile(p.toFile)
              } yield StoredImagesCache.remove(e)
            task.getOrElse(Task.now(()))
        }
      // Update the cache
      tasks.sequenceU.unsafePerformSync
    }

    val watcher = cacheDir.toPath.getFileSystem.newWatchService()
    cacheDir.toPath.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
    // Keep listening for updates and close at the end
    B.forever(waitForWatcher(watcher)).onFinish(_ => Task.delay(watcher.close()))
  }

  def run(cacheDir: File): Unit = {
    val task = for {
      _ <- populateInitialCache(cacheDir)
      _ <- trimStoredImages(cacheDir)
      c <- watch(cacheDir)
    } yield c
    Task.fork(task).unsafePerformAsync(println)
  }
}
