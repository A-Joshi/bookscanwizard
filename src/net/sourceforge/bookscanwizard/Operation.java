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

import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ImageWriterSpi;
import javax.media.jai.BorderExtenderConstant;
import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import net.sourceforge.bookscanwizard.OpDefinition.Argument;
import net.sourceforge.bookscanwizard.gui.MainFrame;
import net.sourceforge.bookscanwizard.gui.OperationList;
import net.sourceforge.bookscanwizard.op.Rotate;
import net.sourceforge.bookscanwizard.util.ImageUtilities;

/**
 * The master Operation type
 */
abstract public class Operation {
    public static final int GRAY_STANDARD = 100;

    /**
     * The operation should be followed by an =, but a : or a space are also valid.
     */
    public static final Pattern MATCH_OP = Pattern.compile("([^: =]*)([: =]*)(.*)|.*");

    /**
     * Arguments can be separated by commas or spaces, and quoted with double quotes
     */
    public static final Pattern ARG_PATTERN = Pattern.compile("[^\\s\",]+|\"[^\"]*\"");

    private static List<Operation> allOperations;

    private static int minPass;
    private static int maxPass;
    private static volatile int currentPass =getMinPass();
    protected static List<Operation> currentPreviewOps;

    protected String arguments;
    private static final Properties properties = new Properties();
    private OpDefinition definition;
    protected PageSet pageSet;
    private Map<String,String> options;

    public static List<Operation> getOperations(String config) throws  Exception {
        BSW.instance().fireNewConfigListeners();
        PageSet pageSet = new PageSet();
        ArrayList<Operation> operations = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new StringReader(config))) {
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
                    operations.addAll(ops);
                }
            }
        }
        minPass = Integer.MAX_VALUE;
        maxPass = Integer.MIN_VALUE;
        for (Operation op : operations) {
            minPass = Math.min(minPass, op.getOperationMinPass());
            maxPass = Math.max(maxPass, op.getOperationMaxPass());
        }


        setAllOperations(operations);
        return operations;
    }
    private String[] textArgs;

    /**
     * Called when the command is first configured.
     */
    protected List<Operation> setup(List<Operation> operationList) throws Exception {
        return operationList;
    }

    /**
     * Previews an operation.  The default is do the same operation for perform operation.
     */
    protected RenderedImage previewOperation(FileHolder holder, RenderedImage img) throws Exception {
        return performOperation(holder, img);
    }

    /**
     * Performs the operation.
     */
    protected RenderedImage performOperation(FileHolder holder, RenderedImage img) throws Exception {
        return img;
    }
    
    private static void verifySaveOperationExists(List<Operation> ops) throws Exception {
        boolean found = false;
        for (Operation op : ops) {
            if (op instanceof SaveOperation) {
                found = true;
                break;
            }
        }
        if (!found) {
            Operation lastOp = ops.get(ops.size()-1);
            List<Operation> op = Operation.getOperation("Pages = all", null, lastOp.getPageSet());
            ops.addAll(op);
            ops = Operation.getOperation("SaveImages = ", null, lastOp.getPageSet());
            ops.addAll(op);
        }
    }

    /**
     *  An operation that is performed after all files have been processed.
     */
    public void postOperation() throws Exception {
    }

    public static void setAllOperations(List<Operation> allOperations) {
        Operation.allOperations = allOperations;
    }
    final public String getName() {
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
            String value;
            String key = arguments.substring(pos+1, end);
            if (key.isEmpty()) {
                value = "$";
            } else {
                value = BSW.getProperty(key);
            }
            arguments = arguments.substring(0, start) + value + arguments.substring(end+1);
        }
    }
    
    /**
     * Validates that a command contains valid arguments.  The default is to 
     * validate that the required parameters are filled in.
     */
    protected void validateArguments() {
        OpDefinition def = OperationList.findDefinition(getClass().getSimpleName());
        if (def == null) {
            return;
        }
        int required = 0;
        for (Argument arg : def.getArguments()) {
            if (arg.isRequired()) {
                required++;
            } else {
                break;
            }
        }
        if (required > getTextArgs().length) {
            throw new UserException(getClass().getSimpleName()+" requires "+required+" parameters, but only "+getTextArgs().length+" were included");
        }
    }

    protected synchronized String getOption(String key) {
        if (options == null) {
            options = new HashMap<>();
            ArrayList<String> newList = new ArrayList<>();
            for (String a : getTextArgs()) {
                if (a.contains("=")) {
                    String[] keyValue = a.split("=");
                    options.put(keyValue[0], keyValue[1]);
                } else {
                    newList.add(a);
                }
            }
            textArgs = newList.toArray(new String[newList.size()]);
        }
        return options.get(key);
    }
    
    final protected String[] getTextArgs() {
        if (textArgs == null) {
            Matcher matcher = ARG_PATTERN.matcher(arguments);
            textArgs = getArgs(matcher);
        }
        return textArgs;
    }

    final protected double[] getArgs() {
        String[] txtArgs = getTextArgs();
        double[] retVal = new double[txtArgs.length];
        for (int i=0; i < txtArgs.length; i++) {
            try {
                retVal[i] = Double.parseDouble(txtArgs[i]);
            } catch (NumberFormatException e) {
                // ignore
            }
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

    /**
     * Converts a line to an operation, for the purpose of editing the 
     * configuration
     */
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
            try {
                operation.init(args, null);
                operation.pageSet = new PageSet(null);
                operation.setup(Collections.singletonList(operation));
            } catch (Exception e) {
                // ignore.
            }
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
        } catch (ClassNotFoundException | NoClassDefFoundError ex) {
            throw new UserException("Could not find operation "+name);
        }
        Operation operation;
        List<Operation> retVal = null;
        if (cls != null) {
            operation = (Operation) cls.newInstance();
            operation.init(args, reader);
            operation.pageSet = pageSet;
            operation.validateArguments();
            retVal = operation.setup(Collections.singletonList(operation));
        }
        return retVal;
    }

    public static RenderedImage performOperations(FileHolder holder, List<Operation> ops) throws Exception {
        verifySaveOperationExists(ops);
        RenderedImage img = null;
        for (Operation op :ops) {
            op.preprocess(holder, img, false);
        }
        for (Operation op : ops) {
            if ((!holder.isDeleted() || op instanceof ProcessDeleted) && op.getPageSet().getFileHolders().contains(holder)) {
                if (op.matchesPass()) {
                    if (img == null) {
                        img = holder.getImage();
                    }
                    img = op.performOperation(holder, img);
                } else {
                    System.out.println("no match "+holder.getName());
                }
            }
        }
        return img;
    }

    protected boolean matchesPass() {
        return true;
        //multipass isn't quite ready for prime time.
//        return currentPass >= getOperationMinPass() && currentPass <= getOperationMaxPass();
    }

    public static List<Operation> getAllOperations() {
        return allOperations;
    }

    /**
     * Preview operations.  If the page is deleted, it will not perform
     * perspective or color operations, and only cropping if it is done
     * before a perspective operation.
     * 
     * @param ops The operations to preview
     * @param holder The holder of the current page
     * @param img The image to be processed.
     * @param cursorAfterConfig if the last operation should be previewed instead of rendered.
     * 
     * @return the image with the operations performed.
     */
    public static RenderedImage previewOperations(List<Operation> ops, FileHolder holder, RenderedImage img, boolean cursorAfterConfig) throws Exception {
        if (holder == null) {
            return null;
        }
        currentPreviewOps = ops;
        MainFrame main = BSW.instance().getMainFrame();
        main.getViewerPanel().setPreviewCrop(null);
        if (ops.isEmpty()) {
            throw new UserException("There are no operations defined. Check to make sure the text cursor appears after the configuration to test.");
        }
        Operation lastOperation = null;
        if (!ops.isEmpty() && !cursorAfterConfig) {
            lastOperation = ops.get(ops.size()-1);
        }
        for (Operation op :ops) {
            op.preprocess(holder, img, true);
        }
        holder.setDeleted(false); // if it is removed, the RemovePages command will switch it back.
        for (Operation op : ops) {
            boolean preview = true;
            if ((!holder.isDeleted() || op instanceof ProcessDeleted) && op.getPageSet().getFileHolders().contains(holder)) {
                if (op instanceof ColorOp) {
                    if (!main.isShowColors()) {
                        continue;
                    }
                } else if (op instanceof CropOp) {
                    preview = op == lastOperation || !main.isShowCrops();
                } else if (op instanceof PerspectiveOp) {
                    preview = op == lastOperation || !main.isShowPerspective();
                } else if (op instanceof ScaleOp) {
                    if (!main.isShowScale()) {
                        continue;
                    }
                }
                if (preview) {
                    img = op.previewOperation(holder, img);
                } else {
                    img = op.performOperation(holder, img);
                }
            }
        }
        validate();
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
        ArrayList<String> list = new ArrayList<>();
        while (matcher.find()) {
            String value = matcher.group();
            if (value.startsWith("#")) {  //xyzzy
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

    protected int getOperationMinPass() {
        return 5;
    }

    protected int getOperationMaxPass() {
        return 5;
    }

    public static int getCurrentPass() {
        return currentPass;
    }

    public static void setCurrentPass(int currentPass) {
        Operation.currentPass = currentPass;
    }

    public static int getMinPass() {
        return minPass;
    }
    public static int getMaxPass() {
        return maxPass;
    }

    private static void validate() {
        if (BSW.instance().isInPreview()) {
            BSW.instance().getMainFrame().getThumbTable().setTranspose(Rotate.getLeftTranspose(), Rotate.getRightTranspose());
        }
    }
    
    static {
        // the native version has problems with compression.
        ImageUtilities.allowNativeCodec("jpeg2000", ImageWriterSpi.class, false);
        // the native version doesn't want to return metadata.
        ImageUtilities.allowNativeCodec("jpeg", ImageWriterSpi.class, false);
        ImageUtilities.allowNativeCodec("jpeg", ImageReaderSpi.class, false);
        ImageUtilities.allowNativeCodec("jpeg", ImageWriterSpi.class, false);
    }

    /**
     * This is called before a preview or process operation.
     * 
     * @param holder
     * @param img 
     */
    protected void preprocess(FileHolder holder, RenderedImage img, boolean preview) throws Exception {
    }
    
      /**
     * Expands an image if the crop points are outside of the original image.
     */
    protected RenderedImage expandImageIfNecessary(RenderedImage img, Point2D[] pts) {
        int rightX = img.getMinX() + img.getWidth();
        int bottomY = img.getMinY() + img.getHeight();

        int left = (int) Math.round(Math.max(0, img.getMinX() - pts[0].getX()));
        int top = (int)  Math.round(Math.max(0, img.getMinY() - pts[0].getY()));
        int right = (int) Math.round(Math.max(0, pts[1].getX() - rightX));
        int bottom = (int) Math.round(Math.max(0, pts[1].getY() - bottomY));
        
        if (left + top + right + bottom > 0) {
            ParameterBlock params = new ParameterBlock();
            params.addSource(img);
            params.add(left).add(right).add(top).add(bottom);
            // calculate the maximum value, and set the color to it
            int white = (1 << img.getColorModel().getComponentSize()[0]) - 1;
            params.add(new BorderExtenderConstant(new double[] { white}));
            img = JAI.create("border", params);
        }
        return img;
    }
}
