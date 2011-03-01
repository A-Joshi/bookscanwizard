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

package net.sourceforge.bookscanwizard.qr;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import net.sourceforge.bookscanwizard.util.LazyHashMap;

public class SplitBooks {
    private static final SimpleDateFormat df = new SimpleDateFormat("yyMMddHHmmss");

    public static void main(String[] args) throws Exception {
        ArrayList<String> mainArgs = new ArrayList<String>();
        float scale = 1;

        for (int i=0; i < args.length; i++) {
            if ("-scale".equals(args[i])) {
                i++;
                scale = Float.parseFloat(args[i]);
            } else if ("-threshold".equals(args[i])) {
                i++;
                ReadCodes.setThreshold(Double.parseDouble(args[i]));
            } else {
                mainArgs.add(args[i]);
            }
        }
        if (mainArgs.size() < 2) {
            System.err.println("Invalid arguments to -split");
            System.exit(1);
        }
        ReadCodes readCodes = new ReadCodes(mainArgs.get(0), scale);
        File destination = new File(mainArgs.get(1));
        String subdir = null;
        if (mainArgs.size() > 2) {
            subdir = mainArgs.get(2);
        }
        List<File> files = readCodes.getFiles();
        LazyHashMap<String, List<File>> books =
            new LazyHashMap<String, List<File>>(ArrayList.class);
        LazyHashMap<String, List<QRData>> bookCodes =
            new LazyHashMap<String, List<QRData>>(ArrayList.class);

        String title = null;
        String newTitle = null;
        boolean noBookBoundary = false;
        boolean isLeft = false;
        boolean foundBarcode = false;
        readCodes.getCodes();
        for (File f : files) {
            isLeft = !isLeft;
            List<QRData> data = readCodes.getCodes(f);
            for (QRData value : data) {
                if (value.getCode().equals(QRCodeControls.END.name())) {
                    foundBarcode = true;
                    // if we have saved any pages, start a new book, otherwise
                    // keep the old title
                    if (noBookBoundary) {
                        newTitle = null;
                    }
                } else if (value.getCode().startsWith("Title: ")) {
                    foundBarcode = true;
                    newTitle = value.getCode().substring("Title: ".length());
                }
            }
            if (!data.isEmpty()) {
                System.out.println();
            }
            if (isLeft) {
                if (foundBarcode) {
                    title = newTitle;
                    foundBarcode = false;
                } else {
                    noBookBoundary = true;
                }
            }
            if (title == null) {
                title = "Book_" + df.format(new Date(f.lastModified()));
            }
            books.getOrCreate(title).add(f);
            bookCodes.getOrCreate(title).addAll(data);
        }
        for (Map.Entry<String, List<File>> entry : books.entrySet()) {
            File newDir = new File(destination, entry.getKey());
            if (subdir != null) {
                newDir = new File(newDir, subdir);
            }
            newDir.mkdirs();
            for (File f : entry.getValue()) {
                f.renameTo(new File(newDir, f.getName()));
            }
            File barcodeFile = new File(newDir, "barcodes.csv");
            QRData.write(barcodeFile, bookCodes.get(entry.getKey()));
        }
    }
}
