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
import net.sourceforge.bookscanwizard.FileHolder;
import net.sourceforge.bookscanwizard.Operation;

/*
 * This scales all images to the size of the first image.  It can be used
 * to correct for differences between the zoom levels for the left & right sides.
 *
 */
public class ScaleToFirst extends Operation {
    private float width;
    private float height;

    @Override
    protected RenderedImage performOperation(FileHolder holder, RenderedImage img) throws Exception {
        if (width == 0) {
            width = img.getWidth();
            height = img.getHeight();
        } else {
            if (img.getWidth() != width || img.getHeight() != height) {
                img = Scale.qualityScale(img, img.getWidth() / width, img.getHeight() / height);
            }
        }
        return img;
    }
}

