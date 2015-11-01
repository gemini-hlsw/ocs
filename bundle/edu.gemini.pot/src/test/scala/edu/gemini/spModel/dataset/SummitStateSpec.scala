package edu.gemini.spModel.dataset

import edu.gemini.spModel.dataset.SummitState._
import org.scalacheck.Prop.forAll

import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import java.util.UUID


object SummitStateSpec extends Specification with ScalaCheck with Arbitraries {

  "SummitState userRequest" should {
    "produce a state with the given request" in {
      forAll { (ss: SummitState, req: DatasetQaState) =>
        val ss2 = ss.transition.userRequest(req)
        ss2.request == req
      }
    }

    "produce an idle state if it matches the GSA" in {
      forAll { (ss: SummitState, req: DatasetQaState) =>
        val ss2 = ss.transition.userRequest(req)
        ss2.gsaQaStateOption.forall { gsaQa =>
          (gsaQa != req) || (ss2 match {
            case Idle(_) => true
            case _       => false
          })
        }
      }
    }

    "produce a pending post if the request doesn't match" in {
      forAll { (ss: SummitState, req: DatasetQaState) =>
        val ss2 = ss.transition.userRequest(req)
        ss2.gsaQaStateOption.forall { gsaQa =>
          (gsaQa == req) || (ss2 match {
            case ar: ActiveRequest => ar.status == QaRequestStatus.PendingPost
            case _                 => false
          })
        }
      }
    }
  }

  "SummitState gsaState update" should {
    "transition to missing if there is no state information" in {
      forAll { (ss: SummitState, gsa: Option[DatasetGsaState]) =>
        val ss2 = ss.transition.gsaState(gsa)
        gsa.isDefined || (ss2 match {
          case Missing(r) => r == ss.request
          case _          => false
        })
      }
    }

    "update the GSA state iff it is newer than what is recorded" in {
      forAll { (ss: SummitState, update: DatasetGsaState) =>
        val updateIsNewer = ss.gsaTimestampOption.forall(update.timestamp.isAfter)
        val ss2           = ss.transition.gsaState(Some(update))

        ss2.gsaOption.exists { gsa2 =>
          if (updateIsNewer) gsa2 == update else gsa2 != update
        }
      }
    }

    "transition to idle unless GSA state is new and UNDEFINED" in {
      forAll { (ss: SummitState, update: DatasetGsaState) =>
        val updateIsNewer = ss.gsaTimestampOption.forall(update.timestamp.isAfter)
        val ss2           = ss.transition.gsaState(Some(update))

        !updateIsNewer || {
          ss2 match {
            case ar: ActiveRequest if ar.status == QaRequestStatus.PendingPost =>
              update.qa == DatasetQaState.UNDEFINED && ss.request != DatasetQaState.UNDEFINED && (ss match {
                case Missing(_) => true
                case _          => false
              })
            case Idle(`update`) => true
            case _              => false
          }
        }
      }
    }
  }

  "SummitState sendRequest" should {
    "move PendingPost to ProcessingPost" in {
      forAll { (ss: SummitState, update: DatasetGsaState) =>
        val uuid = ss.requestId.getOrElse(UUID.randomUUID())
        val ss2  = ss.transition.pendingToProcessing(uuid)

        ss match {
          case ar: ActiveRequest if ar.status == QaRequestStatus.PendingPost =>
            ss2 match {
              case ar: ActiveRequest => ar.status == QaRequestStatus.ProcessingPost
              case _                 => false
            }
          case _ =>
            ss == ss2
        }
      }
    }

    "be ignored if the ids don't match" in {
      forAll { (ss: SummitState, update: DatasetGsaState) =>
        ss.transition.pendingToProcessing(UUID.randomUUID()) == ss
      }
    }
  }
}
