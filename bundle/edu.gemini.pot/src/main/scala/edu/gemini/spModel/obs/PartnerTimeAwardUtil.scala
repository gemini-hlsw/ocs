package edu.gemini.spModel.obs

import edu.gemini.pot.sp.ISPProgram
import edu.gemini.spModel.gemini.obscomp.SPProgram
import edu.gemini.spModel.time.ChargeClass.PARTNER

import edu.gemini.spModel.timeacct.TimeAcctAllocation
import edu.gemini.spModel.timeacct.TimeAcctAward
import edu.gemini.spModel.timeacct.TimeAcctCategory

import java.time.Duration
import java.util.logging.{Level, Logger}

import scala.collection.JavaConverters._

/** A utility that will be needed during the transition to a new time accounting
  * model. Specifically it is used to come up with a fake partner time award
  * that exactly matches the actual current executed partner time.  This will
  * make the remaining time calculation, which is now:
  *
  * (program + partner award) - (program + partner executed)
  *
  * come out the same for pre-2017B programs.  Once there is no longer an active
  * pre-2017B program in the queue this code can be deleted.
  */
object PartnerTimeAwardUtil {
  private val Log = Logger.getLogger(PartnerTimeAwardUtil.getClass.getName)

  /** Sets the partner time award to the executed partner time amount in the
    * given program.
    */
  def setPartnerAwardToExecuted(prog: ISPProgram): Unit = {

    // Distributes amt over all the map keys as close to the given ratios as
    // possible, but ensuring that the sum of all the values is amt.
    def distribute[A](amt: Long, ratios: Map[A, Double]): Map[A, Long] = {
      val proportions = ratios.map { case (a, r) =>
        val f = amt * r
        (a, f.floor.toLong, f - f.floor)
      }.toList

      val sum    = proportions.map(_._2).sum
      val error  = (amt - sum).toInt // how many values need to be incremented

      // Sort by the fractional value and then discard it.
      val wholes = proportions.sortBy(_._3).map { case (cat, whole, _) => (cat, whole) }

      // Round up the first whole values so that in the end it sums to amt.
      val (incr, nochange) = wholes.splitAt(error)
      incr.map { case (cat, whole) => (cat, whole + 1) }.toMap ++ nochange.toMap
    }

    val dataObj             = prog.getDataObject.asInstanceOf[SPProgram]
    val alloc               = dataObj.getTimeAcctAllocation
    val award               = alloc.getSum
    val awardedPartnerTime  = award.getPartnerAward.toMillis
    val awardedProgramTime  = award.getProgramAward.toMillis

    // Distributes the given executed partner time amount over the partner
    // award according to its awarded program time ratio.
    def updatePartnerAward(executedPartnerTime: Long): Unit = {
      Log.info(s"Updating awarded partner time for ${prog.getProgramID} to ${Duration.ofMillis(executedPartnerTime)}")

      // We need to use the executedPartnerTime as the partner award.  To do so,
      // we should distribute that time amount across the various partners
      // supporting the program. We will use the same proportion as the awarded
      // *program* time for the partner since we're constantly updating the
      // partner time.
      val ratios = (Map.empty[TimeAcctCategory, Double]/:TimeAcctCategory.values()) { case (m, cat) =>
        val portion = alloc.getAward(cat).getProgramAward.toMillis
        if (portion == 0) m else m.updated(cat, portion.toDouble / awardedProgramTime)
      }

      val partnerTimeDistribution = distribute(executedPartnerTime, ratios)

      // Build a new allocation map with the new partner times.
      val newAllocMap = (Map.empty[TimeAcctCategory, TimeAcctAward]/:partnerTimeDistribution) { case (m, (cat, partTime)) =>
        val award = alloc.getAward(cat)
        m.updated(cat, new TimeAcctAward(award.getProgramAward, Duration.ofMillis(partTime)))
      }

      val newAlloc = new TimeAcctAllocation(newAllocMap.asJava)

      // Update the program with the new allocations.
      dataObj.setTimeAcctAllocation(newAlloc)
      prog.setDataObject(dataObj)
    }

    // Update only if there is awarded program time (otherwise we're looking at
    // an engineering or calibration program or something) and if the executed
    // partner time differs from the awarded partner time.
    if (awardedProgramTime != 0) {
      val exec = ObsTimesService.getCorrectedObsTimes(prog).getTimeCharges.getTime(PARTNER)
      if (exec != awardedPartnerTime) updatePartnerAward(exec)
    }
  }
}
