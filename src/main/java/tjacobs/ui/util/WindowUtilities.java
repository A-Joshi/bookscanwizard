package tjacobs.ui.util;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

public abstract class WindowUtilities {

	public static final int RESIZE_MARGIN_SIZE = 4;
	private static Window sExitWindow = null;
	private static boolean sWindowCloseOnEsc = false;
	private static KeyListener sEscCloser;

	public static void setWindowCloseOnEscape(boolean b) {
		if (sWindowCloseOnEsc == b)
			return;
		sWindowCloseOnEsc = b;
		Iterator<Window> _i = WindowTiler.getWindows();
		if (b) {
			sEscCloser = new KeyAdapter() {
				public void keyPressed(KeyEvent ke) {
					if (ke.getKeyCode() == KeyEvent.VK_ESCAPE) {
						((JFrame) ke.getSource()).dispose();
					}
				}
			};
			while (_i.hasNext()) {
				_i.next().addKeyListener(sEscCloser);
			}
		} else {
			while (_i.hasNext()) {
				_i.next().removeKeyListener(sEscCloser);
			}
			sEscCloser = null;
		}
	}

	public static interface CreatePopupWindow {
		public JDialog createPopupWindow(Point p);
	}

	// ///
	/**
	 * Returns an point which has been adjusted to take into account of the
	 * desktop bounds, taskbar and multi-monitor configuration.
	 * <p>
	 * This adustment may be cancelled by invoking the application with
	 * -Djavax.swing.adjustPopupLocationToFit=false
	 */
	public static Point adjustPopupLocationToFitScreen(Component popup,
			Component invoker, int xposition, int yposition) {
		Point p = new Point(xposition, yposition);

		if (// popupPostionFixDisabled == true ||
		GraphicsEnvironment.isHeadless())
			return p;

		Toolkit toolkit = Toolkit.getDefaultToolkit();
		Rectangle screenBounds;
		Insets screenInsets;
		GraphicsConfiguration gc = null;
		// Try to find GraphicsConfiguration, that includes mouse
		// pointer position
		GraphicsEnvironment ge = GraphicsEnvironment
				.getLocalGraphicsEnvironment();
		GraphicsDevice[] gd = ge.getScreenDevices();
		for (int i = 0; i < gd.length; i++) {
			if (gd[i].getType() == GraphicsDevice.TYPE_RASTER_SCREEN) {
				GraphicsConfiguration dgc = gd[i].getDefaultConfiguration();
				if (dgc.getBounds().contains(p)) {
					gc = dgc;
					break;
				}
			}
		}

		// If not found and we have invoker, ask invoker about his gc
		if (gc == null && invoker != null) {
			gc = invoker.getGraphicsConfiguration();
		}

		if (gc != null) {
			// If we have GraphicsConfiguration use it to get
			// screen bounds and insets
			screenInsets = toolkit.getScreenInsets(gc);
			screenBounds = gc.getBounds();
		} else {
			// If we don't have GraphicsConfiguration use primary screen
			// and empty insets
			screenInsets = new Insets(0, 0, 0, 0);
			screenBounds = new Rectangle(toolkit.getScreenSize());
		}

		int scrWidth = screenBounds.width
				- Math.abs(screenInsets.left + screenInsets.right);
		int scrHeight = screenBounds.height
				- Math.abs(screenInsets.top + screenInsets.bottom);

		Dimension size;

		size = popup.getPreferredSize();

		if ((p.x + size.width) > screenBounds.x + scrWidth)
			p.x = screenBounds.x + scrWidth - size.width;

		if ((p.y + size.height) > screenBounds.y + scrHeight)
			p.y = screenBounds.y + scrHeight - size.height;

		/*
		 * Change is made to the desired (X,Y) values, when the PopupMenu is too
		 * tall OR too wide for the screen
		 */
		if (p.x < screenBounds.x)
			p.x = screenBounds.x;
		if (p.y < screenBounds.y)
			p.y = screenBounds.y;

		return p;
	}

	// ///

	public static MouseListener addAsPopup(final CreatePopupWindow cpw,
			Component owner) {
		if (cpw == null || owner == null) {
			return null;
		}
		MouseListener ml = new MouseAdapter() {
			CreatePopupWindow create = cpw;

			public void mousePressed(MouseEvent ev) {
				testPopup(ev);
			}

			public void mouseReleased(MouseEvent ev) {
				testPopup(ev);
			}

			private void testPopup(MouseEvent ev) {
				if (ev.isPopupTrigger()) {
					final JDialog pop = create.createPopupWindow(ev.getPoint());
					// pop.setUndecorated(true);
					if (pop == null) {
						return;
					}
					Window w = SwingUtilities.getWindowAncestor(ev
							.getComponent());
					pop.setLocation(ev.getX() + w.getX(), ev.getY() + w.getY());
					pop.pack();
					pop.addWindowListener(new WindowAdapter() {
						public void windowDeactivated(WindowEvent we) {
							pop.dispose();
						}
					});
					pop.setFocusableWindowState(true);
					pop.setVisible(true);
					pop.requestFocus();
					// pop.show();
				}
			}
		};
		owner.addMouseListener(ml);
		return ml;
	}

	public static MouseListener addAsPopup(final JPopupMenu popup,
			Component owner) {
		if (popup == null || owner == null) {
			return null;
		}
		// popup.setUndecorated(true);
		MouseListener ml = new MouseAdapter() {
			JPopupMenu pop = popup;

			public void mousePressed(MouseEvent ev) {
				testPopup(ev);
			}

			public void mouseReleased(MouseEvent ev) {
				testPopup(ev);
			}

			private void testPopup(MouseEvent ev) {
				if (ev.isPopupTrigger()) {
					// pop.setLocation(ev.getPoint());
					// pop.pack();
					// pop.setVisible(true);
					// JPopupMenu pop = new JPopupMenu();
					// pop.add(new JMenuItem("Hi"));
					pop.show(ev.getComponent(), ev.getX(), ev.getY());

				}
			}
		};
		owner.addMouseListener(ml);
		return ml;
	}

	public static MouseListener addAsPopup(final JDialog popup, Component owner) {
		if (popup == null || owner == null) {
			return null;
		}
		// popup.setUndecorated(true);
		MouseListener ml = new MouseAdapter() {
			Window pop = popup;

			public void mousePressed(MouseEvent ev) {
				testPopup(ev);
			}

			public void mouseReleased(MouseEvent ev) {
				testPopup(ev);
			}

			private void testPopup(MouseEvent ev) {
				if (ev.isPopupTrigger()) {
					pop.setLocation(ev.getPoint());
					pop.pack();
					pop.setVisible(true);
				}
			}
		};
		owner.addMouseListener(ml);
		return ml;
	}

	public static Point getBottomRightOfScreen(Component c) {
		Dimension scr_size = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension c_size = c.getSize();
		return new Point(scr_size.width - c_size.width, scr_size.height
				- c_size.height);
	}

	public static Window visualize(Component c) {
		return visualize(c, sExitWindow == null);
	}

	public static Window visualize(Image im) {
		return visualize(new JLabel(new ImageIcon(im)));
	}
	
	public static Window visualize(Action a) {
		return visualize(new JButton(a));
	}

	public static Window visualize(Icon icon) {
		return visualize(new JLabel(icon));
	}

	public static Window visualize(Component c, int width, int height) {
		return visualize(c, sExitWindow == null, width, height);
	}

	public static Window visualize(Component c, boolean exit, int width,
			int height) {
		JFrame f;
		Window w;
		if (c instanceof Window) {
			w = (Window) c;
		} else {
			f = new JFrame();
			if (c.getName() != null) f.setTitle(c.getName());
			f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			if (sWindowCloseOnEsc) {
				f.addKeyListener(sEscCloser);
			}
			f.getContentPane().add(c);
			w = f;
		}
		// to be thread safe, this should be synchonized.
		// but it doesn't really matter in everyday situations
		if (sExitWindow == null) {
			sExitWindow = w;
		}
		w.setSize(width, height);
		// ask WindowTiler for suggestion
		Point p = WindowTiler.SuggestLocation(w);
		w.setLocation(p);
		if (exit) {
			WindowTiler.Watch(w);
			WindowTiler.setExitWhenAllWindowsClosed(true);
		}
		//the try/catch block below is set up
		//to try to get the application name from
		//tjacobs.io.App. If App is on the classpath
		//and has been instantiated, the created window
		//will use the app's name. 
		//
		//this code is implemented thru reflection to
		//remove the dependency on Main & App
		try {
			Class<?> clz = Class.forName("tjacobs.io.Main");
			if (clz != null) {
				Method m = clz.getMethod("getSingleton", new Class[] {});
				Object mainObj = m.invoke(clz, new Object[] {});
				if (mainObj != null) {
					Class<?> appClz = Class.forName("tjacobs.io.App");
					if (mainObj.getClass().equals(appClz)) {
						m = appClz.getMethod("getProjectName", new Class[] {});
						String name = (String)m.invoke(mainObj, new Object[] {});
						if (w instanceof JFrame) {
							((JFrame) w).setTitle(name);
						}
					}
				}
			}
		}
		catch (NoSuchMethodException e) {
			//this could only happen if App / Main is changed
			//and code here is not updated, but it would be a problem
			//if it did happen
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			//can be ignored - App does not exist in this
			//config, so we can't automatically set 
			//the title of the frame
		} catch (IllegalAccessException e) {
			//this could only happen if App / Main is changed
			//and code here is not updated, but it would be a problem
			//if it did happen
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			//unlikely to be a problem... would only happen
			//if the App method was called on an
			//object that wasn't an App. But it definitely
			//should be dealt with if it becomes one
			e.printStackTrace();
		}
		
		w.setVisible(true);
		return w;
	}

	public static void center(Window w) {
		Dimension dim = w.getSize();
		Dimension scr = Toolkit.getDefaultToolkit().getScreenSize();
		w.setLocation((scr.width - dim.width) / 2,
				(scr.height - dim.height) / 2);
	}

	public static Window visualize(JMenuItem menu) {
		JFrame f = new JFrame("");
		JMenuBar bar = new JMenuBar();
		f.setJMenuBar(bar);
		bar.add(menu);
		f.setBounds(100, 100, 100, 100);
		f.setLocation(WindowTiler.SuggestLocation(f));
		WindowTiler.Watch(f);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setVisible(true);
		return f;
	}

	public static Window visualize(Component c, boolean exit) {
		JFrame f;
		Window w;
		if (c instanceof Window) {
			w = (Window) c;
		} else {
			f = new JFrame();
			if (sWindowCloseOnEsc) {
				f.addKeyListener(sEscCloser);
			}
			f.getContentPane().add(c);
			w = f;
		}
		w.pack();
		// w.setLocation(100, 100);
		// Use the WindowTiler
		Point p = WindowTiler.SuggestLocation(w);
		//w.setLocation(p);
		//WindowTiler.Watch(w);
		visualize(w, exit, p.x, p.y);
		//w.setVisible(true);
		return w;
	}

	public static java.util.List<Window> visualize(Component... components) {
		java.util.List<Window> windows = new ArrayList<Window>();
		for (Component c : components) {
			windows.add(visualize(c));
		}
		return windows;
	}

	public static void visualizeModalDialog(Component c) {
		JOptionPane.showMessageDialog(null, c);
	}

	/**
	 * @deprecated use SwingUtilities.isEventDispatchThread();
	 * @param t
	 * @return
	 */
	public static boolean isUIThread(Thread t) {
		return SwingUtilities.isEventDispatchThread();
		// StackTraceElement[] elems = t.getStackTrace();
		// StackTraceElement elem = elems[elems.length - 1];
		// return (elem.getClassName().equals("java.awt.EventDispatchThread"));
	}

	public static Image tile(Image src, Image dest) {
		if (src == null || dest == null)
			throw new IllegalArgumentException(
					"Parameters to WindowUtilities.tile cannot be null");
		int destWidth = dest.getWidth(null);
		int destHeight = dest.getHeight(null);
		// return dest;
		Graphics2D g = (Graphics2D) dest.getGraphics();
		tile(src, destWidth, destHeight, g);
		return dest;
	}

	public static void tile(Image src, int destWidth, int destHeight, Graphics g) {
		int w1 = src.getWidth(null);
		int h1 = src.getHeight(null);
		for (int i = 0; i < (destHeight / h1) + 1; i++) {
			for (int j = 0; j < (destWidth / w1) + 1; j++) {
				// g.translate(j * w1, i * h1);
				g.drawImage(src, j * w1, i * h1, null);
				// g.translate(-j * w1, -i * h1);
			}
		}

	}

	public static Image tile(Image src, int wid, int ht) {
		BufferedImage im = new BufferedImage(BufferedImage.TYPE_INT_RGB, wid,
				ht);
		tile(src, wid, ht, ((Graphics2D) im.getGraphics()));
		return im;
	}

}
