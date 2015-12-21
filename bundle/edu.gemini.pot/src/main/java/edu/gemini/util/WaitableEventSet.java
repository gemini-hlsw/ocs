package edu.gemini.util;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.logging.Logger;

/**
 * Synchronization object analogous to a Condition, which allows the user to await() one of several
 * events and signal() an event's occurrence. Like a Condition, a WaitableEventSet is associated 
 * with a particular Lock which must be held in order to await() or signal(), and this Lock is 
 * released for the duration of any call to await().
 * <p>
 * This object is useful in cases where you might want to await the signalling of any one of 
 * several Conditions associated with the same Lock. So in a case such as 
 * <pre>
 * Lock lock = new ReentrantLock();
 * Condition a = lock.newCondition();
 * Condition b = lock.newCondition();
 * Condition c = lock.newCondition();
 * Condition d = lock.newCondition();
 * 
 * lock.lock();
 * // How can I wait on a OR c?
 * lock.unlock();
 * </pre>
 * you can write
 * <pre>
 * enum Events { A, B, C, D };
 * Lock lock = new ReentrantLock();
 * WaitableEventSet&lt;Events&gt; eventSet = new WaitableEventSet&lt;Events&gt;(lock);
 *  
 * lock.lock();
 * eventSet.await(Events.A, Events.C); // Will awake on signalling of A or C
 * lock.unlock();
 * 
 * </pre>
 * The signal() method (obviously) takes an event as its argument.
 * <P>
 * <i>It would be quite easy to add awaitAll(), which allows you to wait for a series of events
 * that may occur in any order. There isn't a need for this (yet).</i>
 * @author rnorris
 * @param <T> an enumerated type representing the waitable events
 */
public class WaitableEventSet<T extends Enum<T>> {

    private static final Logger LOGGER = Logger.getLogger(WaitableEventSet.class.getName());

    private final Map<Thread, EventSet> waitSets = new HashMap<>();
    private final Lock lock;

    private class EventSet {
        final EnumSet<T> waitingEvents;
        final Condition condition = lock.newCondition();
        T wakingEvent;
        public EventSet(final EnumSet<T> waitingEvents) {
            this.waitingEvents = waitingEvents;
        }
    }

    /**
     * Constructs a new WaitableEventSet with the specified Lock.
     * @param lock a Lock, may not be null
     */
    public WaitableEventSet(Lock lock) {
        if (lock == null) throw new IllegalArgumentException("Lock may not be null.");
        this.lock = lock;
    }

    /**
     * Returns the associated Lock.
     * @return the Lock associated with this WaitableEventSet
     */
    public Lock getLock() {
        return lock;
    }

    /**
     * Awaits any of the specified events with the specified timeout. This call will block until
     * one of the specified events is signalled (returning the waking event) or the thread is
     * interrupted. The associated Lock must be held when calling this method; the Lock will be
     * released during the blocking period, just like the behavior of
     * {@link Condition#await(long, TimeUnit)}. Calling this method is identical in behavior to
     * calling {@link WaitableEventSet#await(long, Enum, Enum[])} with a timeout of 0.
     * @param first the first event to await
     * @param rest other events to await
     * @return the waking event
     * @throws InterruptedException if the thread is interrupted
     * @throws IllegalMonitorStateException if the caller is not holding the associated Lock
     * @see {@link Condition#await(long, TimeUnit)}
     */
    public T await(T first, T... rest) throws InterruptedException {
        return await(0, first, rest);
    }

    /**
     * Awaits any of the specified events with the specified timeout. This call will block until
     * one of the specified events is signalled (returning the waking event), the timeout expires
     * (returning null), or the thread is interrupted. The associated Lock must be held when calling
     * this method; the Lock will be released during the blocking period, just like the
     * behavior of {@link Condition#await(long, TimeUnit)}.
     * @param timeoutMillis a timeout in milliseconds, or 0 to wait indefinitely
     * @param first the first event to await
     * @param rest other events to await
     * @return the waking event, or <code>null</code> if the timeout expires
     * @throws InterruptedException if the thread is interrupted
     * @throws IllegalMonitorStateException if the caller is not holding the associated Lock
     * @see {@link Condition#await(long, TimeUnit)}
     */
    public T await(long timeoutMillis, T first, T... rest) throws InterruptedException {
        Thread thread = Thread.currentThread();
        EventSet set = new EventSet(EnumSet.of(first, rest));
        waitSets.put(thread, set); // synchronized
        try {
            while (set.wakingEvent == null) {
                if (timeoutMillis == 0) {
                    set.condition.await();
                } else {
                    set.condition.await(timeoutMillis, TimeUnit.MILLISECONDS);
                    break;
                }
            }
            return set.wakingEvent;
        } finally {
            waitSets.remove(thread);
        }
    }

    /**
     * Signals the specified event. All threads waiting on this event will become unblocked.
     * The associated Lock must be held when calling this method, just like the behavior of
     * {@link Condition#signalAll()};
     * @throws IllegalMonitorStateException if the caller is not holding the associated Lock
     * @param event the event to signal, not null
     */
    public void signal(T event) {
        if (event == null)
            throw new IllegalArgumentException("Event may not be null.");
        LOGGER.fine(Thread.currentThread() + " signalled " + event);
        synchronized (waitSets) {
            for (EventSet set: waitSets.values()) {
                if (set.waitingEvents.contains(event)) {
                    set.wakingEvent = event;
                    set.condition.signal();
                }
            }
        }
    }

}
