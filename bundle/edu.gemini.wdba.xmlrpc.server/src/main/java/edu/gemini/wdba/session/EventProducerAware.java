package edu.gemini.wdba.session;

//
// Gemini Observatory/AURA
// $Id: EventProducerAware.java 756 2007-01-08 18:01:24Z gillies $
//

public interface EventProducerAware {

    /**
     * Set the event support for the object.
     * @param evtProducer
     */
    public void setEventProducer(ISessionEventProducer evtProducer);
}
