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

package net.sourceforge.bookscanwizard.op;

import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.jai.Histogram;
import javax.media.jai.JAI;
import net.sourceforge.bookscanwizard.ColorOp;
import net.sourceforge.bookscanwizard.FileHolder;
import net.sourceforge.bookscanwizard.Operation;
import net.sourceforge.bookscanwizard.UserException;

/**
 * Converts an image to either binary (black & white), or gray scale.
 * For binary it will take an optional parameter specifying the threshold.
 * Otherwise it will use the median of the image.
 */
public class Color extends Operation implements ColorOp {
    protected static final Logger logger = Logger.getLogger(Operation.class.getName());
    private double threshold = -1;

    @Override
    public RenderedImage performOperation(FileHolder holder, RenderedImage img) {
        String[] args = getTextArgs();
        if (args[0].equalsIgnoreCase("grey") || args[0].equalsIgnoreCase("gray") || args[0].equalsIgnoreCase("bw")) {
            img = toGray(img);
            if (args[0].equals("bw")) {
                if (threshold < 0) {
                    if (args.length > 1) {
                        threshold = Double.parseDouble(args[1]) / 100 * 255;
                    } else {
                        // Generate a histogram.
                        Histogram histogram =
                            (Histogram)JAI.create("histogram", img).getProperty("histogram");

                        double[] blackLevels = histogram.getPTileThreshold(.01);
                        double[] whiteLevels = histogram.getPTileThreshold(.99);

                        // Set the threshold halfway between the 1% and 99% levels
                        threshold = (blackLevels[0] + whiteLevels[0]) /2;
                        logger.log(Level.INFO, "threshold: {0}", threshold);
                    }
                }
                // Binarize the image.
                img = JAI.create("binarize", img, threshold);
            }
        } else {
            throw new UserException("Unknown color type: "+arguments);
        }
        return img;
    }

    public static RenderedImage toGray(RenderedImage img) {
        int numBands = img.getSampleModel().getNumBands();
        if (numBands > 1) {
            if (img.getSampleModel().getNumBands() != 3) {
                throw new IllegalArgumentException("Image # bands <> 3");
            }
            double[][] matrix = {{0.114D, 0.587D, 0.299D, 0.0D}};
            ParameterBlock pb = new ParameterBlock();
            pb.addSource(img);
            pb.add(matrix);
            img = JAI.create("bandcombine", pb, null);
        } else if (img.getSampleModel().getSampleSize(0) == 1) {
            BufferedImage newImage = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
            Graphics2D g2d= newImage.createGraphics();
            g2d.drawImage(newImage, 0, 0, null);
            g2d.dispose();
            img = newImage;
            /*
             // This doesn't seem to work correctly... 
            ParameterBlock pb = new ParameterBlock();
            pb.addSource(img);
            ColorModel cm =
                    new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
                                                                  new int[] {8},
                                                                  false,
                                                                  false,
                                                                  Transparency.OPAQUE,
                                                                  DataBuffer.TYPE_BYTE);
            pb.add(cm);
            img = JAI.create("ColorConvert", pb);*/
        }
        return img;
    }
}
