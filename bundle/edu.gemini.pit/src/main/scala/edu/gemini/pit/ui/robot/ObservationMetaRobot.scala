package edu.gemini.pit.ui.robot

import edu.gemini.model.p1.immutable._

import scalaz.Lens
import java.util.{Timer, TimerTask}

import scala.swing.Swing
import edu.gemini.pit.model.Model

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Base trait for handlers of observation meta data.
 */
trait ObservationMetaRobot[K, V] extends Robot {

  // Result wraps the value when a value was obtained by the query or else
  // marks the result as either pending or failed.
  sealed trait Result[+A] {
    def isFailure = false
    def isPending = false
    def toOption: Option[A] = None
  }

  object Result {

    case class Success[A](v: A) extends Result[A] {
      override def toOption = Some(v)
    }

    case object Failure extends Result[Nothing] {
      override def isFailure = true
    }

    case object Pending extends Result[Nothing] {
      override def isPending = true
    }

    def apply(opt: Option[V]): Result[V] =
      opt.map(v => Success(v)).getOrElse(Failure)
  }

  // Our state
  type State = Map[K, Result[V]]
  protected[this] lazy val initialState: State = Map.empty

  // Some lenses
  protected val obsLens = Model.proposal andThen Proposal.observations
  protected val metaLens = Observation.meta
  protected def valueLens: Lens[ObservationMeta, Option[V]]

  // Function from Observation to the map key
  protected def key(o: Observation): Option[K]

  // Synchronous query function from Observation to the value returned by the
  // remote server (massaged into a valid ObservationMeta value).
  protected def query(o: Observation): Option[V]

  // Autorefresh may be established by calling setAutoRefresh with the refresh
  // period in ms.
  private lazy val refreshTimer = new Timer(s"ObservationMetaHandlerRefresh ${getClass.getName}", true)
  private var rebindTask: Option[TimerTask] = None
  def setAutoRefresh(ms: Long) {
    rebindTask foreach {
      _.cancel()
    }

    if (ms <= 0) {
      rebindTask = None
    } else {
      val t = new TimerTask() {
        def run() {
          Swing.onEDT(rebind())
        }
      }
      rebindTask = Some(t)
      refreshTimer.schedule(t, ms, ms)
    }
  }

  // Psuedo-lens from Observation down to the metadata value.  Creates an
  // empty ObservationMeta if needed to get down to the particular metadata
  // value.
  private def mlens: Lens[Observation, Option[V]] = Lens.lensu(
    (o, v) => {
      val meta = metaLens.get(o).getOrElse(ObservationMeta.empty)
      metaLens.set(o, Some(valueLens.set(meta, v)))
    },
    _.meta.flatMap(meta => valueLens.get(meta))
  )

  // Gets keys for every fully defined observation.
  protected def obsKeys(m: Model): Set[K] =
    (for {
      obs <- obsLens.get(m)
      k <- key(obs)
    } yield k).toSet

  // Removes all the state entries for which the value doesn't correspond to
  // at least one observation in the list or which failed in the past.
  protected def cleanState(m: Model) {
    val keys = obsKeys(m)
    state = state.filter {
      case (k, v) => keys.contains(k) && !v.isFailure
    }
  }

  // Lookup the cached value associated with the given observation, if any.
  protected def lookup(o: Observation): Option[V] =
    key(o).flatMap(k => lookup(k))

  protected def lookup(k: K): Option[V] = state.get(k).flatMap(_.toOption)

  // Checks the state to determine whether the key associated with the given
  // observation is defined in the state (even if the value it maps to is None)
  protected def getUpdate(o: Observation): Option[Option[V]] = {
    val update = key(o).flatMap(k => state.get(k)) match {
      case Some(Result.Failure)    => Some(None) // Update with None
      case Some(Result.Success(v)) => Some(Some(v)) // Update with Some(v)
      case Some(Result.Pending)    => None // No update
      case _                       => None // No update
    }

    // Only update if the metadata value that already exists isn't the same.
    update.filter {
      up => mlens.get(o) != up
    }
  }

  // Updates the model to match the current state, if there is anything new
  // in it.
  protected def updateModel(m: Model) {
    // We're mapping over the list essentially but keeping up with whether
    // any obs was actually updated.
    val init: (List[Observation], Boolean) = (Nil, false)

    // Left-fold will reverse the order of the obs list
    val (revList, updated) = (init /: obsLens.get(m)) {
      case ((lst, up), obs) =>
        getUpdate(obs).map {
          newValue =>
            (mlens.set(obs, newValue) :: lst, true)
        }.getOrElse((obs :: lst, up))
    }

    // Only updates the model if there was an update
    if (updated) model = Some(obsLens.set(m, revList.reverse))
  }

  // All the observations for which the meta data value is missing.
  protected def missing(m: Model): List[(K, Observation)] =
    obsLens.get(m) collect {
      case o if needsUpdate(o) => (key(o).get, o)
    }

  // Needs an update if the key is defined for the observation, the meta data
  // value is missing, and we aren't expecting a result from a previous refresh
  protected def needsUpdate(o: Observation): Boolean =
    key(o).exists {
      k =>
        mlens.get(o).isEmpty && state.get(k).forall(r => !r.isPending)
    }

  override protected def refresh(m: Option[Model]) {
    for {
      m <- model
      if !m.proposal.isSubmitted // don't do this once we have submitted
    } doRefresh(m)
  }

  protected def doRefresh(m: Model) {
    cleanState(m)

    // Get a map from key to observation for all observations that need
    // attention.  If multiple observations map to the same key we throw away
    // the extras because it is the key that differentiates two results.
    missing(m).toMap.foreach {
      case (k, o) =>

        // If a successful query result has already been cached, we will just use
        // it.  Otherwise, mark the result as pending.
        val cachedValue = lookup(k)
        if (cachedValue.isEmpty) state = state + (k -> Result.Pending)

        // Do an asynchronous query to update the value
        Future {
          callback(k, query(o))
        }
    }
  }

  // This callback can come from anywhere, so route it onto the UI thread. This
  // ensures that updates are serial and always operate on the current model.
  private def callback(k: K, v: Option[V]) {
    Swing.onEDT {
      model.foreach {
        m =>
          // Cache the result and update the model
          state = state + (k -> Result(v))
          updateModel(m)
      }
    }
  }

}
