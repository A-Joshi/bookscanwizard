/*
 *
 * Copyright (c) 2011 by Steve Devore
 *                       http://bookscanwizard.sourceforge.net
 *
 * This file is part of the Book Scan Wizard.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package net.sourceforge.bookscanwizard.op;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import net.sourceforge.bookscanwizard.BSW;
import net.sourceforge.bookscanwizard.NewConfigListener;
import net.sourceforge.bookscanwizard.Operation;
import net.sourceforge.bookscanwizard.UserException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author Steve
 */
public class Metadata extends Operation{
    private static HashMap<String,String> metaData = new HashMap<String,String>();
    private static boolean init;

    @Override
    protected List<Operation> setup(List<Operation> operationList) throws Exception {
        init();
        int pos = arguments.indexOf(":");
        if (pos < 0) {
            throw new UserException("Metadata missing : separator");
        }
        metaData.put(arguments.substring(0, pos).trim(), arguments.substring(pos+1).trim());
        return operationList;
    }

    public static Map<String,String> getMetaData() {
        return metaData;
    }

    public static void getMetaDataAsXML(OutputStream os) throws Exception {
       DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
        DocumentBuilder bd = fact.newDocumentBuilder();
        Document doc = bd.newDocument();
        Element root = (Element) doc.createElement("metadata");
        doc.appendChild(root);
        for (Map.Entry<String,String> entry : getMetaData().entrySet()) {
           Element e = doc.createElement(entry.getKey());
           root.appendChild(e);
           e.appendChild(doc.createTextNode(entry.getValue()));
        }
        TransformerFactory tFactory = TransformerFactory.newInstance();
        tFactory.setAttribute("indent-number", 2);
        Transformer transformer = tFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        DOMSource source = new DOMSource(doc);
        StreamResult result =  new StreamResult(os);
        transformer.transform(source, result);
    }

    synchronized private static void init() {
        if (!init) {
            init = true;
            BSW.instance().addNewConfigListener(new NewConfigListener(){
                public void newConfig() {
                    metaData.clear();
                }
            });
        }
    }
}
