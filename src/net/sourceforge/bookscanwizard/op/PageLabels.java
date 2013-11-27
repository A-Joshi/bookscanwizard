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

package net.sourceforge.bookscanwizard.op;

import com.itextpdf.text.pdf.PdfPageLabels;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.bookscanwizard.FileHolder;
import net.sourceforge.bookscanwizard.Operation;
import net.sourceforge.bookscanwizard.PageSet;
import net.sourceforge.bookscanwizard.UserException;
import net.sourceforge.bookscanwizard.util.Roman;
import net.sourceforge.bookscanwizard.util.Sequence;

/**
 * Defines pdf page labels for the pages.
 */
public class PageLabels extends Operation {
    /** Matches an optional prefix, followed by a cardinal number. */
    private static final Pattern NUMBER = Pattern.compile("(.*?)([0-9]+)$");
    private final ArrayList<Sequence> sequences = new ArrayList<>();
    private final ArrayList<Boolean> romanNumerals = new ArrayList<>();
    private int sequencePos = 0;
    private static PdfPageLabels labels;
    
    @Override
    protected List<Operation> setup(List<Operation> operationList) throws Exception {
        labels = new PdfPageLabels();
        rename(pageSet);
        return operationList;
    }

    private void rename(PageSet pageSet) {
        int realPage = 1;
        for (String range : getTextArgs()) {
            int pos = range.indexOf('-');
            String start, end;
            if (pos < 0) {
                start = range;
                end = range;
            } else {
                start = range.substring(0, pos);
                end = range.substring(pos+1);
            }
            start = start.trim();
            end = end.trim();
            boolean roman = false;
            int starting;
            int ending = 0;
            String prefix = "";
            Matcher match = NUMBER.matcher(start);
            if (match.find()) {
                try {
                    prefix = match.group(1);
                    starting = Integer.parseInt(match.group(2));
                } catch (NumberFormatException e) {
                    // shouldn't be able to happen
                    throw new RuntimeException(e);
                }
            } else {
                starting = Roman.roman2int(start);
                roman = true;
            }
            int style = roman ? PdfPageLabels.LOWERCASE_ROMAN_NUMERALS : PdfPageLabels.DECIMAL_ARABIC_NUMERALS;
            labels.addPageLabel(realPage, style, prefix);
            
            Sequence seq = new Sequence(prefix + "#",starting, 1);
            if (!end.isEmpty()) {
                match = NUMBER.matcher(end);
                if (match.find()) {
                    try {
                        ending = Integer.parseInt(match.group(2));
                    } catch (NumberFormatException e) {
                        // shouldn't be able to happen
                        throw new RuntimeException(e);
                    }
                } else {
                    ending = Roman.roman2int(end);
                }
                seq.setEndingValue(ending);
                realPage += (ending - starting) + 1;
            }
            sequences.add(seq);
            romanNumerals.add(roman);
        }
        for (FileHolder holder : pageSet.getFileHolders()) {
            if (!holder.isDeleted()) {
                holder.setPdfName(next());
            }
        }
    }

    private String next() {
        while(true) {
            String value = sequences.get(sequencePos).next();
            if (value != null) {
                if (romanNumerals.get(sequencePos)) {
                    value = Roman.int2Roman(sequences.get(sequencePos).getLastValue());
                }
                return value;
            }
            sequencePos++;
            if (sequencePos == sequences.size()) {
                throw new UserException("Ran out of page numbers");
            }
        }
    }

    public static PdfPageLabels getPageLabels() {
        PdfPageLabels retVal = labels;
        labels = null;
        return retVal;
    }
}
