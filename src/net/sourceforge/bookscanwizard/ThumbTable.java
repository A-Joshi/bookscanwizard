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

import com.sun.media.jai.widget.DisplayJAI;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.RenderingHints;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import net.sourceforge.bookscanwizard.util.BlockingLifoQueue;
import net.sourceforge.bookscanwizard.util.Utils;

/**
 * A Table that contains the source image thumbnails.
 */
public class ThumbTable extends JTable {

    public static final int IMAGE_WIDTH = 100;
    private static List<FileHolder> holders = new ArrayList<FileHolder>();
    private static Map<FileHolder, RenderedImage> images =
            Collections.synchronizedMap(new HashMap<FileHolder, RenderedImage>());
    private static final BlockingLifoQueue<Runnable> lifoQueue = new BlockingLifoQueue<Runnable>();
    private static final int threadCt = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
    private static final Executor executor = 
        new ThreadPoolExecutor(threadCt, threadCt, 0L, TimeUnit.MILLISECONDS, lifoQueue,
                               new BSWThreadFactory(Thread.MIN_PRIORITY));
    private int customRowHeight = 0;
    private JPopupMenu popup;
    
    public ThumbTable(ActionListener menuHandler) {
        super(new HolderDataModel());
        ThumbTableCellRenderer renderer = new ThumbTableCellRenderer();
        getColumnModel().getColumn(0).setCellRenderer(renderer);
        setTableHeader(null);
        addMouseListener( new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == 1 &&  e.getClickCount() == 2) {
                    UserFeedbackHelper.doAction(e, new Runnable() {
                        public void run() {
                            FileHolder holder = holders.get(getSelectedRow());
                            System.out.println("selected: "+getSelectedRow()+" "+holder);
                            BSW.instance().getPreviewedImage().setFileHolder(holder);
                        }
                    });
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    JTable source = (JTable)e.getSource();
                    int row = source.rowAtPoint( e.getPoint() );
                    int column = source.columnAtPoint( e.getPoint() );
                    if (! source.isRowSelected(row)) {
                        source.changeSelection(row, column, false, false);                
                    }
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
        popup = new JPopupMenu();
        JMenuItem menuItem;
        menuItem = new JMenuItem("Select Page");
        menuItem.setActionCommand("thumb_select");
        menuItem.setToolTipText("Displays current selection in the viewer");
        menuItem.addActionListener(menuHandler);
        popup.add(menuItem);
        menuItem = new JMenuItem("Insert Page config");
        menuItem.setActionCommand("thumb_insert");
        menuItem.setToolTipText("Copies the selected pages to the configuration");
        menuItem.addActionListener(menuHandler);
        popup.add(menuItem);
        menuItem = new JMenuItem("Copy Page config");
        menuItem.setToolTipText("Copies the selected pages to the clipboard");
        menuItem.setActionCommand("thumb_copy");
        menuItem.addActionListener(menuHandler);
        popup.add(menuItem);
        setTransferHandler(new ThumbTransferHandler());
    }
    
    public FileHolder getSelectedHolder() {
        return holders.get(getSelectedRow());
    }

    /**
     * Returns a Page configuration setting based on the selected
     * table cells.
     */
    public String calcPageConfig() {
        final StringBuilder str = new StringBuilder();
        final MainFrame fr = BSW.instance().getMainFrame();
        final boolean left = fr.getLeftThumb().isSelected();
        final boolean right = fr.getRightThumb().isSelected();
        str.append("Pages = ");
        if (left && right) {
            str.append("all ");
        } else if (left) {
            str.append("left ");
        } else if (right) {
            str.append("right ");
        } else {
            throw new RuntimeException("no pages selected");
        }
        final int min = selectionModel.getMinSelectionIndex();
        final int max = selectionModel.getMaxSelectionIndex();
        int firstPos = -1;
        int lastPos = -1;
        for (int i=min; i <= max; i++) {
            if (selectionModel.isSelectedIndex(i)) {
                if (firstPos < 0) {
                    firstPos = i;
                }
                lastPos = i;
            }
            if (i == max || !selectionModel.isSelectedIndex(i)) {
                if (firstPos >=0) {
                    str.append(holders.get(firstPos).getName());
                    if (lastPos != firstPos) {
                        str.append("-").append(holders.get(lastPos).getName());
                    }
                    str.append(",");
                }
                firstPos = -1;
                lastPos = -1;
            }
        }
        str.setLength(str.length()-1);
        str.append("\n");
        return str.toString();
    }
    
    
    public void update() {
        MainFrame fr =  BSW.instance().getMainFrame();
        boolean left = fr.getLeftThumb().isSelected();
        boolean right = fr.getRightThumb().isSelected();
        if (PageSet.getSourceFiles() != null) {
            if (holders != null) {
                holders.clear();
                lifoQueue.clear();
                for (int i=0; i < PageSet.getSourceFiles().size(); i++) {
                    if (left && (i % 2 == 0) || right && (i % 2 == 1)) {
                        holders.add(PageSet.getSourceFiles().get(i));
                    }
                }
                for (int i=holders.size()-1; i>=0; i--) {
                    FileHolder holder = holders.get(i);
                    if (!images.containsKey(holder)) {
                        ImageRequest imgRequest = new ImageRequest(holders.get(i));
                        executor.execute(imgRequest);
                    }
                }
            }
        }
        ((HolderDataModel) getModel()).fireTableDataChanged();
        updateSelection();
    }

    public void update(final FileHolder h) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                int pos = holders.indexOf(h);
                ((HolderDataModel) getModel()).fireTableRowsUpdated(pos, pos);
            }
        });
    }

    void updateSelection() {
        FileHolder h = BSW.instance().getPreviewedImage().getPreviewHolder();
        int pos = holders.indexOf(h);
        if (pos >=0) {
            getSelectionModel().setSelectionInterval(pos, pos);
        }
    }

    static class HolderDataModel extends AbstractTableModel {

        public int getRowCount() {
            return holders.size();
        }

        public int getColumnCount() {
            return 1;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            return holders.get(rowIndex);
        }
    }

    private class ThumbTableCellRenderer extends JPanel implements TableCellRenderer {
        private Border selectedBorder;
        private Border normalBorder;
        private JLabel lable = new JLabel();
        private DisplayJAI thumb = new DisplayJAI();
        
        public ThumbTableCellRenderer() {
            selectedBorder = new LineBorder(Color.RED, 3);
            normalBorder = new LineBorder(Color.BLACK, 3);
            setLayout(new BorderLayout());
            add(lable, BorderLayout.SOUTH);
            add(thumb, BorderLayout.NORTH);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            FileHolder holder = (FileHolder) value;
            RenderedImage img = images.get(holder);
            boolean found = true;
            if (img == null) {
                found = false;
                img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
                ImageRequest imgRequest = new ImageRequest(holder);
                lifoQueue.remove(imgRequest);
                executor.execute(imgRequest);
            }
            thumb.set(img);
            setBorder(isSelected ? selectedBorder : normalBorder);
            lable.setText(holder.getName());
            if (found && customRowHeight == 0) {
                customRowHeight = getPreferredSize().height;
                ThumbTable.this.setRowHeight(customRowHeight);
            }
            return this;
        }
    }

    private class ImageRequest implements Runnable {
        private FileHolder holder;
        
        @Override
        public int hashCode() {
            int hash = 5;
            hash = 29 * hash + (this.holder != null ? this.holder.hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ImageRequest other = (ImageRequest) obj;
            if (this.holder != other.holder && (this.holder == null || !this.holder.equals(other.holder))) {
                return false;
            }
            return true;
        }

        public FileHolder getHolder() {
            return holder;
        }

        public ImageRequest(FileHolder h) {
            this.holder = h;
        }

        public void run() {
            try {
                RenderedImage img = holder.getThumbnail();
                img = Utils.getScaledInstance(img, IMAGE_WIDTH, IMAGE_WIDTH * img.getHeight() / img.getWidth(), RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                images.put(holder, img);
                update(holder);
            } catch (IOException ex) {
                Logger.getLogger(ThumbTable.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    private class ThumbTransferHandler extends TransferHandler {
        @Override
        public int getSourceActions(JComponent c) {
            return COPY;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            return new StringSelection(calcPageConfig());
        }
    }
}
