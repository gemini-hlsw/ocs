package edu.gemini.spModel.dataset

import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import java.io.{ByteArrayInputStream, ObjectInputStream, ByteArrayOutputStream, ObjectOutputStream}


class DatasetMd5Spec extends Specification with ScalaCheck with Arbitraries {
  "DatasetMd5" should {
    "work with any byte array and round-trip to/from String correctly" in {
      forAll { (a: Array[Byte]) =>
        val md0 = new DatasetMd5(a)
        DatasetMd5.parse(md0.hexString).exists { md1 =>
          md0 == md1 && md0.hashCode == md1.hashCode
        }
      }
    }

    "fail cleanly for invalid input" in {
      // the hex binary spec parses pairs of hex digits into bytes
      DatasetMd5.parse("abc") must_== None
    }

    "be serializable" in {
      forAll { (md5: DatasetMd5) =>
        // i suppose actually one example is enough ...
        val baos = new ByteArrayOutputStream()
        val oos  = new ObjectOutputStream(baos)
        oos.writeObject(md5)
        oos.close()

        val ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray))
        ois.readObject() match {
          case x: DatasetMd5 => md5 == x
          case _             => false
        }
      }
    }
  }
}
