package com.hengtiansoft.peg.byteagent;
import org.dom4j.DocumentException;
import org.objectweb.asm.*;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.Iterator;
import java.util.List;
import com.hengtiansoft.peg.byteagent.configure.XMLParser;
import com.hengtiansoft.peg.byteagent.configure.Configure;
import com.hengtiansoft.peg.byteagent.rule.*;
import com.hengtiansoft.peg.byteagent.util.traceClassContent;
/**
 * Created by jialeirong on 1/21/2016.
 */
public class Transformer implements ClassFileTransformer, FILED_DEFINATION {
    public Instrumentation inst;
    private String configureFilePath;
    Configure configure = null;
    ScriptRepository scriptRepository;
    private boolean isEmpty = true;
    public Transformer(Instrumentation inst, String configureFilePath)
    {
        this.inst = inst;
        this.configureFilePath = configureFilePath;
        scriptRepository = new ScriptRepository();
        isEmpty = scriptRepository.processRules(configureFilePath);
        XMLParser xmlParser = new XMLParser();
        try {
            configure = xmlParser.parserConfigXMLFile(configureFilePath);
        } catch (DocumentException e) {
            System.err.println("no configure file found");
        }
    }
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classFileBuffer)
            throws IllegalClassFormatException {
        ClassReader cr = new ClassReader(classFileBuffer);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        if (isUserPackageClass(className) && isTransformable(className) && !scriptRepository.isEmpty()) {
            ClassAdapter ca = new ClassAdapter(Opcodes.ASM5, cw, configure, scriptRepository);
            cr.accept(ca, 0);
            return cw.toByteArray();
        }
        else
        {
            cr.accept(cw, 0);
            return  cw.toByteArray();
        }
    }

    public boolean isUserPackageClass(String className)
    {
        if (configure.getField(PACKAGE_NAME).equals(""))
        {
            return true;
        }
        if (className.startsWith(configure.getField(PACKAGE_NAME)))
        {
            return true;
        }
        else{
            return false;
        }
    }
    public boolean isTransformable(String className)
    {
        if (className.startsWith(JAVA_LANG_PACKAGE_PREFIX) || className.startsWith(SUN_PACKAGE_PREFIX) || className.startsWith(AGENT_PREFIX)
                || className.startsWith(ORG_PREFIX) || className.startsWith(JAVAX_PREFIX) || className.startsWith(COM_SUN_PACKAGE_PREFIX))
        {
            return  false;
        }
        return true;
    }
    public void installPolicy()
    {
        AgentPolicy policy = new AgentPolicy(Policy.getPolicy());
        Policy.setPolicy(policy);
    }

    private static final  String JAVA_LANG_PACKAGE_PREFIX = "java/";
    private static final  String SUN_PACKAGE_PREFIX = "sun/";
    private static final  String COM_SUN_PACKAGE_PREFIX = "com/sun/";
    private static final  String AGENT_PREFIX = "com/hengtiansoft/peg/byteagent";
    private static final  String ORG_PREFIX = "org/";
    private static final  String JAVAX_PREFIX = "javax/";
}
class ClassAdapter extends ClassVisitor implements FILED_DEFINATION, RULES_DEFINATION
{
    // owner为当前处理类的类名
    private String owner;
    private boolean isInterface;
    private Configure configure;
    private ScriptRepository scriptRepository;
    ClassAdapter(int api, ClassVisitor cv, Configure configure, ScriptRepository scriptRepository)
    {
        super(api,cv);
        this.configure = configure;
        this.scriptRepository = scriptRepository;
    }
    ClassAdapter(int api, Configure configure, ScriptRepository scriptRepository)
    {
        super(api);
        this.configure = configure;
        this.scriptRepository = scriptRepository;
    }

    // 访问类头部的函数
    @Override
    public void visit(int version, int access, String name,
                      String signature, String superName, String[] interfaces)
    {
        // cv是从ClassVisitor继承的
        cv.visit(version, access, name, signature, superName, interfaces);
        owner = name;
        isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
    }

    // 访问类函数成员的函数
    @Override
    public MethodVisitor visitMethod(int access, String name, String desc,
                                     String signature, String[] exceptions)
    {
        MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
        // 普通函数
        System.err.println(name);
        if (!isInterface && mv != null && !name.equals("<init>") && !name.equals("<clinit>"))
        {
            try {
                boolean isTarget = isTargetMethod(name);
                mv = new AgentCommonMethodAdapter(Opcodes.ASM5, mv, name, owner,configure, isTarget, scriptRepository);
                ((AgentCommonMethodAdapter) mv).setLocalVariableAdapter(access, desc, mv);
                ((AgentCommonMethodAdapter) mv).initialLocalVariable();
            }catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        return mv;
    }
    // 类体访问结束
    // 添加类静态变量
    @Override
    public void visitEnd()
    {
        // not add any static field
        // this it not supported in retransform
        cv.visitEnd();
    }
    boolean isTargetMethod(String methodName)
    {
        // TODO add wildcard
        boolean forAllMethod = false;
        List<String> entryFunctionList = configure.getList(METHOD_LIST);
        for(Iterator<String> iterator = entryFunctionList.iterator(); iterator.hasNext();)
        {
            String m = iterator.next();
            if (methodName.equals(m))
            {
                return true;
            }
            if (m.equals("*")) forAllMethod = true;
        }
        List<String> exceptEntryFunctionList = configure.getList(EXCEPT_METHOD_LIST);
        for(String entry : exceptEntryFunctionList)
        {
            if (methodName.equals(entry))return  false;
        }
        return  forAllMethod;
    }
}


