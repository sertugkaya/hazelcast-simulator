package com.hazelcast.simulator.protocol;

import com.hazelcast.simulator.protocol.connector.CoordinatorConnector;
import com.hazelcast.simulator.protocol.connector.WorkerConnector;
import com.hazelcast.simulator.protocol.core.Response;
import com.hazelcast.simulator.protocol.core.SimulatorAddress;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.hazelcast.simulator.protocol.ProtocolUtil.AGENT_START_PORT;
import static com.hazelcast.simulator.protocol.ProtocolUtil.DEFAULT_OPERATION;
import static com.hazelcast.simulator.protocol.ProtocolUtil.DEFAULT_TEST_TIMEOUT_MILLIS;
import static com.hazelcast.simulator.protocol.ProtocolUtil.assertSingleTarget;
import static com.hazelcast.simulator.protocol.ProtocolUtil.getAgentConnector;
import static com.hazelcast.simulator.protocol.ProtocolUtil.getCoordinatorConnector;
import static com.hazelcast.simulator.protocol.ProtocolUtil.getWorkerConnector;
import static com.hazelcast.simulator.protocol.ProtocolUtil.resetLogLevel;
import static com.hazelcast.simulator.protocol.ProtocolUtil.sendFromCoordinator;
import static com.hazelcast.simulator.protocol.ProtocolUtil.setLogLevel;
import static com.hazelcast.simulator.protocol.ProtocolUtil.shutdownCoordinatorConnector;
import static com.hazelcast.simulator.protocol.ProtocolUtil.startCoordinator;
import static com.hazelcast.simulator.protocol.ProtocolUtil.startSimulatorComponents;
import static com.hazelcast.simulator.protocol.ProtocolUtil.stopSimulatorComponents;
import static com.hazelcast.simulator.protocol.core.AddressLevel.TEST;
import static com.hazelcast.simulator.protocol.core.ResponseType.FAILURE_COORDINATOR_NOT_FOUND;
import static com.hazelcast.simulator.protocol.core.ResponseType.SUCCESS;
import static com.hazelcast.simulator.protocol.core.SimulatorAddress.COORDINATOR;
import static org.junit.Assert.assertNull;

public class ProtocolReconnectTest {

    private static final Logger LOGGER = Logger.getLogger(ProtocolReconnectTest.class);

    @Before
    public void setUp() {
        setLogLevel(Level.TRACE);

        startSimulatorComponents(1, 1, 1);
    }

    @After
    public void tearDown() {
        stopSimulatorComponents();

        resetLogLevel();
    }

    @Test(timeout = DEFAULT_TEST_TIMEOUT_MILLIS)
    public void reconnect() {
        SimulatorAddress testAddress = new SimulatorAddress(TEST, 1, 1, 1);
        WorkerConnector worker = getWorkerConnector(0);

        // assert that the connection is working downstream
        Response response = sendFromCoordinator(testAddress);
        assertSingleTarget(response, testAddress, SUCCESS);

        // assert that the connection is working upstream
        response = worker.write(testAddress, COORDINATOR, DEFAULT_OPERATION);
        assertSingleTarget(response, testAddress, COORDINATOR, SUCCESS);

        // shutdown connection
        shutdownCoordinatorConnector();
        assertNull(getCoordinatorConnector());

        // assert that there are no connections found anymore
        response = worker.write(testAddress, COORDINATOR, DEFAULT_OPERATION);
        assertSingleTarget(response, testAddress, getAgentConnector(0).getAddress(), FAILURE_COORDINATOR_NOT_FOUND);

        LOGGER.info("--------------------------");
        LOGGER.info("Starting new connection...");
        LOGGER.info("--------------------------");

        CoordinatorConnector newConnector = startCoordinator("127.0.0.1", AGENT_START_PORT, 1);

        // assert that new connection is working downstream
        response = newConnector.write(testAddress, DEFAULT_OPERATION);
        assertSingleTarget(response, testAddress, SUCCESS);

        // assert that the new connection is working upstream
        response = worker.write(testAddress, COORDINATOR, DEFAULT_OPERATION);
        assertSingleTarget(response, testAddress, COORDINATOR, SUCCESS);

        newConnector.shutdown();
    }

    @Test(timeout = DEFAULT_TEST_TIMEOUT_MILLIS)
    public void connectTwice() {
        SimulatorAddress testAddress = new SimulatorAddress(TEST, 1, 1, 1);
        WorkerConnector worker = getWorkerConnector(0);

        // assert that the connection is working downstream
        Response response = sendFromCoordinator(testAddress);
        assertSingleTarget(response, testAddress, SUCCESS);

        // assert that the connection is working upstream
        response = worker.write(testAddress, COORDINATOR, DEFAULT_OPERATION);
        assertSingleTarget(response, testAddress, COORDINATOR, SUCCESS);

        LOGGER.info("-------------------------------");
        LOGGER.info("Starting second connection...");
        LOGGER.info("-------------------------------");

        CoordinatorConnector secondConnector = startCoordinator("127.0.0.1", AGENT_START_PORT, 1);

        // assert that first connection is still working downstream
        response = sendFromCoordinator(testAddress);
        assertSingleTarget(response, testAddress, SUCCESS);

        // assert that second connection is working downstream
        response = secondConnector.write(testAddress, DEFAULT_OPERATION);
        assertSingleTarget(response, testAddress, SUCCESS);

        // assert that the connections are working upstream
        response = worker.write(testAddress, COORDINATOR, DEFAULT_OPERATION);
        assertSingleTarget(response, testAddress, COORDINATOR, SUCCESS);

        // shutdown first connection
        getCoordinatorConnector().shutdown();

        // assert that the connections are working upstream
        response = worker.write(testAddress, COORDINATOR, DEFAULT_OPERATION);
        assertSingleTarget(response, testAddress, COORDINATOR, SUCCESS);

        // shutdown second connection
        secondConnector.shutdown();

        // assert that there are no connections found anymore
        response = worker.write(testAddress, COORDINATOR, DEFAULT_OPERATION);
        assertSingleTarget(response, testAddress, getAgentConnector(0).getAddress(), FAILURE_COORDINATOR_NOT_FOUND);
    }
}
