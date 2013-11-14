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

package net.sourceforge.bookscanwizard;

import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
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
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.media.jai.JAI;
import javax.media.jai.TileCache;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import net.sourceforge.bookscanwizard.gui.AboutDialog;
import net.sourceforge.bookscanwizard.gui.ConfigEntry;
import net.sourceforge.bookscanwizard.gui.GuiActions;
import net.sourceforge.bookscanwizard.gui.MainFrame;
import net.sourceforge.bookscanwizard.gui.MetadataGui;
import net.sourceforge.bookscanwizard.gui.UserFeedbackHelper;
import net.sourceforge.bookscanwizard.op.SaveToArchive;
import net.sourceforge.bookscanwizard.qr.ReadCodes;
import net.sourceforge.bookscanwizard.qr.SplitBooks;

/**
 * The main program
 */
public class BSW {
    /** If this is set to true, this enables features that are under development */
    public static final boolean EXPERIMENTAL = false;
    public static final Logger parentLogger = Logger.getLogger(BSW.class.getPackage().getName());
    private static final Logger logger = Logger.getLogger(BSW.class.getName());
    public static final RenderingHints QUALITY_HINTS = new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
    public static final RenderingHints SPEED_HINTS = new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    private static BSW instance;
    private static Path currentDirectory = new File(".").getAbsoluteFile().toPath();
    private static TileCache tileCache;
    private static final int threadLimit;
 
    public static double getPreviewScale() {
        return 1;
    }

    public static int getThreadCount() {
        return threadLimit;
    }

    private AtomicInteger completedCount = new AtomicInteger();

    private MainFrame mainFrame;
    private PreviewedImage previewedImage = new PreviewedImage();
    private boolean override = false;

    private File configFile;

    private GuiActions menuHandler = new GuiActions(this);

    private static boolean batchMode;
    private static boolean running;

    private static ExecutorService threadPool;
    /** Set to true if it is in the middle of an abort */
    private volatile boolean abort;
    private static final List<NewConfigListener> newConfigListeners = new ArrayList<>();
    private boolean inPreview;
    private List<Operation> previewOperations;
    
    public static ExecutorService getThreadPool() {
        return threadPool;
    }

    static {
        tileCache = JAI.getDefaultInstance().getTileCache();
        ImageIO.scanForPlugins();
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
        String limit = System.getProperty("bsw.thread_limit");
        if (limit == null) {
            limit = System.getenv("bsw.thread_limit");
        }
        if (limit != null) {
            threadLimit = Integer.parseInt(limit);
        } else {
            // This is a fairly wild guess on how many threads can be run
            // for a given amount of hash space.much memory it takes.
            threadLimit = (int) Math.min(Runtime.getRuntime().availableProcessors(), 
                    Runtime.getRuntime().maxMemory() / 500_000_000);
            logger.log(Level.INFO, "There are {0} processor(s), {1}M, {2} threads", 
                    new Object[]{Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().maxMemory()/1024/1024, threadLimit});
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            String arg = args[0];
            batchMode = true;
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
                return;
            } else if (arg.equals("-version")) {
                System.out.println("BookScanWizard, by Steve Devore, version "+AboutDialog.VERSION+".");
                return;
            }
            batchMode = false;
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
            JAI.getDefaultInstance().getTileCache().setMemoryCapacity(500_000_000);
        } catch (NoClassDefFoundError e) {
            UserFeedbackHelper.displayException(null, e);
        }

        final BSW wizard = new BSW();
        BSW.instance = wizard;
        final File file = getFileFromCurrentDir(configLocation);
        if (configLocation != null) {
            try {
                if (batchMode) {
                    String config = wizard.loadConfig(file);
                    long time = System.currentTimeMillis();
                    wizard.runBatch(config, null);
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
                       BSW.instance.getMenuHandler().newBatch();
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
            menuHandler.setMainFrame(mainFrame);
            mainFrame.getPageListBox().addActionListener(new UserFeedbackHelper() {
                @Override
                public void cursorActionPerformed(ActionEvent e) throws Exception {
                    FileHolder current = (FileHolder) mainFrame.getPageListBox().getSelectedItem();
                    previewedImage.setFileHolder((FileHolder) current);
                }
            });
        }
    }

    public void clearViewer() {
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
        FileReader fr;
        try {
            setCurrentDirectory(configFile.getParentFile().toPath());

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

    public void openConfig() throws Exception {
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

    public void saveConfig() throws IOException {
        if (configFile == null) {
            saveConfigAs();
        } else {
            try (PrintStream ps = new PrintStream(configFile)) {
                ps.print(mainFrame.getConfigEntry().getText());
            }
        }
    }

    public void saveConfigAs() throws IOException {
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

    public void preview() throws Exception {
        RenderedImage img = previewedImage.getPreviewProcessedImage();
        mainFrame.setImage(img, previewedImage.getPreviewHolder());
    }

    public GuiActions getMenuHandler() {
        return menuHandler;
    }

    public boolean isRunning() {
        return running;
    }

    public List<Operation>  getPreviewOperations() {
        return previewOperations;
    }

    public File getConfigFile() {
        return configFile;
    }
    
    public class ProcessImages implements Batch {
        private List<Operation> operations;

        @Override
        public void postOperation() throws Exception {
            for (Operation op : operations) {
                op.postOperation();
            }
        }

        @Override
        public List<Future<Void>> getFutures(final List<Operation> operations) {
            this.operations = operations;
            override = false;
            final ArrayList<Future<Void>> futures = new ArrayList<>();
            PageSet ps = operations.get(operations.size() -1).getPageSet();
            File destinationDir = ps.getDestinationDir();
            if (destinationDir == null) {
                throw new UserException("The destination is not defined.\nPlease define it with the SetDestination operation.");
            }
            destinationDir.mkdirs();
            List<FileHolder> files = PageSet.getSourceFiles();
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
                        processFile(operations, holder);
                        return null;
                    }
                };
                futures.add(threadPool.submit(task));
            }
            return futures;
        }
    }
    
    public void runBatchList(List<File> configFiles) throws Exception {
        System.out.println("run batch list");
        for (File file : configFiles) {
            String config = loadConfig(file);
            runBatch(config, null);
        }
    }

    @SuppressWarnings("CallToThreadRun")
    public synchronized void runBatch(String configText, Batch batchOverride) throws Exception {
        final Batch batch;
        if (batchOverride == null) {
            batch = getNewProcessImages();
        } else {
            batch = batchOverride;
        }
        abort = false;
        logger.fine("running....");
        final List<Operation> operations = getConfig(configText);
        // Create the threadPool with a bit lower than normal priority
        threadPool = Executors.newFixedThreadPool(threadLimit, 
                     new BSWThreadFactory(BSWThreadFactory.LOW_PRIORITY));
        if (operations.isEmpty()) {
            throw new UserException("Add the source directory of the book with the LoadImages operation");
        }

        tileCache.flush();
        final List<Future<Void>> futures = batch.getFutures(operations);

        if (!isBatchMode()) {
            JProgressBar bar = getMainFrame().getProgressBar();
            getMainFrame().setStatusLabel("Running...");
            bar.setVisible(true);
            bar.setMinimum(0);
            bar.setMaximum(futures.size()* (Operation.getMaxPass() - Operation.getMinPass() + 1));
            completedCount.set(0);
        }

        // futures.
        running = true;
        if (!isBatchMode()) {
            mainFrame.setStatus("Abort");
        }
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
               try {
                   for (int i=Operation.getMinPass(); i <= Operation.getMaxPass(); i++) {
                       Operation.setCurrentPass(i);
                       for (Future f : futures) {
                            f.get();
                            if (abort) {
                                throw new UserException("Aborted");
                            }
                        }
                   }
                    // attempt to encourage the releasing of file handles
                    System.gc();
                    if (!abort) {
                        batch.postOperation();
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

    
    private void processFile(List<Operation> operations, FileHolder holder) throws Exception {
        RenderedImage image = Operation.performOperations(holder, operations);
        if (image != null) {
            tileCache.removeTiles(image);
        }
        if (!isBatchMode()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    getMainFrame().getProgressBar().setValue(completedCount.incrementAndGet());
                }
            });
        }
    }

    public static boolean isBatchMode() {
        return batchMode;
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
     * @param path
     * @return
     */
    public static File getFileFromCurrentDir(String path) {
        return currentDirectory.resolve(path).toFile();
    }

    /**
     * This will return the directory that the configuration file is in.
     */
    public static File getCurrentDirectory() {
        return currentDirectory.toFile();
    }

    public static void setCurrentDirectory(Path currentDirectory) {
        BSW.currentDirectory = currentDirectory;
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

    public RenderedImage getConfigImage() throws Exception {
        RenderedImage img = previewedImage.getPreviewProcessedImage();
        Point2D[] pts = mainFrame.getViewerPanel().getPreviewCrop();
        if (pts!= null) {
            Rectangle2D bounds = new Rectangle2D.Double();
            bounds.setFrameFromDiagonal(pts[0], pts[1]);
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

    public class PreviewedImage {
        private FileHolder previewHolder;
        private RenderedImage previewImage;
        private RenderedImage previewProcessedImage;
        private String configEntry;

        public void setFileHolder(FileHolder fileHolder) {
            if (fileHolder != null) {
                if (previewHolder != fileHolder && (previewHolder == null || previewHolder != fileHolder)) {
                    previewHolder = fileHolder;
                    previewImage = null;
                    previewProcessedImage = null;
                    getMainFrame().getPageListBox().setSelectedItem(fileHolder);
                    try {
                        preview();
                    } catch (Exception ex) {
                        Logger.getLogger(BSW.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    getMainFrame().getThumbTable().updateSelection();
                }
            } else {
                configEntry = "";
                mainFrame.setImage(null, null);
            }
        }
        
        private void checkConfig() throws Exception {
            String newConfig = getConfigEntry().getConfigToPreview();
            configEntry = newConfig;
            previewOperations = BSW.this.getConfig(configEntry);
            previewProcessedImage = null;
        }

        public RenderedImage getPreviewProcessedImage() throws Exception {
            inPreview = true;
            try {
                checkConfig();
                if (previewProcessedImage == null) {
                    RenderedImage img = getPreviewImage();
                    getMainFrame().getViewerPanel().clearPoints();
                    
                    boolean isCursorAfterConfig = getConfigEntry().isCursorAfterConfig();
                    img = Operation.previewOperations(previewOperations, previewHolder, img, isCursorAfterConfig);
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
                    previewImage = previewHolder.getImage();
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

    public PreviewedImage getPreviewedImage() {
        return previewedImage;
    }

    public void fireNewConfigListeners() {
        for (NewConfigListener listener : newConfigListeners) {
            listener.newConfig();
        }
    }
    public void prepareMetadata() throws Exception {
        getConfig(getConfigEntry().getText());
        JDialog dialog = new MetadataGui(mainFrame, true);
        dialog.setVisible(true);
    }

    public ProcessImages getNewProcessImages() {
        return new ProcessImages();
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
            "         black and white image.  Default is 35.\n"+
            "      -scale: If this is defined, the images will be scaled down before\n"+
            "         scanning, which will make it faster.  Default is 1 (no scaling).\n"+
            "   -barcodes [-threshold n] [-scale n] directory\n"+
            "       This will search the images in a directory for barcodes and save any\n"+
            "       codes it finds into the file barcodes.csv in the same directory.\n"+
            "      -threshold: is the value from 0-100 that will be used to convert it to a\n"+
            "         black and white image.  Defualt is 35.\n"+
            "      -scale: If this is defined, the images will be scaled down before\n"+
            "         scanning, which will make it faster.  Default is 1 (no scaling).\n"+
            "   -upload filename.zip accessKey secretKey:  Uploads this zip file to\n"+
            "         archive.org\n"+
            "      filename.zip:  The zip file that contains the images and the meta.xml\n"+
            "         file\n"+
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
