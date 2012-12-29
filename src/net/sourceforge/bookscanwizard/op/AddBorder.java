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
import java.awt.image.renderable.ParameterBlock;
import java.util.List;
import javax.media.jai.BorderExtenderConstant;
import javax.media.jai.JAI;
import net.sourceforge.bookscanwizard.FileHolder;
import net.sourceforge.bookscanwizard.Operation;
import net.sourceforge.bookscanwizard.UserException;

/**
 * Adds a white border around the image.
 */
public class AddBorder extends Operation  {
    private int left;
    private int right;
    private int top;
    private int bottom;
    
    @Override
    public List<Operation> setup(List<Operation> operationList) throws Exception {
        double[] args =  getArgs();
        if (args.length == 1) {
            left = right= top = bottom = (int) args[0];
        } else if (args.length == 4) {
            left = (int) args[0];
            right = (int) args[1];
            top = (int) args[2];
            bottom = (int) args[3];
        } else {
            throw new UserException ("There must be 1 or 4 argsments to AddBorder");
        }
        return operationList;
    }

    @Override
    public RenderedImage performOperation(FileHolder holder, RenderedImage img) throws Exception {
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(img);
        pb.add(left);
        pb.add(right);
        pb.add(top);
        pb.add(bottom);
        int numbands = img.getSampleModel().getNumBands();
        double[] fillValue = new double[numbands];
        for (int i = 0; i < numbands; i++) {
            fillValue[i] = 255;
        }
        pb.add(new BorderExtenderConstant(fillValue));
        return JAI.create("border", pb);
    }
}
