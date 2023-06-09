/*
 *
 * Copyright (c) 2013 by Steve Devore
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import net.sourceforge.bookscanwizard.ProcessDeleted;
import net.sourceforge.bookscanwizard.SaveOperation;
import net.sourceforge.bookscanwizard.UserException;
import net.sourceforge.bookscanwizard.op.Metadata.KeyValue;
import net.sourceforge.bookscanwizard.util.Hocr;
import net.sourceforge.bookscanwizard.util.Utils;
import org.w3c.dom.Element;

/**
 * Creates a pdf of all the images.
 */
public class CreatePDF extends Operation implements SaveOperation, ProcessDeleted {
    private static final Logger logger = Logger.getLogger(CreatePDF.class.getName());
    private Document document;
    private PdfWriter pdfWriter;
    private String format;
    // The semaphores are used to ensure that the previous page is rendered
    // before going on to the next.
    private Semaphore[] semaphores;
    private int pageLayout = 0;

    @Override
    protected List<Operation> setup(List<Operation> operationList) throws Exception {
        if (!BSW.instance().isInPreview() && document != null) {
            document.close();
            document = null;
        }
        String pageLayoutStr = getOption("Options");
        if (pageLayoutStr != null) {
            String[] optionList = pageLayoutStr.toLowerCase().split(",");
            HashSet<String> options = new HashSet<>(Arrays.asList(optionList));
            if (options.remove("single")) {
                if (options.remove("scrolling")) {
                    pageLayout = PdfWriter.PageLayoutOneColumn;
                } else {
                    pageLayout = PdfWriter.PageLayoutSinglePage;
                }
            } else if (options.remove("2-up")) {
                if (options.remove("scrolling")) {
                    if (options.remove("title")) {
                        pageLayout = PdfWriter.PageLayoutTwoColumnRight;
                    } else {
                        pageLayout = PdfWriter.PageLayoutTwoColumnLeft;
                    }
                } else {
                    if (options.remove("title")) {
                        pageLayout = PdfWriter.PageLayoutTwoPageRight;
                    } else {
                        pageLayout = PdfWriter.PageLayoutTwoPageLeft;
                    }
                }
            } else if (options.remove("default")) {
                pageLayout = 0;
            }
            for (String option : options) {
                try {
                    pageLayout |= (Integer) PdfWriter.class.getField(option).get(null);
                } catch (NoSuchFieldException|SecurityException|IllegalArgumentException|IllegalAccessException ex) {
                    throw new UserException("Invalid PDF Option: "+option);
                }
            }
        } else {
            pageLayout = -1;
        }
        if (getTextArgs().length > 1) {
            format = getTextArgs()[1];
        }
        List<FileHolder> holders = getPageSet().getFileHolders();
        semaphores = new Semaphore[holders.size()+1];
        Semaphore s = new Semaphore(1);
        for (int i=0; i < holders.size(); i++) {
            semaphores[i]=s;
            s = new Semaphore(0);
        }
        semaphores[semaphores.length-1] = s;
        return operationList;
    }
    
    @Override
    protected RenderedImage previewOperation(FileHolder holder, RenderedImage img) throws Exception {
        return img;
    }

    @Override
    protected RenderedImage performOperation(FileHolder holder, RenderedImage img) throws Exception {
        int pos = getPageSet().getFileHolders().indexOf(holder);
        if (holder.isDeleted()) {
            semaphores[pos].acquire();
        } else {
            int dpi = PageSet.getDestinationDPI();
            BufferedImage bi = Utils.renderedToBuffered(img);
            // check to make sure the previous page has been released before continuing.
            semaphores[pos].acquire();
            byte[] imageBytes = getImageAsBytes(bi, format, PageSet.getDestinationDPI(), getCompression());
            String[] args = getTextArgs();
            if (document == null) {
                if (args.length == 0) {
                    throw new UserException("CreatePDF missing filename");
                }
                document = new Document();
                document.setMargins(0, 0, 0, 0);

                File f = pageSet.getDestinationDir().toPath().resolve(getTextArgs()[0]).toFile();
                f.getParentFile().mkdirs();
                logger.log(Level.INFO, "Creating {0}", f.getAbsolutePath());
                pdfWriter = PdfWriter.getInstance(document, new FileOutputStream(f));
                pdfWriter.setFullCompression();
                if (pageLayout == -1) {
                    if (holder.getPosition() == FileHolder.LEFT) {
                        pageLayout = PdfWriter.PageLayoutTwoPageLeft;
                    } else {
                        pageLayout = PdfWriter.PageLayoutTwoPageRight;
                    }
                }
                pdfWriter.setViewerPreferences(pageLayout);
                pdfWriter.setPageLabels(PageLabels.getPageLabels());
                addMetaData(document, holder);
            }
            int pageDPI = (int) holder.getDPI();
            if (pageDPI == 0) {
                pageDPI = dpi;
            }
            if (pageDPI == 0) {
                double xDPI = bi.getWidth() / 8.0;
                double yDPI = bi.getHeight() / 10.5;
                pageDPI = (int) Math.min(xDPI, yDPI);
            }
            document.setPageSize(new Rectangle(
                    72 * bi.getWidth()/pageDPI,
                    72 * bi.getHeight()/pageDPI));
            if (document.isOpen()) {
                document.newPage();
            } else {
                document.open();
            }
            Image itextImage = Image.getInstance(imageBytes);
            itextImage.setDpi(pageDPI, pageDPI);
            itextImage.setAbsolutePosition(0, 0);
            itextImage.scaleToFit(document.getPageSize().getWidth(), document.getPageSize().getHeight());
            itextImage.setBorder(0);
            document.add(itextImage);

            if (OCR.isUseOCR()) {
                File hocrFile = new File (pageSet.getDestinationDir(), holder.getName()+".html");
                if (hocrFile.isFile()) {
                    Hocr.writeToPDF(hocrFile, pdfWriter, pageDPI);
                }
            }
            // release the page so the next page can continue.
        }
        if (pos+1 < semaphores.length) {
            semaphores[pos+1].release();
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
                compression = Float.parseFloat(arg);
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
        synchronized (CreatePDF.class) {
            if (img.getSampleModel().getSampleSize()[0] == 1) {
                if (format != null && !format.equalsIgnoreCase("png")) {
                    throw new UserException("Only PNG is supported for saving binary (bw) images to PDF");
                }
                format = "png";
            }
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                if ("jpg".equalsIgnoreCase(format) || "jpeg".equalsIgnoreCase(format)) {
                    saveJpeg(baos, img, dpi, quality);
                } else if ("png".equalsIgnoreCase(format)) {
                    SaveImages.writePng(img, baos, dpi);
                } else {
                    if (quality < 0) {
                        quality = .8f;
                    }
                    SaveImages.writeJpeg2000Image(img, baos, PageSet.getDestinationDPI(), quality);
                }
                return baos.toByteArray();
            }
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

    private void addMetaData(Document document, FileHolder holder) {
        for (Map.Entry<String,String> entry : holder.getMetadata().entrySet()) {
            addMeta(document, entry.getKey(), entry.getValue());
        }
        for (KeyValue meta : Metadata.getMetaData()) {
            if (meta.getValue().isEmpty()) {
                continue;
            }
            String key = meta.getKey();
            addMeta(document, key, meta.getValue());
        }
    }

    /**
     * Adds the metadata in the proper format for PDF's. 
     */
    private void addMeta(Document document, String key, String value) {
        // convert from the archive.org standard uses lowercase keys, PDF uses
        // uppercase
        switch (key) {
            case "Subject":
            case "subject":
                document.addSubject(value);
                break;
            case "Title":
            case "title":
                document.addTitle(value);
                break;
            case "Author":
            case "creator":
                document.addAuthor(value);
                break;
            case "Keywords":
            case "keywords":
                document.addKeywords(value);
                break;
            case "identifier":
                break;
            default:
                document.addHeader(key, value);
                break;
        }
    }
}
