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

    case class CommentFormProps(onCommentSubmit: Callback)

    class FormBackend(s: BackendScope[CommentFormProps, Comment]) {
      def render(p: CommentFormProps, c: Comment) = {
        <.div(
          <.div(c.author),
          <.div(c.comment),
          <.form(^.className := "commentForm",
            <.input(^.`type` := "input", ^.placeholder := "Your name", ^.onChange ==> onChangeName, ^.value := c.author),
            <.input(^.`type` := "input", ^.placeholder := "Say something", ^.onChange ==> onChangeComment, ^.value := c.comment),
            <.input(^.`type` := "submit", ^.value := "Post"),
            ^.onSubmit ==> submit(p))
        )
      }

      def onChangeComment(e: ReactEventI) =
        e.preventDefaultCB >> s.modState(_.copy(comment = e.target.value))

      def onChangeName(e: ReactEventI) =
        e.preventDefaultCB >> s.modState(_.copy(author = e.target.value))

      def submit(p: CommentFormProps): (_root_.japgolly.scalajs.react.ReactEventI) => Callback = {
        val submit: ReactEventI => Callback = (e: ReactEventI) =>
          e.preventDefaultCB >> s.setState(Comment("", ""))
        submit
      }
    }

    val CommentForm = ReactComponentB[CommentFormProps]("CommentForm")
        .initialState(new Comment("", ""))
        .renderBackend[FormBackend]
        .build

    case class CommentBoxProps(comments: List[Comment]) {

    }

    class CommentBoxBackend(s: BackendScope[CommentBoxProps, List[Comment]]) {
      def onCommentSubmit = Callback.alert("Sebmit")

      def render(p: CommentBoxProps, s:List[Comment]) = {
        <.div("Hello, world! I am a CommentBox.", ^.className := "commentBox",
          <.h1("Comments"),
          CommentList(s),
          CommentForm(CommentFormProps(onCommentSubmit)))
      }
    }

    val CommentBox = ReactComponentB[CommentBoxProps]("CommentBox")
        .initialState_P(_ => List.empty[Comment])
        .renderBackend[CommentBoxBackend]
        .componentDidMount(s => Callback {
          Ajax.get(
            url = "/api/comments"
          ).foreach { k =>
            val c = read[List[Comment]](k.responseText)
            s.setState(c).runNow()
          }
        })
        .build

    ReactDOM.render(CommentBox(CommentBoxProps(Nil)), document.getElementById("content"))
  }
}
