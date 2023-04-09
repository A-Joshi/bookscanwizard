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

/**
 * Sets the scale of the preview images to be loaded.  Setting this to a 
 * smaller value than 1 will allow previews to be faster.
 */
public class SetPreviewScale extends Operation {
    @Override 
    protected List<Operation> setup(List<Operation> operationList) throws Exception {
        double scale = getArgs()[0];
//        pageSet.setPreviewScale(scale);
        return operationList;
    }
}
