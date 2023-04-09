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

package net.sourceforge.bookscanwizard.util;

import java.awt.image.RenderedImage;
import javax.media.jai.Histogram;
import javax.media.jai.JAI;
import net.sourceforge.bookscanwizard.op.Color;

/**
 * Returns the binarization threshold using the Otsu's method
 */
public class OtsuThreshold {

    public static RenderedImage binarize(RenderedImage img) {
        if (img.getColorModel().getNumColorComponents() > 1) {
            img = Color.toGray(img);
        }
        if (img.getSampleModel().getSampleSize(0) > 1) {
            img = JAI.create("binarize", img, getThreshold(img));
        }
        return img;
    }

    public static double getThreshold(RenderedImage img) {
        Histogram histogram =
           (Histogram)JAI.create("histogram", img).getProperty("histogram");
        int[] bins = histogram.getBins(0);
        return Math.round(otsu(bins, img.getWidth() * img.getHeight()));
    }
    
//    based on:
//    http://www.labbookpages.co.uk/software/imgProc/otsuThreshold.html
    private static double otsu(int[] histData, int total) {
        double sum = 0;
        for (int t = 0; t < 256; t++) {
            sum += t * histData[t];
        }
        double sumB = 0;
        int wB = 0;
        int wF = 0;
        double varMax = 0;
        double threshold = 0;

        for (int t = 0; t < 256; t++) {
            wB += histData[t];               // Weight Background
            if (wB == 0) {
                continue;
            }
            wF = total - wB;                 // Weight Foreground
            if (wF == 0) {
                break;
            }
            sumB += (double) (t * histData[t]);
            double mB = sumB / wB;            // Mean Background
            double mF = (sum - sumB) / wF;    // Mean Foreground

            // Calculate Between Class Variance
            double varBetween = (double) wB * (double) wF * (mB - mF) * (mB - mF);

            // Check if new maximum found
            if (varBetween > varMax) {
                varMax = varBetween;
                threshold = t;
            }
        }
        return threshold;
    }

}
