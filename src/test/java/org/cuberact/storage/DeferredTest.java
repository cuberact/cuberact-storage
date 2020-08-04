package org.cuberact.storage;

import org.cuberact.storage.deferred.*;
import org.junit.jupiter.api.*;

/**
 * @author Michal Nikodim (michal.nikodim@gmail.com)
 */
public class DeferredTest {

    @Test
    public void runOnlyOnce() throws InterruptedException {
        DeferredTask.DEFERRED_DELAY_IN_MILLISECONDS = 100;
        byte[] fakeContent = new byte[0];
     //   Resource fakeResource = Mockito.mock(Resource.class);
        for (int i = 0; i < 20; i++) {
            Thread.sleep(50);
     //       DeferredExecutor.runDeferred(new Resource.WriteTask(fakeResource, () -> fakeContent));
        }
    //    Mockito.verify(fakeResource, Mockito.times(0)).writeInternal(fakeContent, false);
        Thread.sleep(110); //wait for deferred execution
    //    Mockito.verify(fakeResource, Mockito.times(1)).writeInternal(fakeContent, false);
    }
}
