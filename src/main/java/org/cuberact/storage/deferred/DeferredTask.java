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

import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Michal Nikodim (michal.nikodim@gmail.com)
 */
public abstract class DeferredTask implements RunnableScheduledFuture<Object> {

    public static long DEFERRED_DELAY_IN_MILLISECONDS = 3000;

    private final long delayInMilliSeconds;
    private RunnableScheduledFuture<?> delegate;

    protected DeferredTask(long delayInMilliSeconds) {
        this.delayInMilliSeconds = delayInMilliSeconds;
    }

    @Override
    public abstract void run();

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object o);

    public final long getDelayInMilliSeconds() {
        return delayInMilliSeconds;
    }

    final void setDelegate(RunnableScheduledFuture<?> delegate) {
        this.delegate = delegate;
    }

    @Override
    public final boolean isPeriodic() {
        return delegate.isPeriodic();
    }

    @Override
    public final long getDelay(TimeUnit unit) {
        return delegate.getDelay(unit);
    }

    @Override
    public final int compareTo(Delayed o) {
        return delegate.compareTo(o);
    }

    @Override
    public final boolean cancel(boolean mayInterruptIfRunning) {
        return delegate.cancel(mayInterruptIfRunning);
    }

    @Override
    public final boolean isCancelled() {
        return delegate.isCancelled();
    }

    @Override
    public final boolean isDone() {
        return delegate.isDone();
    }

    @Override
    public final Object get() throws InterruptedException, ExecutionException {
        return delegate.get();
    }

    @Override
    public final Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.get(timeout, unit);
    }
}
