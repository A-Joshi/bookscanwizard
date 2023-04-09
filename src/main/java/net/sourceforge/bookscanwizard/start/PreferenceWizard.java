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

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import net.sourceforge.bookscanwizard.gui.UserFeedbackHelper;
import org.netbeans.api.wizard.WizardDisplayer;
import org.netbeans.spi.wizard.Wizard;
import org.netbeans.spi.wizard.WizardPage;


public class PreferenceWizard {

    private final Wizard wizard;

    public PreferenceWizard() {
        wizard = WizardPage.createWizard(new Class[] {PreferencePage.class});
    }

    public Map<String,Serializable> getConfig() throws IOException {
        Map<String,Serializable> settings =
            (Map<String,Serializable>) WizardDisplayer.showWizard(wizard);
        if (settings != null) {
            for (Map.Entry<String,Serializable> entry : settings.entrySet()) {
                AbstractPage.getDefaults().put(entry.getKey(), entry.getValue());
            }
        }
        return settings;
    }

    static {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            UserFeedbackHelper.displayException(null, ex);
        }
    }

    public static void main(String[] args) throws Exception {
        PreferenceWizard newBook = new PreferenceWizard();
        for (Map.Entry<String,Serializable> entry : newBook.getConfig().entrySet()) {
            AbstractPage.getDefaults().put(entry.getKey(), entry.getValue());
        }
    }
    
    
}
