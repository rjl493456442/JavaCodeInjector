package com.hengtiansoft.peg.byteagent.install;

import com.sun.tools.attach.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.jar.JarFile;

/**
 * Created by jialeirong on 1/26/2016.
 */
public class Install {
    public static void main(String[] args) {
        Install attachMachine = new Install();
        attachMachine.parseArgs(args);
        try{
            attachMachine.locateAgent();
            attachMachine.attach();
            attachMachine.injectAgent();
        }catch (Exception e)
        {
            System.err.println(e);
            System.exit(1);
        }
    }
    /**
     *  only this class creates instances
     */
    private Install()
    {
        agentJar = null;
        id = null;
        port = 0;
        addToBoot = false;
        props="";
        vm = null;
    }
    /**
     * attach to the Java process identified by the process id supplied on the command line
     */
    private void attach() throws AttachNotSupportedException, IOException, IllegalArgumentException
    {

        if (id.matches("[0-9]+")) {
            // integer process id
            int pid = Integer.valueOf(id);
            if (pid <= 0) {
                throw new IllegalArgumentException("Install : invalid pid " +id);
            }
            vm = VirtualMachine.attach(Integer.toString(pid));
        } else {
            // try to search for this VM with an exact match
            List<VirtualMachineDescriptor> vmds = VirtualMachine.list();
            for (VirtualMachineDescriptor vmd: vmds) {
                String displayName = vmd.displayName();
                int spacePos = displayName.indexOf(' ');
                if (spacePos > 0) {
                    displayName = displayName.substring(0, spacePos);
                }
                if (displayName.equals(id)) {
                    String pid = vmd.id();
                    vm = VirtualMachine.attach(vmd);
                    return;
                }
            }
            // lets see if we can find a trailing match e.g. if the displayName
            // is org.jboss.Main we will accept jboss.Main or Main
            for (VirtualMachineDescriptor vmd: vmds) {
                String displayName = vmd.displayName();
                int spacePos = displayName.indexOf(' ');
                if (spacePos > 0) {
                    displayName = displayName.substring(0, spacePos);
                }

                if (displayName.indexOf('.') >= 0 && displayName.endsWith(id)) {
                    int idx = displayName.length() - (id.length() + 1);
                    if (displayName.charAt(idx) == '.') {
                        String pid = vmd.id();
                        vm = VirtualMachine.attach(vmd);
                        return;
                    }
                }
            }
            // no match so throw an exception
            throw new IllegalArgumentException("Install : invalid pid " + id);
        }
        //!! TODO -- find a way for the agent to notify it is already loaded via an agent property
    }
    /**
     * get the attached process to upload and install the agent jar using whatever agent options were
     * configured on the command line
     */
    private void injectAgent() throws AgentLoadException, AgentInitializationException, IOException
    {
        try {
            // we need at the very least to enable the listener so that scripts can be uploaded
            String agentOptions = "listener:true";
            if (host != null && host.length() != 0) {
                agentOptions += ",address:" + host;
            }
            if (port != 0) {
                agentOptions += ",port:" + port;
            }
            if (addToBoot) {
                agentOptions += ",boot:" + agentJar;
            }
            if (setPolicy) {
                agentOptions += ",policy:true";
            }
            if (props != null) {
                agentOptions += props;
            }
            vm.loadAgent(agentJar, agentOptions);
        } finally {
            vm.detach();
        }
    }
    /**
     * check the supplied arguments and stash away te relevant data
     * @param args the value supplied to main
     */
    private void parseArgs(String[] args)
    {
        int argCount = args.length;
        int idx = 0;
        if (idx == argCount) {
            usage(0);
        }
        String nextArg = args[idx];
        while (nextArg.length() != 0 &&
                nextArg.charAt(0) == '-') {
            if (nextArg.equals("-p")) {
                idx++;
                if (idx == argCount) {
                    usage(1);
                }
                nextArg = args[idx];
                idx++;
                try {
                    port = Integer.decode(nextArg);
                } catch (NumberFormatException e) {
                    System.out.println("Install : invalid value for port " + nextArg);
                    usage(1);
                }
            }
            else if (nextArg.equals("-h")) {
                idx++;
                if (idx == argCount) {
                    usage(1);
                }
                nextArg = args[idx];
                idx++;
                host = nextArg;
            } else if (nextArg.equals("-b")) {
                idx++;
                addToBoot = true;
            } else if (nextArg.equals("-s")) {
                idx++;
                setPolicy = true;
            } else if (nextArg.startsWith("-D")) {
                idx++;
                String prop=nextArg.substring(2);
                if (!prop.startsWith(AGENT_PREFIX) || prop.contains(",")) {
                    System.out.println("Install : invalid property setting " + prop);
                    usage(1);
                }
                props = props + ",prop:" + prop;
            } else if (nextArg.equals("--help")) {
                usage(0);
            } else {
                System.out.println("Install : invalid option " + args[idx]);
                usage(1);
            }
            if (idx == argCount) {
                usage(1);
            }
            nextArg = args[idx];
        }

        if (idx != argCount - 1) {
            usage(1);
        }

        // we actually allow any string for the process id as we can look up by name also
        id = nextArg;
    }
    /**
     * Check for system property org.jboss.byteman.home in preference to the environment setting
     * BYTEMAN_HOME and use it to identify the location of the byteman agent jar.
     */
    private void locateAgent() throws IOException
    {
        String agentHome = System.getProperty(AGENT_HOME_SYSTEM_PROP);
        if (agentHome == null || agentHome.length() == 0) {
            agentHome = System.getenv(AGENT_HOME_ENV_VAR);
        }
        if (agentHome == null || agentHome.length() == 0 || agentHome.equals("null")) {
            locateAgentFromClasspath();
        } else {
            locateAgentFromHomeDir(agentHome);
        }
    }

    public void locateAgentFromHomeDir(String agentHome) throws IOException
    {
        if (agentHome.endsWith(File.separator)) {
            agentHome = agentHome.substring(0, agentHome.length() - 1);
        }

        File agentHomeFile = new File(agentHome);
        if (!agentHomeFile.isDirectory()) {
            throw new FileNotFoundException("Install : ${" + AGENT_HOME_ENV_VAR + "} does not identify a directory");
        }

        File bmLibFile = new File(agentHome + File.separator + "lib");
        if (!bmLibFile.isDirectory()) {
            throw new FileNotFoundException("Install : ${" + AGENT_HOME_ENV_VAR + "}/lib does not identify a directory");
        }

        try {
            JarFile agentJarFile = new JarFile(agentHome + File.separator + "lib" + File.separator + "agent.jar");
        } catch (IOException e) {
            throw new IOException("Install : ${" + AGENT_HOME_ENV_VAR + "}/lib/agent.jar is not a valid jar file");
        }
        // TODO system platform path difference
        agentJar = agentHome + File.separator + "lib" + File.separator + "agent.jar";
    }

    public void locateAgentFromClasspath() throws IOException {
        String javaClassPath = System.getProperty("java.class.path");
        String pathSepr = System.getProperty("path.separator");
        String fileSepr = System.getProperty("file.separator");
        final String EXTENSION = ".jar";
        final int EXTENSION_LEN = EXTENSION.length();
        final String NAME = "agent";
        final int NAME_LEN = NAME.length();

        String[] elements = javaClassPath.split(pathSepr);
        String jarname = null;
        // add current work path to list
        for (String element : elements) {
            if (element.endsWith(EXTENSION)) {
                String name = element.substring(0, element.length() - EXTENSION_LEN);
                int lastFileSepr = name.lastIndexOf(fileSepr);
                if (lastFileSepr >= 0) {
                    name = name.substring(lastFileSepr + 1);
                }
                if (name.startsWith(NAME)) {
                    if (name.length() == NAME_LEN) {
                        jarname = element;
                        break;
                    }
                }
            }
        }
    }
    private static void usage(int exitValue)
    {
        System.out.println("usage : Install [-w path_to_agent.jar] [-h host] [-p port] [-b] [-Dprop[=value]]* pid");
        System.out.println("        upload the  agent into a running JVM");
        System.out.println("    pid is the process id of the target JVM or the unique name of the process as reported by the jps -l command");
        System.out.println("    -h host selects the host name or address the agent listener binds to");
        System.out.println("    -p port selects the port the agent listener binds to");
        System.out.println("    -b adds the byteman jar to the bootstrap classpath");
        System.out.println("    -s sets an access-all-areas security policy for the Byteman agent code");
        System.out.println("    -Dname=value can be used to set system properties whose name starts with \"org.jboss.byteman.\"");
        System.out.println("    expects to find a byteman agent jar in ${" + AGENT_HOME_ENV_VAR + "}/lib/byteman.jar");
        System.out.println("    (alternatively set System property " + AGENT_HOME_SYSTEM_PROP + " to overide ${" + AGENT_HOME_ENV_VAR + "})");
        System.exit(exitValue);
    }
    private String agentJar;
    private String id;
    private int port;
    private String host;
    private boolean addToBoot;
    private boolean setPolicy;
    private String props;
    private VirtualMachine vm;
    private static final String AGENT_PREFIX="com.hengtiansoft.peg.byteagent.";

    /**
     * System property used to idenitfy the location of the installed agent release.
     */
    private static final String AGENT_HOME_SYSTEM_PROP = AGENT_PREFIX + "home";

    /**
     * environment variable used to idenitfy the location of the installed agent release.
     */
    private static final String AGENT_HOME_ENV_VAR = "AGENT_HOME";
}
