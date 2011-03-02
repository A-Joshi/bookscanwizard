package net.sourceforge.bookscanwizard.unwarp;

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


import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import javax.imageio.ImageIO;
import javax.media.jai.InterpolationBilinear;
import javax.media.jai.JAI;
import javax.media.jai.Warp;
import javax.media.jai.WarpGrid;
import javax.media.jai.operator.TransposeDescriptor;
import net.sourceforge.bookscanwizard.WarpHeight;
import net.sourceforge.bookscanwizard.op.GaussianBlur;
import net.sourceforge.bookscanwizard.util.Interpolate;

/**
 * This is a proof of concept for unwarping based on a green laser line.
 */
public class LaserUnwarp {
    private static final boolean DEBUG = false;

    private static String base;
    /**
     * The sample size used in calculating angle distortion.
     */
    private static final int SAMPLE_SIZE = 10;

    /**
     * The minimum width of a green line to be considered a valid point
     */
    private static int MIN_WIDTH = 10;

    /**
     * The color of the line.  The green laser seems to be exactly green in
     * the RGB spectrum.
     */
    private static float GREEN_HUE = getHue(Color.green);

    /**
     *  The distance in inches from the camera to the base.
     */
    private float cameraDistance = 26;

    /**
     * The DPI measure calculated from the base.  It is currently hardcoded,
     * but could be calculated from a QR-code in the future.
     */
    private float dpi = 193;

    /**
     *  An interpolation for mapping a height to a 0-255 brightness value.
     */
    private Interpolate heightToBrightness;

    /**
     * The height map with a range that matches the heightToBrightess interpolation.
     */
    private RenderedImage heightImage;

    /**
     * The location of the crease between the two pages.
     */
    private WarpHeight heightWarp;

    private Warp angleWarp;


    public LaserUnwarp(RenderedImage img) throws IOException {
        // get the black & white image of the detected lines.
        img = scan(img);

        // rotate the image, because its a lot faster to scan through rows
        // than it is to scan columns.
        img = JAI.create("transpose", img, TransposeDescriptor.ROTATE_270);
        debugImage("height", img);

        // Finds the matching line positions for the image
        int[][] blips = getBlips(img);
        verifyBlips(blips);
        debugImage("centerline", debugCenterLineImage(img, blips));

        // Replace missing data with interpolated values.
        interpolateMissingLines(blips);
        debugImage("centerline2", debugCenterLineImage(img, blips));

        // Render a height map based on the height information.
        RenderedImage heightMap = calculateAllHeights(blips, img);
        debugImage("heightMap", heightMap);

        // Perform warp based on distance to the camera.
        img = heightWarp(img, heightMap);
        debugImage("heightWarp", heightMap);

        ParameterBlock pb2 = new ParameterBlock();
        pb2.addSource(heightMap);
        pb2.add(heightWarp);
        pb2.add(new InterpolationBilinear());
        heightMap = JAI.create("warp", pb2);
        heightImage = heightMap;
        angleWarp = calcAngleWarp(heightImage);
    }

    public RenderedImage unwarp(RenderedImage img) {
        img = JAI.create("transpose", img, TransposeDescriptor.ROTATE_270);
        ParameterBlock pb2 = new ParameterBlock();
        pb2.addSource(img);
        pb2.add(heightWarp);
        pb2.add(new InterpolationBilinear());
        img = JAI.create("warp", pb2);

        pb2 = new ParameterBlock();
        pb2.addSource(img);
        pb2.add(angleWarp);
        pb2.add(new InterpolationBilinear());
        img = JAI.create("warp", pb2);
        img = JAI.create("transpose", img, TransposeDescriptor.ROTATE_90);
        return img;
    }


    public static void main(String[] args) throws Exception {
        File input = new File("C:\\test\\update\\corrected");
        File output = new File("C:\\test\\update\\corrected\\processed");
        output.mkdirs();
        for (File f : input.listFiles()) {
            if (f.getName().toLowerCase().endsWith(".tif") && f.getName().contains("0880")) { //
                System.out.println(f.getName());
                processFile(f, output);
            }
        }
    }

    /**
     * Finds warping lines and saves the found lines and a height map image.
     */
    private static void processFile(File input, File destination) throws IOException {
        String baseName = new File(destination, input.getName()).getPath();
        baseName = baseName.substring(0, baseName.lastIndexOf("."));
        base = baseName;

        RenderedImage img = ImageIO.read(input);
        LaserUnwarp laserUnwarp = new LaserUnwarp(img);
        img = laserUnwarp.unwarp(img);

        File destFile = new File(baseName+".tif");
        System.out.println("dest: "+destFile);
        ImageIO.write(img, "tiff", destFile);
    }

    /**
     * Scans the image for pixels that match the laser line, returns a black
     * and white image of possible matches.
     */
    public final RenderedImage scan(RenderedImage image) {
        image = GaussianBlur.blur(image, 3, 2);
        Raster data = image.getData();
        byte[] b = new byte[]{-1, 0};
        ColorModel model = new IndexColorModel(1, 1, b, b, b);
        WritableRaster detectedRaster = model.createCompatibleWritableRaster(data.getWidth(), data.getHeight());
        int startX = data.getMinX();
        int startY = data.getMinY();
        int endX = data.getWidth() - startX;
        int endY = data.getHeight() - startY;
        int[] pixel = new int[3];
        int[] bitPixel = {1};
        for (int y = startY; y < endY; y++) {
            for (int x = startX; x < endX; x++) {
                data.getPixel(x, y, pixel);
                if (isColorGreen(pixel)) {
                    detectedRaster.setPixel(x, y, bitPixel);
                }
            }
        }
        BufferedImage newImage = new BufferedImage(model, detectedRaster, false, null);
        return newImage;
    }

    /**
     * Scans a column, looking for segments that could be part of the line.
     * It returns the thickest two lines, which seem to work well for the images
     * we currently have.
     */
    public int[] findBlips(RenderedImage img, int y) {
        Raster data = img.getData();
        int lastState = 0;
        int lastPos = 0;
        ArrayList<Line> list = new ArrayList<Line>();
        int width = img.getWidth();
        int[] pixel = new int[1];
        for (int i = 0; i < width; i++) {
            data.getPixel(i, y, pixel);
            if (pixel[0] != lastState) {
                if (lastState != 0) {
                    int len = i - lastPos - 1;
                    if (len > MIN_WIDTH) {
                        list.add(new Line(lastPos, i - 1));
                    }
                }
                lastState = pixel[0];
                lastPos = i;
            }
        }
        if (list.size() < 2) {
            return new int[0];
        } else {
            Collections.sort(list);
            int[] blip = new int[2];
            for (int i = 0; i < blip.length; i++) {
                blip[i] = list.get(i).getMidPoint();
            }
            Arrays.sort(blip);
            return blip;
        }
    }


    /**
     * Renders the blips as a thin line image.
     */
    private RenderedImage debugCenterLineImage(RenderedImage img, int[][] blips) {
        if (DEBUG) {
            return null;
        } else {
            WritableRaster raster = img.getColorModel().createCompatibleWritableRaster(img.getWidth(), img.getHeight());
            int[] pixel = {1};
            int height = img.getHeight();
            for (int y = 0; y < height; y++) {
                for (int i = 0; i < blips[y].length; i++) {
                    pixel[0] = 1;
                    raster.setPixel(blips[y][i], y, pixel);
                }
            }
            return new BufferedImage(img.getColorModel(), raster, false, null);
        }
    }

    /**
     * Scans the blips looking for center that are missing data, and
     * create interpolated versions of the data using the last valid point
     * and next valid point.
     */
    private void interpolateMissingLines(int[][] blips) {
        int starting = 0;
        while (blips[starting].length < 1) {
            starting++;
        }
        int[] last = blips[starting];
        int lastPos = - 1;
        boolean missing = starting > 0;
        for (int i = 0; i < blips.length; i++) {
            if (i + 1 == blips.length && blips[i].length == 0) {
                blips[i] = last;
            }
            if (missing) {
                if (blips[i].length > 0) {
                    // found a new one, so interpolate the missing data.
                    Interpolate intTop = new Interpolate(lastPos, last[0], i, blips[i][0]);
                    Interpolate intBottom = new Interpolate(lastPos, last[1], i, blips[i][1]);
                    for (int j = lastPos + 1; j < i; j++) {
                        blips[j] = new int[] {
                            (int) intTop.interpolate(j),
                            (int) intBottom.interpolate(j)
                        };
                    }
                }
            }
            if (blips[i].length > 0) {
                last = blips[i];
                lastPos = i;
                missing = false;
            } else {
                missing = true;
            }
        }
    }

    /**
     * Returns a height map image based on the found and interpolated line data.
     */
    private RenderedImage calculateAllHeights(int[][] blips, RenderedImage img) {
        final int imageWidth = img.getWidth();
        int sum = 0;
        for (int[] row : blips) {
            sum += row[0] + row[1];
        }
        float centerColumn = (float) sum /blips.length / 2;

        BufferedImage newImage = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster raster = newImage.getRaster();
        int[] pixel = new int[1];

        // determine min & max heights.
        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;
        for (int y = 0; y < blips.length; y++) {
            int[] row = blips[y];
            for (int x=0; x < imageWidth; x += imageWidth-1) {
                float z = ((imageWidth - x) * (centerColumn - row[0]) + x * (row[1] - centerColumn)) / imageWidth / dpi;
                min = Math.min(min, z);
                max = Math.max(max, z);
            }
        }
        if (min < 0) min = 0;
        heightToBrightness = new Interpolate(min, 0, max, 255);

        for (int y = 0; y < blips.length; y++) {
            int[] row = blips[y];
            for (int x=0; x < imageWidth; x++) {
                float z = ((imageWidth - x) * (centerColumn - row[0]) + x * (row[1] - centerColumn)) / imageWidth / dpi;
                if (z < 0) z = 0;
                int p = (int) heightToBrightness.interpolate(z);
                pixel[0] = p;
                raster.setPixel(x, y, pixel);
            }
        }
        return newImage;
    }

    /**
     * Scans each line of an image and returns the valid two line positions.
     */
    private int[][] getBlips(RenderedImage img) {
        int height = img.getHeight();
        int[][] blips = new int[height][];
        for (int y = 0; y < height; y++) {
            blips[y] = findBlips(img, y);
        }
        return blips;
    }

    /**
     * Toss out blips that aren't in the right spots.  One blip should be above
     * the center line and the other blip should be below it.
     */
    private void verifyBlips(int[][] blips) {
        int sum = 0;
        int ct = 0;
        for (int[] row : blips) {
            if (row.length > 0) {
                ct++;
                sum += row[0] + row[1];
            }
        }
        double center = sum / ct / 2;

        for (int i=1; i < blips.length-1; i++) {
            if (blips[i].length > 0) {
                if (blips[i][0] > center || blips[i][1] < center) {
                    blips[i] = new int[0];
                }
            }
        }
    }

    /**
     * Returns true if the pixel seems to match the laser line.
     */
    private static boolean isColorGreen(int[] pixel) {
        float[] hsb = new float[3];
        Color.RGBtoHSB(pixel[0], pixel[1], pixel[2], hsb);
        return hueMatches(hsb[0], GREEN_HUE) && hsb[1] > .2;
    }

    private static float getHue(Color color) {
        return Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null)[0];
    }

    /**
     * Returns true if the hue is the color is an approximate match.
     */
    private static boolean hueMatches(float color1, float color2) {
        float x = color1 - color2;
        return (x - Math.floor(x)) < .10F;
    }

    /**
     * Returns the position of a value in a sorted array.  If the
     * value does not exactly match a value in the array, return the
     * interpolated location.
     */
    private double estimatePos(double[] values, double sample) {
        int pos = Arrays.binarySearch(values, sample);
        if (pos >= 0) {
            return pos;
        } else {
            pos = -(pos + 1);
            if (pos == 0) {
                return 0;
            } else if (pos+1 >= values.length) {
                return values[values.length-1];
            }
            return Interpolate.interpolate(sample, values[pos-1], pos-1, values[pos], pos);
        }
    }

    private void debugImage(String type, RenderedImage img) throws IOException {
        if (DEBUG) {
            ImageIO.write(img, "jpg", new File(base+"_"+type+".jpg"));
        }
    }

    /**
     * A class representing a found line segment.
     */
    private static class Line implements Comparable<Line> {
        int start;
        int length;

        public Line(int start, int end) {
            this.start = start;
            this.length = end - start;
        }

        @Override
        public int compareTo(Line o) {
            return o.length - this.length;
        }

        public int getMidPoint() {
            return start + length / 2;
        }
    }

    private RenderedImage heightWarp(RenderedImage img, RenderedImage heightImage) {
        WarpHeight warp = new WarpHeight(heightImage, heightToBrightness, cameraDistance, img.getWidth()/2, img.getHeight()/2, dpi);
        this.heightImage = warp.getHeightMap();
        heightWarp = warp;

        ParameterBlock pb2 = new ParameterBlock();
        pb2.addSource(img);
        pb2.add(warp);
        pb2.add(new InterpolationBilinear());
        return JAI.create("warp", pb2);
    }

    private Warp calcAngleWarp(RenderedImage heightImage) {
        Raster heightMap = heightImage.getData();

        float inchesPerSample = 1 /dpi * SAMPLE_SIZE;
        final int horizontalIntervals = 2;
        final int verticalIntervals = heightMap.getHeight() / SAMPLE_SIZE;
        final int lastX = heightImage.getWidth() - 1;
        int pixel[] = new int[1];
        // The warping will cause the end points to be invalid.  This
        // will calculate the heights based on the first valid points.
        Interpolate[] heights = new Interpolate[verticalIntervals];
        for (int i=0; i < verticalIntervals; i++) {
            int start = -1;
            int end = lastX;
            double startValue = Double.NaN;
            double endValue = Double.NaN;
            int y = i * SAMPLE_SIZE;
            for (int x = 0; x <= lastX; x++) {
                if (heightMap.getPixel(x, y, pixel)[0] != 0) {
                    start = x + 1;
                    startValue = heightMap.getPixel(start, y, pixel)[0];
                    break;
                }
            }
            for (int x = lastX; x >=0; x--) {
                if (heightMap.getPixel(x, y, pixel)[0] != 0) {
                    end = x - 1;
                    endValue = heightMap.getPixel(end, y, pixel)[0];
                    break;
                }
            }
            heights[i] = new Interpolate(start, startValue, end, endValue);
        }
        final double[][] distance = new double[horizontalIntervals][];
        for (int j=0; j < horizontalIntervals; j++) {
            int x = j * lastX;
            distance[j] = new double[verticalIntervals];
            for (int i=0; i < verticalIntervals; i++) {
                int y = i * SAMPLE_SIZE;
                double height = heightToBrightness.inverse(heights[i].interpolate(x));
                if (i == 0) {
                    distance[j][i] = inchesPerSample;
                } else {
                    double cellDistance;
                    double lastHeight = heightToBrightness.inverse(heights[i-1].interpolate(x));
                    if (Double.isNaN(height)) {
                        cellDistance = 0;
                    } else if (Double.isNaN(lastHeight)) {
                        cellDistance = inchesPerSample;
                    } else {
                        double delta = height - lastHeight;
                        cellDistance = Math.sqrt(delta * delta + inchesPerSample * inchesPerSample) ;
                    }
                    distance[j][i] = cellDistance + distance[j][i-1];
                }
            }
        }

        float[] warpValues = new float[2*(horizontalIntervals)*(verticalIntervals)];
        for (int i=0; i < verticalIntervals; i++) {
            int y = i * SAMPLE_SIZE;
            for (int j=0; j < horizontalIntervals; j++) {
                int x = j * lastX;
                double posEst = estimatePos(distance[j], y/dpi) * SAMPLE_SIZE;
                int pos = 2* (i * horizontalIntervals + j);
                warpValues[pos++] = x;
                warpValues[pos] = (float) posEst;
            }
        }

        // adjust the points so both sides are the same total length;
        int pos = warpValues.length - 4;
        float avg = (warpValues[pos+1] + warpValues[pos+3]) /2;
        float[] adj = new float[] { avg / warpValues[pos+1], avg / warpValues[pos+3]};
        for (int i=0; i < verticalIntervals; i++) {
            for (int j=0; j < horizontalIntervals; j++) {
                int p = 2* (i * horizontalIntervals + j);
                warpValues[p + 1] *= adj[j];
            }
        }

        return new WarpGrid(0, lastX, horizontalIntervals - 1, 0, SAMPLE_SIZE, verticalIntervals - 1, warpValues);
    }


    /**
     * Returns the location of the crease in the center of a book. Start in
     * the center and look for the lowest point, expanding the search from
     * the center until the result doesn't exist close to an end point of the
     * search.
     */
    private int getCenterRow(Raster heightMap) {
        int height = heightMap.getHeight();
        int x = heightMap.getWidth() / 2;
        int[] pixel = new int[1];

        int minWindow = height / 2 - height / 10;
        int maxWindow = height / 2 + height / 10;

        int beginMin = 0;
        int endMin = 0;
        while (true) {
            int min = Integer.MAX_VALUE;
            for (int y = minWindow; y < maxWindow; y++) {
                heightMap.getPixel(x, y, pixel);
                if (min > pixel[0]) {
                    min = pixel[0];
                    beginMin = y;
                }
                if (min == pixel[0]) {
                    endMin = y;
                }
            }
            if (beginMin < minWindow + 10) {
                minWindow = Math.max(0, minWindow - height / 10);
            } else if (endMin > maxWindow + 10) {
                maxWindow = Math.max(height, maxWindow + height / 10);
            } else {
                break;
            }
        }
        int pos = (beginMin + endMin + 1) / 2;
        return pos;
    }

    static {
        JAI.getDefaultInstance().getTileCache().setMemoryCapacity(300000000);
    }
}
