package edu.gemini.model.p1.targetio.impl

import edu.gemini.model.p1.immutable.Target
import edu.gemini.model.p1.targetio.api._
import edu.gemini.model.p1.targetio.api.FileType._

import java.io.File

import org.junit.Assert._
import org.junit.{Ignore, Test}

abstract class WriterTestBase[T <: Target](val reader: TargetReader[T], val writer: TargetWriter[T]) {

  protected def io(targets: List[T], ftype: FileType) {
    val f = File.createTempFile("targets", ".%s".format(ftype.extension))
    f.deleteOnExit()
    assertTrue(writer.write(targets, f, ftype).isRight)
    reader.read(f) match {
      case Left(err)  => fail(err.msg)
      case Right(lst) => validateTargets(targets, lst.map(_.right.get))
    }
  }

  protected def validateTargets(expected: List[T], actual: List[T]) {
    assertEquals(expected.size, actual.size)
    expected.zip(actual) foreach {
      case (ex, ac) => validateTarget(ex, ac)
    }
  }

  protected def validateTarget(expected: T, actual: T)

  protected def mkTargets: List[T]

  protected def io(ftype: FileType) { io(mkTargets, ftype) }

  @Test def testFits() { io(Fits) }
  @Test def testEmptyFits() { io(Nil, Fits) }

  @Test def testCsv() { io(Csv) }
  @Ignore("STIL bug?") @Test def testEmptyCsv() { io(Nil, Csv) }

  @Test def testVo() { io(Vo) }
  @Test def testEmptyVo() { io(Nil, Vo) }

  @Test def testTst() { io(Tst) }
  @Test def testEmptyTst() { io(Nil, Vo) }


}