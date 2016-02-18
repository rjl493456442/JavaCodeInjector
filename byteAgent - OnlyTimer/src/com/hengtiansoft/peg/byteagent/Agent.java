package com.hengtiansoft.peg.byteagent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import com.hengtiansoft.peg.byteagent.configure.Configure;
import com.hengtiansoft.peg.byteagent.configure.XMLParser;
public class Agent implements FILED_DEFINATION {
    private static boolean firstTime = true;
    public static void premain(String args, Instrumentation inst) throws Exception
    {
        CONFIGURE_PATH = null;
        // 防止用户重复加载agent
        synchronized (Agent.class)
        {
            if (firstTime)
            {
                firstTime = false;
            }else
            {
                throw new Exception("Main : attempting to load Agent agent more than once");
            }
        }
        boolean installPolicy = false;
        System.err.println("agent args:");
        System.err.println(args);
        if (args != null)
        {
            // javaagent:<jarPath>[=<options>]
            // options间用,分隔
            String [] argsArray = args.split(",");
            for (String arg : argsArray)
            {
                if (arg.startsWith(CONFIGURE_PREFIX))
                {
                    CONFIGURE_PATH = arg.substring(arg.lastIndexOf(":") + 1);
                } else if (arg.startsWith(BOOT_PREFIX)) {
                    bootJarPaths.add(arg.substring(BOOT_PREFIX.length(), arg.length()));
                } else if (arg.startsWith(SYS_PREFIX)) {
                    sysJarPaths.add(arg.substring(SYS_PREFIX.length(), arg.length()));
                } else if (arg.startsWith(ADDRESS_PREFIX)) {
                    hostname = arg.substring(ADDRESS_PREFIX.length(), arg.length());
                    if (managerClassName == null) {
                        managerClassName=MANAGER_NAME;
                    }
                } else if (arg.startsWith(PORT_PREFIX)) {
                    try {
                        port = Integer.valueOf(arg.substring(PORT_PREFIX.length(), arg.length()));
                        if (port <= 0) {
                            System.err.println("Invalid port specified [" + port + "]");
                            port = null;
                        } else if (managerClassName == null) {
                            managerClassName=MANAGER_NAME;
                        }
                    } catch (Exception e) {
                        System.err.println("Invalid port specified [" + arg + "]. Cause: " + e);
                    }
                } else if (arg.startsWith(LISTENER_PREFIX)) {
                    // listener:true is an alias for manager:o.j.b.a.TransformListener
                    // listener:false means no manager (yes, not even TransformListener)
                    String value = arg.substring(LISTENER_PREFIX.length(), arg.length());
                    if (Boolean.parseBoolean(value)) {
                        managerClassName = MANAGER_NAME;
                    } else {
                        managerClassName = null;
                    }
                } else if (arg.startsWith(PROP_PREFIX)) {
                    // this can be used to set byteman properties
                    String prop = arg.substring(PROP_PREFIX.length(), arg.length());
                    String value="";
                    if (prop.startsWith(AGENT_PREFIX)) {
                        int index = prop.indexOf('=');
                        if (index > 0) {
                            // need to split off the value
                            if (index == prop.length() - 1)
                            {
                                // value is empty so just drop the =
                                prop = prop.substring(0, index);
                            } else {
                                value = prop.substring(index + 1);
                                prop = prop.substring(0, index);
                            }
                        }
                        System.out.println("Setting " + prop + "=" + value);
                        System.setProperty(prop, value);
                    } else {
                        System.err.println("Invalid property : " +  prop);
                    }
                } else if (arg.startsWith(POLICY_PREFIX)) {
                    String value = arg.substring(POLICY_PREFIX.length(), arg.length());
                    installPolicy = Boolean.parseBoolean(value);
                } else {
                    System.err.println("agent:\n" +
                            "  illegal agent argument : " + arg + "\n" +
                            "  valid arguments are boot:<path-to-jar>, sys:<path-to-jar>, or listener:<true-or-false>");
                }
            }

            // add any boot jars to the boot class path

            for (String bootJarPath : bootJarPaths) {
                try {
                    JarFile jarfile = new JarFile(new File(bootJarPath));
                    inst.appendToBootstrapClassLoaderSearch(jarfile);
                } catch (IOException ioe) {
                    System.err.println("agent.Main: unable to open boot jar file : " + bootJarPath);
                    throw ioe;
                }
            }

            // add any sys jars to the system class path

            for (String sysJarPath : sysJarPaths) {
                try {
                    JarFile jarfile = new JarFile(new File(sysJarPath));
                    inst.appendToSystemClassLoaderSearch(jarfile);
                } catch (IOException ioe) {
                    System.err.println("agent.Main: unable to open system jar file : " + sysJarPath);
                    throw ioe;
                }
            }

            Socket dummy = new Socket();

            for(String path : scriptPaths)
            {
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(path);
                    byte[] bytes = new byte[fis.available()];
                    fis.read(bytes);
                    String ruleScript = new String(bytes);
                } catch (IOException ioe) {
                    System.err.println("agent.Main: unable to read rule script file : " + path);
                    throw ioe;
                } finally {
                    if (fis != null)
                        fis.close();
                }
            }
        }

        if (CONFIGURE_PATH == null )
        {
            CONFIGURE_PATH = System.getProperty("user.dir") + File.separator + "configure.xml";
        }
        Configure configure = XMLParser.parserConfigXMLFile(CONFIGURE_PATH);

        if(configure.getField(PACKAGE_NAME).equals(""))
        {
            System.err.println("no package name specific in " + CONFIGURE_PATH + ", which can lead to an accurate analysis result");
        }

        if(configure.getField(LOG_NAME).equals(""))
        {
            System.err.println("no output log file specific, user default output log named output.log");
            configure.setField(LOG_NAME,"output.log");
        }

        // load Transformer class
        ClassLoader loader = ClassLoader.getSystemClassLoader();
        Class /*<Transformer>*/ transformerClazz;
        ClassFileTransformer transformer;
        boolean isRedefine = inst.isRedefineClassesSupported();
        if (managerClassName != null && isRedefine)
        {
            transformerClazz = loader.loadClass(RETRANSFORMER_NAME);
            Constructor constructor = transformerClazz.getConstructor(Instrumentation.class, String.class );
            transformer = (ClassFileTransformer) constructor.newInstance(new Object[]{inst, CONFIGURE_PATH });
        }
        else {
            transformerClazz = loader.loadClass(TRANSFORMER_NAME);
            Constructor/*<Transformer>*/ constructor = transformerClazz.getConstructor(Instrumentation.class, String.class );
            transformer = (ClassFileTransformer) constructor.newInstance(new Object[]{inst, CONFIGURE_PATH});
        }
        inst.addTransformer(transformer, true);

        if (managerClassName != null && isRedefine) {
            Class managerClazz = loader.loadClass(managerClassName);
            try {
                Method method = managerClazz.getMethod("initialize", transformerClazz, String.class, Integer.class);
                method.invoke(null, transformer, hostname, port);
            } catch (NoSuchMethodException e) {
                System.err.println("initialize listener failed");
            }
        }

        if (installPolicy) {
            System.err.println("policy installed");
            Method method = transformerClazz.getMethod("installPolicy");
            method.invoke(transformer);
        }
        /*
        if (isRedefine && managerClassName != null)
        {
            // TODO wthether to add initial rule
            Method method = transformerClazz.getMethod("installBootScripts");
            method.invoke(transformer);
        }
        */

    }

    // 动态注入时,jvm默认调用agentmain
    public static void agentmain(String args, Instrumentation inst) throws Exception
    {
        premain(args, inst);
    }

    /**
     * use to specify agent jar prefix
     */
    private static final String AGENT_PREFIX = "com.hengtiansoft.peg.byteagent.";

    /**
     * prefix used to specify port argument for agent
     */
    private static final String PORT_PREFIX = "port:";

    /**
     * prefix used to specify bind address argument for agent
     */
    private static final String ADDRESS_PREFIX = "address:";

    /**
     * prefix used to specify boot jar argument for agent
     */
    private static final String BOOT_PREFIX = "boot:";

    /**
     * prefix used to specify system jar argument for agent
     */
    private static final String SYS_PREFIX = "sys:";

    /**
     * prefix used to request installation of an access-all-areas security
     * policy at install time for agent code
     */
    private static final String POLICY_PREFIX = "policy:";

    /**
     * prefix used to specify transformer type argument for agent
     */
    private static final String LISTENER_PREFIX = "listener:";

    /**
     * prefix used to specify system properties to be set before starting the agent
     */
    private static final String PROP_PREFIX = "prop:";

    /**
     * name of basic transformer class.
     */
    private static final String TRANSFORMER_NAME = AGENT_PREFIX + "Transformer";

    /**
     * name of retransformer class.
     */
    private static final String RETRANSFORMER_NAME = AGENT_PREFIX + "Retransformer";

    /**
     * name of default manager class.
     */
    private static final String MANAGER_NAME = AGENT_PREFIX + "TransformerListener";

    /**
     * use to specify configure file name
     */
    private static final String CONFIGURE_PREFIX = "configure:";

    /**
     * use to specify configure path
     */
    private static  String CONFIGURE_PATH;



    /**
     * list of paths to extra bootstrap jars supplied on command line
     */
    private static List<String> bootJarPaths = new ArrayList<String>();

    /**
     * list of paths to extra system jars supplied on command line
     */
    private static List<String> sysJarPaths = new ArrayList<String>();

    /**
     * list of paths to script files supplied on command line
     */
    private static List<String> scriptPaths = new ArrayList<String>();

    /**
     * The hostname to bind the listener to, supplied on the command line (optional argument)
     */
    private static String hostname = null;

    /**
     * The port that the listener will listen to, supplied on the command line (optional argument)
     */
    private static Integer port = null;

    /**
     * The name of the manager class responsible for loading/unloading scripts, supplied on the
     * command line (optional argument)
     */
    private static String managerClassName = null;
}

