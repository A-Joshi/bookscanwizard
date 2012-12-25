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
 + * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package net.sourceforge.bookscanwizard.op;

import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfWriter;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.Semaphore;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageOutputStream;
import net.sourceforge.bookscanwizard.BSW;
import net.sourceforge.bookscanwizard.FileHolder;
import net.sourceforge.bookscanwizard.Operation;
import net.sourceforge.bookscanwizard.PageSet;
import net.sourceforge.bookscanwizard.UserException;
import net.sourceforge.bookscanwizard.op.Metadata.KeyValue;
import net.sourceforge.bookscanwizard.util.Utils;
import org.w3c.dom.Element;

/**
 * Creates a pdf of all the images.
 */
public class CreatePDF extends Operation {
    private Document document;
    private String format;
    // The semaphores are used to ensure that the previous page is rendered
    // before going on to the next.
    private Semaphore[] semaphores;

    @Override
    protected List<Operation> setup(List<Operation> operationList) throws Exception {
        if (!BSW.instance().isInPreview() && document != null) {
            document.close();
            document = null;
        }
        if (getTextArgs().length > 1) {
            format = getTextArgs()[1];
        }
        return operationList;
    }

    @Override
    protected RenderedImage performOperation(FileHolder holder, RenderedImage img) throws Exception {
        if (!holder.isDeleted() && !BSW.instance().isInPreview()) {
            // do this here instead of the setup because StartPage can redefine
            // the pages after running setup.
            synchronized(CreatePDF.class) {
                if (semaphores == null) {
                    semaphores = new Semaphore[getPageSet().getFileHolders().size()];
                    for (int i=0; i < semaphores.length; i++) {
                        semaphores[i]=new Semaphore(0);
                    }
                }
            }
            BufferedImage bi = Utils.renderedToBuffered(img);
            int pos = getPageSet().getFileHolders().indexOf(holder);
            if (pos > 0) {
                // check to make sure the previous page has been released before continuing.
                semaphores[pos-1].acquire();
            }
            byte[] imageBytes = getImageAsBytes(bi, format, PageSet.getDestinationDPI(), getCompression());
            String[] args = getTextArgs();
            if (document == null) {
                if (args.length == 0) {
                    throw new UserException("CreatePDF missing filename");
                }
                document = new Document();
                document.setMargins(0, 0, 0, 0);
                File f = BSW.getFileFromCurrentDir(getTextArgs()[0]);
                PdfWriter pdfWriter = PdfWriter.getInstance(document, new FileOutputStream(f));
                pdfWriter.setFullCompression();
                addMetaData(document);
            }
            document.setPageSize(new Rectangle(
                    72 * bi.getWidth()/holder.getDPI(),
                    72 * bi.getHeight()/holder.getDPI()));
            if (document.isOpen()) {
                document.newPage();
            } else {
                document.open();
            }
            Image itextImage = Image.getInstance(imageBytes);
            itextImage.setDpi((int) holder.getDPI(), (int) holder.getDPI());
            itextImage.setAbsolutePosition(0, 0);
            itextImage.scaleToFit(document.getPageSize().getWidth(), document.getPageSize().getHeight());
            itextImage.setBorder(0);
            document.add(itextImage);
            // release the page so the next page can continue.
            semaphores[pos].release();
        }
        return img;
    }

    private float getCompression() {
        float compression = -1;
        String[] args = getTextArgs();
        if (args.length > 2) {
            String arg = args[2];
            int pos = arg.indexOf(":");
            if (pos > 0) {
                compression = Float.parseFloat(arg.substring(pos + 1)) / Float.parseFloat(arg.substring(0, pos));
            } else {
                compression = Float.parseFloat(args[1]);
            }
        }
        return compression;
    }

    @Override
    public void postOperation() throws Exception {
        document.close();
        document = null;
    }
    
    private byte[] getImageAsBytes(RenderedImage img, String format, int dpi, float quality) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        synchronized (CreatePDF.class) {
            if ("jpg".equalsIgnoreCase(format) || "jpeg".equalsIgnoreCase(format)) {
                saveJpeg(baos, img, dpi, quality);
            } else {
                if (quality < 0) {
                    quality = .8f;
                }
                SaveImage.writeJpeg2000Image(img, baos, PageSet.getDestinationDPI(), quality);
            }
            return baos.toByteArray();
        }
    }
    
    /**
     * Save a file as a jpeg
     * 
     * @param os The outputstream to write to.
     * @param img
     * @param dpi 
     * @param quality a value between 0 and 1
     */
    private static void saveJpeg(OutputStream os, RenderedImage img, int dpi, float quality) throws IOException {
        if (quality < 0) {
            quality = .8f;
        }
        img = Utils.renderedToBuffered(img);
        ImageWriter imageWriter = ImageIO.getImageWritersByFormatName("jpeg").next();
        final ImageOutputStream stream = ImageIO.createImageOutputStream(os);
        imageWriter.setOutput(stream);
        ImageWriteParam writeParam = imageWriter.getDefaultWriteParam();
        ImageTypeSpecifier spec = ImageTypeSpecifier.createFromRenderedImage(img);
        writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        writeParam.setCompressionQuality(quality);   // an integer between 0 and 1
        IIOMetadata metadata = imageWriter.getDefaultImageMetadata(spec, writeParam);
        if (dpi > 0) {
            // jpeg files don't seem to want to save the dpi the generic way,
            // so we do this instead.
            Element tree = (Element)metadata.getAsTree("javax_imageio_jpeg_image_1.0");
            Element jfif = (Element)tree.getElementsByTagName("app0JFIF").item(0);
            jfif.setAttribute("Xdensity", Integer.toString(dpi));
            jfif.setAttribute("Ydensity", Integer.toString(dpi));
            jfif.setAttribute("resUnits", "1"); // density is dots per inch
            metadata.setFromTree("javax_imageio_jpeg_image_1.0", tree);
        }
        try {
            imageWriter.write(metadata, new IIOImage(img, null, metadata), writeParam);
        } finally {
            stream.close();
        }
    }

    private void addMetaData(Document document) {
        for (KeyValue meta : Metadata.getMetaData()) {
            System.out.println("ccc: "+meta.getKey()+" "+meta.getValue());
                    
            if (meta.getValue().isEmpty()) {
                continue;
            }
            // convert from the archive.org standard to the pdf standard.
            String key = meta.getKey();
            if (key.equals("subject")) {
                document.addSubject(meta.getValue());
            } else if (key.equals("title")) {
                document.addTitle(meta.getValue());
            } else if (key.equals("creator")) {
                document.addAuthor(meta.getValue());
            } else if (key.equals("keywords")){
                document.addKeywords(meta.getValue());
            } else if (key.equals("identifier")) {
                // ignore
            } else {
                document.addHeader(key, meta.getValue());
            }
        }
    }
}
