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

import com.sun.media.jai.codec.TIFFEncodeParam;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains information about a set of pages that share the same configuration
 */
public class PageSet {

    private static List<FileHolder> sourceFiles;
    private static File destinationDir;
    private static int destinationDPI;

    private List<FileHolder> fileHolders;
    private int compressionType;
    private PageSet parent;
    private String minFile;
    private String maxFile;

    private static File sourceDir;

    public PageSet() {
        // reset static values.
        sourceFiles = null;
        destinationDir = null;
        destinationDPI = 0;
        compressionType = TIFFEncodeParam.COMPRESSION_NONE;
        fileHolders = new ArrayList<FileHolder>();
    }

    public PageSet(PageSet parent) {
        this.parent = parent;
        fileHolders = null;
    }

    public int getCompressionType() {
        if (compressionType > 0) {
            return compressionType;
        } else {
            return parent.getCompressionType();
        }
    }

    public void setCompressionType(int compressionType) {
        this.compressionType = compressionType;
    }

    public static int getDestinationDPI() {
        return destinationDPI;
    }

    public static void setDestinationDPI(int destinationDPI) {
        PageSet.destinationDPI = destinationDPI;
    }
    
    public List<FileHolder> getFileHolders() {
        List<FileHolder> retVal = null;
        if (fileHolders != null) {
            retVal =  fileHolders;
        } else if (parent != null) {
            retVal = parent.getFileHolders();
        }
        if (retVal == null) {
            return null;
        }
        ArrayList<FileHolder> newList = new ArrayList<FileHolder>(retVal.size());
        for (FileHolder fh : retVal) {
            if ((getMinFile() == null || fh.getName().compareTo(getMinFile()) >= 0) &&
                (getMaxFile() == null || fh.getName().compareTo(getMaxFile()) < 0)) 
            {
                newList.add(fh);
            }
        }
        return newList;
    }

    public void setFileHolders(List<FileHolder> fileHolders) {
        this.fileHolders = fileHolders;
    }

    public static List<FileHolder> getSourceFiles() {
        return sourceFiles;
    }

    public void setSourceFiles(List<FileHolder> sourceFiles) {
        PageSet.sourceFiles = sourceFiles;
    }

    public File getDestinationDir() {
        if (destinationDir != null) {
            return destinationDir;
        } else {
            return parent.getDestinationDir();
        }
    }

    public void setDestinationDir(File destinationDir) {
        PageSet.destinationDir = destinationDir;
    }

    public double getPreviewScale() {
        return BSW.getPreviewScale();
    }

    public static File getSourceDir() {
        return sourceDir;
    }

    public static void setSourceDir(File sourceDir) {
        PageSet.sourceDir = sourceDir;
    }

    public String getMinFile() {
        return minFile;
    }

    public void setMinFile(String minFile) {
        this.minFile = minFile;
    }

    public String getMaxFile() {
        return maxFile;
    }

    public void setMaxFile(String maxFile) {
        this.maxFile = maxFile;
    }
}
