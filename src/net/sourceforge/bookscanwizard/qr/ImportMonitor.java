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

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import net.sourceforge.bookscanwizard.UserException;
import net.sourceforge.bookscanwizard.gui.ImportImages;
import net.sourceforge.bookscanwizard.util.Sequence;

/**
 * Monitors a directory for new scans.
 */
public class ImportMonitor implements Runnable {

    private WatchService watcher;
    private HashMap<WatchKey, Info> keyMap = new HashMap<WatchKey, Info>();
    private final Set<File> toProcess =
            Collections.synchronizedSet(new HashSet<File>());
    private final Map<File, Collection<QRData>> processed =
            Collections.synchronizedMap(new TreeMap<File, Collection<QRData>>());
    private File source;
    private File destination;
    private boolean useLR = false;
    private String title;
    private Thread monitorThread = new Thread(this);
    private boolean exitThread;

    public void setSource(File source) throws IOException {
        if (!source.equals(this.source)) {
            this.source = source;
            Path dir = source.toPath();
            useLR = dir.resolve("l").toFile().isDirectory();
            if (watcher != null) {
                watcher.close();
            }
            watcher = FileSystems.getDefault().newWatchService();
            synchronized (toProcess) {
                if (useLR) {
                    Path path = dir.resolve("r");
                    WatchKey key = path.register(watcher, ENTRY_CREATE, ENTRY_MODIFY);
                    keyMap.put(key, new Info(path));
                    toProcess.addAll(Arrays.asList(path.toFile().listFiles()));
                }
                Path path = dir.resolve("l");
                WatchKey key = path.register(watcher, ENTRY_CREATE, ENTRY_MODIFY);
                keyMap.put(key, new Info(path));
                toProcess.addAll(Arrays.asList(path.toFile().listFiles()));
            }
        }
    }

    public void setDestination(File destination) {
        this.destination = destination;
        destination.mkdirs();
    }

    public void setMonitor(boolean monitor) {
        validate();
        if (monitor && !exitThread) {
            if (!monitorThread.isAlive()) {
                monitorThread.start();
            }
        } else if (monitor) {
            exitThread = true;
            monitorThread.interrupt();
        }
    }

    public void run() {
        try {
            processEvents();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void forceBookImport() throws IOException {
        validate();
        importBook(true);
    }

    public synchronized void importBook(boolean force) throws IOException {
        TreeSet<File> files = new TreeSet<File>(toProcess);
        ReadCodes readCodes = new ReadCodes(files);
        for (File f : files) {
            try {
                if (f.exists()) {
                    processed.put(f, readCodes.getCodes(f));
                }
                toProcess.remove(f);
            } catch (Exception e) {
                System.out.println(e);
                if (!force) {
                    return;
                }
                processed.put(f, readCodes.getCodes(f));
            }
        }
        for (;;) {
            if (useLR) {
                TreeSet<File> left = getNextBookPages(new File(source, "l"), true, force);
                TreeSet<File> right = getNextBookPages(new File(source, "r"), false, force);
                if (left == null || right == null) {
                    break;
                }
                processed.keySet().removeAll(left);
                processed.keySet().removeAll(right);
                File dest = newBookPath();
                Path p = dest.toPath().resolve("l");
                for (File f : left) {
                    Files.move(f.toPath(), p.resolve(f.getName()));
                }
                p = p.resolveSibling("r");
                for (File f : right) {
                    Files.move(f.toPath(), p.resolve(f.getName()));
                }
            } else {
                TreeSet<File> book = getNextBookPages(destination, false, force);
                if (book == null) {
                    break;
                }
                processed.keySet().removeAll(book);
                Path dest = newBookPath().toPath();
                for (File f : book) {
                    Files.move(f.toPath(), dest.resolve(f.getName()));
                }
            }
        }
    }

    private File newBookPath() {
        if (title == null) {
            File[] currentDirs = destination.listFiles(titleFilter());
            Arrays.sort(currentDirs);
            if (currentDirs.length > 0) {
                title = nextTitle(currentDirs[currentDirs.length - 1].getName());
            } else {
                title = "Title_001";
            }
        }
        File f = new File(destination, title);
        title = null;
        if (useLR) {
            new File(f, "l").mkdirs();
            new File(f, "r").mkdirs();
        } else {
            f.mkdirs();
        }
        return f;
    }

    public TreeSet<File> getNextBookPages(File directory, final boolean left, boolean force) {
        // right pages include the page ending, left pages do not (it is included
        //in the next book.
        boolean foundEnd = false;
        boolean normalPagesFound = false;
        TreeSet<File> newList = new TreeSet<File>();
        String dir = directory.getAbsolutePath();
        int ct = 0;
        for (Map.Entry<File, Collection<QRData>> entry : processed.entrySet()) {
            File f = entry.getKey();
            if (f.getAbsolutePath().startsWith(dir)) {
                ct++;
                if (!left) {
                    newList.add(f);
                }
                boolean foundCode = false;
                for (QRData value : entry.getValue()) {
                    if (value.getCode().equals(QRCodeControls.END.name()) && normalPagesFound) {
                        foundEnd = true;
                        foundCode = true;
                        break;
                    } else if (value.getCode().startsWith("Title: ")) {
                        title = value.getCode().substring("Title: ".length());
                        foundCode = true;
                    }
                }
                if (foundEnd) {
                    break;
                }
                if (!foundCode) {
                    normalPagesFound = true;
                }
                if (left) {
                    newList.add(f);
                }
            }
        }
        if (left) {
            ImportImages.getInstance().getImportData().setLeftImageCount(ct + "");
        } else {
            ImportImages.getInstance().getImportData().setRightImageCount(ct + "");
        }
        if (newList.isEmpty() || (!foundEnd && !force)) {
            newList = null;
        }
        return newList;
    }

    private int getImageNumber(String name) {
        int pos = name.lastIndexOf(".");
        return Integer.parseInt(name.substring(4, pos));
    }

    /**
     * Process all events for keys queued to the watcher
     */
    void processEvents() throws IOException {
        importBook(false);
        while (!exitThread) {
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException x) {
                return;
            }
            Info info = keyMap.get(key);
            Path dir = info.getPath();
            for (WatchEvent<?> event : key.pollEvents()) {
                Path name = (Path) event.context();
                File file = dir.resolve(name).toFile();
                System.out.println("newFile: " + file);
                info.addFile(file);
            }

            // reset the key
            boolean valid = key.reset();
            if (!valid) {
                throw new RuntimeException("Lost monitor");
            }
            importBook(false);
        }
        exitThread = false;
    }

    public static void main(String[] args) throws IOException {
        ImportMonitor monitor = new ImportMonitor();
        monitor.setDestination(new File("c:/test/processing"));
        monitor.setSource(new File("c:/test"));
        monitor.setMonitor(true);
    }

    public static FilenameFilter titleFilter() {
        return new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return dir.isDirectory() && name.toLowerCase().startsWith("title_");
            }
        };
    }

    private String nextTitle(String name) {
        int pos = "Title_".length();
        int next = Integer.parseInt(name.substring(pos)) + 1;
        return new Sequence("Title_###", next, 1).next();
    }

    public void setTitle(String title) {
        if (!title.trim().isEmpty()) {
            File f = new File(destination, title);
            if (!f.exists()) {
                this.title = title;
            } else {
                System.err.println(title + " already exists");
            }
        }
    }

    private void validate() {
        if (!source.isDirectory()) {
            throw new UserException(source + " does not exist");
        }
        if (!destination.isDirectory()) {
            throw new UserException(destination + " is not a valid path");
        }
    }

    private class Info {
        private Path path;
        private String nextName;
        private String prefix;
        private String suffix;
        private TreeMap<String, File> files = new TreeMap<String, File>();

        public Info(Path path) {
            this.path = path;
        }

        /**
         * Eye-fi doesn't necessarily transfer the files in the right order. So
         * we wait for missing pages before continuing.
         */
        public void addFile(File file) throws IOException {
            boolean found = false;
            String name = file.getName();
            files.put(name, file);
            if (prefix == null) {
                prefix = name.substring(0, 4);
                int pos = name.lastIndexOf(".");
                suffix = name.substring(pos);
                nextName = name;
            }
            for (;;) {
                File f = files.get(nextName);
                if (f == null) {
                    break;
                }
                if (f == null || !f.canRead() || !f.canWrite()) {
                    break;
                }
                found = true;
                System.out.println("  ready: " + file);
                // just in case a file snuck in somehow, add file up to the
                // desired file.  Usually it will be just the one file.
                for (Iterator<File> it = files.values().iterator(); it.hasNext();) {
                    File addFile = it.next();
                    toProcess.add(addFile);
                    it.remove();
                    if (addFile == f) {
                        break;
                    }
                }
                // calculate the next filename.
                String seq = (Integer.toString(10001 + getImageNumber(nextName))).substring(1);
                nextName = prefix + seq + suffix;
            }
            if (found) {
                importBook(false);
            }
        }

        public Path getPath() {
            return path;
        }
    }
}
