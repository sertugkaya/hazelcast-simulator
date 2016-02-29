package com.hazelcast.simulator.utils;

import com.hazelcast.simulator.common.SimulatorProperties;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

import static com.hazelcast.simulator.TestEnvironmentUtils.resetUserDir;
import static com.hazelcast.simulator.TestEnvironmentUtils.setDistributionUserDir;
import static com.hazelcast.simulator.utils.CloudProviderUtils.PROVIDER_EC2;
import static com.hazelcast.simulator.utils.CloudProviderUtils.PROVIDER_STATIC;
import static com.hazelcast.simulator.utils.FileUtils.appendText;
import static com.hazelcast.simulator.utils.FileUtils.deleteQuiet;
import static com.hazelcast.simulator.utils.FileUtils.ensureExistingFile;
import static com.hazelcast.simulator.utils.HarakiriMonitorUtils.getStartHarakiriMonitorCommandOrNull;
import static com.hazelcast.simulator.utils.HarakiriMonitorUtils.isHarakiriMonitorEnabled;
import static com.hazelcast.simulator.utils.ReflectionUtils.invokePrivateConstructor;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class HarakiriMonitorUtilsTest {

    private final SimulatorProperties properties = new SimulatorProperties();

    @BeforeClass
    public static void setUp() {
        setDistributionUserDir();
    }

    @AfterClass
    public static void tearDown() {
        resetUserDir();
    }

    @Test
    public void testConstructor() throws Exception {
        invokePrivateConstructor(HarakiriMonitorUtils.class);
    }

    @Test
    public void testIsEnabled_onStatic() {
        properties.set("CLOUD_PROVIDER", PROVIDER_STATIC);

        assertFalse(isHarakiriMonitorEnabled(properties));
    }

    @Test
    public void testIsEnabled_onEC2() {
        properties.set("CLOUD_PROVIDER", PROVIDER_EC2);

        assertTrue(isHarakiriMonitorEnabled(properties));
    }

    @Test
    public void testIsEnabled_onEC2_featureDisabled() {
        properties.set("CLOUD_PROVIDER", PROVIDER_EC2);
        properties.set("HARAKIRI_MONITOR_ENABLED", "false");

        assertFalse(isHarakiriMonitorEnabled(properties));
    }

    @Test
    public void testIsEnabled_onEC2_equalsIgnoreCase() {
        properties.set("CLOUD_PROVIDER", PROVIDER_EC2);
        properties.set("HARAKIRI_MONITOR_ENABLED", "TruE");

        assertTrue(isHarakiriMonitorEnabled(properties));
    }

    @Test
    public void testGetStartCommand() {
        properties.set("CLOUD_PROVIDER", PROVIDER_EC2);
        properties.set("CLOUD_IDENTITY", "identity");
        properties.set("CLOUD_CREDENTIAL", "credential");

        File identity = ensureExistingFile("identity");
        File credentials = ensureExistingFile("credential");
        try {
            appendText("someIdentity", identity);
            appendText("someCredential", credentials);

            String command = getStartHarakiriMonitorCommandOrNull(properties);
            assertNotNull(command);
            assertTrue(command.contains("someIdentity"));
            assertTrue(command.contains("someCredential"));
        } finally {
            deleteQuiet(identity);
            deleteQuiet(credentials);
        }
    }

    @Test
    public void testGetStartCommand_onStatic() {
        properties.set("CLOUD_PROVIDER", PROVIDER_STATIC);
        properties.set("HARAKIRI_MONITOR_ENABLED", "true");

        String command = getStartHarakiriMonitorCommandOrNull(properties);
        assertNull(command);
    }

    @Test
    public void testGetStartCommand_featureDisabled() {
        properties.set("CLOUD_PROVIDER", PROVIDER_EC2);
        properties.set("HARAKIRI_MONITOR_ENABLED", "false");

        String command = getStartHarakiriMonitorCommandOrNull(properties);
        assertNull(command);
    }
}
