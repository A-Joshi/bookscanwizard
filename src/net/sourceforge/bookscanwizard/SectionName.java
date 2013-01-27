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

package net.sourceforge.bookscanwizard;

import java.util.Arrays;
import net.sourceforge.bookscanwizard.op.NormalizeLighting;
import net.sourceforge.bookscanwizard.op.RemovePages;

/**
 * The list of config sections. 
 */
public enum SectionName {
    BEGIN_MARKER("Begin Marker", false), 
    LOAD_FILES ("Load Files", false),
    REMOVE_PAGES("Remove Pages", false),
    ROTATIONS ("Page Rotations", true),
    PRE_FILTERS("Prefilters", true),
    PERSPECTIVE ("Perspective", true),
    CROPS ("Crops", true),
    FILTERS ( "Filters", false),
    SCALING ("Scaling", false),
    OUTPUT ("Output", false);
    
    private String text;
    private boolean separateLR;
    
    SectionName(String text, boolean separateLR) {
        this.text = text;
        this.separateLR = separateLR;
    }
    
    public String getDescription() {
        return text;
    }
    
    public String getMatchString() {
        return "*** "+text+" ***";
    }
    
    public boolean isLRSeperate() {
        return separateLR;
    }

    public static SectionName getSectionFromOp(Operation op) {
        SectionName retVal = null;
        if (op instanceof PerspectiveOp) {
            retVal = PERSPECTIVE;
        } else if (op instanceof CropOp) {
            retVal = CROPS;
        } else if (op instanceof NormalizeLighting) {
            retVal = PRE_FILTERS;
        } else if (op instanceof ColorOp) {
            retVal = FILTERS;
        } else if (op instanceof RemovePages) {
            retVal = REMOVE_PAGES;
        } else if (op.getClass().getName().contains("Scaling")) {
            retVal = SCALING;
        } else if (op instanceof SaveOperation) {
            retVal = OUTPUT;
        }
        return retVal;
    }
    
    public static SectionName getPreviousSection(SectionName name) {
        int pos = Arrays.asList(values()).indexOf(name) -1;
        return values()[pos];
    }
}
