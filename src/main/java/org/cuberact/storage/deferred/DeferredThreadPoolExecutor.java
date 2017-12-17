/*
 * Copyright 2017 Michal Nikodim
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cuberact.storage.deferred;

import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Michal Nikodim (michal.nikodim@gmail.com)
 */
final class DeferredThreadPoolExecutor extends ScheduledThreadPoolExecutor {

    DeferredThreadPoolExecutor() {
        super(1, r -> {
            Thread thread = new Thread(r, "DeferredExecutorThread");
            thread.setDaemon(true);
            return thread;
        });
        setRemoveOnCancelPolicy(true);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        remove(command);
        return super.schedule(command, delay, unit);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <V> RunnableScheduledFuture<V> decorateTask(Runnable runnable, RunnableScheduledFuture<V> task) {
        ((DeferredTask) runnable).setDelegate(task);
        return (RunnableScheduledFuture<V>) runnable;
    }
}
