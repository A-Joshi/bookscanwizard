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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.image.RenderedImage;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.JAI;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Panel;
import java.awt.Toolkit;
import java.awt.dnd.DragSource;
import java.awt.event.ActionListener;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.SubsampleBinaryToGrayDescriptor;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;

/**
 * The GUI front end to the BSW
 */
public class MainFrame extends JFrame {
    private final ViewerPanel viewerPanel;
    private final ConfigEntry configEntry;
    private final JLabel status;

    private JComboBox pageListBox = new JComboBox() {
        @Override
        public Dimension getMaximumSize() {
            return super.getPreferredSize();
        }

        @Override
        public Dimension getMinimumSize() {
            return super.getPreferredSize();
        }
    };
    private JCheckBox showCrops = new JCheckBox();
    private JCheckBox showPerspective = new JCheckBox();
    private JCheckBox showColors = new JCheckBox();
    private JButton runButton;
    private JComponent focusedComponent;

    private JDialog aboutDialog;
    private OperationList operationList;

    private final JCheckBoxMenuItem showDebuggingInfo;
    private JSplitPane splitPane;
    private final JCheckBoxMenuItem horizontalLayout;
    private final Panel leftPanel;
    private final Panel rightPanel;
    private final JProgressBar progressBar;
    private final BoundsHelper boundsListener;

    public MainFrame(final ActionListener menuHandler) {
        super("Book Scan Wizard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        if (DragSource.getDragThreshold() < 5) {
            System.setProperty("awt.dnd.drag.threshold", "5");
        }
        URL url = MainFrame.class.getClassLoader().getResource("net/sourceforge/bookscanwizard/bsw.png");
        setIconImage(new ImageIcon(url).getImage());

        JMenuBar menuBar = new JMenuBar();
        getRootPane().setJMenuBar(menuBar);
        JMenuItem menuItem;
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        menuBar.add(fileMenu);

        menuItem = new JMenuItem("New");
        menuItem.setMnemonic(KeyEvent.VK_N);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuItem.setActionCommand("new");
        menuItem.addActionListener(menuHandler);
        fileMenu.add(menuItem);

        menuItem = new JMenuItem("Open");
        menuItem.setMnemonic(KeyEvent.VK_O);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuItem.setActionCommand("open");
        menuItem.addActionListener(menuHandler);
        fileMenu.add(menuItem);

        menuItem = new JMenuItem("Save");
        menuItem.setMnemonic(KeyEvent.VK_S);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuItem.setActionCommand("save");
        menuItem.addActionListener(menuHandler);
        fileMenu.add(menuItem);

        menuItem = new JMenuItem("Save As");
        menuItem.setMnemonic(KeyEvent.VK_A);
        menuItem.setActionCommand("save_as");
        menuItem.addActionListener(menuHandler);
        fileMenu.add(menuItem);

        menuItem = new JMenuItem("Exit");
        menuItem.setMnemonic(KeyEvent.VK_X);
        menuItem.setActionCommand("exit");
        menuItem.addActionListener(menuHandler);
        fileMenu.add(menuItem);

        JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic(KeyEvent.VK_E);
        menuBar.add(editMenu);
        ActionListener ccpListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String name = ((JComponent) e.getSource()).getName();
                invokeAction(name, focusedComponent, e);
            }
        };

        menuItem = new JMenuItem("Undo");
        menuItem.setName("undo");
        menuItem.setMnemonic(KeyEvent.VK_U);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuItem.addActionListener(ccpListener);
        editMenu.add(menuItem);
        
        menuItem = new JMenuItem("Redo");
        menuItem.setName("redo");
        menuItem.setMnemonic(KeyEvent.VK_R);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuItem.addActionListener(ccpListener);
        editMenu.add(menuItem);

        editMenu.add(new JSeparator());

        menuItem = new JMenuItem("Cut");
        menuItem.setName("cut");
        menuItem.setMnemonic(KeyEvent.VK_T);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuItem.addActionListener(ccpListener);
        editMenu.add(menuItem);

        menuItem = new JMenuItem("Copy");
        menuItem.setName("copy");
        menuItem.setMnemonic(KeyEvent.VK_C);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuItem.addActionListener(ccpListener);
        editMenu.add(menuItem);

        menuItem = new JMenuItem("Paste");
        menuItem.setName("paste");
        menuItem.setMnemonic(KeyEvent.VK_V);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuItem.addActionListener(ccpListener);
        editMenu.add(menuItem);

        JMenu actionsMenu = new JMenu("Actions");
        actionsMenu.setMnemonic(KeyEvent.VK_A);
        menuBar.add(actionsMenu);

        menuItem = new JMenuItem("Preview");
        menuItem.setMnemonic(KeyEvent.VK_P);
        menuItem.setActionCommand("preview");
        menuItem.addActionListener(menuHandler);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        actionsMenu.add(menuItem);

        menuItem = new JMenuItem("Submit");
        menuItem.setMnemonic(KeyEvent.VK_S);
        menuItem.setActionCommand("run");
        menuItem.addActionListener(menuHandler);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        actionsMenu.add(menuItem);

        menuItem = new JMenuItem("Zoom in");
        menuItem.setMnemonic(KeyEvent.VK_I);
        menuItem.setActionCommand("zoomIn");
        menuItem.addActionListener(menuHandler);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        actionsMenu.add(menuItem);

        menuItem = new JMenuItem("Zoom out");
        menuItem.setMnemonic(KeyEvent.VK_O);
        menuItem.setActionCommand("zoomOut");
        menuItem.addActionListener(menuHandler);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        actionsMenu.add(menuItem);
        
        JMenu toolsMenu = new JMenu("Tools");
        toolsMenu.setMnemonic(KeyEvent.VK_T);

        menuBar.add(toolsMenu);

        menuItem = new JMenuItem("Load DPI Information");
        menuItem.setMnemonic(KeyEvent.VK_L);
        menuItem.setToolTipText("Creates a EstimateDPI command from saved data.");
        menuItem.setActionCommand("op EstimateDPI");
        menuItem.addActionListener(menuHandler);
        toolsMenu.add(menuItem);

        menuItem = new JMenuItem("Save DPI Information");
        menuItem.setMnemonic(KeyEvent.VK_D);
        menuItem.setToolTipText("Saves the source DPI and exif focal length for future scans");
        menuItem.setActionCommand("save_dpi");
        menuItem.addActionListener(menuHandler);
        toolsMenu.add(menuItem);

        toolsMenu.add(new JSeparator());

        menuItem = new JMenuItem("Print Keystone Correction Barcodes...");
        menuItem.setMnemonic(KeyEvent.VK_K);
        menuItem.setToolTipText("Creates standard Keystone Correction Barcodes");
        menuItem.setActionCommand("keystone_barcodes");
        menuItem.addActionListener(menuHandler);
        toolsMenu.add(menuItem);

        menuItem = new JMenuItem("Custom Barcodes...");
        menuItem.setMnemonic(KeyEvent.VK_B);
        menuItem.setToolTipText("Brings up a dialog to print custom QR codes");
        menuItem.setActionCommand("print_qr_codes");
        menuItem.addActionListener(menuHandler);
        toolsMenu.add(menuItem);

        showDebuggingInfo = new JCheckBoxMenuItem("Show debugging info");
        showDebuggingInfo.setMnemonic(KeyEvent.VK_S);
        showDebuggingInfo.setToolTipText("Prints various debugging information to the java console, if it is up.");
        showDebuggingInfo.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                BSW.parentLogger.setLevel(showDebuggingInfo.isSelected() ? Level.ALL : Level.INFO);
            }
        });
        toolsMenu.add(showDebuggingInfo);

        menuItem = new JMenuItem("Create Launch Script");
        menuItem.setMnemonic(KeyEvent.VK_C);
        menuItem.setToolTipText(
                "<html><body>Creates a script that launches the application.<br>"
                + "Use this if you want to run this from the command line if you "
                + "downloaded it as a Web Start application.</body></html>");
        menuItem.setActionCommand("create_script");
        menuItem.addActionListener(menuHandler);
        toolsMenu.add(menuItem);

        horizontalLayout = new JCheckBoxMenuItem("Horizontal Layout");
        horizontalLayout.setMnemonic(KeyEvent.VK_H);
        horizontalLayout.setToolTipText("If checked, it lays the tool out horizontally, if not check it lays out vertically");
        horizontalLayout.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean selected = ((JCheckBoxMenuItem)e.getSource()).isSelected();
                splitPane.setOrientation(selected ? JSplitPane.HORIZONTAL_SPLIT : JSplitPane.VERTICAL_SPLIT);
            }
        });
        toolsMenu.add(horizontalLayout);

        toolsMenu.add(new JSeparator());

        menuItem  = new JMenuItem("Prepare for uploading...");
        menuItem.setToolTipText("Prepares the book for uploading to the Internet Archive");
        menuItem.setActionCommand("prepare_upload");
        menuItem.addActionListener(menuHandler);
        toolsMenu.add(menuItem);

        menuItem  = new JMenuItem("Upload to the Archive...");
        menuItem.setToolTipText("Uploads this book to the Internet Archive");
        menuItem.setActionCommand("upload");
        menuItem.addActionListener(menuHandler);
        toolsMenu.add(menuItem);

        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);
        menuBar.add(helpMenu);
        menuItem = new JMenuItem("About");
        menuItem.setMnemonic(KeyEvent.VK_A);
        helpMenu.add(menuItem);
        menuItem.setActionCommand("about");
        menuItem.addActionListener(menuHandler);
        menuItem = new JMenuItem("Show Help (wiki)");
        menuItem.setMnemonic(KeyEvent.VK_H);
        menuItem.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI("http://sourceforge.net/apps/mediawiki/bookscanwizard"));
                } catch (Exception ex) {
                    Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        helpMenu.add(menuItem);
        menuItem = new JMenuItem("Show Command Helper");
        menuItem.setMnemonic(KeyEvent.VK_S);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));

        menuItem.setActionCommand("command_helper");
        menuItem.addActionListener(menuHandler);
        helpMenu.add(menuItem);

        Container cp = getContentPane();
        cp.setLayout(new BoxLayout(cp, BoxLayout.Y_AXIS));
        viewerPanel = new ViewerPanel(menuHandler);
        JScrollPane imageScroll = new JScrollPane(viewerPanel);
        imageScroll.getVerticalScrollBar().setUnitIncrement(10);
        imageScroll.setPreferredSize(new Dimension(400, 400));

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));

        configEntry = new ConfigEntry(menuHandler);
        configEntry.addKeyListener(viewerPanel);

        operationList = new OperationList(this);

        this.addKeyListener(viewerPanel);
        JScrollPane pane = new JScrollPane(configEntry);
        pane.setPreferredSize(new Dimension(1000,1000));
        pane.setAlignmentX(Component.LEFT_ALIGNMENT);
        north.add(pane);
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        cp.add(buttonPanel);

        buttonPanel.add(Box.createHorizontalStrut(2));

        JButton previewButton = new JButton("Preview");
        previewButton.setToolTipText("Previews the configuration");
        previewButton.setActionCommand("preview");
        previewButton.addActionListener(menuHandler);
        buttonPanel.add(previewButton);
        buttonPanel.add(Box.createHorizontalStrut(2));

        runButton = new JButton("Submit");
        runButton.setToolTipText("Converts all images");
        runButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if ("Submit".equals(runButton.getText())) {
                    menuHandler.actionPerformed(new ActionEvent(e.getSource(), e.getID(), "run"));
                } else {
                    menuHandler.actionPerformed(new ActionEvent(e.getSource(), e.getID(), "abort"));
                }
            }
        });
        buttonPanel.add(runButton);
        buttonPanel.add(Box.createHorizontalStrut(2));

        JButton minusButton = new JButton("-");
        minusButton.setToolTipText("Moves to the previous page (or 2 pages back if the shift key is held down)");
        minusButton.setActionCommand("previousPage");
        minusButton.addActionListener(menuHandler);
        buttonPanel.add(minusButton);
        buttonPanel.add(Box.createHorizontalStrut(2));

        buttonPanel.add(pageListBox);
        buttonPanel.add(Box.createHorizontalStrut(2));

        JButton plusButton = new JButton("+");
        plusButton.setActionCommand("nextPage");
        plusButton.setToolTipText("Moves to the next page (or 2 pages forward if the shift key is held down)");
        plusButton.addActionListener(menuHandler);
        buttonPanel.add(plusButton);
        buttonPanel.add(Box.createHorizontalStrut(2));

        showPerspective.setText("Perspective");
        showPerspective.setToolTipText("<html><body>If checked it will preview the perspective.<br>Otherwise it will display the perspective marks.</body></html>");
        showPerspective.setSelected(true);
        showPerspective.setActionCommand("preview");
        showPerspective.addActionListener(menuHandler);
        buttonPanel.add(showPerspective);
        buttonPanel.add(Box.createHorizontalStrut(2));
        showPerspective.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!showPerspective.isSelected() && showCrops.isSelected()) {
                    showCrops.setSelected(false);
                }
            }
        });

        showCrops.setText("Crops");
        showCrops.setToolTipText("<html><body>If checked it will preview the corp.<br>Otherwise it will display the crop marks.</body></html>");
        showCrops.setSelected(true);
        showCrops.setActionCommand("preview");
        showCrops.addActionListener(menuHandler);
        buttonPanel.add(showCrops);
        buttonPanel.add(Box.createHorizontalStrut(2));
        showCrops.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!showPerspective.isSelected() && showCrops.isSelected()) {
                    showPerspective.setSelected(true);
                }
            }
        });

        showColors.setText("Filters");
        showColors.setToolTipText("If checked it will show color changes and adjustments.");
        showColors.setSelected(true);
        showColors.setActionCommand("preview");
        showColors.addActionListener(menuHandler);
        buttonPanel.add(showColors);
        buttonPanel.add(Box.createHorizontalStrut(2));

        JButton zoomIn = new JButton("Zoom in");
        zoomIn.setToolTipText("Zooms in on the previewed image");
        zoomIn.setActionCommand("zoomIn");
        zoomIn.addActionListener(menuHandler);
        buttonPanel.add(zoomIn);
        buttonPanel.add(Box.createHorizontalStrut(2));

        JButton zoomOut = new JButton("Zoom out");
        zoomOut.setToolTipText("Zooms out on the previewed image");
        zoomOut.setActionCommand("zoomOut");
        zoomOut.addActionListener(menuHandler);

        buttonPanel.add(zoomOut);
        buttonPanel.add(Box.createHorizontalStrut(10));
        status = new JLabel();
        buttonPanel.add(status);
        final JPanel spacer = new JPanel();
        spacer.setPreferredSize(new Dimension(2000,1));
        buttonPanel.add(spacer);

        progressBar = new JProgressBar(JProgressBar.HORIZONTAL) {
            @Override
            public void setVisible(boolean visible) {
                spacer.setVisible(!visible);
                super.setVisible(visible);
                status.setVisible(visible);
            }
        };
        progressBar.setVisible(false);
        progressBar.setPreferredSize(new Dimension(2000,10));
        buttonPanel.add(progressBar);

        splitPane =  new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);
        splitPane.setDividerLocation(.5);
        splitPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        cp.add(splitPane);
        splitPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        splitPane.setLeftComponent(north);

        JPanel viewerPane = new JPanel(new BorderLayout());
        viewerPane.add(imageScroll, BorderLayout.CENTER);
        leftPanel = new Panel();
        leftPanel.setPreferredSize(new Dimension(5,1));
        viewerPane.add(leftPanel, BorderLayout.WEST);
        rightPanel = new Panel();
        rightPanel.setPreferredSize(new Dimension(5,1));
        viewerPane.add(rightPanel, BorderLayout.EAST);
        splitPane.setRightComponent(viewerPane);

        setPreferredSize(new Dimension(1800, 800));
        pack();
        aboutDialog = new AboutDialog(this);

        FocusListener fl = new FocusListener(){
            @Override
            public void focusGained(FocusEvent e) {
                focusedComponent = (JComponent) e.getSource();
            }

            @Override
            public void focusLost(FocusEvent e) {
            }
        };
        configEntry.addFocusListener(fl);
        viewerPanel.addFocusListener(fl);
        boundsListener = new BoundsHelper(this);
    }

    public void setStatus(String status) {
        if (status == null) {
            runButton.setText("Submit");
        } else {
            runButton.setText(status);
        }
    }

    static {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            UserFeedbackHelper.displayException(null, ex);
        }
    }

    public void setImage(RenderedImage img, FileHolder fileHolder) {
        if (img == null) {
            viewerPanel.set(new BufferedImage(1,1, BufferedImage.TYPE_INT_RGB));
            return;
        }
        float postScale = BSW.instance().getPostScale();
        if (postScale != 1) {
            if (img.getSampleModel().getNumBands() == 1
                    && img.getSampleModel().getSampleSize()[0] == 1
                    && postScale < 1)
            {
                img = SubsampleBinaryToGrayDescriptor
                    .create(img, postScale, postScale, BSW.SPEED_HINTS);
            } else {
                ParameterBlock pb = new ParameterBlock().addSource(img).
                        add(postScale).
                        add(postScale);
                img = JAI.create("scale", pb, BSW.SPEED_HINTS);
            }
        }
        viewerPanel.setOrigin(-img.getMinX(), -img.getMinY());
        // renders the data now so the busy cursor works right.
        if (img instanceof RenderedOp) {
            ((RenderedOp) img).copyData();
        }
        viewerPanel.set(img);
        if (fileHolder.getPosition() == FileHolder.LEFT) {
            leftPanel.setBackground(Color.BLACK);
            rightPanel.setBackground(null);
        } else {
            leftPanel.setBackground(null);
            rightPanel.setBackground(Color.BLACK);
        }
    }

    public void setPageList(List<FileHolder> fileHolders) {
        Object selected = pageListBox.getSelectedItem();

        Vector<FileHolder> files = new Vector<FileHolder>();
        files.addAll(fileHolders);
        Collections.sort(files);
        pageListBox.setModel(new DefaultComboBoxModel(files));
        if (selected != null) {
            pageListBox.setSelectedItem(selected);
        } else if (!files.isEmpty()) {
            pageListBox.setSelectedIndex(0);
        }
        if (pageListBox.getSelectedIndex() < 0 && files.size() > 0) {
            pageListBox.setSelectedIndex(0);
        }
        pageListBox.setMaximumRowCount(25);
        pageListBox.setMaximumSize(pageListBox.getPreferredSize());
        pageListBox.setSize(pageListBox.getPreferredSize());
    }

    public void setBusy(boolean busy) {
        if (busy) {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        } else {
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
        viewerPanel.setBusy(busy);
        configEntry.setBusy(busy);
    }

    public ConfigEntry getConfigEntry() {
        return configEntry;
    }

    public JComboBox getPageListBox() {
        return pageListBox;
    }

    public ViewerPanel getViewerPanel() {
        return viewerPanel;
    }

    public JDialog getAboutDialog() {
        return aboutDialog;
    }

    public OperationList getOperationList() {
        return operationList;
    }

    public JCheckBoxMenuItem getShowDebuggingInfo() {
        return showDebuggingInfo;
    }

    public JSplitPane getSplitPane() {
        return splitPane;
    }

    public JCheckBoxMenuItem getHorizontalLayout() {
        return horizontalLayout;
    }

    public JProgressBar getProgressBar() {
        return progressBar;
    }

    public boolean isShowCrops() {
        return showCrops.isSelected();
    }

    public boolean isShowPerspective() {
        return showPerspective.isSelected();
    }

    public boolean isShowColors() {
        return showColors.isSelected();
    }

    public BoundsHelper getBoundsHelper() {
        return boundsListener;
    }

    public void setStatusLabel(String status) {
        this.status.setText(status);
    }

    private void invokeAction(String name, JComponent c, ActionEvent e) {
        Action action = c.getActionMap().get(name);
        if (action != null) {
            action.actionPerformed(new ActionEvent(c,
                               ActionEvent.ACTION_PERFORMED, (String)action.
                               getValue(Action.NAME),
                               EventQueue.getMostRecentEventTime(),
                               e.getModifiers()));
        }
    }
}
