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

import java.awt.geom.Point2D;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import javax.media.jai.JAI;
import javax.media.jai.PerspectiveTransform;
import net.sourceforge.bookscanwizard.BSW;
import net.sourceforge.bookscanwizard.CropOp;
import net.sourceforge.bookscanwizard.FileHolder;
import net.sourceforge.bookscanwizard.Operation;
import net.sourceforge.bookscanwizard.UserException;
import net.sourceforge.bookscanwizard.gui.ViewerPanel;

/**
 * Crops a picture.  This should be passed two points: the upper right
 * and lower left corners.
 */
public class Crop extends Operation implements CropOp {
    @Override
    public RenderedImage previewOperation(FileHolder holder, RenderedImage img) {
        ViewerPanel viewer =  BSW.instance().getMainFrame().getViewerPanel();
        double[] args = getScaledArgs();
        Point2D[] pts = new Point2D[] {
            new Point2D.Double(args[0], args[1]),
            new Point2D.Double(args[2], args[3])
        };
        if (BSW.instance().getMainFrame().isShowPerspective()) {
            viewer.setScaledPoints(pts);
            viewer.setPreviewCrop(null);
        } else {
            PerspectiveTransform tr = viewer.getPreviewTransform();
            Point2D corners[] = new Point2D[] {
                new Point2D.Double(pts[0].getX(), pts[0].getY()),
                new Point2D.Double(pts[1].getX(), pts[0].getY()),
                new Point2D.Double(pts[1].getX(), pts[1].getY()),
                new Point2D.Double(pts[0].getX(), pts[1].getY())
            };
            if (pts == null) {
                throw new NullPointerException();
            }
            if (tr != null) {
                tr.transform(corners, 0, corners, 0, pts.length);
            }
            viewer.setPreviewCrop(corners);
        }
        return img;
    }

    @Override
    public RenderedImage performOperation(FileHolder holder, RenderedImage img) throws Exception {
        double[] args = getScaledArgs();
        if (args.length < 4) {
            throw new UserException("Invalid arguments: "+arguments);
        }
        try {
            // Crop the image.
            ParameterBlock pb = new ParameterBlock();
            pb.addSource(img);
            pb.add((float) args[0]);
            pb.add((float) args[1]);
            pb.add((float) (args[2] - args[0]));
            pb.add((float) (args[3] - args[1]));
            return JAI.create("crop", pb);
        } catch (RuntimeException e) {
            throw new UserException(e.toString()+" for "+arguments, e);
        }
    }
}
