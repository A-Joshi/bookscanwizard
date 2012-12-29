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

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sourceforge.bookscanwizard.FileHolder;
import net.sourceforge.bookscanwizard.PageSet;
import net.sourceforge.bookscanwizard.UserException;

/**
 * This scales an image to amount necessary to the correct scaling to match the desired DPI.
 */
public class ScaleToDPI extends Scale {
    private static final Logger logger = Logger.getLogger(ScaleToDPI.class.getName());
    private float sourceDPI;
    private float destinationDPI;

    @Override
    public RenderedImage performOperation(FileHolder holder, RenderedImage img) throws IOException {
        sourceDPI = holder.getDPI();
        logger.log(Level.FINE, "Source dpi is {0}", sourceDPI);
        double[] args = getArgs();
        if (args.length > 0) {
            destinationDPI = (float) args[0];
        } else {
            destinationDPI = getPageSet().getDestinationDPI();
        }
        if (destinationDPI == 0) {
            throw new UserException("ScaleToDPI: Destination DPI is not set");
        }
        if (getPageSet().getDestinationDPI() == 0) {
            PageSet.setDestinationDPI((int) destinationDPI);
        }
        if (sourceDPI == 0 || Float.isNaN(sourceDPI)) {
            throw new UserException("ScaleToDPI: Source DPI is not set");
        } else if (destinationDPI != sourceDPI) {
            float scale = destinationDPI /  sourceDPI;
            img = qualityScale(img, scale, scale);
        } else {
            logger.info("source dpi matches destination dpi.  Skipping this step");
        }
        return img;
    }
}
