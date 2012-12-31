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

import java.awt.Polygon;
import java.awt.Shape;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.util.List;
import javax.media.jai.JAI;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RenderedOp;
import javax.media.jai.TiledImage;
import net.sourceforge.bookscanwizard.ColorOp;
import net.sourceforge.bookscanwizard.FileHolder;
import net.sourceforge.bookscanwizard.Operation;
import net.sourceforge.bookscanwizard.UserException;

/**
 * Whites out a section of the image.
 */
public class Whiteout extends Operation implements ColorOp {
    private ROI roi;

    @Override
    protected List<Operation> setup(List<Operation> operationList) throws Exception {
        double[] points = getScaledArgs();
        if (points.length < 2) {
            throw new UserException("Whiteout must have at least two coordinates");
        }
        int[] xPoints = new int[points.length/2];
        int[] yPoints = new int[points.length/2];
        for (int i=0; i < xPoints.length; i++) {
            xPoints[i] = (int) points[i*2];
            yPoints[i] = (int) points[i*2+1];
        }
        Shape polygon = new Polygon(xPoints, yPoints, xPoints.length);
        if (xPoints.length == 2) {
            polygon = polygon.getBounds();
        }
        roi = new ROIShape(polygon);
        return operationList;

    }
    @Override
    protected RenderedImage performOperation(FileHolder holder, RenderedImage img) throws Exception {
        TiledImage tiled = new TiledImage(img, true);

        // Create white image
        int bands = img.getColorModel().getNumComponents();
        Byte[] bandValues = new Byte[bands];
        for (int i=0; i < bands; i++) {
            bandValues[i] = (byte) 255;
        }
        ParameterBlock pb = new ParameterBlock();
        pb.add((float) roi.getBounds().width);
        pb.add((float) roi.getBounds().height);
        pb.add(bandValues);
        RenderedOp whiteImage = JAI.create("constant", pb);

        pb = new ParameterBlock();
        pb.addSource(whiteImage);
        pb.add((float) roi.getBounds().x);
        pb.add((float) roi.getBounds().y);
        whiteImage = JAI.create("translate", pb);

        // combine the two images.
        tiled.setData(whiteImage.getData(), roi);
        return tiled;
    }
}
