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

package net.sourceforge.bookscanwizard.util;

import javax.swing.JEditorPane;
import javax.swing.UIManager;

/**
 * A component that looks like a label, but is selectable, even when displaying
 * html.
 */
public class SelectableLabel extends JEditorPane {
    public SelectableLabel() {
          setEditable(false);
          setBorder(null);
          setForeground(UIManager.getColor("Label.foreground"));
          setBackground(UIManager.getColor("Label.background"));
          setFont(UIManager.getFont("Label.font"));
          setContentType("text/html");
    }
}
