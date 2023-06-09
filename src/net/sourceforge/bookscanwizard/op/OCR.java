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
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import net.sourceforge.bookscanwizard.FileHolder;
import net.sourceforge.bookscanwizard.Operation;
import net.sourceforge.bookscanwizard.UserException;
import net.sourceforge.bookscanwizard.gui.UserPreferenceBean;
import net.sourceforge.bookscanwizard.util.OtsuThreshold;
import net.sourceforge.bookscanwizard.util.ProcessHelper;
import org.apache.commons.io.IOUtils;

/**
 * This will use Google Tesseract to OCR the pages to hOCR format, which
 * which can be added to the output PDF
 */
public class OCR extends Operation {
    private String language;
    private static boolean useOCR = false;
    private static final Charset UTF8 = Charset.forName("UTF8");
    
    @Override
    protected List<Operation> setup(List<Operation> operationList) throws Exception {
        language = getOption("language");
        useOCR = false;
        return super.setup(operationList);
    }

    @Override
    protected RenderedImage previewOperation(FileHolder holder, RenderedImage img) throws Exception {
        // do not run during preview.
        return img;
    }

    @Override
    public RenderedImage performOperation(FileHolder holder, RenderedImage img) throws Exception {
        String[] args = getTextArgs();
        ProcessHelper.fixScript(args);
        String name = "tesseract";
        String dir = UserPreferenceBean.instance().getTesseractLocation();
        if (dir != null && !dir.isEmpty()) {
            name = new File(dir, name).getPath();
        }
        name = ProcessHelper.findPath(name);
        File parent = new File(name).getParentFile();
        if (!new File(name).isFile()) {
            throw new UserException("The executable \"tesseract\" could not be found.\nEnsure the directory is on the system path or the location is entered under Tools, Preferences");
        }
        List<String> cmdList = new ArrayList<>();
        cmdList.add(name);
        String pngName = new File(pageSet.getDestinationDir(), holder.getName() + ".png").getCanonicalPath();
        cmdList.add(pngName);
        cmdList.add(new File(pageSet.getDestinationDir(), holder.getName()).getCanonicalPath());
        if (language != null) {
            cmdList.add("-l");
            cmdList.add(language);
        }
        cmdList.add(new File(parent, "tessdata/configs/hocr").getCanonicalPath());
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(pngName))) {
            ImageIO.write(OtsuThreshold.binarize(img), "PNG", os);
//            ImageIO.write(SauvolaBinarisationFilter.filter(img), "PNG", os);
        }
        ProcessBuilder processBuilder = new ProcessBuilder(cmdList)
                .directory(pageSet.getDestinationDir())
                .redirectErrorStream(true);
        Process proc = processBuilder.start();
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            IOUtils.copy(proc.getInputStream(), bos);
            int retVal = proc.waitFor();
            new File(pngName).delete();
            if (retVal != 0) {
                String msg = new String(bos.toByteArray(), UTF8);
                throw new UserException(msg);
            }
        }
        useOCR = true;
        return img;
    }
    
    @Override
    public void postOperation() throws Exception {
        useOCR = false;
    }

    /**
     * Indicates if OCR has been run.
     * 
     * @return true if OCR has been enabled for this run
     */
    public static boolean isUseOCR() {
        return useOCR;
    }
}
