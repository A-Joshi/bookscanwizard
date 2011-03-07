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

import com.sun.media.imageio.plugins.jpeg2000.J2KImageWriteParam;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;
import net.sourceforge.bookscanwizard.BSW;
import net.sourceforge.bookscanwizard.FileHolder;
import net.sourceforge.bookscanwizard.Operation;
import net.sourceforge.bookscanwizard.PageSet;
import net.sourceforge.bookscanwizard.UserException;
import net.sourceforge.bookscanwizard.s3.ArchiveTransfer;
import net.sourceforge.bookscanwizard.util.ImageUtilities;
import net.sourceforge.bookscanwizard.util.Utils;
import org.w3c.dom.NodeList;

/**
 *
 * @author Steve
 */
public class CreateArchiveZip extends Operation  {
    private static ZipOutputStream zipOut;
    private static ImageWriter writer;
    static {
        ImageUtilities.allowNativeCodec("jpeg2000", ImageWriterSpi.class, false);
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg2000");
        writer = writers.next();
    }

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
    protected RenderedImage performOperation(FileHolder holder, RenderedImage img) throws Exception {
        if (!holder.isDeleted() && !BSW.instance().isInPreview()) {
            synchronized(SaveToArchive.class) {
                if (zipOut == null) {
                    if (getTextArgs().length == 0) {
                        throw new UserException("CreateArchveZip missing filename");
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
                SaveImage.writeJpeg2000Image(img, zipOut, PageSet.getDestinationDPI(), 1f/10f);
                zipOut.closeEntry();
            }
        }
        return img;
    }

    @Override
    public void postOperation() throws Exception {
        zipOut.close();
        zipOut = null;
    }
    
}
