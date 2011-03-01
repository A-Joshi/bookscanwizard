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

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import javax.media.jai.BorderExtender;
import javax.media.jai.JAI;
import javax.media.jai.KernelJAI;
import net.sourceforge.bookscanwizard.ColorOp;
import net.sourceforge.bookscanwizard.FileHolder;
import net.sourceforge.bookscanwizard.Operation;
import net.sourceforge.bookscanwizard.util.Kernels;

/**
 * Sharpens an image.
 */
public class Sharpen extends Operation implements ColorOp {
    private KernelJAI kernel = null;

    @Override
    protected RenderedImage performOperation(FileHolder holder, RenderedImage img) throws Exception {
        float gain = 1;
        float radius = 2;
        float sigma;
        double[] args = getArgs();
        if (args.length > 0) {
            gain = (float) args[0];
        }
        if (args.length > 1) {
            radius = (float) args[1];
        }
        if (args.length > 2) {
            sigma = (float) args[2];
        } else {
            sigma = (float) (radius < 1 ? radius : Math.sqrt(radius));
        }
        if (kernel == null) {
            kernel = Kernels.generateGaussianKernel(radius, sigma);
        }
        ParameterBlock unsharp_params = new ParameterBlock();
        unsharp_params.addSource(img);
        unsharp_params.add(kernel);
        unsharp_params.add(gain);
        RenderingHints unsharp_hints = new RenderingHints(
                JAI.KEY_BORDER_EXTENDER,
                BorderExtender.createInstance(BorderExtender.BORDER_COPY)
         );
        img = JAI.create("UnsharpMask", unsharp_params, unsharp_hints);
        return img;
    }
}
