//
// $Id: AbstractSessionEventConsumer.java 756 2007-01-08 18:01:24Z gillies $
//

package edu.gemini.wdba.session;

import edu.gemini.spModel.event.ExecEvent;
import edu.gemini.wdba.glue.api.WdbaGlueException;

import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * This class provides a base class for consumers of
 * <tt>{@link SessionEvent}</tt> objects.
 * <p>
 * It is assumed that there will be several different "services" interested
 * in the events coming out of the seqexec and tcc.  This class provides
 * a base class for easy construction of those consumers.
 *
  */
public abstract class AbstractSessionEventConsumer implements ISessionEventListener, Runnable {

    private static final Logger LOG = Logger.getLogger(AbstractSessionEventConsumer.class.getName());

    /**
     * Internal class to handle queuing of events for this thread
    **/
    private static final class EventQueue {
        private LinkedList<ExecEvent> _list = new LinkedList<>();

        // Add an event and notify listeners
        private synchronized void add(ExecEvent ev) {
            _list.addFirst(ev);
            notifyAll();
        }

        // Remove an event from the end
        private synchronized ExecEvent remove() throws InterruptedException {
            while (_list.isEmpty())
                wait();

            return _list.removeLast();
        }
    }

    private final EventQueue _evq = new EventQueue();

    public AbstractSessionEventConsumer(ISessionEventProducer ssp) {
        if (ssp == null) throw new NullPointerException();
        ssp.addSessionEventListener(this);
    }

    /**
     * Derived services/consumers implement this to handle events.
     * @param ev
     * @throws WdbaGlueException
     */
    protected abstract void doMsgUpdate(ExecEvent ev) throws WdbaGlueException;

    /**
     * Method implementing <code>ISessionEventListener</code>.   The events are placed on the queue and
     * handled by this thread.
     * @param sse an <code>ExecEvent</code>
     */
    public void sessionUpdate(ExecEvent sse) {
        LOG.info(String.format("%s: enqueueing event: %s", getName(), sse));
        // Place on queue
        _evq.add(sse);
    }

    public abstract String getName();

    // Runnable
    public void run() {
        // Remove from queue and run
        while (true) {
            try {
                final ExecEvent se = _evq.remove();
                LOG.info(String.format("%s: start processing event: %s", getName(), se));
                doMsgUpdate(se);
                LOG.info(String.format("%s: done processing event: %s", getName(), se));
            } catch (WdbaGlueException ex) {
                LOG.severe("Logging the service exception from the thread!");
                LOG.log(Level.SEVERE,  ex.getMessage(), ex);
            } catch (InterruptedException ex) {
                LOG.log(Level.SEVERE, ex.getMessage(), ex);
                return;
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
    }

}
