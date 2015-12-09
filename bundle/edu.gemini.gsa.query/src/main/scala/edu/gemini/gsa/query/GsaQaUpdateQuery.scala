package edu.gemini.gsa.query

import java.net.URL

import edu.gemini.gsa.core.{QaRequest, QaResponse}
import edu.gemini.spModel.core.Site
import edu.gemini.spModel.dataset.{DatasetLabel, DatasetQaState}
import edu.gemini.gsa.query.JsonCodecs._

import scalaz.\/

/** A synchronous GSA query that sets the dataset QA state for one or more
  * datasets.
  */
sealed trait GsaQaUpdateQuery {
  def setQaStates(updates: List[QaRequest]): GsaResponse[List[QaResponse]]

  def setQaState(label: DatasetLabel, qa: DatasetQaState): GsaResponse[List[QaResponse]] =
    setQaStates(List(QaRequest(label, qa)))
}

object GsaQaUpdateQuery {

  /** The archive will return an error message without an ID or else a valid
    * `QaRresponse`.  We will convert all the error messages into a single
    * String and apply it to any dataset for which a valid response wasn't
    * returned.  `EitherQaResponse` should not be visible outside of this
    * package.  It's a quirk of how the FITS server works.
    */
  private[query] type EitherQaResponse = String \/ QaResponse


  /** Constructs a QA update query for the given host and site.  The `auth`
    * parameter is how the GSA service provides some minimal protection against
    * inappropriate setting of QA states.  The value is sent in a cookie that
    * is checked on the server.
    */
  def apply(host: GsaHost.Summit, site: Site, auth: GsaAuth): GsaQaUpdateQuery =
    new GsaQaUpdateQuery {
      override def setQaStates(requests: List[QaRequest]): GsaResponse[List[QaResponse]] = {
        val url = new URL(s"${host.protocol}://${host.host}/update_headers")
        GsaQuery.post[List[QaRequest], List[EitherQaResponse]](url, requests, auth).map(toQaResponses(requests, _))
      }
    }

  private[query] def noResponseMessage(lab: DatasetLabel): String =
    s"Archive did not return a response for the $lab update request."

  private[query] def archiveFailureMessage(m: String): String =
    s"Archive server returned: $m"

  private[query] def toQaResponses(requests: List[QaRequest], responses: List[EitherQaResponse]): List[QaResponse] = {

    // Partition the responses into a Set of general error messages and a list
    // of valid responses.
    val (errors, validResponses) = ((Set.empty[String], List.empty[QaResponse])/:responses) { case ((errSet,valids), r) =>
        r.fold(e => (errSet + e, valids), v => (errSet, v :: valids))
    }

    // Sort and concatenate the errors, turning them into a string with one
    // error per line.
    val errorMessage  = archiveFailureMessage(errors.toList.sorted.mkString("\n"))

    val requestLabels = requests.map(_.label).toSet
    val qaLabels      = validResponses.map(_.label).toSet
    val missingLabels = requestLabels &~ qaLabels

    // Create a valid QaResponse for each missing response from the server.
    val errorResponses = missingLabels.map { lab =>
      val msg = if (errors.isEmpty) noResponseMessage(lab) else errorMessage
      QaResponse(lab, Some(msg))
    }

    // Prepend errors for any update requests that were silently ignored by the
    // server. Ignore any extra responses the server might return.  The goal
    // is to have 1:1 request:response ratio in the end.
    errorResponses.toList ++ validResponses.filter(r => requestLabels(r.label))
  }
}
