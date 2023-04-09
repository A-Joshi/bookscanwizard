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

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import org.icepdf.core.exceptions.PDFException;
import org.icepdf.core.exceptions.PDFSecurityException;
import org.icepdf.core.pobjects.Document;
import org.icepdf.core.pobjects.PDimension;
import org.icepdf.core.pobjects.Page;
import org.icepdf.core.pobjects.Thumbnail;
import org.icepdf.core.util.GraphicsRenderingHints;

/**
 * This is a fallback pdf renderer.  For this renderer we assume it is 
 * set to 300 dpi.
 */
public class PDFReferenceAlternate {
    private Document document;
    // If a PDF doesn't have any images, we need some sort of arbritrary size.
    // this should be configurable.
    private static final float dpi = 300;
    private static final float mult = dpi / 72.0f;
    
    public PDFReferenceAlternate(File f) {
        document = new Document();
        try {
            document.setFile(f.getPath());
        } catch (PDFException | PDFSecurityException | IOException e) {
            throw new UserException("Could not read " + f + " as a PDF file");
        }
    }

    public RenderedImage getThumbnail(int pg) throws IOException {
        RenderedImage retVal;
        Thumbnail thumbnail = document.getPageTree().getPage(pg-1).getThumbnail();
        if (thumbnail != null) {
            retVal = thumbnail.getImage();
        } else {
            retVal = getImage(pg);
        }
        return retVal;
    }
    
    public RenderedImage getImage(int pg) throws IOException {
        int page = pg - 1;  // pages start with 0 with this toolkit
        PDimension pDimension = document.getPageDimension(page, 0f);
        int width = (int) Math.round((pDimension.getWidth() * mult));
        int height = (int) Math.round((pDimension.getHeight() * mult));
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D imageGraphics = image.createGraphics();
        document.paintPage(page, imageGraphics, GraphicsRenderingHints.PRINT, Page.BOUNDARY_CROPBOX, 0f, mult);
        return (RenderedImage) image;
    }

    public float getDpi() {
        return dpi;
    }
}
