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

import java.awt.image.RenderedImage;
import java.util.HashMap;
import net.sourceforge.bookscanwizard.FileHolder;
import net.sourceforge.bookscanwizard.Operation;

/**
 * This saves an image for later use in the script
 */
public class SaveTemp extends Operation  {
    static ThreadLocal<HashMap<String,RenderedImage>> save = new ThreadLocal() {
        @Override
        protected HashMap<String,RenderedImage> initialValue() {
            return new HashMap<>();
        }
    };
    
    @Override
    public RenderedImage performOperation(FileHolder holder, RenderedImage img) throws Exception {
        String name = getTextArgs()[0];
        save.get().put(name, img);
        return img;
    }
    
    @Override
    public void postOperation() {
        save.get().clear();
    }
    
    public static RenderedImage load(String name) {
        return save.get().get(name);
    }
}
