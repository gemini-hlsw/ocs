package edu.gemini.epics.acm;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Define the interface to the objects used to monitor the execution of a
 * command, and to retrieve its results. Command results are a set of named
 * values.
 * 
 * @author jluhrs
 *
 */
public interface CaCommandMonitor {
    public enum State {
        BUSY, IDLE, ERROR
    }

    /**
     * Retrieves the completion error, if any.
     * 
     * @return the <code>Exception</code> object that caused the abnormal
     *         completion, or <code>null</code> if the command has not completed
     *         or completed without an error.
     */
    public Exception error();

    /**
     * Asks if the command is running.
     * 
     * @return true if the command is running.
     */
    public boolean isDone();

    /**
     * Blocks the current thread until the command completes.
     * 
     * @param timeout
     *            time to wait for the command completion, in seconds.
     * @throws TimeoutException
     */
    public void waitDone(long timeout, TimeUnit unit) throws TimeoutException,
            InterruptedException;

    public void waitDone() throws InterruptedException;

    /**
     * Retrieves the current execution state of the command.
     * 
     * @return the execution state of the command.
     */
    public State state();

    /**
     * Sets a listener that will be called when the command completes.
     * 
     * @param cb
     *            the command listener.
     */
    public void setCallback(CaCommandListener cb);

}
