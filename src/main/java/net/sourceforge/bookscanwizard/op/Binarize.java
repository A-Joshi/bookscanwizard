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
import java.util.logging.Logger;
import net.sourceforge.bookscanwizard.ColorOp;
import net.sourceforge.bookscanwizard.FileHolder;
import net.sourceforge.bookscanwizard.Operation;
import net.sourceforge.bookscanwizard.filters.BinarizeFilter;
import net.sourceforge.bookscanwizard.filters.BinarizeFilter.NiblackVersion;
import net.sourceforge.bookscanwizard.filters.SauvolaBinarisationFilter;

/**
 * Converts an image to either binary (black & white), or gray scale.
 * For binary it will take an optional parameter specifying the threshold.
 * Otherwise it will use the median of the image.
 */
public class Binarize extends Operation implements ColorOp {
    protected static final Logger logger = Logger.getLogger(Operation.class.getName());

    @Override
    public RenderedImage performOperation(FileHolder holder, RenderedImage img) {
        return opt1(img);
    }
    
    public RenderedImage opt1(RenderedImage img) {
        return SauvolaBinarisationFilter.filter(img);
    }
    
    public RenderedImage opt2(RenderedImage img) {
        BinarizeFilter binarize = new BinarizeFilter(NiblackVersion.WOLFJOLION, .2, 128);        
        img = binarize.filter(Color.toGray(img));
        return img;
    }
}
