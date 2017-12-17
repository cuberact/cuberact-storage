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

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author Michal Nikodim (michal.nikodim@gmail.com)
 */
public class DeferredExecutor {

    private static final DeferredThreadPoolExecutor DEFERRED_EXECUTOR = new DeferredThreadPoolExecutor();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> DEFERRED_EXECUTOR.shutdownNow().forEach(Runnable::run), "DEFERRED_EXECUTOR - shutdown"));
    }

    public static void runDeferred(DeferredTask task) {
        DEFERRED_EXECUTOR.schedule(task, task.getDelayInMilliSeconds(), TimeUnit.MILLISECONDS);
    }

    public static void runImmediately(Predicate<DeferredTask> matched) {
        List<DeferredTask> selected = DEFERRED_EXECUTOR.getQueue().stream()
                .map(runnable -> (DeferredTask) runnable)
                .filter(matched)
                .collect(Collectors.toList());
        selected.forEach(deferredTask -> {
            boolean canceled = deferredTask.cancel(false);
            if (canceled) {
                DEFERRED_EXECUTOR.getQueue().remove(deferredTask);
                deferredTask.run();
            }
        });
    }
}
