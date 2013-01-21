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

package net.sourceforge.bookscanwizard.gui;

import java.awt.Cursor;
import java.awt.Event;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.text.Document;
import net.sourceforge.bookscanwizard.BSW;
import net.sourceforge.bookscanwizard.Batch;
import net.sourceforge.bookscanwizard.ColorOp;
import net.sourceforge.bookscanwizard.CropOp;
import net.sourceforge.bookscanwizard.FileHolder;
import net.sourceforge.bookscanwizard.Operation;
import net.sourceforge.bookscanwizard.PerspectiveOp;
import net.sourceforge.bookscanwizard.SectionName;
import net.sourceforge.bookscanwizard.UserException;
import net.sourceforge.bookscanwizard.config.ConfigAutoLevels;
import net.sourceforge.bookscanwizard.config.ConfigBalancedAutoLevels;
import net.sourceforge.bookscanwizard.config.ConfigGrayCard;
import net.sourceforge.bookscanwizard.op.Barcodes;
import net.sourceforge.bookscanwizard.op.EstimateDPI;
import net.sourceforge.bookscanwizard.op.Pages;
import net.sourceforge.bookscanwizard.op.RemovePages;
import net.sourceforge.bookscanwizard.op.StartPage;
import net.sourceforge.bookscanwizard.op.WhiteBalance;
import net.sourceforge.bookscanwizard.qr.PrintCodes;
import net.sourceforge.bookscanwizard.qr.PrintCodesDialog;
import net.sourceforge.bookscanwizard.start.NewBook;
import net.sourceforge.bookscanwizard.start.PreferenceWizard;
import net.sourceforge.bookscanwizard.unwarp.FilterWizard;
import net.sourceforge.bookscanwizard.util.ProcessHelper;

/**
 * This contains the actions specific to the interactive processing.
 */
public class GuiActions extends UserFeedbackHelper {
    private final BSW bsw;
    MainFrame mainFrame;

    public GuiActions(BSW bsw) {
        this.bsw = bsw;
    }

    public void setMainFrame(MainFrame mf) {
        this.mainFrame = mf;
    }
    
    @Override
    public void cursorActionPerformed(ActionEvent e) throws Exception {
        String cmd = e.getActionCommand();
        String key = cmd.split(" ")[0];
        if ("abort".equals(cmd)) {
            bsw.abort();
            return;
        }
        if (bsw.isRunning()) {
            throw new UserException("A process is running.  Either cancel the process or wait for it to complete before continuing.");
        }
        
        String config;
        int inc;
        switch (key) {
            case "about":
                mainFrame.getAboutDialog().setVisible(true);
                break;
            case "add_to_batch":
                BatchList.getInstance(mainFrame).addItem(bsw.getConfigFile());
                BatchList.getInstance(mainFrame);
                break;
            case "auto_levels":
                autoLevels(false);
                break;
            case "auto_rgb_levels":
                autoLevels(true);
                break;
            case "balanced_normalize_lighting":
                config = new ConfigBalancedAutoLevels().getConfig(bsw.getConfigImage());
                insertConfig(config, false, true);
                break;
            case "create_script":
                createScript();
                break;
            case "command_helper":
                mainFrame.getOperationList().setVisible(true);
                break;
            case "copy_points_to_viewer":
                mainFrame.getViewerPanel().setPointDef(bsw.getConfigEntry().getCurrentLineOrSelection());
                break;
            case "crop":
                insertCoordinates("Crop =");
                break;
            case "crop_and_scale":
                cropAndScale();
                break;
            case "display_batch_list":
                BatchList.getInstance(mainFrame);
                break;
            case "do_upload":
                UploadFile uploadFile = ((UploadFile) ((JComponent) e.getSource()).getTopLevelAncestor());
                bsw.runBatch(mainFrame.getConfigEntry().getText(), new UploadImages(uploadFile));
                break;
            case "expand_barcode_operations":
                bsw.preview();
                insertConfigNoPreview(Barcodes.getConfiguration(), true, false);
                break;
            case "exit":
                System.exit(0);
                break;
            case "filter_toolkit":
                mainFrame.showFilterDialog();
                break;
            case "gray_card":
                config = new ConfigGrayCard().getConfig(bsw.getConfigImage());
                insertConfig(config, false, true);
                break;
            case "insert_config":
                String insertText = cmd.substring(cmd.indexOf(" ")+1);
                insertConfigNoPreview(insertText, false, true);
                break;
            case "import_monitor":
                ImportImages.getInstance().setVisible(true);
                ImportImages.getInstance().requestFocus();
                break;
            case "keystone_barcodes":
                PrintCodes.keystoneCodes();
                break;
            case "laser_filter":
                FilterWizard filterWizard = new FilterWizard();
                filterWizard.setImage(bsw.getPreviewedImage().getPreviewImage());
                filterWizard.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                filterWizard.setVisible(true);
                break;
            case "metadata":
                bsw.prepareMetadata();
                break;
            case "new":
                newBatch();
                break;
            case "nextPage":
                inc = ((e.getModifiers() & Event.SHIFT_MASK) != 0) ? 2 : 1;
                if (mainFrame.getPageListBox().getSelectedIndex() + inc >= mainFrame.getPageListBox().getItemCount()) {
                    throw new UserException("You are at the last image");
                }
                mainFrame.getPageListBox().setSelectedIndex(mainFrame.getPageListBox().getSelectedIndex() + inc);
                break;
            case "normalize_lighting":
                normalizeLighting();
                break;
            case "op":
                if (cmd.equals("op EstimateDPI")) {
                    String dpiInfo = EstimateDPI.getConfig();
                    if (dpiInfo == null) {
                        throw new UserException("No DPI information is saved");
                    }
                    insertConfigNoPreview(dpiInfo, false, false);
                } else {
                    insertConfigNoPreview(cmd.substring(3) + " = ", false, true);
                }
                break;
            case "open":
                bsw.openConfig();
                break;
            case "perspective":
                insertCoordinates("Perspective =");
                break;
            case "perspective_and_crop":
                insertCoordinates("PerspectiveAndCrop =");
                break;
            case "preferences":
                PreferenceWizard wizard = new PreferenceWizard();
                wizard.getConfig();
                break;
            case "preview":
                bsw.preview();
                break;
            case "preview_if_not_shift":
                boolean shift = (e.getModifiers() & InputEvent.SHIFT_MASK) != 0;
                if (!shift) {
                    bsw.preview();
                }
                break;
            case "preview_to_cursor":
                bsw.preview();
                break;
            case "previousPage":
                inc = ((e.getModifiers() & Event.SHIFT_MASK) != 0) ? 2 : 1;
                if (mainFrame.getPageListBox().getSelectedIndex() - inc < 0) {
                    throw new UserException("You are at the first image");
                }
                mainFrame.getPageListBox().setSelectedIndex(mainFrame.getPageListBox().getSelectedIndex() - inc);
                break;
            case "print_qr_codes":
                JDialog dialog = new PrintCodesDialog(mainFrame, false);
                dialog.setVisible(true);
                break;
            case "remove_page":
                removePage();
                break;
            case "rotate":
                insertCoordinates("Rotate =");
                break;
            case "run":
                bsw.runBatch(mainFrame.getConfigEntry().getText(), null);
                break;
            case "run_batch_list":
                bsw.runBatchList(BatchList.getInstance(mainFrame).getBatchList());
                break;
            case "save":
                bsw.saveConfig();
                break;
            case "save_as":
                bsw.saveConfigAs();
                break;
            case "save_dpi":
                bsw.preview();
                EstimateDPI.saveFocalLength();
                break;
            case "save_filter":
                insertConfig(mainFrame.getFilterDialog().getConfig(), false, true);
                break;
            case "thumb_checkbox":
                mainFrame.getThumbTable().update();
                break;
            case "thumb_copy":
                String page = "Pages = " + mainFrame.getThumbTable().calcPageConfig();
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                StringSelection data = new StringSelection(page);
                clipboard.setContents(data, data);
                break;
            case "thumb_insert":
                String text = "Pages = " + mainFrame.getThumbTable().calcPageConfig();
                insertConfigNoPreview(text, false, false);
                break;
            case "thumb_remove_pages":
                text ="RemovePages = "+bsw.getMainFrame().getThumbTable().calcPageConfig(); 
                insertConfigNoPreview(text, false, true);
                break;
             case "thumb_select":
                FileHolder h = mainFrame.getThumbTable().getSelectedHolder();
                System.out.println("sel: " + h + " " + h.isDeleted());
                bsw.getPreviewedImage().setFileHolder(h);
                break;
            case "tip_of_the_day":
                mainFrame.showTipsDialog();
                break;
            case "upload":
                UploadFile.upload(this);
                break;
            case "zoomIn":
                mainFrame.getViewerPanel().multScale(1.5f);
                break;
            case "zoomOut":
                mainFrame.getViewerPanel().multScale(1f / 1.5F);
                break;
            case "white_balance":
                config = WhiteBalance.getConfig(bsw.getConfigImage());
                insertConfig(config, false, true);
                break;
            case "whiteout":
                insertCoordinates("Whiteout =");
                break;
            default:
                throw new UserException("Unknown action type: " + cmd);
        }
    }

    private void cropAndScale() throws Exception {
        String text = mainFrame.getViewerPanel().getPointDef();
        if (text.length() > 0) {
            text = "CropAndScale = "+ text+ " "+
                    mainFrame.getViewerPanel().getXCropScale() + " " +
                    mainFrame.getViewerPanel().getYCropScale() + " # " +
                    bsw.getPreviewedImage().getPreviewHolder().toString();
            insertConfig(text, false, true);
        }
    }

    private void insertCoordinates(String prefix) throws Exception {
        String text = mainFrame.getViewerPanel().getPointDef();
        if (text.length() > 0) {
            Operation op = bsw.getPreviewOperations().get(bsw.getPreviewOperations().size()-1);
            text = prefix+" "+ text;
            insertConfig(text, false, true);
         }
    }

    private void insertConfig(String origText, boolean replace, boolean ensurePosition) throws Exception {
        insertConfigNoPreview(origText+"\n", replace, ensurePosition);
         mainFrame.getConfigEntry().setSelectionEnd(mainFrame.getConfigEntry().getSelectionEnd()-1);

        Operation op = Operation.getStandaloneOp(origText);
        if (op instanceof PerspectiveOp) {
            if (mainFrame.isShowPerspective()) {
                mainFrame.getViewerPanel().setPointDef("");
                bsw.preview();
            }
        } else if (op instanceof CropOp) {
            if (mainFrame.isShowCrops()) {
                mainFrame.getViewerPanel().setPointDef("");
                bsw.preview();
            }
        } else if (op instanceof ColorOp) {
            if (mainFrame.isShowColors()) {
                bsw.preview();
            }
        } else {
            bsw.preview();
        }
    }
    private void autoLevels(boolean separateRGB) throws Exception {
        String config = new ConfigAutoLevels().getConfig(bsw.getConfigImage(),separateRGB);
        insertConfig(config, false, true);
    }

    private void normalizeLighting() throws Exception {
        String config =  "NormalizeLighting = "+bsw.getPreviewedImage().getPreviewHolder().getName();
        insertConfig(config, false, true);
    }

    public void removePage() throws Exception {
        String line = bsw.getConfigEntry().getCurrentLineOrSelection();
        if (line.endsWith("\n")) {
            line = line.substring(0, line.length()-1);
        }
        if (line.contains("RemovePages")) {
            line = line + ", "+ bsw.getPreviewedImage().getPreviewHolder().getName();
            insertConfig(line, true, true);
        } else {
            insertConfig("RemovePages = " + bsw.getPreviewedImage().getPreviewHolder().getName(), false, true);
        }
    }

    /**
     * Inserts text into the script.
     * 
     * @param newText The text insert
     * @param replace If the selected text should be replaced.  Otherwise it
     *        will do an insert.
     * @param ensurePosition If the insert should insert a Pages setting if needed
     *        to match the selected page.
     * @throws Exception 
     */
    public void insertConfigNoPreview(String newText, boolean replace, boolean ensurePosition) throws Exception {
        String text = newText;
        ConfigEntry config = mainFrame.getConfigEntry();
        Document document = config.getDocument();
        int start = config.getSelectionStart();
        int end = config.getSelectionEnd();
        if (replace) {
            if (end < start) {
                int x = start;
                start = end;
                end = x;
            }
            document.remove(start, end - start);
            end = start;
        }
        replace = false;
        SectionName sectionName = null;
        boolean addCurrentPage = false;
        if (ensurePosition && !replace) {
            System.out.println("found: "+newText);
            Operation op = null;
            for (String newLine : newText.split("\n")) {
                op =  Operation.getStandaloneOp(newLine);
            }
            if (op != null) {
                sectionName = SectionName.getSectionFromOp(op);
            }
            if (sectionName != null) {
                String currentName = bsw.getPreviewedImage().getPreviewHolder().getName();
                boolean sectionBegan = false;
                boolean lastPageMatched = false;
                start = 0;
                int lastMatchLine = 0;
                String match = sectionName.getMatchString();
                SectionName originalName = sectionName;
                while (!config.getText().contains(match)) {
                    sectionName = SectionName.getPreviousSection(sectionName);
                    match = sectionName.getMatchString();
                    if (sectionName == SectionName.BEGIN_MARKER) {
                        break;
                    }
                }
                for (String line : config.getText().split("\n")) {
                    if(sectionBegan && line.contains("# *** ")) {
                        end = start;
                        break;
                    }
                    if (line.contains(match)) {
                        sectionBegan = true;
                    }
                    if (!line.trim().isEmpty()) {
                        start += line.length()+1;
                    }
                    if (sectionBegan && originalName == sectionName) {
                        Operation testOp = Operation.getStandaloneOp(line);
                        if (testOp instanceof Pages) {
                            Pages pages = (Pages) testOp;
                            int previewPos = bsw.getPreviewedImage().getPreviewHolder().getPosition();
                            int pagePos = pages.getPosition();
                            if ((pagePos == FileHolder.ALL || previewPos == pagePos) && 
                                pages.getPageSet().getFileHolders().contains(bsw.getPreviewedImage().getPreviewHolder()))
                            {
                                lastPageMatched = true;
                            } else {
                                lastPageMatched = false;
                            }
                        }
                        if (lastPageMatched) {
                            if (testOp != null && testOp.getClass() == op.getClass()) {
                                addCurrentPage = true;
                            }
                            if (testOp instanceof StartPage) {
                                String compare = testOp.getPageSet().getMinFile();
                                if (currentName.equals(compare)) {
                                    replace = true;
                                } else if (currentName.compareTo(compare) < 0) {
                                    break;
                                }
                            } else if (replace) {
                                end = start;
                                start = start - line.length() -1 ;
                                document.remove(start, end-start);
                                end = start;
                                break;
                            } else {
                                lastMatchLine = start;
                            }
                        }
                    }
                    if (line.trim().isEmpty()) {
                        start += line.length()+1;
                    }
                }
                if (!replace) {
                    if (lastMatchLine > 0) {
                        end = lastMatchLine;
                        if (addCurrentPage && !(op instanceof RemovePages)) {
                            text = "StartPage = "+currentName+"\n"+text;
                        }
                    } else if (!(op instanceof RemovePages)) {
                       String posText = (bsw.getPreviewedImage().getPreviewHolder().getPosition()
                                  == FileHolder.LEFT ? "left" : "right");
                        text = "Pages = "+posText+"\n"+text;
                    }
                    if (originalName != sectionName) {
                        text = "# " + originalName.getMatchString()+"\n" + text;
                    }
                }
            }
        }
        
        document.insertString(end, text, null);
        config.setSelectionStart(end);
        config.setSelectionEnd(end + text.length());
        config.requestFocus();
    }
    
    private void createScript() throws IOException {
        final JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Enter the name and location of the script to save");
        fc.setApproveButtonText("Save");

        int returnVal = fc.showOpenDialog(mainFrame);
        if (returnVal != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File scriptFile = fc.getSelectedFile();
        try (PrintWriter out = new PrintWriter(new FileWriter(scriptFile))) {
            if (ProcessHelper.isWindows()) {
                out.println("@echo off");
            } else {
                out.println("#/bin/sh");
            }
            out.print("java -classpath \"");
            out.print(ProcessHelper.getRealClasspath());
            out.print("\" -Xmx");
            out.print(""+Runtime.getRuntime().maxMemory());
            out.print(" ");
            out.print(BSW.class.getName());
            out.print(" ");
            if (ProcessHelper.isWindows()) {
                out.append("%*");
            } else {
                out.append("@$");
            }
            out.println();
        }
        scriptFile.setExecutable(true);
    }

    public void newBatch() throws Exception {
        bsw.clearViewer();
        NewBook newBook = new NewBook();
        String result = newBook.getConfig();
        if (result != null) {
            mainFrame.getConfigEntry().setText(result);
            mainFrame.getConfigEntry().getCaret().setDot(result.length());
            bsw.getConfig(result);
        }
    }

    private static class UploadImages implements Batch {
        private UploadFile uploadFile;

        public UploadImages (UploadFile uploadFile) {
            this.uploadFile = uploadFile;
        }

        @Override
        public List<Future<Void>> getFutures(List<Operation> operations) {
            return new ArrayList<>();
        }

        @Override
        public void postOperation() throws Exception {
            uploadFile.performUpload();
        }
    }
}
