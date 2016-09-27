package edu.gemini.catalog.image

import java.io.File
import java.nio.file._
import java.nio.file.StandardWatchEventKinds._
import java.nio.file.attribute.BasicFileAttributes
import java.util.logging.{Level, Logger}

import jsky.util.Preferences

import scalaz.concurrent.Task
import scala.collection.JavaConverters._
import scalaz._
import Scalaz._

/**
  * Observes the file system to keep the cache populated
  */
object ImageCacheWatcher {
  val log: Logger = Logger.getLogger(this.getClass.getCanonicalName)

  /**
    * Populates the cache when the application starts
    */
  private def populateInitialCache(cacheDir: File): Task[StoredImages] = {
    // TODO Support more file extensions?
    // TODO Should we somehow validate the files?
    def initStream(cacheDir: File): Task[DirectoryStream[Path]] = Task.delay(Files.newDirectoryStream(cacheDir.toPath, "img_*"))

    def closeStream(stream: DirectoryStream[Path]): Task[Unit] = Task.delay(stream.close())

    def readFiles(stream: DirectoryStream[Path]): Task[StoredImages] =
      Task.delay {
        val u = stream.iterator().asScala.toList
        val p = u.flatMap(f => ImageEntry.entryFromFile(f.toFile).map(e => StoredImagesCache.addAt(Files.readAttributes(f, classOf[BasicFileAttributes]).lastAccessTime.toInstant, e)))
        (p.sequenceU *> StoredImagesCache.get).unsafePerformSync
      }.onFinish(f => Task.delay(f.foreach(u => stream.close()))) // Make sure the stream is closed

    for {
      ds <- initStream(cacheDir)
      ab <- readFiles(ds)
      _  <- closeStream(ds)
    } yield ab
  }

  /**
    * Observe the file system to detect when files are modified and update the cache accordingly
    */
  private def watch(cacheDir: File)(implicit B: Bind[Task]): Task[Unit] = {
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
    log.info(s"Start watching the images cache at ${cacheDir.getAbsolutePath}")
    // Keep listening for updates and close at the end
    B.forever(waitForWatcher(watcher)).onFinish(_ => Task.delay(watcher.close()))
  }

  /**
    * Run the ImageCacheWatcher
    */
  def run(): Unit = {
    val task = for {
      cd <- Task.delay(Preferences.getPreferences.getCacheDir)
      _  <- populateInitialCache(cd)
      c  <- watch(cd)
    } yield c

    // Execute the watcher in a separate thread
    Task.fork(task).unsafePerformAsync {
      case \/-(_) =>
      case -\/(e) => log.log(Level.SEVERE, "Error starting the images cache watcher", e)
    }
  }
}
