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

import java.awt.image.RenderedImage;
import java.beans.*;
import java.io.Serializable;


/**
 * Bean that contains data from the import form.
 */
public class ImportImagesData implements Serializable {
    private final PropertyChangeSupport propertySupport;

    public static final String PROP_SOURCE = "source";
    public static final String PROP_DESTINATION = "destination";
    public static final String PROP_RIGHTIMAGECOUNT = "rightImageCount";
    public static final String PROP_RIGHTIMAGE = "rightImage";
    public static final String PROP_LEFTPAGENUMBER = "leftPageNumber";
    public static final String PROP_TITLE = "";
    public static final String PROP_RIGHTPAGENUMBER = "rightPageNumber";
    public static final String PROP_LEFTIMAGECOUNT = "leftImageCount";
    private String source = "source";
    private String destination = "destination";
    transient private String title = "";
    transient private String leftImageCount = "--";
    transient private String rightImageCount = "--";
    transient private int leftPageNumber = 1;
    transient private int rightPageNumber = 1;
    transient private RenderedImage leftImage;
    transient private RenderedImage rightImage;

    public String getSource() {
        return source;
    }

    public String getRightImageCount() {
        return rightImageCount;
    }

    public void setRightImageCount(String rightImageCount) {
        String oldRightImageCount = this.rightImageCount;
        this.rightImageCount = rightImageCount;
        propertySupport.firePropertyChange(PROP_RIGHTIMAGECOUNT, oldRightImageCount, rightImageCount);
    }

    public String getDestination() {
        return destination;
    }
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        String oldTitle = this.title;
        this.title = title;
        propertySupport.firePropertyChange(PROP_TITLE, oldTitle, title);
    }

    public int getLeftPageNumber() {
        return leftPageNumber;
    }

    public void setLeftPageNumber(int leftPageNumber) {
        int oldLeftPageNumber = this.leftPageNumber;
        this.leftPageNumber = leftPageNumber;
        propertySupport.firePropertyChange(PROP_LEFTPAGENUMBER, oldLeftPageNumber, leftPageNumber);
    }
    public String getLeftImageCount() {
        return leftImageCount;
    }
    public int getRightPageNumber() {
        return rightPageNumber;
    }

    public void setRightPageNumber(int rightPageNumber) {
        int oldRightPageNumber = this.rightPageNumber;
        this.rightPageNumber = rightPageNumber;
        propertySupport.firePropertyChange(PROP_RIGHTPAGENUMBER, oldRightPageNumber, rightPageNumber);
    }

    public void setLeftImageCount(String leftImageCount) {
        String oldLeftImageCount = this.leftImageCount;
        this.leftImageCount = leftImageCount;
        propertySupport.firePropertyChange(PROP_LEFTIMAGECOUNT, oldLeftImageCount, leftImageCount);
    }

    public void setDestination(String destination) {
        String oldDestination = this.destination;
        this.destination = destination;
        propertySupport.firePropertyChange(PROP_DESTINATION, oldDestination, destination);
    }

    public void setSource(String source) {
        String oldSource = this.source;
        this.source = source;
        propertySupport.firePropertyChange(PROP_SOURCE, oldSource, source);
    }
    public ImportImagesData() {
        propertySupport = new PropertyChangeSupport(this);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.removePropertyChangeListener(listener);
    }

    public RenderedImage getLeftImage() {
        return leftImage;
    }

    public void setLeftImage(RenderedImage leftImage) {
        this.leftImage = leftImage;
    }
    public RenderedImage getRightImage() {
        return rightImage;
    }

    public void setRightImage(RenderedImage rightImage) {
        RenderedImage oldRightImage = this.rightImage;
        this.rightImage = rightImage;
        propertySupport.firePropertyChange(PROP_RIGHTIMAGE, oldRightImage, rightImage);
    }
}
