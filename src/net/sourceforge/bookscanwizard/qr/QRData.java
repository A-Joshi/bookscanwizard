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

import com.google.zxing.ResultPoint;
import com.google.zxing.qrcode.decoder.Version;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.sourceforge.bookscanwizard.util.LazyHashMap;
import org.supercsv.io.CsvBeanReader;
import org.supercsv.io.CsvBeanWriter;
import org.supercsv.io.ICsvBeanReader;
import org.supercsv.io.ICsvBeanWriter;
import org.supercsv.prefs.CsvPreference;

/**
 * Contains data on a QR-code
 */
public class QRData {
    private static final String[] COLUMNS = new String[] {"fileName", "code", "points", "size"};
    private static boolean foundBarcodeFile;

    private String fileName;
    private String code;

    private float size;
    private Point2D[] points;

    public QRData() {}

    public QRData(String fileName, String text, ResultPoint[] points, Version v) {
        this.fileName = fileName;
        int pos = text.indexOf(",");
        this.size = Float.parseFloat(text.substring(0, pos));
        this.code = text.substring(pos + 1);
        this.points = new Point2D[points.length];

        for (int i =0; i < 3; i++) {
            this.points[i] = new Point2D.Float(points[i].getX(), points[i].getY());
        }
        // adjust the 4th marker to match the other ones.
        if (points.length == 4) {
            float segmentsBetweenMarkers = v.getDimensionForVersion() -7;
            // alignment pattern is 3 up from the other markers.
            float mult = segmentsBetweenMarkers / (segmentsBetweenMarkers - 3);
            float x = Math.round((points[3].getX()-points[1].getX()) * mult + points[1].getX());
            float y = Math.round((points[3].getY()-points[1].getY()) * mult + points[1].getY());
            this.points[3] = new Point2D.Float(x, y);
        }
        if (points.length > 4) {
            throw new IllegalArgumentException("Was expecting 3 or 4 points, found "+points.length);
        }
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getX() {
        return (int) points[0].getX();
    }

    public int getY() {
        return (int) points[0].getY();
    }

    public Point2D[] getPointsArray() {
        return points;
    }

    public String getPoints() {
        StringBuilder str = new StringBuilder();
        for (Point2D pt : points) {
            str.append(pt.getX()).append(",").append(pt.getY()).append(", ");
        }
        str.setLength(str.length()-2);
        return str.toString();
    }

    public void setPoints(String pts) {
        String[] str = pts.split(",");
        points = new Point2D.Float[str.length/2];
        for (int i=0; i < points.length; i++) {
            points[i] = new Point2D.Float(Float.parseFloat(str[i*2]), Float.parseFloat(str[i*2+1]));
        }
    }

    /**
     * Returns the size between the registration markers
     */
    public float getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = Float.parseFloat(size);
    }

    public static boolean isFoundBarcodeFile() {
        return foundBarcodeFile;
    }

    public double getDPIEstimate() {
        double sizeInches = ReadCodes.getBarcodeDimensions(code, (int) size) /72;
        double length = 0;
        length += points[0].distance(points[1]);
        length += points[1].distance(points[2]);
        length += points[2].distance(points[3]);
        length += points[3].distance(points[0]);
        return (length / 4) / sizeInches;
    }

    /**
     * Returns a list of barcodes to pages.
     * @param f
     * @return
     * @throws IOException
     */
    public static Map<String,List<QRData>> read(File f) throws IOException {
        LazyHashMap<String, List<QRData>> map = new LazyHashMap<>(ArrayList.class);
        foundBarcodeFile = f.isFile();
        if (foundBarcodeFile) {
            ICsvBeanReader reader = new CsvBeanReader(new FileReader(f), CsvPreference.EXCEL_PREFERENCE);
            while (true) {
                QRData data = reader.read(QRData.class, COLUMNS);
                if (data == null) {
                    break;
                }
                //TODO: remove file
                map.getOrCreate(new File(data.getFileName()).getName()).add(data);
            }
            reader.close();
        }
        return map;
    }
    
    @Override
    public String toString() {
        try {
            StringWriter str = new StringWriter();
            ICsvBeanWriter writer = new CsvBeanWriter(str, CsvPreference.EXCEL_PREFERENCE);
            writer.write(this, COLUMNS);
            writer.close();
            return str.toString();
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Saves a list of barcode text to a csv file
     *
     * @param f the file to save
     * @param data a list of the data to save
     * @throws IOException
     */
    public static void write(File f, List<QRData> data) throws IOException {
        ICsvBeanWriter writer = new CsvBeanWriter(new FileWriter(f), CsvPreference.EXCEL_PREFERENCE);
        for (QRData row : data) {
            writer.write(row, COLUMNS);
        }
        writer.close();
    }
}
