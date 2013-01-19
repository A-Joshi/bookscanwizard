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
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import net.sourceforge.bookscanwizard.op.EstimateDPI;

public class Page2 extends AbstractPage {
    private JComponent showFocalLengths;

    public static String getDescription() {
        return "Enter addtional content information";
    }

    @Override
    protected void renderingPage() {
        boolean hide = (EstimateDPI.getConfig().isEmpty());
        showFocalLengths.setVisible(!hide);
    }

    public Page2() {
        JComponent pane;
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        JTextField tf;
        JLabel label;
        JComboBox comboBox;
        JCheckBox checkBox;
        String[] choices;
        String value;

        JPanel showRotations = new JPanel();
        showRotations.setLayout(new BoxLayout(showRotations, BoxLayout.PAGE_AXIS));
        add(showRotations);

        pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.LINE_AXIS));
        label = new JLabel("Left Page orientation");
        label.setPreferredSize(LABEL_SIZE);
        pane.add(label);
        choices = new String[] {"0", "90", "-90", "180"};
        comboBox = new JComboBox(choices) {
            @Override
            public Dimension getMaximumSize() {
                return super.getPreferredSize();
            }
        };
        comboBox.setName(NewBook.LEFT_ORIENT);
        value = (String) getDefaults().get(NewBook.LEFT_ORIENT);
        if (value != null) {
            comboBox.setSelectedItem(value);
        }
        pane.add(comboBox);
        showRotations.add(pane);
        pane.add(Box.createHorizontalGlue());
        showRotations.add(Box.createVerticalStrut(10));

        pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.LINE_AXIS));
        label = new JLabel("Right Page orientation");
        label.setPreferredSize(LABEL_SIZE);
        pane.add(label);
        choices = new String[] {"0", "90", "-90", "180"};
        comboBox = new JComboBox(choices) {
            @Override
            public Dimension getMaximumSize() {
                return super.getPreferredSize();
            }
        };
        comboBox.setName(NewBook.RIGHT_ORIENT);
        value = (String) getDefaults().get(NewBook.RIGHT_ORIENT);
        if (value != null) {
            comboBox.setSelectedItem(value);
        }
        pane.add(comboBox);
        showRotations.add(pane);
        pane.add(Box.createHorizontalGlue());
        showRotations.add(Box.createVerticalStrut(10));

        pane = new JPanel();
        showFocalLengths = pane;
        pane.setLayout(new BoxLayout(pane, BoxLayout.LINE_AXIS));
        label = new JLabel("Use Focal Length for source DPI");
        label.setPreferredSize(LABEL_SIZE);
        pane.add(label);
        checkBox = new JCheckBox();
        checkBox.setName(NewBook.USE_FOCAL_LENGTH);
        checkBox.setSelected((Boolean) getDefaults().get(NewBook.USE_FOCAL_LENGTH));
        pane.add(checkBox);
        pane.add(Box.createHorizontalGlue());
        showRotations.add(pane);
        showRotations.add(Box.createVerticalStrut(10));
    }
}
