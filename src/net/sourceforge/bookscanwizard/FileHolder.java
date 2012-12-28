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
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import javax.media.jai.JAI;
import net.sourceforge.bookscanwizard.qr.QRData;
import net.sourceforge.bookscanwizard.util.Utils;

/**
 * A holder used to contain a source file and a page name.
 * It is used to display the list of pages for the preview dropdown list.
 */
public class FileHolder implements Comparable<FileHolder> {
   private File file;
   /* the page number of a multi-page source */
   private int page;

   private String name;
   private String oldName;
   private int position;
   private boolean deleted;
   private float dpi;

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
        return deleted;
    }

    public synchronized void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public void setDPI(float dpi) {
        this.dpi = dpi;
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
            return source.getImage(page);
        } else {
            JPEGMetaData metadata = new JPEGMetaData(getFile(), true);
            RenderedImage img = metadata.getThumbnail();
            if (img == null) {
                System.out.println("thumbnail failed");
                img = getImage();
            }
            return img;
        }
    }
    
    public RenderedImage getImage() throws IOException {
       if (source != null) {
           return source.getImage(page);
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
           return img;
       }
    }
}
