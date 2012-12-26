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
/*
 * Based on code from
 * http://stackoverflow.com/questions/808441/inverse-bilinear-interpolation/813702#813702
 */
package net.sourceforge.bookscanwizard.util;

import java.util.Arrays;

public class Bilinear {

    private static boolean equals(double a, double b, double tolerance) {
        return (a == b)
                || ((a <= (b + tolerance))
                && (a >= (b - tolerance)));
    }

    private static double cross2(double x0, double y0, double x1, double y1) {
        return x0 * y1 - y0 * x1;
    }

    private static boolean in_range(double val, double range_min, double range_max, double tol) {
        return ((val + tol) >= range_min) && ((val - tol) <= range_max);
    }

    public static class Solution {
        int solutions;
        double s;
        double t;
        double s2;
        double t2;
    }

    /* Returns number of solutions found.  If there is one valid solution, it will be put in s and t */
    public static Solution inverse(double x0, double y0, double x1, double y1, double x2, double y2, double x3, double y3, double x, double y) {
        Solution solution = new Solution();
        int t_valid, t2_valid;

        double a = cross2(x0 - x, y0 - y, x0 - x2, y0 - y2);
        double b1 = cross2(x0 - x, y0 - y, x1 - x3, y1 - y3);
        double b2 = cross2(x1 - x, y1 - y, x0 - x2, y0 - y2);
        double c = cross2(x1 - x, y1 - y, x1 - x3, y1 - y3);
        double b = 0.5 * (b1 + b2);

        double s, s2 = 0, t = 0, t2 = 0;

        double am2bpc = a - 2 * b + c;
        /* this is how many valid s values we have */
        int num_valid_s = 0;

        if (equals(am2bpc, 0, 1e-10)) {
            if (equals(a - c, 0, 1e-10)) {
                /* Looks like the input is a line */
                /* You could set s=0.5 and solve for t if you wanted to */
                return solution;
            }
            s = a / (a - c);
            if (in_range(s, 0, 1, 1e-10)) {
                num_valid_s = 1;
            }
        } else {
            double sqrtbsqmac = Math.sqrt(b * b - a * c);
            s = ((a - b) - sqrtbsqmac) / am2bpc;
            s2 = ((a - b) + sqrtbsqmac) / am2bpc;
            num_valid_s = 0;
            if (in_range(s, 0, 1, 1e-10)) {
                num_valid_s++;
                if (in_range(s2, 0, 1, 1e-10)) {
                    num_valid_s++;
                }
            } else {
                if (in_range(s2, 0, 1, 1e-10)) {
                    num_valid_s++;
                    s = s2;
                }
            }
        }

        if (num_valid_s == 0) {
            return solution;
        }

        t_valid = 0;
        if (num_valid_s >= 1) {
            double tdenom_x = (1 - s) * (x0 - x2) + s * (x1 - x3);
            double tdenom_y = (1 - s) * (y0 - y2) + s * (y1 - y3);
            t_valid = 1;
            if (equals(tdenom_x, 0, 1e-10) && equals(tdenom_y, 0, 1e-10)) {
                t_valid = 0;
            } else {
                /* Choose the more robust denominator */
                if (Math.abs(tdenom_x) > Math.abs(tdenom_y)) {
                    t = ((1 - s) * (x0 - x) + s * (x1 - x)) / (tdenom_x);
                } else {
                    t = ((1 - s) * (y0 - y) + s * (y1 - y)) / (tdenom_y);
                }
                if (!in_range(t, 0, 1, 1e-10)) {
                    t_valid = 0;
                }
            }
        }

        /* Same thing for s2 and t2 */
        t2_valid = 0;
        if (num_valid_s == 2) {
            double tdenom_x = (1 - s2) * (x0 - x2) + s2 * (x1 - x3);
            double tdenom_y = (1 - s2) * (y0 - y2) + s2 * (y1 - y3);
            t2_valid = 1;
            if (equals(tdenom_x, 0, 1e-10) && equals(tdenom_y, 0, 1e-10)) {
                t2_valid = 1;
            } else {
                /* Choose the more robust denominator */
                if (Math.abs(tdenom_x) > Math.abs(tdenom_y)) {
                    t2 = ((1 - s2) * (x0 - x) + s2 * (x1 - x)) / (tdenom_x);
                } else {
                    t2 = ((1 - s2) * (y0 - y) + s2 * (y1 - y)) / (tdenom_y);
                }
                if (!in_range(t2, 0, 1, 1e-10)) {
                    t2_valid = 0;
                }
            }
        }

        /* Final cleanup */
        if (t2_valid > 0 && t_valid == 0) {
            s = s2;
            t = t2;
            t_valid = t2_valid;
            t2_valid = 0;
        }

        /* Output */
        if (t_valid > 0) {
            solution.s = s;
            solution.t = t;
        }

        if (t2_valid > 0) {
            solution.s2 = s2;
            solution.t2 = t2;
        }
        solution.solutions = t_valid + t2_valid;
        return solution;
    }

    public static double[] interpolate(double x0, double y0, double x1, double y1, double x2, double y2, double x3, double y3, double s, double t) {
        double[] xy = new double[2];
        xy[0] = t * (s * x3 + (1 - s) * x2) + (1 - t) * (s * x1 + (1 - s) * x0);
        xy[1] = t * (s * y3 + (1 - s) * y2) + (1 - t) * (s * y1 + (1 - s) * y0);
        return xy;
    }


   // -------------------------------------------------------------------
   // Testing methods below


    private static double randrange(double range_min, double range_max) {
        double range_width = range_max - range_min;
        return (Math.random() * range_width) + range_min;
    }

    /* Returns number of failed trials */
    private static int fuzzTestInvBilerp(int num_trials) {
        int num_failed = 0;

        double x0 = 0, y0 = 0, x1 = 0, y1 = 0, x2 = 0, y2 = 0, x3 = 0, y3 = 0, x = 0, y = 0, s, t, s2, t2, orig_s, orig_t;
        int num_st;
        int itrial;
        for (itrial = 0; itrial < num_trials; itrial++) {
            int failed = 0;
            /* Get random positions for the corners of the quad */
            x0 = randrange(-10, 10);
            y0 = randrange(-10, 10);
            x1 = randrange(-10, 10);
            y1 = randrange(-10, 10);
            x2 = randrange(-10, 10);
            y2 = randrange(-10, 10);
            x3 = randrange(-10, 10);
            y3 = randrange(-10, 10);
            /*x0 = 0, y0 = 0, x1 = 1, y1 = 0, x2 = 0, y2 = 1, x3 = 1, y3 = 1;*/
            /* Get random s and t */
            s = randrange(0, 1);
            t = randrange(0, 1);
            orig_s = s;
            orig_t = t;
            /* interpolate to get x and y */
            double xy[] = interpolate(x0, y0, x1, y1, x2, y2, x3, y3, s, t);
            x = xy[0];
            y = xy[1];
            System.out.println(x+" "+y+" "+x0+" "+y0+" "+x1+" "+y1+" "+x2+" "+y2+" "+x3+" "+y3);
            /* invert */
            Solution sol = inverse(x0, y0, x1, y1, x2, y2, x3, y3, x, y);
            s = sol.s;
            t = sol.t;
            s2 = sol.s2;
            t2 = sol.t2;

            num_st = sol.solutions;
            if (num_st == 0) {
                failed = 1;
            } else if (num_st == 1) {
                if (!(equals(orig_s, s, 1e-5) && equals(orig_t, t, 1e-5))) {
                    failed = 1;
                }
            } else if (num_st == 2) {
                if (!((equals(orig_s, s, 1e-5) && equals(orig_t, t, 1e-5))
                        || (equals(orig_s, s2, 1e-5) && equals(orig_t, t2, 1e-5)))) {
                    failed = 1;
                }
            }

            if (failed != 0) {
                num_failed++;
                System.out.println("Failed trial " + itrial);
            }
        }
        return num_failed;
    }

    public static void main(String[] args) {
//        int num_failed = fuzzTestInvBilerp(2); //(10000000);
//        System.out.println(num_failed + " of the tests failed");

        double[] xy = interpolate(100,100, 110,100,  110,110, 100,110, 1, .5);
        System.out.println(Arrays.toString(xy));
    }
}
