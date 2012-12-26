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

import net.sourceforge.bookscanwizard.UserException;

/**
 * Returns a sequence of numbers for use in page file names.
 */
public class Sequence {
    private static final String ZEROS = "0000000000";
    private static final String HASH  = "##########";

    private int lastValue = 1;
    private int increment = 1;
    private String prefix;
    private String suffix;
    private int patternLength;
    private int endingValue = Integer.MAX_VALUE;

    public Sequence(String pattern) {
        this(pattern, 1, 1);
    }
    
    public Sequence(int maxValue) {
        this(HASH.substring(0, Integer.toString(maxValue).length()));
    }

    public Sequence(String pattern, int startingValue, int increment) {
        this.lastValue = startingValue - increment;
        this.increment = increment;
        int first = pattern.indexOf("#");
        int last = pattern.lastIndexOf("#");
        if (first < 0) {
            throw new UserException("Could not find # for the pattern "+pattern);
        }
        prefix = pattern.substring(0, first);
        suffix = pattern.substring(last + 1);
        patternLength = last - first + 1;
    }

    /**
     * Returns the next name in the sequence
     */
    public String next() {
        if (lastValue == endingValue) {
            return null;
        }
        lastValue = lastValue += increment;
        return last();
    }

    /**
     * Returns the last name in the sequence
     */
    public String last() {
        return prefix + zeroFill(lastValue, patternLength) + suffix;
    }

    /**
     *  The last value returned by this sequence
     */
    public int getLastValue() {
        return lastValue;
    }

    /**
     * Sets the maximum value that should be returned by the sequence
     */
    public void setEndingValue(int endingValue) {
        this.endingValue = endingValue;
    }

    public static String zeroFill(int value, int length) {
        String x = ZEROS + value;
        return x.substring(x.length() - length);
    }

    public static void main(String[] args) {
        Sequence seq = new Sequence("test####.tif", 2, 2);
        System.out.println(seq.next());
        System.out.println(seq.next());
        System.out.println(seq.next());
    }
}
