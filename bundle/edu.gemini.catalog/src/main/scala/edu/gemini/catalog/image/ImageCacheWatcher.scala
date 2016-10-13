package edu.gemini.catalog.image

import java.nio.file._
import java.nio.file.StandardWatchEventKinds._
import java.nio.file.attribute.BasicFileAttributes
import java.time.Instant
import java.util.concurrent.Executors
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
  val Log: Logger = Logger.getLogger(this.getClass.getCanonicalName)

  /**
    * Populates the cache when the application starts
    */
  private def populateInitialCache(cacheDir: Path): Task[StoredImages] = {
    /**
      * Creates a stream of files in a dir
      */
    def initStream: Task[DirectoryStream[Path]] =
      Task.delay(Files.newDirectoryStream(cacheDir, "img_*"))

    /**
      * Read each file and its access time to populate the cache
      */
    def readFiles(stream: DirectoryStream[Path]): Task[StoredImages] = {
      // This will read the last access time of the file. Note that this is OS dependent,
      // it may be disabled in some systems and we'd get creation time instead
      def lastAccessTime(file: Path): Instant =
        Files.readAttributes(file, classOf[BasicFileAttributes]).lastAccessTime.toInstant

      def fileAndTime(f: Path): Option[Task[StoredImages]] =
        ImageInFile.entryFromFile(f.toFile).map(StoredImagesCache.addAt(lastAccessTime(f), _))

      for {
        cacheFiles   <- Task.delay(stream.iterator().asScala.toList)
        _            <- cacheFiles.flatMap(fileAndTime).sequenceU
        initialCache <- StoredImagesCache.get
      } yield initialCache
    }

    /**
      * Delete temporary files. This is done at startup, there maybe temporal
      * files left in the cache if e.g. the OT exits in the middle of a download
      */
    def rmTempFiles: Task[Path] = Task.delay {
      val matcher = FileSystems.getDefault.getPathMatcher("glob:.img*.fits")

      Files.walkFileTree(cacheDir, new SimpleFileVisitor[Path] {
        override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
          super.visitFile(file, attrs)
          if (matcher.matches(file.getFileName)) {
            file.toFile.delete()
          }
          FileVisitResult.CONTINUE
        }
      })
    }

    def closeStream(stream: DirectoryStream[Path]): Task[Unit] = Task.delay(stream.close())

    for {
      _  <- rmTempFiles
      ds <- initStream
      ab <- readFiles(ds).onFinish(_ => closeStream(ds)) // Make sure the stream is closed
    } yield ab
  }

  /**
    * Observe the file system to detect when files are modified and update the cache accordingly
    */
  private def watch(cacheDir: Path): Task[Unit] = {
    def waitForWatcher(watcher: WatchService) : Task[Unit] = Task.delay {
      val watchKey = watcher.take()
      // Called when a file is deleted
      val tasks = watchKey.pollEvents().asScala.toList.map(e => (e.context(), e.kind())).collect {
          case (p: Path, ENTRY_DELETE) =>
            ImageInFile.entryFromFile(p.toFile).map(StoredImagesCache.remove)
        }
      // Update the cache, removing deleted files
      tasks.flatten.sequenceU.unsafePerformSync
    }

    Log.info(s"Start watching the images cache at ${cacheDir.toFile.getAbsolutePath}")

    val watcher = cacheDir.getFileSystem.newWatchService()
    // Register to listen for out-of-band deletion keeping the cache up to date
    cacheDir.register(watcher, ENTRY_DELETE)
    // Keep listening for updates and close the watcher at the end
    Bind[Task].forever(waitForWatcher(watcher)).onFinish(_ => Task.delay(watcher.close()))
  }

  /**
    * Run the ImageCacheWatcher
    */
  def run(): Unit = {
    // Don't use the default executor
    val executor = Executors.newFixedThreadPool(1)

    val task = for {
      cachePath <- Task.delay(Preferences.getPreferences.getCacheDir).map(_.toPath)
      _         <- populateInitialCache(cachePath)
      cache     <- watch(cachePath)
    } yield cache

    // Execute the watcher in a separate thread
    Task.fork(task)(executor).unsafePerformAsync {
      case \/-(_) => // Ignore, nothing to report
      case -\/(e) => Log.log(Level.SEVERE, "Error on images cache watcher", e)
    }
  }
}
