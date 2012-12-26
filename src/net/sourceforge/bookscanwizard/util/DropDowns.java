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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TreeMap;

/**
 * Contains lookup data for dropdown lists.
 */
public class DropDowns {
    // This is really a subset of ISO 639-2, but I'm assuming that for anything common
    // it will match the marc code (I couldn't find a lookup from the 639-2 code to marc).
    private final static TreeMap<String,String> lookupMarc = new TreeMap<String,String>();
    
    private DropDowns(){}
    
    static {
        try {
            InputStream is = DropDowns.class.getClassLoader().getResourceAsStream("language.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                String [] fields = line.split("\t");
                lookupMarc.put(fields[0], fields[1]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static String getLocalizedLanguage() {
        String locale = Locale.getDefault().getISO3Language();
        return lookupMarc.get(locale);
    }
    
    public static String[] getMarcCodes() {
        String[] retVal = new String[lookupMarc.size()];
        ArrayList<String> list = new ArrayList(lookupMarc.values());
        return list.toArray(retVal);
    }
}
