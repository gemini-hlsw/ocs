package edu.gemini.sp.vcs.log.impl

import edu.gemini.util.security.principal._
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.sp.vcs.log._
import scala.slick.lifted.MappedTypeMapper
import edu.gemini.util.security.principal.AffiliatePrincipal
import edu.gemini.util.security.principal.ProgramPrincipal
import edu.gemini.util.security.principal.UserPrincipal
import edu.gemini.util.security.principal.AffiliatePrincipal
import edu.gemini.util.security.principal.ProgramPrincipal
import edu.gemini.util.security.principal.UserPrincipal

import scala.reflect.runtime.universe.TypeTag
import doobie.imports._


// A typesafe Id
case class Id[A](n: Int)

// And a companion with a type mapper
object Id {
  implicit def mapper[A] = MappedTypeMapper.base[Id[A], Int](_.n, Id(_))

  // N.B. the type tag constraint will go away in doobie 0.3
  implicit def idMeta[A](implicit ev: TypeTag[Id[A]]): Meta[Id[A]] = 
    Meta[Int].xmap(Id(_), _.n)

}

/**
 * Some helpers for mapping VCS model objects to database columns.
 */
object PersistentVcsMappers {

  // Indirection for principal type names
  val Affiliate = "Affiliate"
  val Program   = "Program"
  val User      = "User"
  val Staff     = "Staff"
  val Visitor   = "Visitor"

  // Clazz extractor
  implicit class EnhanceGP(p: GeminiPrincipal) {
    def clazz: String = p match {
      case AffiliatePrincipal(n) => Affiliate
      case ProgramPrincipal(pid) => Program
      case UserPrincipal(n)      => User
      case StaffPrincipal(n)     => Staff
      case VisitorPrincipal(n)   => Visitor
    }
  }

  // Clazz constructor
  implicit class EnhanceGPCompanion(gp: GeminiPrincipal.type) {

    // We may read the same principal many times, so memoize it to keep the graph small. The total number can easily
    // fit it memory at the moment, so we won't worry about cleaning up for now.
    private val memo: collection.mutable.Map[(String, String), GeminiPrincipal] = collection.mutable.Map()

    def apply(clazz: String, name: String): GeminiPrincipal = memo.getOrElseUpdate((clazz, name),
      clazz match {
        case Affiliate => AffiliatePrincipal(edu.gemini.spModel.core.Affiliate.fromString(name)) // TODO: this is unsafe
        case Program   => ProgramPrincipal(SPProgramID.toProgramID(name))
        case User      => UserPrincipal(name)
        case Staff     => StaffPrincipal(name)
        case Visitor   => VisitorPrincipal(SPProgramID.toProgramID(name))
      }
    )

  }

  // Operations are mapped to strings. Indirection here decouples the names.
  implicit val VcsOpMapper =
    MappedTypeMapper.base[VcsOp, String]({
      case OpFetch => "Fetch"
      case OpStore => "Store"
    }, {
      case "Fetch" => OpFetch
      case "Store" => OpStore
    })

  // Operations are mapped to strings. Indirection here decouples the names.
  implicit val VcsOpMeta: Meta[VcsOp] =
    Meta[String].nxmap({
      case "Fetch" => OpFetch
      case "Store" => OpStore
    }, {
      case OpFetch => "Fetch"
      case OpStore => "Store"
    })

  // Program ids are mapped to strings
  implicit val SPProgramIdMapper =
    MappedTypeMapper.base[SPProgramID, String](_.toString, SPProgramID.toProgramID(_))

  // Program ids are mapped to strings
  implicit val SPProgramIDMeta: Meta[SPProgramID] =
    Meta[String].nxmap(SPProgramID.toProgramID, _.toString)

}
