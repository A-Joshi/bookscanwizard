package tjacobs.ui.ex;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.ComboBoxModel;
import javax.swing.JButton;

import tjacobs.io.App;
import tjacobs.ui.swing_ex.JComboBox;
import tjacobs.ui.swing_ex.JFileChooser;
import tjacobs.ui.util.WindowUtilities;




  public class FileField extends AbstractComboBoxButtonField{

	private static final long serialVersionUID = 1L;
	private JComboBox combo;
	  public FileField() {}
	  
	  public FileField(String file) {
		  setText(file);
	  }
	  
	public static void main(String[] args) {
		new App("FileField", null);
		final FileField ff = new FileField("foo");
		WindowUtilities.visualize(ff);
		JButton jb = new JButton("Print File");
		jb.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				System.out.println("File: " + ff.getFile());
			}
		});
		WindowUtilities.visualize(jb);
	}

	public void setText(String text) {
		boolean includes = false;
		ComboBoxModel model = getComboBox().getModel();
		for (int i = 0; i < model.getSize(); i++) {
			if (model.getElementAt(i).equals(text)) {
				includes = true;
				break;
			}
		}
		if (!includes) getComboBox().insertItemAt(text, 0);
		getComboBox().setSelectedItem(text);
	}
	
	public String getText() {
		return (String) getComboBox().getSelectedItem();
	}
		
		
		@Override
	public String getButtonText() {
		return "...";
	}

	@Override
	public JComboBox getComboBox() {
		if (combo == null) {
			combo = new JComboBox();
			combo.setEditable(true);
		}
		return combo;
	}

	public File getFile() {
		 String str = (String)getComboBox().getSelectedItem();
		 if (str == null) return null;
		 return new File(str);
	}
	
	public void buttonClick() {
		JFileChooser chooser = new JFileChooser();
		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			JComboBox c = getComboBox();
			File selected = chooser.getSelectedFile();
			String name = selected.getAbsolutePath();
			c.removeItem(name);
			c.insertItemAt(name, 0);
			c.setSelectedItem(name);
		}
	}
}
