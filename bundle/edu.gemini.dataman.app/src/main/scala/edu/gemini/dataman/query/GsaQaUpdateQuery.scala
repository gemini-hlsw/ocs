package edu.gemini.dataman.query

import edu.gemini.dataman.core.{GsaAuth, GsaHost, QaRequest, QaResponse}
import edu.gemini.dataman.query.JsonCodecs._
import edu.gemini.spModel.core.Site
import edu.gemini.spModel.dataset.{DatasetLabel, DatasetQaState}

import java.net.URL

/** A synchronous GSA query that sets the dataset QA state for one or more
  * datasets. */
sealed trait GsaQaUpdateQuery {
  def setQaStates(updates: List[QaRequest]): GsaResponse[List[QaResponse]]

  def setQaState(label: DatasetLabel, qa: DatasetQaState): GsaResponse[List[QaResponse]] =
    setQaStates(List(QaRequest(label, qa)))
}

object GsaQaUpdateQuery {

  /** Constructs a QA update query for the given host and site.  The `auth`
    * parameter is how the GSA service provides some minimal protection against
    * inappropriate setting of QA states.  The value is sent in a cookie that
    * is checked on the server. */
  def apply(host: GsaHost.Summit, site: Site, auth: GsaAuth): GsaQaUpdateQuery =
    new GsaQaUpdateQuery {
      override def setQaStates(requests: List[QaRequest]): GsaResponse[List[QaResponse]] =
        GsaQuery.post[List[QaRequest], List[QaResponse]](new URL(s"${host.protocol}://${host.host}/update_headers"), requests, auth).map { responses =>
          // Prepend errors for any update requests that were silently ignored.
          (requests.map(_.label).toSet &~ responses.map(_.label).toSet).toList.map { lab =>
            QaResponse(lab, Some(s"No response returned for $lab update request."))
          } ++ responses
        }
    }
}
