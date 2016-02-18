package com.hengtiansoft.peg.byteagent.rule;

import com.hengtiansoft.peg.byteagent.ScriptRepository;
import com.hengtiansoft.peg.byteagent.configure.Configure;
import com.sun.crypto.provider.OAEPParameters;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Created by jialeirong on 2/1/2016.
 */
public class AgentCommonMethodAdapter extends AgentMethodAdapter {
    // localVariable
    private int logger;
    private int timerBegin;
    private int localString;
    private int fileHandler;
    private int ioException;


    private Label logFileHandleOpenException = new Label();
    private boolean isTargetMethod;
    public AgentCommonMethodAdapter(int api, String methodName, String owner , Configure configure, boolean isTargetMethod, ScriptRepository scriptRepository) {
        super(api, methodName, owner, configure, scriptRepository);
        this.isTargetMethod = isTargetMethod;
    }

    public AgentCommonMethodAdapter(int api, MethodVisitor mv, String methodName, String owner, Configure configure, boolean isTargetMethod, ScriptRepository scriptRepository) {
        super(api, mv, methodName, owner, configure, scriptRepository);
        this.isTargetMethod = isTargetMethod;
    }

    @Override
    public void initialLocalVariable() {

        logger = lvs.newLocal(Type.LONG_TYPE);
        timerBegin = lvs.newLocal(Type.LONG_TYPE);
        localString = lvs.newLocal(Type.LONG_TYPE);
        fileHandler = lvs.newLocal(Type.LONG_TYPE);
        ioException = lvs.newLocal(Type.LONG_TYPE);
    }

    // 访问函数代码部分
    @Override
    public void visitCode() {
        mv.visitCode();
        if (!scriptRepository.isEmpty() && isTargetMethod) {
            injectLogger();
            if (isInjectTimer) {
                setTimer();
            }
        }
    }

    // 访问函数结束部分
    // 还包括抛出异常部分
    @Override
    public void visitInsn(int opcode) {
        if ((opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) || opcode == Opcodes.ATHROW) {
            if (!scriptRepository.isEmpty() && isTargetMethod)
            {
                if (isInjectTimer)
                {
                    logElapsedTime();
                }
            }
        }
        mv.visitInsn(opcode);
    }
    public void injectLogger()
    {
        mv.visitLdcInsn("byteAgent");
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/logging/Logger", "getLogger", "(Ljava/lang/String;)Ljava/util/logging/Logger;", false);
        mv.visitVarInsn(Opcodes.ASTORE, logger);
        mv.visitInsn(Opcodes.ACONST_NULL);
        mv.visitVarInsn(Opcodes.ASTORE, fileHandler);
        // fileHandler = new FileHandler(path/to/logfile)
        mv.visitTypeInsn(Opcodes.NEW, "java/util/logging/FileHandler");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn(configure.getField(LOG_NAME));
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/util/logging/FileHandler", "<init>", "(Ljava/lang/String;)V", false);
        mv.visitVarInsn(Opcodes.ASTORE, fileHandler);
        // try catch block
        mv.visitJumpInsn(Opcodes.GOTO, logFileHandleOpenException);
        mv.visitVarInsn(Opcodes.ASTORE,ioException);
        mv.visitVarInsn(Opcodes.ALOAD,ioException);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/IOException", "printStackTrace", "()V", false);
        mv.visitLabel(logFileHandleOpenException);
        // setlevel
        mv.visitVarInsn(Opcodes.ALOAD, fileHandler);
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/util/logging/Level", "INFO", "Ljava/util/logging/Level;");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/logging/FileHandler", "setLevel", "(Ljava/util/logging/Level;)V", false);
        // logger.addHandler(fileHandler)
        mv.visitVarInsn(Opcodes.ALOAD, logger);
        mv.visitVarInsn(Opcodes.ALOAD, fileHandler);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/logging/Logger", "addHandler", "(Ljava/util/logging/Handler;)V", false);
        // set use parent false
        mv.visitVarInsn(Opcodes.ALOAD, logger);
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/logging/Logger", "setUseParentHandlers", "(Z)V", false);
    }
    public void setTimer()
    {
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
        mv.visitVarInsn(Opcodes.ASTORE, timerBegin);
    }
    public void logElapsedTime()
    {
        // elapsedTime = current - begin;
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
        mv.visitLdcInsn("[Takes(nano Sec)]");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false);
        mv.visitVarInsn(Opcodes.ALOAD, timerBegin);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false);
        mv.visitInsn(Opcodes.LSUB);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "toString", "(J)Ljava/lang/String;", false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
        mv.visitVarInsn(Opcodes.ASTORE, localString);
        // log.info
        mv.visitVarInsn(Opcodes.ALOAD, logger);
        mv.visitVarInsn(Opcodes.ALOAD, localString);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/logging/Logger", "info", "(Ljava/lang/String;)V", false);
    }

}
