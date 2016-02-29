/*
 * Copyright (c) 2008-2016, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hazelcast.simulator.protocol.core;

import com.hazelcast.simulator.utils.EmptyStatement;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.concurrent.CountDownLatch;

/**
 * Manages incoming client connections so we can send messages to them.
 */
public class ConnectionManager implements ConnectionListener {

    private final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private final CountDownLatch countDownLatch = new CountDownLatch(1);

    @Override
    public void connected(Channel channel) {
        if (channels.add(channel)) {
            countDownLatch.countDown();
        }
    }

    @Override
    public void disconnected(Channel channel) {
        channels.remove(channel);
    }

    public void waitForAtLeastOneChannel() {
        try {
            countDownLatch.await();
        } catch (InterruptedException ignored) {
            EmptyStatement.ignore(ignored);
        }
    }

    public ChannelGroup getChannels() {
        return channels;
    }
}
