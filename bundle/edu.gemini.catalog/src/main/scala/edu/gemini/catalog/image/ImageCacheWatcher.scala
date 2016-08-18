package edu.gemini.catalog.image

import java.io.File
import java.nio.file._
import java.nio.file.StandardWatchEventKinds._

import edu.gemini.spModel.core.Coordinates

import scalaz.concurrent.Task
import scala.collection.JavaConverters._
import scalaz._
import Scalaz._

/**
  * Observes the file system to keep the cache populated
  */
object ImageCacheWatcher {
  // Fill the in-memory cache with references to the existing images
  def populateInitialCache(path: File): Task[StoredImages] = {
    // TODO Support more file extensions?
    // TODO Should we somehow validate the files?
    def initStream: Task[DirectoryStream[Path]] = Task.delay(Files.newDirectoryStream(path.toPath, "img_*.fits.gz"))

    def closeStream(stream: DirectoryStream[Path]): Task[Unit] = Task.delay(stream.close())

    def readFiles(stream: DirectoryStream[Path]): Task[StoredImages] =
      Task.delay {
        val u = stream.iterator().asScala.toList
        val p = u.flatMap(f => ImageEntry.entryFromFile(f.toFile)).map(StoredImagesCache.add)
        (p.sequenceU *> StoredImagesCache.get).unsafePerformSync
      }.onFinish(f => Task.delay(f.foreach(u => stream.close()))) // Make sure the stream is closed

    for {
      ds <- initStream
      ab <- readFiles(ds)
      _  <- closeStream(ds)
    } yield ab
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
      c <- watch(cacheDir)
    } yield c
    Task.fork(task).unsafePerformAsync(println)
  }
}
