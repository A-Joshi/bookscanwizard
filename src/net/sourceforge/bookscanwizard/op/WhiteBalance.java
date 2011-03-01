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
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import javax.media.jai.Histogram;
import javax.media.jai.JAI;
import javax.media.jai.LookupTableJAI;
import net.sourceforge.bookscanwizard.FileHolder;
import net.sourceforge.bookscanwizard.Operation;
import net.sourceforge.bookscanwizard.util.Spline;

/**
 * Not ready for prime time
 */
public class WhiteBalance extends Operation {
    private static DecimalFormat formatter = new DecimalFormat("#.#");
    private LookupTableJAI lookupTable;

    @Override
    protected List<Operation> setup(List<Operation> operationList) throws Exception {
        double[] args = getArgs();
        double[][] xAr = new double[args.length/2-1][];
        double[][] yAr = new double[xAr.length][];
        double avgBlack = args[args.length-2] * 255D / 100D;
        double avgWhite = args[args.length-1] * 255D / 100D;
        for (int i=0; i < xAr.length; i++) {
            double[] x = new double[5];
            double[] y = new double[5];
            xAr[i] = x;
            yAr[i] = y;
            x[0] = 0;
            y[0] = 0;
            x[1] = args[i*2] * 255D / 100D;
            y[1] = avgBlack;
            x[2] = ImageStatistics.GRAY_STANDARD;
            y[2] = ImageStatistics.GRAY_STANDARD;
            x[3] = args[i*2+1] * 255D / 100D;
            y[3] = avgWhite;
            x[4] = 255;
            y[4] = 255;
            System.out.println(Arrays.toString(x));
            System.out.println(Arrays.toString(y));
        }
        byte[][] table = new byte[xAr.length][];
        for (int i = 0; i < xAr.length; i++) {
            double[] x = xAr[i];
            double[] y = yAr[i];
            byte[] data = new byte[256];
            for (int j = 0; j <= 255; j++) {
                double value = Spline.poly_interpolate(x, y, j, 3);
                data[j] = (byte) Math.max(Math.min(255, value), 0);
                System.out.println(j+" "+data[j]);
            }
            table[i] = data;
        }
        lookupTable = new LookupTableJAI(table);
        return operationList;
    }

    @Override
    protected RenderedImage performOperation(FileHolder holder, RenderedImage img) throws Exception {
        return JAI.create("lookup", img, lookupTable);
    }

    public static String getConfig(RenderedImage img) {
        StringBuilder retVal = new StringBuilder("WhiteBalance = ");
        Histogram histogram =
           (Histogram)JAI.create("histogram", img).getProperty("histogram");
        double[] blackLevels = histogram.getPTileThreshold(.01);
        double[] whiteLevels = histogram.getPTileThreshold(.99);
        double avgBlack = 0;
        double avgWhite =0;
        for (int i=0; i < blackLevels.length; i++) {
            avgBlack += blackLevels[i];
            avgWhite += whiteLevels[i];
        }
        avgBlack /= blackLevels.length;
        avgWhite /= blackLevels.length;
        for (int i=0; i < blackLevels.length; i++) {
            retVal.append(pct(blackLevels[i]))
                .append(",")
                .append(pct(whiteLevels[i]))
                .append(", ");
        }
        retVal.append(pct(avgBlack)+","+pct(avgWhite));
        return retVal.toString();
    }

    private static String pct(double value) {
        return formatter.format(value * 100D / 255D);
    }
}
