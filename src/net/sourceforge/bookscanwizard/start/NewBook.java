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

package net.sourceforge.bookscanwizard.start;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import net.sourceforge.bookscanwizard.BSW;
import net.sourceforge.bookscanwizard.gui.UserFeedbackHelper;
import net.sourceforge.bookscanwizard.op.EstimateDPI;
import org.netbeans.api.wizard.WizardDisplayer;
import org.netbeans.spi.wizard.Wizard;
import org.netbeans.spi.wizard.WizardPage;


/**
 *  A wizard that will create a basic script for a directory.
 */
public class NewBook {
    public static final String WORKING_DIRECTORY = "working_directory";
    public static final String SOURCE_DIRECTORY = "source_directory";
    public static final String DESTINATION_DIRECTORY = "destination_directory";
    public static final String USE_BARCODES = "use_barcodes";
    public static final String SOURCE_DPI = "override_source_dpi";
    public static final String DESTINATION_DPI = "destination_dpi";
    public static final String COMPRESSION = "compression";
    public static final String OUTPUT_TYPE = "output_type";
    public static final String LEFT_ORIENT = "left_orient";
    public static final String RIGHT_ORIENT = "right_orient";
    public static final String USE_FOCAL_LENGTH = "use_focal_length";
    public static final String FOCAL_LENGTH1 = "focal length1";
    public static final String ESTIMATED_DPI1 = "estimated_dpi_1";
    public static final String FOCAL_LENGTH2 = "focal length2";
    public static final String ESTIMATED_DPI2= "estimated_dpi_2";
    public static final String FILE_FORMAT = "file_format";

    private final Wizard wizard;

    public NewBook() {
        wizard = WizardPage.createWizard(new Class[] {Page1.class, Page2.class});
    }

    public String getConfig() throws IOException {
        Map<String,Serializable> settings =
            (Map<String,Serializable>) WizardDisplayer.showWizard(wizard);
        if (settings == null) {
            return null;
        }
        System.out.println(settings);
        BSW.setCurrentDirectory(new File((String) settings.get(WORKING_DIRECTORY)).toPath());
        settings.put(WORKING_DIRECTORY, BSW.getCurrentDirectory().getParent());
        AbstractPage.putMatchingSettings(settings);

        StringBuilder str = new StringBuilder();
        str.append("# Book Scan Wizard Script \n" + 
                   "# http://bookscanwizard.sourceforge.net\n" + 
                   "#   ").append(BSW.getCurrentDirectory()).append("\n\n\n");
                   
        File sourceDir = new File((String) settings.get(SOURCE_DIRECTORY));
        File leftDir = BSW.getFileFromCurrentDir(new File(sourceDir, "l").getPath()).getAbsoluteFile();
        File rightDir = BSW.getFileFromCurrentDir(new File(sourceDir, "r").getPath()).getAbsoluteFile();
        str.append("# *** Load Files ***\n");
        if (leftDir.isDirectory() && rightDir.isDirectory()) {
            str.append("# the source directory, with subdirectories for left & right files\n");
            str.append("LoadLRImages = ");
        } else {
            str.append("# the source directory\n");
            str.append("LoadImages = ");
        }
        str.append(sourceDir);
        str.append("\n\n");
        if (!((String)settings.get(SOURCE_DPI)).isEmpty()) {
            str.append("# Override source DPI\n" + 
                       "SetSourceDPI = ").append(settings.get(SOURCE_DPI)).append("\n\n");
        }
        str.append(
                   "# The Destination directory\n" +
                   "SetDestination = ");
        str.append(settings.get(DESTINATION_DIRECTORY));
        str.append("\n\n");
        if (((Boolean) settings.get(USE_FOCAL_LENGTH))) {
            str.append("# Estimate the source DPI from the focal length setting\n");
            str.append(EstimateDPI.getConfig()).append("\n\n");
        }
        String rotate;
        str.append("# *** Page Rotations ***\n"+
                   "Pages = left\n");
        rotate = (String) settings.get(LEFT_ORIENT);
        if (!rotate.equals("0")) {
            str.append("Rotate = ").append(rotate).append("\n");
        }
        str.append("Pages = right\n");
        rotate = (String) settings.get(RIGHT_ORIENT);
        if (!rotate.equals("0")) {
            str.append("Rotate = ").append(rotate).append("\n");
        }
        str.append("\n");
        boolean useBarcodes = (Boolean) settings.get(USE_BARCODES);
        if (useBarcodes) {
            str.append("# Use barcodes.csv to define operations \n"+
                       "Barcodes =\n");
        }
        str.append("# *** Remove Pages ***\n");
        str.append("# *** Perspective ***\n");
        str.append("# *** Crops ***\n");
        str.append("# *** Filters ***\n");
        String color = (String) settings.get(OUTPUT_TYPE);
        System.out.println("color: "+color);
        if ("Greyscale".equals(color) || "B/W".equals(color)) {
            str.append("Color = gray\n\n");
        }
        if ("B/W".equals(color)) {
            str.append("#Change to a binary (black & white) image, with a clipping point of 60%\n"+
                       "Color=bw 60\n\n");
        }
        str.append("# *** Scaling ***\n");
        str.append("Pages = all\n");
        String dpi = (String) settings.get(DESTINATION_DPI);
        dpi = dpi.replace("Keep Source DPI", "0");
        if (!dpi.equals("0")) {
            str.append("# Rescale the image to match the final DPI\n" + "ScaleToDPI=").append(dpi).append("\n\n");
        }
        str.append("# This will ensure the left and right pages are exactly the same size.\n");
        str.append("ScaleToFirst=\n\n");
        
        str.append("# *** Output ***\n");
        str.append("Pages=all\n");

        String imagetype = (String) settings.get(FILE_FORMAT);
        String compression = (String) settings.get(COMPRESSION);
        if (compression == null) {
            compression = "";
        }
        if (imagetype.equals("PDF")) {
            str.append("CreatePDF = ").append(BSW.getCurrentDirectory().getName()).append(".pdf\n");
        } else {
            str.append("SaveImages = ").append(imagetype).append(" ").append(compression).append("\n\n");
        }
        return str.toString();
    }

    static {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            UserFeedbackHelper.displayException(null, ex);
        }
    }

    public static void main(String[] args) throws Exception {
        NewBook newBook = new NewBook();
        System.out.println(newBook.getConfig());
    }
}
