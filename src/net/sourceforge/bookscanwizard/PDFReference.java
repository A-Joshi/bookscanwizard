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

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.ImageRenderInfo;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.RenderListener;
import com.itextpdf.text.pdf.parser.TextRenderInfo;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A reference to a PDF source document.
 */
public class PDFReference {
    private static final Logger logger = Logger.getLogger(PDFReference.class.getName());
    private static final Map<File,PDFReference> references = new WeakHashMap<>();
    
    protected int pageCount;

    private PdfReader reader;
    private PdfReaderContentParser parser;
    private RenderListener listener;
    private RenderedImage lastImage;
    private File file;
    private PDFReferenceAlternate alternate;
    
    protected PDFReference() {}

    public static PDFReference getReference(File f) throws IOException {
        synchronized(references) {
           PDFReference ref = references.get(f);
           if (ref == null) {
               ref = new PDFReference(f);
               references.put(f, ref);
           }
           return ref;
        }
    }
    
    private PDFReference(File f) throws IOException {
        this.file = f;
        this.reader = new PdfReader(f.getPath());
        this.pageCount =  reader.getNumberOfPages();
    }

    public int getPageCount() {
        return pageCount;
    }

    public RenderedImage getThumbnail(int pg) throws IOException {
        if (alternate != null) {
            return alternate.getThumbnail(pg);
        }
        return getImage(pg);
    }
    
    public RenderedImage getImage(int page) throws IOException {
        if (alternate != null) {
            return alternate.getImage(page);
        }
        if (parser == null) {
            parser = new PdfReaderContentParser(reader);
            listener = new BSWImageRenderListener();
        }
        lastImage = null;
        parser.processContent(page, listener);
        if (lastImage == null || lastImage.getHeight() <=1 || lastImage.getHeight() <=1) {
            synchronized(this) {
                logger.log(Level.INFO, "Problem reading {0}. using alternate renderer", file);
                if (alternate == null) {
                    alternate = new PDFReferenceAlternate(file);
                }
                reader.close();
            }
            return alternate.getImage(page);
        }
        return lastImage;
    }

    private class BSWImageRenderListener implements RenderListener {
        @Override
        public void beginTextBlock() {
        }

        @Override
        public void renderText(TextRenderInfo renderInfo) {
        }

        @Override
        public void endTextBlock() {
        }

        @Override
        public void renderImage(ImageRenderInfo renderInfo) {
            try {
                lastImage = renderInfo.getImage().getBufferedImage();
            } catch (IOException ex) {
                Logger.getLogger(PDFReference.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
