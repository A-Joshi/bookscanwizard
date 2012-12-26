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

import java.awt.image.RenderedImage;
import java.util.Arrays;
import java.util.List;
import net.sourceforge.bookscanwizard.FileHolder;
import net.sourceforge.bookscanwizard.Operation;
import net.sourceforge.bookscanwizard.PageMatch;
import net.sourceforge.bookscanwizard.PageSet;

/**
 * Disables the processing of the listed pages.  This can be a list of pages
 * or ranges, and preceeded by a 0 or 1.
 *
 */
public class RemovePages extends Operation {
    private PageMatch pageMatch;
    private PageSet allPages;

    @Override
    public List<Operation> setup(List<Operation> operationList) throws Exception {
        List<FileHolder> fileHolders = pageSet.getSourceFiles();
        String list = Pages.getPageList(Arrays.asList(getTextArgs()));
        list = list.replace("first", fileHolders.get(0).getName());
        list = list.replace("last", fileHolders.get(fileHolders.size()-1).getName());
        pageMatch = new PageMatch(list);
        allPages = new PageSet(pageSet);
        allPages.setFileHolders(pageSet.getSourceFiles());
        return operationList;
    }

    @Override
    public PageSet getPageSet() {
        return allPages;
    }

    /**
     * Draws an X through the image to indicate it is not to be included
     * @return
     */
    @Override
    public RenderedImage performOperation(FileHolder holder, RenderedImage img) {
        if (pageMatch.matches(holder.getName())) {
            holder.setDeleted(true);
        }
        return img;
    }
}
