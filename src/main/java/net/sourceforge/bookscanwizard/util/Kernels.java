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

package net.sourceforge.bookscanwizard.util;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import javax.media.jai.KernelJAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.ConvolveDescriptor;

/**
 * Utilities to generate convolve kernels
 */
public class Kernels {
    /**
     * Make a Gaussian blur kernel.
     * @param radius
     * @param sigma
     * @return 
     */
    public static KernelJAI generateGaussianKernel(float radius, float sigma) {
        // work with doubles for intermediate values
        int r = (int) Math.ceil(radius);
        int rows = r * 2 + 1;
        float[] matrix = new float[rows];
        double sigma22 = 2 * sigma * sigma;
        double sigmaPi2 = 2 * Math.PI * sigma;
        double sqrtSigmaPi2 = Math.sqrt(sigmaPi2);
        double radius2 = radius * radius;
        double total = 0;
        int index = 0;
        for (int row = -r; row <= r; row++) {
            float distance = row * row;
            if (distance > radius2) {
                matrix[index] = 0;
            } else {
                matrix[index] = (float) (Math.exp(-(distance) / sigma22) / sqrtSigmaPi2);
            }
            total += matrix[index];
            index++;
        }
        for (int i = 0; i < rows; i++) {
            matrix[i] /= total;
        }
        return new KernelJAI(rows, rows, r, r, matrix, matrix);
    }

    /**
     * JAI doesn't use the separate vertical & horizontal convolutions for
     * sizes bigger than 7 if native libraries are used (for some reason).
     * @param img
     * @param kernel
     * @param hints
     * @return 
     */
    public static RenderedOp convolveSymmetric(RenderedImage img, KernelJAI kernel, RenderingHints hints) {
        if (kernel.getHeight() <= 7) {
            return ConvolveDescriptor.create(img, kernel, hints);
        } else {
            KernelJAI vKernel = new KernelJAI(1, kernel.getHeight(), kernel.getVerticalKernelData());
            img = ConvolveDescriptor.create(img, vKernel, hints);
            KernelJAI hKernel = new KernelJAI(kernel.getWidth(), 1, kernel.getHorizontalKernelData());
            return ConvolveDescriptor.create(img, hKernel, hints);
        }
    }
}
