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

package net.sourceforge.bookscanwizard;

import java.awt.geom.Point2D;
import javax.media.jai.Warp;

/**
 * A warp operation that attempts to determine the inverse warp.  The warp
 * must be regular enough that an iterative approach will be able to solve
 * the warp.
 */
public final class InverseWarp extends Warp {
    private Warp warp;
    private static final double MAX_MATCH = .1;


    public InverseWarp(Warp warp) {
        this.warp = warp;
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
        Point2D.Float pt = new Point2D.Float();
        for (int y = ymin; y < ymax; y += periodY) {
            for (int x = xmin; x < xmax; x += periodX) {
                pt.x = x;
                pt.y = y;
                Point2D dest = findSource(pt);
                destRect[index++] = (float) dest.getX();
                destRect[index++] = (float) dest.getY();
            }
        }
        return destRect;
    }

    private Point2D findSource(Point2D destination) {
        Point2D.Double min = new Point2D.Double(-500,-500);
        Point2D.Double max = new Point2D.Double(1050,301);
        Point2D.Double guess  = new Point2D.Double((min.getX() + max.getX()/2), (min.getY() + max.getY()) /2);
        for (int i=0; i < 20; i++) {
            guess.x = findClosestX(guess.y, min.x, max.x, destination.getX());
            guess.y = findClosestY(guess.x, min.y, max.y, destination.getY());
            if (getDistanceSq(guess, destination) < MAX_MATCH * MAX_MATCH) {
                break;
            }
        }
        return guess;
    }

    private double findClosestX(double y, double min, double max, double target) {
        Point2D.Double pt = new Point2D.Double();
            pt.y = y;
        while (true) {
            double center = (min + max) /2;
            pt.x = center;
            double centerF = getPoint(pt).getX();
            if ((max - min) < MAX_MATCH) {
                return center;
            }
            if (centerF > target) {
                max = center;
            } else {
                min = center;
            }
        }
    }

    private double findClosestY(double x, double min, double max, double target) {
        Point2D.Double pt = new Point2D.Double();
            pt.x = x;
        while (true) {
            double center = (min + max) /2;
            pt.y = center;
            double centerF = getPoint(pt).getY();
            if ((max - min) < MAX_MATCH) {
                return center;
            }
            if (centerF > target) {
                max = center;
            } else {
                min = center;
            }
        }
    }

    private double getDistanceSq(Point2D guess, Point2D target) {
      Point2D test =getPoint(guess);
      return test.distanceSq(target);
    }

    private Point2D getPoint(Point2D source) {
        return warp.mapDestPoint(source);
    }
}
