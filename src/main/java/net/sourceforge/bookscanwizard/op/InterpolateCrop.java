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

import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.util.Arrays;
import java.util.List;
import javax.media.jai.JAI;
import net.sourceforge.bookscanwizard.BSW;
import net.sourceforge.bookscanwizard.FileHolder;
import net.sourceforge.bookscanwizard.Operation;
import net.sourceforge.bookscanwizard.PageSet;
import net.sourceforge.bookscanwizard.PerspectiveOp;
import net.sourceforge.bookscanwizard.UserException;
import net.sourceforge.bookscanwizard.util.Interpolate;

/**
 */
public class InterpolateCrop extends Operation implements PerspectiveOp {
    private int x0;
    private int x1;
    private Rectangle2D r1;
    private Rectangle2D r2;

    @Override
    public List<Operation> setup(List<Operation> operationList) throws Exception {
        List<String> args = Arrays.asList(getTextArgs());
        if (args.size() != 10) {
            throw new UserException("Invalid arguments: "+arguments+" " +args.size());
        }
        x0 = getPos(pageSet, args.get(0));
        x1 = getPos(pageSet, args.get(5));
        r1 = new Rectangle2D.Double();
        r1.setFrameFromDiagonal(scaled(args.get(1)), scaled(args.get(2)), scaled(args.get(3)), scaled(args.get(4)));
        r2 = new Rectangle2D.Double();
        r2.setFrameFromDiagonal(scaled(args.get(6)), scaled(args.get(7)), scaled(args.get(8)), scaled(args.get(9)));
        return operationList;
    }

    @Override
    public RenderedImage performOperation(FileHolder holder, RenderedImage img) {
        Rectangle2D rect = interpolate(getPos(getPageSet(), holder.getName()));
        try {
            // Crop the image.
            ParameterBlock pb = new ParameterBlock();
            pb.addSource(img);
            pb.add((float) rect.getMinX());
            pb.add((float) rect.getMinY());
            pb.add((float) rect.getWidth());
            pb.add((float) rect.getHeight());
            return JAI.create("crop", pb);
        } catch (RuntimeException e) {
            throw new UserException(e.toString()+" for "+arguments, e);
        }
    }

    public double interpolate(double pos, double limit1, double limit2) {
        return Interpolate.interpolate(pos, x0, limit1, x1, limit2);
    }

    private int getPos(PageSet pageSet, String fileName) {
        List<FileHolder> holders = pageSet.getFileHolders();
        for (int i=0; i < holders.size(); i++) {
            if (holders.get(i).getName().equals(fileName)) {
                return i;
            }
        }
        throw new UserException("Could not file "+fileName);
    }

    private double scaled(String arg) {
        return Double.parseDouble(arg) * BSW.getPreviewScale();
    }

    private Rectangle2D interpolate(int pos) {
        double x = interpolate(pos, r1.getX(), r2.getX());
        double y = interpolate(pos, r1.getY(), r2.getY());
        double width = interpolate(pos, r1.getWidth(), r2.getWidth());
        double height = interpolate(pos, r1.getHeight(), r2.getHeight());
        return new Rectangle2D.Double(x, y, width, height);
    }
}
