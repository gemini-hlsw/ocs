package edu.gemini.spModel.io.impl.migration.to2010B

import edu.gemini.spModel.pio.Container
import edu.gemini.spModel.pio.xml.PioXmlUtil
import java.io.StringReader
import org.junit._
import Assert._
import scala.xml.Node

class ToGnirsAtGnTest {

  val sxd =
    (<container kind="observation" type="Observation" version="2009A-1" subtype="basic" key="ff631cb5-56d0-4864-b26f-e8cf4541beff" name="">
      <paramset name="Observation" kind="dataObj">
        <param name="title" value="GNIRS Observation"/>
        <param name="status" value="PHASE2"/>
      </paramset>
      <container kind="obsComp" type="Instrument" version="2006B-1" subtype="GNIRS" key="dff42615-421e-4fa4-9328-17cf61f221af" name="GNIRS">
        <paramset name="GNIRS" kind="dataObj">
          <param name="exposureTime" value="17.0"/>
          <param name="pixelScale" value="PS_015"/>
          <param name="crossDispersed" value="NO"/>
        </paramset>
      </container>
      <container kind="seqComp" type="Iterator" version="2009A-1" subtype="base" key="e0f126fc-f36f-4d83-af58-82ad9f22ac97" name="Sequence">
        <paramset name="Sequence" kind="dataObj"/>
        <container kind="seqComp" type="Iterator" version="2006B-1" subtype="GNIRS" key="ce02e07c-99b2-42bd-90ea-cd085055592b" name="GNIRS Sequence">
          <paramset name="GNIRS Sequence" kind="dataObj">
            <param name="crossDispersed">
              <value sequence="0">NO</value>
              <value sequence="1">YES</value>
              <value sequence="2">NO</value>
              <value sequence="3">YES</value>
            </param>
          </paramset>
          <container kind="seqComp" type="Observer" version="2009A-1" subtype="observe" key="a608b663-7344-4ae6-b3a0-d492e583141e" name="Observe">
            <paramset name="Observe" kind="dataObj">
              <param name="repeatCount" value="1"/>
            </paramset>
          </container>
        </container>
      </container>
    </container>,
    List(List("NO", "SXD", "NO", "SXD"))
   )

  @Test def testSxd() { verify(sxd) }

  val lxd =
    (<container kind="observation" type="Observation" version="2009A-1" subtype="basic" key="ff631cb5-56d0-4864-b26f-e8cf4541beff" name="">
      <paramset name="Observation" kind="dataObj">
        <param name="title" value="GNIRS Observation"/>
        <param name="status" value="PHASE2"/>
      </paramset>
      <container kind="obsComp" type="Instrument" version="2006B-1" subtype="GNIRS" key="dff42615-421e-4fa4-9328-17cf61f221af" name="GNIRS">
        <paramset name="GNIRS" kind="dataObj">
          <param name="exposureTime" value="17.0"/>
          <param name="pixelScale" value="PS_005"/>
          <param name="crossDispersed" value="NO"/>
        </paramset>
      </container>
      <container kind="seqComp" type="Iterator" version="2009A-1" subtype="base" key="e0f126fc-f36f-4d83-af58-82ad9f22ac97" name="Sequence">
        <paramset name="Sequence" kind="dataObj"/>
        <container kind="seqComp" type="Iterator" version="2006B-1" subtype="GNIRS" key="ce02e07c-99b2-42bd-90ea-cd085055592b" name="GNIRS Sequence">
          <paramset name="GNIRS Sequence" kind="dataObj">
            <param name="crossDispersed">
              <value sequence="0">NO</value>
              <value sequence="1">YES</value>
              <value sequence="2">NO</value>
              <value sequence="3">YES</value>
            </param>
          </paramset>
          <container kind="seqComp" type="Observer" version="2009A-1" subtype="observe" key="a608b663-7344-4ae6-b3a0-d492e583141e" name="Observe">
            <paramset name="Observe" kind="dataObj">
              <param name="repeatCount" value="1"/>
            </paramset>
          </container>
        </container>
      </container>
    </container>,
    List(List("NO", "LXD", "NO", "LXD"))
   )

  @Test def testLxd() { verify(lxd) }

  val nested =
    (<container kind="observation" type="Observation" version="2009A-1" subtype="basic" key="ff631cb5-56d0-4864-b26f-e8cf4541beff" name="">
      <paramset name="Observation" kind="dataObj">
        <param name="title" value="GNIRS Observation"/>
        <param name="status" value="PHASE2"/>
      </paramset>
      <container kind="obsComp" type="Instrument" version="2006B-1" subtype="GNIRS" key="dff42615-421e-4fa4-9328-17cf61f221af" name="GNIRS">
        <paramset name="GNIRS" kind="dataObj">
          <param name="exposureTime" value="17.0"/>
          <param name="pixelScale" value="PS_005"/>
          <param name="crossDispersed" value="NO"/>
        </paramset>
      </container>
      <container kind="seqComp" type="Iterator" version="2009A-1" subtype="base" key="e0f126fc-f36f-4d83-af58-82ad9f22ac97" name="Sequence">
        <paramset name="Sequence" kind="dataObj"/>

        <container kind="seqComp" type="Iterator" version="2006B-1" subtype="GNIRS" key="ce02e07c-99b2-42bd-90ea-cd085055592b" name="GNIRS Sequence">
          <paramset name="GNIRS Sequence" kind="dataObj">
            <param name="crossDispersed">
              <value sequence="0">YES</value>
              <value sequence="1">NO</value>
            </param>
          </paramset>
          <container kind="seqComp" type="Iterator" version="2006B-1" subtype="GNIRS" key="ce02e07c-99b2-42bd-90ea-cd085055592c" name="GNIRS Sequence">
            <paramset name="GNIRS Sequence" kind="dataObj">
              <param name="crossDispersed">
                <value sequence="0">NO</value>
                <value sequence="1">YES</value>
              </param>
            </paramset>
            <container kind="seqComp" type="Observer" version="2009A-1" subtype="observe" key="a608b663-7344-4ae6-b3a0-d492e583141e" name="Observe">
              <paramset name="Observe" kind="dataObj">
                <param name="repeatCount" value="1"/>
              </paramset>
            </container>
          </container>
        </container>
      </container>
    </container>,
    List(List("LXD", "NO"), List("NO", "LXD"))
   )

  @Test def testNested() { verify(nested) }

  val sibling =
    (<container kind="observation" type="Observation" version="2009A-1" subtype="basic" key="ff631cb5-56d0-4864-b26f-e8cf4541beff" name="">
      <paramset name="Observation" kind="dataObj">
        <param name="title" value="GNIRS Observation"/>
        <param name="status" value="PHASE2"/>
      </paramset>
      <container kind="obsComp" type="Instrument" version="2006B-1" subtype="GNIRS" key="dff42615-421e-4fa4-9328-17cf61f221af" name="GNIRS">
        <paramset name="GNIRS" kind="dataObj">
          <param name="exposureTime" value="17.0"/>
          <param name="pixelScale" value="PS_005"/>
          <param name="crossDispersed" value="NO"/>
        </paramset>
      </container>
      <container kind="seqComp" type="Iterator" version="2009A-1" subtype="base" key="e0f126fc-f36f-4d83-af58-82ad9f22ac97" name="Sequence">
        <paramset name="Sequence" kind="dataObj"/>
        <container kind="seqComp" type="Iterator" version="2006B-1" subtype="GNIRS" key="ce02e07c-99b2-42bd-90ea-cd085055592b" name="GNIRS Sequence">
          <paramset name="GNIRS Sequence" kind="dataObj">
            <param name="crossDispersed">
              <value sequence="0">NO</value>
              <value sequence="1">YES</value>
              <value sequence="2">NO</value>
              <value sequence="3">YES</value>
            </param>
          </paramset>
          <container kind="seqComp" type="Observer" version="2009A-1" subtype="observe" key="a608b663-7344-4ae6-b3a0-d492e583141e" name="Observe">
            <paramset name="Observe" kind="dataObj">
              <param name="repeatCount" value="1"/>
            </paramset>
          </container>
        </container>
        <container kind="seqComp" type="Iterator" version="2006B-1" subtype="GNIRS" key="ce02e07c-99b2-42bd-90ea-cd085055592b" name="GNIRS Sequence">
          <paramset name="GNIRS Sequence" kind="dataObj">
            <param name="crossDispersed">
              <value sequence="0">YES</value>
              <value sequence="1">NO</value>
            </param>
          </paramset>
          <container kind="seqComp" type="Observer" version="2009A-1" subtype="observe" key="a608b663-7344-4ae6-b3a0-d492e583141e" name="Observe">
            <paramset name="Observe" kind="dataObj">
              <param name="repeatCount" value="1"/>
            </paramset>
          </container>
        </container>
      </container>
    </container>,
    List(List("NO", "LXD", "NO", "LXD"), List("LXD", "NO"))
   )

  @Test def testSibling() { verify(sibling) }

  val none =
    (<container kind="observation" type="Observation" version="2009A-1" subtype="basic" key="ff631cb5-56d0-4864-b26f-e8cf4541beff" name="">
      <paramset name="Observation" kind="dataObj">
        <param name="title" value="GNIRS Observation"/>
        <param name="status" value="PHASE2"/>
      </paramset>
      <container kind="obsComp" type="Instrument" version="2006B-1" subtype="GNIRS" key="dff42615-421e-4fa4-9328-17cf61f221af" name="GNIRS">
        <paramset name="GNIRS" kind="dataObj">
          <param name="exposureTime" value="17.0"/>
          <param name="pixelScale" value="PS_005"/>
          <param name="crossDispersed" value="NO"/>
        </paramset>
      </container>
      <container kind="seqComp" type="Iterator" version="2009A-1" subtype="base" key="e0f126fc-f36f-4d83-af58-82ad9f22ac97" name="Sequence">
        <paramset name="Sequence" kind="dataObj"/>
        <container kind="seqComp" type="Observer" version="2009A-1" subtype="observe" key="a608b663-7344-4ae6-b3a0-d492e583141e" name="Observe">
          <paramset name="Observe" kind="dataObj">
            <param name="repeatCount" value="1"/>
          </paramset>
        </container>
      </container>
    </container>,
    List()
   )

  @Test def testNone() { verify(none) }

  val uptodate =
    (<container kind="observation" type="Observation" version="2009A-1" subtype="basic" key="ff631cb5-56d0-4864-b26f-e8cf4541beff" name="">
      <paramset name="Observation" kind="dataObj">
        <param name="title" value="GNIRS Observation"/>
        <param name="status" value="PHASE2"/>
      </paramset>
      <container kind="obsComp" type="Instrument" version="2010B-2" subtype="GNIRS" key="dff42615-421e-4fa4-9328-17cf61f221af" name="GNIRS">
        <paramset name="GNIRS" kind="dataObj">
          <param name="exposureTime" value="17.0"/>
          <param name="pixelScale" value="PS_005"/>
          <param name="crossDispersed" value="NO"/>
        </paramset>
      </container>
      <container kind="seqComp" type="Iterator" version="2009A-1" subtype="base" key="e0f126fc-f36f-4d83-af58-82ad9f22ac97" name="Sequence">
        <paramset name="Sequence" kind="dataObj"/>

        <container kind="seqComp" type="Iterator" version="2006B-1" subtype="GNIRS" key="ce02e07c-99b2-42bd-90ea-cd085055592b" name="GNIRS Sequence">
          <paramset name="GNIRS Sequence" kind="dataObj">
            <param name="crossDispersed">
              <value sequence="0">XXX</value>
              <value sequence="1">ILLEGAL</value>
            </param>
          </paramset>
          <container kind="seqComp" type="Observer" version="2009A-1" subtype="observe" key="a608b663-7344-4ae6-b3a0-d492e583141e" name="Observe">
            <paramset name="Observe" kind="dataObj">
              <param name="repeatCount" value="1"/>
            </paramset>
          </container>
        </container>
      </container>
    </container>,
    List(List("XXX", "ILLEGAL"))
   )

  // Up-to-date so nothing is done.  XXX and ILLEGAL aren't modified
  @Test def testUptodate() { verify(uptodate) }


  private def xlat(orig: Node): Seq[Seq[String]] = {
    // Read it into a Pio Container.  Excuse the cast please.
    val node = PioXmlUtil.read(new StringReader(orig.toString()))
    val container: Container = node.asInstanceOf[Container]

    // Convert all GNIRS sequences that it contains
    ToGnirsAtGn.instance.update(container)

    // Turn it into a Scala node sequence
    val updated = scala.xml.XML.loadString(PioXmlUtil.toXmlString(container))

    // Extract all the GNIRS sequence values into a List of List of String,
    // where each value is the expected translated value.

    // First, get the paramset for the GNIRS Sequence
    val seq = (updated \\ "paramset").filter(n => (n \ "@name").text == "GNIRS Sequence")

    // For each, map its values to a List of String.
    seq.map(n => (n \\ "value").map(_.text))
  }

  private def verify(tup: (Node, List[List[String]])) =
    assertEquals(tup._2, xlat(tup._1))

}