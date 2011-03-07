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
import java.text.DecimalFormat;
import javax.media.jai.Histogram;
import javax.media.jai.JAI;
import net.sourceforge.bookscanwizard.ColorOp;
import net.sourceforge.bookscanwizard.FileHolder;
import net.sourceforge.bookscanwizard.Operation;

/**
 * Displays statistics about the image.  This should really display as a dialog
 * instead of going to stdout.
 */

public class ImageStatistics extends Operation implements ColorOp {
    private DecimalFormat formatter = new DecimalFormat("#.#");

    @Override
    public RenderedImage performOperation(FileHolder holder, RenderedImage img) throws Exception {
        // Get a threshold equal to the median.
        if (img.getSampleModel().getNumBands() > 1) {
            System.out.println("color: ");
            dumpStats(img);
            System.out.println();
        }
        System.out.println("bw: ");
        dumpStats(Color.toGray(img));
        return img;
    }

    private void dumpStats(RenderedImage img) {
        Histogram histogram =
            (Histogram)JAI.create("histogram", img).getProperty("histogram");

        double[] thresholds = histogram.getPTileThreshold(0.5);

        // Set up the parameter block for the source image and
        // the constants
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(img); // The source image
        pb.add(null);      // roi (the whole image)
        pb.add(1);         // The horizontal sampling rate
        pb.add(1);         // The vertical sampling rate
        RenderedImage op = JAI.create("extrema", pb);
        double[][] extrema = (double[][]) op.getProperty("extrema");

        System.out.println("median: "+toPct(thresholds));
        System.out.println("mean:   "+toPct(histogram.getMean()));
        System.out.println("low:    "+toPct(extrema[0]));
        System.out.println("high:   "+toPct(extrema[1]));

        System.out.println(".5% low: "+toPct(histogram.getPTileThreshold(.005)));
        System.out.println("99% high:"+toPct(histogram.getPTileThreshold(.99)));

        System.out.println("Adjust to gray: "+adjustToGray(histogram.getMean()));

    }
    
    private String adjustToGray(double[] values) {
        double[] sub = new double[values.length];
        for (int i=0; i < values.length; i++) {
            sub[i] = values[i] - GRAY_STANDARD;
        }
        return toPct(sub);
    }

    private String toPct(double[] values) {
        StringBuilder str = new StringBuilder("[");
        for (double n : values) {
            str.append(formatter.format(n * 100 / 255)).append(", ");
        }
        str.setLength(str.length()-2);
        str.append("]");
        return str.toString();
    }
}
