package edu.gemini.seqexec.web.client

import edu.gemini.seqexec.web.common.Comment
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.{Reusability, Px}
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

    object CommentForm {

      case class Props(onCommentSubmit: Comment => Callback)

      class Backend(s: BackendScope[Props, Comment]) {
        def render(p: Props, c: Comment) = {
          <.div(
            <.form(^.className := "commentForm",
              <.input(^.`type` := "input", ^.placeholder := "Your name", ^.onChange ==> onChangeName, ^.value := c.author),
              <.input(^.`type` := "input", ^.placeholder := "Say something!", ^.onChange ==> onChangeComment, ^.value := c.comment),
              <.input(^.`type` := "submit", ^.value := "Post"),
              ^.onSubmit ==> submit(p))
          )
        }

        def onChangeComment(e: ReactEventI) =
          e.preventDefaultCB >> s.modState(_.copy(comment = e.target.value))

        def onChangeName(e: ReactEventI) =
          e.preventDefaultCB >> s.modState(_.copy(author = e.target.value))

        def submit(p: Props): (ReactEventI) => Callback = {
          val submit: ReactEventI => Callback = (e: ReactEventI) => {
            // FIXME This is not idiomatic
            (s.state >>= p.onCommentSubmit).runNow()
            // Clear the form
            e.preventDefaultCB >> s.setState(Comment("", ""))
          }
          submit
        }
      }

      val CommentForm = ReactComponentB[Props]("CommentForm")
        .initialState(new Comment("", ""))
        .renderBackend[Backend]
        .build

      def apply(p: Props) = CommentForm(p)
    }

    object CommentBox {

      case class Props(comments: List[Comment])

      case class State(c: List[Comment])

      implicit val reusableProps = Reusability.fn[Props]((p1, p2) =>
          p1.comments eq p2.comments
        )

      class Backend(s: BackendScope[Props, State]) {
        case class Callbacks(P: Props) {
          def onCommentSubmit(a: Comment) = Callback {
            // Optimistically update the local copy
            s.modState(s => s.copy(c = s.c :+ a))
            Ajax.post(
                url = "/api/comments",
                data = write(a)
            ).map { k =>
              val c = read[List[Comment]](k.responseText)
              s.setState(State(c)).runNow()
            }
          }
        }
        val cbs = Px.cbA(s.props).map(Callbacks)

        def render(p: Props, s: State) = {
          val cb = cbs.value()
          <.div("Hello, world! I am a CommentBox.", ^.className := "commentBox",
            <.h1("Comments"),
            CommentList(s.c),
            CommentForm(CommentForm.Props(cb.onCommentSubmit)))
        }

        def load() = Callback {
          // Load initial data from the server
          Ajax.get(
            url = "/api/comments"
          ).foreach { k =>
            val c = read[List[Comment]](k.responseText)
            s.setState(State(c)).runNow()
          }
        }
      }

      val component = ReactComponentB[Props]("CommentBox")
        .initialState_P(p => State(List.empty[Comment]))
        .renderBackend[Backend]
        .componentDidMount(_.backend.load())
        .build

      def apply(p: Props) = component(p)
    }

    ReactDOM.render(CommentBox(CommentBox.Props(Nil)), document.getElementById("content"))
  }
}
