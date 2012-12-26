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

import net.sourceforge.bookscanwizard.Operation;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import javax.media.jai.JAI;
import net.sourceforge.bookscanwizard.ColorOp;
import net.sourceforge.bookscanwizard.FileHolder;

/**
 * Increate or decrease the Brightness of the images.  If one number is given,
 * it will increase all channels by that amount.  If three numbers are given,
 * then it will do the change by channel.
 */
public class Brightness extends Operation implements ColorOp {

    @Override
    public RenderedImage performOperation(FileHolder holder, RenderedImage img) {
        double[] values =getArgs();
        for (int i=0; i < values.length; i++) {
            values[i] = 1 + (getArgs()[i] / 100);
        }
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(img);
        pb.add(values);
        return JAI.create("multiplyconst", pb);
    }
}
