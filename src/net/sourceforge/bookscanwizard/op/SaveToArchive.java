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
public class SaveToArchive extends Operation {
    private static ZipOutputStream zipOut;
    private static boolean abortRequested;
    private static SaveToArchive lastSave;
    private static JProgressBar progressBar;

    private String access;
    private String secret;
    private String fileName;

    @Override
    protected List<Operation> setup(List<Operation> operationList) throws Exception {
        String[] args = getTextArgs();
        abortRequested = false;
        if (zipOut != null) {
            zipOut.close();
            zipOut = null;
        }
        ArchiveTransfer.checkMetaData(Metadata.getMetaData());
        lastSave = this;
        fileName = args[0];
        access = args[1];
        secret = args[2];
        return operationList;
    }

    @Override
    public void postOperation() throws Exception {
        saveToArchive(fileName, access, secret);
    }

    public void saveToArchive(String[] args) throws Exception {
        saveToArchive(args[0], args[1], args[2]);
    }

    public static void saveToArchive(String fileName, String access, String secret) throws Exception {
        if (access == null || secret == null || access.isEmpty() || secret.isEmpty()) {
            throw new UserException("The archive access and secret keys must not be null");
        }
        final ArchiveTransfer transfer = new ArchiveTransfer(access, secret);
        final ProgressListener progressListener = new ProgressListener() {
            public void updateProgress(double pctComplete) {
                if (abortRequested) {
                    throw new UserException("Aborted");
                }
                progressBar.setValue((int) (pctComplete * 100));
            }
        };
        if (!BSW.isBatchMode()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    progressBar = BSW.instance().getMainFrame().getProgressBar();
                    progressBar.setMaximum(100);
                    BSW.instance().getMainFrame().setStatusLabel("Uploading..");
                    transfer.setProgressListener(progressListener);
                }
            });
        }
        Logger.getLogger(SaveToArchive.class.getName()).log(Level.INFO, "Begin saving to archive");
        transfer.saveToArchive(BSW.getFileFromCurrentDir(fileName));
        Logger.getLogger(SaveToArchive.class.getName()).log(Level.INFO, "Finished saving to archive");
    }

    public static void abortRequested() {
        abortRequested = true;
    }

    public static SaveToArchive getLastSave() {
        return lastSave;
    }

    public String getAccess() {
        return access;
    }

    public String getFileName() {
        return fileName;
    }

    public String getSecret() {
        return secret;
    }
}
