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

/**
 * Crops a picture, then scales it.  This is intended to adjust the crop
 * to a previous crop that needs the scaling adjusted.
 */
public class CropAndScale extends Crop {
    @Override
    public RenderedImage performOperation(FileHolder holder, RenderedImage img) throws Exception {
        img = super.performOperation(holder, img);
        return Scale.qualityScale(img, (float) getArgs()[5], (float) getArgs()[6]);
    }
}
