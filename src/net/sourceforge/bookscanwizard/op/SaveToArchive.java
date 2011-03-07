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
+ * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package net.sourceforge.bookscanwizard.op;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import net.sourceforge.bookscanwizard.BSW;
import net.sourceforge.bookscanwizard.Operation;
import net.sourceforge.bookscanwizard.UserException;
import net.sourceforge.bookscanwizard.s3.ArchiveTransfer;
import net.sourceforge.bookscanwizard.s3.ProgressListener;

/**
 *
 * @author Steve
 */
public class SaveToArchive extends Operation implements ProgressListener {
    private static ZipOutputStream zipOut;
    private static boolean abortRequested;
    private static String defaultAccess;
    private static String defaultSecret;
    private ImageWriter writer = (ImageWriter) ImageIO.getImageWritersByFormatName("jpeg 2000").next();
    private JProgressBar progressBar;

    @Override
    protected List<Operation> setup(List<Operation> operationList) throws Exception {
        abortRequested = false;
        if (zipOut != null) {
            zipOut.close();
            zipOut = null;
        }
        ArchiveTransfer.checkMetaData(Metadata.getMetaData());
        return operationList;
    }

    @Override
    public void postOperation() throws Exception {
        String[] args = getTextArgs();
        saveToArchive(args);
    }

    public void saveToArchive(String[] args) throws Exception {
        String access = null;
        String secret = null;
        String fileName = args[0];

        if (args.length > 1) {
            access = args[1];
        }
        if (args.length > 2) {
            secret = args[2];
        }
        if (access == null) {
            access = defaultAccess;
        }
        if (secret == null) {
            secret = defaultSecret;
        }
        if (access == null || secret == null || access.isEmpty() || secret.isEmpty()) {
            throw new UserException("The archive access and secret keys must not be null");
        }
        final ArchiveTransfer transfer = new ArchiveTransfer(access, secret);
        if (!BSW.isBatchMode()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    progressBar = BSW.instance().getMainFrame().getProgressBar();
                    progressBar.setMaximum(100);
                    BSW.instance().getMainFrame().setStatusLabel("Uploading..");
                    transfer.setProgressListener(SaveToArchive.this);
                }
            });
        }
        Logger.getLogger(SaveToArchive.class.getName()).log(Level.INFO, "Begin saving to archive");
        transfer.saveToArchive(BSW.getFileFromCurrentDir(fileName));
        Logger.getLogger(SaveToArchive.class.getName()).log(Level.INFO, "Finished saving to archive");
    }

    public void updateProgress(double pctComplete) {
        if (abortRequested) {
            throw new UserException("Aborted");
        }
        progressBar.setValue((int) (pctComplete * 100));
    }

    public static void abortRequested() {
        abortRequested = true;
    }

    public static void setDefaultKeys(String access, String secret) {
        SaveToArchive.defaultAccess = access;
        SaveToArchive.defaultSecret = secret;
    }
}
