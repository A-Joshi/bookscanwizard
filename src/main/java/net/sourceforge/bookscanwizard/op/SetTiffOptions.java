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

package net.sourceforge.bookscanwizard.op;

import com.sun.media.jai.codec.TIFFEncodeParam;
import java.util.List;
import net.sourceforge.bookscanwizard.Operation;
import net.sourceforge.bookscanwizard.PageSet;
import net.sourceforge.bookscanwizard.UserException;

public class SetTiffOptions extends Operation {
    @Override
    protected List<Operation> setup(List<Operation> operationList) throws Exception {
        String[] args = getTextArgs();
        PageSet.setDestinationDPI(Integer.parseInt(args[0]));
        if (args.length > 0) {
            String type = args[1];
            pageSet.setCompressionType(getTiffCompressionType(type));
        }
        return operationList;
    }
    
    /**
     * Returns the compression type to use for saving TIFF images.
     * @param type name of the compression
     * @return the integer that represents the compression type.
     */
    public static int getTiffCompressionType(String type) {
        int compressionType;
        switch (type) {
            case "NONE":
                compressionType = TIFFEncodeParam.COMPRESSION_NONE;
                break;
            case "DEFLATE":
                compressionType = TIFFEncodeParam.COMPRESSION_DEFLATE;
                break;
            case "GROUP4":
                compressionType = TIFFEncodeParam.COMPRESSION_GROUP4;
                break;
            case "JPEG":
                compressionType = TIFFEncodeParam.COMPRESSION_JPEG_TTN2;
                break;
            default:
                throw new UserException("Could not find compression type "+type);
        }
        return compressionType;
    }
}
