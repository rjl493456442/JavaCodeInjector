package com.hengtiansoft.peg.byteagent;

import java.io.PrintWriter;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Created by jialeirong on 2/1/2016.
 */
public class Retransformer extends Transformer {
    private Set<String> sysJars = new HashSet<String>();  // jar files that were loaded in the sys CL
    private Set<String> bootJars = new HashSet<String>(); // jar files that were loaded in the boot CL
    /**
     * constructor
     * @param inst
     * @param configureFilePath
     */
    public Retransformer(Instrumentation inst, String configureFilePath)
    {
        super(inst, configureFilePath);
        scriptRepository.removeAllRule();
    }

    /**
     * list all rule loaded
     * @param out
     */
    public void listAllRules(PrintWriter out)
    {
        List<String> rules;
        rules = this.scriptRepository.getAllRules();
        if (rules.isEmpty())
        {
            out.println("No rule has been installed");
            return;
        }
        for(String rule: rules)
        {
            out.println(rule + " has been installed");
        }
        out.println("Totally " + Integer.toString(rules.size()) + " rules has been installed");
    }

    /**
     * install specfic rule
     * @param ruleNames
     * @param out
     */
    public void installScript(List<String> ruleNames, PrintWriter out)
    {
        // TODO add checker
        super.scriptRepository.addAllRules(ruleNames);
        scriptRepository.listAllRules();
        synchronized (this) {
            try {
                List<Class<?>> classList = getClassNeedRetrasnform();
                Class<?>[] transformedArray = new Class<?>[classList.size()];
                classList.toArray(transformedArray);
                for(Class cls: transformedArray)
                {
                    System.err.println(cls.getName());
                }
                inst.retransformClasses(transformedArray);
            } catch (UnmodifiableClassException e) {
                e.printStackTrace();
            }
        }
        out.println("INSTALL RULE:" + ruleNames);
    }

    /**
     * remove specfic rule
     * @param ruleNames
     * @param out
     */
    public void removeScripts(List<String> ruleNames, PrintWriter out)
    {
        super.scriptRepository.removeMultiRules(ruleNames);
        reTransformClass();
        out.println("UNINSTALL RULE: " + ruleNames);
    }

    /**
     * remove all rule loaded
     * retransform to the original classes
     * @param out
     */
    public void removeAllScripts(PrintWriter out)
    {
        super.scriptRepository.removeAllRule();
        reTransformClass();
        out.println("UNINSTALL ALL RULES");
    }
    public void reTransformClass()
    {
        try {
            List<Class<?>> classList = getClassNeedRetrasnform();
            Class<?>[] transformedArray = new Class<?>[classList.size()];
            classList.toArray(transformedArray);
            inst.retransformClasses(transformedArray);
        } catch (UnmodifiableClassException e) {
            e.printStackTrace();
        }
    }
    public void installBootScripts() throws Exception
    {
        // check for scripts which apply to classes already loaded
        // during bootstrap and retransform those classes so that rule
        // triggers are injected

        List<Class<?>> transformed = getClassNeedRetrasnform();
        // retransform all classes for which we found untransformed rules
        if (!transformed.isEmpty()) {
            Class<?>[] transformedArray = new Class<?>[transformed.size()];
            transformed.toArray(transformedArray);
            inst.retransformClasses(transformedArray);
        }
    }

    /**
     * get all loaded class which need to retransform
     * except class contain in skip class list
     * @return
     */
    public  List<Class<?>> getClassNeedRetrasnform()
    {
        List<Class<?>> transformed = new LinkedList<Class<?>>();

        Class<?>[] loaded = inst.getAllLoadedClasses();
        for (Class clazz : loaded) {
            if (isSkipClass(clazz)) {
                continue;
            }
            transformed.add(clazz);
        }
        return transformed;
    }

    /**
     * judge whether the provided class is skip
     * @param clazz
     * @return
     */
    protected boolean isSkipClass(Class<?> clazz)
    {
        if (!inst.isModifiableClass(clazz)) {
            return true;
        }
        // we can safely skip array classes, interfaces and primitive classes
        if (clazz.isArray()) {
            return true;
        }
        if (clazz.isInterface()) {
            return true;
        }
        if (clazz.isPrimitive()) {
            return true;
        }
        String name = clazz.getName().replace('.', '/');
        if (isUserPackageClass(name) && isTransformable(name)) {
            return false;
        }
        return true;
    }
}
