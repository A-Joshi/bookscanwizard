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
package net.sourceforge.bookscanwizard.util;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Hashtable;

import javax.media.jai.RenderedOp;

public class Utils {

    public static BufferedImage renderedToBuffered(RenderedImage img) {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        } else if (img instanceof RenderedOp) {
            return ((RenderedOp) img).getAsBufferedImage();
        }
        ColorModel cm = img.getColorModel();
        WritableRaster raster = cm.createCompatibleWritableRaster(img.getWidth(), img.getHeight());
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        String[] keys = img.getPropertyNames();

        if (keys != null) {
            for (String key : keys) {
                props.put(key, img.getProperty(key));
            }
        }
        BufferedImage bufferedImage = new BufferedImage(cm, raster, cm.isAlphaPremultiplied(), props);
        img.copyData(raster);
        return bufferedImage;
    }
    
    /**
     * Method that returns a scaled instance of the provided
     * {@code BufferedImage}. Unlike simple scaling, this will
     * work well with thumbnail images.
     *
     * @param img the original image to be scaled
     * @param targetWidth the desired width of the scaled instance,
     *    in pixels
     * @param targetHeight the desired height of the scaled instance,
     *    in pixels
     * @param interpolationType one of the rendering hints that corresponds to
     *    {@code RenderingHints.KEY_INTERPOLATION} (e.g.
     *    {@code RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR},
     *    {@code RenderingHints.VALUE_INTERPOLATION_BILINEAR},
     *    {@code RenderingHints.VALUE_INTERPOLATION_BICUBIC})
     * @return a scaled version of the original {@code BufferedImage}
     */
    public static BufferedImage getScaledInstance(RenderedImage renderedImage,
            int targetWidth, int targetHeight, Object interpolationType) {
        BufferedImage img = renderedToBuffered(renderedImage);
        int type = (img.getTransparency() == Transparency.OPAQUE) ? BufferedImage.TYPE_INT_RGB
                : BufferedImage.TYPE_INT_ARGB;
        BufferedImage ret = img;
        // Use multi-step technique: start with original size, then
        // scale down in multiple passes with drawImage()
        // until the target size is reached
        int w = img.getWidth();
        int h = img.getHeight();
        do {
            if (w > targetWidth) {
                w /= 2;
            }
            if (w < targetWidth) {
                w = targetWidth;
            }

            if (h > targetHeight) {
                h /= 2;
            }
            if (h < targetHeight) {
                h = targetHeight;
            }

            BufferedImage tmp = new BufferedImage(w, h, type);
            Graphics2D g2 = tmp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    interpolationType);
            g2.drawImage(ret, 0, 0, w, h, null);
            g2.dispose();

            ret = tmp;
        } while (w != targetWidth || h != targetHeight);

        return ret;
    }

    public static FilenameFilter imageFilter() {
        return new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                String check = name.toLowerCase();
                return check.endsWith(".jpg") ||
                       check.endsWith(".jpeg") ||
                       check.endsWith(".tif") ||
                       check.endsWith(".tiff") ||
                       check.endsWith(".png") ||
                       check.endsWith(".gif") ||
                       check.endsWith(".pdf");
            }
        };
    }
}
