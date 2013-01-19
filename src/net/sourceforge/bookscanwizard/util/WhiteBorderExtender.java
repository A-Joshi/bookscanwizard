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
/**
 * This is a modified version of javax.media.jai.BorderExtenderZero
 */
package net.sourceforge.bookscanwizard.util;

import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import javax.media.jai.BorderExtender;
import javax.media.jai.PlanarImage;

/**
 *
 * @author Steve
 */
public class WhiteBorderExtender extends BorderExtender {

    @Override

    /**
     * Fills in the portions of a given <code>Raster</code> that lie
     * outside the bounds of a given <code>PlanarImage</code> with
     * zeros.
     *
     * <p> The portion of <code>raster</code> that lies within
     * <code>im.getBounds()</code> is not altered.
     *
     * @param raster The <code>WritableRaster</code> the border area of
     *               which is to be filled with zero.
     * @param im     The <code>PlanarImage</code> which determines the
     *               portion of the <code>WritableRaster</code> <i>not</i>
     *               to be filled.
     *
     * @throws <code>IllegalArgumentException</code> if either parameter is
     *         <code>null</code>.
     */
    public final void extend(WritableRaster raster,
                             PlanarImage im) {

        if ( raster == null || im == null ) {
            throw new NullPointerException();
        }

        int width = raster.getWidth();
        int height = raster.getHeight();
        int numBands = raster.getNumBands();

        int minX = raster.getMinX();
        int maxX = minX + width;
        int minY = raster.getMinY();
        int maxY = minY + height;

        int validMinX = Math.max(im.getMinX(), minX);
        int validMaxX = Math.min(im.getMaxX(), maxX);
        int validMinY = Math.max(im.getMinY(), minY);
        int validMaxY = Math.min(im.getMaxY(), maxY);

        int row;
        System.out.println("samplesize: "+raster.getSampleModel().getSampleSize(0));
        System.out.println(" component: "+im.getColorModel().getComponentSize()[0]);
        System.out.println(" pixelsize: "+im.getColorModel().getPixelSize());
        switch (raster.getSampleModel().getDataType()) {
            case DataBuffer.TYPE_BYTE:
            case DataBuffer.TYPE_SHORT:
            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_INT:
                int[] iData = new int[width*numBands];
                if(validMinX > validMaxX || validMinY > validMaxY) {
                    // Raster does not intersect image.
                    for (row = minY; row < maxY; row++) {
                        raster.setPixels(minX, row, width, 1, iData);
                    }
                } else {
                    for (row = minY; row < validMinY; row++) {
                        raster.setPixels(minX, row, width, 1, iData);
                    }
                    for (row = validMinY; row < validMaxY; row++) {
                        if (minX < validMinX) {
                            raster.setPixels(minX, row,
                                             validMinX - minX, 1, iData);
                        }
                        if (validMaxX < maxX) {
                            raster.setPixels(validMaxX, row,
                                             maxX - validMaxX, 1, iData);
                        }
                    }
                    for (row = validMaxY; row < maxY; row++) {
                        raster.setPixels(minX, row, width, 1, iData);
                    }
                }
                break;

            case DataBuffer.TYPE_FLOAT:
                float[] fData = new float[width*numBands];
                if(validMinX > validMaxX || validMinY > validMaxY) {
                    // Raster does not intersect image.
                    for (row = minY; row < maxY; row++) {
                        raster.setPixels(minX, row, width, 1, fData);
                    }
                } else {
                    for (row = minY; row < validMinY; row++) {
                        raster.setPixels(minX, row, width, 1, fData);
                    }
                    for (row = validMinY; row < validMaxY; row++) {
                        if (minX < validMinX) {
                            raster.setPixels(minX, row,
                                             validMinX - minX, 1, fData);
                        }
                        if (validMaxX < maxX) {
                            raster.setPixels(validMaxX, row,
                                             maxX - validMaxX, 1, fData);
                        }
                    }
                    for (row = validMaxY; row < maxY; row++) {
                        raster.setPixels(minX, row, width, 1, fData);
                    }
                }
                break;

            case DataBuffer.TYPE_DOUBLE:
                double[] dData = new double[width*numBands];
                if(validMinX > validMaxX || validMinY > validMaxY) {
                    // Raster does not intersect image.
                    for (row = minY; row < maxY; row++) {
                        raster.setPixels(minX, row, width, 1, dData);
                    }
                } else {
                    for (row = minY; row < validMinY; row++) {
                        raster.setPixels(minX, row, width, 1, dData);
                    }
                    for (row = validMinY; row < validMaxY; row++) {
                        if (minX < validMinX) {
                            raster.setPixels(minX, row,
                                             validMinX - minX, 1, dData);
                        }
                        if (validMaxX < maxX) {
                            raster.setPixels(validMaxX, row,
                                             maxX - validMaxX, 1, dData);
                        }
                    }
                    for (row = validMaxY; row < maxY; row++) {
                        raster.setPixels(minX, row, width, 1, dData);
                    }
                }
                break;
        }
    }
}
