package com.hazelcast.simulator.protocol.registry;

import com.hazelcast.simulator.protocol.core.AddressLevel;
import com.hazelcast.simulator.protocol.core.SimulatorAddress;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AgentDataTest {

    private static final int DEFAULT_ADDRESS_INDEX = 1;
    private static final String DEFAULT_PUBLIC_ADDRESS = "172.16.16.1";
    private static final String DEFAULT_PRIVATE_ADDRESS = "127.0.0.1";

    @Test
    public void testConstructor() {
        AgentData agentData = new AgentData(DEFAULT_ADDRESS_INDEX, DEFAULT_PUBLIC_ADDRESS, DEFAULT_PRIVATE_ADDRESS);

        assertEquals(new SimulatorAddress(AddressLevel.AGENT, DEFAULT_ADDRESS_INDEX, 0, 0), agentData.getAddress());
        assertEquals(DEFAULT_ADDRESS_INDEX, agentData.getAddressIndex());
        assertEquals(DEFAULT_PUBLIC_ADDRESS, agentData.getPublicAddress());
        assertEquals(DEFAULT_PRIVATE_ADDRESS, agentData.getPrivateAddress());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_addressIndexNegative() {
        new AgentData(-1, DEFAULT_PUBLIC_ADDRESS, DEFAULT_PRIVATE_ADDRESS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_addressIndexZero() {
        new AgentData(0, DEFAULT_PUBLIC_ADDRESS, DEFAULT_PRIVATE_ADDRESS);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructor_publicAddressNull() {
        new AgentData(DEFAULT_ADDRESS_INDEX, null, DEFAULT_PRIVATE_ADDRESS);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructor_privateAddressNull() {
        new AgentData(DEFAULT_ADDRESS_INDEX, DEFAULT_PUBLIC_ADDRESS, null);
    }
}
