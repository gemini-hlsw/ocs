// Copyright (c) 2016-2018 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.wdba.session;

import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.Option;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public final class ServiceExecutor {

    private final String threadName;
    private final Runnable task;

    private final AtomicReference<Option<ExecutorService>> exec =
        new AtomicReference<>(ImOption.empty());

    public ServiceExecutor(
        String threadName,
        Runnable task
    ) {
        this.threadName = threadName;
        this.task       = task;
    }

    private ExecutorService startNewExecutor() {
        final ExecutorService e = Executors.newSingleThreadExecutor(r -> new Thread(r, threadName));
        e.execute(task);
        return e;
    }

    public void start() {
        exec.updateAndGet(o -> o.exists(e -> !e.isShutdown()) ? o : ImOption.apply(startNewExecutor()));
    }

    public void stop() {
        exec.updateAndGet(o -> {
            o.foreach(ExecutorService::shutdownNow);
            return ImOption.empty();
        });
    }
}
