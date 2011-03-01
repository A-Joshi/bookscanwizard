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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import net.sourceforge.bookscanwizard.BSW;
import net.sourceforge.bookscanwizard.FileHolder;
import net.sourceforge.bookscanwizard.Operation;
import net.sourceforge.bookscanwizard.PageSet;
import net.sourceforge.bookscanwizard.UserException;
import net.sourceforge.bookscanwizard.qr.QRData;
import net.sourceforge.bookscanwizard.util.Utils;

/**
 * Defines a directory that the images should be loaded from.
 */
public class LoadImages extends Operation {

    @Override
    public List<Operation> setup(List<Operation> operationList) throws Exception {
        File sourceDir = BSW.getFileFromCurrentDir(arguments);
        PageSet.setSourceDir(sourceDir);

        Map<String,List<QRData>> qrData = QRData.read(new File(sourceDir, "barcodes.csv"));
        File[] files = BSW.getFileFromCurrentDir(arguments).listFiles(Utils.imageFilter());
        if (files == null || files.length == 0) {
            throw new UserException("Could not find any files in "+sourceDir);
        }
        Arrays.sort(files);
        ArrayList<FileHolder> holders = new ArrayList<FileHolder>(files.length);
        boolean odd = true;
        for (File f : files) {
            FileHolder holder = new FileHolder(f, qrData.get(f.getName()));
            holder.setPosition(odd ? FileHolder.LEFT : FileHolder.RIGHT);
            holders.add(holder);
            odd = !odd;
        }
        pageSet.setSourceFiles(holders);
        ArrayList<Operation> list = new ArrayList<Operation>();
        list.addAll(operationList);
        list.addAll(getOperation("Pages= -", null, pageSet));
        return list;
    }
}
