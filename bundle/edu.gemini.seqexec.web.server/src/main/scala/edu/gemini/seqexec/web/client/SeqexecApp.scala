package edu.gemini.seqexec.web.client

import japgolly.scalajs.react.{ReactDOM, ReactComponentB}
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.scalajs.js.JSApp
import org.scalajs.dom.document

object SeqexecApp extends JSApp {

  def main(): Unit = {
    val Comment = ReactComponentB[String]("Comment")
        .render( c =>
          <.div(^.className := "comment",
            <.h2(c, ^.className := "commentAuthor")
          )
        )

    val CommentList = ReactComponentB[Unit]("CommentList")
        .render( _ =>
          <.div("Hello, world! I am a CommentList.", ^.className := "commentList")
        ).buildU

    val CommentForm = ReactComponentB[Unit]("CommentForm")
        .render( _ =>
          <.div("Hello, world! I am a CommentForm.", ^.className := "commentForm")
        ).buildU

    val CommentBox = ReactComponentB[Unit]("CommentBox")
        .render( _ =>
          <.div("Hello, world! I am a CommentBox.", ^.className := "commentBox",
            <.h1("Comments"),
            CommentList(),
            CommentForm())
        ).buildU

    ReactDOM.render(CommentBox(), document.getElementById("content"))
  }
}
