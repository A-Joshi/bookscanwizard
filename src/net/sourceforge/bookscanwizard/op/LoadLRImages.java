package net.sourceforge.bookscanwizard.op;

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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import net.sourceforge.bookscanwizard.BSW;
import net.sourceforge.bookscanwizard.MatchPages;
import net.sourceforge.bookscanwizard.Operation;
import net.sourceforge.bookscanwizard.PageSet;
import net.sourceforge.bookscanwizard.util.Sequence;

/**
 * Load camera images from separate left and right folders.
 */
public class LoadLRImages extends Operation {
    private static final String IGNORE_TIMES = "ignore_times";
    @Override
    public List<Operation> setup(List<Operation> operationList) throws Exception {
        boolean ignoreTimes = false;
        String fileName;
        if (getTextArgs()[0].equalsIgnoreCase(IGNORE_TIMES)) {
            fileName = arguments.substring(IGNORE_TIMES.length()).trim();
            ignoreTimes = true;
        } else {
            fileName = arguments;
        }
        File file = BSW.getFileFromCurrentDir(fileName);
        PageSet.setSourceDir(file);
        Sequence seq = new Sequence("BSW_####", 0, 1);
        MatchPages matchPages = new MatchPages(file, seq, ignoreTimes);
        pageSet.setSourceFiles(matchPages.getAllPages());
        ArrayList<Operation> list = new ArrayList<Operation>();
        list.addAll(operationList);
        list.addAll(getOperation("Pages= -", null, pageSet));
        return list;
    }
}
