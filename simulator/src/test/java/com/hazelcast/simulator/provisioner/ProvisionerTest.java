package com.hazelcast.simulator.provisioner;

import com.hazelcast.simulator.common.SimulatorProperties;
import com.hazelcast.simulator.utils.Bash;
import com.hazelcast.simulator.utils.CommandLineExitException;
import com.hazelcast.simulator.utils.jars.HazelcastJARs;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static com.hazelcast.simulator.TestEnvironmentUtils.createAgentsFileWithLocalhost;
import static com.hazelcast.simulator.TestEnvironmentUtils.createCloudCredentialFiles;
import static com.hazelcast.simulator.TestEnvironmentUtils.deleteAgentsFile;
import static com.hazelcast.simulator.TestEnvironmentUtils.deleteCloudCredentialFiles;
import static com.hazelcast.simulator.TestEnvironmentUtils.resetUserDir;
import static com.hazelcast.simulator.TestEnvironmentUtils.setDistributionUserDir;
import static java.util.Collections.singleton;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProvisionerTest extends AbstractComputeServiceTest {

    private Bash bash;
    private Provisioner provisioner;

    @Before
    public void setUp() {
        setDistributionUserDir();
        createAgentsFileWithLocalhost();
        createCloudCredentialFiles();

        SimulatorProperties properties = new SimulatorProperties();
        initComputeServiceMock();
        bash = mock(Bash.class);
        HazelcastJARs hazelcastJars = mock(HazelcastJARs.class);

        provisioner = new Provisioner(properties, computeService, bash, hazelcastJars, false, 0);
    }

    @After
    public void tearDown() {
        provisioner.shutdown();

        resetUserDir();
        deleteAgentsFile();
        deleteCloudCredentialFiles();
    }

    @Test
    public void testScale_toNegative() {
        mockComputeServiceForScaleDown();
        provisioner.scale(-1);
    }

    @Test
    public void testScale_toZero() {
        mockComputeServiceForScaleDown();
        provisioner.scale(0);
    }

    @Test
    public void testScale_toOne() {
        provisioner.scale(1);
    }

    @Test
    public void testScale_toTwo() throws Exception {
        mockComputeServiceForScaleUp();
        provisioner.scale(2);
    }

    @Test(expected = IllegalStateException.class)
    public void testScale_toZero_withException() {
        provisioner.scale(0);
    }

    @Test(expected = CommandLineExitException.class)
    public void testScale_toTwo_withException() throws Exception {
        IllegalStateException exception = new IllegalStateException("expected exception");
        when(computeService.createNodesInGroup(anyString(), anyInt(), any(Template.class))).thenThrow(exception);
        provisioner.scale(2);
    }

    @Test
    public void testInstallSimulator() {
        provisioner.installSimulator();

        verify(bash, atLeastOnce()).ssh(eq("127.0.0.1"), anyString());
        verify(bash, atLeastOnce()).sshQuiet(eq("127.0.0.1"), anyString());
        verify(bash, atLeastOnce()).uploadToRemoteSimulatorDir(eq("127.0.0.1"), anyString(), anyString());
    }

    @Test
    public void testDownload() {
        provisioner.download("workers");

        verify(bash, atLeastOnce()).executeQuiet(anyString());
    }

    @Test
    public void testClean() {
        provisioner.clean();

        verify(bash).ssh(eq("127.0.0.1"), anyString());
    }

    @Test
    public void testKill() {
        provisioner.killJavaProcesses();

        verify(bash).killAllJavaProcesses(eq("127.0.0.1"));
    }

    @Test
    public void testTerminate() {
        mockComputeServiceForScaleDown();
        provisioner.terminate();
    }

    private void mockComputeServiceForScaleDown() {
        Set destroyedSet = singleton(mock(NodeMetadata.class));
        doReturn(destroyedSet).when(computeService).destroyNodesMatching(any(NodeMetadataPredicate.class));
    }

    private void mockComputeServiceForScaleUp() throws Exception {
        NodeMetadata nodeMetadata = mock(NodeMetadata.class, RETURNS_DEEP_STUBS);
        when(nodeMetadata.getPrivateAddresses().iterator().next()).thenReturn("127.0.0.1");
        when(nodeMetadata.getPublicAddresses().iterator().next()).thenReturn("172.16.16.1");

        Set createdSet = singleton(nodeMetadata);
        doReturn(createdSet).when(computeService).createNodesInGroup(anyString(), anyInt(), any(Template.class));
    }
}
