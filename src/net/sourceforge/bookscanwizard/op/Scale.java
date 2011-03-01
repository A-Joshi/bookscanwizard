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
import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import net.sourceforge.bookscanwizard.BSW;
import net.sourceforge.bookscanwizard.FileHolder;
import net.sourceforge.bookscanwizard.Operation;

/**
 * Scales an image. Normally this isn't needed since saving a file will scale
 * it to the value that matches the DPI.
 */
public class Scale extends Operation {
    @Override
    public RenderedImage performOperation(FileHolder holder, RenderedImage img) throws Exception {
        float xScale = (float) getArgs()[0];
        float yScale;
        if (getArgs().length > 1) {
            yScale = (float) getArgs()[1];
        } else {
            yScale = xScale;
        }
        return qualityScale(img, xScale, yScale);
    }
    
    /**
     * This renders an image in a quality way, using interpolation for
     * values greater than 1, or subsample averaging for values less than 1
     */
    public static RenderedImage qualityScale(RenderedImage img, float xScale, float yScale)  {
        if (Float.isNaN(xScale) || Float.isNaN(yScale)) {
            throw new IllegalArgumentException();
        }
        if (xScale >= 1 && yScale >= 1) {
            ParameterBlock pb = new ParameterBlock()
                .addSource(img).add(xScale).add(yScale)
                .add(0F).add(0F).
                add(Interpolation.getInstance(Interpolation.INTERP_BILINEAR));
            img = JAI.create("scale", pb, BSW.QUALITY_HINTS);
        } else if (xScale <= 1 && yScale <= 1) {
            RenderingHints scaleHints = new RenderingHints(null);
            scaleHints.add(BSW.QUALITY_HINTS);
            // This gets around a bug where jai doesn't seem to want to save
            // this operation to the cache.
            scaleHints.put(JAI.KEY_TILE_CACHE, BSW.getTileCache());

            // There seems to be a problem where sometimes black lines appear
            // across the image.  so we get around that by ensuring the entire
            // operation is done as a single tile.
            int tileWidth = (int) (img.getWidth() * xScale + 1);
            int tileHeight = (int) (img.getHeight() * xScale + 1);
            ImageLayout tileLayout = new ImageLayout(img);
            tileLayout.setTileWidth(tileWidth);
            tileLayout.setTileHeight(tileHeight);
            scaleHints.put(JAI.KEY_IMAGE_LAYOUT, tileLayout);

            ParameterBlock pb = new ParameterBlock();
            pb.addSource(img);
            pb.add((double) xScale);
            pb.add((double) yScale);
            img = JAI.create("SubsampleAverage", pb, scaleHints);
        } else {
            img = qualityScale(img, xScale, 1);
            img = qualityScale(img, 1, yScale);
        }
        return img;
    }
}
