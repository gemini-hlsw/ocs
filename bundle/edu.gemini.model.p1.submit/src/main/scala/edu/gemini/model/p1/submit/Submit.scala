package edu.gemini.model.p1.submit

import edu.gemini.model.p1.immutable.ProposalIo
import java.io.File
import java.util.Date
import edu.gemini.model.p1.submit.SubmitDestination._
import edu.gemini.model.p1.submit.SubmitResult.{Failure, Success}

/**
 * A simple test app for running a proposal submission and dumping the results
 * to the console.  Expects to be called with a proposal file argument.
 */
object Submit {
  def main(args: Array[String]): Unit = {
    if (args.length == 0) {
      println("specify the proposal XML to send")
      sys.exit()
    }

    val f = new File(args(0))
    if (!f.isFile) {
      println("%s doesn't exist or isn't a normal file".format(args(0)))
      sys.exit()
    }

    SubmitClient.test.submit(ProposalIo.read(f)) { propResult =>
      println("Proposal Sumission Results")
      println(ProposalIo.writeToString(propResult.proposal))
      propResult.results foreach { destResult =>
        destResult.destination match {
          case Ngo(ngo)               => println("*** NGO: " + ngo.name)
          case Exchange(exc)          => println("*** Exchange: " + exc.name)
          case Special(tipe)          => println("*** Special: " + tipe.name)
          case LargeProgram           => println("*** LargeProgram")
          case FastTurnaroundProgram  => println("*** LargeProgram")
          case SubaruIntensiveProgram => println("*** SubaruProgram")
        }
        destResult.result match {
          case Success(ref, time, contact, msg) =>
            println("\tref....: " + ref)
            println("\ttime...: " + new Date(time))
            println("\tcontact: " + contact)
            println("\tmsg....: " + msg)
          case f: Failure =>
            println("\tfailure: " + f.message)
        }
      }
    }
  }
}
