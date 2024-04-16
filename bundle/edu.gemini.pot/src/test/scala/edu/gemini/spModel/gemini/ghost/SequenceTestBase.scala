package edu.gemini.spModel.gemini.ghost

import edu.gemini.pot.sp.SPComponentType
import edu.gemini.spModel.data.config.{DefaultParameter, IParameter}
import edu.gemini.spModel.test.InstrumentSequenceTestBase
import edu.gemini.spModel.test.InstrumentSequenceTestBase._
import java.beans.PropertyDescriptor
import org.junit.Test

import scala.collection.JavaConverters._


abstract class SequenceTestBase extends InstrumentSequenceTestBase[Ghost, SeqConfigGhost] {
  override protected def getObsCompSpType: SPComponentType =
    SPComponentType.INSTRUMENT_GHOST

  override protected def getSeqCompSpType: SPComponentType =
    SeqConfigGhost.SP_TYPE

  protected def getParameter[T](pd: PropertyDescriptor, values: T*): IParameter =
    DefaultParameter.getInstance(pd.getName, values.toList.asJava)

  override def setUp(): Unit = {
    super.setUp()
  }
}
