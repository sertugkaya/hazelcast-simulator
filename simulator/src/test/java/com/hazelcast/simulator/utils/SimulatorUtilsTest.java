package com.hazelcast.simulator.utils;

import com.hazelcast.simulator.common.SimulatorProperties;
import com.hazelcast.simulator.protocol.registry.ComponentRegistry;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static com.hazelcast.simulator.TestEnvironmentUtils.resetUserDir;
import static com.hazelcast.simulator.TestEnvironmentUtils.setDistributionUserDir;
import static com.hazelcast.simulator.utils.FileUtils.deleteQuiet;
import static com.hazelcast.simulator.utils.FileUtils.ensureExistingFile;
import static com.hazelcast.simulator.utils.FileUtils.writeText;
import static com.hazelcast.simulator.utils.ReflectionUtils.invokePrivateConstructor;
import static com.hazelcast.simulator.utils.SimulatorUtils.getPropertiesFile;
import static com.hazelcast.simulator.utils.SimulatorUtils.loadComponentRegister;
import static com.hazelcast.simulator.utils.SimulatorUtils.loadSimulatorProperties;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SimulatorUtilsTest {

    private File agentsFile;
    private ComponentRegistry componentRegistry;

    @Before
    public void setUp() throws IOException {
        agentsFile = ensureExistingFile("SimulatorUtilsTest-agentsFile.txt");
    }

    @After
    public void tearDown() {
        deleteQuiet(agentsFile);
    }

    @Test
    public void testConstructor() throws Exception {
        invokePrivateConstructor(SimulatorUtils.class);
    }

    @Test
    public void testLoadComponentRegister() {
        writeText("192.168.1.1,10.10.10.10", agentsFile);

        componentRegistry = loadComponentRegister(agentsFile);
        assertEquals(1, componentRegistry.agentCount());
    }

    @Test(expected = CommandLineExitException.class)
    public void testLoadComponentRegister_emptyFile_withSizeCheck() {
        componentRegistry = loadComponentRegister(agentsFile, true);
    }

    @Test
    public void testLoadComponentRegister_emptyFile_withoutSizeCheck() {
        componentRegistry = loadComponentRegister(agentsFile, false);
        assertEquals(0, componentRegistry.agentCount());
    }

    @Test
    public void testLoadSimulatorProperties() {
        setDistributionUserDir();
        try {
            OptionSet options = mock(OptionSet.class);
            when(options.has(any(OptionSpec.class))).thenReturn(false);

            SimulatorProperties properties = loadSimulatorProperties(options, null);
            assertNotNull(properties);
        } finally {
            resetUserDir();
        }
    }

    @Test
    public void testGetPropertiesFile() {
        OptionSet options = mock(OptionSet.class);
        when(options.has(any(OptionSpec.class))).thenReturn(true);
        when(options.valueOf(any(OptionSpec.class))).thenReturn("test");

        File expectedFile = new File("test");
        File actualFile = getPropertiesFile(options, null);

        assertEquals(expectedFile, actualFile);
    }

    @Test
    public void testGetPropertiesFile_noPropertiesSpec() {
        OptionSet options = mock(OptionSet.class);
        when(options.has(any(OptionSpec.class))).thenReturn(false);

        File actualFile = getPropertiesFile(options, null);

        assertEquals(null, actualFile);
    }
}
