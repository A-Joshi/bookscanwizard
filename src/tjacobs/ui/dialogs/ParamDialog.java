package tjacobs.ui.dialogs;

import java.awt.HeadlessException;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import tjacobs.ui.ex.FileField;
import tjacobs.ui.util.WindowUtilities;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

/** Usage:
 * [code]
 * 	ParamDialog pd = new ParamDialog(new String[] {"A", "B", "C"});
 * pd.pack();
 * pd.setVisible(true);
 * Properties p = pd.getProperties();
 */

public class ParamDialog extends StandardDialog {

	private static final long serialVersionUID = 1L;
	public static final String SECRET = "(SECRET)";
	public static final String CHECKBOX = "(CHECKBOX)";
	public static final String BUTTON = "(BUTTON)";
	public static final String FILE = "FILE)";
	public static final String FONT = "(FONT)";
	public static final String BUTTON_SOLO = "(BUTTON_BOTH)";
	//public static final String BLANK = "(BLANK)";
	public static final String COMBO = "COMBO)";
	
	String[] mFields;
	HashMap<String, GetText> mValues = new HashMap<String, GetText>();
	private boolean mShowApplyButton = false;
	//UniversalChangeListener mUChangeListener = null;
	private boolean mCancel = false;
	private int mColumns = 1;
	private JScrollPane mMain;
		
	private static interface GetText {
		public String getText2();
		public void setText2(String txt);
	}
	
	private static class TextField extends JTextField implements GetText {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public void setText2(String s) {
			setText(s);
		}
		
		public String getText2() {
			return getText();
		}
	}
	private static class PasswordField extends JPasswordField implements GetText {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public void setText2(String s) {
			setText(s);
		}
		
		@SuppressWarnings("deprecation")
		public String getText2() {
			return getText();
		}
	}
	private static class TextButton extends JButton implements GetText {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public TextButton(String s) {
			super(s);
		}
		
		public void setText2(String s) {
			setText(s);
		}
		
		public String getText2() {
			return getText();
		}
	}
	private static class TextCheckbox extends JCheckBox implements GetText {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public void setText2(String s) {
			setSelected("true".equals(s));
		}
		
		public String getText2() {
			return "" + isSelected();
		}
		
		public void _setText(String s) {
			super.setText(s);
		}
		
		public String _getText() {
			return super.getText();
		}
		
		public String getLabel() {
			return "";
		}
	}
	private static class TextCombo extends JComboBox implements GetText {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public String getText2() {
			return getSelectedItem().toString();
		}
		
		public void setText2(String str) {
			setSelectedItem(str);
		}
	}
	
	private static class TextFileField extends FileField implements GetText {
		private static final long serialVersionUID = 1L;

		public TextFileField(String name) {
			super();
			setName(name);
		}
		
		public void setText2(String text) {
			setText(text);
		}
		
		public String getText2() {
			return getText();
		}
	}
	
	public ParamDialog(String[] fields) throws HeadlessException {
		this(null, fields);
	}
	
	public ParamDialog(JFrame owner, String[] fields) {
		this (null, fields, null);
	}
	
	public ParamDialog(JFrame owner, String[] fields, String[] initialValues) {
		super(owner);
		setModal(true);
		setParams(fields, initialValues);
	}
	
	public void addField(String field, String initialValue, int location) {
		if (location < 0) location = 0;
		location = Math.min(location, mFields.length);
		String newFields[] = new String[mFields.length + 1];
		String newValues[] = new String[mFields.length + 1];
		Properties p = getProperties();
		for (int i = 0; i < location; i++) {
			newFields[i] = mFields[i];
			newValues[i] = p.getProperty(newFields[i]);
		}
		newFields[location] = field;
		newValues[location] = initialValue;
		for (int i = location; i < mFields.length; i++) {
			newFields[i + 1] = mFields[i];
			newValues[i + 1] = p.getProperty(newFields[i + 1]);
		}
		setParams(newFields, newValues);
		mMain.invalidate();
		invalidate();
		mMain.validate();
		validate();
		repaint();
	}
	
	public void removeField(int fieldNum) {
		if (fieldNum < 0) fieldNum = 0;
		fieldNum = Math.min(fieldNum, mFields.length - 1);
		String newFields[] = new String[mFields.length - 1];
		String newValues[] = new String[mFields.length - 1];
		Properties p = getProperties();
		for (int i = 0; i < fieldNum; i++) {
			newFields[i] = mFields[i];
			newValues[i] = p.getProperty(newFields[i]);
		}
		for (int i = fieldNum + 1; i < mFields.length; i++) {
			newFields[i - 1] = mFields[i];
			newValues[i - 1] = mFields[i];
		}
		setParams(newFields, newValues);		
		mMain.invalidate();
		invalidate();
		mMain.validate();
		validate();
		repaint();
	}
	
	public String[] getFieldNames() {
		return mFields.clone();
	}
	
	/**
	 * Order of fields is not guarenteed
	 * @param p
	 */
	public void setParams(Properties p) {
		String fields[] = new String[p.size()];
		String initialValues[] = new String[p.size()];
		Iterator<Object> _i = p.keySet().iterator();
		for (int i = 0; i  < fields.length; i++) {
			fields[i] = _i.next().toString();
			initialValues[i] = p.getProperty(fields[i]);
		}
		setParams(fields, initialValues);
	}
	
	public void setParams(String fields[], String initialValues[]) {
		this.mFields = Arrays.copyOf(fields, fields.length);
		JPanel main = null;
		if (mMain != null) {
			main = (JPanel) mMain.getViewport().getView();
			main.removeAll();
		}
		if (main == null) main = new JPanel();
//		if (applyOnChange()) {
//			ucl = new UniversalChangeListener() {
//				public void apply(AWTEvent ev) {
//					ParamDialog.this.apply();
//				}
//			};
//		}
		//changed for columns
		//main.setLayout(new GridLayout(fields.length, 1));
		main.setLayout(new GridLayout(mFields.length / mColumns, mColumns));
		createMainContent(mFields, initialValues, main);
			if (mMain == null) {
			JScrollPane sp = new JScrollPane(main);
			Dimension d = main.getPreferredSize();
			sp.setPreferredSize(new Dimension(d.width + 10, d.height + 10));
			mMain = sp;
			setMainContent(sp);
		}
	}
	
	protected Component[] createMainContent(String fields[], String initialValues[], Container container) {
		JPanel buttonP = null;
		Component comps[] = new Component[fields.length];
		for (int i = 0; i < fields.length; i++) {
			JPanel con = new JPanel(new FlowLayout(FlowLayout.RIGHT));

			if (fields[i].endsWith(BUTTON_SOLO)) {
				JLabel l = new JLabel(fields[i].substring(0, fields[i].length() - BUTTON_SOLO.length()));
				con.add(l);
				TextButton jb = new TextButton("...");
				comps[i] = null;
				mValues.put(fields[i], jb);
				//mValues.put(fields[i], new GetText[] {jb});
				con.add(jb);			
			}
			else
			if (fields[i].endsWith(BUTTON)) {
				TextButton jb = new TextButton(fields[i].substring(0, fields[i].length() - BUTTON.length()));
				comps[i] = null;
				mValues.put(fields[i], jb);
				//mValues.put(fields[i], new GetText[] {jb});
				if (buttonP != null) {
					buttonP.add(jb);
					buttonP = null;
					continue;
				}
				else {
					con.add(jb);
					buttonP = con;
				}
			}
			else {
				buttonP = null;
				if (fields[i].endsWith(CHECKBOX)) {
					TextCheckbox tb = new TextCheckbox();
					comps[i] = tb;
					con.add(new JLabel(fields[i].substring(0, fields[i].length() - CHECKBOX.length())));
					con.add(tb);
					mValues.put(fields[i], tb);
					//mValues.put(fields[i], new GetText[] {tb});

				}
				else if (fields[i].endsWith(COMBO)) {
					int idx = fields[i].lastIndexOf('(');
					if (idx == -1) {
						throw new IllegalArgumentException(fields[i]);
					}
					String[] args = fields[i].substring(idx + 1, fields[i].length() - COMBO.length()).split(" ");
					TextCombo combo = new TextCombo();
					comps[i] = combo;
					for (int j = 0; j < args.length; j++) {
						combo.addItem(args[j]);
					}
					combo.setEditable(false);
					//String nm =fields[i].substring(0, idx);
					JLabel l = new JLabel(fields[i].substring(0, idx));
					con.add(l);
					con.add(combo);
					mValues.put(fields[i], combo);
					//mValues.put(fields[i], new GetText[] {combo});
				}
				else if (fields[i].endsWith(SECRET)) {
					PasswordField tf;
					con.add(new JLabel(fields[i].substring(0, fields[i].length() - SECRET.length())));
					tf = new PasswordField();
					comps[i] = tf;
					tf.setColumns(12);
					con.add(tf);
//					if (initialValues != null && initialValues.length > i) {
//						tf.setText(initialValues[i]);
//					}
					mValues.put(fields[i], tf);
					//mValues.put(fields[i], new GetText[] {tf});
				} else if (fields[i].endsWith(FONT)) {
					//hmm...
					//fc.
				} else if (fields[i].endsWith(FILE)) {
					int idx = fields[i].lastIndexOf('(');
					if (idx == -1) {
						throw new IllegalArgumentException(fields[i]);
					}
					String argsTxt = fields[i].substring(idx + 1, fields[i].length() - FILE.length()); 
					//System.out.println("argsTxt: " + argsTxt);
					String[] args = argsTxt.split(" ");
					TextFileField ff = new TextFileField(fields[i]);
					comps[i] = ff;
					for (int j = 0; j < args.length; j++) {
						ff.setText(args[j]);
					}
					//ff.setEditable(true);
					fields[i] =fields[i].substring(0, idx);
					JLabel l = new JLabel(fields[i].substring(0, idx));
					con.add(l);
					con.add(ff);
					mValues.put(fields[i], ff);				
				}
				else {
					TextField tf;
					con.add(new JLabel(fields[i]));
					tf = new TextField();
					tf.setColumns(12);
					con.add(tf);
//					if (initialValues != null && initialValues.length > i) {
//						tf.setText(initialValues[i]);
//					}
					mValues.put(fields[i], tf);
					comps[i] = tf;
					//mValues.put(fields[i], new GetText[] {tf});
				}
				if (comps[i] != null) {
					if (initialValues != null && initialValues.length > i) {
						((GetText)comps[i]).setText2(initialValues[i]);
					}
				}
			}
			container.add(con);
		}
		return comps;
	}
	
	public Component getComponentForField(String name) {
		return (Component)mValues.get(name);
	}

	public boolean showApplyButton() {
		return mShowApplyButton;
	}
	
	public void setActionListener(String buttonName, ActionListener listener) {
		GetText gt = mValues.get(buttonName);
		if (gt instanceof AbstractButton) {
			AbstractButton ab = (AbstractButton) gt;
			ab.addActionListener(listener);
		}
		
	}

	public void apply() {
	}
		
	public void cancel() {
		mCancel = true;
		super.cancel();
	}
	
	public void setField(String field, String value) {
		//JTextField tf = mValues.get(field);
		GetText tf = mValues.get(field);
		if (tf != null) {
			tf.setText2(value);
		}
	}
	
	public Properties getProperties() {
		if (mCancel) return null;
		Properties p = new Properties();
		for (int i = 0; i < mFields.length; i++) {
			p.put(mFields[i], mValues.get(mFields[i]).getText2());
		}
		return p;
	}
	
	public void setProperties(Properties p) {
		Iterator<Object> _i= p.keySet().iterator();
		while (_i.hasNext()) {
			String key = (String) _i.next();
			String value = (String) p.get(key);
			setField(key, value);
		}
	}
	
	protected Component getMainPane() {
		if (mMain == null) return null;
		return mMain.getViewport().getView();
	}
	
	public static Properties getProperties(Properties p) {
		String[] fields = new String[p.size()];
		List<String> names = new ArrayList<String>(p.size());
		Iterator<?> _i = p.keySet().iterator();
		while (_i.hasNext()) {
			names.add(_i.next().toString());
		}
		Collections.sort(names);
		names.toArray(fields);
		final ParamDialog pd = new ParamDialog(fields);
		pd.setProperties(p);
		pd.pack();
		pd.setLocation(100,100);
		pd.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
				pd.cancel();
			}
		});
		pd.setVisible(true);
		return pd.getProperties();		
	}
	
	public static Properties getProperties(String[] fields, String[] initialValues) {
		System.out.println("fields: " + fields[0]);
		final ParamDialog pd = new ParamDialog(fields);
		System.out.println("fields: " + fields[0]);
		pd.setParams(fields, initialValues);
		System.out.println("fields: " + fields[0]);
		//pd.setModal(false);
		//Window w = WindowUtilities.visualize(pd);
		pd.pack();
		pd.setLocation(100,100);
		pd.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
				pd.cancel();
			}
		});
		pd.setVisible(true);
		return pd.getProperties();		
	}
	
	public static Properties getProperties(String[] fields) {
		ParamDialog pd = new ParamDialog(fields);
		//pd.setModal(false);
		WindowUtilities.visualize(pd);
		return pd.getProperties();		
	}
	
	public void setShowApplyButton(boolean b) {
		mShowApplyButton = b;
	}
	
	public boolean getShowApplyButton() {
		return mShowApplyButton;
	}
	
	public boolean applyOnChange() {
		return false;
	}
	
	public void save(File f) throws IOException {
		FileOutputStream out = new FileOutputStream(f);
		getProperties().store(out, "params");
	}
	
	public void load(File f) throws IOException {
		Properties p = new Properties();
		p.load(new FileInputStream(f));
		setProperties(p);
	}

	public int getColumns() {
		return mColumns;
	}
	
	public void setColumns(int columns) {
		mColumns = columns;
		JPanel main = (JPanel) mMain.getViewport().getView();
		
		main.setLayout(new GridLayout(mFields.length / mColumns + (mFields.length % mColumns == 0 ? 0 : 1), mColumns));
		System.out.println(mMain.getViewport().getPreferredSize());
		Dimension di = mMain.getViewport().getPreferredSize();
		mMain.setPreferredSize(new Dimension(di.width + 10, di.height + 10));
		if (isVisible()) {
			Dimension d = getSize();
			setSize(d.width * columns, d.height / columns);
			main.repaint();
		}
	}
}
