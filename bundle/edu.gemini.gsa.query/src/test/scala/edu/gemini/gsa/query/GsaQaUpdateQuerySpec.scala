package edu.gemini.gsa.query

import edu.gemini.gsa.core.{QaResponse, QaRequest, Arbitraries}
import edu.gemini.spModel.dataset.DatasetLabel
import org.scalacheck._
import org.scalacheck.Arbitrary._
import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import scalaz.{-\/, \/-}

object GsaQaUpdateQuerySpec extends Specification with ScalaCheck with Arbitraries {
  import GsaQaUpdateQuery._

  val requestResponsePair: Gen[(List[QaRequest], List[EitherQaResponse])] =
    for {
      reqs <- Gen.listOf(arbitrary[QaRequest])
      reps <- genResponses(reqs)
    } yield (reqs, reps)


  "GsaQaUpdateQuery" should {
    "produce a response for every request" in {
      forAll(requestResponsePair) { case (reqs, ereps) =>

        val reqLabs = requestLabels(reqs)
        val repLabs = responseLabels(toQaResponses(reqs, ereps))

        (reqLabs &~ repLabs).isEmpty
      }
    }

    "not produce a response for a dataset that wasn't requested" in {
      forAll(requestResponsePair) { case (reqs, ereps) =>

        val reqLabs = requestLabels(reqs)
        val repLabs = responseLabels(toQaResponses(reqs, ereps))

        (repLabs &~ reqLabs).isEmpty
      }
    }

    "produce a `no response' message if nothing was returned from server" in {
      forAll(requestResponsePair) { case (reqs, ereps) =>
        // if there is no general failure, all requests for which the server
        // didn't generate a response should be the "no response" message
        ereps.exists(_.isLeft) || {
          actualNoResponseMessages(reqs, ereps).forall {
            case (lab, msgOption) => msgOption.exists(_ == noResponseMessage(lab))
          }
        }
      }
    }

    "use general errors for missing server responses if available" in {
      forAll(requestResponsePair) { case (reqs, ereps) =>
        // Unless all the failures were specific to individual datasets, any
        // dataset for which the server didn't generate a specific response
        // should be attributed to the general failure(s)
        ereps.forall(_.isRight) || {

          // Gather up all expected messages, put them in a Set to remove
          // duplicates, convert back to a sorted list, and then make a single
          // string.
          val expectedMessage = archiveFailureMessage(ereps.collect {
            case -\/(m) => m
          }.toSet.toList.sorted.mkString("\n"))

          actualNoResponseMessages(reqs, ereps).forall {
            case (_, msgOption) => msgOption.exists(_ == expectedMessage)
          }
        }
      }
    }
  }

  private def labels[A](lst: List[A])(f: A => DatasetLabel): Set[DatasetLabel] =
    lst.map(f).toSet

  private def requestLabels(reqs: List[QaRequest]): Set[DatasetLabel] =
    labels(reqs)(_.label)

  private def responseLabels(reps: List[QaResponse]): Set[DatasetLabel] =
    labels(reps)(_.label)

  private def ereponseLabels(ereps: List[EitherQaResponse]): Set[DatasetLabel] =
    responseLabels(ereps.collect { case \/-(r) => r })

  // Gets a map of all the messages for requests for which the server did not
  // send a specific response.
  private def actualNoResponseMessages(reqs: List[QaRequest], ereps: List[EitherQaResponse]): Map[DatasetLabel, Option[String]] = {
    val noServerResponse = requestLabels(reqs) &~ ereponseLabels(ereps)
    val repMap           = toQaResponses(reqs, ereps).map(r => r.label -> r).toMap

    reqs.filter(r => noServerResponse(r.label)).map { r =>
      r.label -> (for {
        rep <- repMap.get(r.label)
        msg <- rep.failure
      } yield msg)
    }.toMap
  }


  private def genResponses(reqs: List[QaRequest]): Gen[List[EitherQaResponse]] = {
    def genValidResponse(req: QaRequest): Gen[EitherQaResponse] =
      for {
        lab     <- Gen.frequency((95, req.label), (5, arbitrary[DatasetLabel]))
        msg     <- Gen.alphaStr.map(_.take(10))
        failure <- Gen.frequency((9, Option.empty[String]), (1, Some(msg)))
      } yield \/-(QaResponse(lab, failure))

    def genOptionalValidResponse(req: QaRequest): Gen[Option[EitherQaResponse]] =
      for {
        valid  <- genValidResponse(req)
        result <- Gen.frequency((95, Option(valid)), (5, Option.empty[EitherQaResponse]))
      } yield result

    val normalResponses: Gen[List[EitherQaResponse]] = {
      val gens = reqs.map(r => genOptionalValidResponse(r))
      Gen.sequence[List, Option[EitherQaResponse]](gens).map(_.flatten)
    }

    val errorResponses: Gen[List[EitherQaResponse]] =
      for {
        n    <- Gen.frequency((75, 1), (25, Gen.posNum[Int]))
        msgs <- Gen.listOfN(n, Gen.alphaStr.map(_.take(10)))
      } yield msgs.map(-\/(_))

    val mixedResponses: Gen[List[EitherQaResponse]] =
      for {
        norm <- normalResponses
        err  <- errorResponses
      } yield (norm ++ err).sortBy(_.hashCode())

    Gen.frequency((80, normalResponses), (10, errorResponses), (10, mixedResponses))
  }
}
