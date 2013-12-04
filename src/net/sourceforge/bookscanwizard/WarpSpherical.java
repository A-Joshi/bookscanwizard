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

import javax.media.jai.Warp;

/**
 * A warp operation that can correct pincushion or barrel 
 * distortion. 
 */
public final class WarpSpherical extends Warp {
    private final double amplitude3;
    private final double amplitude2;
    private final double amplitude1;

    private final double focusX;
    private final double focusY;

    /**
     * Create a new warp operation
     *
     * @param amplitude3 the amplitude of warp.  To correct Barrel distortion
     *        use a small negative value (such as -.000001).  To correct
     *        pincushion distortion use a small positive value.
     * @param x the x coordinate of the focal point of the distortion
     * @param y the y coordinate of the focal point of the distortion
     * @param amplitude2
     * @param amplitude1
     */
    public WarpSpherical(double x, double y, double amplitude3, double amplitude2, double amplitude1) {
        this.focusX = x;
        this.focusY = y;
        this.amplitude3 = amplitude3;
        this.amplitude2 = amplitude2;
        this.amplitude1 = amplitude1;
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
        for (int y = ymin; y < ymax; y += periodY) {
            for (int x = xmin; x < xmax; x += periodX) {
                double x1 = x - focusX;
                double y1 = y - focusY;
                double r = Math.sqrt(x1 * x1 + y1 * y1);
                double s = 1;
                if (amplitude3 != 0) {
                    s += amplitude3 * r * r * r;
                }
                if (amplitude2 != 0) {
                    s += amplitude2 * r * r;
                }
                if (amplitude1 != 0) {
                    s += amplitude1 * r;
                }
                destRect[index++] = (float) (s * x1 + focusX);
                destRect[index++] = (float) (s * y1 + focusY);
            }
        }
        return destRect;
    }
}
