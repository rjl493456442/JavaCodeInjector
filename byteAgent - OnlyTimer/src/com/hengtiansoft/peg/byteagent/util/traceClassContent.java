package com.hengtiansoft.peg.byteagent.util;
import org.objectweb.asm.ClassReader;
import java.io.IOException;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.*;
import org.objectweb.asm.util.TraceClassVisitor;
import java.io.*;

/**
 * Created by jialeirong on 1/21/2016.
 */

public class traceClassContent {
    private static final String CLASS_PATH_ROOT = System.getProperty("user.dir");
    private static final Integer BUFFER_SIZE = new Integer(4096);
    private static byte[] BUFFER = new byte[BUFFER_SIZE];

    public static void main(String[] args)
    {
        try {
            Trace("TimerStack");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void Trace(String className) throws IOException {
        BUFFER = getClassData(convertCLassName2Path(className));
        if (BUFFER != null) {
            ClassReader cr = new ClassReader(BUFFER);
            TraceClassVisitor cv = new TraceClassVisitor(new PrintWriter(System.out));
            //ClassPrint cp = new ClassPrint(Opcodes.ASM5);
            cr.accept(cv, 0);
        }
        else {
            System.err.println("No class file data");
        }
    }
    public static void TraceWithByte(byte[] classData)
    {
        ClassReader cr = new ClassReader(classData);
        TraceClassVisitor cv = new TraceClassVisitor(new PrintWriter(System.out));
        cr.accept(cv, 0);
    }
    private static byte[] getClassData(String classPath)
    {
        System.out.println(classPath);
        try {
            InputStream ins =  new FileInputStream(classPath);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int cnt = 0;
            while ((cnt = ins.read(BUFFER)) != -1)
            {
                out.write(BUFFER, 0, cnt);
            }
            return out.toByteArray();
        }catch (FileNotFoundException e)
        {
            System.err.println("The specific class file not found!");
            return null;
        }catch (IOException e)
        {
            System.err.println("IOException occur :" + e.getMessage());
            return null;
        }
    }
    private static String convertCLassName2Path(String className)
    {
        if (className.lastIndexOf(".") == -1)
        {
            return CLASS_PATH_ROOT + File.separator + className + ".class";
        }
        else{
            return CLASS_PATH_ROOT + File.separator + className.replace('.', File.separatorChar) + ".class";
        }
    }
}

class ClassPrint extends ClassVisitor{
    public ClassPrint(int api)
    {
        super(api);
    }
    @Override
    public void visitOuterClass(String s, String s1, String s2) {

    }

    @Override
    public void visit(int i, int i1, String s, String s1, String s2, String[] strings) {
        System.out.print( s + "extends" + s2 + "{");
    }

    @Override
    public void visitAttribute(Attribute attribute) {
    }

    @Override
    public FieldVisitor visitField(int i, String s, String s1, String s2, Object o) {
        System.out.println(s1  + " " + s2);
        return null;
    }

    @Override
    public MethodVisitor visitMethod(int i, String s, String s1, String s2, String[] strings) {
        System.out.println(s + s1);
        return null;
    }

    @Override
    public void visitEnd() {
        System.out.println("}");
    }
}