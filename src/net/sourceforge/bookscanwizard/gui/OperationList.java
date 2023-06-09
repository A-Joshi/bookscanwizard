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

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
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
import net.sourceforge.bookscanwizard.BSW;
import net.sourceforge.bookscanwizard.BoundsHelper;
import net.sourceforge.bookscanwizard.OpDefinition;
import net.sourceforge.bookscanwizard.Operation;
import net.sourceforge.bookscanwizard.op.*;
import net.sourceforge.bookscanwizard.util.SelectableLabel;

public class OperationList extends JFrame {
    private final SelectableLabel argNotes;
    private JTable table;
    private static final ArrayList<OpDefinition> defs;
    private final BoundsHelper boundsListener;

    private static final Class[] operations = {
        AddBorder.class,
        AutoLevels.class,
        BarcodePerspective.class,
        Barcodes.class,
        BarrelCorrection.class,
        Brightness.class,
        Color.class,
        CreateArchiveZip.class,
        CreatePDF.class,
        Crop.class,
        CropAndScale.class,
//      Deprecated
//        DeselectPages.class,
        EstimateDPI.class,
//        Despeckle.class,
        Gamma.class,
//        GaussianBlur.class, 
        ImageStatistics.class,
        InterpolateCrop.class,
        Levels.class,
        LoadImages.class,
        LoadLRImages.class,
        LoadTemp.class,
        Metadata.class,
        NormalizeLighting.class,
        OCR.class,
        PageLabels.class,
        Pages.class,
        Perspective.class,
        PerspectiveAndCrop.class,
        PipePNG.class,
        PostCommand.class,
        RemovePages.class,
        Rename.class,
        Rotate.class,
        Saturation.class,
        SaveImages.class,
        SaveTemp.class,
        SaveToArchive.class,
        Scale.class,
        ScaleToDPI.class,
        ScaleToFirst.class,
        ScanTailor.class,
        SetDestination.class,
        SetDestinationDPI.class,
        SetPreviewScale.class,
        SetDestinationDPI.class,
        SetSourceDPI.class,
        SetTiffOptions.class,
        Sharpen.class,
        StartPage.class,
//      Not ready for prime time
        // Unwarp
//        WhiteBalance.class
        Whiteout.class
    };

    public OperationList(JFrame mainFrame) {
        super("BSW Help");
        setIconImage(mainFrame.getIconImage());
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
                    return def.getShortDescription();
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
                        String command = "insert_config "+def.getExample().trim()+"\n";
                        ActionEvent event = new ActionEvent(e.getSource(), e.getID(), command);
                        try {
                            BSW.instance().getMenuHandler().cursorActionPerformed(event);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        });
        table.setColumnModel(columnModel);
        JScrollPane scrollpane = new JScrollPane(table);
        scrollpane.setPreferredSize(new Dimension(750, 200));

        argNotes = new SelectableLabel();
        argNotes.setBorder(new EmptyBorder(10,10,10,10));
        JScrollPane scroll2 = new JScrollPane(argNotes);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollpane, scroll2);
        splitPane.setPreferredSize(new Dimension(800, 500));

        getContentPane().add(splitPane);

        pack();
        splitPane.setDividerLocation(.3);
        Dimension scrSize = Toolkit.getDefaultToolkit().getScreenSize();
        int scrPos = (int) Math.min(850, (scrSize.getWidth() - getWidth()));
        setLocation(scrPos, getLocation().x);
        boundsListener = new BoundsHelper(this);
        table.getSelectionModel().setSelectionInterval(0, 0);
    }

    @Override
    public void setVisible(boolean visible) {
        String line = BSW.instance().getConfigEntry().getCurrentLineOrSelection();
        int pos = line.indexOf("=");
        if (pos > 0) {
            String op = line.substring(0, pos).trim();

            for (int y=0; y < table.getModel().getRowCount(); y++) {
                String name = (String) table.getModel().getValueAt(y, 0);
                if (op.equals(name)) {
                    table.getSelectionModel().setSelectionInterval(y, y);
                    table.scrollRectToVisible(table.getCellRect(y, 0, true));
                    break;
                }
            }
        }
        super.setVisible(visible);
    }

    private void updateArgs() {
        int row = table.getSelectedRow();
        if (row >=0) {
            OpDefinition def = defs.get(row);
            argNotes.setText(getColumnNotes(def));
        }
    }

    public static OpDefinition findDefinition(String name) {
        for (OpDefinition def : defs) {
            if (def.getName().equals(name)) {
                return def;
            }
        }
        return null;
    }

    public String getColumnNotes(OpDefinition def) {
        StringBuilder str = new StringBuilder();
        str.append("<html><body><b>").append(def.getName())
                .append("</b><br><p>").append(def.getHelper())
                .append("</p>\n");
        if (def.getExample() != null) {
            str.append("<br><p><b>Example:</b></p>\n").append(def.getExample())
               .append("<br><br>");
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
    
    static {
        defs = new ArrayList<>();
        for (Class cls : operations) {
            try {
                Operation op = (Operation) cls.newInstance();
                defs.add(op.getDefinition());
            } catch (InstantiationException | IllegalAccessException ex) {
                Logger.getLogger(OperationList.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
