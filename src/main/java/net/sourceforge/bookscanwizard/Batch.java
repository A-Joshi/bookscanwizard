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

package net.sourceforge.bookscanwizard;

import java.util.List;
import java.util.concurrent.Future;

/**
 * A batch that can work with the process bar.
 * @author Steve
 */
public interface Batch {
    /**
     * The list of operations to be performed
     * 
     * @param operations
     * @return 
     */
    List<Future<Void>> getFutures(final List<Operation> operations);
    /**
     * An operation that is called after the images have been processed.
     */
    void postOperation() throws Exception;
}
