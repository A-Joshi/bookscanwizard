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

import java.awt.geom.Point2D;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.util.List;
import javax.media.jai.InterpolationBilinear;
import javax.media.jai.JAI;
import javax.media.jai.PerspectiveTransform;
import javax.media.jai.WarpPerspective;
import net.sourceforge.bookscanwizard.BSW;
import net.sourceforge.bookscanwizard.DpiSetter;
import net.sourceforge.bookscanwizard.FileHolder;
import net.sourceforge.bookscanwizard.Operation;
import net.sourceforge.bookscanwizard.PerspectiveOp;
import net.sourceforge.bookscanwizard.UserException;
import net.sourceforge.bookscanwizard.util.Utils;

/**
 * Assuming that 4 coordinates are defined, starting at the top left, and
 * going clockwise.
 */
public class Perspective extends Operation implements PerspectiveOp, DpiSetter {
    private PerspectiveTransform tr;
    private Point2D[] points;
    private Point2D[] enteredPoints;
    private float dpi;

    @Override
    protected List<Operation> setup(List<Operation> operationList) throws Exception {
        double[] p = getScaledArgs();
        if (p.length < 8) {
            throw new UserException("There must be at least 8 parameters for PerspectiveCorrection");
        }
        double dx1 = Point2D.distance(p[0], p[1], p[2], p[3]);
        double dx2 = Point2D.distance(p[6], p[7], p[4], p[5]);
        double dx = (dx1 + dx2) / 2;
        double dy1 = Point2D.distance(p[0], p[1], p[6], p[7]);
        double dy2 = Point2D.distance(p[2], p[3], p[4], p[5]);
        double dy = (dy1 + dy2) / 2;

        enteredPoints = new Point2D.Double[4];
        enteredPoints[0] = new Point2D.Double(p[0], p[1]);
        enteredPoints[1] = new Point2D.Double(p[2], p[3]);
        enteredPoints[2] = new Point2D.Double(p[4], p[5]);
        enteredPoints[3] = new Point2D.Double(p[6], p[7]);

        points = new Point2D.Double[4];
        points[0] = new Point2D.Double(p[0], p[1]);
        points[1] = new Point2D.Double(p[0] + dx, p[1]);
        points[2] = new Point2D.Double(p[0] + dx, p[1] + dy);
        points[3] = new Point2D.Double(p[0], p[1] + dy);

        tr = PerspectiveTransform.getQuadToQuad(
                points[0].getX(), points[0].getY(), points[1].getX(), points[1].getY(),
                points[2].getX(), points[2].getY(), points[3].getX(), points[3].getY(),
                p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7]
                );
        if (p.length > 9) {
            double adj = (dx / dy) / (getArgs()[8] / getArgs()[9]);
            tr.scale(adj, 1);
            dpi = (float) (dx / getArgs()[8] / pageSet.getPreviewScale());
        }
        return operationList;
    }

    @Override
    public RenderedImage previewOperation(FileHolder holder, RenderedImage img) {
        BSW.instance().getMainFrame().getViewerPanel().setScaledPoints(enteredPoints);
        BSW.instance().getMainFrame().getViewerPanel().setPreviewTransform(tr);
        return img;
    }

    @Override
    public RenderedImage performOperation(FileHolder holder, RenderedImage img) {
        return warpAndCrop(holder, img, false);
    }

    protected RenderedImage warpAndCrop(FileHolder holder, RenderedImage img, boolean crop) {
        if (dpi > 0) {
            holder.setDPI(dpi);
        }
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(img);
        pb.add(new WarpPerspective(tr));
        pb.add(new InterpolationBilinear());
        img =  JAI.create("warp", pb);
        if (crop) {
            // Crop the image.
            pb = new ParameterBlock();
            pb.addSource(img);
            pb.add((float) points[0].getX());
            pb.add((float) points[0].getY());
            pb.add((float) (points[2].getX() - points[0].getX()));
            pb.add((float) (points[2].getY() - points[0].getY()));
            img=JAI.create("crop", pb);
        }
        return img;
    }

    public float getDPI() {
        return dpi;
    }
}
