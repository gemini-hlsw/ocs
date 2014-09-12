package edu.gemini.spModel.util

import org.junit.Test
import org.junit.Assert._

final class VersionTokenUtilTest {

  private def data[A](ds: (String, A)*): List[(VersionToken, A)] =
    ds.map { case (vt, a) => VersionToken.valueOf(vt) -> a }.toList

  private def normalize[A](ds: (String, A)*): List[(VersionToken, A)] =
    VersionTokenUtil.normalize(data(ds: _*))

  private def matches[A](actual: List[(VersionToken, A)], expected: (String, A, String)*): Unit = {
    actual.zip(expected).foreach {
      case ((actualToken, actualA), (expectedTokenString, expectedA, expectedNext)) =>

      assertEquals(expectedTokenString, actualToken.toString)
      assertEquals(expectedA, actualA)
      assertEquals(expectedNext, actualToken.next().toString)
    }

    assertEquals(expected.size, actual.size)
  }

  @Test def normalize1() {
    val res = normalize(("1", 'a'), ("1.1", 'b'), ("1.1", 'c'))
    matches(res, ("1", 'a', "1.3"), ("1.1", 'b', "1.1.1"), ("1.2", 'c', "1.2.1"))
  }

  @Test def normalizeSortOrder() {
    val res = normalize(("7.1", 'a'), ("3.2", 'b'), ("3.1", 'c'), ("8.1.1", 'd'))
    matches(res, ("3.1", 'c', "3.1.1"), ("3.2", 'b', "3.2.1"), ("7.1", 'a', "7.1.1"), ("8.1.1", 'd', "8.1.1.1"))
  }

  @Test def normalizeNoParent() {
    val res = normalize(("1.1", 'b'), ("1.1", 'c'))
    matches(res, ("1.1", 'b', "1.1.1"), ("1.2", 'c', "1.2.1"))
  }

  @Test def normalizeWithChildren() {
    val res = normalize(("1.1", 'b'), ("1.1", 'c'), ("1.1.1", 'd'))
    matches(res, ("1.1", 'b', "1.1.2"), ("1.1.1", 'd', "1.1.1.1"), ("1.2", 'c', "1.2.1"))
  }

  @Test def normalizeNoCommonParent() {
    val res = normalize(("1", 'a'), ("1", 'b'))
    matches(res, ("1", 'a', "1.1"), ("2", 'b', "2.1"))
  }

  @Test def removeDuplicates() {
    val res = normalize(("1", 'a'), ("1", 'a'))
    matches(res, ("1", 'a', "1.1"))
  }

  @Test def testMergeNoDuplicates() {
    val d0 = data(("1", 'a'), ("1.1", 'b'), ("1.2", 'c'))
    val d1 = data(("2", 'd'), ("2.1", 'e'))

    val res = VersionTokenUtil.merge(d0, d1)
    matches(res, ("1", 'a', "1.3"), ("1.1", 'b', "1.1.1"), ("1.2", 'c', "1.2.1"), ("2", 'd', "2.2"), ("2.1", 'e', "2.1.1"))
  }

  @Test def testMergeDuplicate() {
    val d0 = data(("1", 'a'), ("1.1", 'b'))
    val d1 = data(("1", 'a'), ("1.1", 'c'))

    val res = VersionTokenUtil.merge(d0, d1)
    matches(res, ("1", 'a', "1.3"), ("1.1", 'b', "1.1.1"), ("1.2", 'c', "1.2.1"))
  }

  @Test def testMergeRenumberChildren() {
    val d0 = data(("1", 'a'), ("1.1", 'b'))
    val d1 = data(("1.1", 'c'), ("1.1.1", 'd'))

    val res = VersionTokenUtil.merge(d0, d1)
    matches(res, ("1", 'a', "1.3"), ("1.1", 'b', "1.1.1"), ("1.2", 'c', "1.2.2"), ("1.2.1", 'd', "1.2.1.1"))
  }

  @Test def testMergeReplaceParent() {
    val d0 = data(("1.1", 'b'))
    val d1 = data(("1", 'a'), ("1.1", 'c'), ("1.1.1", 'd'))

    val res = VersionTokenUtil.merge(d0, d1)
    matches(res, ("1", 'a', "1.3"), ("1.1", 'b', "1.1.1"), ("1.2", 'c', "1.2.2"), ("1.2.1", 'd', "1.2.1.1"))
  }
}
