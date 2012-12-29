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

package net.sourceforge.bookscanwizard.start;

import java.awt.Dimension;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class Page1 extends AbstractPage {

    public static String getDescription() {
        return "Enter Scan Settings";
    }

    private static Dimension LABEL_SIZE = new Dimension(200,1);

    public Page1() {
        JComponent pane;
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        JTextField tf;
        JLabel label;
        JComboBox comboBox;
        JCheckBox checkBox;
        String[] choices;
        String value;

        addDirectory("Working directory", NewBook.WORKING_DIRECTORY, "Sets the directory that the script should be saved to.");
        addDirectory("Source directory", NewBook.SOURCE_DIRECTORY, "Sets the directory that contains the images, or the l & r folders");
        addDirectory("Destination directory", NewBook.DESTINATION_DIRECTORY, "Sets the directory that the output files should be stored");

        pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.LINE_AXIS));
        label = new JLabel("Use QR Barcodes");
        label.setPreferredSize(LABEL_SIZE);
        pane.add(label);
        checkBox = new JCheckBox();
        checkBox.setName(NewBook.USE_BARCODES);
        checkBox.setSelected((Boolean) getDefaults().get(NewBook.USE_BARCODES));
        pane.add(checkBox);
        pane.add(Box.createHorizontalGlue());
        add(pane);
        add(Box.createVerticalStrut(10));


        pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.LINE_AXIS));
        label = new JLabel("Override Source DPI");
        label.setPreferredSize(LABEL_SIZE);
        pane.add(label);
        tf = new JTextField(10);
        tf.setName(NewBook.SOURCE_DPI);
        tf.setPreferredSize(new Dimension(100,20));
        tf.setMaximumSize(getPreferredSize());
        pane.add(tf);
        pane.add(Box.createHorizontalGlue());
        add(pane);
        add(Box.createVerticalStrut(10));

        pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.LINE_AXIS));
        label = new JLabel("Destination DPI");
        label.setPreferredSize(LABEL_SIZE);
        pane.add(label);
        choices = new String[] {"Keep Source DPI", "300", "450", "600"};
        comboBox = new JComboBox(choices) {
            @Override
            public Dimension getMaximumSize() {
                return super.getPreferredSize();
            }
        };
        comboBox.setName(NewBook.DESTINATION_DPI);
        value = (String) getDefaults().get(NewBook.DESTINATION_DPI);
        if (value != null) {
            comboBox.setSelectedItem(value);
        }
        comboBox.setEditable(true);
        pane.add(comboBox);
        add(pane);
        pane.add(Box.createHorizontalGlue());
        add(Box.createVerticalStrut(10));

        pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.LINE_AXIS));
        label = new JLabel("Use Compression");
        label.setPreferredSize(LABEL_SIZE);
        pane.add(label);
        choices = new String[] {"NONE", "DEFLATE", "GROUP4"};
        comboBox = new JComboBox(choices) {
            @Override
            public Dimension getMaximumSize() {
                return super.getPreferredSize();
            }
        };
        comboBox.setName(NewBook.COMPRESSION);
        value = (String) getDefaults().get(NewBook.COMPRESSION);
        if (value != null) {
            comboBox.setSelectedItem(value);
        }
        pane.add(comboBox);
        add(pane);
        pane.add(Box.createHorizontalGlue());
        add(Box.createVerticalStrut(10));


        pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.LINE_AXIS));
        label = new JLabel("Output type");
        label.setPreferredSize(LABEL_SIZE);
        pane.add(label);
        choices = new String[] {"Color", "Greyscale", "B/W"};
        comboBox = new JComboBox(choices) {
            @Override
            public Dimension getMaximumSize() {
                return super.getPreferredSize();
            }
        };
        comboBox.setName(NewBook.OUTPUT_TYPE);
        value = (String) getDefaults().get(NewBook.OUTPUT_TYPE);
        if (value != null) {
            comboBox.setSelectedItem(value);
        }
        pane.add(comboBox);
        add(pane);
        pane.add(Box.createHorizontalGlue());
        add(Box.createVerticalGlue());
    }

    private void addDirectory(String name, String key, String help) {
        JLabel label;
        JTextField tf;
        JButton button;

        JPanel pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.LINE_AXIS));
        label = new JLabel(name);
        label.setToolTipText(help);
        label.setPreferredSize(LABEL_SIZE);
        pane.add(label);
        tf = new JTextField(20);
        String value = (String) getDefaults().get(key);
        if (value != null) {
            tf.setText(value);
        }
        tf.setName(key);
        pane.add(tf);
        pane.add(Box.createHorizontalStrut(10));
        button = new JButton("Choose");
        button.addActionListener(new FileChooser(tf));
        pane.add(button);
        add(pane);
        add(Box.createVerticalStrut(10));
    }
}
