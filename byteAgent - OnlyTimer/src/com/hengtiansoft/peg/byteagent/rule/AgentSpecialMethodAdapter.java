package com.hengtiansoft.peg.byteagent.rule;

import com.hengtiansoft.peg.byteagent.ScriptRepository;
import com.hengtiansoft.peg.byteagent.configure.Configure;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Created by jialeirong on 2/1/2016.
 */

/**
 * to handle clinit method in class
 * initial some class static variable
 */
public class AgentSpecialMethodAdapter extends AgentMethodAdapter {
    private int fileHandler;
    private int fileNotFoundExcetion;
    private Label fileNotFoundExceptionEnd = new Label();
    private Configure configure;

    public AgentSpecialMethodAdapter(int api, String methodName, String owner, Configure configure, ScriptRepository scriptRepository) {
        super(api, methodName, owner, configure, scriptRepository);
        this.configure = configure;
    }

    public AgentSpecialMethodAdapter(int api, MethodVisitor mv, String methodName, String owner, Configure configure, ScriptRepository scriptRepository) {
        super(api, mv, methodName, owner, configure, scriptRepository);
        this.configure = configure;
    }

    // 访问函数代码部分
    @Override
    public void initialLocalVariable() {
        fileHandler = lvs.newLocal(Type.LONG_TYPE);
        fileNotFoundExcetion = lvs.newLocal(Type.LONG_TYPE);
    }

    @Override
    public void visitCode() {
        mv.visitCode();
        injectInitialByteCode();
    }
    public  void injectInitialByteCode()
    {
        // new Stack<>()
        mv.visitTypeInsn(Opcodes.NEW, "java/util/Stack");
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/util/Stack", "<init>", "()V", false);
        mv.visitFieldInsn(Opcodes.PUTSTATIC, owner, AGENT_TIMER_STACK, "Ljava/util/Stack;");
        // new Stack<>()
        mv.visitTypeInsn(Opcodes.NEW, "java/util/Stack");
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/util/Stack", "<init>", "()V", false);
        mv.visitFieldInsn(Opcodes.PUTSTATIC, owner, AGENT_POSITION_STACK, "Ljava/util/Stack;");
        // FileHandler fileHandler = null
        mv.visitLdcInsn("byteAgent");
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/logging/Logger", "getLogger", "(Ljava/lang/String;)Ljava/util/logging/Logger;", false);
        mv.visitFieldInsn(Opcodes.PUTSTATIC, owner, AGENT_LOGGER, "Ljava/util/logging/Logger;");
        mv.visitInsn(Opcodes.ACONST_NULL);
        mv.visitVarInsn(Opcodes.ASTORE, fileHandler);

        mv.visitFieldInsn(Opcodes.GETSTATIC, owner, AGENT_LOGGER, "Ljava/util/logging/Logger;");
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/logging/Logger", "setUseParentHandlers", "(Z)V", false);

        // fileHandler = new FileHandler(path/to/logfile)
        mv.visitTypeInsn(Opcodes.NEW, "java/util/logging/FileHandler");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn(configure.getField(LOG_NAME));
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/util/logging/FileHandler", "<init>", "(Ljava/lang/String;)V", false);
        mv.visitVarInsn(Opcodes.ASTORE, fileHandler);
        // no exception found
        mv.visitJumpInsn(Opcodes.GOTO, fileNotFoundExceptionEnd);
        mv.visitVarInsn(Opcodes.ASTORE, fileNotFoundExcetion);
        mv.visitVarInsn(Opcodes.ALOAD, fileNotFoundExcetion);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/IOException", "printStackTrace", "()V", false);
        mv.visitLabel(fileNotFoundExceptionEnd);
        // FileHandler.setLevel
        mv.visitVarInsn(Opcodes.ALOAD, fileHandler);
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/util/logging/Level", "INFO", "Ljava/util/logging/Level;");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/logging/FileHandler", "setLevel", "(Ljava/util/logging/Level;)V", false);
        // logger.addHandler(fileHandler)
        mv.visitFieldInsn(Opcodes.GETSTATIC, owner, AGENT_LOGGER, "Ljava/util/logging/Logger;");
        mv.visitVarInsn(Opcodes.ALOAD, fileHandler);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/logging/Logger", "addHandler", "(Ljava/util/logging/Handler;)V", false);
    }
}
