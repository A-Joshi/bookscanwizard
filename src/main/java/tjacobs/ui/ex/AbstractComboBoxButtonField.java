package tjacobs.ui.ex;

import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.AbstractButton;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;

import tjacobs.ui.util.WindowTiler;
import tjacobs.ui.util.WindowUtilities;

public abstract class AbstractComboBoxButtonField extends JComponent {
	/* Default serial UID */
	private static final long serialVersionUID = 1L;

	private class ClickListener implements ActionListener {
		public void actionPerformed(ActionEvent ae) {
			buttonClick();
		}
	}
	
	private class EnterListener implements ActionListener {
		public void actionPerformed(ActionEvent ae) {
			Object selected  =getComboBox().getSelectedItem();
			if (selected == null) return;
			ComboBoxModel model = getComboBox().getModel();
			for (int i = 0; i < model.getSize(); i++) {
				if (model.getElementAt(i).equals(selected)) return;
			}
			//getComboBox().removeItem(selected);
			getComboBox().insertItemAt(selected,0);
			getComboBox().setSelectedItem(selected);
		}
	}
		
	public abstract JComboBox getComboBox();
	private JButton mButton;
	
	public abstract String getButtonText();

	public abstract void buttonClick();
	
	public AbstractComboBoxButtonField() {
		setLayout(new BorderLayout());
		add(getComboBox(), BorderLayout.CENTER);
		mButton = new JButton(getButtonText());
		add(mButton, BorderLayout.EAST);
		mButton.addActionListener(new ClickListener());
		getComboBox().addActionListener(new EnterListener());
	}
	
	public static void main(String[] args) {
		final AbstractComboBoxButtonField field = new AbstractComboBoxButtonField() {
			JComboBox combo;
			@Override
			public void buttonClick() {
				JFileChooser chooser = new JFileChooser();
				if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
					JComboBox c = getComboBox();
					DefaultComboBoxModel model = (DefaultComboBoxModel)c.getModel();
					File selected = chooser.getSelectedFile();
					String name = selected.getAbsolutePath();
					c.removeItem(name);
					c.insertItemAt(name,0);
					c.setSelectedItem(name);
				}
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
			
		};
		Window w = WindowUtilities.visualize(field);
		WindowTiler.setExitWhenAllWindowsClosed(false);
		w.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
				System.out.println("File: " + field.getComboBox().getSelectedItem());
				System.exit(0);
			}
		});
	}
}
