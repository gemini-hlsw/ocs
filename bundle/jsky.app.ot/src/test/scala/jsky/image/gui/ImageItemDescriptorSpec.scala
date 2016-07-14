package jsky.image.gui

import java.net.URL

import org.scalatest.{FlatSpec, Matchers}

class ImageItemDescriptorSpec extends FlatSpec with Matchers  {
  "An ImageItemDescriptor matches" should "return None at the same coordinates" in {
    val display = new DivaMainImageDisplay()
    val id = ImageItemDescriptor(display, 0, 0, 0, 0, "title", new URL("file://tmp"), "")
    id.matches(0, 0) shouldBe None
  }
  it should "return Some at the same coordinates with width more than 0" in {
    val display = new DivaMainImageDisplay()
    val id = ImageItemDescriptor(display, 0, 0, 10, 0, "title", new URL("file://tmp"), "")
    id.matches(0, 0) shouldBe Some(0.0)
  }
  it should "return None at the same coordinates with height more than 0" in {
    val display = new DivaMainImageDisplay()
    val id = ImageItemDescriptor(display, 0, 0, 0, 10, "title", new URL("file://tmp"), "")
    id.matches(0, 0) shouldBe None
  }
  it should "return None if the dec difference is too big" in {
    val display = new DivaMainImageDisplay()
    val id = ImageItemDescriptor(display, 0, 0, 10, 10, "title", new URL("file://tmp"), "")
    id.matches(4, 6) shouldBe None
  }
  it should "return Some if the dec difference is small enough" in {
    val display = new DivaMainImageDisplay()
    val id = ImageItemDescriptor(display, 0, 0, 10, 10, "title", new URL("file://tmp"), "")
    id.matches(4, 4) shouldBe Some(339.27328323445585)
  }
  it should "return Some if the ra difference is very small" in {
    val display = new DivaMainImageDisplay()
    val id = ImageItemDescriptor(display, 359.8, 0, 0.8, 10, "title", new URL("file://tmp"), "")
    id.matches(0.2, 0) shouldBe Some(24.000000000022787)
  }
  it should "return Some if the ra difference is very small, second part" in {
    val display = new DivaMainImageDisplay()
    val id = ImageItemDescriptor(display, 0.2, 0, 0.9, 10, "title", new URL("file://tmp"), "")
    id.matches(359.8, 0) shouldBe Some(24.000000000022787)
  }
}
