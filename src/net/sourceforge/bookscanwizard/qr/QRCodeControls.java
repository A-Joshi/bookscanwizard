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
package net.sourceforge.bookscanwizard.qr;

public enum QRCodeControls {
    PERSPECTIVE("Perspective Page"),
    GRAY("Next Pages Gray"),
    BW ("Next Pages B/W"),
    COLOR ("Next Pages Color"),
    REDO ("Redo previous page set"),
    FLAG ("Flag Spot"),
    END ("End Book"),
    SKIP ("Skip this page"),
    DPI ("Set the Source DPI");

    private final String description;

    QRCodeControls(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
