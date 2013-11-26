package net.sourceforge.bookscanwizard.util;

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
/*

/*
 * This class is a modified version of:
 * http://www.hero.com/public/HocrToPdf.java
 *
 * Copyright 2007, 2008
 *
 * Based on code from  
 * @author Florian Hackenberger <florian@hackenberger.at>
 * @author Kenneth Berland <ken@hero.com>
 */
import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.itextpdf.awt.geom.AffineTransform;
import com.itextpdf.text.BadElementException;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.CMYKColor;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfWriter;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.StartTag;
import net.htmlparser.jericho.StartTagType;

/**
 * This saves the OCR information as part of a pdf page
 */
public class Hocr {
    public static void writeToPDF(File hocrFile, PdfWriter pdfWriter, Image image, int dpi) throws IOException, BadElementException, DocumentException {
        float dotsPerPoint = dpi / 72.0f;
        float pageImageHeight = image.getHeight() / dotsPerPoint;
        Font defaultFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.BOLD, CMYKColor.BLACK);

        // Using the jericho library to parse the HTML file
        Source source = new Source(hocrFile);
        StartTag divTag = source.getNextStartTag(0, HTMLElementName.DIV, StartTagType.NORMAL);

        // Find the tag of class ocr_page in order to load the scanned image
        //System.out.println("div  tag start/end: " + divTag.getBegin() + ":" + divTag.getEnd() );
        Pattern imagePattern = Pattern.compile("image\\s+([^;]+)");
        Matcher imageMatcher = imagePattern.matcher(divTag.getElement().getAttributeValue("title"));
        if (!imageMatcher.find()) {
            throw new RuntimeException("Could not find a tag of class \"ocr_page\", aborting.");
        }

        // Put the text behind the picture (reverse for debugging)
        PdfContentByte cb = pdfWriter.getDirectContentUnder();
        //PdfContentByte cb = pdfWriter.getDirectContent();

        // In order to place text behind the recognised text snippets we are interested in the bbox property		
        Pattern bboxPattern = Pattern.compile("bbox(\\s+\\d+){4}");
        // This pattern separates the coordinates of the bbox property
        Pattern bboxCoordinatePattern = Pattern.compile("(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)");
        // Only tags of the ocr_line class are interesting
        StartTag ocrLineTag = source.getNextStartTag(divTag.getEnd(), "class", "ocr_line", false);
        AffineTransform matrix = new AffineTransform();
        //System.out.println("check: " + ocrLineTag.getBegin() + " < " + divTag.getElement().getEndTag().getEnd()  );
        while (ocrLineTag != null && ocrLineTag.getBegin() < divTag.getElement().getEndTag().getEnd()) {
            Element lineElement = ocrLineTag.getElement();
            Matcher bboxMatcher = bboxPattern.matcher(lineElement.getAttributeValue("title"));
            if (bboxMatcher.find()) {
                // We found a tag of the ocr_line class containing a bbox property
                Matcher bboxCoordinateMatcher = bboxCoordinatePattern.matcher(bboxMatcher.group());
                bboxCoordinateMatcher.find();
                float[] coordinates = new float[4];
                for (int i=0; i < 4; i++) {
                    coordinates[i] = Integer.parseInt(bboxCoordinateMatcher.group(i+1)) / dotsPerPoint;
                }
                float bboxWidthPt = coordinates[2] - coordinates[0];
                float bboxHeightPt = coordinates[3] - coordinates[1];
                float x =  coordinates[0];
                float y = pageImageHeight - coordinates[3];

                String line = lineElement.getContent().getTextExtractor().toString();
                // Put the text into the PDF
                cb.beginText();
                // Comment the next line to debug the PDF output (visible Text)
                cb.setTextRenderingMode(PdfContentByte.TEXT_RENDER_MODE_INVISIBLE);
                // Set the base font height
                cb.setFontAndSize(defaultFont.getBaseFont(), bboxHeightPt);
                float width = defaultFont.getBaseFont().getWidthPoint(line, bboxHeightPt);
                matrix.setToTranslation(x, y);
                // scale width to take up the entire bounding box
                matrix.scale(bboxWidthPt / width, 1);
                cb.setTextMatrix(matrix);
                cb.showText(line);
                cb.endText();
            }
            ocrLineTag = source.getNextStartTag(ocrLineTag.getEnd(), "class", "ocr_line", false);
        }
        hocrFile.delete();
    }
}
