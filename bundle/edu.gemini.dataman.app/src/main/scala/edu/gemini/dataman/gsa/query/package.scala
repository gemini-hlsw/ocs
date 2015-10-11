package edu.gemini.dataman.gsa

import scalaz.\/

package object query {

  type GsaResponse[A] = GsaQueryError \/ A

}
