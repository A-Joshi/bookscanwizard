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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

public class TextMessageDialog extends JDialog {
    private JLabel label;

    public TextMessageDialog(JFrame owner, String title, String message) {
        super(owner, "Book Scan Wizard", true);
        setIconImage(owner.getIconImage());
        JPanel p = new JPanel();
        getContentPane().add(p, BorderLayout.WEST);
        label = new JLabel(message);
        label.setBorder(new EmptyBorder(5, 10, 5, 10));
        label.setBackground(getBackground());
        p.add(label);
        getContentPane().add(p, BorderLayout.CENTER);
        JButton btOK = new JButton("OK");
        ActionListener lst = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        };
        btOK.addActionListener(lst);
        p = new JPanel();
        p.add(btOK);
        getContentPane().add(p, BorderLayout.SOUTH);
        pack();
        setResizable(false);
    }

    public JComponent getLabel() {
        return label;
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            int x = getParent().getX() + getParent().getWidth() / 2;
            int y = getParent().getY() + getParent().getHeight() / 2;
            setLocation(x, y);
        }
        super.setVisible(visible);
    }
}
