package com.hazelcast.simulator.test;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.simulator.tests.PropertiesTest;
import com.hazelcast.simulator.tests.SuccessTest;
import com.hazelcast.simulator.tests.TestContextTest;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.hazelcast.simulator.utils.FileUtils.deleteQuiet;
import static com.hazelcast.simulator.utils.FileUtils.ensureExistingFile;
import static com.hazelcast.simulator.utils.FileUtils.writeText;
import static com.hazelcast.simulator.utils.FormatUtils.NEW_LINE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class TestRunnerTest {

    private final SuccessTest successTest = new SuccessTest();
    private final TestRunner<SuccessTest> testRunner = new TestRunner<SuccessTest>(successTest);

    private File configFile;

    @After
    public void tearDown() {
        Hazelcast.shutdownAll();
        deleteQuiet(configFile);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructor_nullTest() {
        new TestRunner<SuccessTest>(null);
    }

    @Test
    public void testBasics() throws Exception {
        assertEquals(successTest, testRunner.getTest());
        assertNull(testRunner.getHazelcastInstance());
        assertTrue(testRunner.getDurationSeconds() > 0);

        testRunner.withDuration(3).withSleepInterval(1);
        assertEquals(3, testRunner.getDurationSeconds());

        testRunner.run();
        assertNotNull(testRunner.getHazelcastInstance());

        Set<TestPhase> testPhases = successTest.getTestPhases();
        assertEquals(TestPhase.values().length, testPhases.size());
    }

    @Test
    public void testTestContext() throws Exception {
        TestContextTest test = new TestContextTest();
        TestRunner<TestContextTest> testRunner = new TestRunner<TestContextTest>(test);
        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance();

        testRunner.withDuration(0).withHazelcastInstance(hazelcastInstance).run();

        TestContext testContext = test.getTestContext();
        assertNotNull(testContext);
        assertEquals(hazelcastInstance, testContext.getTargetInstance());
        assertNotNull(testContext.getTestId());
        assertNotNull(testContext.getPublicIpAddress());
    }

    @Test
    public void testWithProperties() throws Exception {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("testProperty", "testValue");

        PropertiesTest propertiesTest = new PropertiesTest();
        assertNull(propertiesTest.testProperty);

        TestRunner testRunner = new TestRunner<PropertiesTest>(propertiesTest, properties);

        testRunner.withDuration(1);
        assertEquals(1, testRunner.getDurationSeconds());

        testRunner.run();
        assertEquals("testValue", propertiesTest.testProperty);
    }

    @Test
    public void testWithDuration_zero() {
        testRunner.withDuration(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWithDuration_negative() {
        testRunner.withDuration(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWithSleepInterval_zero() {
        testRunner.withSleepInterval(0);
    }

    @Test(expected = NullPointerException.class)
    public void testWithHazelcastInstance_null() {
        testRunner.withHazelcastInstance(null);
    }

    @Test
    public void testWithHazelcastInstance() {
        HazelcastInstance hazelcastInstance = mock(HazelcastInstance.class);
        testRunner.withHazelcastInstance(hazelcastInstance);

        assertEquals(hazelcastInstance, testRunner.getHazelcastInstance());
    }

    @Test(expected = NullPointerException.class)
    public void withHazelcastConfigFile_null() throws Exception {
        testRunner.withHazelcastConfigFile(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void withHazelcastConfigFile_notFound() throws Exception {
        testRunner.withHazelcastConfigFile(new File("notFound"));
    }

    @Test
    public void withHazelcastConfigFile() throws Exception {
        configFile = ensureExistingFile("config.xml");
        writeText("<hazelcast xsi:schemaLocation=\"http://www.hazelcast.com/schema/config"
                + NEW_LINE + "                               http://www.hazelcast.com/schema/config/hazelcast-config-3.6.xsd\""
                + NEW_LINE + "           xmlns=\"http://www.hazelcast.com/schema/config\""
                + NEW_LINE + "           xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                + NEW_LINE + "</hazelcast>", configFile);

        testRunner.withHazelcastConfigFile(configFile);
    }

    @Test(expected = NullPointerException.class)
    public void withHazelcastConfig_null() {
        testRunner.withHazelcastConfig(null);
    }

    @Test
    public void withHazelcastConfig() {
        Config config = new Config();

        testRunner.withHazelcastConfig(config);
    }
}
