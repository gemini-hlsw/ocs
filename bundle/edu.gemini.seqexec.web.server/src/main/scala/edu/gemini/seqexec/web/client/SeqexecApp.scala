package edu.gemini.seqexec.web.client

import edu.gemini.seqexec.web.common.Comment
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.scalajs.js.JSApp

import org.scalajs.dom.document
import org.scalajs.dom.ext.Ajax

import upickle.default._

import scalajs.concurrent.JSExecutionContext.Implicits.runNow

object SeqexecApp extends JSApp {

  def main(): Unit = {
    val ReactComment = ReactComponentB[Comment]("ReactComment")
        .render_P( c =>
          <.div(^.className := "comment",
            <.h2(c.author, ^.className := "commentAuthor"),
            c.comment
          )
        ).build

    val CommentList = ReactComponentB[List[Comment]]("CommentList")
        .render_P( l =>
          <.div("Hello, world! I am a CommentList.", ^.className := "commentList",
            l.map(c => ReactComment(c))
          )
        ).build

    val CommentForm = ReactComponentB[Unit]("CommentForm")
        .render( _ =>
          <.div("Hello, world! I am a CommentForm.", ^.className := "commentForm")
        ).buildU

    val CommentBox = ReactComponentB[Unit]("CommentBox")
        .initialState(List(Comment("Carlos", "My comment"), Comment("Jose", "His comment")))
        .render_S( k =>
          <.div("Hello, world! I am a CommentBox.", ^.className := "commentBox",
            <.h1("Comments"),
            CommentList(k),
            CommentForm())
        )
        .componentDidMount(s => Callback {
          Ajax.get(
            url = "/api/comments"
          ).map(k => println(k.responseText))

          s.modState(_ => List(Comment("Carlos", "My comment"), Comment("Jose", "His comment"))).runNow()
        })
        .buildU

    ReactDOM.render(CommentBox(), document.getElementById("content"))
  }
}
