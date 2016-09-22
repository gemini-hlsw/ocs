package edu.gemini.catalog.ui.image

import java.beans.{PropertyChangeEvent, PropertyChangeListener}
import java.nio.file.Path
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ExecutorService, Executors, ThreadFactory}

import edu.gemini.catalog.image._
import edu.gemini.shared.util.immutable.ScalaConverters._

import scala.collection.JavaConverters._
import edu.gemini.pot.sp._
import edu.gemini.spModel.config.ConfigBridge
import edu.gemini.spModel.config.map.ConfigValMapInstances
import edu.gemini.spModel.config2.{ConfigSequence, ItemKey}
import edu.gemini.spModel.core.{Coordinates, Wavelength}
import edu.gemini.spModel.core.WavelengthConversions._
import jsky.app.ot.tpe.{InstrumentContext, TpeContext, TpeImageWidget, TpeManager}
import jsky.util.Preferences

import scalaz._
import Scalaz._
import scala.reflect.ClassTag
import scala.swing.Swing
import scalaz.concurrent.{Strategy, Task}

/**
  * Describes a requested target image
  */
case class TargetImageRequest(key: SPNodeKey, coordinates: Coordinates, obsWavelength: Option[Wavelength])

/**
  * Listens for program changes and download images as required
  */
object BackgroundImageLoader {
  val cacheDir: Path = Preferences.getPreferences.getCacheDir.toPath
  private val ObsWavelengthKey    = new ItemKey("instrument:observingWavelength")

  val ImageDownloadsThreadFactory = new ThreadFactory {
    private val threadNumber: AtomicInteger = new AtomicInteger(1)
    private val defaultThreadFactory = Executors.defaultThreadFactory()

    override def newThread(r: Runnable): Thread = {
      val t = defaultThreadFactory.newThread(r)
      t.setDaemon(true)
      t.setName("Background Image Downloads - " + threadNumber.getAndIncrement())
      t.setPriority(Thread.MIN_PRIORITY)
      t
    }
  }

  /**
    * Execution context lets up to 4 low priority threads
    */
  private val lowPriorityEC = Executors.newFixedThreadPool(4, ImageDownloadsThreadFactory)

  /**
    * Regular execution context
    */
  private val highPriorityEC = Strategy.DefaultExecutorService

  /** Called when a program is created to download its images */
  def watch(prog: ISPProgram): Unit = {
    prog.addCompositeChangeListener(ChangeListener)
    val targets = prog.getAllObservations.asScala.toList.flatMap(_.getObsComponents.asScala.map(n => requestedImage(TpeContext(n))))
    // remove duplicates
    val tasks = targets.flatten.distinct.map(requestImageDownload(ImageLoadingListener.zero))
    // Run
    runAsync(tasks) {
      case \/-(e) => println(e)// done
      case -\/(e) => println(e)
    }(lowPriorityEC)
  }

  /** Called when a program is removed to clear the cache */
  def unwatch(prog: ISPProgram): Unit = {
    prog.removeCompositeChangeListener(ChangeListener)
  }

  /******************************************
    * Methods interacting with the java side
    *****************************************/
  /**
    * Display an image if available on disk or request the download if necessary
    */
  def loadImageOnTheTpe(tpe: TpeContext, listener: ImageLoadingListener): Unit = {
    val task = requestedImage(tpe).map(requestImageDownload(listener)).getOrElse(Task.now(()))

    // This is called on an explicit user interaction so we'd rather
    // Request the execution in a higher priority thread
    // Execute and set the image on the tpe
    runAsync(task) {
      case \/-(_)       => // No image was found, don't do anything
      case -\/(e)       => // Ignore the errors
    }(highPriorityEC)
  }

  // Watches for changes to existing observations, runs BAGS on them when updated.
  object ChangeListener extends PropertyChangeListener {
    override def propertyChange(evt: PropertyChangeEvent): Unit =
      evt.getSource match {
        case node: ISPNode => Option(node.getContextObservation).foreach { o =>
          val task = requestedImage(TpeContext(o)).map(requestImageDownload(ImageLoadingListener.zero)).getOrElse(Task.now(()))

          // Run it in the background as it is lower priority than GUI
          runAsync(task) {
            case \/-(_)       => // No image was found, don't do anything
            case -\/(e)       => // Ignore the errors
          }(lowPriorityEC)

        }
      }
  }

  /**
    * Creates a task to load an image and set it on the tpe
    */
  private[image] def requestImageDownload(listener: ImageLoadingListener)(t: TargetImageRequest): Task[Unit] =
    for {
      catalog <- ObservationCatalogOverrides.catalogFor(t.key, t.obsWavelength)
      image   <- ImageCatalogClient.loadImage(cacheDir)(ImageSearchQuery(catalog, t.coordinates))(listener)
    } yield image match {
      case Some(e) => setTpeImage(e)
      case _       => // Ignore
    }

  /**
    * Extracts the data to request an image from the current context
    */
  private def requestedImage(tpe: TpeContext): Option[TargetImageRequest] =
    for {
      ctx    <- tpe.obsContext
      base   <- tpe.targets.base
      obs    <- tpe.obsShell
      when   = ctx.getSchedulingBlockStart.asScalaOpt | Instant.now.toEpochMilli
      coords <- base.getTarget.coords(when)
      key    <- tpe.obsKey
    } yield TargetImageRequest(key, coords, extractObsWavelength(tpe.instrument, obs))

  /**
    * Attempt to extract the Wavelength for the observation. Assume everything can be null
    */
  private def extractObsWavelength(ctx: InstrumentContext, obs: ISPObservation): Option[Wavelength] = {
    // Extract a double value from a string in the configuration
    def extractDoubleFromString(c: ConfigSequence, key: ItemKey): Throwable \/ Double =
      for {
        s <- extractAs[String](c, key)
        d <- \/.fromTryCatchNonFatal(s.toDouble)
      } yield d

    // Helper method that enforces that whatever we get from the config
    // for the given key is not null and matches the type we expect.
    def extractAs[A](cs: ConfigSequence, key: ItemKey)(implicit clazz: ClassTag[A]): Throwable \/ A = {
      def missingKey(key: ItemKey): \/[Throwable, A] =
        new RuntimeException(s"Missing config value for key ${key.getPath}").left[A]

      Option(cs.getItemValue(0, key)).fold(missingKey(key)) { v =>
        \/.fromTryCatchNonFatal(clazz.runtimeClass.cast(v).asInstanceOf[A])
      }
    }

    def parseWavelength(cs: ConfigSequence): Option[Wavelength] = {
      if (ctx.is(SPComponentType.INSTRUMENT_ACQCAM)) {
        None
      } else if (ctx.is(SPComponentType.INSTRUMENT_GNIRS)) {
        None
      } else {
        // regular instrument
        extractDoubleFromString(cs, ObsWavelengthKey).map(_.microns).toOption
      }
    }

    Option(ConfigBridge.extractSequence(obs, new java.util.HashMap(), ConfigValMapInstances.IDENTITY_MAP)).flatMap(parseWavelength)
  }

  /**
    * Utility methods to run the tasks on separate threads of the pool
    */
  private def runAsync[A](tasks: List[Task[A]])(f: Throwable \/ List[A] => Unit)(pool: ExecutorService) =
    Task.gatherUnordered(tasks.map(t => Task.fork(t)(pool))).unsafePerformAsync(f)

  private def runAsync[A](task: Task[A])(f: Throwable \/ A => Unit)(pool: ExecutorService) =
    Task.fork(task).unsafePerformAsync(f)

  /**
    * Attempts to set the image on the tpe, note that this is called from a separate
    * thread so we need to go to Swing for updating the UI
    * Since an image download may take a while the tpe may have moved.
    * We'll only update the position if the coordinates match
    */
  private def setTpeImage(entry: ImageEntry): Unit = {
    def markAndSet(iw: TpeImageWidget): Task[Unit] = StoredImagesCache.markAsUsed(entry) *> Task.now(iw.setFilename(entry.file.getAbsolutePath))

    Swing.onEDT {
      for {
        tpe <- Option(TpeManager.get())
        iw  <- Option(tpe.getImageWidget)
        c   <- requestedImage(iw.getContext)
        if entry.query.isNearby(c.coordinates) // The TPE may have moved so only display if the coordinates match
      } {
        val r = ImagesInProgress.contains(entry.query) >>= { inProgress => if (!inProgress) markAndSet(iw) else Task.now(())}
        // TODO: Handle errors
        r.unsafePerformSync
      }
    }
  }
}
