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
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import net.sourceforge.bookscanwizard.BSW;
import net.sourceforge.bookscanwizard.FileHolder;
import net.sourceforge.bookscanwizard.Operation;
import net.sourceforge.bookscanwizard.PageSet;
import net.sourceforge.bookscanwizard.UserException;
import net.sourceforge.bookscanwizard.s3.ArchiveTransfer;
import net.sourceforge.bookscanwizard.s3.ProgressListener;
import net.sourceforge.bookscanwizard.util.Utils;
import org.w3c.dom.NodeList;

/**
 *
 * @author Steve
 */
public class SaveToArchive extends Operation implements ProgressListener {
    private static ZipOutputStream zipOut;
    private static boolean abortRequested;
    private ImageWriter writer = (ImageWriter) ImageIO.getImageWritersByFormatName("jpeg 2000").next();
    private JProgressBar progressBar;

    @Override
    protected List<Operation> setup(List<Operation> operationList) throws Exception {
        abortRequested = false;
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
                    File f = BSW.getFileFromCurrentDir("bswArchive.zip");
                    zipOut = new ZipOutputStream(new FileOutputStream(f));
                }
                zipOut.putNextEntry(new ZipEntry(holder.getName()+".jp2"));
                img = Utils.renderedToBuffered(img);
                writeJpeg2000Image(img, zipOut);
                zipOut.closeEntry();
            }
        }
        return img;
    }

    @Override
    public void postOperation() throws Exception {
        zipOut.close();
        zipOut = null;
        writer.dispose();
        String[] args = getTextArgs();
        String access = args[0];
        String secret = args[1];
        final ArchiveTransfer transfer = new ArchiveTransfer(access, secret);
        transfer.setMetaData(Metadata.getMetaData());
        if (!BSW.isBatchMode()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    progressBar = BSW.instance().getMainFrame().getProgressBar();
                    progressBar.setMaximum(100);
                    BSW.instance().getMainFrame().setStatusLabel("Uploading..");
                    transfer.setProgressListener(SaveToArchive.this);
                }
            });
        }

        System.out.println("beginning transfer");
        transfer.saveToArchive(BSW.getFileFromCurrentDir("bswArchive.zip"));
        System.out.println("ending transfer");
    }

    private void writeJpeg2000Image(RenderedImage image, OutputStream out) throws IOException {
        ImageTypeSpecifier spec = ImageTypeSpecifier.createFromRenderedImage(image);
        J2KImageWriteParam paramJ2K = new J2KImageWriteParam();
        paramJ2K.setLossless(false);
        paramJ2K.setFilter(J2KImageWriteParam.FILTER_97);
        paramJ2K.setEncodingRate(.5);
        IIOMetadata metadata = writer.getDefaultImageMetadata(spec, paramJ2K);

        metadata = setDpi(metadata, PageSet.getDestinationDPI());
        IIOImage ioImage = new IIOImage(image, null, metadata);
        ImageOutputStream ios = ImageIO.createImageOutputStream(out);
        writer.setOutput(ios);
        writer.write(null, ioImage, paramJ2K);
        ios.close();
    }

    private IIOMetadata setDpi(IIOMetadata meta, int dpi) throws IOException {
        IIOMetadataNode nodes = (IIOMetadataNode) meta.getAsTree("javax_imageio_1.0");
        NodeList nl;
        IIOMetadataNode dim;

        nl = nodes.getElementsByTagName("Dimension");
        if ((nl != null) && (nl.getLength() > 0)) {
            dim = (IIOMetadataNode) nl.item(0);
        } else {
            dim = new IIOMetadataNode("Dimension");
            nodes.appendChild(dim);
        }

        nl = nodes.getElementsByTagName("HorizontalPixelSize");
        if ((nl == null) || (nl.getLength() == 0)) {
            IIOMetadataNode horz = new IIOMetadataNode("HorizontalPixelSize");
            dim.appendChild(horz);
            horz.setAttribute("value", Float.toString(25.4F / dpi));
        }

        nl = nodes.getElementsByTagName("VerticalPixelSize");
        if ((nl == null) || (nl.getLength() == 0)) {
            IIOMetadataNode horz = new IIOMetadataNode("VerticalPixelSize");
            dim.appendChild(horz);
            horz.setAttribute("value", Float.toString(25.4F / dpi));
        }
        meta.mergeTree("javax_imageio_1.0", nodes);
        return meta;
    }

    public void updateProgress(double pctComplete) {
        if (abortRequested) {
            throw new UserException("Aborted");
        }
        progressBar.setValue((int) (pctComplete * 100));
    }

    public static void abortRequested() {
        abortRequested = true;
    }
}
