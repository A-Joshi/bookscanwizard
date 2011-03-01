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

import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import javax.media.jai.Histogram;
import javax.media.jai.JAI;
import net.sourceforge.bookscanwizard.ColorOp;
import net.sourceforge.bookscanwizard.FileHolder;
import net.sourceforge.bookscanwizard.Operation;

/**
 * This moves the darkest 1% of colors to black, and the lightest 1% of
 * colors to white.
 */
public class AutoLevels extends Operation implements ColorOp {
    @Override
    public RenderedImage performOperation(FileHolder currentHolder, RenderedImage img) throws Exception {
        double lowThreshold = .01;
        double highThreshold = .99;
        if (getArgs().length > 0) {
            lowThreshold = getArgs()[0] / 100;
        }
        if (getArgs().length > 1) {
            highThreshold = getArgs()[1] / 100;
        }

        Histogram histogram =
           (Histogram)JAI.create("histogram", img).getProperty("histogram");
        double[] blackLevels = histogram.getPTileThreshold(lowThreshold);
        double[] whiteLevels = histogram.getPTileThreshold(highThreshold);
        double[] scale = new double[blackLevels.length];
        double[] offset = new double[blackLevels.length];
        for (int i=0; i < scale.length; i++) {
            scale[i] = 255D / (whiteLevels[i] - blackLevels[i]);
            offset[i] = 255D * blackLevels[i] / (blackLevels[i] - whiteLevels[i]);
        }
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(img);
        pb.add(scale);
        pb.add(offset);
        img =  JAI.create("rescale", pb);
        return img;
    }
}
