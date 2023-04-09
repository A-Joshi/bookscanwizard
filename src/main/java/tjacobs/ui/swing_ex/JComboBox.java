package tjacobs.ui.swing_ex;

import java.util.Arrays;
import java.util.Vector;

import javax.swing.ComboBoxModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import tjacobs.io.App;
import tjacobs.io.Main;
import tjacobs.ui.util.WindowUtilities;

public class JComboBox extends javax.swing.JComboBox {

	private static final long serialVersionUID = 1L;
	private boolean mPersisting, mSortItems, mBlank = true;
	
	public JComboBox() {
		super();
		_init();
	}
	
	public JComboBox(ComboBoxModel aModel) {
		super(aModel);
		_init();
	}
	
	public JComboBox(Object[] items) {
		super(items);
		_init();
	}
	
	public JComboBox(Vector<?> items) {
		super(items);
		_init();
	}
	
	private ComboBoxModel mModel;
	
	private void _init() {
		mModel = getModel();
		mModel.addListDataListener(new ListDataListener() {
			public void intervalRemoved(ListDataEvent ev) {}
			public void intervalAdded(ListDataEvent ev) {}
			public void contentsChanged(ListDataEvent ev) {
				//System.out.println("contents");
				int size = mModel.getSize();
				Object selected = getSelectedItem();
				for (int i = 0; i < size; i++) {
					//System.out.println(mModel.getElementAt(i));
					if (mModel.getElementAt(i).equals(selected))
						return;
				}
				addItem(selected);
				String vals[] = null;
				if (mSortItems) {
					int ct = JComboBox.this.getMaximumRowCount();
					Object item = getSelectedItem();
					setMaximumRowCount(mModel.getSize());
					vals = CBModelAsStringArr(mModel);
					Arrays.sort(vals);
					removeAllItems();
					for (int i = 0; i < vals.length; i++) {
						addItem(vals[i]);
					}
					setSelectedItem(item);
					setMaximumRowCount(ct);
					vals = null;
				}
				if (mPersisting) {
					if (vals == null) vals = CBModelAsStringArr(mModel);
					StringBuilder sb = new StringBuilder();
					for (String val : vals) {
						sb.append(val + "\n");
					}
					Main m = Main.getSingleton();
					m.setProperty(getName() + "values", sb.toString());
					m.saveProperties();
				}
//				SwingUtilities.invokeLater(new Runnable() {
//					public void run() {
//						JComboBox.this.updateUI();
//					}
//				});
				//				updateUI();
			}
		});
	}
	
/*
	public Object getSelectedItem() {
		//System.out.println("here");
		System.out.println("Model: " + mModel.getSelectedItem());
		return super.getSelectedItem();
	}
	*/

	public String[] CBModelAsStringArr(ComboBoxModel model) {
		int size = model.getSize();
		int num = Math.min(size, getMaximumRowCount());
		String[] vals = new String[num];
		for (int i = 1; i <= num; i++) {
			vals[num - i] = model.getElementAt(size - i).toString();
		}
		return vals;
	}
	
	public void setPersistWithName(String name) {
		super.setName(name);
		mPersisting = name != null;
		if (name == null) return;
		Main m = Main.getSingleton();
		if (m == null) {
			m = new App("Combo", null);
		}
		String prop = m.getProperty(getName() + "values");
		if (prop == null) return;
		String vals[] = prop.split("\n");
		super.removeAllItems();
		boolean blank = false;
		for (int i = 0; i < vals.length; i++) {
			addItem(vals[i]);
			if (vals[i].trim().equals("")) blank = true;
		}
		if (!blank && mBlank) {
			insertItemAt("", 0);
			setSelectedIndex(0);
		}
	}
		
	public void setAlwaysShowBlankOption(boolean b) {
		mBlank = b;
	}
	
	public boolean getAlwaysShowBlankOption() {
		return mBlank;
	}
	
	public void setSortItems(boolean b) {
		mSortItems = b;
	}
	
	public boolean getSortItems() {
		return mSortItems;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		JComboBox box = new JComboBox(new String[] {"","A","B","C"});
		//box.setAlwaysShowBlankOption(false);
		box.setPersistWithName("Test");
		box.setMaximumRowCount(6);
		box.setSortItems(true);
		box.setEditable(true);
		WindowUtilities.visualize(box);
	}
}
