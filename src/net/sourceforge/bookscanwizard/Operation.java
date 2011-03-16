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

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.spi.ImageWriterSpi;
import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import net.sourceforge.bookscanwizard.op.SaveImage;
import net.sourceforge.bookscanwizard.util.ImageUtilities;

/**
 * The master Operation type
 */
abstract public class Operation {
    public static int GRAY_STANDARD = 100;

    /**
     * The operation should be followed by an =, but a : or a space are also valid.
     */
    public static final Pattern MATCH_OP = Pattern.compile("([^: =]*)([: =]*)(.*)|.*");

    /**
     * Arguments can be separated by commas or spaces, and quoted with double quotes
     */
    public static final Pattern ARG_PATTERN = Pattern.compile("[^\\s\",]+|\"[^\"]*\"");

    private static List<Operation> allOperations;

    protected String arguments;
    private static final Properties properties = new Properties();
    private OpDefinition definition;
    protected PageSet pageSet;

    static List<Operation> getOperations(String config) throws  Exception {
        BSW.instance().fireNewConfigListeners();
        PageSet pageSet = new PageSet();
        ArrayList<Operation> operations = new ArrayList<Operation>();
        BufferedReader reader = new BufferedReader(new StringReader(config));
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            List<Operation> ops = Operation.getOperation(line, reader, pageSet);
            if (ops != null) {
                for (Operation o : ops) {
                    pageSet = o.getPageSet();
                }

                if (ops != null) {
                    operations.addAll(ops);
                }
            }
        }
        reader.close();
        boolean found = false;
        for (Operation op : operations) {
            if (op instanceof SaveImage) {
                found = true;
                break;
            }
        }
        if (!found) {
            List<Operation> ops = Operation.getOperation("SaveImage = ", null, pageSet);
            operations.addAll(ops);
        }
        setAllOperations(operations);
        return operations;
    }

    /**
     * Called when the command is first configured.
     */
    protected List<Operation> setup(List<Operation> operationList) throws Exception {
        return operationList;
    }

    /**
     * Previews an operation.  The default is to do nothing.
     */
    protected RenderedImage previewOperation(FileHolder holder, RenderedImage img) throws Exception {
        return img;
    }

    /**
     * Performs the operation.
     */
    protected RenderedImage performOperation(FileHolder holder, RenderedImage img) throws Exception {
        return img;
    }

    /**
     *  An operation that is performed after all files have been processed.
     */
    public void postOperation() throws Exception {
    }

    public static void setAllOperations(List<Operation> allOperations) {
        Operation.allOperations = allOperations;
    }
    final protected String getName() {
        return getClass().getSimpleName();
    }
    
    final public OpDefinition getDefinition() {
        if (definition == null) {
            definition = new OpDefinition(
                    getName(),
                    properties.getProperty(getName()+".help"),
                    properties.getProperty(getName()+".example")
            );
            int index = 0;
            while (true) {
                index++;
                String key = getName()+".p"+index;
                String type = properties.getProperty(key+".type");
                if (type == null) {
                    break;
                }
                String name = properties.getProperty(key+".name");
                String tooltip = properties.getProperty(key+".text");
                definition.add(type, name, tooltip);
            }
        }
        return definition;
    }

    protected void init(String args, BufferedReader reader) {
        this.arguments = args.trim();
        int start = 0;
        while (true) {
            int pos = arguments.indexOf("$");
            if (pos < 0) {
                break;
            }
            int end = arguments.indexOf("$", pos+1);
            if (end < 0) {
                break;
            }
            String value = null;
            String key = arguments.substring(pos+1, end);
            if (key.isEmpty()) {
                value = "$";
            } else {
                value = BSW.getProperty(key);
            }
            if (key != null) {
                arguments = arguments.substring(0, start) + value + arguments.substring(end+1);
            } else {
                start = end;
            }
        }
    }

    final protected String[] getTextArgs() {
        Matcher matcher = ARG_PATTERN.matcher(arguments);
        return getArgs(matcher);
    }

    final protected double[] getArgs() {
        String[] textArgs = getTextArgs();
        double[] retVal = new double[textArgs.length];
        for (int i=0; i < textArgs.length; i++) {
            retVal[i] = Double.parseDouble(textArgs[i]);
        }
        return retVal;
    }

    final protected double[] getScaledArgs() {
        double scale = BSW.getPreviewScale();
        double[] args = getArgs();
        double[] scaledArgs = new double[args.length];
        for (int i=0; i < args.length; i++) {
            scaledArgs[i] = args[i] * scale;
        }
        return scaledArgs;
    }

    public PageSet getPageSet() {
        return pageSet;
    }

    protected FileHolder getHolder(String name) {
        for (FileHolder holder : PageSet.getSourceFiles()) {
            if (holder.getName().equals(name)) {
                return holder;
            }
        }
        throw new UserException("Could not find file "+name);
    }

    public static Operation getStandaloneOp(String line) throws Exception {
        Matcher matcher = MATCH_OP.matcher(line);
        if (!matcher.find()) {
            return null;
        }
        String name = matcher.group(1);
        String args = matcher.group(3);
        if (name.isEmpty() || name.startsWith("#")) {
            return null;
        }

        String className = name;
        if (!className.contains(".")) {
            className = Operation.class.getPackage().getName()+".op."+name;
        }
        Class cls;
        try {
            cls = Class.forName(className);
        } catch (ClassNotFoundException ex) {
            throw new UserException("Could not find operation "+name);
        }
        if (cls != null) {
            Operation operation = (Operation) cls.newInstance();
            operation.init(args, null);
            operation.pageSet = new PageSet(null);
            operation.setup(Collections.singletonList(operation));
            return operation;
        }
        return null;
    }

    public static List<Operation> getOperation(String line, BufferedReader reader, PageSet pageSet)
            throws Exception {
        if (pageSet == null) {
            throw new NullPointerException();
        }
        Matcher matcher = MATCH_OP.matcher(line);
        if (!matcher.find()) {
            return null;
        }
        String name = matcher.group(1);
        String args = matcher.group(3);
        if (name.isEmpty() || name.startsWith("#")) {
            return null;
        }

        String className = name;
        if (!className.contains(".")) {
            className = Operation.class.getPackage().getName()+".op."+name;
        }
        Class cls;
        try {
            cls = Class.forName(className);
        } catch (ClassNotFoundException ex) {
            throw new UserException("Could not find operation "+name);
        } catch (NoClassDefFoundError ex) {
            throw new UserException("Could not find operation "+name);
        }
        Operation operation = null;
        List<Operation> retVal = null;
        if (cls != null) {
            operation = (Operation) cls.newInstance();
            operation.init(args, reader);
            PageSet newPageSet = new PageSet(pageSet);
            operation.pageSet = newPageSet;
            retVal = operation.setup(Collections.singletonList(operation));
        }
        return retVal;
    }

    public static RenderedImage performOperations(FileHolder holder, RenderedImage img) throws Exception {
        for (Operation op : allOperations) {
            if (op.getPageSet().getFileHolders().contains(holder)) {
                img = op.performOperation(holder, img);
            }
        }
        return img;
    }

    public static List<Operation> getAllOperations() {
        return allOperations;
    }

    public static RenderedImage previewOperations(FileHolder holder, RenderedImage img) throws Exception {
        if (holder == null) {
            return null;
        }
        MainFrame main = BSW.instance().getMainFrame();
        main.getViewerPanel().setPreviewCrop(null);
        if (allOperations == null) {
            throw new UserException("There are no operations defined. Check to make sure the text cursor appears after the configuration to test.");
        }
        for (Operation op : allOperations) {
            boolean preview = false;
            if (op.getPageSet().getFileHolders().contains(holder)) {
                if (op instanceof ColorOp) {
                    preview = !main.isShowColors();
                } else if (op instanceof CropOp) {
                    preview = !main.isShowCrops();
                } else if (op instanceof PerspectiveOp) {
                    preview = !main.isShowPerspective();
                }
                if (preview) {
                    img = op.previewOperation(holder, img);
                } else {
                    img = op.performOperation(holder, img);
                }
            }
        }
        return img;
    }

    static {
        try {
            InputStream is = Operation.class.getClassLoader()
                    .getResourceAsStream("net/sourceforge/bookscanwizard/bookscanwizard.properties");
            properties.load(is);
        } catch (Exception e) {
//            e.printStackTrace();
        }
    }

    public static String[] getArgs(Matcher matcher) {
        ArrayList<String> list = new ArrayList<String>();
        while (matcher.find()) {
            String value = matcher.group();
            if (value.startsWith("#")) {
                // This is the start of a comment
                break;
            }
            if (value.startsWith("\"")) {
                value = value.substring(1, value.length()-1);
            }
            list.add(value);
        }
        String[] retVal = new String[list.size()];
        return list.toArray(retVal);
    }

    /**
     * Use the full image as the tile size.  This is helpful in working around
     * various JAI bugs.
     */
    protected static RenderingHints useFullTile(RenderedImage img, RenderingHints hints) {
        if (hints == null) {
            hints = new RenderingHints(null);
        }
        ImageLayout imageLayout = new ImageLayout();
        imageLayout.setTileWidth(img.getWidth());
        imageLayout.setTileHeight(img.getHeight());
        hints.put(JAI.KEY_IMAGE_LAYOUT, imageLayout);
        return hints;
    }

    static {
        // the native version has problems with compression.
        ImageUtilities.allowNativeCodec("jpeg2000", ImageWriterSpi.class, false);
        // the native version doesn't want to return metadata.
        ImageUtilities.allowNativeCodec("jpeg", ImageWriterSpi.class, false);
    }
}
