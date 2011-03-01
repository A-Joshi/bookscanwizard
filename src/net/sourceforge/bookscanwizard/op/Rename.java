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

package net.sourceforge.bookscanwizard.op;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.sourceforge.bookscanwizard.FileHolder;
import net.sourceforge.bookscanwizard.Operation;
import net.sourceforge.bookscanwizard.PageSet;
import net.sourceforge.bookscanwizard.util.Roman;
import net.sourceforge.bookscanwizard.util.Sequence;
import net.sourceforge.bookscanwizard.UserException;

/**
 * Renames the pages according to a pattern.
 */
public class Rename extends Operation {
    private final ArrayList<Sequence> sequences = new ArrayList<Sequence>();
    private final ArrayList<Boolean> romanNumerals = new ArrayList<Boolean>();
    private int sequencePos = 0;
    
    @Override
    protected List<Operation> setup(List<Operation> operationList) throws Exception {
        rename(pageSet);
        return operationList;
    }

    private void rename(PageSet pageSet) {
        // if there are multiple threads, only let one through.
        String[] args = getTextArgs();
        for (int i=0; i < args.length; i+=3) {
            String pattern = args[i];
            boolean roman = false;
            int starting = 1;
            if (i+1 < args.length) {
                try {
                    starting = Integer.parseInt(args[i+1]);
                } catch (NumberFormatException e) {
                    starting = Roman.roman2int(args[i+1]);
                    roman = true;
                }
            }
            Sequence seq = new Sequence(pattern,starting, 1);
            if (i+2 < args.length) {
                int ending;
                try {
                    ending = Integer.parseInt(args[i+2]);
                } catch (NumberFormatException e) {
                    ending = Roman.roman2int(args[i+2]);
                }
                seq.setEndingValue(ending);
            }
            sequences.add(seq);
            romanNumerals.add(roman);
        }
        for (FileHolder holder : pageSet.getFileHolders()) {
            if (!holder.isDeleted()) {
                holder.setName(next());
            }
        }
        // verify there are no duplicates
        HashSet<String> pageNames = new HashSet<String>();
        for (FileHolder h : pageSet.getSourceFiles()) {
            if (!pageNames.add(h.getName())) {
                throw new UserException("Duplicate page:  There are muliple pages named: "+h.getName());
            }
        }
    }

    private String next() {
        while(true) {
            String value = sequences.get(sequencePos).next();
            if (value != null) {
                if (romanNumerals.get(sequencePos)) {
                    value += " "+Roman.int2Roman(sequences.get(sequencePos).getLastValue());
                }
                return value;
            }
            sequencePos++;
            if (sequencePos == sequences.size()) {
                throw new UserException("Ran out of page numbers");
            }
        }
    }
}
