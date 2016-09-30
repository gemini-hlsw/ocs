package edu.gemini.catalog.image

import java.nio.file._
import java.nio.file.StandardWatchEventKinds._
import java.nio.file.attribute.BasicFileAttributes
import java.time.Instant
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
  private def populateInitialCache(cacheDir: Path): Task[StoredImages] = {
    /**
      * Creates a stream of files in a dir
      */
    def initStream: Task[DirectoryStream[Path]] = Task.delay(Files.newDirectoryStream(cacheDir, "img_*"))

    /**
      * Read each file and its access time to populate the cache
      */
    def readFiles(stream: DirectoryStream[Path]): Task[StoredImages] = {
      // This will read the last access time of the file. Note that this is OS dependent, it may be disabled in some systems
      def lastAccessTime(file: Path): Instant = Files.readAttributes(file, classOf[BasicFileAttributes]).lastAccessTime.toInstant

      def fileAndTime(f: Path): Option[Task[StoredImages]] = ImageInFile.entryFromFile(f.toFile).map(StoredImagesCache.addAt(lastAccessTime(f), _))

      val populateCache = for {
        cacheFiles <- Task.delay(stream.iterator().asScala.toList)
        accessTimes <- cacheFiles.flatMap(fileAndTime).sequenceU
      } yield accessTimes
      populateCache *> StoredImagesCache.get
    }

    /**
      * Delete temporary files. This is done at startup, there maybe temporal
      * files if the OT exits in the middle of a download
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
  private def watch(cacheDir: Path)(implicit B: Bind[Task]): Task[Unit] = {
    def waitForWatcher(watcher: WatchService) : Task[Unit] = Task.delay {
      val watchKey = watcher.take()
      val tasks = watchKey.pollEvents().asScala.toList.collect {
          case ev: WatchEvent[Path] if ev.kind() == ENTRY_DELETE =>
            val p = ev.context()
            val task = ImageInFile.entryFromFile(p.toFile).map(StoredImagesCache.remove)
            task.getOrElse(Task.now(()))
        }
      // Update the cache
      tasks.sequenceU.unsafePerformSync
    }

    log.info(s"Start watching the images cache at ${cacheDir.toFile.getAbsolutePath}")

    val watcher = cacheDir.getFileSystem.newWatchService()
    // Listen for out-of-band deletion to keep the cache up to date
    cacheDir.register(watcher, ENTRY_DELETE)
    // Keep listening for updates and close the watcher at the end
    B.forever(waitForWatcher(watcher)).onFinish(_ => Task.delay(watcher.close()))
  }

  /**
    * Run the ImageCacheWatcher
    */
  def run(): Unit = {
    val task = for {
      cd <- Task.delay(Preferences.getPreferences.getCacheDir).map(_.toPath)
      e  <- populateInitialCache(cd)
      c  <- {println(e.entries);watch(cd)}
    } yield c

    // Execute the watcher in a separate thread
    Task.fork(task).unsafePerformAsync {
      case \/-(_) => // Ignore, nothing to report
      case -\/(e) => log.log(Level.SEVERE, "Error starting the images cache watcher", e)
    }
  }
}
