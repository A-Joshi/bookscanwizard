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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
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
 * Defines additonal metadata to save.
 */
public class Metadata extends Operation{
    private static ArrayList<KeyValue> metaData = new ArrayList<KeyValue>();
    private static boolean init;
    private static final Pattern idPattern = Pattern.compile("[A-Za-z0-9\\-\\.\\_]");


    @Override
    protected List<Operation> setup(List<Operation> operationList) throws Exception {
        init();
        int pos = arguments.indexOf(":");
        if (pos < 0) {
            throw new UserException("Metadata missing : separator");
        }
        metaData.add(new KeyValue(arguments.substring(0, pos).trim(), arguments.substring(pos+1).trim()));
        return operationList;
    }

    public static ArrayList<KeyValue> getMetaData() {
        ArrayList<KeyValue> newMeta = new ArrayList<KeyValue>();
        boolean found = false;
        String title = null;
        for (KeyValue k : metaData) {
            if (k.getKey().equals("title")) {
                title = k.getValue();
            } else if (k.getKey().equals("identifier") && !k.getValue().trim().isEmpty()) {
                found = true;
                break;
            }
            if (!k.getValue().trim().isEmpty()) {
                newMeta.add(k);
            }
        }
        if (!found && title != null) {
            String str = calcIdFromTitle(title);
            newMeta.add(new KeyValue("identifier", str.toString()));
        }
        return newMeta;
    }

    public static String calcIdFromTitle(String title) {
        StringBuilder str = new StringBuilder();
        for (char c : title.toCharArray()) {
            if (idPattern.matcher(new String(new char[] {c})).matches()) {
                str.append(c);
            }
        }
        return str.toString();
    }
    
    public static void getMetaDataAsXML(OutputStream os) throws Exception {
       DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
        DocumentBuilder bd = fact.newDocumentBuilder();
        Document doc = bd.newDocument();
        Element root = (Element) doc.createElement("metadata");
        doc.appendChild(root);
        for (KeyValue entry : getMetaData()) {
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

    public static class KeyValue implements Comparable<KeyValue> {

        private String key;
        private String value;

        public KeyValue (String key, String value) {
            this.key = key;
            this.value = value;
        }

        public KeyValue (String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }

        public int compareTo(KeyValue that) {
            return this.key.compareTo(that.key);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final KeyValue other = (KeyValue) obj;
            if ((this.key == null) ? (other.key != null) : !this.key.equals(other.key)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }
    }
}
