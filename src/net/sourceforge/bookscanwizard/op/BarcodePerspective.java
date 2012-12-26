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
import java.util.List;
import javax.media.jai.InterpolationBilinear;
import javax.media.jai.JAI;
import javax.media.jai.PerspectiveTransform;
import javax.media.jai.WarpPerspective;
import net.sourceforge.bookscanwizard.BSW;
import net.sourceforge.bookscanwizard.FileHolder;
import net.sourceforge.bookscanwizard.Operation;
import net.sourceforge.bookscanwizard.PerspectiveOp;
/*
 * Based on code from the following:
 *
 *  Copyright 2011 Robert Baruch (robert.c.baruch@gmail.com).
 *  Support at diybookscanner.org.
 *
 *  This file is part of QR Perspective Correction.
 *
 *  QR Perspective Correction is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  QR Perspective Correction is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with QR Perspective Correction.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Transforms an image to a square, normally defined by qrcodes.
 */
public class BarcodePerspective extends Operation implements PerspectiveOp {
    private Point2D.Double[] enteredPoints;
    private PerspectiveTransform perspectiveTransform;

    @Override
    protected RenderedImage previewOperation(FileHolder holder, RenderedImage img) {
        BSW.instance().getMainFrame().getViewerPanel().setScaledPoints(enteredPoints);
        BSW.instance().getMainFrame().getViewerPanel().setPreviewTransform(perspectiveTransform);
        return img;
    }

    @Override
    protected List<Operation> setup(List<Operation> operationList) throws Exception {
        perspectiveTransform = toTransform(getScaledArgs());
        return operationList;
    }

    @Override
    public RenderedImage performOperation(FileHolder holder, RenderedImage img) {
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(img);
        pb.add(new WarpPerspective(perspectiveTransform));
        pb.add(new InterpolationBilinear());
        img =  JAI.create("warp", pb);
        return img;
    }

    public PerspectiveTransform toTransform(double[] args) {
                    int pos = 0;
            double s_TLx = args[pos++];
            double s_TLy = args[pos++];
            double s_TRx = args[pos++];
            double s_TRy = args[pos++];
            double s_BRx = args[pos++];
            double s_BRy = args[pos++];
            double s_BLx = args[pos++];
            double s_BLy = args[pos++];

            double side = (float) Math.sqrt((s_TRx - s_TLx) * (s_TRx - s_TLx) + (s_TRy - s_TLy) * (s_TRy - s_TLy));

            double d_TLx = s_TLx;
            double d_TLy = s_TLy;
            double d_TRx = d_TLx + side;
            double d_TRy = d_TLy;
            double d_BLx = d_TLx;
            double d_BLy = d_TLy + side;
            double d_BRx = d_TLx + side;
            double d_BRy = d_TLy + side;

            enteredPoints = new Point2D.Double[4];
            enteredPoints[0] = new Point2D.Double(args[0], args[1]);
            enteredPoints[1] = new Point2D.Double(args[2], args[3]);
            enteredPoints[2] = new Point2D.Double(args[4], args[5]);
            enteredPoints[3] = new Point2D.Double(args[6], args[7]);
            
            return PerspectiveTransform.getQuadToQuad(
                    d_TLx, d_TLy, d_TRx, d_TRy, d_BRx, d_BRy, d_BLx, d_BLy,
                    s_TLx, s_TLy, s_TRx, s_TRy, s_BRx, s_BRy, s_BLx, s_BLy);

    }
}
