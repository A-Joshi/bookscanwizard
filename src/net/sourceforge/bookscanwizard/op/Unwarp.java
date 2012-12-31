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
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import net.sourceforge.bookscanwizard.FileHolder;
import net.sourceforge.bookscanwizard.Operation;
import net.sourceforge.bookscanwizard.unwarp.LaserUnwarp;

public class Unwarp extends Operation {
    LaserUnwarp laserUnwarp;

    @Override
    protected int getOperationMinPass() {
        return super.getMinPass() - 1;
    }

    @Override
    protected RenderedImage performOperation(FileHolder holder, RenderedImage img) throws Exception {
        if (laserUnwarp == null) {
            double[] args = getArgs();
            float hue = (float) args[0];
            float threshold = (float) args[1];
            float saturation = (float) args[2];
            float brightness = (float) args[3];
            int calibrationImages = (int) args[4];

            ArrayList<File> calibration = new ArrayList<File>();
            List<FileHolder> holders = getPageSet().getFileHolders();
            for (int i=0; i < calibrationImages; i++) {
                FileHolder h = holders.get(i);
                calibration.add(h.getFile());
                h.setDeleted(true);
            }
            LaserUnwarp.configure(hue, threshold, saturation, brightness);
            laserUnwarp.calibrateHeights(calibration);

            laserUnwarp = new LaserUnwarp(img);
        }
        return img;
    }
}
