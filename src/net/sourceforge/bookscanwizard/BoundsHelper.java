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

import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import javax.swing.JFrame;

/**
 * Keeps track of the unmaximized bounds of a window.
 */
public class BoundsHelper {
    private JFrame frame;
    private Rectangle unmaximizedBounds;
    private int lastState;

    public BoundsHelper(JFrame fr) {
        this.frame = fr;
        frame.addComponentListener(new ComponentListener(){

            @Override
            public void componentResized(ComponentEvent e) {
                updateBounds();
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                updateBounds();
            }

            @Override
            public void componentShown(ComponentEvent e) {
                updateBounds();
            }

            @Override
            public void componentHidden(ComponentEvent e) {
            }
        });
    }

    public Rectangle getUnmaximizedBounds() {
        return unmaximizedBounds;
    }

    public int getLastState() {
        return lastState;
    }

    private void updateBounds() {
        lastState = frame.getExtendedState();
        if (frame.getExtendedState() == JFrame.NORMAL) {
            unmaximizedBounds = frame.getBounds();
        }
    }
}
