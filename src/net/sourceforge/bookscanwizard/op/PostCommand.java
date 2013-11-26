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

import java.util.logging.Level;
import java.util.logging.Logger;
import net.sourceforge.bookscanwizard.BSW;
import net.sourceforge.bookscanwizard.Operation;
import net.sourceforge.bookscanwizard.PageSet;

/**
 * Runs a script after creating the images, with parameters
 */
public class PostCommand extends Operation{
    static final Logger logger = Logger.getLogger(BSW.class.getName());

    @Override
    public void postOperation() throws Exception {
        String cmd = arguments;
        cmd = cmd.replace("%destDir%", getPageSet().getDestinationDir().getAbsolutePath());
        cmd = cmd.replace("%currentDir%", BSW.getCurrentDirectory().getAbsolutePath());

        String fileName = PageSet.getSourceDir().getAbsoluteFile().getParentFile().getName();
        cmd = cmd.replace("%fileName%", fileName);
        cmd = cmd.replace("%parentAsName%", BSW.getCurrentDirectory().getAbsolutePath());
        logger.log(Level.INFO, "running external script: {0}", cmd);
        long time = System.currentTimeMillis();
        Process proc = Runtime.getRuntime().exec("cmd /c "+cmd, null, BSW.getCurrentDirectory());
        proc.waitFor();
        logger.log(Level.INFO, "{0} elapsed sec", ((System.currentTimeMillis() - time) / 1000));
    }
}
