package edu.gemini.pit.catalog


package object votable {

  type Elem = scala.xml.Elem
  type Row = List[String]
  
  implicit class pimpElem(val e:Elem) extends AnyVal {
    def attr(s:String) = attrOption(s).getOrElse(sys.error("%s: attr '%s' not found".format(e.label, s)))
    def attrOption(s:String) = (e \ ("@" + s)).headOption.map(_.text.trim)
    def elems(s:String) = (e \ s).toList.map(n => Option(n.asInstanceOf[Elem])).flatten
    def elem(s:String) = elems(s) match {
      case e0 :: Nil => e0
      case es => sys.error("%s: expected elem '%s' to occur exactly once; found %d".format(e.label, s, es.length))
    }
    def elemOption(s:String) = elems(s) match {
      case e0 :: Nil => Some(e0)
      case Nil => None
      case es => sys.error("%s: expected elem '%s' to occur at most once; found %d".format(e.label, s, es.length))
    }
  }
  
}