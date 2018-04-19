package edu.gemini.ictd.dao

import doobie.imports._
import edu.gemini.spModel.core.Site

//mysql> select * from Site;
//+------+---------------+
//| Site | ActiveCutSite |
//+------+---------------+
//| GN   |             1 |
//| GS   |             1 |
//+------+---------------+

trait SiteMeta {

  implicit val SiteMeta: Meta[Site] =
    Meta[String].xmap(Site.parse, _.abbreviation)

}

object SiteMeta extends SiteMeta
