package edu.gemini.dataman.gsa.query

import edu.gemini.spModel.core.Site
import edu.gemini.spModel.dataset.{DatasetQaState, DatasetLabel}

import argonaut._
import Argonaut._

import java.net.URL

/** A synchronous GSA query that sets the dataset QA state for one or more
  * datasets. */
sealed trait GsaQaUpdateQuery {
  def setQaStates(updates: List[GsaQaUpdate.Request]): GsaResponse[List[GsaQaUpdate.Response]]

  def setQaState(label: DatasetLabel, qa: DatasetQaState): GsaResponse[List[GsaQaUpdate.Response]] =
    setQaStates(List(GsaQaUpdate.Request(label, qa)))
}

object GsaQaUpdateQuery {
  import GsaQaUpdate._

  /** Constructs a QA update query for the given host and site.  The `auth`
    * parameter is how the GSA service provides some minimal protection against
    * inappropriate setting of QA states.  The value is sent in a cookie that
    * is checked on the server. */
  def apply(host: String, site: Site, auth: String): GsaQaUpdateQuery =
    new GsaQaUpdateQuery {
      override def setQaStates(requests: List[Request]): GsaResponse[List[Response]] =
        GsaQuery.post[List[Request], List[Response]](new URL(s"http://$host/update_headers"), requests, auth).map { responses =>
          // Prepend errors for any update requests that were silently ignored.
          (requests.map(_.label).toSet &~ responses.map(_.label).toSet).toList.map { lab =>
            Response(lab, Some(s"No response returned for $lab update request."))
          } ++ responses
        }
    }
}
