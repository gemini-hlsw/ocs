package edu.gemini.epics.acm;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class CaCommandMonitorImpl implements CaCommandMonitor {

    private State currentState;
    private Exception cause;
    private CaCommandListener callback;

    @Override
    public synchronized Exception error() {
        if (currentState == State.ERROR) {
            return cause;
        } else {
            return null;
        }
    }

    @Override
    public synchronized boolean isDone() {
        return currentState == State.IDLE || currentState == State.ERROR;
    }

    @Override
    public synchronized void waitDone(long timeout, TimeUnit unit)
            throws TimeoutException, InterruptedException {
        wait(unit.toMillis(timeout));
    }

    @Override
    public synchronized void waitDone() throws InterruptedException {
        if (currentState == State.BUSY) {
            wait();
        }
    }

    @Override
    public synchronized State state() {
        return currentState;
    }

    @Override
    public synchronized void setCallback(CaCommandListener cb) {
        callback = cb;
        if (callback != null) {
            if (currentState == State.IDLE) {
                callback.onSuccess();
            } else if (currentState == State.ERROR) {
                callback.onFailure(cause);
            }
        }
    }

    CaCommandMonitorImpl(State initialState) {
        currentState = initialState;
    }

    CaCommandMonitorImpl() {
        this(State.BUSY);
    }

    public synchronized void completeSuccess() {
        if (currentState == State.BUSY) {
            currentState = State.IDLE;
            notifyAll();
            if (callback != null) {
                callback.onSuccess();
            }
        }
    }

    public synchronized void completeFailure(Exception cause) {
        if (currentState == State.BUSY) {
            this.cause = cause;
            currentState = State.ERROR;
            notifyAll();
            if (callback != null) {
                callback.onFailure(cause);
            }
        }
    }

}
