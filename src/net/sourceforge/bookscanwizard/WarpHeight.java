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

package net.sourceforge.bookscanwizard;

import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import javax.media.jai.InterpolationBilinear;
import javax.media.jai.JAI;
import javax.media.jai.Warp;
import net.sourceforge.bookscanwizard.util.Interpolate;

/**
 * A warp operation that can correct for different z values
 */
public final class WarpHeight extends Warp {
    private Raster heightMap;
    private final float cameraDistance;
    private final Interpolate interpolate;
    private final RenderedImage newHeightMap;
    private final float centerX;
    private final float centerY;
    private final float dpi;

    public WarpHeight(RenderedImage heightMap, Interpolate interpolate, float cameraDistance, int centerX, int centerY, float dpi) {
        this.interpolate = interpolate;
        this.heightMap = heightMap.getData();
        this.cameraDistance = cameraDistance;
        this.centerX = centerX;
        this.centerY = centerY;
        this.dpi = dpi;

        ParameterBlock pb2 = new ParameterBlock();
        pb2.addSource(heightMap);
        pb2.add(this);
        pb2.add(new InterpolationBilinear());
        newHeightMap = JAI.create("warp", pb2);
        this.heightMap = newHeightMap.getData();
    }

    @Override
    public float[] warpSparseRect(int xmin, int ymin, int width, int height,
            int periodX, int periodY, float[] destRect) {
        int xmax = xmin + width;
        int ymax = ymin + height;
        int count = ((width + (periodX - 1)) / periodX) * ((height + (periodY - 1)) / periodY);
        if (destRect == null) {
            destRect = new float[2 * count];
        }
        int index = 0;
        int[] pixel = new int[1];
        for (int y = ymin; y < ymax; y += periodY) {
            float fromCenterY = (y - centerY) / dpi;
            float fromCenterY2 = fromCenterY * fromCenterY;
            for (int x = xmin; x < xmax; x += periodX) {
                double fromCenterX = (x - centerX) / dpi;
                float mult;
                try {
                    float pointHeight = (float) interpolate.inverse(heightMap.getPixel(x, y, pixel)[0]);
                    double z = cameraDistance - pointHeight;
                    z = Math.sqrt(z * z + fromCenterX*fromCenterX + fromCenterY2);
                    mult = (float) (cameraDistance / z);
                } catch (Exception e) {
                    mult = Float.NaN;
                }
                float val = (x - centerX) * mult + centerX;
                if (val < 0) {
                    val = x;
                }
                destRect[index++] = val;
                val = (y - centerY) * mult + centerY;
                if (val < 0) {
                    val = y;
                }
                destRect[index++] = val;
            }
        }
        return destRect;
    }

    public RenderedImage getHeightMap() {
        return newHeightMap;
    }
}
