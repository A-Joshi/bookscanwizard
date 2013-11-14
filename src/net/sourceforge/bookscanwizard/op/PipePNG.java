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
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import javax.imageio.ImageIO;
import net.sourceforge.bookscanwizard.BSW;
import net.sourceforge.bookscanwizard.ColorOp;
import net.sourceforge.bookscanwizard.FileHolder;
import net.sourceforge.bookscanwizard.Operation;
import net.sourceforge.bookscanwizard.UserException;
import net.sourceforge.bookscanwizard.util.ProcessHelper;

/**
 * This will write the image out to stdout as a png, and read in a replacement
 * image from stdin.
 */
public class PipePNG extends Operation implements ColorOp {
        @Override
    public RenderedImage performOperation(FileHolder holder, RenderedImage img) throws Exception {
        String[] args = getTextArgs();
        ProcessHelper.fixScript(args);
        ProcessBuilder processBuilder = new ProcessBuilder(args)
            .directory(BSW.getCurrentDirectory())
            .redirectErrorStream(true);
        Process proc = processBuilder.start();
        try (OutputStream os = new BufferedOutputStream(proc.getOutputStream())) {
            ImageIO.write(img, "PNG", os);
        }
            Exception imageException;
            int retVal;
            try (InputStream is = new BufferedInputStream(proc.getInputStream())) {
                imageException = null;
                try {
                    img = ImageIO.read(is);
                } catch (Exception e) {
                    imageException = e;
                }
                retVal = proc.waitFor();
            }
        if  (retVal != 0) {
            throw new UserException("PipeCommand failed with a return value of "+retVal);
        }
        if (imageException != null) {
            throw imageException;
        }
        return img;
    }
}
