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
package net.sourceforge.bookscanwizard.op;

import java.awt.geom.Point2D;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import net.sourceforge.bookscanwizard.BSW;
import net.sourceforge.bookscanwizard.FileHolder;
import net.sourceforge.bookscanwizard.Operation;
import net.sourceforge.bookscanwizard.PageSet;
import net.sourceforge.bookscanwizard.UserException;
import net.sourceforge.bookscanwizard.qr.PrintCodes;
import net.sourceforge.bookscanwizard.qr.QRCodeControls;
import net.sourceforge.bookscanwizard.qr.QRData;
import net.sourceforge.bookscanwizard.qr.ReadCodes;
import net.sourceforge.bookscanwizard.util.LazyHashMap;

public class Barcodes extends Operation {
    private static final Logger logger = Logger.getLogger(Barcodes.class.getName());
    private static String configuration;
    private static DecimalFormat whole = new DecimalFormat("#");

    private TreeSet<FileHolder> deleted;
    private List<FileHolder> holders;

    public static String getConfiguration() {
        return configuration;
    }

    @Override
    public List<Operation> setup(List<Operation> operationList) throws Exception {
        if (!QRData.isFoundBarcodeFile()) {
            String threshold = getNextArg("threshold");
            if (threshold != null) {
                ReadCodes.setThreshold(Double.parseDouble(threshold));
            }

            if (!BSW.isBatchMode()) {
                int confirm = JOptionPane.showConfirmDialog(BSW.instance().getMainFrame(), "The barcodes have not been scanned.  Do you wish to do that now?  it will take some time", "Barcodes not scanned", JOptionPane.YES_NO_OPTION);
                if (confirm != JOptionPane.YES_OPTION) {
                    throw new UserException("Please remove the Barcodes operation, or run -split or -barcodes");
                }
                BSW.instance().getMainFrame().setBusy(true);
            }
            ArrayList<File> files = new ArrayList<File>();
            for (FileHolder holder : PageSet.getSourceFiles()) {
                files.add(holder.getFile());
            }
            ReadCodes readCodes = new ReadCodes(files);
            List<QRData> codes = readCodes.getCodes();
            LazyHashMap<String,List<QRData>> saveMap = new LazyHashMap<String,List<QRData>>(ArrayList.class);
            for (FileHolder holder : pageSet.getSourceFiles()) {
                holder.setQrData(readCodes.getCodes(holder.getFile()));
                if (holder.getQRData() != null) {
                    saveMap.getOrCreate(holder.getFile().getParent()).addAll(holder.getQRData());
                }
            }
            for (Map.Entry<String,List<QRData>> entry : saveMap.entrySet()) {
                File destination = new File(entry.getKey(), "barcodes.csv");
                QRData.write(destination, entry.getValue());
            }
            if (!BSW.isBatchMode()) {
                BSW.instance().getMainFrame().setBusy(false);
            }
        }
        deleted = new TreeSet<FileHolder>();

        TreeSet<FileHolder> flag = new TreeSet<FileHolder>();
        TreeMap<FileHolder, String> colors = new TreeMap<FileHolder, String>();
        ArrayList<FileHolder> transformOps = new ArrayList<FileHolder>();
        ArrayList<FileHolder>  dpiHolders = new ArrayList<FileHolder>();
        holders = pageSet.getSourceFiles();
        for (int i = 0; i < holders.size(); i++) {
            FileHolder holder = holder = holders.get(i);
            if (holder.getQRData() != null && holder.getQRData().size() > 0) {
                deleted.add(holder);

                // Check for persective operations
                if (holder.getQRData().size() > 0 && holder.getQRData().get(0).getCode().startsWith("X_")) {
                    transformOps.add(holder);
                    continue;
                }

                for (QRData q : holder.getQRData()) {
                    // For title page codes, do nothing
                    String code = q.getCode();
                    if (code.startsWith("Title: ")) {
                        continue;
                    }

                    QRCodeControls ctrl = QRCodeControls.valueOf(code);
                    switch (ctrl) {
                        case SKIP:
                        case END:
                            break;
                        case REDO:
                            int pos = i - 3;
                            if (holder.getPosition() == FileHolder.LEFT) {
                                pos++;
                            }
                            deleteOtherPage(i);
                            deleted.add(holders.get(pos));
                            deleted.add(holders.get(pos + 1));
                            break;
                        case FLAG:
                            flag.add(holders.get(i - 2));
                            deleteOtherPage(i);
                            break;
                        case BW:
                            colors.put(holders.get(i + 2), "bw");
                            deleteOtherPage(i);
                            break;
                        case GRAY:
                            colors.put(holders.get(i + 2), "gray");
                            deleteOtherPage(i);
                            break;
                        case COLOR:
                            colors.put(holders.get(i + 2), null);
                            deleteOtherPage(i);
                            break;
                        case DPI:
                            dpiHolders.add(holder);
                            break;
                        default:
                            logger.log(Level.WARNING, "Unhandled barcode: {0}", code);
                    }
                }
            }
        }
        StringBuilder config = new StringBuilder();
        config.append("# Barcodes = \n");
        if (!dpiHolders.isEmpty()) {
            boolean all = false;
            boolean left = false;
            boolean right = false;
            boolean multiple = false;
            for (FileHolder h : dpiHolders) {
                if (h.getPosition() == FileHolder.LEFT) {
                    if (left) multiple = true;
                    left = true;
                } else if (h.getPosition() == FileHolder.RIGHT) {
                    if (right) multiple = true;
                    right = true;
                }
            }

            if (left == false || right == false) {
                config.append("Pages = all\n");
                all = true;
            }
            for (int i=0; i < dpiHolders.size(); i++) {
                FileHolder h = dpiHolders.get(i);
                if (!all) {
                    config.append("Pages = "+(h.getPosition() == FileHolder.LEFT ? "left" : "right"));
                    if (multiple) {
                        config.append(" "+h.getName()+"-");
                        String next ="";
                        for (int j=i+1; j < dpiHolders.size(); j++) {
                            if (h.getPosition() == dpiHolders.get(j).getPosition()) {
                                next = dpiHolders.get(j-1).getName();
                                break;
                            }
                        }
                        config.append(next);
                    }
                    config.append("\n");
                }
                config.append("SetSourceDPI = "+whole.format(h.getQRData().get(0).getDPIEstimate())+" # "+h+"\n");
            }
            config.append("\n");
        }
        config.append("Pages = all\n");
        if (!deleted.isEmpty()) {
            config.append("RemovePages = ");
            for (FileHolder h : deleted) {
                config.append(h.getName()+", ");
            }
            config.setLength(config.length()-2);
            config.append("\n");
        }
        if (!flag.isEmpty()) {
            config.append("\n# The following pages were flagged:\n");
            for (FileHolder h : flag) {
                config.append("# "+h.getName()+"\n");
            }
            config.append("\n");
        }
        if (!transformOps.isEmpty()) {
            HashMap<Integer,String> oldPages = new HashMap<Integer,String>();
            HashMap<Integer,String> oldAllBounds = new HashMap<Integer,String>();
            for (FileHolder h : transformOps) {
                String oldPage = oldPages.get(h.getPosition());
                String oldBounds = oldAllBounds.get(h.getPosition());
                String bounds = getPerspective(h);
                if (oldPage != null) {
                    FileHolder previousHolder = holders.get(holders.indexOf(h) -1);
                    if (oldBounds != null) {
                        config.append("Pages = "+(h.getPosition() == FileHolder.LEFT ? "left" : "right")+" "+oldPage+"-"+previousHolder.getName()+"\n");
                        config.append(getPerspective(h));
                    }
                }
                oldAllBounds.put(h.getPosition(), bounds);
                oldPages.put(h.getPosition(), h.getName());
            }
            if (oldAllBounds != null) {
                config.append("Pages = left "+oldPages.get(FileHolder.LEFT)+"-\n");
                config.append(oldAllBounds.get(FileHolder.LEFT)+"\n");
                config.append("Pages = right "+oldPages.get(FileHolder.RIGHT)+"-\n");
                config.append(oldAllBounds.get(FileHolder.RIGHT)+"\n\n");
            }
        }
        if (!colors.isEmpty()) {
            String oldPage = "";
            String oldColor = null;
            for (Map.Entry<FileHolder,String> entry : colors.entrySet()) {
                FileHolder h = entry.getKey();
                String color = entry.getValue();
                if (!oldPage.isEmpty()) {
                    FileHolder previousHolder = holders.get(holders.indexOf(h) -1);
                    if (oldColor != null) {
                        config.append("Pages = "+oldPage+"-"+previousHolder.getName()+"\n");
                        config.append("Color = "+oldColor+"\n");
                    }
                }
                oldColor = color;
                oldPage = h.getName();
            }
            if (oldColor != null) {
                config.append("Pages = "+oldPage+"-\n");
                config.append("Color = "+oldColor+"\n");
            }
        }
        config.append("# End barcode configured operations\n");
        configuration = config.toString();
        String[] lines = configuration.split("\n");
        PageSet tempPageSet = pageSet;
        ArrayList<Operation> ops = new ArrayList<Operation>();
        for (String line : lines) {
            List<Operation> newOperations = getOperation(line, null, tempPageSet);
            if (newOperations != null) {
                tempPageSet = newOperations.get(newOperations.size()-1).getPageSet();
                ops.addAll(newOperations);
            }
        }
        return ops;
    }

    private void deleteOtherPage(int page) {
        FileHolder h = holders.get(page);
        if (!h.isProblemFile()) {
            int pos = h.getPosition() == FileHolder.LEFT ? 1 : -1;
            deleted.add(holders.get(page + pos));
        }
    }

    private String getPerspective(FileHolder h) {
        StringBuilder str = new StringBuilder();
        str.append("BarcodePerspective = ");
        ArrayList<Point2D> pts = new ArrayList<Point2D>();
        ArrayList<String> found = new ArrayList<String>();
        String dpiCode = null;
        for (String code : PrintCodes.CORNER_CODES) {
            for (QRData data : h.getQRData()) {
                if (data.getCode().startsWith(code)) {
                    dpiCode = data.getCode();
                    found.add(code.substring(2,4));
                    str.append(data.getX()+","+data.getY()+", ");
                    pts.add(new Point2D.Double(data.getX(), data.getY()));
                    break;
                 }
            }
        }
        if (pts.size() != 4) {
            return "# Could not read the entire perspective barcode on this page: "+h+"\n"+
                   "# Found these corners: "+found+"\n";
        } else {
            str.setLength(str.length()-2);
            str.append(" # "+h.toString()+"\n");
            double dx1 = pts.get(1).distance(pts.get(0));
            double dx2 = pts.get(2).distance(pts.get(3));
            double dy1 = pts.get(3).distance(pts.get(0));
            double dy2 = pts.get(3).distance(pts.get(2));
            double avg = (dx1 + dx2 + dy1 + dy2) / 4;
            double inchesBetweenCodes = Double.parseDouble(dpiCode.substring(5));
            double dpi = avg /inchesBetweenCodes;
            
            str.append("SetSourceDPI = "+((int) dpi)+"\n");
        }
        return str.toString();
    }

    protected String getNextArg(String arg) {
        String[] args = getTextArgs();
        for (int i=0; i < getTextArgs().length; i++) {
            if (arg.equalsIgnoreCase(args[i]) && i +1 < getTextArgs().length) {
                return args[i+1];
            }
        }
        return null;
    }
}
