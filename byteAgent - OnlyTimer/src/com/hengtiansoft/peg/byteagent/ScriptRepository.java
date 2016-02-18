package com.hengtiansoft.peg.byteagent;

import java.util.ArrayList;
import java.util.List;
import com.hengtiansoft.peg.byteagent.configure.XMLParser;
import com.hengtiansoft.peg.byteagent.configure.Configure;
import com.hengtiansoft.peg.byteagent.rule.RULES_DEFINATION;
import org.dom4j.DocumentException;

/**
 * Created by jialeirong on 2/1/2016.
 */
public class ScriptRepository implements FILED_DEFINATION{
    public ScriptRepository()
    {
        ruleNames = new ArrayList<String>();
    }

    /**
     *
     * @param configureFilePath
     * @return whether initialize rule is empty
     */
    public boolean processRules(String configureFilePath)
    {
        try {
            configure = XMLParser.parserConfigXMLFile(configureFilePath);
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        List<String> rules = configure.getList(RULE_LIST);
        if (rules.isEmpty())return true;
        else return  false;
    }
    public void addRule(String ruleName)
    {
        String previous;
        if (ruleNames.contains(ruleName))
        {
            return;
        }
        ruleNames.add(ruleName);
    }
    public void addAllRules(List<String> rules)
    {
        for(String rule: rules)
        {
            this.addRule(rule);
        }
    }
    public void removeRule(String ruleName)
    {
        if (ruleNames.contains(ruleName))
        {
            ruleNames.remove(ruleName);
        }
    }
    public void removeMultiRules(List<String> rules)
    {
        ruleNames.removeAll(rules);
    }
    public void removeAllRule()
    {
        ruleNames.clear();
    }
    public List<String> getAllRules()
    {
        return ruleNames;
    }

    public boolean isEmpty()
    {
        return ruleNames.isEmpty();
    }

    public  boolean isExist(String ruleName)
    {
        return ruleNames.contains(ruleName);
    }

    public void listAllRules()
    {
        System.err.println("ScriptRepository rule list");
        for(String rule: ruleNames)
        {
            System.err.println(rule);
        }
    }
    private List<String> ruleNames;
    private Configure configure;
}
