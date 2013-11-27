/*cd \
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

import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.JFrame;
import javax.swing.JSplitPane;
import net.sourceforge.bookscanwizard.gui.ImportImages;
import net.sourceforge.bookscanwizard.gui.MainFrame;
import net.sourceforge.bookscanwizard.gui.TipsDialog;
import net.sourceforge.bookscanwizard.op.EstimateDPI;
import net.sourceforge.bookscanwizard.qr.ImportImagesData;
import net.sourceforge.bookscanwizard.start.AbstractPage;

/**
 * Handles saving and restoring some gui preferences.
 */
public class PrefsHelper {

    private static final Logger logger = Logger.getLogger(PrefsHelper.class.getName());
    private static final Preferences prefs =
            Preferences.userRoot().node(PrefsHelper.class.getPackage().getName());
    private static HashMap<String,Object> miscPrefs = new HashMap<>();

    public static void loadPreferences() {
        Thread savePrefs = new Thread(new Runnable() {

            @Override
            public void run() {
                savePreferences();
            }
        }, "Save prefs");
        try {
            Runtime.getRuntime().addShutdownHook(savePrefs);
            BSW bsw = BSW.instance();
            MainFrame mainFrame = bsw.getMainFrame();
            boolean opListVisible = prefs.getBoolean("operationListVisible", true);
            mainFrame.getOperationList().setVisible(opListVisible);
            float scale = prefs.getFloat("scale", 1);
            mainFrame.getViewerPanel().setPostScale(scale);
            Rectangle bounds = (Rectangle) getObject("mainBounds");
            if (bounds != null) {
                mainFrame.setBounds(bounds);
            }
            int mainState = prefs.getInt("mainState", JFrame.NORMAL);
            mainFrame.setExtendedState(mainState);
            bounds = (Rectangle) getObject("helperBounds");
            if (bounds != null) {
                bsw.getMainFrame().getOperationList().setBounds(bounds);
            }
            int helperState = prefs.getInt("helperState", JFrame.NORMAL);
            bsw.getMainFrame().getOperationList().setExtendedState(helperState);
            boolean debug = prefs.getBoolean("debug", false);
            if (debug) {
                mainFrame.getShowDebuggingInfo().doClick();
            }
            int dividerLocation = prefs.getInt("dividerLocation", 0);
            if (dividerLocation > 0) {
                mainFrame.getSplitPane().setDividerLocation(dividerLocation);
            }
            int orientation = prefs.getInt("orientation", JSplitPane.HORIZONTAL_SPLIT);
            if (orientation == JSplitPane.HORIZONTAL_SPLIT) {
                mainFrame.getHorizontalLayout().doClick();
            }
            Map<String,Serializable> wizardPrefs = (Map<String, Serializable>) getObject("wizard");
            AbstractPage.putDefaults(wizardPrefs);
            EstimateDPI.setInfo((float[]) getObject("dpiInfo"));
            Object tmp = getObject("miscPrefs");
            if (tmp != null) {
                miscPrefs = (HashMap<String, Object>) getObject("miscPrefs");
            }
            tmp  = getObject("import");
            if (tmp != null) {
                ImportImages.getInstance().setImportData((ImportImagesData) tmp);
            }
            TipsDialog.setTipNumber(prefs.getInt("tipNumber", 0));
            mainFrame.getTipsDialog().getShowOnStartup().setSelected(prefs.getBoolean("showTips", true));
            
        } catch (Exception ex) {
            logger.log(Level.WARNING, null, ex);
        }
    }

    private static void savePreferences() {
        BSW bsw = BSW.instance();
        MainFrame mainFrame = bsw.getMainFrame();
        prefs.putBoolean("operationListVisible", mainFrame.getOperationList().isVisible());
        prefs.putFloat("scale", mainFrame.getViewerPanel().getPostScale());
        putObject("mainBounds", mainFrame.getBoundsHelper().getUnmaximizedBounds());
        prefs.putInt("mainState", mainFrame.getBoundsHelper().getLastState());
        putObject("helperBounds", mainFrame.getOperationList().getBoundsHelper().getUnmaximizedBounds());
        prefs.putInt("helperState", mainFrame.getBoundsHelper().getLastState());
        prefs.putBoolean("debug", mainFrame.getShowDebuggingInfo().isSelected());
        prefs.putInt("dividerLocation", mainFrame.getSplitPane().getDividerLocation());
        prefs.putInt("orientation", mainFrame.getSplitPane().getOrientation());
        putObject("dpiInfo", EstimateDPI.getInfo());
        putObject("wizard", (Serializable) AbstractPage.getDefaults());
        putObject("miscPrefs", miscPrefs);
        putObject("import", ImportImages.getInstance().getImportData());
        prefs.putInt("tipNumber", TipsDialog.getTipNumber()+1);
        prefs.putBoolean("showTips", mainFrame.getTipsDialog().getShowOnStartup().isSelected());
    }
    
    synchronized public static Object getPref(String key) {
        return miscPrefs.get(key);
    }
    
    synchronized public static void setPref(String key, Object value) {
        miscPrefs.put(key, value);
    }
    
    synchronized public static HashMap<String,Object> getPrefs() {
        return miscPrefs;
    }

    synchronized public static String getPrefString(String key) {
        String value = (String) miscPrefs.get(key);
        if (value == null) {
            value = "";
        }
        return value;
    }
    

    /*            Logger parentLogger = Logger.getLogger(BSW.class.getPackage().getName());
    if (System.)
    parentLogger.setLevel(Level.FINEST);
     */
    private static void putObject(String key, Serializable obj) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            oos.close();
            byte[] array = bos.toByteArray();
            prefs.putByteArray(key, array);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Object getObject(String key) {
        try {
            byte[] buf = prefs.getByteArray(key, null);
            if (buf == null) {
                return null;
            } else {
                ByteArrayInputStream bis = new ByteArrayInputStream(buf);
                ObjectInputStream ois = new ObjectInputStream(bis);
                ois.close();
                return ois.readObject();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
