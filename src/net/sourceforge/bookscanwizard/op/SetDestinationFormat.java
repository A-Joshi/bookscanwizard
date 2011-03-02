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

import java.util.List;
import net.sourceforge.bookscanwizard.Operation;

public class SetDestinationFormat extends Operation {
    private String format;
    @Override
    protected List<Operation> setup(List<Operation> operationList) throws Exception {
        format = getTextArgs()[0];
        return operationList;
    }

}
