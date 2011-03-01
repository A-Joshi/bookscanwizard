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

import java.awt.image.RenderedImage;
import java.util.List;
import net.sourceforge.bookscanwizard.FileHolder;
import net.sourceforge.bookscanwizard.Operation;

/**
 * This is a template to use when creating a new operation.  If a method is
 * not required for a particular command, it can be removed.  This has the methods
 * that are customarily overridden by Operations.
 */
public class TemplateOperation extends Operation {
    /**
     * Called when the command is first configured
     */
    @Override
    protected List<Operation> setup(List<Operation> operationList) throws Exception {
        return operationList;
    }

    /**
     * Previews an operation.  The default is to do nothing.
     */
    @Override
    protected RenderedImage previewOperation(FileHolder holder, RenderedImage img) throws Exception {
        return img;
    }

    /**
     * Performs the operation.
     */
    @Override
    protected RenderedImage performOperation(FileHolder holder, RenderedImage img) throws Exception {
        return img;
    }
}
