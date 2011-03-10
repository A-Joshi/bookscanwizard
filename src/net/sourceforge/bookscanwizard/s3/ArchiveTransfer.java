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

package net.sourceforge.bookscanwizard.s3;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import net.sourceforge.bookscanwizard.AboutDialog;
import net.sourceforge.bookscanwizard.UserException;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A class to help with book uploads to archive.org.
 */
public class ArchiveTransfer {
    // don't use x-archive it doesn't seem to work with authentication
    private static final String PREFIX = "x-amz-";
    private static final SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
    private static final boolean LOW_SECURITY = false;
    private static final String HEADER_CHARSET = "UTF-8";

    private HashMap<String,String> metaData = new HashMap<String,String>();
    private String awsAccessKey;
    private String awsSecretKey;
    private ProgressListener progressListener;

    public ArchiveTransfer(String accessKey, String secretKey) {
        this.awsAccessKey = accessKey;
        this.awsSecretKey = secretKey;

        // add default metadat
        this.metaData.put("mediatype", "texts");
        this.metaData.put("collection", "opensource");
        this.metaData.put("postprocessor", "BookScanWizard: "+AboutDialog.VERSION);
    }

    private static final String[] requiredMetaData = new String[] {
        "title",
        "description",
        "keywords"
    };

    public static void main(String[] args) throws Exception {
        ArchiveTransfer test = new ArchiveTransfer("", "");
/*        HashMap<String,String> p = new HashMap<String,String>();
        p.put("title", "Big Book of Fairy Tales");
        p.put("creator", "Gustave Doré");
        p.put("noindex", "true");
        p.put("date", "1892");
        p.put("identifier", "BigBookOfFairyTales14");
        p.put("subject", "Fairy Tales");
        p.put("description", "Children's book of fairy tales");
        p.put("keywords", "Fairy Tales");
        test.setMetaData(p);*/

//        System.out.println("is: "+test.isItem());

        test.saveToArchive(new File("c:/books/done/fairy/bswArchive.zip"));
//        test.saveToArchive(null);
    }

    public void setMetaData(Map<String,String> metaData) {
        this.metaData = new HashMap<String,String>();
        this.metaData.putAll(metaData);
    }

    public String getArchiveId() {
        String id = metaData.get("identifier");
        if (id == null || id.isEmpty()) {
            id = createIdentifier();
            metaData.put("identifier", id);
        }
        return id;
    }

    public boolean isItem() throws IOException {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        String bucketName = getArchiveId();
        HttpGet head = new HttpGet("http://s3.us.archive.org/"+bucketName);
        HttpResponse response = httpclient.execute(head);
        StatusLine status = response.getStatusLine();
        return (status.getStatusCode() == 200);
    }

    public void setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    public void saveToArchive(File zipFile) throws Exception {
        readMetadataFromZip(zipFile);

        DefaultHttpClient httpclient = new DefaultHttpClient();

        String bucketName = getArchiveId();
        HttpPut put;
        if (zipFile == null) {
            put = new HttpPut("http://s3.us.archive.org/"+bucketName);
            ByteArrayEntity byteEntity = new ByteArrayEntity(new byte[0]);
            put.setEntity(byteEntity);
        } else {
            if (!zipFile.isFile()) {
                throw new FileNotFoundException(zipFile.toString());
            }
            put = new HttpPut("http://s3.us.archive.org/"+bucketName+"/"+bucketName+"_images.zip");
            FileEntity fileEntity = new FileEntity(zipFile, "application/zip");
            ProgressEntity entity = new ProgressEntity(fileEntity);
            entity.setProgressListener(progressListener);
            put.setEntity(entity);
            put.setHeader(fileEntity.getContentType());
            put.setHeader(PREFIX+"auto-make-bucket", "1");
        }
        put.setHeader(PREFIX+"ignore-preexisting-bucket", "1");
        for (Map.Entry<String,String> entry : metaData.entrySet()) {
            if (!entry.getKey().equals("identifier")) {
                put.setHeader(PREFIX+"meta-"+entry.getKey(), entry.getValue());
            }
        }
        put.setHeader(getAuthHeader(put));
        BasicHttpParams params = (BasicHttpParams) put.getParams();
        params.setParameter(CoreProtocolPNames.HTTP_ELEMENT_CHARSET, HEADER_CHARSET);

        HttpResponse response = httpclient.execute(put);
        StatusLine status = response.getStatusLine();
        if (status.getStatusCode() != 200) {
            InputStream is = response.getEntity().getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            while(true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                System.err.println(line);
            }
            throw new IOException(response.getStatusLine().toString());
        }
    }

    public static void checkMetaData(Map<String, String> metaData) {
        for(String key : requiredMetaData) {
           String value = metaData.get(key);
           if (value == null || value.isEmpty() ) {
               throw new UserException(key+" missing. It is a required metadata item");
           }
        }
    }

    /**
     * This creates the authentication header according to
     * http://docs.amazonwebservices.com/AmazonS3/2006-03-01/dev/RESTAuthentication.html
     */
    private Header getAuthHeader(HttpRequestBase request) {
        if (LOW_SECURITY) {
            return new BasicHeader("Authorization", "LOW "+awsAccessKey+":"+awsSecretKey);
        }
        if (request.getFirstHeader("Date") == null) {
            request.setHeader("Date", format.format(new Date()));
        }
        Header contentType = request.getFirstHeader("Content-Type");
        StringBuilder str = new StringBuilder();
        str.append(request.getMethod()).append("\n")
            .append("\n")
            .append(contentType == null ? "" : contentType.getValue()).append("\n")
            .append(request.getFirstHeader("Date").getValue()).append("\n");

        TreeMap<String, String> headers = new TreeMap<String,String>();
        for (Header h : request.getAllHeaders()) {
            String key = h.getName().toLowerCase();
            if (h.getName().toLowerCase().startsWith(PREFIX)) {
                String oldValue = headers.get(key);
                if (oldValue == null) {
                    headers.put(key, h.getValue());
                } else {
                    headers.put(key, oldValue+","+h.getValue());
                }
            }
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            str.append(entry.getKey()).append(":").append(entry.getValue()).append("\n");
        }
        str.append(request.getURI().getPath());
        String toSign = str.toString();
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            SecretKeySpec secret = new SecretKeySpec(awsSecretKey.getBytes(HEADER_CHARSET),"HmacSHA1");
            mac.init(secret);
            byte[] digest = mac.doFinal(toSign.getBytes());
            String base64 = Base64.encodeBase64String(digest);
            base64 = base64.substring(0, base64.length()-2);
            Header h = new BasicHeader("Authorization", "AWS "+awsAccessKey+":"+base64);
            return h;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String createIdentifier() {
        String title = metaData.get("title");
        title=title.replace(" " , "");
        return title;
    }

    private void readMetadataFromZip(File zipFile) throws Exception {
        ZipFile zipF = new ZipFile(zipFile);
        ZipEntry entry = zipF.getEntry("meta.xml");
        if (entry == null) {
            return;
        }
        InputStream is = zipF.getInputStream(entry);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(is);
        Element root = doc.getDocumentElement();
        NodeList nodeList = root.getChildNodes();
        for (int i=0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node instanceof Element) {
                Element e = (Element) node;
                String key = node.getNodeName();
                String value = e.getTextContent();
                metaData.put(key, value);
            }
        }
    }

    /** Other headers not currently used
     *
     * Don't run the derive task:
     *   x-archive-queue-derive:0
     *
     * Delete derived files
     *   x-archive-cascade-delete:1
     *
     */
}
