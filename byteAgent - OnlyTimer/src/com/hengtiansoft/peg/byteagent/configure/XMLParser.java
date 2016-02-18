package com.hengtiansoft.peg.byteagent.configure;
/**
 * Created by jialeirong on 2/1/2016.
 */
import com.hengtiansoft.peg.byteagent.FILED_DEFINATION;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class XMLParser implements FILED_DEFINATION {
    private static Log logger = LogFactory.getLog("agentLogger");
    public static void main(String[] args)
    {
        try {
            createConfigXMLFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void createConfigXMLFile() throws IOException
    {
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement("agentConfigure");
        /**
         * specify package in which class may be tranform
         * if not provided, default value is all class except system class will be transformed
         */
        Element packageName = root.addElement("packageName", "")
                .addText( "" );
        /**
         * select function need transformation
         */
        Element serviceNode = root.addElement("serviceEntryFunctions", "");
        for(int i = 0; i < 10; ++i)
        {
            serviceNode.addElement("functionName","")
                    .addText("function");
        }
        /**
         * output log file configure
         */
        Element logFileNode = root.addElement("logFileName", "")
                .addText("test.log");
        /**
         * specify rules of transformation
         */
        Element ruleNode = root.addElement("rules", "");
        for(int i = 0; i < 10; ++i)
        {
            ruleNode.addElement("rule","")
                    .addText("rule1");
        }

        OutputFormat format = OutputFormat.createPrettyPrint();
        XMLWriter writer = new XMLWriter( new FileWriter("configure.xml"), format );
        writer.write( document );
        writer.flush();
    }
    public static Configure parserConfigXMLFile(String configureFilePath) throws DocumentException
    {
        SAXReader saxReader = new SAXReader();
        Document document = saxReader.read(new File(configureFilePath));

        Element root = document.getRootElement();

        Element packageNode = root.element(PACKAGE_NAME);
        Element serviceNode = root.element(METHOD_LIST);
        Element logFileNameNode = root.element(LOG_NAME);
        Element ruleNode = root.element(RULE_LIST);
        Element exceptServiceNode = root.element(EXCEPT_METHOD_LIST);
        List<String> seriviceNameList = new ArrayList<String>();
        List<String> ruleList = new ArrayList<String>();
        List<String> exceptServiceNameList = new ArrayList<String>();
        List<Element> configElements = serviceNode.elements();
        for (Iterator<Element> iter  = serviceNode.elementIterator(); iter.hasNext();)
        {
            Element element = iter.next();
            seriviceNameList.add(element.getText());
        }
        configElements = ruleNode.elements();
        for (Iterator<Element> iter  = ruleNode.elementIterator(); iter.hasNext();)
        {
            Element element = iter.next();
            ruleList.add(element.getText());
        }
        configElements = exceptServiceNode.elements();
        for(Iterator<Element> iter = exceptServiceNode.elementIterator(); iter.hasNext();)
        {
            exceptServiceNameList.add(iter.next().getText());
        }

        Configure configure =  new Configure();
        configure.setField(PACKAGE_NAME, packageNode.getText());
        configure.setField(LOG_NAME, logFileNameNode.getText());
        configure.setList(RULE_LIST, ruleList);
        configure.setList(METHOD_LIST, seriviceNameList);
        configure.setList(EXCEPT_METHOD_LIST, exceptServiceNameList);
        return  configure;
    }
}
