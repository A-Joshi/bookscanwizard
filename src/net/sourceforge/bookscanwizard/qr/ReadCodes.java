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

import com.google.zxing.Binarizer;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.multi.qrcode.QRCodeMultiReader;
import com.google.zxing.qrcode.decoder.Version;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import net.sourceforge.bookscanwizard.BSW;
import net.sourceforge.bookscanwizard.BSWThreadFactory;
import net.sourceforge.bookscanwizard.util.GlobFilter;
import net.sourceforge.bookscanwizard.util.LazyHashMap;
import net.sourceforge.bookscanwizard.util.Utils;

/**
 * This contains code that scan a group of files, and save any barcodes it finds.
 */
public class ReadCodes {
    private static final Logger logger = Logger.getLogger(ReadCodes.class.getName());
    private static final int DEFAULT_THRESHOLD = 90;
    public static final String PREFIX = "BSW_";
    // scale down the image so the barcode reading will be faster.
    private static float scale = .25f;
    private static double threshold = DEFAULT_THRESHOLD;
    private static final Hashtable hints = new Hashtable();

    private Collection<File> files;
    private LazyHashMap<String, List<QRData>> codes = new LazyHashMap<>(ArrayList.class);

    public ReadCodes(Collection<File> files) {
        this.files = files;
    }

    public ReadCodes(String fileGlob) throws FileNotFoundException {
        this.files = getFiles(Collections.singleton(fileGlob), Utils.imageFilter());
    }


    public ReadCodes(String fileGlob, float scale) throws FileNotFoundException {
        this.scale = scale;
        this.files = getFiles(Collections.singleton(fileGlob), Utils.imageFilter());
    }

    public List<QRData> getCodes() throws IOException, InterruptedException, ExecutionException {
        ExecutorService threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), 
                new BSWThreadFactory(BSWThreadFactory.LOW_PRIORITY));
        final ArrayList<Future<List<QRData>>> futures = new ArrayList<>();

        for (final File f : files) {
            Callable<List<QRData>> task = new Callable<List<QRData>>() {
                @Override
                public List<QRData> call() throws Exception {
                    return findCodesInternal(f);
                }
            };
            futures.add(threadPool.submit(task));
        }
        List<QRData> fileList = new ArrayList<>();
        for (Future<List<QRData>> f : futures) {
            fileList.addAll(f.get());
        }
        for (QRData data : fileList) {
            codes.getOrCreate(data.getFileName()).add(data);
        }
        return fileList;
    }

    public List<QRData> getCodes(File f) throws IOException {
        if (codes.isEmpty()) {
            return findCodesInternal(f);
        } else {
            return codes.getOrCreate(f.getName());
        }
    }

    private List<QRData> findCodesInternal(File f) throws IOException {
        logger.log(Level.INFO, "scanning {0}", f.getName());
        RenderedImage img = ImageIO.read(f);
        return findCodes(img, f.getName());
    }

    /**
     * Returns the size of the barcode from one anchor to the next.
     */
    private static HashMap<Integer,Double> dimensions = new HashMap<>();
    public static double getBarcodeDimensions(String code, int size) {
        Double retVal = dimensions.get(size);
        if (retVal == null) {
            // This crazy code is because there isn't a good way to go from
            // a requested print size to the distance between anchors.  So
            // we render an image, then measure the image.  Crazy.
            try {
                RenderedImage img = PrintCodes.encodeString(code, size);
                List<QRData> list = findCodes(img, "");
                QRData data = list.get(0);
                retVal = data.getPointsArray()[0].getY() - data.getPointsArray()[1].getY();
                dimensions.put(size, retVal);
            } catch (WriterException e) {
                // shouldn't happen.
                throw new RuntimeException(e);
            }
        }
        return retVal;
    }

    public static List<QRData> findCodes(RenderedImage img, String fileName) {
        ArrayList<QRData> list = new ArrayList<>();
        BufferedImageLuminanceSource luminanceSource;
        synchronized(ReadCodes.class) {
            img = preprocessImage(img);
            luminanceSource =
                    new BufferedImageLuminanceSource(Utils.renderedToBuffered(img));
        }
        Binarizer binarizer = new GlobalHistogramBinarizer(luminanceSource);

        try {
            QRCodeMultiReader detector = new QRCodeMultiReader();
            Result[] results = detector.decodeMultiple(new BinaryBitmap(binarizer), hints);
            for (Result result : results) {
                String text = result.getText();
                if (text.startsWith(PREFIX)) {
                    Version v = (Version) result.getResultMetadata().get(ResultMetadataType.VERSION_NUMBER);
                    QRData qrdata = new QRData(
                        fileName,
                        text.substring(PREFIX.length()),
                        result.getResultPoints(), v);
                    list.add(qrdata);
                }
            }
        } catch (NotFoundException e) {
            // ignore
        }
        return list;
    }

    static List<File> getFiles(Collection<String> args, FilenameFilter defaultFilter) throws FileNotFoundException {
        ArrayList<File> allData = new ArrayList<>();
        for (String name : args) {
            File source = new File(name);
            if (source.isFile()) {
                allData.add(source);
            } else if (source.isDirectory()) {
                allData.addAll(Arrays.asList(source.listFiles(defaultFilter)));
            } else {
                File parent = source.getParentFile();
                if (parent == null) {
                    throw new FileNotFoundException("Could not find "+source);
                }
                if (parent.isDirectory()) {
                    GlobFilter filter = new GlobFilter(source.getName());
                    allData.addAll(Arrays.asList(parent.listFiles(filter)));
                }
            }
        }
        return allData;
    }

    public Collection<File> getFiles() {
        return files;
    }

    public static void setThreshold(double threshold) {
        ReadCodes.threshold = threshold < 0 ? DEFAULT_THRESHOLD : (threshold * 2.55);
    }


    static {
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
    }

    /**
     * Scales down the image and reduces it to a single band.  This simplistic
     * conversion does a better job than zxing's binarizers for well lit
     * high resolution images.
     */
    private static RenderedImage preprocessImage(RenderedImage img) {
        if (img.getSampleModel().getNumBands() > 1) {
            // Just use the green band
            img = JAI.create("bandselect",img,new int[] {1});
            if (scale != 1) {
                ParameterBlock pb = new ParameterBlock()
                    .addSource(img).add(scale).add(scale)
                    .add(0F).add(0F).
                    add(Interpolation.getInstance(Interpolation.INTERP_NEAREST));
                img = JAI.create("scale", pb, BSW.SPEED_HINTS);
            }
        }
        img = JAI.create("binarize", img, threshold);
        return img;
    }

    public static void main(String[] args) throws Exception {
        String directory = null;
        for (int i=0; i < args.length; i++) {
            switch (args[i]) {
                case "-scale":
                    i++;
                    scale = Float.parseFloat(args[i]);
                    break;
                case "-threshold":
                    i++;
                    setThreshold(Double.parseDouble(args[i]));
                    break;
                default:
                    directory = args[i];
                    break;
            }
        }
        if (directory == null) {
            System.err.println("Bad parameters");
            System.exit(1);
        }
        File destination = new File(directory);
        if (!destination.isDirectory()) {
            System.err.println(directory+" is not a directory");
        }
        destination = new File(destination, "barcodes.csv");
        ReadCodes codes = new ReadCodes(directory);
        List<QRData> allData = codes.getCodes();
        QRData.write(destination, allData);
    }
}
