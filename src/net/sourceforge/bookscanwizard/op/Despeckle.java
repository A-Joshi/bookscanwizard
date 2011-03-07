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
import net.sourceforge.bookscanwizard.ColorOp;
import net.sourceforge.bookscanwizard.FileHolder;
import net.sourceforge.bookscanwizard.Operation;

/**
 * Based on:
 * http://www.fmwconcepts.com/imagemagick/isonoise/index.php
 *What the script does is as follows:

    * Performs median filtering on the image
    * Computes the difference image between the noisy and median filtered images.
    * Thresholds the difference image to try to locate the areas of noise.
    * Composites the original image and the median filtered image
      such that the result using the median filtered image where
      there is noise and the original image where there is no noise.

This is equivalent to the following IM commands:

    * convert $infile -median $radius $tmp0
    * convert $infile $tmp0 -compose Difference -composite -threshold $thresh% $tmp1
    * convert $infile $tmp0 $tmp1 -compose src -composite $outfile

 */
public class Despeckle extends Operation implements ColorOp {

    @Override
    public RenderedImage performOperation(FileHolder holder, RenderedImage img) {
        return img;
    }
}
