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
package net.sourceforge.bookscanwizard.qr;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.Writer;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

/**
 * A utility to print arbitrary QR codes.
 */

public class PrintCodes implements Printable, Pageable {
    /**
     * This is the percentage of the symbol that is between two markers.
     */
    public static double BOUNDS_TO_MARKERS = .54;
    private static final int PPI = 72;
    private static final int[] xOffset = {0, 1, 1, 0};
    private static final int[] yOffset = {0, 0, 1, 1};
    public static final String[] CORNER_CODES = new String[] {"X_TL_", "X_TR_", "X_BR_", "X_BL_"};

    private List<String> codes;
    private List<String> text;
    private int codeSize;
    private int spacing;
    private String description;
    private PageFormat pageFormat = new PageFormat();

    /**
     * This will print an array of qr codes, 4 to a page.
     *
     * @param messages  The codes to print
     * @param codeSize the size in inches of the qr code.  It includes the whitespace around the image.
     * @param spacing the distance from the start of one code to the other.
     * @param description the description of the page.  This is generally used for keystone correction.
     * @param printText If true it will print the text of the code below the image.
     */
    public PrintCodes(List<String> codes, List<String> text, double codeSize, double spacing, String description) {
        this.codes = codes;
        this.text = text;
        this.codeSize = (int) (codeSize * PPI);
        this.spacing = (int) (spacing * PPI);
        this.description = description;
    }

    public static void keystoneCodes() {
        ArrayList<String> codes = new ArrayList<String>();
        for (String code : CORNER_CODES) {
            codes.add(code+"3.0");
        }
        PrintCodes pc = new PrintCodes(codes, codes, 1.6, 3, "Keystone Correction");
        pc.print();
    }

    public boolean print() {
        PrinterJob pj = PrinterJob.getPrinterJob();
        pj.setPageable(this);
        if (pj.printDialog()) {
            try {
                pj.print();
                return true;
            } catch (PrinterException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    @Override
    public int getNumberOfPages() {
        return (codes.size() + 3) / 4;
    }

    @Override
    public PageFormat getPageFormat(int pageIndex) throws IndexOutOfBoundsException {
        return pageFormat;
    }

    @Override
    public Printable getPrintable(int pageIndex) throws IndexOutOfBoundsException {
        return this;
    }

    @Override
    public int print(Graphics g, PageFormat pf, int pageIndex) throws PrinterException {
        try {
            if (pageIndex * 4 >= codes.size()) {
                return Printable.NO_SUCH_PAGE;
            }
            int pos = pageIndex * 4;
            Graphics2D g2d = (Graphics2D) g;
            g2d.translate(pf.getImageableX()+1, pf.getImageableY()+1);
            g2d.setColor(Color.BLACK);
            g2d.setFont(g2d.getFont().deriveFont(Font.BOLD));
            for (int i = 0; i < 4; i++) {
                if ((pos + i) >= codes.size()) {
                    break;
                }
                String msg = codes.get(pos + i);
                BufferedImage img = encodeString(msg, codeSize);
                if (img != null) {
                    int x = spacing * xOffset[i];
                    int y = spacing * yOffset[i];
                    g2d.drawImage(img, null, x, y);
                    int height = codeSize;
                    if (text != null) {
                        String codeText = text.get(pos + i);
                        if (codeText != null && !codeText.isEmpty()) {
                            Rectangle2D rect = getStringBounds(g2d, codeText);
                            height += (int) rect.getHeight();
                            g2d.drawString(text.get(pos + i), x + (int) Math.max(0, (codeSize - rect.getWidth()) / 2), y + codeSize);
                        }
                    }
                    g2d.drawRect(x, y, codeSize, height);
                }
            }
            if (description != null) {
                Rectangle2D rect = getStringBounds(g2d, description);
                g2d.drawString(description, (int) (spacing - rect.getWidth() / 2 - 36), (int) (spacing - rect.getHeight() - 36));
            }
            return Printable.PAGE_EXISTS;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static BufferedImage encodeString(String message, int size) throws WriterException {
        Writer qrCode = new QRCodeWriter();
        Hashtable hints = new Hashtable();
        hints.put(EncodeHintType.MIN_VERSION, Integer.valueOf(2));
        BitMatrix bb = qrCode.encode(ReadCodes.PREFIX+size+","+message, BarcodeFormat.QR_CODE, size, size, hints);
        return MatrixToImageWriter.toBufferedImage(bb);
    }

    private Rectangle2D getStringBounds(Graphics2D g2d, String description) {
        FontMetrics fm   = g2d.getFontMetrics(g2d.getFont());
        return fm.getStringBounds(description, g2d);
    }

    public static void main(String[] args) throws Exception {
        args = new String[QRCodeControls.values().length];
        for (int i=0; i < args.length; i++) {
            args[i] = QRCodeControls.values()[i].description();
        }
        List<String> ar = Arrays.asList(args);
        PrintCodes pc = new PrintCodes(ar, ar , 3, 3.5, null);
        pc.print();
    }
}
