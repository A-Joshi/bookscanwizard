/*
 * Created on May 17, 2005 by @author Tom Jacobs
 *
 */
package tjacobs.ui.swing_ex;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
/**
 * Overridden JFileChooser adds two features: The ability to warn the user on write over, and
 * maintain the current working directory so that the next JFileChooser will open in the same place
 * as the previous one.
 * @author HP_Administrator
 *
 */
public class JFileChooser extends javax.swing.JFileChooser {
	private static final long serialVersionUID = 1L;
	private boolean mWarnOnWriteOver = false;
	private boolean mAncestorNull = false;
	
	public JFileChooser() {
		super();
		init();
	}
	
	private void init() {
		setCurrentDirectory(new File(System.getProperty("user.dir")));
	}

	public JFileChooser(String arg0) {
		super(arg0);
		init();
	}

	public JFileChooser(File arg0) {
		super(arg0);
		init();
	}

	public JFileChooser(FileSystemView arg0) {
		super(arg0);
		init();
	}

	public JFileChooser(File arg0, FileSystemView arg1) {
		super(arg0, arg1);
		init();
	}

	public JFileChooser(String arg0, FileSystemView arg1) {
		super(arg0, arg1);
		init();
	}
	
	public boolean getWarnOnWriteOver() {
		return mWarnOnWriteOver;
	}
	
	public void setWarnOnWriteOver(boolean b) {
		mWarnOnWriteOver = b;
		if (b) {
			addPropertyChangeListener(new PropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent pe) {
					//System.out.println("Here");
					//System.out.println(pe.getPropertyName() + ": " + pe.getNewValue());
					if (pe.getPropertyName().equals("ancestor") && pe.getNewValue() == null) {
						mAncestorNull = true;
						return;
					}
					mAncestorNull = false;
				}
			});
		}
	}
	
	private boolean changeDirOnSave = true, changeDirOnLoad = true;
	
	public int showSaveDialog(Component parent) {
		int result = super.showSaveDialog(parent);
		if (changeDirOnSave && result == javax.swing.JFileChooser.APPROVE_OPTION) {
			File selected = getSelectedFile();
			selected = selected.getParentFile();
			System.setProperty("user.dir", selected.getAbsolutePath());
		}
		return result;
	}
	
	public int showOpenDialog(Component parent) {
		int result = super.showOpenDialog(parent);
		if (changeDirOnLoad && result == javax.swing.JFileChooser.APPROVE_OPTION) {
			File selected = getSelectedFile();
			selected = selected.getParentFile();
			System.setProperty("user.dir", selected.getAbsolutePath());
		}
		return result;		
	}
	
	public boolean isChangeDirOnSave() {
		return changeDirOnSave;
	}

	public void setChangeDirOnSave(boolean changeDirOnSave) {
		this.changeDirOnSave = changeDirOnSave;
	}

	public boolean isChangeDirOnLoad() {
		return changeDirOnLoad;
	}

	public void setChangeDirOnLoad(boolean changeDirOnLoad) {
		this.changeDirOnLoad = changeDirOnLoad;
	}

	public File getSelectedFile() {
		File f = super.getSelectedFile();
		if (f == null) return f;
		if (getDialogType() == javax.swing.JFileChooser.SAVE_DIALOG && f.getName().indexOf(".") == -1) {
			//check if it's using file extension filter. If so, add the filter extension
			FileFilter ff = getFileFilter();
			if (ff instanceof FileNameExtensionFilter) {
				String ending = ((FileNameExtensionFilter)ff).getExtensions()[0];
				f = new File(f.getParent(), f.getName() + "." + ending);
			}
		}
		// now check if it's overwriting another file
		if (mWarnOnWriteOver && mAncestorNull) {
			if (f.exists()) {
				int ans = JOptionPane.showConfirmDialog(null, "" + f.getName() + " exists. Overwrite?", "Save Over Existing File", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
				if (ans == JOptionPane.OK_OPTION) 
					return f;
				return null; 
			}
		}
		return f;
	}
}
