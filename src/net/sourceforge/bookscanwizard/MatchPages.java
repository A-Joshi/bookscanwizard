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

package net.sourceforge.bookscanwizard;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sourceforge.bookscanwizard.qr.QRData;
import net.sourceforge.bookscanwizard.util.Sequence;
import net.sourceforge.bookscanwizard.util.Utils;

/**
 * This class tries to take a more intelligent approach for matching pages.
 * Simply putting the files in date order can cause problem if a page is missing,
 * as not only is the page missing but it causes the remainder pages to be treated
 * as being in the wrong position.  There is also a problem if test pictures
 * are included before the scanning starts.
 *
 * So instead, we assume that all correct pages will have timestamps within two
 * seconds of each other.  This will match up the pages that happened at the
 * same time and any problem files will be marked as such and saved to the end
 * of the list.
 *
 * This will also handle shots from cameras whose cameras are not synchronized,
 * as this will first compare the times from the last two photos from each
 * camera, and will use determine the offset from those.  For this to work correctly,
 * the last pictures taken should be done at the same time.
 *
 * To use this, create a l and an r directory underneath the configure directory,
 * and put the images from the left and right cameras in the right spots.
 */
public class MatchPages {
    private static final Logger logger = Logger.getLogger(MatchPages.class.getName());

    private List<FileHolder> pages;

    public MatchPages(File inputPath, Sequence sequence, boolean ignoreTime) throws IOException {
        ArrayList<FileHolder> retVal = new ArrayList<FileHolder>();
        Sequence problemSeq = new Sequence("_###");
        File leftPath = new File(inputPath, "l");
        File rightPath = new File(inputPath, "r");
        File[] leftFiles;
        File[] rightFiles;
        if (!leftPath.isDirectory() || !rightPath.isDirectory()) {
            leftPath = inputPath;
            rightPath = inputPath;
            ignoreTime = true;
        }
        if (inputPath.isFile()) {
            leftFiles = new File[] {inputPath};
            rightFiles = new File[] {inputPath};
        } else {
            leftFiles = leftPath.listFiles(Utils.imageFilter());
            rightFiles = rightPath.listFiles(Utils.imageFilter());
        }
        if (leftFiles == null || leftFiles.length == 0) {
            throw new UserException("Could not find folder "+leftPath);
        }
        if (rightFiles == null|| rightFiles.length == 0) {
            throw new UserException("Could not find folder "+rightPath);
        }
        Map<String,List<QRData>> leftBarcodes = QRData.read(new File(leftPath, "barcodes.csv"));
        Map<String,List<QRData>> rightBarcodes = QRData.read(new File(rightPath, "barcodes.csv"));

        Arrays.sort(leftFiles);
        Arrays.sort(rightFiles);
        long offset = leftFiles[leftFiles.length -1].lastModified() -rightFiles[rightFiles.length -1].lastModified();
        if (leftFiles.length > 1 && rightFiles.length > 1) {
            long offset2 = leftFiles[leftFiles.length -2].lastModified() -rightFiles[rightFiles.length -2].lastModified();
            offset = Math.min(offset, offset2);
        }
        logger.log(Level.FINE, "Using offset of {0} ms.", offset);

        // validate dates.
        int i = 0;
        for (File left : leftFiles) {
            while (true) {
                if (i >= rightFiles.length) {
                    FileHolder holder = new FileHolder(left, "z_"+sequence.last()+problemSeq.next(), leftBarcodes.get(left.getName()));
                    holder.setPosition(FileHolder.LEFT);
                    retVal.add(holder);
                    i++;
                    break;
                } else {
                    File right = rightFiles[i];
                    if (ignoreTime || Math.abs(left.lastModified() - right.lastModified() - offset) <= 2000) {
                        FileHolder holder;
                        holder = new FileHolder(left, sequence.next(), leftBarcodes.get(left.getName()));
                        holder.setPosition(FileHolder.LEFT);
                        retVal.add(holder);
                        holder = new FileHolder(right, sequence.next(),rightBarcodes.get(right.getName()));
                        holder.setPosition(FileHolder.RIGHT);
                        retVal.add(holder);
                        i++;
                        break;
                    } else {
                        if (left.lastModified() < right.lastModified() + offset) {
                            FileHolder holder = new FileHolder(left, "z_"+sequence.last()+problemSeq.next(), leftBarcodes.get(left.getName()));
                            holder.setPosition(FileHolder.LEFT);
                            retVal.add(holder);
                            break;
                        } else {
                            FileHolder holder = new FileHolder(right, "z_"+sequence.last()+problemSeq.next(),rightBarcodes.get(right.getName()));
                            holder.setPosition(FileHolder.RIGHT);
                            retVal.add(holder);
                            i++;
                        }
                    }
                }
            }
        }
        for (; i < rightFiles.length; i++) {
            FileHolder holder = new FileHolder(rightFiles[i], "z_"+sequence.last()+problemSeq.next(), rightBarcodes.get(rightFiles[i].getName()));
            holder.setPosition(FileHolder.RIGHT);
            retVal.add(holder);
        }
        Collections.sort(retVal);
        pages = retVal;
    }

    /**
     * Returns a set of FileHolders which combine the left and right directories
     * @param inputPath
     * @param sequence
     * @return
     */
    public List<FileHolder>getAllPages() {
        return pages;
    }

    /**
     * Test method.
     */
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println(
                "Usage:  [-move] sourceFiles destination\n"+
                "Will display the old and new names, or if -move is given it will " +
                "perform the move");
        }
        boolean move = false;
        int pos = 0;
        if (args[0].equals("-move"))  {
            pos++;
            move= true;
        }
        if (move) {
            new File(args[pos+1]).mkdirs();
        }
        MatchPages matchPages = new MatchPages(BSW.getFileFromCurrentDir(args[pos]), new Sequence("BSW_####", 0, 1), false);
        for (FileHolder holder : matchPages.getAllPages()) {
            File dest = new File(BSW.getFileFromCurrentDir(args[pos+1]), holder.getName()+".jpg");
            if (move) {
                holder.getFile().renameTo(dest);
            } else {
                System.out.println("mv "+holder.getFile()+" "+dest);
            }
        }
    }
}
