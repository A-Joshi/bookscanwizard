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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package net.sourceforge.bookscanwizard;

import com.bric.image.jpeg.JPEGMetaData;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.JAI;
import net.sourceforge.bookscanwizard.qr.QRData;
import net.sourceforge.bookscanwizard.util.Utils;
import org.w3c.dom.NodeList;

/**
 * A holder used to contain a source file and a page name.
 * It is used to display the list of pages for the preview dropdown list.
 */
public class FileHolder implements Comparable<FileHolder> {
   private static final Logger logger = Logger.getLogger(FileHolder.class.getName());
   private static Method IIOMETADATA = null;
    
   private final File file;
   /* the page number of a multi-page source */
   private int page;

   private String name;
   private final String oldName;
   private int position;
   private boolean deleted;
   private boolean forceOn;

   private float dpi;
   private boolean dpiChecked;

   private List<QRData> qrData;

   public static int ALL = 0;
   public static int LEFT = 1;
   public static int RIGHT = 2;
   private PDFReference source;

   public FileHolder(File file, List<QRData> qrData, int page) {
       this.file = file;
       this.page = page;
       this.name = getNameNoExt();
       this.oldName = name;
       this.qrData = qrData;
   }

   public FileHolder(File file, String name, List<QRData> qrData) {
       this.file = file;
       this.name = name;
       this.oldName = getNameNoExt();
       this.qrData = qrData;
   }

    @Override
    public int compareTo(FileHolder o) {
        return name.compareTo(o.name);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final FileHolder other = (FileHolder) obj;
        if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + (this.name != null ? this.name.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        if (name.equals(oldName)) {
            return name +" "+(getPosition()==LEFT ? "L" : "R");
        } else {
            return name +" "+(getPosition()==LEFT ? "L" : "R")+" ("+oldName+")";
        }
    }

    public File getFile() {
        return file;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public boolean isDeleted() {
        return deleted && !forceOn;
    }

    public synchronized void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public void setDPI(float dpi) {
        this.dpi = dpi;
        if (PageSet.getDestinationDPI() <= 0) {
            PageSet.setDestinationDPI((int) dpi);
        }
    }

    public float getDPI() {
        return dpi;
    }

    public List<QRData> getQRData() {
        return qrData;
    }

    public void setQrData(List<QRData> qrData) {
        this.qrData = qrData;
    }

    private String getNameNoExt() {
       String temp = file.getName();
       int pos = temp.lastIndexOf(".");
       if (pos >=0) {
           temp = temp.substring(0, pos);
       }
       return temp;
   }

    public boolean isProblemFile() {
        return name.startsWith("z_");
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public void setSource(PDFReference ref) {
        this.source = ref;
    }
    
    public PDFReference getSource() {
        return source;
    }
    
    public RenderedImage getThumbnail() throws IOException {
        if (source != null) {
            return source.getThumbnail(page);
        } else {
            RenderedImage img = null;
            try {
                JPEGMetaData metadata = new JPEGMetaData(getFile(), true);
                img = metadata.getThumbnail();
            } catch (Exception e) {
                // ignore
            }
            if (img == null) {
                logger.log(Level.FINEST, "thumbnail failed: {0}", getName());
                img = getImage();
            }
            return img;
        }
    }
    
    public RenderedImage getImage() {
        try {
            if (source != null) {
                RenderedImage img = source.getImage(page);
                setDPI(source.getDpi());
                return img;
            } else {
                RenderedImage img;
                try {
                    img = Utils.renderedToBuffered(JAI.create("fileload", file.getPath()));
                } catch (Exception e) {
                    System.out.println("could not read using JAI.. tring ImageIO..");
                    img = ImageIO.read(file);
                    img = Utils.getScaledInstance(img, img.getWidth(), img.getHeight(), 
                            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                }
                readDPI(file);
                return img;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private int readDPI(File f) throws IOException {
        InputStream in = new BufferedInputStream(new FileInputStream(f));
        if (getDPI() <=0 && !dpiChecked) {
            dpiChecked = true;
            try (ImageInputStream iis = ImageIO.createImageInputStream(in)) {
                Iterator it = ImageIO.getImageReaders(iis);
                if (it.hasNext()) {
                    ImageReader reader = (ImageReader) it.next();
                    reader.setInput(iis);
                    IIOMetadata meta = reader.getImageMetadata(0);
                    IIOMetadataNode dimNode = (IIOMetadataNode) IIOMETADATA.invoke(meta);
                    NodeList nodes = dimNode.getElementsByTagName("HorizontalPixelSize");
                    IIOMetadataNode dpcWidth = (IIOMetadataNode)nodes.item(0);
                    setDPI(Math.round(25.4 / Double.parseDouble(dpcWidth.getAttribute("value"))));
                }
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                System.out.println(ex);
            }
        }
        return 0;
    }
    
    public void setForceOn(boolean forceOn) {
        this.forceOn = forceOn;
    }

    static {
       try {
           IIOMETADATA = IIOMetadata.class.getDeclaredMethod("getStandardDimensionNode");
           IIOMETADATA.setAccessible(true);
       } catch (NoSuchMethodException | SecurityException ex) {
           throw new RuntimeException(ex);
       }
    }
}
