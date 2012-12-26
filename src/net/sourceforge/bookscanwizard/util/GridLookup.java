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

import java.util.Arrays;
import net.sourceforge.bookscanwizard.util.Bilinear.Solution;

/**
 * Returns the position in the grid of a given coordinates.  It will
 * interpolate if the coordinates are between samples.
 */
public class GridLookup {
    private int width;
    private int height;
    private float[] data;
    
    public GridLookup (int width, int height, float[] data) {
        this.width = width;
        this.height = height;
        this.data = data;
        System.out.println("wid: "+width+" "+height);
    }

    /**
     * Returns the source position of the x,y value
     */
    public float[] findPosition(float x, float y, float[] results) {
        // does a 2d binary search, followed by a  bi-linear inverse interpolation
        if (results == null) {
            results = new float[2];
        }
        int xMin = 0;
        int yMin = 0;
        int xMax = width-1;
        int yMax = height-1;
        if (!contains(x, y, xMin, yMin, xMax, yMax)) {
            results[0] = Float.NaN;
            results[1] = Float.NaN;
            return results;
        }
        boolean splitX = true;
        boolean splitY = true;
        while(splitX || splitY) {
            boolean foundSplit = false;
            if (splitX) {
                int testX = (xMin + xMax) /2;
                if (contains(x, y, xMin, yMin, testX, yMax)) {
                    xMax = testX;
                    foundSplit = true;
                } else {
                    if (contains(x, y, testX, yMin, xMax, yMax)) {
                        xMin = testX;
                        foundSplit = true;
                    }
                }
                if (xMax - xMin  <= 1) {
                    splitX = false;
                }
            }
            if (splitY) {
                int testY = (yMin + yMax) / 2;
                if (testY == yMin) {
                    break;
                }
                if (contains(x, y, xMin, yMin, xMax, testY)) {
                    yMax = testY;
                    foundSplit = true;
                } else {
                    if (contains(x, y, xMin, testY, xMax, yMax)) {
                        yMin = testY;
                        foundSplit = true;
                    }
                }
            }
            if (!foundSplit) {
                break;
            }
        }
//        System.out.println(xMin+"-"+xMax+" "+yMin+"-"+yMax);


        int pos = 0;
        float[] s = new float[8];
        for (int i = xMin; i <= xMax; i+= (xMax - xMin)) {
            for (int j=yMin; j <= yMax; j+= (yMax - yMin)) {
                getValues(i, j, results);
                s[pos++] = results[0];
                s[pos++] = results[1];
            }
        }
        while (((xMax-xMin) > 1 || (yMax-yMin>1))) {
            boolean found = false;
            if (contains(x, y, xMin+1, yMin, xMax, yMax)) {
                xMin++;
                found = true;
            }
            if (contains(x, y, xMin, yMin+1, xMax, yMax)) {
                yMin++;
                found = true;
            }
            if (contains(x, y, xMin, yMin, xMax-1, yMax)) {
                xMax--;
                found = true;
            }
            if (contains(x, y, xMin, yMin, xMax, yMax-1)) {
                yMax--;
                found = true;
            }
            if (!found) {
                break;
            }
        }
        if ((xMax-xMin) > 2 || (yMax-yMin>2)) {
            double found = Float.MAX_VALUE;
            int foundX = 0;
            int foundY = 0;
            for (int ty=0; ty < height; ty++) {
                for (int tx=0; tx < width; tx++) {
                    getValues(tx, ty, results);
                    double xDelta = (results[0] - x);
                    double yDelta = (results[1] - y);
                    double distance = xDelta * xDelta + yDelta * yDelta;
                    if (distance < found) {
                        found = distance;
                        foundX = tx;
                        foundY = ty;
                    }
                }
            }
            results[0] = foundX;
            results[1] = foundY;
            System.out.println(">1 "+x+" "+y+" "+xMin+"-"+xMax+" "+yMin+"-"+yMax+" using: "+Arrays.toString(results));
            return results;
        }

        // now we do some fancy bi-linear inverse interpolation
        Solution sol = Bilinear.inverse(s[0], s[1], s[2], s[3], s[4], s[5], s[6], s[7], x, y);
        if (sol.solutions == 0) {
            System.err.println("no solutions: "+sol.solutions+" "+x+" "+y+Arrays.toString(s));
            results[0] = Float.NaN;
            results[1] = Float.NaN;
            return results;
        }
        results[0] = (float) (xMin + (xMax - xMin) * sol.t);
        results[1] = (float) (yMin + (yMax - yMin) * sol.s);
        return results;
    }

    public final boolean contains(float x, float y, int x0, int y0, int x1, int y1) {
        float[] ul = new float[2];
        float[] lr = new float[2];
        float[] ll = new float[2];
        float[] ur = new float[2];
        getValues(x0, y0, ul);
        getValues(x1, y0, ur);
        getValues(x0, y1, ll);
        getValues(x1, y1, lr);
//        System.out.println("  "+x+" "+y);
//        System.out.println("0: "+x0+","+y0+" "+Arrays.toString(ul));
//        System.out.println("0: "+x1+","+y0+" "+Arrays.toString(ur));
//        System.out.println("0: "+x0+","+y1+" "+Arrays.toString(ll));
//        System.out.println("0: "+x1+","+y1+" "+Arrays.toString(lr));
        boolean retVal = x >= ul[0] && x >= ll[0] && x <= ur[0] && x <= lr[0] &&
               y >= ul[1] && y >= ur[1] && y <= ll[1] && y <= lr[1];
//        System.out.println(retVal);
        return retVal;
    }

    public final boolean contains1(float x, float y, int x0, int y0, int x1, int y1) {
        float[] ul = new float[2];
        float[] lr = new float[2];

        getValues(x0, y0, ul);
        getValues(x1, y1, lr);
        return x >= ul[0] && x <= lr[0] && y >= ul[1] && y <= lr[1];
    }

    public final void getValues(int x, int y, float[] results) {
        int pos=  (y * width + x) *2;
        results[0] = data[pos++];
        results[1] = data[pos];
    }
}
