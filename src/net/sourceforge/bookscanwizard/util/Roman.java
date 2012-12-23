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

package net.sourceforge.bookscanwizard.util;

/**
 * Code to convert to and from Roman numerals.
 */
public class Roman {
    public enum RomanDigit {
        m  (1000),
        cm  (900),
        d   (500),
        cd  (400),
        c   (100),
        xc   (90),
        l    (50),
        xl   (40),
        x    (10),
        ix    (9),
        v     (5),
        iv    (4),
        i     (1);

        private final int value;

        RomanDigit(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    public static String int2Roman(int decimal) {
        if (decimal < 1) {
            throw new IllegalArgumentException("Value must be > 0)");
        }
        StringBuilder str = new StringBuilder();
        int remainder = decimal;
        while (remainder > 0) {
            for (RomanDigit digit : RomanDigit.values()) {
                if (digit.value() <= remainder) {
                    str.append(digit.toString());
                    remainder -= digit.value();
                    break;
                }
            }
        }
        return str.toString();
    }

    public static int roman2int(String romanValue) {
        int value = 0;
        String temp = romanValue.trim().toLowerCase();
        while (!temp.isEmpty()) {
            boolean found = false;
            for (RomanDigit digit : RomanDigit.values()) {
                if (temp.startsWith(digit.toString())) {
                    value += digit.value();
                    temp = temp.substring(digit.toString().length());
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new IllegalArgumentException(romanValue+" is not a valid roman numeral");
            }
        }
        return value;
    }

    public static  void main(String args[]) {
        int[] tests = {1,4,16,19,501,999,1999,3001};
        for (int test : tests) {
            System.out.println(int2Roman(test));
        }
        for (int test : tests) {
            int value = roman2int(int2Roman(test));
            System.out.println(roman2int(int2Roman(test)));
            if (value != test) {
                throw new RuntimeException();
            }
        }
    }
}
