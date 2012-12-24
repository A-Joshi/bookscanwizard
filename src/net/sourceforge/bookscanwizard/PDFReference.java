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

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.ImageRenderInfo;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.RenderListener;
import com.itextpdf.text.pdf.parser.TextRenderInfo;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A reference to a PDF source document.
 */
public class PDFReference {
    private int pageCount;
    private PdfReader reader;
    private PdfReaderContentParser parser;
    private RenderListener listener;
    private BufferedImage lastImage;
    
    public PDFReference(File f) throws IOException {
        reader = new PdfReader(f.getPath());
        pageCount =  reader.getNumberOfPages();
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }

    public PdfReader getReader() {
        return reader;
    }

    public void setReader(PdfReader reader) {
        this.reader = reader;
    }
    
    public RenderedImage getImage(int page) throws IOException {
        if (parser == null) {
            parser = new PdfReaderContentParser(reader);
            listener = new BSWImageRenderListener();
        }
        lastImage = null;
        parser.processContent(page+1, listener);
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
