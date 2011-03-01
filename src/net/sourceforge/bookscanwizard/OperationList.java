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

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import net.sourceforge.bookscanwizard.op.*;

public class OperationList extends JFrame {
    private JLabel argNotes;
    private JTable table;
    private ArrayList<OpDefinition> defs;
    private final BoundsHelper boundsListener;

    private static Class[] operations = {
        AutoLevels.class,
        BarrelCorrection.class,
        Barcodes.class,
        BarcodePerspective.class,
        Brightness.class,
        Color.class,
        Crop.class,
        CropAndScale.class,
        EstimateDPI.class,
        Gamma.class,
        ImageStatistics.class,
        InterpolateCrop.class,
        Levels.class,
        LoadImages.class,
        LoadLRImages.class,
//        NormalizeLighting.class,
        Pages.class,
        PipePNG.class,
        Perspective.class,
        PerspectiveAndCrop.class,
        PipePNG.class,
        PostCommand.class,
        RemovePages.class,
        Rename.class,
        Rotate.class,
        Scale.class,
        ScaleToDPI.class,
        ScaleToFirst.class,
        SetDestination.class,
        SetPreviewScale.class,
        SetSourceDPI.class,
        SetTiffOptions.class,
        Sharpen.class
    };

    public OperationList(JFrame mainFrame) {
        super("BSW Help");
        setIconImage(mainFrame.getIconImage());
        defs = new ArrayList<OpDefinition>();
        Vector<String> ops = new Vector<String>();
        for (Class cls : operations) {
            try {
                Operation op = (Operation) cls.newInstance();
                defs.add(op.getDefinition());
            } catch (InstantiationException ex) {
                Logger.getLogger(OperationList.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(OperationList.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        DefaultTableColumnModel columnModel = new DefaultTableColumnModel();
        TableColumn col1 = new TableColumn(0, 150);
        col1.setHeaderValue("Operation");
        columnModel.addColumn(col1);
        TableColumn col2 = new TableColumn(1, 700);
        col2.setHeaderValue("Description");
        columnModel.addColumn(col2);

        TableModel dataModel = new AbstractTableModel() {
            @Override
            public int getColumnCount() {
                return 2;
            }

            @Override
            public int getRowCount() {
                return defs.size();
            }

            @Override
            public Object getValueAt(int row, int col) {
                OpDefinition def = defs.get(row);
                if (col == 0) {
                    return def.getName();
                } else if (col == 1) {
                    return def.getHelper();
                } else {
                    return def;
                }
            }
        };
        table = new JTable(dataModel);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener(){
            @Override
            public void valueChanged(ListSelectionEvent e) {
                updateArgs();
            }
        });
        table.addFocusListener(new FocusListener(){
            @Override
            public void focusGained(FocusEvent e) {
                updateArgs();
            }

            @Override
            public void focusLost(FocusEvent e) {
            }
        });
        table.addMouseListener(new MouseAdapter(){
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2){
                    if (table.getSelectedRow() >=0) {
                        OpDefinition def = defs.get(table.getSelectedRow());
                        ConfigEntry entry = BSW.instance().getConfigEntry();
                        int start = entry.getSelectionStart();
                        BSW.instance().getConfigEntry().insertCommand(def);
                        entry.setSelectionStart(start);
                        entry.setSelectionEnd(start + def.toString().length());
                        entry.requestFocus();
                    }
                }
            }
        });
        table.setColumnModel(columnModel);
        JScrollPane scrollpane = new JScrollPane(table);
        scrollpane.setPreferredSize(new Dimension(750, 200));
        argNotes = new JLabel();
        argNotes.setBorder(new EmptyBorder(5, 5, 5, 5));
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollpane, argNotes);
        splitPane.setPreferredSize(new Dimension(800, 500));

        getContentPane().add(splitPane);

        pack();
        Dimension scrSize = Toolkit.getDefaultToolkit().getScreenSize();
        int scrPos = (int) Math.min(850, (scrSize.getWidth() - getWidth()));
        setLocation(scrPos, getLocation().x);
        boundsListener = new BoundsHelper(this);
    }

    private void updateArgs() {
        int row = table.getSelectedRow();
        if (row >=0) {
            OpDefinition def = defs.get(row);
            argNotes.setText(getColumnNotes(def));
        }
    }

    public OpDefinition findDefinition(String name) {
        for (OpDefinition def : defs) {
            if (def.getName().equals(name)) {
                return def;
            }
        }
        throw new IllegalArgumentException("Could not find: "+name);
    }

    public String getColumnNotes(OpDefinition def) {
        StringBuilder str = new StringBuilder();
        str.append("<html><body><b>").append(def.getName())
                .append("</b><br><p>").append(def.getHelper())
                .append("</p>\n");
        if (def.getExample() != null) {
            str.append("<br><p><b>Example:</b></p>\n"+
                       def.getExample()+"<br><br>");
        }
        if (def.getArguments().isEmpty()) {
            str.append("<p>No parameters</p>");
        } else {
            str.append("<p>Parameters:</p>\n"+
                       "<table border='1'>"+
                       "<tr><th>Name</th><th>Required</th><th>Description</th>");
        }
        for (OpDefinition.Argument arg : def.getArguments()) {
            str.append("<tr><td>").append(arg.getName())
                .append("</td><td>").append(arg.isRequired()? "Yes" : "")
                .append("</td><td>").append(arg.getTooltip())
                .append("</td></tr>");
        }
        str.append(
                "</table></body></html>");
        return str.toString();
    }

    public BoundsHelper getBoundsHelper() {
        return boundsListener;
    }
}
