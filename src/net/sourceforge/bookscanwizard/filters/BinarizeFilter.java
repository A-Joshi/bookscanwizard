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

package net.sourceforge.bookscanwizard.filters;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.RenderedImage;

/**
 *
 * Code to perform various local thresholds algorythms
 */
public final class BinarizeFilter {

    private static final int win = 128;
    private final double k;
    private final double dR;
    private final NiblackVersion version;
    private int rows;
    private int cols;

    /**
     * Based on c++ code from:
     * http://liris.cnrs.fr/christian.wolf/software/binarize/ Binarization with
     * several methods (0) Niblacks method (1) Sauvola & Co. ICDAR 1997, pp
     * 147-152 (2) by myself - Christian Wolf Research notebook 19.4.2001, page
     * 129 (3) by myself - Christian Wolf 20.4.2007
     *
     * See also: Research notebook 24.4.2001, page 132 (Calculation of s)
     */
    
    /**
     * These are three local thresholding approaches.
     */
    public enum NiblackVersion {
        NIBLACK,
        SAUVOLA,
        WOLFJOLION,
    };

    public BinarizeFilter(NiblackVersion version, double k, double dR) {
        this.version = version;
        this.k = k;
        this.dR = dR;
    }

    /**
     * glide a window across the image and create two maps: mean and standard
     * deviation.
     */
    double calcLocalStats(int[][] im, double[][] map_m, double[][] map_s) {

        double m, s, max_s, sum, sum_sq, foo;
        int wxh = win / 2;
        @SuppressWarnings("UnusedAssignment")
        int x_firstth = wxh;
        int y_lastth = rows - wxh - 1;
        int y_firstth = wxh;
        double winarea = win * win;

        max_s = 0;
        for (int j = y_firstth; j <= y_lastth; j++) {
            // Calculate the initial window at the beginning of the line
            sum = sum_sq = 0;
            for (int wy = 0; wy < win; wy++) {
                int[] rowBytes = im[wy];
                for (int wx = 0; wx < win; wx++) {
                    foo = rowBytes[wx];
                    sum += foo;
                    sum_sq += foo * foo;
                }
            }
            m = sum / winarea;
            s = Math.sqrt((sum_sq - (sum * sum) / winarea) / winarea);
            if (s > max_s) {
                max_s = s;
            }
            map_m[j][x_firstth] = m;
            map_s[j][x_firstth] = s;

            // Shift the window, add and remove	new/old values to the histogram
            for (int i = 1; i <= cols - win; i++) {

                // Remove the left old column and add the right new column
                for (int wy = 0; wy < win; ++wy) {
                    foo = im[j - wxh + wy][i - 1];
                    sum -= foo;
                    sum_sq -= foo * foo;
                    foo = im[j - wxh + wy][i + win - 1];
                    sum += foo;
                    sum_sq += foo * foo;
                }
                m = sum / winarea;
                s = Math.sqrt((sum_sq - (sum * sum) / winarea) / winarea);
                if (s > max_s) {
                    max_s = s;
                }
                map_m[j][i + wxh] = m;
                map_s[j][i + wxh] = s;
            }
        }

        return max_s;
    }

    public RenderedImage filter(RenderedImage in) {
        if (in.getData().getNumBands() != 1) {
            throw new RuntimeException("This method requires Grayscale images");
        }
        byte[] pixels = ((DataBufferByte) in.getData().getDataBuffer()).getData();
        int[][] inMat = new int[in.getHeight()][in.getWidth()];
        int pos = 0;
        for (int[] inMat1 : inMat) {
            for (int x = 0; x < inMat[0].length; x++) {
                int p = pixels[pos++];
                if (p < 0) {
                    p = 256 - p;
                }
                inMat1[x] = p;
            }
        }
        pixels = new byte[in.getHeight() * in.getWidth()];
        filter(inMat, pixels);
        BufferedImage image = new BufferedImage(in.getWidth(), in.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        image.getRaster().setDataElements(0, 0, in.getWidth(), in.getHeight(), pixels);
        return image;
    }

    /**
     * The binarization routine
     */
    void filter(int[][] input, byte[] output) {
        cols = input[0].length;
        rows = input.length;
        double m, s, max_s;
        double th = 0;
        double min_I;
        int winHalfHeight = win / 2;
        int x_firstth = winHalfHeight;
        int x_lastth = cols - winHalfHeight - 1;
        int y_lastth = rows - winHalfHeight - 1;
        int y_firstth = winHalfHeight;

        // Create local statistics and store them in a double matrices
        double[][] map_m = new double[rows][cols];
        double[][] map_s = new double[rows][cols];
        max_s = calcLocalStats(input, map_m, map_s);
        int min = 255;
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                if (input[y][x] < min) {
                    min = input[y][x];
                }
            }
        }
        min_I = min;

        double[][] thsurf = new double[rows][cols];

        // Create the threshold surface, including border processing
        // ----------------------------------------------------
        for (int j = y_firstth; j <= y_lastth; j++) {

            // NORMAL, NON-BORDER AREA IN THE MIDDLE OF THE WINDOW:
            for (int i = 0; i <= cols - win; i++) {
                m = map_m[j][i + winHalfHeight];
                s = map_s[j][i + winHalfHeight];

                // Calculate the threshold
                switch (version) {
                    case NIBLACK:
                        th = m + k * s;
                        break;

                    case SAUVOLA:
                        th = m * (1 + k * (s / dR - 1));
                        break;

                    case WOLFJOLION:
                        th = m + k * (s / max_s - 1) * (m - min_I);
                        break;

                    default:
                        System.err.println("Unknown threshold type in ImageThresholder::surfaceNiblackImproved()\n");
                        System.exit(1);
                }

                thsurf[j][i + winHalfHeight] = th;

                if (i == 0) {
                    // LEFT BORDER
                    for (int ii = 0; ii <= x_firstth; ++ii) {
                        thsurf[j][ii] = th;
                    }

                    // LEFT-UPPER CORNER
                    if (j == y_firstth) {
                        for (int u = 0; u < y_firstth; ++u) {
                            for (int ii = 0; ii <= x_firstth; ++ii) {
                                thsurf[u][ii] = th;
                            }
                        }
                    }

                    // LEFT-LOWER CORNER
                    if (j == y_lastth) {
                        for (int u = y_lastth + 1; u < rows; ++u) {
                            for (int ii = 0; ii <= x_firstth; ++ii) {
                                thsurf[u][ii] = th;
                            }
                        }
                    }
                }

                // UPPER BORDER
                if (j == y_firstth) {
                    for (int u = 0; u < y_firstth; ++u) {
                        thsurf[u][i + winHalfHeight] = th;
                    }
                }

                // LOWER BORDER
                if (j == y_lastth) {
                    for (int u = y_lastth + 1; u < rows; ++u) {
                        thsurf[u][i + winHalfHeight] = th;
                    }
                }
            }

            // RIGHT BORDER
            for (int i = x_lastth; i < cols; ++i) {
                thsurf[j][i] = th;
            }

            // RIGHT-UPPER CORNER
            if (j == y_firstth) {
                for (int u = 0; u < y_firstth; ++u) {
                    for (int i = x_lastth; i < cols; ++i) {
                        thsurf[u][i] = th;
                    }
                }
            }

            // RIGHT-LOWER CORNER
            if (j == y_lastth) {
                for (int u = y_lastth + 1; u < rows; ++u) {
                    for (int i = x_lastth; i < cols; ++i) {
                        thsurf[u][i] = th;
                    }
                }
            }
        }
        int pos = 0;
        for (int y = 0; y < rows; ++y) {
            for (int x = 0; x < cols; ++x) {
                if (input[y][x] >= thsurf[y][x]) {
                    output[pos++] = (byte) 255;
                } else {
                    output[pos++] = (byte) 0;
                }
            }
        }
    }
}
