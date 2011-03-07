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

package net.sourceforge.bookscanwizard;

import net.sourceforge.bookscanwizard.op.SaveToArchive;
import javax.media.jai.TileCache;
import net.sourceforge.bookscanwizard.op.Pages;
import java.util.regex.Matcher;
import net.sourceforge.bookscanwizard.config.ConfigBalancedAutoLevels;
import java.util.logging.LogRecord;
import net.sourceforge.bookscanwizard.util.ProcessHelper;
import com.sun.media.jai.codec.TIFFEncodeParam;
import com.sun.media.jai.codec.TIFFField;
import java.awt.Event;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.event.ActionEvent;
import java.awt.image.RenderedImage;
import java.io.IOException;

import javax.media.jai.JAI;

import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.renderable.ParameterBlock;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import javax.imageio.ImageIO;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.text.Document;
import net.sourceforge.bookscanwizard.config.ConfigAutoLevels;
import net.sourceforge.bookscanwizard.config.ConfigGrayCard;
import net.sourceforge.bookscanwizard.op.Barcodes;
import net.sourceforge.bookscanwizard.op.EstimateDPI;
import net.sourceforge.bookscanwizard.op.NormalizeLighting;
import net.sourceforge.bookscanwizard.op.WhiteBalance;
import net.sourceforge.bookscanwizard.qr.PrintCodes;
import net.sourceforge.bookscanwizard.qr.PrintCodesDialog;
import net.sourceforge.bookscanwizard.qr.ReadCodes;
import net.sourceforge.bookscanwizard.qr.SplitBooks;
import net.sourceforge.bookscanwizard.start.NewBook;
import static com.sun.media.imageio.plugins.tiff.BaselineTIFFTagSet.TAG_X_RESOLUTION;
import static com.sun.media.imageio.plugins.tiff.BaselineTIFFTagSet.TAG_Y_RESOLUTION;
import static com.sun.media.imageio.plugins.tiff.BaselineTIFFTagSet.TAG_RESOLUTION_UNIT;
import static com.sun.media.imageio.plugins.tiff.BaselineTIFFTagSet.RESOLUTION_UNIT_INCH;

/**
 * The main program
 */
public class BSW {
    public static final Logger parentLogger = Logger.getLogger(BSW.class.getPackage().getName());
    private static final Logger logger = Logger.getLogger(BSW.class.getName());
    public static final RenderingHints QUALITY_HINTS = new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
    public static final RenderingHints SPEED_HINTS = new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    private static BSW instance;
    private static File currentDirectory = new File(".").getAbsoluteFile();
    private static TileCache tileCache = JAI.getDefaultInstance().getTileCache();

    private AtomicInteger completedCount = new AtomicInteger();

    private MainFrame mainFrame;
    private PreviewedImage previewedImage = new PreviewedImage();

    private static double previewScale = 1;
    private float postScale = 1F;
    private File configFile;

    private MenuActionListener menuHandler = new MenuActionListener();
    private static boolean batchMode;
    private static boolean running;

    private static ExecutorService threadPool;
    /** Set to true if it is in the middle of an abort */
    private volatile boolean abort;
    private static final List<NewConfigListener> newConfigListeners = new ArrayList<NewConfigListener>();
    private boolean inPreview;

    static {
        LogManager.getLogManager().reset();
        Logger.getLogger("").setLevel(Level.OFF);
        parentLogger.setLevel(Level.INFO);
        Handler handler = new ConsoleHandler();
        handler.setFormatter(new Formatter(){
            @Override
            public String format(LogRecord record) {
                String name = "";
                try {
                    name = Class.forName(record.getSourceClassName()).getSimpleName();
                } catch (ClassNotFoundException ex) {
                }
                return name + " " + formatMessage(record) + "\n";
            }
        });
        parentLogger.addHandler(handler);
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            String arg = args[0];
            if (arg.equals("-mergelr") && args.length == 3) {
                MatchPages.main(new String[] {"-move", args[1], args[2]});
                return;
            } else if (arg.equals("-split")) {
                SplitBooks.main(Arrays.copyOfRange(args, 1, args.length));
                return;
            } else if (arg.equals("-barcodes")) {
                ReadCodes.main(Arrays.copyOfRange(args, 1, args.length));
                return;
            } else if (arg.equals("-upload")) {
                SaveToArchive saveToArchive = new SaveToArchive();
                saveToArchive.saveToArchive(Arrays.copyOfRange(args, 1, args.length));
            } else if (arg.equals("-version")) {
                System.out.println("BookScanWizard, by Steve Devore, version "+AboutDialog.VERSION+".");
                return;
            }
        }

        String configLocation = "book.bsw";
        for (String arg : args) {
            if (arg.equals("-batch")) {
                batchMode = true;
            } else if (arg.equals("-nocfg")) {
                configLocation = null;
            } else if (arg.startsWith("-")){
                usage();
                System.exit(1);
            } else {
                configLocation = arg;
            }
        }
        try {
            JAI.getDefaultInstance().getTileCache().setMemoryCapacity(500000000);
        } catch (NoClassDefFoundError e) {
            UserFeedbackHelper.displayException(null, e);
        }

        final BSW wizard = new BSW();
        BSW.instance = wizard;
        final File file = new File(configLocation);
        if (configLocation != null) {
            try {
                if (batchMode) {
                    String config = wizard.loadConfig(file);
                    long time = System.currentTimeMillis();
                    wizard.runBatch(config);
                    logger.log(Level.INFO, "Total elapsed time: {0} seconds.", ((System.currentTimeMillis() - time) / 1000));
                    System.exit(0);
                }
            } catch (Exception e) {
                UserFeedbackHelper.displayException(instance().mainFrame, e);
            }
        }
        SwingUtilities.invokeLater(new Runnable(){
            @Override
            public void run() {
                PrefsHelper.loadPreferences();
                instance().mainFrame.setVisible(true);
                try {
                    if (file.isFile()) {
                        wizard.loadConfig(file);
                    } else {
                       wizard.newBatch();
                    }
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            UserFeedbackHelper helper = new UserFeedbackHelper() {
                                @Override
                                public void cursorActionPerformed(ActionEvent e) throws Exception {
                                    wizard.preview();
                                }
                            };
                            helper.run();
                        }
                    });
                } catch (Exception e) {
                    UserFeedbackHelper.displayException(instance().mainFrame, e);
                }
            }
        });
    }
    public BSW() {
        if (!batchMode) {
            mainFrame = new MainFrame(menuHandler);
            mainFrame.getPageListBox().addActionListener(new UserFeedbackHelper() {
                @Override
                public void cursorActionPerformed(ActionEvent e) throws Exception {
                    previewedImage.setFileHolder((FileHolder) mainFrame.getPageListBox().getSelectedItem());
                    preview();
                }
            });
        }
    }

    private void clearViewer() {
        if (!isBatchMode()) {
            mainFrame.getConfigEntry().setText("");
            mainFrame.setPageList(Collections.EMPTY_LIST);
            previewedImage.setFileHolder(null);
            fireNewConfigListeners();
        }
    }

    private String loadConfig(File configFile) throws Exception {
        clearViewer();
        String result="";
        this.configFile = configFile;
        FileReader fr = null;
        try {
            setCurrentDirectory(configFile.getAbsoluteFile().getParentFile());

            fr = new FileReader(configFile);
            BufferedReader reader = new BufferedReader(fr);
            StringBuilder str = new StringBuilder();
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                str.append(line).append("\n");
            }
            result = str.toString();
            if (!isBatchMode()) {
                mainFrame.getConfigEntry().setText(result);
                mainFrame.getConfigEntry().getCaret().setDot(result.length());
                BSW.this.getConfig(result);
            }
        } catch (FileNotFoundException e) {
            if (isBatchMode()) {
                throw e;
            }
            // otherwise ignore.
        }
        return result;
    }

    private void openConfig() throws Exception {
        File file = configFile;
        if (file == null) {
            file = getFileFromCurrentDir("book.bsw");
        }
        final JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Open Configuration");
        fc.setCurrentDirectory(file.getCanonicalFile().getParentFile());

        fc.setSelectedFile(file);
        fc.setApproveButtonText("Open");

        int returnVal = fc.showOpenDialog(mainFrame);
        if (returnVal != JFileChooser.APPROVE_OPTION) {
            return;
        }
        loadConfig(fc.getSelectedFile());
        preview();
    }

    private void saveConfig() throws IOException {
        if (configFile == null) {
            saveConfigAs();
        } else {
            PrintStream ps = new PrintStream(configFile);
            ps.print(mainFrame.getConfigEntry().getText());
            ps.close();
        }
    }

    private void saveConfigAs() throws IOException {
        File file = configFile;
        if (file == null) {
            file = getFileFromCurrentDir("book.bsw");
        }
        final JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Configuration");
        fc.setCurrentDirectory(file.getCanonicalFile().getParentFile());

        fc.setSelectedFile(file);
        fc.setApproveButtonText("Save");

        int returnVal = fc.showOpenDialog(mainFrame);
        if (returnVal != JFileChooser.APPROVE_OPTION) {
            return;
        }
        configFile = fc.getSelectedFile();
        saveConfig();
    }

    public List<Operation> getConfig(String config) throws Exception {
        return Operation.getOperations(config);
    }

    private void preview() throws Exception {
        RenderedImage img = previewedImage.getPreviewProcessedImage();
        mainFrame.setImage(img, previewedImage.getPreviewHolder());
    }

    private class MenuActionListener extends UserFeedbackHelper {
        @Override
        public void cursorActionPerformed(ActionEvent e) throws Exception {
            String cmd = e.getActionCommand();
            if ("abort".equals(cmd)) {
                abort();
                return;
            }
            if (running) {
                throw new UserException("A process is running.  Either cancel the process or wait for it to complete before continuing.");
            }
            if ("new".equals(cmd)) {
                newBatch();
            } else if ("open".equals(cmd)) {
                openConfig();
            } else if ("save".equals(cmd)) {
                saveConfig();
            } else if ("save_as".equals(cmd)) {
                saveConfigAs();
            } else if ("exit".equals(cmd)) {
                System.exit(0);
            } else if ("preview".equals(cmd)) {
                preview();
            } else if ("run".equals(cmd)) {
                runBatch(mainFrame.getConfigEntry().getText());
            } else if ("previousPage".equals(cmd)) {
                int inc = ((e.getModifiers() & Event.SHIFT_MASK) != 0) ? 2 : 1;
                if (mainFrame.getPageListBox().getSelectedIndex() - inc < 0) {
                    throw new UserException("You are at the first image");
                }
                mainFrame.getPageListBox().setSelectedIndex(mainFrame.getPageListBox().getSelectedIndex() - inc);
            } else if ("nextPage".equals(cmd)) {
                int inc = ((e.getModifiers() & Event.SHIFT_MASK) != 0) ? 2 : 1;
                if (mainFrame.getPageListBox().getSelectedIndex() + inc >= mainFrame.getPageListBox().getItemCount()) {
                    throw new UserException("You are at the last image");
                }
                mainFrame.getPageListBox().setSelectedIndex(mainFrame.getPageListBox().getSelectedIndex() + inc);
            } else if ("zoomIn".equals(cmd)) {
                setPostScale(postScale * 1.5F);
                preview();
            } else if ("zoomOut".equals(cmd)) {
                setPostScale(postScale / 1.5F);
                preview();
            } else if ("zoom".equals(cmd)) {
                setPostScale((float) (postScale * getMainFrame().getViewerPanel().getZoom()));
                preview();
            } else if ("about".equals(cmd)) {
                mainFrame.getAboutDialog().setVisible(true);
            } else if ("command_helper".equals(cmd)) {
                mainFrame.getOperationList().setVisible(true);
            } else if ("perspective_and_crop".equals(cmd)) {
               insertCoordinates("PerspectiveAndCrop =");
            } else if ("perspective".equals(cmd)){
                insertCoordinates("Perspective =");
            } else if ("rotate".equals(cmd)) {
                insertCoordinates("Rotate =");
            } else if ("crop".equals(cmd)) {
                insertCoordinates("Crop =");
            } else if ("crop_and_scale".equals(cmd)) {
                cropAndScale();
            } else if ("copy_points_to_viewer".equals(cmd)) {
                mainFrame.getViewerPanel().setPointDef(getConfigEntry().getCurrentLineOrSelection());
            } else if ("create_script".equals(cmd)) {
                createScript();
            } else if ("auto_levels".equals(cmd)) {
                autoLevels(false);
            } else if ("auto_rgb_levels".equals(cmd)) {
                autoLevels(true);
            } else if ("gray_card".equals(cmd)) {
                String config = new ConfigGrayCard().getConfig(getConfigImage());
                insertConfig(config, false, true);
            } else if ("remove_page".equals(cmd)) {
                removePage();
            } else if ("white_balance".equals(cmd)) {
                String config = WhiteBalance.getConfig(getConfigImage());
                insertConfig(config, false, true);
            } else if ("balanced_normalize_lighting".equals(cmd)) {
                String config = new ConfigBalancedAutoLevels().getConfig(getConfigImage());
                insertConfig(config, false, true);
            } else if ("normalize_lighting".equals(cmd)) {
                normalizeLighting();
            } else if ("keystone_barcodes".equals(cmd)) {
                PrintCodes.keystoneCodes();
            } else if ("print_qr_codes".equals(cmd)) {
                JDialog dialog = new PrintCodesDialog(getMainFrame(), false);
                dialog.setVisible(true);
            } else if ("upload".equals(cmd)) {
                upload();
            } else if ("expand_barcode_operations".equals(cmd)) {
                preview();
                insertConfigNoPreview(Barcodes.getConfiguration(), true, false);
            } else if ("preview_to_cursor".equals(cmd)) {
                preview();
            } else if ("save_dpi".equals(cmd)) {
                preview();
                EstimateDPI.saveFocalLength();
            } else if ("op EstimateDPI".equals(cmd)) {
                String dpiInfo = EstimateDPI.getConfig();
                if (dpiInfo == null) {
                    throw new UserException("No DPI information is saved");
                }
                insertConfigNoPreview(dpiInfo, false, false);
            } else if (cmd.startsWith("op ")) {
                insertConfigNoPreview(cmd.substring(3)+" = ", false, false);
            } else {
                throw new UserException("Unknown action type: " + cmd);
            }
        }

        private void cropAndScale() throws Exception {
            String text = mainFrame.getViewerPanel().getPointDef();
            if (text.length() > 0) {
                text = "CropAndScale = "+ text+ " "+
                        mainFrame.getViewerPanel().getXCropScale() + " " +
                        mainFrame.getViewerPanel().getYCropScale() + " # " +
                        previewedImage.getPreviewHolder().toString();
                insertConfig(text, false, true);
            }
        }

        private void insertCoordinates(String prefix) throws Exception {
            String text = mainFrame.getViewerPanel().getPointDef();
            if (text.length() > 0) {
                text = prefix+" "+ text+" # "+previewedImage.getPreviewHolder().toString();
                insertConfig(text, false, true);
             }
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
            PrintWriter out = new PrintWriter(new FileWriter(scriptFile));
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
            out.close();
            scriptFile.setExecutable(true);
        }

    }

    public synchronized void runBatch(String configText) throws Exception {
        boolean override = false;
        abort = false;
        logger.fine("running....");
        setPreviewScale(1);
        final List<Operation> operations = getConfig(configText);
        // Create the threadPool with a bit lower than normal priority
        threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), new BSWThreadFactory());
        if (operations.isEmpty()) {
            throw new UserException("Add the source directory of the book with the LoadImages operation");
        }

        PageSet ps = operations.get(operations.size() -1).getPageSet();
        File destinationDir = ps.getDestinationDir();
        if (destinationDir == null) {
            throw new UserException("The destination is not defined.\nPlease define it with the SetDestination operation.");
        }
        destinationDir.mkdirs();
        List<FileHolder> files = PageSet.getSourceFiles();
        tileCache.flush();
        if (!isBatchMode()) {
            JProgressBar bar = getMainFrame().getProgressBar();
            getMainFrame().setStatusLabel("Running...");
            bar.setVisible(true);
            bar.setMinimum(0);
            bar.setMaximum(files.size());
            completedCount.set(0);
        }
        final ArrayList<Future<Void>> futures = new ArrayList<Future<Void>>();
        for (final FileHolder holder : files) {
            String page = holder.getName();
            final File destFile = new File(destinationDir, page + ".tif");
            if (destFile.exists()) {
                if (!override) {
                    if (!isBatchMode()) {
                        int confirm = JOptionPane.showConfirmDialog(mainFrame.getFocusOwner(), "Some output files aleady exist.  Do you want to overwrite them?", "Files exist", JOptionPane.YES_NO_OPTION);
                        if (confirm != JOptionPane.YES_OPTION) {
                            throw new UserException("Submit aborted");
                        }
                        override = true;
                    } else {
                        throw new UserException("File already exists: "+destFile);
                    }
                } else {
                    destFile.delete();
                }
            }
            Callable<Void> task = new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    processFile(operations, holder, destFile);
                    return null;
                }
            };
            futures.add(threadPool.submit(task));
        }
        running = true;
        if (!isBatchMode()) {
            mainFrame.setStatus("Abort");
        }
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
               try {
                    for (Future f : futures) {
                        f.get();
                        if (abort) {
                            throw new UserException("Aborted");
                        }
                    }
                    // attempt to encourage the releasing of file handles
                    System.gc();
                    if (!abort) {
                        for (Operation op : operations) {
                            op.postOperation();
                        }
                    }
                } catch (Exception e) {
                    threadPool.shutdownNow();
                    UserFeedbackHelper.displayException(mainFrame, e);
                } finally {
                    tileCache.flush();
                    System.gc();
                   if (!isBatchMode()) {
                       SwingUtilities.invokeLater(new Runnable(){
                            @Override
                            public void run() {
                                mainFrame.setStatus(null);
                                mainFrame.getProgressBar().setVisible(false);
                            }
                        });
                        running = false;
                   }
                }
           }
        };
        Thread thread = new Thread(runnable);
        if (isBatchMode()) {
            thread.run();
        } else {
            thread.start();
        }
    }

    public void abort() {
        abort = true;
        SaveToArchive.abortRequested();
        threadPool.shutdownNow();
        try {
            threadPool.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            logger.warning("Could not stop queue");
        }
    }

    private void insertConfigNoPreview(String origText, boolean replace, boolean ensurePosition) throws Exception {
        String text = origText;
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
        String lastPageLine = null;
        for (String line : (config.getDocument().getText(0, end)+text).split("\n")) {
            Matcher matcher = Operation.MATCH_OP.matcher(line);
            if (matcher.matches() && matcher.group(1).equals("Pages")) {
                lastPageLine = line;
            }
        }
        if (lastPageLine != null) {
            Pages pages = (Pages) Operation.getStandaloneOp(lastPageLine);
            if ((ensurePosition && pages.getPosition() != previewedImage.getPreviewHolder().getPosition()) ||
                !pages.getPageSet().getFileHolders().contains(previewedImage.getPreviewHolder()))
            {
                String pos;
                if (ensurePosition) {
                    pos = (previewedImage.getPreviewHolder().getPosition()
                          == FileHolder.LEFT ? "left" : "right");
                } else {
                    pos = "all";
                }
                text = "Pages = " +pos+"\n"+ text;
            }
        }
        document.insertString(end, text, null);
        config.setSelectionStart(end);
        config.setSelectionEnd(end + text.length());
        config.requestFocus();
    }

    private void insertConfig(String origText, boolean replace, boolean ensurePosition) throws Exception {
        insertConfigNoPreview(origText+"\n", replace, ensurePosition);

        Operation op = Operation.getStandaloneOp(origText);
        if (op instanceof PerspectiveOp) {
            if (mainFrame.isShowPerspective()) {
                getMainFrame().getViewerPanel().setPointDef("");
                preview();
            }
        } else if (op instanceof CropOp) {
            if (mainFrame.isShowCrops()) {
                getMainFrame().getViewerPanel().setPointDef("");
                preview();
            }
        } else if (op instanceof ColorOp) {
            if (mainFrame.isShowColors()) {
                preview();
            }
        } else {
            preview();
        }
    }

    private void processFile(List<Operation> operations, FileHolder holder, File destFile) throws Exception {
        RenderedImage image = null;
        Operation lastOp = null;
        for (Operation op : operations) {
            if (!holder.isDeleted() && op.getPageSet().getFileHolders().contains(holder)) {
                if (image == null) {
                    image = ImageIO.read(holder.getFile());
                }
                image = op.performOperation(holder, image);
                lastOp = op;
            }
        }
        if (image != null && !holder.isDeleted()) {
            saveFile(lastOp.getPageSet(), image, holder, destFile);
        }
        tileCache.removeTiles(image);
        if (!isBatchMode()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    getMainFrame().getProgressBar().setValue(completedCount.incrementAndGet());
                }
            });
        }
    }

    public void saveFile(PageSet pageSet, RenderedImage image, FileHolder holder, File file) {
        int dpi = PageSet.getDestinationDPI();
        if (dpi == 0) {
            dpi = (int) holder.getDPI();
        }
        // saves the image to a tiff file
        TIFFEncodeParam param = new TIFFEncodeParam();
        param.setCompression( pageSet.getCompressionType());

        TIFFField[] extras = new TIFFField[3];
        extras[0] = new TIFFField(TAG_X_RESOLUTION, TIFFField.TIFF_RATIONAL, 1, new long[][] {{dpi, 1},{0 ,0}});
        extras[1] = new TIFFField(TAG_Y_RESOLUTION, TIFFField.TIFF_RATIONAL, 1, new long[][] {{dpi, 1},{0 ,0}});
        extras[2] = new TIFFField(TAG_RESOLUTION_UNIT, TIFFField.TIFF_SHORT, 1, new char[] { RESOLUTION_UNIT_INCH});
        param.setExtraFields(extras);
        JAI.create("filestore", image, file.toString(), "TIFF", param);
        logger.log(Level.INFO, "saved {0}", file.getName());
    }

    private void newBatch() throws Exception {
        int n = JOptionPane.showConfirmDialog(getMainFrame(),
                "There is no config file in this directory.\nDo you want to use the Wizard to create one?",
                "Book Scan Wizard",
                JOptionPane.YES_NO_CANCEL_OPTION);
        if (n != JOptionPane.CANCEL_OPTION) {
            clearViewer();
            if (n == JOptionPane.YES_OPTION) {
                NewBook newBook = new NewBook();
                String result = newBook.getConfig();
                if (result != null) {
                    mainFrame.getConfigEntry().setText(result);
                    mainFrame.getConfigEntry().getCaret().setDot(result.length());
                    getConfig(result);
                }
            }
        }
    }

    public static void setPreviewScale(double previewScale) {
        BSW.previewScale = previewScale;
    }

    public static double getPreviewScale() {
        return isBatchMode() ? 1 : previewScale;
    }

    public static boolean isBatchMode() {
        return batchMode;
    }

    public float getPostScale() {
        return (float) previewScale * postScale;
    }

    public void setPostScale(float postScale) {
        postScale = Math.max(.01f, postScale);
        postScale = Math.min(2, postScale);
        this.postScale = postScale;
    }

    public boolean isInPreview() {
        return inPreview;
    }

    public static BSW instance() {
        return instance;
    }

    public ConfigEntry getConfigEntry() {
        return mainFrame.getConfigEntry();
    }

    /**
     * This will return an absolute path as-is, or else return the path
     * relative from the current directory.
     * @param f
     * @return
     */
    public static File getFileFromCurrentDir(String path) {
        File f = new File(path);
        if (f.isAbsolute()) {
            return f;
        }
        if (ProcessHelper.isWindows()) {
            // on Windows it isn't considered absolute unless it includes a drive
            // or is a unc path.. but for our purposes the following should
            // be considered absolute.
            if (path.contains(":") || f.getPath().startsWith(File.separator)) {
                return f;
            }
        }
        return new File(currentDirectory, path);
    }

    /**
     * This will return the directory that the configuration file is in.
     */
    public static File getCurrentDirectory() {
        return currentDirectory;
    }

    public static void setCurrentDirectory(File currentDirectory) {
        BSW.currentDirectory = currentDirectory.getAbsoluteFile();
    }

    public MainFrame getMainFrame() {
        return mainFrame;
    }

    static String getProperty(String key) {
        String value = System.getProperty(key);
        if (value == null) {
            value = System.getenv(key);
        }
        return value;
    }

    private void autoLevels(boolean separateRGB) throws Exception {
        String config = new ConfigAutoLevels().getConfig(getConfigImage(),separateRGB);
        insertConfig(config, false, false);
    }

    private void normalizeLighting() throws Exception {
        String config = new NormalizeLighting().getConfig(previewedImage.getPreviewHolder(), getConfigImage());
        insertConfig(config, false, true);
    }

    private RenderedImage getConfigImage() throws Exception {
        RenderedImage img = previewedImage.getPreviewProcessedImage();
        List<Point2D> pts = mainFrame.getViewerPanel().getPoints();
        if (pts.size() > 2) {
            throw new IllegalArgumentException("There should be 0 or 2 points selected");
        }
        if (pts.size() == 2) {
            Rectangle2D bounds = new Rectangle2D.Double();
            bounds.setFrameFromDiagonal(pts.get(0), pts.get(1));
            ParameterBlock pb = new ParameterBlock();
            pb.addSource(img);
            pb.add((float) bounds.getMinX());
            pb.add((float) bounds.getMinY());
            pb.add((float) bounds.getWidth());
            pb.add((float) bounds.getHeight());
            img =  JAI.create("crop", pb);
        }
        return img;
    }

    class PreviewedImage {
        private FileHolder previewHolder;
        private RenderedImage previewImage;
        private RenderedImage previewProcessedImage;
        private String configEntry;
        private boolean showCrops = true;
        private boolean showPerspective =true;
        private boolean showColors = true;

        public void setFileHolder(FileHolder fileHolder) {
            if (previewHolder == null ? fileHolder != null : !previewHolder.equals(fileHolder)) {
                previewHolder = fileHolder;
                previewImage = null;
                previewProcessedImage = null;
            }
            if (fileHolder == null) {
                configEntry = "";
                mainFrame.setImage(null, null);
            }
        }

        private void checkConfig() throws Exception {
            String newConfig = getConfigEntry().getConfigToPreview();
            if (!newConfig.equals(configEntry) 
                    || showCrops != mainFrame.isShowCrops() 
                    || showPerspective != mainFrame.isShowPerspective() 
                    || showColors != mainFrame.isShowColors() ) {
                showCrops = mainFrame.isShowCrops();
                showPerspective = mainFrame.isShowPerspective();
                showColors = mainFrame.isShowColors();
                configEntry = newConfig;
                BSW.this.getConfig(configEntry);
                previewProcessedImage = null;
            }
        }

        public RenderedImage getPreviewProcessedImage() throws Exception {
            inPreview = true;
            try {
                checkConfig();
                if (previewProcessedImage == null) {
                    RenderedImage img = getPreviewImage();
                    img = Operation.previewOperations(previewHolder, img);
                    if (PageSet.getSourceFiles() != null) {
                        previewProcessedImage = img;
                        mainFrame.setPageList(PageSet.getSourceFiles());
                    }
                }
            } finally {
                inPreview = false;
            }
            return previewProcessedImage;
        }

        public RenderedImage getPreviewImage() throws Exception {
            checkConfig();
            try {
                if (previewImage == null && previewHolder != null) {
                    previewImage = ImageIO.read(previewHolder.getFile());
                    if (getPreviewScale() != 1F) {
                        previewImage = JAI.create("SubsampleAverage",
                            previewImage, getPreviewScale(), getPreviewScale(), BSW.SPEED_HINTS);
                    }
                }
            } catch (Exception e) {
                previewImage = null;
                throw e;
            }
            return previewImage;
        }

        public FileHolder getPreviewHolder() {
            return previewHolder;
        }

        public String getConfig() {
            return configEntry;
        }
    }

    public static TileCache getTileCache() {
        return tileCache;
    }

    public void addNewConfigListener(NewConfigListener newConfigListener) {
        newConfigListeners.add(newConfigListener);
    }

    PreviewedImage getPreviewedImage() {
        return previewedImage;
    }

    public void fireNewConfigListeners() {
        for (NewConfigListener listener : newConfigListeners) {
            listener.newConfig();
        }
    }
    private void removePage() throws Exception {
        String line = getConfigEntry().getCurrentLineOrSelection();
        if (line.endsWith("\n")) {
            line = line.substring(0, line.length()-1);
        }
        if (line.contains("RemovePages")) {
            line = line + ", "+ previewedImage.getPreviewHolder().getName();
            insertConfig(line, true, false);
        } else {
            insertConfig("RemovePages = " + previewedImage.getPreviewHolder().getName(), false, false);
        }
    }

    private void upload() throws Exception {
        getConfig(getConfigEntry().getText());
        JDialog dialog = new ArchiveMetadata(mainFrame, true);
        dialog.setVisible(true);
    }

    private static void usage() {
        System.out.println(
            "Usage:\n"+
            "    config_name\n"+
            "      If no options are given, it will bring up the gui either using the \n"+
            "      configuration file given, or book.bsw if it is not defined.\n"+
            "\n"+
            " Options:\n"+
            "   -version Returns the version number of the software\n"+
            "   -mergelr [-ignoretimes] sourceFiles destination\n"+
            "       Merges images from separate left and right folders, where the folders\n"+
            "       are in 'l' and 'r'.\n"+
            "      -ignoretimes:  Ignore the timestamps when merging the files.\n"+
            "\n"+
            "   -split [-threshold n] [-scale n] source destination [subdirectory]\n"+
            "       This will use the 'Title' and 'End Book' barcodes to split a bunch\n"+
            "       of scans to seperate books, and save a barcodes.csv for each directory.\n"+
            "      -threshold: is the value from 0-100 that will be used to convert it to a\n"+
            "         black and white image.  Defualt is 118.\n"+
            "      -scale: If this is defined, the images will be scaled down before\n"+
            "         scanning, which will make it faster.  Default is 1 (no scaling).\n"+
            "   -barcodes [-threshold n] [-scale n] directory\n"+
            "       This will search the images in a directory for barcodes and save any\n"+
            "       codes it finds into the file barcodes.csv in the same directory.\n"+
            "      -threshold: is the value from 0-100 that will be used to convert it to a\n"+
            "         black and white image.  Defualt is 35.\n"+
            "      -scale: If this is defined, the images will be scaled down before\n"+
            "         scanning, which will make it faster.  Default is 1 (no scaling).\n"+
            "   -upload filename.zip accessKey secretKey:  Uploads this zip file to archive.org\n"+
            "       filename.zip:  The zip file that contains the images and the meta.xml file\n"+
            "      accessKey :  The user access key of the account to upload under\n"+
            "      secretKey : The secret key of the account\n"+
            "\n"+
            "   -batch config_name\n"+
            "       Runs the preprocessor from the command line without bring up the gui.\n"+
            "       If config_name is not specified, it will look for file book.bsw in the\n"+
            "       current directory.\n"
        );
    }
}
