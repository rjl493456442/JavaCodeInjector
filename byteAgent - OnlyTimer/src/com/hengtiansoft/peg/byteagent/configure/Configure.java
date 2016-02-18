package com.hengtiansoft.peg.byteagent.configure;

import com.hengtiansoft.peg.byteagent.FILED_DEFINATION;
import org.objectweb.asm.Label;

import java.util.List;

/**
 * Created by jialeirong on 2/1/2016.
 */

public class Configure implements FILED_DEFINATION
{
    /**
     * get configure xml List element
     * @param name
     * @return
     */
    public List<String> getList(String name)
    {
        if (name.equals(METHOD_LIST))
        {
            return serviceList;
        } else if (name.equals(RULE_LIST))
        {
            return rules;
        } else if (name.equals(EXCEPT_METHOD_LIST))
        {
            return exceptServiceList;
        }
        else
        {
            System.err.println("Invalid xml field");
            return null;
        }
    }

    /**
     * get configure xml String element
     * @param name
     * @return
     */
    public String getField(String name)
    {
        if (name.equals(PACKAGE_NAME))
        {
            return packageName;
        } else if (name.equals(LOG_NAME))
        {
            return logFileName;
        } else
        {
            System.err.println("Invalid xml field");
            return null;
        }
    }

    /**
     * set configure xml List element
     * @param name specify which element to set
     * @param value provided value
     */
    public void setList(String name, List<String> value)
    {
        if (name.equals(METHOD_LIST))
        {
            serviceList = value;
        } else if (name.equals(RULE_LIST))
        {
            rules = value;
        } else if (name.equals(EXCEPT_METHOD_LIST))
        {
            exceptServiceList = value;
        }
        else
        {
            System.err.println("Invalid xml field");
            return;
        }
    }

    /**
     * set configure xml String element
     * @param name specify which element to set
     * @param value provided value
     */
    public void setField(String name, String value)
    {
        if (name.equals(PACKAGE_NAME))
        {
            packageName = value;
        } else if (name.equals(LOG_NAME))
        {
            logFileName = value;
        } else
        {
            System.err.println("Invalid xml field");
            return;
        }
    }

    private String packageName;
    private List<String> serviceList;
    private String logFileName;
    private List<String> rules;
    private List<String> exceptServiceList;
}