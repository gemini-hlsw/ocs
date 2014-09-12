package edu.gemini.sp.vcs.log.impl

import java.io.File
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.sp.vcs.log._
import java.sql._
import scala.slick.driver.H2Driver.simple._
import edu.gemini.util.security.principal.{StaffPrincipal, UserPrincipal, GeminiPrincipal}
import annotation.tailrec
import scala.util.{Failure, Success, Try}
import PersistentVcsUtil._
import scalaz.std.tuple._
import scalaz.syntax.bifunctor._
import java.util.logging.Logger
import scala.slick.jdbc.{StaticQuery => Q}

final class PersistentVcsLog(dir: File) extends VcsLogEx with PersistentVcsSchema {

  lazy val Log = Logger.getLogger(classOf[PersistentVcsLog].getName)
  final val Anonymous: Set[GeminiPrincipal] = Set(UserPrincipal("Anonymous"))

  // as an experiment we will try a connection pool here
  private def dataSource(url: String): Database = {
    val cpds = new com.mchange.v2.c3p0.ComboPooledDataSource();
    cpds.setDriverClass("org.h2.Driver");
    cpds.setJdbcUrl(url);
//    cpds.setUser("sa");
//    cpds.setPassword("");
    Database.forDataSource(cpds)
  }

  // Construct a database connection and create/update our schema on first use.
  lazy val database = {
    val path = dir.getAbsolutePath
    require(dir.mkdirs() || dir.isDirectory, s"Not a valid directory: ${path}")
    val db = dataSource(s"jdbc:h2:${path};DB_CLOSE_ON_EXIT=FALSE;TRACE_LEVEL_FILE=4") //, driver = "org.h2.Driver")
    db.withSession((s: Session) => checkSchema(path)(s))
    db
  }

  // VCSLOG IMPLEMENTATION

  // VcsLog.log implementation
  def log(op: VcsOp, pid: SPProgramID, principals: Set[GeminiPrincipal]): VcsEvent =
    log(op, System.currentTimeMillis, pid, principals)

  // VcsLogEx.log
  def log(op: VcsOp, time:Long, pid: SPProgramID, principals: Set[GeminiPrincipal]): VcsEvent =
    database.withSession((s: Session) => doLog(op, new java.sql.Timestamp(time), pid, principals)(s))

  // VcsLog.selectByProgram implementation
  def selectByProgram(pid: SPProgramID, offset: Int, size: Int): (List[VcsEventSet], Boolean) =
    database.withSession((s: Session) => doSelectByProgram(pid, offset, size)(s))

  def archive(f: File): Unit = {
    val p = f.getAbsolutePath
    Log.info("Archiving VCS log to " + p)
    database.withSession((s: Session) => Q.update[String]("BACKUP TO ?").execute(p)(s))
  }

  // HELPERS

  // Log implementation. Insert the event, insert the principals, hook them up, read it back.
  def doLog(op: VcsOp, time:Timestamp, pid: SPProgramID, _principals: Set[GeminiPrincipal])(implicit s: Session): VcsEvent = {
    // OCSINF-118: if the principal set is empty, add an anonymous principal
    val principals = if (_principals.isEmpty) Anonymous else _principals
    val ids = principals.map(insertPrincipal)
    val eid = insertEvent(op, time, pid, setHash(ids.map(_.n)))
    ids.foreach(id => EVENT_PRINCIPAL.insert((eid, id)))
    selectEvent(eid)
  }

  // Ok this sucks because ALL of these types should be inferrable. These are the column and output types for the
  // Event x Principal table join. Hopefully we can fix this because it shouldn't be a problem.
  type E = ((Column[Id[VcsEvent]], Column[VcsOp], Column[Timestamp], Column[SPProgramID], Column[String]), (Column[String], Column[String]))
  type U = ((Id[VcsEvent], VcsOp, Timestamp, SPProgramID, String), (String, String))

  // To select by program we join with the principal table and stream results back, chunking by program and principals
  // and then decoding into a stream of event sets. We can then drop the offset and take the size.
  def doSelectByProgram(pid: SPProgramID, offset: Int, size: Int)(implicit s: Session): (List[VcsEventSet], Boolean) = {

    // Ignore the red underlines. This is correct.
    val query: Query[E, U] = {
      val select = for {
        e <- EVENT if e.pid === pid
        j <- EVENT_PRINCIPAL if j.eventId === e.id
        p <- PRINCIPAL if p.id === j.principalId
      } yield ((e.id, e.operator, e.timestamp, e.pid, e.principalHash), (p.clazz, p.name))
      select.sortBy(_._1._1.desc)
    }

    // Let's chunk by pid and principalHash where the timestamps differ by TimeSlice or less.
    val TimeSlice = 1000 * 60 * 60 // 1 hour
    query.elements.use((it: Iterator[U]) =>
      it.toStream
        .chunked { case (((_, _, ts0, pid0, ph0), _), ((_, _, ts1, pid1, ph1), _)) =>
          (ts1.getTime - ts0.getTime < TimeSlice) && (pid0 == pid1) && (ph0 == ph1)
        }.map(decode2)
        .drop(offset)
        .splitAt(size)
        .bimap(_.toList, !_.isEmpty))

  }

  // Selecting a single event is a special case of the above, and uses the same decoder.
  def selectEvent(id: Id[VcsEvent])(implicit s: Session): VcsEvent = {
    val q: Query[E, U] = for {
      e <- EVENT if e.id === id
      j <- EVENT_PRINCIPAL if j.eventId === e.id
      p <- PRINCIPAL if p.id === j.principalId
    } yield ((e.id, e.operator, e.timestamp, e.pid, e.principalHash), (p.clazz, p.name))
    decode(q.list)
  }

  // Decode a chunk of rows, which must be uniform and no-empty.
  def decode(chunk: Traversable[U]): VcsEvent = {
    val ps = chunk.map(_._2).map(p => GeminiPrincipal(p._1, p._2))
    chunk.head._1 match {
      case (id, op, ts, pid, _) => VcsEvent(id.n, op, ts.getTime, pid, ps.toSet)
    }
  }

  // Decode a chunk of rows, which must be uniform and no-empty.
  def decode2(chunk: Stream[U]): VcsEventSet = {

    // Pull rollup data out of the chunk
    val ids:Set[Int] = chunk.map(_._1._1.n).toSet
    val ops:Map[VcsOp, Int] = chunk.map(_._1._2).groupBy(identity).mapValues(_.length)
    val tss:Set[Long] = chunk.map(_._1._3.getTime).toSet
    val pid:SPProgramID = chunk.head._1._4
    val gps:Set[GeminiPrincipal] = chunk.map(_._2).map(p => GeminiPrincipal(p._1, p._2)).toSet

    // And construct our event set!
    VcsEventSet(
      ids.min to ids.max,
      ops.toSeq.toMap, // Hack: ops is actually a MapLike and isn't serializable
      (tss.min, tss.max),
      pid,
      gps)

  }

  // Insert the event and return its Id
  def insertEvent(op: VcsOp, time: Timestamp, pid: SPProgramID, principalHash:String)(implicit s: Session): Id[VcsEvent] =
    EVENT.create.insert((op, time, pid, principalHash))

  // Canonicalize a principal. To be more efficient we do the lookup first, and if that fails we insert. This means we
  // there's a race we need to handle.
  @tailrec def insertPrincipal(p: GeminiPrincipal)(implicit s: Session): Id[GeminiPrincipal] =
    Try(lookupPrincipal(p).getOrElse(PRINCIPAL.create.insert((p.clazz, p.getName)))) match {
      case Success(id) => id
      case Failure(e: SQLException) if e.getErrorCode == DUPLICATE_KEY => insertPrincipal(p)(s)
      case Failure(e) => throw e
    }

  // Look up a principla by name and class.
  def lookupPrincipal(gp: GeminiPrincipal)(implicit s: Session): Option[Id[GeminiPrincipal]] =
    PRINCIPAL.filter(p => p.clazz === gp.clazz && p.name === gp.getName).map(_.id).firstOption

}


object Test extends App {

  val log = new PersistentVcsLog(new File("/tmp/testdb/foo"))

  import log._

  (VERSION.ddl ++ PRINCIPAL.ddl ++ EVENT.ddl ++ EVENT_PRINCIPAL.ddl).createStatements.foreach(println)

  val pid = SPProgramID.toProgramID("GS-2008A-Q-1")
  val e = log.log(OpFetch, pid, Set[GeminiPrincipal](StaffPrincipal("Gemini"), UserPrincipal("bob@dole.com")))

  println(e)

  // Pages of size 2

  val pageSize = 2
  for (i <- 0 to 10) {
    println(s"page $i")
    val (es, b) = log.selectByProgram(pid, i * pageSize, pageSize)
    es.foreach(e => println(s"  $e"))
    println(s"  more? $b")
  }

}


