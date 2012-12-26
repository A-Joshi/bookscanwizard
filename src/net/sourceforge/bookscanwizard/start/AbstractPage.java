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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JComboBox;
import org.netbeans.spi.wizard.WizardPage;
import org.netbeans.spi.wizard.WizardPage.CustomComponentListener;

/**
 * The wizard doesn't seem to support JComboBoxes correctly.. this adds
 * support for them.
 */
public class AbstractPage extends WizardPage {
    private static final Map<String,Serializable> defaults = new HashMap<String,Serializable>();

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
    }

    public static Map<String,Serializable> getDefaults() {
        return defaults;
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
}
