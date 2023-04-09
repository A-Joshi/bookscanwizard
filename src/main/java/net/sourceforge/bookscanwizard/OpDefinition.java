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

import java.util.ArrayList;

public class OpDefinition {
    private final String name;
    private final String helper;
    private final String example;
    private final ArrayList<Argument> arguments = new ArrayList<>();

    public ArrayList<Argument> getArguments() {
        return arguments;
    }

    public String getHelper() {
        return helper;
    }

    public String getName() {
        return name;
    }

    public String getShortDescription() {
        String shortDescription = helper;
        if (helper == null) {
            System.out.println("missing desc: "+name);
            return "";
        }
        int pos = shortDescription.indexOf('.');
        if (pos > 0) {
            shortDescription = shortDescription.substring(0, pos + 1);
        }
        return shortDescription;
    }

    public String getExample() {
        return getName()+" = "+example+"\n\n";
    }

    public OpDefinition(String name, String helper, String example) {
        this.name = name;
        this.helper = helper;
        this.example = example == null ? "" : example;
    }

    void add(String type, String name, String tooltip) {
        boolean required = type.equalsIgnoreCase("required");
        arguments.add(new Argument(required, name, tooltip));
    }

    public class Argument {
        private final boolean required;
        private final String name;
        private final String tooltip;

        public Argument(boolean required, String name, String tooltip) {
            this.required = required;
            this.name = name;
            this.tooltip = tooltip;
        }

        public String getName() {
            return name;
        }
        
        public boolean isRequired() {
            return required;
        }

        public String getTooltip() {
            return tooltip;
        }
    }
}
