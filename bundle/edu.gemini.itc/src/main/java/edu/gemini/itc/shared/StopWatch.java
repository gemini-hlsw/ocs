// Copyright 2001 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
//

/*
 * StopWatch.java
 *
 * Created on April 8, 2005, 2:40 PM
 */

package edu.gemini.itc.shared;

/**
 * StopWatch Class used for benchmarking.  Elapsed time is returned in milliseconds.
 */
public class StopWatch {

    private long startTime = -1;
    private long stopTime = -1;
    private boolean running = false;

    public StopWatch start() {
        startTime = System.currentTimeMillis();
        running = true;
        return this;
    }

    public StopWatch stop() {
        stopTime = System.currentTimeMillis();
        running = false;
        return this;
    }

    public long getElapsedTime() {
        if (startTime == -1) {
            return 0;
        }
        if (running) {
            return System.currentTimeMillis() - startTime;
        } else {
            return stopTime - startTime;
        }
    }

    public StopWatch reset() {
        startTime = -1;
        stopTime = -1;
        running = false;
        return this;
    }

}
