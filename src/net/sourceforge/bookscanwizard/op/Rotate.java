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
import net.sourceforge.bookscanwizard.PageSet;
import net.sourceforge.bookscanwizard.FileHolder;
import net.sourceforge.bookscanwizard.Operation;
import javax.media.jai.operator.TransposeType;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import javax.media.jai.InterpolationBilinear;
import javax.media.jai.JAI;
import static javax.media.jai.operator.TransposeDescriptor.ROTATE_270;
import static javax.media.jai.operator.TransposeDescriptor.ROTATE_90;
import static javax.media.jai.operator.TransposeDescriptor.ROTATE_180;

/**
 * Rotates an image.  The rotation can either be specified in degrees,
 * or as two points.  The two points should specify a horizontal line
 * from left to right.
 */
public class Rotate extends Operation {
    private double degrees;

    @Override
    protected List<Operation> setup(List<Operation> operationList) throws Exception {
        double[] args = getArgs();
        if (args.length == 1) {
            degrees = args[0];
        } else {
            args = getScaledArgs();
            degrees = Math.toDegrees(Math.atan2(args[1] - args[3], args[2] - args[0]));
        }
        return operationList;
    }

    @Override
    public RenderedImage performOperation(FileHolder holder, RenderedImage img) {
        TransposeType transpose = null;
        if (degrees == 90) {
            transpose = ROTATE_90;
        } else if (degrees == 180) {
            transpose = ROTATE_180;
        } else if (degrees == -90 || degrees == 270) {
            transpose = ROTATE_270;
        }
        if (transpose != null) {
            // the easy way
            return JAI.create("transpose", img, transpose);
        } else {
            // the hard way
            float angle = (float) Math.toRadians(degrees);
            float centerX = 0;//img.getWidth() / 2;
            float centerY = 0;//img.getHeight() / 2;
            ParameterBlock pb = new ParameterBlock();
            pb.addSource(img);
            pb.add(centerX);
            pb.add(centerY);
            pb.add(angle);
            pb.add(new InterpolationBilinear());
            return JAI.create("rotate", pb);
        }
    }
}
