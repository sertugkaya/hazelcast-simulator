package com.hazelcast.simulator.worker.loadsupport;

import com.hazelcast.core.IMap;
import com.hazelcast.util.EmptyStatement;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class SyncMapStreamerTest {

    @SuppressWarnings("unchecked")
    private final IMap<Integer, String> map = mock(IMap.class);

    private Streamer<Integer, String> streamer;

    @Before
    public void setUp() {
        StreamerFactory.enforceAsync(false);
        streamer = StreamerFactory.getInstance(map);
    }

    @Test
    public void testPushEntry() {
        streamer.pushEntry(15, "value");

        verify(map).set(15, "value");
        verifyNoMoreInteractions(map);
    }

    @Test(timeout = 10000)
    public void testPushEntry_withException() {
        doThrow(new IllegalArgumentException()).when(map).set(anyInt(), anyString());

        try {
            streamer.pushEntry(1, "foobar");
            fail("Expected exception directly thrown by pushEntry() method");
        } catch (Exception ignored) {
            EmptyStatement.ignore(ignored);
        }

        // this method should never block and never throw an exception
        streamer.await();

        verify(map).set(1, "foobar");
        verifyNoMoreInteractions(map);
    }
}
