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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.sourceforge.bookscanwizard.FileHolder;
import net.sourceforge.bookscanwizard.Operation;
import net.sourceforge.bookscanwizard.PageMatch;
import net.sourceforge.bookscanwizard.PageSet;

/**
 * Selects a set of pages from the source directory.  It is expecting even,
 * odd, or all, followed by an optional list of pages.  The pages can
 * contain dashes (-) for ranges, and commas (,) for individual pages.
 *
 * Note that this is even/odd with respect to the page order, not the actual
 * page number of the file or in the image.
 */
public class Pages extends Operation {
    private PageMatch pageMatch;
    private int position;

    @Override
    protected List<Operation> setup(List<Operation> operationList) throws Exception {
        pageSet = new PageSet(pageSet);
        List<FileHolder> selectedPages = new ArrayList<>();
        int pos=0;

        String[] textArguments = getTextArgs();
        String pagePosition = textArguments[0];
        position = FileHolder.ALL;
        if(pagePosition.equalsIgnoreCase("all")) {
            pos++;
        } else {
            if (pagePosition.equalsIgnoreCase("left") || pagePosition.equalsIgnoreCase("odd")) {
                position = FileHolder.LEFT;
                pos++;
            } else if (pagePosition.equalsIgnoreCase("right") || pagePosition.equalsIgnoreCase("even")) {
                position = FileHolder.RIGHT;
                pos++;
            }
        }
        List<String> strList = Arrays.asList(textArguments).subList(pos, textArguments.length);
        if (!strList.isEmpty()) {
            String str = getPageList(strList);
            pageMatch = new PageMatch(str.toString());
        }
        for (FileHolder holder : pageSet.getSourceFiles()) {
            if (position == 0 || position == holder.getPosition()) {
                if (pageMatch == null || pageMatch.matches(holder.getName())) {
                    selectedPages.add(holder);
                }
            }
        }
        pageSet.setFileHolders(selectedPages);
        return operationList;
    }

    public static String getPageList(List<String> textArguments) {
        StringBuilder str = new StringBuilder();
        boolean endedWithDash =true;
        for (String arg : textArguments) {
            if (!endedWithDash && !arg.startsWith("-")) {
                str.append(",");
            }
            endedWithDash = arg.endsWith("-");
            str.append(arg);
        }
        return str.toString();
    }

    public PageMatch getPageMatch() {
        return pageMatch;
    }

    public int getPosition() {
        return position;
    }
}
