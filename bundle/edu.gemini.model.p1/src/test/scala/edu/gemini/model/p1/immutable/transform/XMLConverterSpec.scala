package edu.gemini.model.p1.immutable.transform

import org.specs2.mutable.SpecificationWithJUnit
import edu.gemini.model.p1.immutable.transform.XMLConverter._
import org.specs2.scalaz.ValidationMatchers._
import xml.Atom
import edu.gemini.model.p1.immutable.{SemesterProperties, Semester}

import scalaz._
import Scalaz._

class XMLConverterSpec extends SpecificationWithJUnit with SemesterProperties {
  "StepResult" should {
    "have  a zero" in {
      mzero[StepResult] must beEqualTo(StepResult(Nil, StepResult.ZeroNode))
    }
    "support addition" in {
      StepResult(List("a", "b"), new Atom("A")) |+| mzero[StepResult] must beEqualTo(StepResult(List("a", "b"), new Atom("A")))
      mzero[StepResult] |+| StepResult(List("a", "b"), new Atom("A")) must beEqualTo(StepResult(List("a", "b"), new Atom("A")))
      StepResult(List("a", "b"), new Atom("A")) |+| StepResult(List("b", "c"), new Atom("B")) must beEqualTo(StepResult(List("a", "b", "b", "c"), new Atom("B")))
    }
    "support joining as validations" in {
      val vl1 = StepResult(List("a", "b"), new Atom("A")).successNel[String]
      val vl2 = StepResult(List("c", "d"), new Atom("B")).successNel[String]
      val s = List(vl1, vl2)
      StepResult.join(s) must beSuccessful(StepResult(List("a", "b", "c", "d"), Seq(new Atom("A"), new Atom("B"))))
    }
    "support flattening validations" in {
      val vl1 = StepResult(List("a", "b"), new Atom("A")).successNel[String]
      val vl2 = StepResult(List("c", "d"), new Atom("B")).successNel[String]
      val s = List(vl1, vl2)
      StepResult.flatten(s) must beSuccessful(StepResult(List("a", "b", "c", "d"), new Atom("B")))
    }
  }
  "XMLConverter" should {
    implicit val current = Semester.current

    val semesterTransformToCurrent:TransformFunction = {
      case <semester>{ns @ _*}</semester>  => StepResult(s"Updated semester to ${current.display}" :: Nil, <semester year={current.year.toString} half={current.half.toString}>{ns}</semester>).successNel
    }
    val proposalTransformVersion:TransformFunction = {
      case <proposal>{ns @ _*}</proposal>  => StepResult("Updated schema version to 2.0" :: Nil, <proposal version="2.0">{ns}</proposal>).successNel
    }
    val proposalAndTransformVersion:TransformFunction = {
      case <proposal>{ns @ _*}</proposal>  => StepResult("Updated schema version to 2.0 and eaten the semester child" :: Nil, <proposal version="2.0"></proposal>).successNel
    }
    val withFailure:TransformFunction = {
      case <proposal>{ns @ _*}</proposal>  => "Error message".failNel
    }
    "preserve non-convertive node" in {
      val node = new Atom("A")
      val transformed = XMLConverter.transform(node, semesterTransformToCurrent)
      transformed must beSuccessful.like {
        case StepResult(Nil, n) => n must beEqualTo(node)
      }
    }
    "convert single nodes not processed" in {
      val node = <proposal/>
      val transformed = XMLConverter.transform(node, semesterTransformToCurrent)
      transformed must beSuccessful.like {
        case StepResult(Nil, n) => n must beEqualTo(node)
      }
    }
    "convert nodes with children not processed" in {
      val node = <proposal><a attr=""></a></proposal>
      val transformed = XMLConverter.transform(node)
      transformed must beSuccessful.like {
        case StepResult(Nil, n) => n must beEqualTo(node)
      }
    }
    "convert with top node processed" in {
      val node = <semester year="2014" half="A"/>
      val transformed = XMLConverter.transform(node, semesterTransformToCurrent)
      transformed must beSuccessful.like {
        case StepResult(changes, n) => {
          changes must have length 1
          changes must contain(s"Updated semester to ${Semester.current.display}")
          n must ==/(<semester year={current.year.toString} half={current.half.toString}/>)
        }
      }
    }
    "convert with top node processed and children" in {
      val node = <semester year="2013" half="A"><child></child></semester>
      val transformed = XMLConverter.transform(node, semesterTransformToCurrent)
      transformed must beSuccessful.like {
        case StepResult(changes, n) => {
          changes must have length 1
          changes must contain(s"Updated semester to ${Semester.current.display}")
          n must ==/(<semester year={current.year.toString} half={current.half.toString}><child></child></semester>)
        }
      }
    }
    "convert with top node processed and deep child" in {
      val node = <semester year="2014" half="A"><child><child1 a="abc"></child1></child></semester>
      val transformed = XMLConverter.transform(node, semesterTransformToCurrent)
      transformed must beSuccessful.like {
        case StepResult(changes, n) => {
          changes must have length 1
          changes must contain(s"Updated semester to ${Semester.current.display}")
          n must ==/(<semester year={current.year.toString} half={current.half.toString}><child><child1 a="abc"></child1></child></semester>)
        }
      }
    }
    "convert with top node processed and several children" in {
      val node = <semester year="2013" half="A"><child></child><child></child></semester>
      node.child must have length 2
      val transformed = XMLConverter.transform(node, semesterTransformToCurrent)
      transformed must beSuccessful.like {
        case StepResult(changes, n) => {
          changes must have length 1
          changes must contain(s"Updated semester to ${Semester.current.display}")
          n.head.child must have length 2
          n must ==/(<semester year={current.year.toString} half={current.half.toString}><child></child><child></child></semester>)
        }
      }
    }
    "convert with top node processed, several children and deeper nodes" in {
      val node = <semester year="2013" half="A"><child><child1 a="abc"></child1></child><child><child2 a="abc"></child2></child></semester>
      node.child must have length 2
      val transformed = XMLConverter.transform(node, semesterTransformToCurrent)
      transformed must beSuccessful.like {
        case StepResult(changes, n) => {
          changes must have length 1
          changes must contain(s"Updated semester to ${Semester.current.display}")
          n.head.child must have length 2
          n must ==/(<semester year={current.year.toString} half={current.half.toString}><child><child1 a="abc"></child1></child><child><child2 a="abc"></child2></child></semester>)
        }
      }
    }
    "convert with children node processed" in {
      val node = <proposal><semester year="2013" half="A"></semester></proposal>
      node.child must have length 1
      val transformed = XMLConverter.transform(node, semesterTransformToCurrent)
      transformed must beSuccessful.like {
        case StepResult(changes, n) => {
          changes must have length 1
          changes must contain(s"Updated semester to ${Semester.current.display}")
          n must ==/(<proposal><semester year={current.year.toString} half={current.half.toString}></semester></proposal>)
        }
      }
    }
    "convert with children node processed and extra children" in {
      val node = <proposal><child1></child1><semester year="2013" half="A"><child2></child2></semester></proposal>
      node.child must have length 2
      val transformed = XMLConverter.transform(node, semesterTransformToCurrent)
      transformed must beSuccessful.like {
        case StepResult(changes, n) => {
          changes must have length 1
          changes must contain(s"Updated semester to ${Semester.current.display}")
          n must ==/(<proposal><child1></child1><semester year={current.year.toString} half={current.half.toString}><child2></child2></semester></proposal>)
        }
      }
    }
    "convert siblings with multiple processors" in {
      val node = <root><proposal version="1.0"></proposal><semester year="2013" half="A"></semester></root>
      node.child must have length 2
      val transformed = XMLConverter.transform(node, proposalTransformVersion, semesterTransformToCurrent)
      transformed must beSuccessful.like {
        case StepResult(changes, n) => {
          changes must have length 2
          changes must contain("Updated schema version to 2.0")
          changes must contain(s"Updated semester to ${Semester.current.display}")
          n must ==/(<root><proposal version="2.0"></proposal><semester year={current.year.toString} half={current.half.toString}></semester></root>)
        }
      }
    }
    "convert parent/children with multiple processors" in {
      val node = <proposal version="1.0"><semester year="2013" half="A"></semester></proposal>
      val transformed = XMLConverter.transform(node, proposalTransformVersion, semesterTransformToCurrent)
      transformed must beSuccessful.like {
        case StepResult(changes, n) => {
          changes must have length 2
          changes must contain("Updated schema version to 2.0")
          changes must contain(s"Updated semester to ${Semester.current.display}")
          n must ==/(<proposal version="2.0"><semester year={current.year.toString} half={current.half.toString}></semester></proposal>)
        }
      }
    }
    "convert parent/children with multiple processors and a failure" in {
      val node = <proposal version="1.0"><semester year="2013" half="A"></semester></proposal>
      val transformed = XMLConverter.transform(node, proposalTransformVersion, semesterTransformToCurrent, withFailure)
      transformed must beFailing.like {
        case e:NonEmptyList[String] => {
          e.list must have size 1
          e.list must contain("Error message")
        }
      }
    }
    "convert parent/children when the parent transform transform the children" in {
      val node = <proposal version="1.0"><semester year="2013" half="B"></semester></proposal>
      val transformed = XMLConverter.transform(node, proposalAndTransformVersion)
      transformed must beSuccessful.like {
        case StepResult(changes, n) => {
          changes must have length 1
          changes must contain("Updated schema version to 2.0 and eaten the semester child")

          n must ==/(<proposal version="2.0"></proposal>)
        }
      }
    }
  }
}