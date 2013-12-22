/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.sourceforge.bookscanwizard.gui;

import java.beans.*;
import java.io.Serializable;

/**
 *
 * @author Steve
 */
public class UserPreferenceBean implements Serializable {
    private static final long serialVersionUID = 1L;

    public static void setPrefsBean(UserPreferenceBean userPreferenceBean) {
        instance = userPreferenceBean;
    }
    
    private String tesseractLocation;
    private static UserPreferenceBean instance = new UserPreferenceBean();

    public static final String PROP_TESSERACTLOCATION = "tesseractLocation";
    
    public String getTesseractLocation() {
        return tesseractLocation;
    }

    public void setTesseractLocation(String tesseractLocation) {
        String oldTesseractLocation = this.tesseractLocation;
        this.tesseractLocation = tesseractLocation;
        propertySupport.firePropertyChange(PROP_TESSERACTLOCATION, oldTesseractLocation, tesseractLocation);
    }

        private String scanTailorLocation;

    public static final String PROP_SCANTAILORLOCATION = "scanTailorLocation";

    public String getScanTailorLocation() {
        return scanTailorLocation;
    }

    public void setScanTailorLocation(String scanTailorLocation) {
        String oldScanTailorLocation = this.scanTailorLocation;
        this.scanTailorLocation = scanTailorLocation;
        propertySupport.firePropertyChange(PROP_SCANTAILORLOCATION, oldScanTailorLocation, scanTailorLocation);
    }

    private PropertyChangeSupport propertySupport;
    
    private UserPreferenceBean() {
        propertySupport = new PropertyChangeSupport(this);
    }
    
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.addPropertyChangeListener(listener);
    }
    
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.removePropertyChangeListener(listener);
    }

    public static UserPreferenceBean instance() {
        return instance;
    }
}
