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

import java.awt.Dimension;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import net.sourceforge.bookscanwizard.BSW;
import net.sourceforge.bookscanwizard.FileHolder;
import net.sourceforge.bookscanwizard.Operation;
import net.sourceforge.bookscanwizard.PageSet;
import net.sourceforge.bookscanwizard.SaveOperation;
import net.sourceforge.bookscanwizard.UserException;
import net.sourceforge.bookscanwizard.s3.ArchiveTransfer;
import net.sourceforge.bookscanwizard.util.Utils;

/**
 * Creates a zip file of jp2 files that are formatted for the Internet Archive.
 */
public class CreateArchiveZip extends Operation implements SaveOperation {
    private static ZipOutputStream zipOut;
    private static Dimension lastImageSize;
    private static int layerCount;
    private static List<FileHolder> lastFiles;

    @Override
    protected List<Operation> setup(List<Operation> operationList) throws Exception {
        if (zipOut != null) {
            zipOut.close();
            zipOut = null;
        }
        ArchiveTransfer.checkMetaData(Metadata.getMetaData());
        return operationList;
    }
    
    @Override
    protected RenderedImage previewOperation(FileHolder holder, RenderedImage img) throws Exception {
        return img;
    }

    @Override
    protected RenderedImage performOperation(FileHolder holder, RenderedImage img) throws Exception {
        lastImageSize = new Dimension(img.getWidth(), img.getHeight());
        layerCount = img.getColorModel().getNumComponents();
        lastFiles = getPageSet().getFileHolders();
        if (!holder.isDeleted() && !BSW.instance().isInPreview()) {
            synchronized(SaveToArchive.class) {
                String[] args = getTextArgs();
                if (zipOut == null) {
                    if (args.length == 0) {
                        throw new UserException("CreateArchiveZip missing filename");
                    }
                    File f = BSW.getFileFromCurrentDir(getTextArgs()[0]);
                    zipOut = new ZipOutputStream(new FileOutputStream(f));
                    zipOut.putNextEntry(new ZipEntry("meta.xml"));
                    Metadata.getMetaDataAsXML(zipOut);
                    zipOut.closeEntry();
                }
                ZipEntry zipEntry = new ZipEntry(holder.getName()+".jp2");
                zipOut.putNextEntry(zipEntry);
                img = Utils.renderedToBuffered(img);
                SaveImage.writeJpeg2000Image(img, zipOut, PageSet.getDestinationDPI(), getCompression());
                zipOut.closeEntry();
            }
        }
        return img;
    }

    private float getCompression() {
        float compression = 1f/10f;
        String[] args = getTextArgs();
        if (args.length > 1) {
            String arg = args[1];
            int pos = arg.indexOf(":");
            if (pos > 0) {
                compression = Float.parseFloat(arg.substring(pos+1)) / Float.parseFloat(arg.substring(0, pos));
            } else {
                compression = Float.parseFloat(args[1]);
            }
        }
        return compression;
    }

    public String estimateZipSize() {
        if (!BSW.instance().getMainFrame().isShowCrops()) {
            return "Show Crops must be checked to estimate size";
        }
        float compression = getCompression();
        if (compression >= 1) {
            compression = .3f;
        } else if (compression > .2) {
            compression = .2f;
        }
        BSW.instance();
        int ct = 0;
        for (FileHolder h : lastFiles) {
            if (!h.isDeleted()) {
                ct++;
            }
        }
        long size = (long) (lastImageSize.getHeight() * lastImageSize.getWidth() * layerCount * ct * compression)/1024/1024;
        return "Estimated size of zip file is "+size +"M, containing "+ct+" files.";
    }

    @Override
    public void postOperation() throws Exception {
        zipOut.close();
        zipOut = null;
    }
}
