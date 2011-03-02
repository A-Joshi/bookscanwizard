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

package net.sourceforge.bookscanwizard.op;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sourceforge.bookscanwizard.BSW;
import net.sourceforge.bookscanwizard.NewConfigListener;
import net.sourceforge.bookscanwizard.Operation;
import net.sourceforge.bookscanwizard.UserException;

/**
 *
 * @author Steve
 */
public class Metadata extends Operation{
    private static HashMap<String,String> metaData = new HashMap<String,String>();

    static {
        BSW.instance().addNewConfigListener(new NewConfigListener(){
            public void newConfig() {
                metaData.clear();
            }
        });
    }

    @Override
    protected List<Operation> setup(List<Operation> operationList) throws Exception {
        int pos = arguments.indexOf(":");
        if (pos < 0) {
            throw new UserException("Metadata missing : separator");
        }
        metaData.put(arguments.substring(0, pos).trim(), arguments.substring(pos+1).trim());
        return operationList;
    }

    public static Map<String,String> getMetaData() {
        return metaData;
    }
}
