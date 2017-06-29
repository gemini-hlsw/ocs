package edu.gemini.spModel.io.ocs3

import edu.gemini.pot.sp._
import edu.gemini.pot.sp.SPComponentType.{ITERATOR_BASE, OBSERVATION_BASIC}
import edu.gemini.pot.spdb.{DBAbstractFunctor, IDBDatabaseService}
import edu.gemini.spModel.gemini.obscomp.SPProgram
import edu.gemini.spModel.io.{PioDocumentBuilder, SequenceOutputService}
import edu.gemini.spModel.io.PioSyntax._
import edu.gemini.spModel.pio.{Document, Container, Pio}
import edu.gemini.spModel.pio.xml.{PioXmlFactory, PioXmlUtil}
import edu.gemini.spModel.rich.pot.sp._

import org.dom4j.Element
import org.dom4j.io.{OutputFormat, XMLWriter}

import java.io.StringWriter
import java.security.Principal
import java.util.Set

import scala.collection.JavaConverters._

import scalaz._
import Scalaz._

import Ocs3ExportFunctor._

/** An ODB "functor" that obtains an XML string representation of a program,
  * group, or observation that is suitable for ingestion into OCS3.  In
  * particular, it replaces the sequence hierarchy with the sequence XMl as
  * exported by the WDBA and renames the containers exported by ordinary
  * PIO.  The goal is to simplify reading the document in OCS3 since it does
  * not include any OCS2 classes or their dependencies.
  */
final class Ocs3ExportFunctor(format: ExportFormat) extends DBAbstractFunctor {

  val fact = new PioXmlFactory

  var result: Option[String] = None

  override def execute(db: IDBDatabaseService, n: ISPNode, ps: Set[Principal]): Unit = {
    val doc = PioDocumentBuilder.instance.toDocument(n)

    result = format match {
      case ExportFormat.Ocs3 => mutateToOcs3(doc, n)
      case ExportFormat.Pio  => wrapWithProg(doc, n)
    }
  }

  private def mutateToOcs3(doc: Document, n: ISPNode): Option[String] = {
    def mapObs(obsList: Traversable[ISPObservation]): Map[SPNodeKey, ISPObservation] =
      obsList.map(o => o.getNodeKey -> o).toMap

    val nodeMap: Map[SPNodeKey, ISPObservation] =
      n match {
        case p: ISPProgram =>
          // To my surprise, template observations aren't included in
          // MemProgram's getAllObservations results.
          mapObs(p.allObservationsIncludingTemplateObservations)

        case oc: ISPObservationContainer =>
          mapObs(oc.getAllObservations.asScala)

        case o: ISPObservation =>
          Map(o.getNodeKey -> o)

        case _ =>
          Map.empty
      }

    // Go through all the observation containers removing the root sequence
    // node and replacing it with sequence xml in a new param set.
    doc.allContainers.foreach { c =>
      c.nodeKey.foreach { k =>
        nodeMap.get(k).foreach { o =>
          // Find the sequence root.
          c.findContainers(ITERATOR_BASE).foreach { c.removeChild }

          // Add it as an XML element to the observation.
          val seqXml = SequenceOutputService.instance.toSequenceXml(o, false)
          PioXmlUtil.toElement(c).add(seqXml.getRootElement)
        }
      }

      // Rename the "paramset" containing the data object to "data", remove
      // the useless "name" and "kind" attributes.
      c.dataObject.foreach { ps =>
        val xml  = PioXmlUtil.toElement(ps)
        xml.setName("data")
        xml.rmAttrs("name", "kind")
      }

      // We will rename the element from "container" to its type, except for obs
      // log, template, and note because there are different kinds of these.
      import edu.gemini.pot.sp.SPComponentBroadType.{INFO, OBSLOG, TEMPLATE}
      val name = c.getType match {
        case INFO.value                    => c.getSubtype
        case OBSLOG.value | TEMPLATE.value => c.getKind
        case x                             => x.toLowerCase
      }
      val xml  = PioXmlUtil.toElement(c)
      xml.setName(name)

      // Remove useless attributes but differentiate "instrument" on its
      // subtype.
      val sub  = c.getSubtype
      xml.rmAttrs("type", "kind", "key", "version", "subtype")
      if (name == "instrument") {
        xml.addAttribute("type", sub)
      }
    }

    // Write just the "program" or "observation" element.
    PioXmlUtil.toElement(doc).elementList.headOption.map(_.xmlString)
  }

  // Wraps the XML for the node inside of a program so that it can be imported
  // easily by the client.
  private def wrapWithProg(doc: Document, n: ISPNode): Option[String] = {
    n match {
      case _: ISPProgram => // do nothing
      case _             =>
        val f  = new PioXmlFactory
        val p  = n.getProgram

        // Make the program container
        val c  = f.createContainer("program", "Program", SPProgram.VERSION)
        c.setSubtype("basic")
        c.setKey(p.getNodeKey.toString)
        c.setName(p.getProgramID.stringValue)

        // Add the data object
        c.addParamSet(p.getDataObject.getParamSet(f))

        // Remove children from the document.
        val children = doc.getContainers.asInstanceOf[java.util.List[Container]].asScala
        children.foreach(doc.removeChild)

        // Add them to the program container.
        children.foreach(c.addContainer)

        // Add the program container to the document.
        doc.addContainer(c)
    }

    Some(PioXmlUtil.toElement(doc).xmlString)
  }
}

object Ocs3ExportFunctor {
  implicit class Dom4jElementOps(xml: Element) {

    def rmAttr(n: String): Unit =
      Option(xml.attribute(n)).foreach { xml.remove }

    def rmAttrs(ns: String*): Unit =
      ns.foreach(rmAttr)

    def elementList: List[Element] =
      xml.elements.asScala.toList.collect {
        case e: Element => e
      }

    def xmlString: String = {
      val sw = new StringWriter
      val ft = new OutputFormat("  ", true, "UTF-8")
      val xw = new XMLWriter(sw, ft)
      xw.write(xml)
      sw.toString
    }
  }
}
