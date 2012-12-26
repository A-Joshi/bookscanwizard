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

import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * Defines a list of pages to match.  It takes a list of pages, separated
 * with dashes to indicate ranges, and commas for individual pages or
 * multiple ranges.  The keywords first and last refer to the first and last
 * pages of the book.
 */
public class PageMatch {
    // This will pretty much guarentee that this will compare greater than
    // any filename.
    private static String LAST_STRING =
        new String(new char[] {Character.MAX_VALUE, Character.MAX_VALUE});
    
    ArrayList<String> minValues = new ArrayList<String>();
    ArrayList<String> maxValues = new ArrayList<String>();

    public PageMatch (String pages) {
        StringTokenizer tokens = new StringTokenizer(pages, ",", false);
        while (tokens.hasMoreTokens()) {
            String set = tokens.nextToken();
            StringTokenizer setTokens = new StringTokenizer(set, "-", true);
            boolean range = false;
            String min = setTokens.nextToken();
            if (min.equals("-")) {
                range = true;
                min = "";
            } else {
                if (setTokens.hasMoreTokens()) {
                    range = true;
                    // must be a -
                    setTokens.nextToken();
                }
            }
            String max;
            if (setTokens.hasMoreTokens()) {
                max = setTokens.nextToken();
            } else {
                max = range ? LAST_STRING : min;
            }
            minValues.add(min);
            maxValues.add(max);
        }
    }

    public boolean matches(String page) {
        for (int i=0; i < minValues.size(); i++) {
            if (page.compareTo(minValues.get(i)) >= 0 && page.compareTo(maxValues.get(i)) <=0) {
                return true;
            }
        }
        return false;
    }

    public ArrayList<String> getMinValues() {
        return minValues;
    }

    public ArrayList<String> getMaxValues() {
        return maxValues;
    }
}
