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
package com.hazelcast.simulator.provisioner;

import com.hazelcast.simulator.common.AgentsFile;
import com.hazelcast.simulator.common.SimulatorProperties;
import com.hazelcast.simulator.protocol.registry.AgentData;
import com.hazelcast.simulator.protocol.registry.ComponentRegistry;
import com.hazelcast.simulator.utils.Bash;
import com.hazelcast.simulator.utils.CommandLineExitException;
import com.hazelcast.simulator.utils.ThreadSpawner;
import com.hazelcast.simulator.utils.jars.HazelcastJARs;
import com.hazelcast.util.EmptyStatement;
import org.apache.log4j.Logger;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.hazelcast.simulator.common.GitInfo.getBuildTime;
import static com.hazelcast.simulator.common.GitInfo.getCommitIdAbbrev;
import static com.hazelcast.simulator.provisioner.ProvisionerCli.init;
import static com.hazelcast.simulator.provisioner.ProvisionerCli.run;
import static com.hazelcast.simulator.provisioner.ProvisionerUtils.calcBatches;
import static com.hazelcast.simulator.provisioner.ProvisionerUtils.ensureIsCloudProviderSetup;
import static com.hazelcast.simulator.provisioner.ProvisionerUtils.ensureIsRemoteSetup;
import static com.hazelcast.simulator.provisioner.ProvisionerUtils.getInitScriptFile;
import static com.hazelcast.simulator.utils.CommonUtils.exitWithError;
import static com.hazelcast.simulator.utils.CommonUtils.getElapsedSeconds;
import static com.hazelcast.simulator.utils.CommonUtils.getSimulatorVersion;
import static com.hazelcast.simulator.utils.CommonUtils.sleepSeconds;
import static com.hazelcast.simulator.utils.ExecutorFactory.createFixedThreadPool;
import static com.hazelcast.simulator.utils.FileUtils.appendText;
import static com.hazelcast.simulator.utils.FileUtils.fileAsText;
import static com.hazelcast.simulator.utils.FileUtils.getSimulatorHome;
import static com.hazelcast.simulator.utils.FileUtils.rename;
import static com.hazelcast.simulator.utils.FormatUtils.HORIZONTAL_RULER;
import static com.hazelcast.simulator.utils.FormatUtils.NEW_LINE;
import static com.hazelcast.simulator.utils.HarakiriMonitorUtils.getStartHarakiriMonitorCommandOrNull;
import static com.hazelcast.simulator.utils.SimulatorUtils.loadComponentRegister;
import static java.lang.String.format;

public class Provisioner {

    private static final int MACHINE_WARMUP_WAIT_SECONDS = 10;
    private static final int EXECUTOR_TERMINATION_TIMEOUT_SECONDS = 10;

    private static final String INDENTATION = "    ";

    private static final String SIMULATOR_HOME = getSimulatorHome().getAbsolutePath();

    private static final Logger LOGGER = Logger.getLogger(Provisioner.class);

    private final File agentsFile = new File(AgentsFile.NAME);
    private final ExecutorService executor = createFixedThreadPool(10, Provisioner.class);

    private final SimulatorProperties properties;
    private final ComputeService computeService;
    private final Bash bash;
    private final HazelcastJARs hazelcastJARs;

    private final int machineWarmupSeconds;

    private final ComponentRegistry componentRegistry;
    private final File initScriptFile;

    public Provisioner(SimulatorProperties properties, ComputeService computeService, Bash bash, HazelcastJARs hazelcastJARs,
                       boolean enterpriseEnabled) {
        this(properties, computeService, bash, hazelcastJARs, enterpriseEnabled, MACHINE_WARMUP_WAIT_SECONDS);
    }

    public Provisioner(SimulatorProperties properties, ComputeService computeService, Bash bash, HazelcastJARs hazelcastJARs,
                       boolean enterpriseEnabled, int machineWarmupSeconds) {
        echo("Hazelcast Simulator Provisioner");
        echo("Version: %s, Commit: %s, Build Time: %s", getSimulatorVersion(), getCommitIdAbbrev(), getBuildTime());
        echo("SIMULATOR_HOME: %s", SIMULATOR_HOME);

        this.properties = properties;
        this.computeService = computeService;
        this.bash = bash;
        this.hazelcastJARs = hazelcastJARs;

        this.machineWarmupSeconds = machineWarmupSeconds;

        this.componentRegistry = loadComponentRegister(agentsFile, false);
        this.initScriptFile = getInitScriptFile(SIMULATOR_HOME);

        if (hazelcastJARs != null) {
            echo("Preparing Hazelcast JARs...");
            hazelcastJARs.prepare(enterpriseEnabled);
        } else if (enterpriseEnabled) {
            String hazelcastVersionSpec = properties.getHazelcastVersionSpec();
            echoImportant("WARNING: Hazelcast Enterprise JARs will not be uploaded for %s!", hazelcastVersionSpec);
        }
    }

    // just for testing
    HazelcastJARs getHazelcastJARs() {
        return hazelcastJARs;
    }

    // just for testing
    ComponentRegistry getComponentRegistry() {
        return componentRegistry;
    }

    void scale(int size) {
        ensureIsCloudProviderSetup(properties, "scale");

        int agentSize = componentRegistry.agentCount();
        int delta = size - agentSize;
        if (delta == 0) {
            echo("Current number of machines: " + agentSize);
            echo("Desired number of machines: " + (agentSize + delta));
            echo("Ignoring spawn machines, desired number of machines already exists.");
        } else if (delta > 0) {
            scaleUp(delta);
        } else {
            scaleDown(-delta);
        }
    }

    void installSimulator() {
        ensureIsRemoteSetup(properties, "install");

        long started = System.nanoTime();
        echoImportant("Installing Simulator on %d machines...", componentRegistry.agentCount());

        ThreadSpawner spawner = new ThreadSpawner("installSimulator", true);
        for (final AgentData agentData : componentRegistry.getAgents()) {
            spawner.spawn(new Runnable() {
                @Override
                public void run() {
                    echo("Installing Simulator on %s", agentData.getPublicAddress());
                    uploadJARs(agentData.getPublicAddress());
                }
            });
        }
        spawner.awaitCompletion();

        long elapsed = getElapsedSeconds(started);
        echoImportant("Finished installing Simulator on %d machines (%s seconds)", componentRegistry.agentCount(), elapsed);
    }

    void download(final String target) {
        ensureIsRemoteSetup(properties, "download");

        long started = System.nanoTime();
        echoImportant("Download artifacts of %s machines...", componentRegistry.agentCount());
        bash.execute("mkdir -p " + target);

        ThreadSpawner spawner = new ThreadSpawner("download", true);

        final String baseCommand = "rsync --copy-links %s-avv -e \"ssh %s\" %s@%%s:%%s %s";
        final String sshOptions = properties.getSshOptions();
        final String sshUser = properties.getUser();

        final String workersPath = format("hazelcast-simulator-%s/workers/*", getSimulatorVersion());

        final String rsyncCommand = format(baseCommand, "", sshOptions, sshUser, target);
        final String rsyncCommandSuffix = format(baseCommand, "--backup --suffix=-%s ", sshOptions, sshUser, target);

        final File agentOut = new File(target, "agent.out");
        final File agentErr = new File(target, "agent.err");

        // download Worker logs
        for (final AgentData agentData : componentRegistry.getAgents()) {
            spawner.spawn(new Runnable() {
                @Override
                public void run() {
                    String ip = agentData.getPublicAddress();

                    echo("Downloading Worker logs from %s", ip);
                    bash.executeQuiet(format(rsyncCommand, ip, workersPath));
                }
            });
        }

        // download Agent logs
        spawner.spawn(new Runnable() {
            @Override
            public void run() {
                for (final AgentData agentData : componentRegistry.getAgents()) {
                    String ip = agentData.getPublicAddress();
                    String agentAddress = agentData.getAddress().toString();

                    echo("Downloading Agent logs from %s", ip);
                    bash.executeQuiet(format(rsyncCommandSuffix, ip, ip, "agent.out"));
                    bash.executeQuiet(format(rsyncCommandSuffix, ip, ip, "agent.err"));

                    rename(agentOut, new File(target, agentAddress + '-' + ip + "-agent.out"));
                    rename(agentErr, new File(target, agentAddress + '-' + ip + "-agent.err"));
                }
            }
        });

        spawner.awaitCompletion();

        long elapsed = getElapsedSeconds(started);
        echoImportant("Finished downloading artifacts of %s machines (%s seconds)", componentRegistry.agentCount(), elapsed);
    }

    void clean() {
        ensureIsRemoteSetup(properties, "clean");

        long started = System.nanoTime();
        echoImportant("Cleaning Worker homes of %s machines...", componentRegistry.agentCount());
        final String cleanCommand = format("rm -fr hazelcast-simulator-%s/workers/*", getSimulatorVersion());

        ThreadSpawner spawner = new ThreadSpawner("clean", true);
        for (final AgentData agentData : componentRegistry.getAgents()) {
            spawner.spawn(new Runnable() {
                @Override
                public void run() {
                    echo("Cleaning %s", agentData.getPublicAddress());
                    bash.ssh(agentData.getPublicAddress(), cleanCommand);
                }
            });
        }
        spawner.awaitCompletion();

        long elapsed = getElapsedSeconds(started);
        echoImportant("Finished cleaning Worker homes of %s machines (%s seconds)", componentRegistry.agentCount(), elapsed);
    }

    void killJavaProcesses() {
        ensureIsRemoteSetup(properties, "kill");

        long started = System.nanoTime();
        echoImportant("Killing %s Java processes...", componentRegistry.agentCount());

        ThreadSpawner spawner = new ThreadSpawner("killJavaProcesses", true);
        for (final AgentData agentData : componentRegistry.getAgents()) {
            spawner.spawn(new Runnable() {
                @Override
                public void run() {
                    echo("Killing Java processes on %s", agentData.getPublicAddress());
                    bash.killAllJavaProcesses(agentData.getPublicAddress());
                }
            });
        }
        spawner.awaitCompletion();

        long elapsed = getElapsedSeconds(started);
        echoImportant("Successfully killed %s Java processes (%s seconds)", componentRegistry.agentCount(), elapsed);
    }

    void terminate() {
        ensureIsCloudProviderSetup(properties, "terminate");

        scaleDown(Integer.MAX_VALUE);
    }

    void shutdown() {
        echo("Shutting down Provisioner...");

        // shutdown thread pool
        try {
            executor.shutdown();
            executor.awaitTermination(EXECUTOR_TERMINATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception ignored) {
            EmptyStatement.ignore(ignored);
        }

        // shutdown compute service (which holds another thread pool)
        if (computeService != null) {
            computeService.getContext().close();
        }

        if (hazelcastJARs != null) {
            hazelcastJARs.shutdown();
        }

        echo("Done!");
    }

    @SuppressWarnings("PMD.PreserveStackTrace")
    private void scaleUp(int delta) {
        echoImportant("Provisioning %s %s machines", delta, properties.getCloudProvider());
        echo("Current number of machines: " + componentRegistry.agentCount());
        echo("Desired number of machines: " + (componentRegistry.agentCount() + delta));

        String groupName = properties.get("GROUP_NAME", "simulator-agent");
        echo("GroupName: " + groupName);
        echo("Username: " + properties.getUser());
        echo("Using init script: " + initScriptFile.getAbsolutePath());

        String jdkFlavor = properties.get("JDK_FLAVOR", "outofthebox");
        if ("outofthebox".equals(jdkFlavor)) {
            echo("JDK spec: outofthebox");
        } else {
            String jdkVersion = properties.get("JDK_VERSION", "7");
            echo("JDK spec: %s %s", jdkFlavor, jdkVersion);
        }

        long started = System.nanoTime();
        Template template = new TemplateBuilder(computeService, properties).build();
        String startHarakiriMonitorCommand = getStartHarakiriMonitorCommandOrNull(properties);

        try {
            echo("Creating machines (can take a few minutes)...");
            Set<Future> futures = new HashSet<Future>();
            for (int batch : calcBatches(properties, delta)) {
                Set<? extends NodeMetadata> nodes = computeService.createNodesInGroup(groupName, batch, template);
                for (NodeMetadata node : nodes) {
                    String privateIpAddress = node.getPrivateAddresses().iterator().next();
                    String publicIpAddress = node.getPublicAddresses().iterator().next();

                    echo(INDENTATION + publicIpAddress + " LAUNCHED");
                    appendText(publicIpAddress + ',' + privateIpAddress + NEW_LINE, agentsFile);

                    componentRegistry.addAgent(publicIpAddress, privateIpAddress);
                }

                for (NodeMetadata node : nodes) {
                    String publicIpAddress = node.getPublicAddresses().iterator().next();
                    Future future = executor.submit(new InstallNodeTask(publicIpAddress, startHarakiriMonitorCommand));
                    futures.add(future);
                }
            }

            for (Future future : futures) {
                future.get();
            }
        } catch (Exception e) {
            throw new CommandLineExitException("Failed to provision machines: " + e.getMessage());
        }

        echo("Pausing for machine warmup... (%d sec)", machineWarmupSeconds);
        sleepSeconds(machineWarmupSeconds);

        long elapsed = getElapsedSeconds(started);
        echoImportant("Successfully provisioned %s %s machines (%s seconds)", delta, properties.getCloudProvider(), elapsed);
    }

    private void scaleDown(int count) {
        if (count > componentRegistry.agentCount()) {
            count = componentRegistry.agentCount();
        }

        echoImportant("Terminating %s %s machines (can take some time)", count, properties.getCloudProvider());
        echo("Current number of machines: " + componentRegistry.agentCount());
        echo("Desired number of machines: " + (componentRegistry.agentCount() - count));

        long started = System.nanoTime();

        int destroyedCount = 0;
        for (int batchSize : calcBatches(properties, count)) {
            Map<String, AgentData> terminateMap = new HashMap<String, AgentData>();
            for (AgentData agentData : componentRegistry.getAgents(batchSize)) {
                terminateMap.put(agentData.getPublicAddress(), agentData);
            }
            Set destroyedSet = computeService.destroyNodesMatching(new NodeMetadataPredicate(componentRegistry, terminateMap));
            destroyedCount += destroyedSet.size();
        }

        echo("Updating " + agentsFile.getAbsolutePath());
        AgentsFile.save(agentsFile, componentRegistry);

        long elapsed = getElapsedSeconds(started);
        echoImportant("Terminated %s of %s machines (%s remaining) (%s seconds)", destroyedCount, count,
                componentRegistry.agentCount(), elapsed);

        if (destroyedCount != count) {
            throw new IllegalStateException("Terminated " + destroyedCount + " of " + count
                    + NEW_LINE + "1) You are trying to terminate physical hardware that you own (unsupported feature)"
                    + NEW_LINE + "2) If and only if you are using AWS our Harakiri Monitor might have terminated them"
                    + NEW_LINE + "3) You have not payed you bill and your instances have been terminated by your cloud provider"
                    + NEW_LINE + "4) You have terminated your own instances (perhaps via some console interface)"
                    + NEW_LINE + "5) Someone else has terminated your instances"
                    + NEW_LINE + "Please try again!");
        }
    }

    private void uploadJARs(String ip) {
        String simulatorVersion = getSimulatorVersion();
        bash.ssh(ip, format("mkdir -p hazelcast-simulator-%s/lib/", simulatorVersion));
        bash.ssh(ip, format("mkdir -p hazelcast-simulator-%s/user-lib/", simulatorVersion));

        // delete the old lib folder to prevent different versions of the same JAR to bite us
        bash.sshQuiet(ip, format("rm -f hazelcast-simulator-%s/lib/*", simulatorVersion));

        // delete the old user-lib folder to prevent interference with older setups
        bash.sshQuiet(ip, format("rm -f hazelcast-simulator-%s/user-lib/*", simulatorVersion));

        // upload Simulator JARs
        uploadLibraryJar(ip, "simulator-*");
        uploadLibraryJar(ip, "probes-*");
        uploadLibraryJar(ip, "tests-*");
        uploadLibraryJar(ip, "utils-*");

        // we don't copy all JARs to the agent to increase upload speed, e.g. YourKit is uploaded on demand by the Coordinator
        uploadLibraryJar(ip, "cache-api*");
        uploadLibraryJar(ip, "commons-codec*");
        uploadLibraryJar(ip, "commons-lang3*");
        uploadLibraryJar(ip, "gson-*");
        uploadLibraryJar(ip, "guava-*");
        uploadLibraryJar(ip, "javassist-*");
        uploadLibraryJar(ip, "jopt*");
        uploadLibraryJar(ip, "junit*");
        uploadLibraryJar(ip, "HdrHistogram-*");
        uploadLibraryJar(ip, "log4j*");
        uploadLibraryJar(ip, "netty-*");
        uploadLibraryJar(ip, "slf4j-log4j12-*");

        // upload remaining files
        bash.uploadToRemoteSimulatorDir(ip, SIMULATOR_HOME + "/bin/", "bin");
        bash.uploadToRemoteSimulatorDir(ip, SIMULATOR_HOME + "/conf/", "conf");
        bash.uploadToRemoteSimulatorDir(ip, SIMULATOR_HOME + "/jdk-install/", "jdk-install");
        bash.uploadToRemoteSimulatorDir(ip, SIMULATOR_HOME + "/tests/", "tests");
        bash.uploadToRemoteSimulatorDir(ip, SIMULATOR_HOME + "/user-lib/", "user-lib/");

        // purge Hazelcast JARs
        bash.sshQuiet(ip, format("rm -rf hazelcast-simulator-%s/hz-lib", simulatorVersion));

        // upload Hazelcast JARs if configured
        if (hazelcastJARs != null) {
            echo("Uploading Hazelcast JARs on %s", ip);
            hazelcastJARs.upload(ip, SIMULATOR_HOME);
        }

        String initScript = loadInitScript();
        bash.ssh(ip, initScript);
    }

    private void uploadLibraryJar(String ip, String jarName) {
        bash.uploadToRemoteSimulatorDir(ip, SIMULATOR_HOME + "/lib/" + jarName, "lib");
    }

    private String loadInitScript() {
        String initScript = fileAsText(initScriptFile);

        initScript = initScript.replaceAll(Pattern.quote("${user}"), properties.getUser());
        initScript = initScript.replaceAll(Pattern.quote("${version}"), getSimulatorVersion());

        return initScript;
    }

    private void echo(String message, Object... args) {
        LOGGER.info(message == null ? "null" : format(message, args));
    }

    private void echoImportant(String message, Object... args) {
        echo(HORIZONTAL_RULER);
        echo(message, args);
        echo(HORIZONTAL_RULER);
    }

    private final class InstallNodeTask implements Runnable {

        private final String ip;
        private final String startHarakiriMonitorCommand;

        private InstallNodeTask(String ip, String startHarakiriMonitorCommand) {
            this.ip = ip;
            this.startHarakiriMonitorCommand = startHarakiriMonitorCommand;
        }

        @Override
        public void run() {
            // install Java if needed
            if (!"outofthebox".equals(properties.get("JDK_FLAVOR"))) {
                echo(INDENTATION + ip + " JAVA INSTALLATION STARTED...");
                bash.scpToRemote(ip, getJavaSupportScript(), "jdk-support.sh");
                bash.scpToRemote(ip, getJavaInstallScript(), "install-java.sh");
                bash.ssh(ip, "bash install-java.sh");
                echo(INDENTATION + ip + " JAVA INSTALLED");
            }

            echo(INDENTATION + ip + " SIMULATOR INSTALLATION STARTED...");
            uploadJARs(ip);
            echo(INDENTATION + ip + " SIMULATOR INSTALLED");

            if (startHarakiriMonitorCommand != null) {
                bash.ssh(ip, startHarakiriMonitorCommand);
                echo(INDENTATION + ip + " HARAKIRI MONITOR STARTED");
            }
        }

        private File getJavaInstallScript() {
            String flavor = properties.get("JDK_FLAVOR");
            String version = properties.get("JDK_VERSION");

            String script = "jdk-" + flavor + '-' + version + "-64.sh";
            File scriptDir = new File(SIMULATOR_HOME, "jdk-install");
            return new File(scriptDir, script);
        }

        private File getJavaSupportScript() {
            File scriptDir = new File(SIMULATOR_HOME, "jdk-install");
            return new File(scriptDir, "jdk-support.sh");
        }
    }

    public static void main(String[] args) {
        try {
            run(args, init(args));
        } catch (Exception e) {
            exitWithError(LOGGER, "Could not execute command", e);
        }
    }
}
