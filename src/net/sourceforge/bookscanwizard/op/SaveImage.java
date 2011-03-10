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

import com.sun.media.imageio.plugins.jpeg2000.J2KImageWriteParam;
import com.sun.media.jai.codec.TIFFEncodeParam;
import com.sun.media.jai.codec.TIFFField;
import static com.sun.media.imageio.plugins.tiff.BaselineTIFFTagSet.TAG_X_RESOLUTION;
import static com.sun.media.imageio.plugins.tiff.BaselineTIFFTagSet.TAG_Y_RESOLUTION;
import static com.sun.media.imageio.plugins.tiff.BaselineTIFFTagSet.TAG_RESOLUTION_UNIT;
import static com.sun.media.imageio.plugins.tiff.BaselineTIFFTagSet.RESOLUTION_UNIT_INCH;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.JAI;
import net.sourceforge.bookscanwizard.BSW;
import net.sourceforge.bookscanwizard.FileHolder;
import net.sourceforge.bookscanwizard.Operation;
import net.sourceforge.bookscanwizard.PageSet;
import net.sourceforge.bookscanwizard.util.ImageUtilities;
import org.w3c.dom.NodeList;

/**
 * Saves the image.
 */
public class SaveImage extends Operation  {
    private static final Logger logger = Logger.getLogger(SaveImage.class.getName());

    private static ImageWriter jpeg2000Writer;
    private static ImageWriter jpegWriter;
    private static ImageWriter pngWriter;

    static {
        ImageUtilities.allowNativeCodec("jpeg2000", ImageWriterSpi.class, false);
        jpeg2000Writer = ImageIO.getImageWritersByFormatName("jpeg2000").next();
        jpegWriter = ImageIO.getImageWritersByFormatName("jpeg").next();
        pngWriter = ImageIO.getImageWritersByFormatName("png").next();
    }

    @Override
    protected RenderedImage performOperation(FileHolder holder, RenderedImage img) throws Exception {
        if (!holder.isDeleted() && !BSW.instance().isInPreview()) {
            savedImages = true;
            String[] args = getTextArgs();
            String format = "tiff";
            if (args.length > 0) {
                format = args[0].toLowerCase();
            }
            int dpi = PageSet.getDestinationDPI();
            if (dpi == 0) {
                dpi = (int) holder.getDPI();
            }
            File destinationDir = pageSet.getDestinationDir();
            String destFile = new File(destinationDir, holder.getName()).toString();
            if (format.startsWith("tif")) {
                saveTiff(destFile, img, dpi);
            } else if (format.equals("jpeg") || format.equals("jpg")) {
                saveJpeg(destFile, img, dpi);
            } else if (format.equals("jp2")) {
                saveJ2000(destFile, img, dpi);
            } else if (format.equals("png")) {
                savePng(destFile, img, dpi);
            }
            logger.log(Level.INFO, "saved {0}", holder.getName());

        }
        return img;
    }

    private void saveTiff(String destFile, RenderedImage img, int dpi) {
        // Leave this as the jai instead of imageio because its working, and
        // I don't want to take the time to verify it would still work ok
        // with imageio.

        TIFFEncodeParam param = new TIFFEncodeParam();
        param.setCompression( pageSet.getCompressionType());

        TIFFField[] extras = new TIFFField[3];
        extras[0] = new TIFFField(TAG_X_RESOLUTION, TIFFField.TIFF_RATIONAL, 1, new long[][] {{dpi, 1},{0 ,0}});
        extras[1] = new TIFFField(TAG_Y_RESOLUTION, TIFFField.TIFF_RATIONAL, 1, new long[][] {{dpi, 1},{0 ,0}});
        extras[2] = new TIFFField(TAG_RESOLUTION_UNIT, TIFFField.TIFF_SHORT, 1, new char[] { RESOLUTION_UNIT_INCH});
        param.setExtraFields(extras);
        JAI.create("filestore", img, destFile+".tif", "TIFF", param);
    }

    private void saveJpeg(String destFile, RenderedImage img, int dpi) throws IOException {
        ImageWriteParam writeParam = jpegWriter.getDefaultWriteParam();
        String args[] = getTextArgs();
        if (args.length > 1 ) {
            float quality = Float.parseFloat(args[0]);
            writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            writeParam.setCompressionQuality(quality);   // an integer between 0 and 1
        }
        final ImageOutputStream stream = ImageIO.createImageOutputStream(new File(destFile+".jpg"));
        ImageTypeSpecifier typeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB);
        IIOMetadata metadata = jpegWriter.getDefaultImageMetadata(typeSpecifier, writeParam);
        setDpi(metadata, dpi);
        try {
            jpegWriter.write(metadata, new IIOImage(img, null, metadata), writeParam);
        } finally {
            stream.close();
        }
    }

    private void saveJ2000(String destFile, RenderedImage img, int dpi) throws IOException {
        String args[] = getTextArgs();
        float rate = 1f/10f;
        if (args.length > 1 ) {
            rate = 1f / Float.parseFloat(args[0]);
        }
        FileOutputStream fos = new FileOutputStream(destFile+".jp2");
        try {
            writeJpeg2000Image(img, fos, dpi, getCompression());
        } finally {
            fos.close();
        }
    }

    private void savePng(String destFile, RenderedImage img, int dpi) throws IOException {
        final ImageOutputStream stream = ImageIO.createImageOutputStream(new File(destFile+".png"));
        ImageWriteParam writeParam = pngWriter.getDefaultWriteParam();
        ImageTypeSpecifier typeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB);
        IIOMetadata metadata = pngWriter.getDefaultImageMetadata(typeSpecifier, writeParam);
        setDpi(metadata, dpi);
        try {
            pngWriter.write(metadata, new IIOImage(img, null, metadata), writeParam);
        } finally {
            stream.close();
        }
    }

    public static void writeJpeg2000Image(RenderedImage img, OutputStream out, int dpi, double rate) throws IOException {
        ImageTypeSpecifier spec = ImageTypeSpecifier.createFromRenderedImage(img);
        J2KImageWriteParam paramJ2K = new J2KImageWriteParam();
        if (rate < 1) {
            paramJ2K.setLossless(false);
            paramJ2K.setFilter(J2KImageWriteParam.FILTER_97);
            paramJ2K.setEncodingRate(rate*24);
        } else {
            paramJ2K.setLossless(true);
        }
        IIOMetadata metadata = jpeg2000Writer.getDefaultImageMetadata(spec, paramJ2K);
        metadata = setDpi(metadata, dpi);
        IIOImage ioImage = new IIOImage(img, null, metadata);
        ImageOutputStream ios = ImageIO.createImageOutputStream(out);
        jpeg2000Writer.setOutput(ios);
        jpeg2000Writer.write(null, ioImage, paramJ2K);
        ios.close();
    }

    private static IIOMetadata setDpi(IIOMetadata meta, int dpi) throws IOException {
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
}
