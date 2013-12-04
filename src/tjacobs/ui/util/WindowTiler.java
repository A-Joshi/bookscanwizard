/*
 * Created on Mar 27, 2005 by @author Tom Jacobs
 *
 */
package tjacobs.ui.util;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Iterator;

public class WindowTiler {

	private WindowTiler() {}
	
	public static final int TILE = 0;
	public static final int CASCADE = 1;
	public static final int INCH = 72;
	public static final int CASCADE_AMT = 25;
	private static int sWindowArrangementStyle = TILE;
	protected Container mContent;

	private static ArrayList<Window> sWindows = new ArrayList<Window>();
	private static WindowRemover sRemover = new WindowRemover();
	
	public static void setWindowArrangementStyle(int style) {
		sWindowArrangementStyle = style;
	}

	public static int getWindowArrangementStyle() {
		return sWindowArrangementStyle;
	}
	
	public static Point SuggestLocation(Window w) {
		boolean removed = sWindows.remove(w);
		Point p = SuggestLocation(w.getSize());
		if (removed) {
			sWindows.add(w);
		}
		return p;
	}
	
	public static Point SuggestLocation(Dimension size) {
		if (sWindows.size() == 0) {
			return new Point(INCH,INCH);
		}
		if (sWindowArrangementStyle == TILE) {
			Dimension scr = Toolkit.getDefaultToolkit().getScreenSize();
			Window last = sWindows.get(sWindows.size() - 1);
			Rectangle r = last.getBounds();
			int x = r.x + r.width;
			int y = r.y;
			if (scr.width > x + size.width) {
				return new Point(x,y);
			}
			else {
				x = INCH;
				y+= r.height;
				if (y > scr.height) return null;
				else return new Point(x,y);
			}
		}
		else {
			Dimension scr = Toolkit.getDefaultToolkit().getScreenSize();
			Window last = sWindows.get(sWindows.size() - 1);
			Rectangle r = last.getBounds();
			int x = r.x + CASCADE_AMT;
			int y = r.y + CASCADE_AMT;
			if (x >= scr.width - 2.5 * INCH) {
				x = INCH;
			}
			if (y >= scr.height - 2.5 * INCH) {
				y = INCH;
			}
			return new Point(x, y);
		}
	}
	
	public static void retileWindows() {
		Iterator<Window> _i = sWindows.iterator();
		int x = INCH;
		int y = INCH;
		Dimension scr = Toolkit.getDefaultToolkit().getScreenSize();
		while (_i.hasNext()) {
			Window w = _i.next();
			w.setLocation(x, y);
			x += w.getWidth();
			if (x > scr.width - (2.5 * INCH)) {
				x = INCH;
				y += w.getHeight();
			}
		}
	}
	
	public static void recascadeWindows() {
		Iterator<Window> _i = sWindows.iterator();
		int x = INCH;
		int y = INCH;
		Dimension scr = Toolkit.getDefaultToolkit().getScreenSize();
		while (_i.hasNext()) {
			Window w = _i.next();
			w.setLocation(x, y);
			x += CASCADE_AMT;
			y += CASCADE_AMT;
			if (x > scr.width - (2.5 * INCH)) {
				x = INCH;
			}
			if (y > scr.height - (2.5 * INCH)) {
				y = INCH;
			}
		}		
	}
	
	public static void Watch(Window w) {
		if (sWindows.contains(w)) return;
		//System.out.println(sRemover.mExitWhenAllClosed);
		sWindows.add(w);
		w.addWindowListener(sRemover);
	}
	
	public static void RemoveWatch(Window w) {
		sWindows.remove(w);
		w.removeWindowListener(sRemover);
	}

	private static class WindowRemover extends WindowAdapter {
		
		private boolean mExitWhenAllClosed = true;
		
		public void windowClosing(WindowEvent we) {
			//sWindows.remove(we.getWindow());
			RemoveWatch(we.getWindow());
			//System.out.println("Window removed. Have: " + sWindows.size());
			if (mExitWhenAllClosed && sWindows.size() == 0) {
				System.exit(0);
			}			
//			else {
//				CollectionUtils.printCollection(sWindows);
//			}
		}
		
		public void windowClosed(WindowEvent we) {
			//sWindows.remove(we.getWindow());
			RemoveWatch(we.getWindow());
			if (mExitWhenAllClosed && sWindows.size() == 0) {
				System.exit(0);
			}
//			else {
//				CollectionUtils.printCollection(sWindows);
//			}
		}
	}
	
	public static void setExitWhenAllWindowsClosed(boolean b) {
		if (sRemover != null) {
			sRemover.mExitWhenAllClosed = b;
		}
	}
	
	public static boolean getExitWhenAllWindowsClosed() {
		if (sRemover == null) {
			return false;
		}
		return sRemover.mExitWhenAllClosed;
	}
	
	/**
	 * package private on purpose
	 */

	static Iterator<Window> getWindows() {
		return sWindows.iterator();
	}
}
