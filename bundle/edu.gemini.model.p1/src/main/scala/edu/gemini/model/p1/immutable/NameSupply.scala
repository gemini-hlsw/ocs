package edu.gemini.model.p1.immutable

class NameSupply[A](prefix:String) {

  private var map:Map[A, Int] = Map.empty

  def nameOf(a:A):String = synchronized {
    map.get(a) match {
      case Some(i) => "%s-%s".format(prefix, i)
      case None    =>
        map += (a -> map.size)
        nameOf(a)
    }
  }

}

