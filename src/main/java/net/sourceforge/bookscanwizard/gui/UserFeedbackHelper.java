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

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventObject;
import java.util.concurrent.ExecutionException;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import net.sourceforge.bookscanwizard.BSW;
import net.sourceforge.bookscanwizard.UserException;

/**
 * This class contains a wrapper for an action listener that displays the busy
 * cursor and handles displaying exceptions to the user.
 */
abstract public class UserFeedbackHelper implements ActionListener {
    public final static Cursor busyCursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
    public final static Cursor defaultCursor = Cursor.getDefaultCursor();

    public abstract void cursorActionPerformed(ActionEvent e) throws Exception;

    @Override
    @SuppressWarnings({"BroadCatchBlock", "TooBroadCatch"})
    public final void actionPerformed(ActionEvent e) {
        MainFrame fr = BSW.instance().getMainFrame();
        try {
            fr.setBusy(true);
            cursorActionPerformed(e);
        } catch (Throwable ex) {
            displayException((Component) e.getSource(), ex);
        } finally {
            fr.setBusy(false);
        }
    }

    public static void doAction(EventObject e, Runnable runnable) {
        MainFrame fr = BSW.instance().getMainFrame();
        try {
            fr.setBusy(true);
            runnable.run();
        } catch (Throwable ex) {
            displayException((Component) e.getSource(), ex);
        } finally {
            fr.setBusy(false);
        }
    }

public void run() {
        ActionEvent evt = new ActionEvent(BSW.instance().getMainFrame(), 0, null);
        actionPerformed(evt);
    }

    public static synchronized void displayException(final Component c, final Throwable ex) {
        ex.printStackTrace();
        if (BSW.isBatchMode()) {
            System.exit(2);
        } else {
            Runnable r = new Runnable() {
                @Override
                @SuppressWarnings("ThrowableResultIgnored")
                public void run() {
                    Throwable displayException = ex;
                    if (displayException instanceof ExecutionException) {
                        displayException = displayException.getCause();
                    }
                    Throwable ue = displayException;
                    while(ue.getCause() != null) {
                        if (ue instanceof UserException) {
                            displayException = ue;
                            break;
                        }
                        ue = ue.getCause();
                    }
                    String message = displayException instanceof UserException 
                            ? displayException.getMessage() : displayException.toString();
                    JOptionPane.showMessageDialog(c, message, "", JOptionPane.INFORMATION_MESSAGE);
                }
            };
            SwingUtilities.invokeLater(r);
            Toolkit.getDefaultToolkit().beep();
        }
    }
}
