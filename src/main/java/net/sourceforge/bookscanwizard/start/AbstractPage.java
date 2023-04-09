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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;
import net.sourceforge.bookscanwizard.PrefsHelper;
import org.netbeans.spi.wizard.WizardPage;
import org.netbeans.spi.wizard.WizardPage.CustomComponentListener;

/**
 * The wizard doesn't seem to support JComboBoxes correctly.. this adds
 * support for them.
 */
public class AbstractPage extends WizardPage {
    protected static final Dimension LABEL_SIZE = new Dimension(200,1);

    protected static final Map<String,Serializable> defaults = new HashMap<>();

    static {
        defaults.put(NewBook.COMPRESSION, "NONE");
        defaults.put(NewBook.DESTINATION_DIRECTORY, "tiff");
        defaults.put(NewBook.LEFT_ORIENT, "-90");
        defaults.put(NewBook.OUTPUT_TYPE, "Color");
        defaults.put(NewBook.RIGHT_ORIENT, "90");
        defaults.put(NewBook.SOURCE_DIRECTORY, "source");
        defaults.put(NewBook.ESTIMATED_DPI1, "");
        defaults.put(NewBook.ESTIMATED_DPI2, "");
        defaults.put(NewBook.FOCAL_LENGTH1, "");
        defaults.put(NewBook.FOCAL_LENGTH2, "");
        defaults.put(NewBook.USE_BARCODES, Boolean.FALSE);
        defaults.put(NewBook.USE_FOCAL_LENGTH, Boolean.FALSE);
        defaults.put(NewBook.WORKING_DIRECTORY, "");
        defaults.put(NewBook.DESTINATION_DPI, "300");

        defaults.put(PreferencePage.START_WITH_PERSPECTIVE, true);
        defaults.put(PreferencePage.START_WITH_CROP, true);
        defaults.put(PreferencePage.START_WITH_FILTERS, true);
        defaults.put(PreferencePage.HORIZONTAL_LAYOUT, true);
        defaults.put(PreferencePage.SHOW_DEBUGGING, true);
        defaults.put(PreferencePage.PREVIEW_ON_STARTUP, true);
        defaults.put(PreferencePage.MIN_ZOOM, ".1");
        defaults.put(PreferencePage.MAX_ZOOM, "10");        
        
        for (Map.Entry<String,Object> entry : PrefsHelper.getPrefs().entrySet()) {
            defaults.put(entry.getKey(), (Serializable) entry.getValue());
        }
    }

    public static Map<String,Serializable> getDefaults() {
        return defaults;
    }
    
    public Object getDefault(String key, Object defaultValue) {
        Object value = defaults.get(key);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }

    public static void putDefaults(Map<String,Serializable> defaults) {
        if (defaults != null) {
            AbstractPage.defaults.putAll(defaults);
         }
    }

    /**
     * Saves the settings which will be saved as user preferences.
     */
    static void putMatchingSettings(Map<String, Serializable> settings) {
        for (Map.Entry<String,Serializable> entry : defaults.entrySet()) {
            Serializable obj = settings.get(entry.getKey());
            if (obj != null) {
                entry.setValue(obj);
            }
        }
    }

    @Override
    protected CustomComponentListener createCustomComponentListener() {
        return new CustomComponentListener() {
            private ActionListener actionListener;

            @Override
            public boolean accept(Component cmpnt) {
                return cmpnt instanceof JComboBox;
            }

            @Override
            public void startListeningTo(final Component cmpnt,
                    final CustomComponentNotifier ccn) {
                final JComboBox cb = (JComboBox) cmpnt;
                actionListener = new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        ccn.userInputReceived(cmpnt, valueFor(cmpnt));
                    }
                };
                cb.addActionListener(actionListener);
            }

            @Override
            public void stopListeningTo(Component cmpnt) {
                final JComboBox cb = (JComboBox) cmpnt;
                cb.removeActionListener(actionListener);
            }

            @Override
            public Object valueFor(Component cmpnt) {
                final JComboBox cb = (JComboBox) cmpnt;
                return cb.getSelectedItem();
            }
        };
    }
    
    protected void checkbox(String name, String description) {
        boolean defaultValue = (Boolean) getDefaults().get(name);
        JPanel pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.LINE_AXIS));
        JLabel label = new JLabel(description);
        label.setPreferredSize(LABEL_SIZE);
        pane.add(label);
        JCheckBox checkBox = new JCheckBox();
        checkBox.setName(name);
        checkBox.setSelected((Boolean) getDefault(name, defaultValue));
        pane.add(checkBox);
        pane.add(Box.createHorizontalGlue());
        add(pane);
        add(Box.createVerticalStrut(5));
    }

    protected void textField(String name, String description) {
        String defaultValue = (String) getDefaults().get(name);
        JPanel pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.LINE_AXIS));
        JLabel label = new JLabel(description);
        label.setPreferredSize(LABEL_SIZE);
        pane.add(label);
        JTextField textField = new JTextField(10);
        textField.setPreferredSize(new Dimension(100,20));
        textField.setName(name);
        textField.setText(defaultValue);
        pane.add(textField);
        pane.add(Box.createHorizontalGlue());
        add(pane);
        add(Box.createVerticalStrut(5));
    }
    
    protected class FileChooser implements ActionListener {
        private JTextComponent tc;

        public FileChooser(JTextComponent tc) {
            this.tc = tc;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Working Directory");
            fc.setCurrentDirectory(new File(tc.getText()));
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fc.setApproveButtonText("Select");
            int returnVal = fc.showOpenDialog(AbstractPage.this);
            if (returnVal != JFileChooser.APPROVE_OPTION) {
                return;
            }
            tc.setText(fc.getSelectedFile().getPath());
        }
    }

}
