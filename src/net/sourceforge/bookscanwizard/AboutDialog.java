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

import java.awt.Desktop;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import javax.swing.JFrame;

public class AboutDialog extends TextMessageDialog {
    private static final String URL = "http://bookscanwizard.sourceforge.net/";
    public static final String VERSION = "1.1b";

    private static final String message =
        "<html><head/>\n"+
        "<body>\n"+
        "<p><center><b>Book Scan Wizard</b> by Steve Devore</center></p>"+
        "<p><a href='"+URL+"'>"+URL+"</a></p>"+
        "<p> Version "+VERSION+"</p>"+
        "</body>"+
        "</html>";

    public AboutDialog(JFrame owner) {
        super(owner, "Book Scan Wizard", message);
        getLabel().addMouseListener(new MouseAdapter(){
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI(URL));
                } catch (Exception ex) {
                    // ignore;
                }
                setVisible(false);
            }
        });
    }
}
