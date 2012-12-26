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

import javax.media.jai.Warp;

/**
 * An abstract warp.  It isn't the fastest way to warp, but it saves
 * trying to implement warpSparseRect()
 */
public abstract class AbstractWarp extends Warp {

    @Override
    public float[] warpSparseRect(int xmin, int ymin, int width, int height,
            int periodX, int periodY, float[] destRect) {
        float[] point = new float[2];
        int xmax = xmin + width;
        int ymax = ymin + height;
        int count = ((width + (periodX - 1)) / periodX) * ((height + (periodY - 1)) / periodY);
        if (destRect == null) {
            destRect = new float[2 * count];
        }
        int index = 0;
        for (int y = ymin; y < ymax; y += periodY) {
            for (int x = xmin; x < xmax; x += periodX) {
                calcWarp(point, x,y);
                destRect[index++] = point[0];
                destRect[index++] = point[1];
            }
        }
        return destRect;
    }

    abstract public void calcWarp(float[] point, int x, int y);
}
