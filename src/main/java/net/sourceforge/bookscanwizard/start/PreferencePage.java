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

import javax.swing.BoxLayout;

public final class PreferencePage extends AbstractPage {
    public static final String START_WITH_PERSPECTIVE = "start_with_perspective";
    public static final String START_WITH_CROP = "start_with_crop";
    public static final String START_WITH_FILTERS = "start_with_filters";
    public static final String HORIZONTAL_LAYOUT = "horizontal_layout";
    public static final String SHOW_DEBUGGING = "show_debugging";
    public static final String PREVIEW_ON_STARTUP = "preview_on_startup";
    public static final String MIN_ZOOM = "min_zoom";
    public static final String MAX_ZOOM = "max_zoom";
    
    
    public static String getDescription() {
        return "Enter Preferences";
    }

    public PreferencePage() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        
        checkbox(START_WITH_PERSPECTIVE, "Perspective enabled on startup");
        checkbox(START_WITH_CROP, "Crop enabled on startup");
        checkbox(START_WITH_FILTERS, "Filters enabled on startup");
        checkbox(HORIZONTAL_LAYOUT, "Filters enabled on startup");
        checkbox(SHOW_DEBUGGING, "Show debugging information");
        checkbox(PREVIEW_ON_STARTUP, "Show preview when config is first started");
        
        textField(MIN_ZOOM, "Minimum zoom value for slider");
        textField(MAX_ZOOM, "Maximum zoom value for slider");
    }
}
