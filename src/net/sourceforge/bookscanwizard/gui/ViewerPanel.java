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

package net.sourceforge.bookscanwizard.gui;

import com.sun.media.jai.widget.DisplayJAI;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DragSource;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import javax.media.jai.PerspectiveTransform;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.TransferHandler;
import net.sourceforge.bookscanwizard.BSW;
import net.sourceforge.bookscanwizard.Operation;
import net.sourceforge.bookscanwizard.util.Fraction;
import net.sourceforge.bookscanwizard.util.Interpolate;

/**
 * A panel to display the current image, as well as code to select the
 * coordinates
 */
public class ViewerPanel extends DisplayJAI implements KeyListener, ClipboardOwner {

    private List<Point2D> scaledPoints = new ArrayList<>();
    private List<Point2D> registrationPoints = new ArrayList<>();
    private Path2D.Float plus;
    private Point lastPoint;
    private Point lastPressPoint;
    private PerspectiveTransform previewTransform;
    private Point2D[] previewCrop;
    private double zoom;
    private double postScale = 1D;
    private static int MAX_DISTANCE_TO_POINT = 12;
    private double xCropScale = 1;
    private double yCropScale = 1;
    private Interpolate sliderInterpolate = new Interpolate(-1000, Math.log(.05), 1000, Math.log(2));
    private Point lastViewportPoint;
    private boolean suppressClick;

    private boolean isInDrag;
    private static final double INCHES_TO_MM = 25.4;
    

    public ViewerPanel(final ActionListener menuHandler) {
        setFocusable(true);
        setEnabled(true);
        plus = new Path2D.Float();
        plus.moveTo(-5, 0);
        plus.lineTo(5, 0);
        plus.moveTo(0, -5);
        plus.lineTo(0, 5);
        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        MouseAdapter mouseListener = new MouseAdapter() {

            @Override
            public void mouseReleased(MouseEvent e) {
                // not sure what is going on here.. We should be getting clicks
                // not drags if the movement is below the threshold.  But
                // at least for me it isn't working.  So we use the mouseRelease event.
                if (suppressClick) {
                    suppressClick = false;
                } else {
                    if (e.getSource().equals(ViewerPanel.this) && lastPressPoint != null &&
                       lastPressPoint.distance(e.getPoint()) < DragSource.getDragThreshold()) {
                        myMouseClicked(e);
                    }
                }
                lastPressPoint = null;
                isInDrag = false;
            }

            public void myMouseClicked(MouseEvent e) {
                requestFocusInWindow();

                if (e.getButton() != MouseEvent.BUTTON1) {
                    return;
                }
                Point pt = new Point(e.getX(), e.getY());
                if (e.isControlDown()) {
                    registrationPoints.add(getScaledPoint(pt));
                } else {
                    if (scaledPoints.size() < 4) {
                        scaledPoints.add(getScaledPoint(pt));
                    } else {
                        Point2D scaledPoint = getScaledPoint(pt);
                        Point2D nearest = getNearest(scaledPoint);
                        nearest.setLocation(scaledPoint);
                    }
                }
                e.consume();
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                lastPoint = e.getPoint();
                lastPressPoint = e.getPoint();
                lastViewportPoint = e.getPoint();
            }
            
            @Override
            public void mouseDragged(MouseEvent e) {
                if ((e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) == 0) {
                    return;
                }
                if (!isInDrag && (lastPressPoint == null || lastPressPoint.distance(e.getPoint()) < DragSource.getDragThreshold())) {
                    return;
                }
                boolean dragImage = false;
                isInDrag = true;
                boolean suppressDragImage = false;
                if (scaledPoints.size() > 1) {
                    boolean pan = false;
                    Point sPoint = new Point(e.getX(), e.getY());
                    Point2D scaledPoint = getScaledPoint(sPoint);
                    Point2D nearest = getNearest(scaledPoint);
                    Point nearestPt = getImagePoint(nearest);
                    if (e.isControlDown() && scaledPoints.size() == 2) {
                        Point2D firstPoint = scaledPoints.get(0);
                        if (!nearest.equals(firstPoint)) {
                            double xNewDist = scaledPoint.getX() - firstPoint.getX();
                            double yNewDist = scaledPoint.getY() - firstPoint.getY();
                            double xCurDist = nearest.getX() - firstPoint.getX();
                            double yCurDist = nearest.getY() - firstPoint.getY();
                            if (xCurDist != 0 && yCurDist != 0) {
                                double xMult = (xNewDist / xCurDist);
                                double yMult = (yNewDist / yCurDist);
                                xCropScale *= xMult;
                                yCropScale *= yMult;
                                nearest.setLocation(firstPoint.getX() + xCurDist * xMult, firstPoint.getY() + yCurDist * yMult);
                                for (Point2D regPoint : registrationPoints) {
                                    xCurDist = regPoint.getX() - firstPoint.getX();
                                    yCurDist = regPoint.getY() - firstPoint.getY();
                                    regPoint.setLocation(
                                        firstPoint.getX() + xCurDist * xMult, firstPoint.getY() + yCurDist * yMult
                                    );
                                }
                            }
                        }
                    } else {
                        if (nearestPt.distance(sPoint) <= MAX_DISTANCE_TO_POINT) {
                            nearest.setLocation(scaledPoint);
                            suppressDragImage = true;
                        } else {
                            pan = isPointInside(scaledPoint);
                        }
                    }
                    if (pan) {
                        int xOffset = e.getX() - lastPoint.x;
                        int yOffset = e.getY() - lastPoint.y;
                        for (Point2D sp : scaledPoints) {
                            Point scrPt = getImagePoint(sp);
                            scrPt.x += xOffset;
                            scrPt.y += yOffset;
                            sp.setLocation(getScaledPoint(scrPt));
                        }
                        for (Point2D registrationPoint : registrationPoints) {
                            Point scrPt = getImagePoint(registrationPoint);
                            scrPt.x += xOffset;
                            scrPt.y += yOffset;
                            registrationPoint.setLocation(getScaledPoint(scrPt));
                        }
                        lastPoint = e.getPoint();
                    } else {
                        dragImage = !suppressDragImage;
                    }
                    repaint();
                } else {
                    dragImage = true;
                }
                if (dragImage) {
                    suppressClick = true;
                    JViewport viewport = (JViewport) getParent();
                    Point pt = viewport.getViewPosition();
                    int x = lastViewportPoint.x - e.getPoint().x;
                    int y = lastViewportPoint.y - e.getPoint().y;
                    pt.x +=x;
                    pt.y +=y;
                    viewport.setViewPosition(pt);
                }
                e.consume();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                setMouseCursor(e);
            }
        };
        addMouseListener(mouseListener);
        addMouseMotionListener(mouseListener);
        addMouseWheelListener(new MouseWheelListener(){
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                float zoom = (float) (-e.getWheelRotation() /8D + 1);
                multScale(zoom);
                e.consume();
            }
        });
        addKeyListener(this);
        createPopupMenu(menuHandler);

        setTransferHandler(new TransferHandler("pointDef") {
            @Override
            protected Transferable createTransferable(JComponent c) {
                return new StringSelection(getPointDef());
            }

            @Override
            public int getSourceActions(JComponent c) {
                return COPY_OR_MOVE;
            }

            @Override
            public void exportToClipboard(JComponent comp, Clipboard clip, int action)
                throws IllegalStateException
            {
                super.exportToClipboard(comp, clip, action);
                if (action == MOVE) {
                    setPointDef("");
                }
            }

            @Override
            public boolean importData(TransferHandler.TransferSupport support) {
                return paste(support.getTransferable());
            }
        });

        // Now bind the Ctrl-C keystroke to a "Copy" command.
        InputMap im = new InputMap();
        im.setParent(getInputMap(WHEN_FOCUSED));
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "cut");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "copy");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "paste");
        setInputMap(WHEN_FOCUSED, im);

        ActionMap am = new ActionMap();
        am.setParent(getActionMap());
        am.put("cut", TransferHandler.getCutAction());
        am.put("copy", TransferHandler.getCopyAction());
        am.put("paste", TransferHandler.getPasteAction());
        setActionMap(am);
    }

    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
        // do nothing.
    }

    public double getZoom() {
        return zoom;
    }

    public float getXCropScale() {
        return (float) xCropScale;
    }
    
    public float getYCropScale() {
        return (float) yCropScale;
    }
    
    public void setPreviewTransform(PerspectiveTransform tr) {
        try {
            this.previewTransform = tr;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public PerspectiveTransform getPreviewTransform() {
        return previewTransform;
    }

    public void setPreviewCrop(Point2D[] corners) {
        previewCrop = corners;
    }

    public Point2D[] getPreviewCrop() {
        return previewCrop;
    }

    private Cursor oldCursor;
    void setBusy(boolean busy) {
        if (busy) {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        } else {
            setCursor(oldCursor);
        }
    }
    
    public float getPostScale() {
        return (float) postScale;
    }

    public void setPostScale(double postScale) {
        if (postScale != this.postScale) {
            this.postScale = postScale;
            repaint();
            BSW.instance().getMainFrame().getSlider().setValue(getSliderValue());
        }
    }

    public void multScale(float f) {
        setPostScale(postScale * f);
    }
    
    int getSliderValue() {
        int value =  (int) sliderInterpolate.inverse((Math.log(postScale)));
        return value;
    }

    void setSliderValue(int value) {
        double scale = Math.exp(sliderInterpolate.interpolate(value));
        setPostScale(scale);
    }
    
    private static class PopupItem extends JMenuItem {
        private int minPoints;
        private int maxPoints;

        public PopupItem(String name, int min, int max) {
            super(name);
            this.minPoints = min;
            this.maxPoints = max;
        }

        void updateVisiblity(int size) {
            setVisible(size >= minPoints && size <= maxPoints);
        }
    }

    private void createPopupMenu(ActionListener menuHandler) {
        JMenuItem menuItem;
        final JPopupMenu popup = new JPopupMenu() {
            @Override
            public void setVisible(boolean visible) {
                for (Component c : getComponents()) {
                    if (c instanceof PopupItem) {
                        PopupItem item = (PopupItem) c;
                        item.updateVisiblity(scaledPoints.size());
                    }
                }
                super.setVisible(visible);
            }
        };
        ActionListener copyOrCut = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                StringSelection stringSelection = new StringSelection( getPointDef() );
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents( stringSelection, ViewerPanel.this );
                if (e.getActionCommand().equals("cut")) {
                    setPointDef("");
                }
            }
        };
        menuItem = new PopupItem("Cut", 1, Integer.MAX_VALUE);
        menuItem.setActionCommand("cut");
        menuItem.addActionListener(copyOrCut);
        popup.add(menuItem);
        menuItem = new PopupItem("Copy", 1, Integer.MAX_VALUE);
        menuItem.setActionCommand("copy");
        menuItem.addActionListener(copyOrCut);
        popup.add(menuItem);
        menuItem = new PopupItem("Paste", 0, Integer.MAX_VALUE);
        menuItem.setActionCommand("paste");
        menuItem.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                paste(clipboard.getContents(null));
            }
        });
        popup.add(menuItem);

        menuItem = new PopupItem("Remove Page", 0, Integer.MAX_VALUE);
        menuItem.setToolTipText("Marks this page as one that should not be converted");
        menuItem.setActionCommand("remove_page");
        menuItem.addActionListener(menuHandler);
        popup.add(menuItem);
        
        popup.add(new JSeparator());

        menuItem = new PopupItem("Fix Perspective and crop", 4,4);
        menuItem.setToolTipText("Sets the perspective and crops in one operation");
        menuItem.setActionCommand("perspective_and_crop");
        menuItem.addActionListener(menuHandler);
        popup.add(menuItem);

        menuItem = new PopupItem("Fix Perspective", 4,4);
        menuItem.setToolTipText("Fixes keystone or rotation by squaring up the corners");
        menuItem.setActionCommand("perspective");
        menuItem.addActionListener(menuHandler);
        popup.add(menuItem);

        menuItem = new PopupItem("Rotate", 2,2);
        menuItem.setToolTipText("Rotates the image so that the two points are horizontal");
        menuItem.setActionCommand("rotate");
        menuItem.addActionListener(menuHandler);
        popup.add(menuItem);

        menuItem = new PopupItem("Crop", 2,2);
        menuItem.setToolTipText("Crops an image to the indicated box");
        menuItem.setActionCommand("crop");
        menuItem.addActionListener(menuHandler);
        popup.add(menuItem);

        menuItem = new PopupItem("Crop and Scale", 2,2) {
            @Override
            void updateVisiblity(int size) {
                setVisible(size == 2 && ((Math.abs(xCropScale - 1) > .0001) || (Math.abs(yCropScale - 1) > .0001)));
            }
        };
        menuItem.setToolTipText("Crops the image to the rectange, then scales it to match the original crop");
        menuItem.setActionCommand("crop_and_scale");
        menuItem.addActionListener(menuHandler);
        popup.add(menuItem);

        popup.add(new JSeparator());

        menuItem = new PopupItem("Auto Levels", 0,2);
        menuItem.setToolTipText("Calculates the autolevels for this page.");
        menuItem.setActionCommand("auto_levels");
        menuItem.addActionListener(menuHandler);
        popup.add(menuItem);

        menuItem = new PopupItem("Auto RGB Levels", 0,2);
        menuItem.setToolTipText("Calculates the autolevels for each color component separately");
        menuItem.setActionCommand("auto_rgb_levels");
        menuItem.addActionListener(menuHandler);
        popup.add(menuItem);
        
        menuItem = new PopupItem("Whiteout", 2,999);
        menuItem.setToolTipText("Whites out an area of the image");
        menuItem.setActionCommand("whiteout");
        menuItem.addActionListener(menuHandler);
        popup.add(menuItem);
        if (BSW.EXPERIMENTAL) {
            menuItem = new PopupItem("Gray Card", 0,2);
            menuItem.setToolTipText("Adjusts white balance and and exposure");
            menuItem.setActionCommand("gray_card");
            menuItem.addActionListener(menuHandler);
            popup.add(menuItem);

            menuItem = new PopupItem("White balance", 0,2);
            menuItem.setToolTipText("Adjusts white balance");
            menuItem.setActionCommand("white_balance");
            menuItem.addActionListener(menuHandler);
            popup.add(menuItem);

            // not really working right now.
            menuItem = new PopupItem("Extend Color Range", 0,2);
            menuItem.setToolTipText("Does the equivalent of autolevels, but doesn't change the brightness");
            menuItem.setActionCommand("balanced_normalize_lighting");
            menuItem.addActionListener(menuHandler);
            popup.add(menuItem);

        }

        menuItem = new PopupItem("Normalize Lighting", 0,2);
        menuItem.setToolTipText("Use this on a blank (preferably gray), page to adjust for uneven lighting");
        menuItem.setActionCommand("normalize_lighting");
        menuItem.addActionListener(menuHandler);
        popup.add(menuItem);

        MouseListener popupListener = new PopupListener(popup);
        addMouseListener(popupListener);
    }

    @Override
    public void paintComponent(Graphics g) {
        updateStatus();
        superPaintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(Color.BLUE);
        AffineTransform tr = new AffineTransform();
        Polygon poly = new Polygon();
        List<Point> points = getImagePoints();
        for (int i=0; i < points.size(); i++) {
            Point pt = points.get(i);
            tr.setToTranslation(pt.getX(), pt.getY());
            g2.draw(plus.createTransformedShape(tr));
            if (i != 2 || points.size() != 3) {
                poly.addPoint(pt.x, pt.y);
            }
        }
        g2.setColor(Color.RED);
        for (Point2D registrationPoint : registrationPoints) {
            Point pt = getImagePoint(registrationPoint);
            tr.setToTranslation(pt.getX(), pt.getY());
            g2.draw(plus.createTransformedShape(tr));
        }
        g2.setColor(Color.BLUE);
        tr.setToTranslation(0, 0);
        if (points.size() > 1) {
            if (points.size() == 2) {
                Rectangle r = new Rectangle(points.get(0));
                r.add(points.get(1));
                g2.draw(r);
            } else {
                Path2D.Float polyLine = new Path2D.Float();
                polyLine.moveTo(points.get(0).x, points.get(0).y);
                polyLine.lineTo(points.get(1).x, points.get(1).y);
                polyLine.lineTo(points.get(2).x, points.get(2).y);
                if (points.size() == 4) {
                    polyLine.lineTo(points.get(3).x, points.get(3).y);
                    polyLine.closePath();
                }
                g2.draw(polyLine.createTransformedShape(tr));
            }
        }
        if (previewCrop != null) {
            Path2D.Float polyLine = new Path2D.Float();
            Point pt = getImagePoint(previewCrop[0]);
            polyLine.moveTo(pt.getX(), pt.getY());
            for (int i=1; i < 4; i++) {
                pt = getImagePoint(previewCrop[i]);
                polyLine.lineTo(pt.getX(), pt.getY());
            }
            polyLine.closePath();
            BasicStroke stroke = new BasicStroke(1);
            g2.setColor(Color.GREEN.darker().darker());
            g2.setStroke(stroke);
            g2.draw(polyLine);
        }
        if (BSW.instance().getPreviewedImage().getPreviewHolder() != null) {
            if (BSW.instance().getPreviewedImage().getPreviewHolder().isDeleted()) {
                markDeleted(g);
            }
        }
        g2.dispose();
    }

    public List<Point2D> getPoints() {
        return scaledPoints;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            clearPoints();
        }
    }

    public void clearPoints() {
        previewCrop = null;
        scaledPoints.clear();
        registrationPoints.clear();
        xCropScale = 1;
        yCropScale = 1;
        repaint();
    }

    public void setScaledPoints(Point2D[] points) {
        scaledPoints.clear();
        xCropScale = 1;
        yCropScale = 1;
        previewCrop = null;
        scaledPoints.addAll(Arrays.asList(points));
        repaint();
    }

    public void setPointDef(String currentLine) {
        Matcher matcher = Operation.ARG_PATTERN.matcher(currentLine);
        String[] points = Operation.getArgs(matcher);
        scaledPoints.clear();
        for (int i=0; i < points.length; i++) {
            try {
                double x = Double.parseDouble(points[i]);
                double y = Double.parseDouble(points[++i]);
                scaledPoints.add(new Point2D.Double(x, y));
            } catch (Exception e) {
                continue;
            }
        }
        repaint();
    }
    
    /**
     * Returns the cropped size, if there are either scaled points or
     * if a crop is previewed. Otherwise return null.
     */
    public Dimension getCroppedSize() {
        if (scaledPoints.size() == 2) {
            double width = Math.abs(scaledPoints.get(1).getX() - scaledPoints.get(0).getX());
            double height = Math.abs(scaledPoints.get(1).getY() - scaledPoints.get(0).getY());
            return new Dimension((int) width, (int) height);
        } else if (scaledPoints.size() == 4) {
            double[] p = new double[8];
            for (int i=0; i < 4; i++) {
                p[i*2] = scaledPoints.get(i).getX();
                p[i*2+1] = scaledPoints.get(i).getY();
            }
            // Copied from Perspective.java
            double dx1 = Point2D.distance(p[0], p[1], p[2], p[3]);
            double dx2 = Point2D.distance(p[6], p[7], p[4], p[5]);
            double dx = (dx1 + dx2) / 2;
            double dy1 = Point2D.distance(p[0], p[1], p[6], p[7]);
            double dy2 = Point2D.distance(p[2], p[3], p[4], p[5]);
            double dy = (dy1 + dy2) / 2;
            return new Dimension((int) dx, (int) dy);
        } else if (previewCrop != null) {
            double width =  Math.abs(previewCrop[0].getX() - previewCrop[2].getX());
            double height = Math.abs(previewCrop[0].getY() - previewCrop[2].getY());
            return new Dimension((int) width, (int) height);
        }
        return null;
    }

    public String getPointDef() {
        if (scaledPoints.isEmpty()) {
            return "";
        }
        StringBuilder str = new StringBuilder(" ");
        for (Point2D pt : scaledPoints) {
            str.append(Math.round(pt.getX())).append(",").append(Math.round(pt.getY())).append(", ");
        }
        str.setLength(str.length() - 2);
        return str.toString();
    }
    
    private boolean isPointInside(Point2D scaledPoint) {
        if (scaledPoints.size() == 2) {
            Rectangle2D r = new Rectangle2D.Double(scaledPoints.get(0).getX(), scaledPoints.get(0).getY(),0,0);
            r.add(scaledPoints.get(1));
            return r.contains(scaledPoint);
        } else {
            boolean first = true;
            Path2D.Double path = new Path2D.Double();
            for (Point2D pt : scaledPoints) {
                if (first) {
                    path.moveTo(pt.getX(), pt.getY());
                    first = false;
                } else {
                    path.lineTo(pt.getX(), pt.getY());
                }
            }
            path.closePath();
            return path.contains(scaledPoint);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    @Override
    public void keyTyped(KeyEvent e) {}

    private Point2D getNearest(Point2D pt) {
        double distanceSq = Double.MAX_VALUE;
        Point2D closest = null;
        for (Point2D test : scaledPoints) {
            double testDistanceSq = pt.distanceSq(test);
            if (testDistanceSq < distanceSq) {
                distanceSq = testDistanceSq;
                closest = test;
            }
        }
        return closest;
    }

    private Point2D getScaledPoint(Point pt) {
        double scale = postScale;
        double x = (pt.x - getOrigin().x) / scale;
        double y = (pt.y - getOrigin().y) / scale;
        return new Point2D.Double(x, y);
    }

    private Point getImagePoint(Point2D pt) {
        double scale = postScale;
        int x = (int) Math.round((pt.getX() * scale + getOrigin().x));
        int y = (int) Math.round((pt.getY() * scale + getOrigin().y));
        return new Point(x, y);
    }

    private List<Point> getImagePoints() {
        ArrayList<Point> imagePoints = new ArrayList<>();
        for (Point2D pt : getPoints()) {
            imagePoints.add(getImagePoint(pt));
        }
        return imagePoints;
    }

    private boolean paste(Transferable contents) {
        DataFlavor bestFlavor = DataFlavor.selectBestTextFlavor(contents.getTransferDataFlavors());
        if (bestFlavor == null) {
            return false;
        }
        try {
            String txt = (String) contents.getTransferData(bestFlavor);
            setPointDef(txt);
            return true;
        } catch (UnsupportedFlavorException | IOException ex) {
            // ignore
        }
        return false;
    }

    private void updateStatus() {
        StringBuilder str = new StringBuilder();
        if (BSW.instance().getPreviewedImage().getPreviewHolder() == null) {
            return;
        }
        double dpi = BSW.instance().getPreviewedImage().getPreviewHolder().getDPI();
        str.append(String.format("%.0f DPI, Image: ", dpi));
        str.append(getImageSize(dpi, source.getWidth(), source.getHeight()));

        Dimension d = getCroppedSize();
        if (d != null) {
            str.append(", crop: ");
            str.append(getImageSize(dpi, d.width, d.height));
        }
        String rightStatus = String.format("Scale: %.2f", postScale);
        BSW.instance().getMainFrame().setStatusText(str.toString(), rightStatus);
    }

    private String getImageSize(double dpi, int width, int height) {
        double widthInches = width / dpi;
        double heightInches = height / dpi;
        StringBuilder str = new StringBuilder();
        if (dpi > 0) {
            String leftStatus = String.format(
                "%s\" x %s\" (%.0fx%.0f mm), ",
                new Fraction(widthInches, 8), new Fraction(heightInches, 8), 
                widthInches * INCHES_TO_MM, heightInches * INCHES_TO_MM
            );
            str.append(leftStatus);
        }
        str.append(String.format("pixels: %dx%d", width, height));
        return str.toString();
    }
    
    private void markDeleted(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        BasicStroke stroke = new BasicStroke(3);
        double width = getSource().getWidth() * postScale;
        double height = getSource().getHeight() * postScale;
        Line2D line1 = new Line2D.Double(0, 0, width, height);
        Line2D line2 = new Line2D.Double(width, 0, 0, height);
        g2.setColor(Color.RED);
        g2.setStroke(stroke);
        g2.draw(line1);
        g2.draw(line2);
    }

    private void setMouseCursor(MouseEvent e) {
        Point pt = new Point(e.getX(), e.getY());
        Point2D scaledPoint = getScaledPoint(pt);
        Point2D nearest = getNearest(scaledPoint);
        if (nearest != null) {
            Point nearestPt = getImagePoint(nearest);
            if (nearestPt.distance(pt) <= MAX_DISTANCE_TO_POINT) {
                setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            } else if (isPointInside(scaledPoint)) {//inside
                if (e.isControlDown()) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                } else {
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                }
            } else {
                if (scaledPoints.size() < 4) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                } else {
                    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
            }
        } else {
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        }
    }

    // a modified version of DisplayJAI.paintComponent that handles scaling.
    public synchronized void superPaintComponent(Graphics g) {

        Graphics2D g2d = (Graphics2D)g;

        // empty component (no image)
        if ( source == null ) {
            g2d.setColor(getBackground());
            g2d.fillRect(0, 0, getWidth(), getHeight());
            return;
        }

        // clear damaged component area
        Rectangle clipBounds = g2d.getClipBounds();
        g2d.setColor(getBackground());
        g2d.fillRect(clipBounds.x,
                     clipBounds.y,
                     clipBounds.width,
                     clipBounds.height);

        // account for borders
        Insets insets = getInsets();
        int tx = insets.left + originX;
        int ty = insets.top  + originY;

        // Translation moves the entire image within the container
        try {
            AffineTransform tr = AffineTransform.getScaleInstance(postScale, postScale);
            tr.translate(tx, ty);
            g2d.drawRenderedImage(source, tr);
        } catch( OutOfMemoryError e ) {
        }
    }
}
