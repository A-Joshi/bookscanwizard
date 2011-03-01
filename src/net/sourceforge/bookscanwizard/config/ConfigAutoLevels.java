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

package net.sourceforge.bookscanwizard.config;

import java.awt.image.RenderedImage;
import java.text.DecimalFormat;
import javax.media.jai.Histogram;
import javax.media.jai.JAI;
import net.sourceforge.bookscanwizard.op.Color;

/**
 * Calculates the autolevels for an image.
 */
public class ConfigAutoLevels {
    private DecimalFormat formatter = new DecimalFormat("#.#");

    public String getConfig(RenderedImage img, boolean separateRGB) {
        if (!separateRGB) {
            img = Color.toGray(img);
        }
        StringBuilder retVal = new StringBuilder("Levels = ");
        Histogram histogram =
           (Histogram)JAI.create("histogram", img).getProperty("histogram");
        double[] blackLevels = histogram.getPTileThreshold(.01);
        double[] whiteLevels = histogram.getPTileThreshold(.99);
        for (int i=0; i < blackLevels.length; i++) {
            double level = blackLevels[i] * 100D / 255D;
            retVal.append(formatter.format(level)).append(",");
            level = whiteLevels[i] * 100D / 255D;
            retVal.append(formatter.format(level)).append(", ");
        }
        retVal.setLength(retVal.length() - 2);
        return retVal.toString();
    }
}
