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
package net.sourceforge.bookscanwizard.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.BoxView;
import javax.swing.text.ComponentView;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.GlyphView;
import javax.swing.text.IconView;
import javax.swing.text.LabelView;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.ParagraphView;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import net.sourceforge.bookscanwizard.OpDefinition;
import net.sourceforge.bookscanwizard.Operation;
import net.sourceforge.bookscanwizard.op.CreateArchiveZip;

public final class ConfigEntry extends JTextArea {
    private static final Logger logger = Logger.getLogger(ConfigEntry.class.getName());
    public static final String LINE_BREAK_ATTRIBUTE_NAME = "line_break_attribute";
    private ActionListener menuHandler;
    private JMenuItem popupItemBarcode;
    private JMenuItem popupItemPoints;
    private JMenuItem helpItem;
    private JSeparator helpItemSeparator;
    private int lastLine = -1;
    private static final Color GRAYED_OUT = new Color(235,235,235);
    private boolean cursorAfterConfig;

    private static final String[][] ops = new String[][] {
        new String[] {"Cropping, Perspective", "Crop", "Perspective", "PerspectiveAndCrop", "Rotate", "BarrelCorrection"},
        new String[] {"Filters", "AutoLevels", "Brightness", "Color", "Sharpen", "Levels", "Gamma", "Saturation"},
        new String[] {"Scaling", "Scale", "ScaleToDPI", "ScaleToFirst"},
        new String[] {"Saving", "SaveImages", "CreatePDF", "CreateArchiveZip"},
        new String[] {"Other Operatons", "LoadImages", "LoadLRImages", "Rename", "Barcodes", "Pages", "RemovePages", "SetSourceDPI", "SetDestinationDPI", "EstimateDPI"}
    };

    public ConfigEntry(ActionListener handler) {
        super("", 10, 80);
        setOpaque(false);
        menuHandler = handler;

        final JLabel estimateZipSize = new JLabel() {
            @Override
            public void setVisible(boolean visible) {
                if (visible) {
                    setFont(getFont().deriveFont(14).deriveFont(Font.BOLD));
                }
                super.setVisible(visible);
            }
        };

        final JPopupMenu popup = new JPopupMenu() {
            @Override
            public void show(Component invoker, int x, int y) {
                String text = getCurrentLineOrSelection();
                boolean found = text.contains("Barcodes");
                popupItemBarcode.setVisible(found);
                popupItemPoints.setVisible(!found);
                found = text.contains("CreateArchiveZip");
                if (found) {
                    try {
                        CreateArchiveZip op = (CreateArchiveZip) Operation.getStandaloneOp(text);
                        estimateZipSize.setText(op.estimateZipSize());
                    } catch (Exception ex) {
                        logger.log(Level.WARNING, ex.getMessage(), ex);
                    }
                }
                estimateZipSize.setVisible(found);
                popupItemPoints.setVisible(!found);
                Operation op = null;
                try {
                    op = Operation.getStandaloneOp(getCurrentLineOrSelection());
                    if (op != null) {
                        helpItem.setVisible(true);
                        helpItemSeparator.setVisible(true);
                        helpItem.setText("Help for "+op.getName());
                    }
                } catch (Exception ex) {
                    logger.log(Level.WARNING, ex.getMessage(), ex);
                }
                if (op == null) {
                    helpItem.setVisible(false);
                    helpItemSeparator.setVisible(false);
                }
                super.show(invoker, x, y);
            }
        };
        popup.add(estimateZipSize);

        popupItemPoints = new JMenuItem("Copy points to viewer");
        popupItemPoints.setActionCommand("copy_points_to_viewer");
        popupItemPoints.addActionListener(menuHandler);
        popup.add(popupItemPoints);
        estimateZipSize.setFont(popupItemPoints.getFont());

        popupItemBarcode = new JMenuItem("Expand Barcode Operations");
        popupItemBarcode.setActionCommand("expand_barcode_operations");
        popupItemBarcode.addActionListener(menuHandler);
        popup.add(popupItemBarcode);

        popup.add(new JSeparator());

        for (String[] line : ops) {
            JMenu menu = new JMenu(line[0]);
            popup.add(menu);
            for (int i=1; i < line.length; i++) {
                JMenuItem item = new JMenuItem(line[i]);
                menu.add(item);
                item.setActionCommand("op "+line[i]);
                item.addActionListener(menuHandler);
            }
        }
        helpItemSeparator = new JSeparator();
        popup.add(helpItemSeparator);
        helpItem = new JMenuItem();
        helpItem.setActionCommand("command_helper");
        helpItem.addActionListener(menuHandler);
        popup.add(helpItem);

        MouseListener popupListener = new PopupListener(popup);
        addMouseListener(popupListener);

        final UndoManager undo = new UndoManager();
        Document doc = getDocument();
        // Listen for undo and redo events
        doc.addUndoableEditListener(new UndoableEditListener() {

            @Override
            public void undoableEditHappened(UndoableEditEvent evt) {
                undo.addEdit(evt.getEdit());
            }
        });

        // Create an undo action and add it to the text component
        getActionMap().put("undo", new AbstractAction("undo") {

            @Override
            public void actionPerformed(ActionEvent evt) {
                try {
                    if (undo.canUndo()) {
                        undo.undo();
                    }
                } catch (CannotUndoException e) {
                }
            }
        });
        // Create a redo action and add it to the text component
        getActionMap().put("redo", new AbstractAction("redo") {

            @Override
            public void actionPerformed(ActionEvent evt) {
                try {
                    if (undo.canRedo()) {
                        undo.redo();
                    }
                } catch (CannotRedoException e) {
                }
            }
        });
//        setEditorKit(new WrapEditorKit());
    }

    public void insertCommand(OpDefinition def) {
        try {
            getDocument().insertString(getCaretPosition(), def.getExample(), null);
        } catch (BadLocationException ex) {
            Logger.getLogger(ConfigEntry.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String getConfigToPreview() {
        int pos = Math.max(0, getCaret().getDot());
        String txt = getText();
        int endLine = txt.indexOf("\n", pos);
        if (endLine < 0) {
            endLine = txt.length();
        }
        int startLastLine = txt.lastIndexOf("\n", pos-1)+1;
        txt = txt.substring(0, endLine);
        String currentLine = txt.substring(startLastLine, endLine);
        int commentPos = currentLine.indexOf("#");
        if (commentPos >=0) {
            currentLine = currentLine.substring(0, commentPos);
        }
        cursorAfterConfig = !currentLine.contains("=");
        return txt.substring(0, endLine);
    }
    
    public boolean isCursorAfterConfig() {
        return cursorAfterConfig;
    }

    public String getCurrentLineOrSelection() {
        String text = getText();
        int dot = getCaret().getDot();
        int mark = getCaret().getMark();
        if (dot == mark) {
            int endLine = text.indexOf("\n", dot);
            if (endLine < 0) {
                endLine = text.length();
            }
            int startLine = text.lastIndexOf("\n", dot - 1)+1;
            if (startLine < 0) {
                startLine = 0;
            }
            setSelectionStart(startLine);
            setSelectionEnd(endLine);
            return text.substring(startLine, endLine);
        } else {
            return text.substring(Math.min(dot, mark), Math.max(dot, mark));
        }
    }

    private Cursor oldCursor;
    void setBusy(boolean busy) {
        if (busy) {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        } else {
            setCursor(oldCursor);
        }
    }

/*
 * The following is based on code from http://java-sl.com/src/wrap_src.html
 * @author Stanislav Lapitsky
 */
    private static class WrapEditorKit extends StyledEditorKit {
        private ViewFactory defaultFactory = new WrapColumnFactory();

        @Override
        public ViewFactory getViewFactory() {
            return defaultFactory;
        }

        @Override
        public MutableAttributeSet getInputAttributes() {
            MutableAttributeSet mAttrs = super.getInputAttributes();
            mAttrs.removeAttribute(LINE_BREAK_ATTRIBUTE_NAME);
            return mAttrs;
        }

        private static class WrapColumnFactory implements ViewFactory {
            @Override
            public View create(Element elem) {
                String kind = elem.getName();
                if (kind != null) {
                    switch (kind) {
                        case AbstractDocument.ContentElementName:
                            return new WrapLabelView(elem);
                        case AbstractDocument.ParagraphElementName:
                            return new NoWrapParagraphView(elem);
                        case AbstractDocument.SectionElementName:
                            return new BoxView(elem, View.Y_AXIS);
                        case StyleConstants.ComponentElementName:
                            return new ComponentView(elem);
                        case StyleConstants.IconElementName:
                            return new IconView(elem);
                    }
                }
                // default to text display
                return new LabelView(elem);
            }
        }
    }

    private static class NoWrapParagraphView extends ParagraphView {
        public NoWrapParagraphView(Element elem) {
            super(elem);
        }

        @Override
        public void layout(int width, int height) {
            super.layout(Short.MAX_VALUE, height);
        }

        @Override
        public float getMinimumSpan(int axis) {
            return super.getPreferredSpan(axis);
        }
    }

    private static class WrapLabelView extends LabelView {
        public WrapLabelView(Element elem) {
            super(elem);
        }

        @Override
        public int getBreakWeight(int axis, float pos, float len) {
            if (axis == View.X_AXIS) {
                checkPainter();
                int p0 = getStartOffset();
                int p1 = getGlyphPainter().getBoundedPosition(this, p0, pos, len);
                if (p1 == p0) {
                    // can't even fit a single character
                    return View.BadBreakWeight;
                }
                try {
                    //if the view contains line break char return forced break
                    if (getDocument().getText(p0, p1 - p0).indexOf("\r") >= 0) {
                        return View.ForcedBreakWeight;
                    }
                } catch (BadLocationException ex) {
                    //should never happen
                }
            }
            return super.getBreakWeight(axis, pos, len);
        }

        @Override
        public View breakView(int axis, int p0, float pos, float len) {
            if (axis == View.X_AXIS) {
                checkPainter();
                int p1 = getGlyphPainter().getBoundedPosition(this, p0, pos, len);
                try {
                    //if the view contains line break char break the view
                    int index = getDocument().getText(p0, p1 - p0).indexOf("\r");
                    if (index >= 0) {
                        GlyphView v = (GlyphView) createFragment(p0, p0 + index + 1);
                        return v;
                    }
                } catch (BadLocationException ex) {
                    //should never happen
                }
            }
            return super.breakView(axis, p0, pos, len);
        }
    }

    /*
     * Overridden to gray out the configuration that will not be run.
     */
    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(getBackground());
        try {
            int line = getLineOfOffset(getSelectionEnd());
            if (line != lastLine) {
                lastLine = line;
                repaint();
            }
            int pos = (line+1) * getRowHeight() +2;
            g.fillRect(0, 0, getWidth(), pos);
            g.setColor(GRAYED_OUT);
            g.fillRect(0, pos+1, getWidth(), getHeight()- pos);
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
        super.paintComponent(g);
    }
}
