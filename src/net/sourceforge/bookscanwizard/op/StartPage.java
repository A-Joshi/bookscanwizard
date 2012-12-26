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

import java.util.List;
import net.sourceforge.bookscanwizard.Operation;
import net.sourceforge.bookscanwizard.PageSet;

/**
 * Defines a new start page.  This modifies an existing Pages command to end it
 * and start a new section, within the same page range.
 */
public class StartPage extends Operation {

    @Override
    protected List<Operation> setup(List<Operation> operationList) throws Exception {
        PageSet newPageSet = new PageSet(pageSet);
        newPageSet.setFileHolders(pageSet.getFileHolders());
        newPageSet.setMinFile(getTextArgs()[0]);
        pageSet.setMaxFile(getTextArgs()[0]);
        pageSet = newPageSet;
        return operationList;
    }
}
