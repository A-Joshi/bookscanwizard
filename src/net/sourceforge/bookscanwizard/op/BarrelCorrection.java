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
import java.awt.image.renderable.ParameterBlock;
import javax.media.jai.InterpolationBilinear;
import javax.media.jai.JAI;
import net.sourceforge.bookscanwizard.FileHolder;
import net.sourceforge.bookscanwizard.Operation;
import net.sourceforge.bookscanwizard.PerspectiveOp;
import net.sourceforge.bookscanwizard.WarpSpherical;

/**
 * Fixes barrel or pincushion distortion of an image.
 */
public class BarrelCorrection extends Operation implements PerspectiveOp {
    private WarpSpherical warpSpherical;

    @Override
    public RenderedImage performOperation(FileHolder holder, RenderedImage newImage) {
        double[] args = getArgs();
        double amplitude3 = -args[0];
        double amplitude2 = 0;
        double amplitude1 = 0;
        if (getArgs().length> 1) {
            amplitude2 = -args[1];
        }
        if (getArgs().length> 2) {
            amplitude1 = -args[2];
        }
        double focusX;
        double focusY;
        if (getScaledArgs().length > 3) {
            focusX = getScaledArgs()[3];
            focusY = getScaledArgs()[4];
        } else {
            focusX = newImage.getWidth() /2;
            focusY = newImage.getHeight()/2;
        }
        ParameterBlock pb2 = new ParameterBlock();
        pb2.addSource(newImage);
        warpSpherical = new WarpSpherical(focusX, focusY, amplitude3, amplitude2, amplitude1);
        pb2.add(warpSpherical);
        pb2.add(new InterpolationBilinear());
        return JAI.create("warp", pb2);
    }
}
