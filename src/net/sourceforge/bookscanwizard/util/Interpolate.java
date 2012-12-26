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

/**
 * Defines a linear interpolation.
 */
public final class Interpolate {
    private double x0;
    private double x1;
    private double y0;
    private double y1;

    public Interpolate(double x0, double y0, double x1, double y1) {
        this.x0 = x0;
        this.x1 = x1;
        this.y0 = y0;
        this.y1 = y1;
    }

    /**
     * Interpolates the y value for a give x value
     */
    public double interpolate(double x) {
        return interpolate(x, x0, y0, x1, y1);
    }

    /**
     * Returns the x value for a given Y value.
     */
    public double inverse(double y) {
        return interpolate(y, y0, x0, y1, x1);
    }

    public static double interpolate(double x, double x0, double y0, double x1, double y1) {
        // interpolate result
        return ((x - x0) * y1 + (x1 - x) * y0) / (x1 - x0);
    }
}
