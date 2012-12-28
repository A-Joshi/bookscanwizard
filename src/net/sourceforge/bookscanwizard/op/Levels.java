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
import java.awt.image.renderable.ParameterBlock;
import javax.media.jai.JAI;
import net.sourceforge.bookscanwizard.ColorOp;
import net.sourceforge.bookscanwizard.FileHolder;
import net.sourceforge.bookscanwizard.Operation;
import net.sourceforge.bookscanwizard.UserException;

/**
 * Increate the darkness and/or brightness of an image.  Brightness
 * is defined as values between 0-100, with 100 being white and 0 being black.
 * This command spreads out the available colors over a smaller range, so that
 * the first number and below is pure black, and the second number above is
 * pure wite.  Or to adjust the 3 channels independently, give 3 numbers
 * for the lows, then 3 numbers for the high levels.
 */
public class Levels extends Operation implements ColorOp {
    @Override
    public RenderedImage performOperation(FileHolder holder, RenderedImage img) {
        if (getArgs().length < 2) {
            throw new UserException("There must be at least two parameters for the Levels command");
        }
        if (getArgs().length % 2 > 0) {
            throw new UserException("There must be an even numbert of parameters for the Levels command");
        }
        double[] scale = new double[getArgs().length / 2];
        double[] offset = new double[scale.length];
        for (int i=0; i < scale.length; i++) {
            double blackLevel = getArgs()[i * 2] * 255D / 100D;
            double whiteLevel = getArgs()[i * 2 + 1] * 255D / 100D;
            scale[i] = 255D / (whiteLevel - blackLevel);
            offset[i] = 255D * blackLevel / (blackLevel - whiteLevel);
        }
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(img);
        pb.add(scale);
        pb.add(offset);
        img = JAI.create("rescale", pb);
        return img;
    }
}
