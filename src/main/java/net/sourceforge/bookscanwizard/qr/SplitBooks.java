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

package net.sourceforge.bookscanwizard.qr;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import net.sourceforge.bookscanwizard.util.LazyHashMap;

public class SplitBooks {
    private static final SimpleDateFormat df = new SimpleDateFormat("yyMMddHHmmss");

    public static void main(String[] args) throws Exception {
        ArrayList<String> mainArgs = new ArrayList<>();
        float scale = 1;

        for (int i=0; i < args.length; i++) {
            switch (args[i]) {
                case "-scale":
                    i++;
                    scale = Float.parseFloat(args[i]);
                    break;
                case "-threshold":
                    i++;
                    ReadCodes.setThreshold(Double.parseDouble(args[i]));
                    break;
                default:
                    mainArgs.add(args[i]);
                    break;
            }
        }
        if (mainArgs.size() < 2) {
            System.err.println("Invalid arguments to -split");
            System.exit(1);
        }
        File destination = new File(mainArgs.get(1));
        String subdir = null;
        if (mainArgs.size() > 2) {
            subdir = mainArgs.get(2);
        }
        SplitBooks splitBooks = new SplitBooks(args[0], destination, subdir, scale);
    }
    
    public SplitBooks(String source, File destination, String subdir, float scale) throws Exception {
        ReadCodes readCodes = new ReadCodes(source, scale);
        Collection<File> files = readCodes.getFiles();
        LazyHashMap<String, List<File>> books = new LazyHashMap<>(ArrayList.class);
        LazyHashMap<String, List<QRData>> bookCodes = new LazyHashMap<>(ArrayList.class);

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
