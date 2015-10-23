package com.hazelcast.simulator.protocol.handler;

import com.hazelcast.simulator.agent.workerjvm.WorkerJvmManager;
import com.hazelcast.simulator.protocol.core.AddressLevel;
import com.hazelcast.simulator.protocol.core.ConnectionManager;
import com.hazelcast.simulator.protocol.core.SimulatorAddress;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.log4j.Logger;

import static com.hazelcast.simulator.protocol.core.SimulatorMessageCodec.getMessageId;
import static com.hazelcast.simulator.protocol.core.SimulatorMessageCodec.isSimulatorMessage;
import static java.lang.String.format;

public class ForwardToCoordinatorHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private static final Logger LOGGER = Logger.getLogger(ForwardToCoordinatorHandler.class);

    private final SimulatorAddress localAddress;
    private final AddressLevel addressLevel;

    private final ConnectionManager connectionManager;
    private final WorkerJvmManager workerJvmManager;

    public ForwardToCoordinatorHandler(SimulatorAddress localAddress, ConnectionManager connectionManager,
                                       WorkerJvmManager workerJvmManager) {
        this.localAddress = localAddress;
        this.addressLevel = localAddress.getAddressLevel();

        this.connectionManager = connectionManager;
        this.workerJvmManager = workerJvmManager;
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final ByteBuf buffer) throws Exception {
        if (isSimulatorMessage(buffer)) {
            if (LOGGER.isTraceEnabled()) {
                long messageId = getMessageId(buffer);
                LOGGER.trace(format("[%d] %s %s forwarding message to parent", messageId, addressLevel, localAddress));
            }

            workerJvmManager.updateLastSeenTimestamp(buffer);

            buffer.retain();
            connectionManager.getChannels().writeAndFlush(buffer);
        }
    }
}
