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
import net.sourceforge.bookscanwizard.Operation;

/**
 * Maximizes the color range while keeping the mid range set at the current
 * value.  This should be done when the brightness has already been normalized
 */
public class ConfigBalancedAutoLevels {
    private DecimalFormat formatter = new DecimalFormat("#.#");

    public String getConfig(RenderedImage img) {
//        if (true) return new ConfigGrayCard().getConfig(img);
        StringBuilder retVal = new StringBuilder("Levels = ");
        StringBuilder gamma = new StringBuilder("Gamma = ");
        Histogram histogram =
           (Histogram)JAI.create("histogram", img).getProperty("histogram");
        double[] blackLevels = histogram.getPTileThreshold(.01);
        double[] whiteLevels = histogram.getPTileThreshold(.99);
        for (int i=0; i < blackLevels.length; i++) {
            double blackLevel = blackLevels[i] * 100D / 255D;
            retVal.append(formatter.format(blackLevel)).append(",");
            double whiteLevel = whiteLevels[i] * 100D / 255D;
            retVal.append(formatter.format(whiteLevel)).append(", ");

            double scale = 255D / (whiteLevels[i] - blackLevels[i]);
            double offset = 255D * blackLevels[i] / (blackLevels[i] - whiteLevels[i]);

            double newGray = offset + Operation.GRAY_STANDARD * scale;
            gamma.append(formatter.format(newGray)).append(", ");
        }
        retVal.setLength(retVal.length() - 2);
        retVal.append("\n");
        gamma.setLength(gamma.length() - 2);
        retVal.append(gamma);
        return retVal.toString();
    }
}
