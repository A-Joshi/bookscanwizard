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

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import javax.media.jai.BorderExtender;
import javax.media.jai.JAI;
import javax.media.jai.KernelJAI;
import net.sourceforge.bookscanwizard.ColorOp;
import net.sourceforge.bookscanwizard.FileHolder;
import net.sourceforge.bookscanwizard.Operation;
import net.sourceforge.bookscanwizard.util.Kernels;

/**
 *
 */
public class GaussianBlur extends Operation implements ColorOp {
    private KernelJAI kernel = null;

    @Override
    protected RenderedImage performOperation(FileHolder holder, RenderedImage img) throws Exception {
        float radius = 3;
        float sigma;
        double[] args = getArgs();
        if (args.length > 0) {
            radius = (float) args[0];
        }
        if (args.length > 1) {
            sigma = (float) args[1];
        } else {
            sigma = (float) (radius < 1 ? radius : Math.sqrt(radius));
        }
        if (kernel == null) {
            kernel = Kernels.generateGaussianKernel(radius, sigma);
        }
        RenderingHints unsharp_hints = new RenderingHints(
                JAI.KEY_BORDER_EXTENDER,
                BorderExtender.createInstance(BorderExtender.BORDER_COPY)
         );
        img = Kernels.convolveSymmetric(img, kernel, unsharp_hints);
        return img;
    }
    
    public static  RenderedImage blur(RenderedImage img, float radius) {
        return blur(img, radius, radius < 1 ? radius : (float) Math.sqrt(radius));
    }

    public static RenderedImage blur(RenderedImage img, float radius, float sigma) {
        RenderingHints unsharp_hints = new RenderingHints(
                JAI.KEY_BORDER_EXTENDER,
                BorderExtender.createInstance(BorderExtender.BORDER_COPY)
         );
        KernelJAI kernel = Kernels.generateGaussianKernel(radius, sigma);
        return Kernels.convolveSymmetric(img, kernel, unsharp_hints);
    }
}
