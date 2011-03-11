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

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import net.sourceforge.bookscanwizard.DpiSetter;
import net.sourceforge.bookscanwizard.FileHolder;
import net.sourceforge.bookscanwizard.util.JpegMetaData;
import net.sourceforge.bookscanwizard.Operation;
import net.sourceforge.bookscanwizard.UserException;

/**
 * This examines the focal from the jpeg exif metadata, and uses that
 * information to estimate the source DPI of the image.  This method assumes
 * that the distance to page remains the same, and the FocalLength accurately
 * represents the relative zoom level.
 */
public class EstimateDPI extends Operation {
    private static final String INVALID = "There must be two different zoom levels for both the right and left cameras to use this feature";
    private double x0;
    private double y0;
    private double x1;
    private double y1;

    private static float[] dpiInfo;

    @Override
    public List<Operation> setup(List<Operation> operationList) throws Exception {
        double[] p = getArgs();
        x0 = p[0];
        y0 = p[1];
        x1 = p[2];
        y1 = p[3];
        return operationList;
    }

    @Override
    protected RenderedImage performOperation(FileHolder holder, RenderedImage img) throws Exception {
        holder.setDPI((float) calculateDPI(holder.getFile()));
        return img;
    }

    private double calculateDPI(double x) {
        // interpolate result
        return ((x - x0) * y1 + (x1 - x) * y0) / (x1 - x0);
    }

    /**
     * Estimates the DPI of a file by examining the focal length
     *
     * @param file the file to estimate
     * @return the estimate dpi
     */
    private double calculateDPI(File file) throws IOException {
        double focalLength = focalLength(file);
        return calculateDPI(focalLength);
    }

    private static double focalLength(File file) throws IOException {
        JpegMetaData metaData = new JpegMetaData(file);
        String fl = metaData.getValue("FocalLength");
        int pos = fl.indexOf("/");
        double focalLength;
        if (pos < 0) {
            focalLength = Double.parseDouble(fl);
        } else {
            focalLength = Double.parseDouble(fl.substring(0, pos)) /
                          Double.parseDouble(fl.substring(pos + 1));
        }
        return focalLength;
    }

    public static float[] getInfo() {
        return dpiInfo;
    }

    public static void setInfo(float[] dpiInfo) {
        EstimateDPI.dpiInfo = dpiInfo;
    }

    public static String getConfig() {
        StringBuilder str = new StringBuilder();
        if (dpiInfo != null) {
            if (dpiInfo.length == 4) {
                str.append("Pages = all\n");
                str.append("EstimateDPI = "+dpiInfo[0]+","+dpiInfo[1]+", "+dpiInfo[2]+","+dpiInfo[3]+"\n");
            } if (dpiInfo.length == 8) {
                str.append("Pages = left\n");
                str.append("EstimateDPI = "+dpiInfo[0]+","+dpiInfo[1]+", "+dpiInfo[2]+","+dpiInfo[3]+"\n");
                str.append("Pages = right\n");
                str.append("EstimateDPI = "+dpiInfo[4]+","+dpiInfo[5]+", "+dpiInfo[6]+","+dpiInfo[7]+"\n");
            }
        }
        return str.toString();
    }

    public static void saveFocalLength() throws IOException {
        TreeMap<Float,Float> left = new TreeMap<Float,Float>();
        TreeMap<Float,Float> right = new TreeMap<Float,Float>();
        HashMap<Integer,Float> currentDPI = new HashMap<Integer,Float>();
        for (Operation op : getAllOperations()) {
            if (op instanceof DpiSetter) {
                float dpi = ((DpiSetter) op).getDPI();
                for (FileHolder holder : op.getPageSet().getFileHolders()) {
                    Float previous = currentDPI.get(holder.getPosition());
                    if (previous == null || dpi != previous) {
                        TreeMap<Float,Float> side = (holder.getPosition() == FileHolder.LEFT ? left : right);
                        if (op instanceof EstimateDPI) {
                            EstimateDPI est = (EstimateDPI) op;
                            side.put((float) est.y0, (float) est.x0);
                            side.put((float) est.y1, (float) est.x1);
                        } else {
                            float fl = (float) focalLength(holder.getFile());
                            side.put(dpi, fl);
                        }
                    }
                }
            }
        }

        if (left.size() < 2 || right.size() < 2) {
            throw new UserException(INVALID);
        }
        ArrayList<Float> info = new ArrayList<Float>();
        info.add(left.firstEntry().getValue());
        info.add(left.firstEntry().getKey());
        info.add(left.lastEntry().getValue());
        info.add(left.lastEntry().getKey());
        if (left.firstEntry().getValue() == left.lastEntry().getValue()) {
            throw new UserException(INVALID);
        }
        if (!left.equals(right)) {
            info.add(right.firstEntry().getValue());
            info.add(right.firstEntry().getKey());
            info.add(right.lastEntry().getValue());
            info.add(right.lastEntry().getKey());
            if (right.firstEntry().getValue() == right.lastEntry().getValue()) {
                throw new UserException(INVALID);
            }
        }
        dpiInfo = new float[info.size()];
        for (int i=0; i < dpiInfo.length; i++) {
            dpiInfo[i] = info.get(i);
        }
    }
}
