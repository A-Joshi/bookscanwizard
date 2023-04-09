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
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.util.logging.Logger;
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
                        threshold = .5 * 255;
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
            if (numBands != 3) {
                throw new IllegalArgumentException("Image # bands <> 3");
            }
            // RGB image
            double[][] matrix = {{0.114D, 0.587D, 0.299D, 0.0D}};
            ParameterBlock pb = new ParameterBlock();
            pb.addSource(img);
            pb.add(matrix);
            img = JAI.create("bandcombine", pb, null);
        } else if (img.getSampleModel().getSampleSize(0) != 8) {
            //black & white:
            BufferedImage newImage = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
            Graphics2D g2d= newImage.createGraphics();
            g2d.drawImage(newImage, 0, 0, null);
            g2d.dispose();
            img = newImage;
        }
        return img;
    }
}
