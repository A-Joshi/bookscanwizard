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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import net.sourceforge.bookscanwizard.AboutDialog;
import net.sourceforge.bookscanwizard.UserException;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;

/**
 * A class to help with book uploads to archive.org.
 */
public class ArchiveTransfer {
    // don't use x-archive it doesn't seem to work with authentication
    private static final String PREFIX = "x-amz-";
    private static final SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");

    private HashMap<String,String> metaData;
    private String awsAccessKey;
    private String awsSecretKey;
    private ProgressListener progressListener;

    public ArchiveTransfer(String accessKey, String secretKey) {
        this.awsAccessKey = accessKey;
        this.awsSecretKey = secretKey;
    }

    private static final String[] requiredMetaData = new String[] {
        "title",
        "description",
        "keywords"
    };

    public static void main(String[] args) throws Exception {
        ArchiveTransfer test = new ArchiveTransfer("BzzVcWXjXPJbgpxo", "DFzKyXlkM7THK7Rg");
        HashMap<String,String> p = new HashMap<String,String>();
        p.put("title:", "Big Book of Fairy Tales");
        p.put("creator", "Gustave Doré");
        p.put("noindex", "true");
        p.put("date", "1892");
        p.put("identifier", "BigBookOfFairyTales14");
        p.put("subject", "Fairy Tales");
        p.put("description", "Children's book of fairy tales");
        p.put("keywords", "Fairy Tales");
        test.setMetaData(p);

        System.out.println("is: "+test.isItem());

        test.saveToArchive(new File("c:/books/done/fairy/bswArchive.zip"));
    }

    public void setMetaData(Map<String,String> metaData) {
        this.metaData = new HashMap<String,String>();
        this.metaData.put("mediatype", "texts");
        this.metaData.put("collection", "opensource");
        this.metaData.put("bookscanwizard", AboutDialog.VERSION);
        this.metaData.putAll(metaData);
    }

    public String getArchiveId() {
        String id = metaData.get("identifier");
        if (id == null) {
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

    public void saveToArchive(File zipFile) throws IOException {
        if (!zipFile.isFile()) {
            throw new FileNotFoundException(zipFile.toString());
        }

        DefaultHttpClient httpclient = new DefaultHttpClient();
        String bucketName = getArchiveId();
        HttpPut put = new HttpPut("http://s3.us.archive.org/"+bucketName+"/"+bucketName+"_images.zip");
        FileEntity fileEntity = new FileEntity(zipFile, "application/zip");
        ProgressEntity entity = new ProgressEntity(fileEntity);
        entity.setProgressListener(progressListener);
        put.setEntity(entity);

        put.setHeader(fileEntity.getContentType());
        put.setHeader(PREFIX+"auto-make-bucket", "1");
        for (Map.Entry<String,String> entry : metaData.entrySet()) {
            if (!entry.getKey().equals("identifier")) {
                put.setHeader(PREFIX+"meta-"+entry.getKey(), entry.getValue());
            }
        }
        put.setHeader(getAuthHeader(put));
        HttpResponse response = httpclient.execute(put);
        StatusLine status = response.getStatusLine();
        if (status.getStatusCode() != 200) {
            throw new IOException(response.getStatusLine().toString());
        }
    }

    public static void checkMetaData(Map<String, String> metaData) {
        for(String key : requiredMetaData) {
           if (!metaData.containsKey(key)) {
               throw new UserException(key+" missing. It is a required metadata item");
           }
        }

    }

    /**
     * This creates the authentication header according to
     * http://docs.amazonwebservices.com/AmazonS3/2006-03-01/dev/RESTAuthentication.html
     */
    private Header getAuthHeader(HttpRequestBase request) {
        if (request.getFirstHeader("Date") == null) {
            request.setHeader("Date", format.format(new Date()));
        }

        StringBuilder str = new StringBuilder();
        str.append(request.getMethod()).append("\n")
            .append("\n")
            .append(request.getFirstHeader("Content-Type").getValue()).append("\n")
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
            str.append(entry.getKey()+":"+entry.getValue()+"\n");
        }
        str.append(request.getURI().getPath());
        String toSign = str.toString();
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            SecretKeySpec secret = new SecretKeySpec(awsSecretKey.getBytes(),"HmacSHA1");
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
}
