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
import java.awt.Transparency;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.io.IOException;
import net.sourceforge.bookscanwizard.Operation;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import javax.imageio.ImageIO;
import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import net.sourceforge.bookscanwizard.ColorOp;
import net.sourceforge.bookscanwizard.FileHolder;
import net.sourceforge.bookscanwizard.util.FixedIHSColorSpace;
import net.sourceforge.bookscanwizard.util.Utils;

/**
 * Increate or decrease the Brightness of the images.  If one number is given,
 * it will increase all channels by that amount.  If three numbers are given,
 * then it will do the change by channel.
 */
public class Saturation extends Operation implements ColorOp {

    @Override
    public RenderedImage performOperation(FileHolder holder, RenderedImage img) {
        double[] values =getArgs();
        return changeSaturation(img, values[0]);
    }


    public static void main(String[] args) throws IOException {
        RenderedImage img = ImageIO.read(new File("c:/books/done/fairy/l/IMG_0001.JPG"));
        img = changeSaturation(img, 1);
        ImageIO.write(img, "jpg", new File("c:/test013/t.jpg"));
    }

    public static RenderedImage changeSaturation(RenderedImage img, double saturation) {
        ColorModel oldModel = img.getColorModel();
        img = Utils.renderedToBuffered(img);
        FixedIHSColorSpace ihs = FixedIHSColorSpace.getInstance();
        ColorModel ihsColorModel = new ComponentColorModel(ihs, new int[]{8, 8, 8}, false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(img);
        pb.add(ihsColorModel);
        RenderedImage ihsImage = JAI.create("colorconvert", pb);
        RenderedImage[] bands = new RenderedImage[3];
        for (int band = 0; band < 3; band++) {
            pb = new ParameterBlock();
            pb.addSource(ihsImage);
            pb.add(new int[]{band});
            bands[band] = JAI.create("bandselect", pb);
        }

        pb = new ParameterBlock();
        pb.addSource(bands[2]);
        pb.add(new double[]{saturation});
        RenderedImage newSaturation = JAI.create("multiplyconst", pb);
        
        ImageLayout imageLayout = new ImageLayout();
        imageLayout.setColorModel(ihsColorModel);
        imageLayout.setSampleModel(ihsImage.getSampleModel());
        RenderingHints rendHints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, imageLayout);
        pb = new ParameterBlock();
        pb.addSource(bands[0]);
        pb.addSource(bands[1]);
        pb.addSource(newSaturation);
        img = JAI.create("bandmerge", pb, rendHints);
        
        pb = new ParameterBlock();
        pb.addSource(img);
        pb.add(oldModel);
        return JAI.create("colorconvert", pb);
    }
}
