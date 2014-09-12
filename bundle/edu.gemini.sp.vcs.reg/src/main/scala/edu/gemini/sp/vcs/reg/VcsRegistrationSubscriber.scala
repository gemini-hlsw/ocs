package edu.gemini.sp.vcs.reg

trait VcsRegistrationSubscriber {
  def notify(evt: VcsRegistrationEvent, pub: VcsRegistrar)
}
