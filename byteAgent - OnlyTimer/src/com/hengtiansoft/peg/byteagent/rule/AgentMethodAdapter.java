package com.hengtiansoft.peg.byteagent.rule;
import com.hengtiansoft.peg.byteagent.*;
import com.hengtiansoft.peg.byteagent.configure.Configure;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.LocalVariablesSorter;

import java.util.List;

/**
 * Created by jialeirong on 2/1/2016.
 */

/**
 * parent method adapter
 */
public class AgentMethodAdapter extends MethodVisitor implements FILED_DEFINATION, VARIABLES_DEFINATION, RULES_DEFINATION
{
    protected String methodName;
    public LocalVariablesSorter lvs;
    // specify the class method belong to
    public String  owner;
    public Configure configure;
    public boolean isInjectTimer = false;
    public boolean isInjectPosition = false;
    public ScriptRepository scriptRepository;
    public AgentMethodAdapter(int api, String methodName, String owner, Configure configure, ScriptRepository scriptRepository)
    {
        super(api);
        this.methodName = methodName;
        this.owner = owner;
        this.configure = configure;
        this.scriptRepository = scriptRepository;
        initialize();
    }
    public AgentMethodAdapter(int api, MethodVisitor mv, String methodName, String owner, Configure configure, ScriptRepository scriptRepository)
    {
        super(api, mv);
        this.methodName = methodName;
        this.owner = owner;
        this.configure = configure;
        this.scriptRepository = scriptRepository;
        initialize();
    }

    /**
     * initialize LocalVariablesSorter
     * @param access
     * @param desc
     * @param mv
     */
    public void setLocalVariableAdapter(int access, String desc, MethodVisitor mv)
    {
        this.lvs = new LocalVariablesSorter(access, desc, mv);
    }

    /**
     * abstract function to initial localVariable which will be insert to target bytecode later
     */
    public void initialLocalVariable()
    {
    }

    /**
     * extract rules
     */
    public void initialize()
    {
        if (scriptRepository.isExist(INJECT_TIMER))
        {
            isInjectTimer = true;
        }
    }
}

