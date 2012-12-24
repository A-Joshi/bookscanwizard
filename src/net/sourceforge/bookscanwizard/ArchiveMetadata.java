/*
 *
 * Copyright (c) 2011 by Steve Devore
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

import java.awt.Desktop;
import java.awt.KeyboardFocusManager;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import net.sourceforge.bookscanwizard.op.Metadata;
import net.sourceforge.bookscanwizard.op.Metadata.KeyValue;
import net.sourceforge.bookscanwizard.op.SaveToArchive;
import net.sourceforge.bookscanwizard.s3.ArchiveTransfer;
import net.sourceforge.bookscanwizard.util.DropDowns;

public class ArchiveMetadata extends javax.swing.JDialog {
    private static final String[] columnNames = {"Name", "Value"};
    private ArrayList<String[]> rows = new ArrayList<String[]>();

    public ArchiveMetadata(java.awt.Frame parent, boolean modal) throws Exception {
        super(parent, modal);
        initComponents();
        updateGui();
        jDescription.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
        jDescription.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        bindingGroup = new org.jdesktop.beansbinding.BindingGroup();

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jTitle = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        jDescription = new javax.swing.JTextArea();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jKeywords = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        jAuthor = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        jDate = new javax.swing.JTextField();
        jTestItem = new javax.swing.JCheckBox();
        jSubject = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jLabel10 = new javax.swing.JLabel();
        jAccess = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jLabel12 = new javax.swing.JLabel();
        jButton3 = new javax.swing.JButton();
        jSecret = new javax.swing.JTextField();
        jButton2 = new javax.swing.JButton();
        jLabel13 = new javax.swing.JLabel();
        jIdentifier = new javax.swing.JTextField();
        jLabel14 = new javax.swing.JLabel();
        languageCB = new javax.swing.JComboBox();
        jCheckBox1 = new javax.swing.JCheckBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setBackground(java.awt.Color.white);

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel1.setText("Upload this book to the Internet Archive");

        jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/archive_logo.jpg"))); // NOI18N
        jLabel2.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel2MouseClicked(evt);
            }
        });

        jLabel3.setText("<html><body>Use this to upload books to the Internet Archive that you have the right to share</body</html>");

        jLabel4.setText("Title");

        jDescription.setColumns(20);
        jDescription.setFont(new java.awt.Font("Tahoma", 0, 13)); // NOI18N
        jDescription.setRows(5);
        jScrollPane1.setViewportView(jDescription);

        jLabel5.setText("Description");

        jLabel6.setText("<html><body>Keywords (seperated by commas)</body></html>");

        jKeywords.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jKeywordsActionPerformed(evt);
            }
        });

        jLabel7.setText("Author");
        jLabel7.setToolTipText("The author or creator of this work");

        jAuthor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jAuthorActionPerformed(evt);
            }
        });

        jLabel8.setText("Date or Year");
        jLabel8.setToolTipText("The date or year this was published");

        jTestItem.setText("Test Item (Remove after 30 days)");
        jTestItem.setToolTipText("If checked, this item will not be indexed and will be deleted after 30 days.");
        jTestItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTestItemActionPerformed(evt);
            }
        });

        jLabel9.setText("Subject");

        jButton1.setText("Ok");
        jButton1.setToolTipText("Saves this information and returns");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jLabel10.setText("Access Key");
        jLabel10.setToolTipText("This is the access key that is used to save to the Internet Archive");

        jLabel11.setText("Secret Key");
        jLabel11.setToolTipText("This is the secret key to save to the internet archive");

        jTable1.setModel(getTableModel());
        jTable1.setRowSelectionAllowed(false);
        jScrollPane2.setViewportView(jTable1);

        jLabel12.setText("Other Metadata to save with the book:");
        jLabel12.setToolTipText("Any sort of metadata can be saved with this book.");

        jButton3.setText("Cancel");
        jButton3.setToolTipText("Exits this dialog without saving");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jSecret.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jSecretActionPerformed(evt);
            }
        });

        jButton2.setText("Lookup Keys");
        jButton2.setToolTipText("This will launch a browser to the page where the two keys are listed");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jLabel13.setText("Identifier");
        jLabel13.setToolTipText("");

        jIdentifier.setToolTipText("This is the identifier for the book.  If not filled in a modified title is used.");
        jIdentifier.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                jIdentifierPropertyChange(evt);
            }
        });

        jLabel14.setText("Language");
        jLabel14.setToolTipText("The author or creator of this work");

        languageCB.setModel(new javax.swing.DefaultComboBoxModel(DropDowns.getMarcCodes()));
        System.out.println("local: "+DropDowns.getLocalizedLanguage());
        languageCB.setSelectedItem(DropDowns.getLocalizedLanguage());

        jCheckBox1.setText("Add another language");
        jCheckBox1.setToolTipText("Add another MARC code and description to the dropdown list");

        org.jdesktop.beansbinding.Binding binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, languageCB, org.jdesktop.beansbinding.ELProperty.create("${editable}"), jCheckBox1, org.jdesktop.beansbinding.BeanProperty.create("selected"));
        bindingGroup.addBinding(binding);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(99, 99, 99)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 383, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel1))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel2))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel7)
                            .addComponent(jLabel13)
                            .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(0, 0, 0)
                                .addComponent(jIdentifier, javax.swing.GroupLayout.PREFERRED_SIZE, 181, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addComponent(jTitle))))
                .addGap(26, 26, 26))
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(jLabel11)
                                        .addGap(18, 18, 18)
                                        .addComponent(jSecret))
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(jLabel10)
                                        .addGap(18, 18, 18)
                                        .addComponent(jAccess, javax.swing.GroupLayout.PREFERRED_SIZE, 155, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 122, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jButton2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jTestItem)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jButton1)
                                .addGap(18, 18, 18)
                                .addComponent(jButton3)))
                        .addGap(16, 16, 16))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jLabel12, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, 80, Short.MAX_VALUE)
                                .addComponent(jLabel5))
                            .addComponent(jLabel8)
                            .addComponent(jLabel9)
                            .addComponent(jLabel14))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jSubject, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jKeywords)
                            .addComponent(jScrollPane1)
                            .addComponent(jAuthor)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jDate, javax.swing.GroupLayout.PREFERRED_SIZE, 263, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(languageCB, javax.swing.GroupLayout.PREFERRED_SIZE, 175, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(jCheckBox1)))
                                .addGap(0, 0, Short.MAX_VALUE)))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(29, 29, 29)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jIdentifier, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel13))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTitle, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jAuthor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel7))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel14)
                    .addComponent(languageCB, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jCheckBox1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jDate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jSubject, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel9))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jKeywords, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addGap(44, 44, 44)
                        .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(18, 18, 18)
                .addComponent(jLabel12)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(17, 17, 17)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jButton2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel10)
                            .addComponent(jAccess, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel11)
                            .addComponent(jSecret, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jTestItem)
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jButton3)
                            .addComponent(jButton1))))
                .addGap(23, 23, 23))
        );

        bindingGroup.bind();

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        try {
            saveToConfig();
            dispose();
        } catch (Exception e) {
            UserFeedbackHelper.displayException(this, e);
        }
}//GEN-LAST:event_jButton1ActionPerformed

    private void jKeywordsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jKeywordsActionPerformed
        // TODO add your handling code here:
}//GEN-LAST:event_jKeywordsActionPerformed

    private void jTestItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTestItemActionPerformed
        // TODO add your handling code here:
}//GEN-LAST:event_jTestItemActionPerformed

    private void jSecretActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jSecretActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jSecretActionPerformed

    private void jLabel2MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel2MouseClicked
        try {
            Desktop.getDesktop().browse(new URI("http://www.archive.org"));
        } catch (Exception ex) {
            UserFeedbackHelper.displayException(this, ex);
        }
    }//GEN-LAST:event_jLabel2MouseClicked

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        try {
            Desktop.getDesktop().browse(new URI("http://www.archive.org/account/s3.php"));
        } catch (Exception ex) {
            UserFeedbackHelper.displayException(this, ex);
        }
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jAuthorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jAuthorActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jAuthorActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        dispose();
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jIdentifierPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_jIdentifierPropertyChange
        // TODO add your handling code here:
    }//GEN-LAST:event_jIdentifierPropertyChange

    private TableModel getTableModel() {
        TableModel tableModel = new AbstractTableModel() {
            @Override
            public String getColumnName(int col) {
                return columnNames[col];
            }
            public int getRowCount() { return rows.size() +1; }

            public int getColumnCount() { return columnNames.length; }

            public Object getValueAt(int row, int col) {
                if (row >= rows.size()) {
                    return "";
                } else {
                    return rows.get(row)[col];
                }
            }

            @Override
            public boolean isCellEditable(int row, int col) { return true; }

            @Override
            public void setValueAt(Object value, int row, int col) {
                if (row >= rows.size()) {
                    rows.add(new String[2]);
                }
                rows.get(row)[col] = value.toString();
                fireTableCellUpdated(row, col);
            }
        };
        return tableModel;
    }

    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                ArchiveMetadata dialog;
                try {
                    dialog = new ArchiveMetadata(new javax.swing.JFrame(), true);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField jAccess;
    private javax.swing.JTextField jAuthor;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JTextField jDate;
    private javax.swing.JTextArea jDescription;
    private javax.swing.JTextField jIdentifier;
    private javax.swing.JTextField jKeywords;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTextField jSecret;
    private javax.swing.JTextField jSubject;
    private javax.swing.JTable jTable1;
    private javax.swing.JCheckBox jTestItem;
    private javax.swing.JTextField jTitle;
    private javax.swing.JComboBox languageCB;
    private org.jdesktop.beansbinding.BindingGroup bindingGroup;
    // End of variables declaration//GEN-END:variables

    /**
     * Updates this gui with the configured metadata.
     */
    private void updateGui() throws Exception {
        List<Metadata.KeyValue> data = Metadata.getMetaData();
        ArrayList<Metadata.KeyValue> customData = new ArrayList<Metadata.KeyValue>(data);
        Collections.sort(customData);
        if (SaveToArchive.getLastSave() != null) {
            jAccess.setText(SaveToArchive.getLastSave().getAccess());
            jSecret.setText(SaveToArchive.getLastSave().getSecret());
        }
        jIdentifier.setText(getAndRemove(customData, "identifier"));
        jTitle.setText(getAndRemove(customData, "title"));
        jAuthor.setText(getAndRemove(customData, "creator"));
        jDate.setText(getAndRemove(customData, "date"));
        jSubject.setText(getAndRemove(customData, "subject"));
        String language = getAndRemove(customData, "language");
        if (language == null || language.isEmpty()) {
            language = DropDowns.getLocalizedLanguage();
        }
        languageCB.setSelectedItem(language);
        jDescription.setText(getAndRemove(customData, "description"));
        jKeywords.setText(getAndRemove(customData, "keywords"));
        jTestItem.setSelected("true".equalsIgnoreCase(getAndRemove(customData, "noindex")));
        rows.clear();
        for (KeyValue entry : customData) {
            rows.add(new String[] {entry.getKey(), entry.getValue()});
        }
    }

    private String getAndRemove(List<KeyValue> list, String key) {
        int pos = list.indexOf(new KeyValue(key));
        String retVal = null;
        if (pos >= 0) {
            retVal = list.remove(pos).getValue();
        }
        return retVal;
    }

    /**
     * Saves this configuration
     */
    private void saveToConfig() {
        try {
            ArchiveTransfer.validateId(jIdentifier.getText());
            ArrayList<String[]> data = new ArrayList<String[]>();
            data.add(new String[]{"identifier", jIdentifier.getText()});
            data.add(new String[]{"title", jTitle.getText()});
            data.add(new String[]{"creator", jAuthor.getText()});
            data.add(new String[]{"date", jDate.getText()});
            data.add(new String[]{"language", languageCB.getSelectedItem().toString()});
            data.add(new String[]{"subject", jSubject.getText()});
            data.add(new String[]{"description", jDescription.getText()});
            data.add(new String[]{"keywords", jKeywords.getText()});
            if (jTestItem.isSelected()) {
                data.add(new String[]{"noindex", "true"});
            }
            for (String[] row : rows) {
                if (row[0] != null && row[0].length() > 0) {
                    data.add(row);
                }
            }
            StringBuilder str = new StringBuilder();
            for (String[] row: data) {
                str.append("\nMetadata = ").append(row[0]).append(": ").append(row[1]);
            }
            ConfigEntry entry = BSW.instance().getMainFrame().getConfigEntry();
            Document doc = entry.getDocument();
            int oldPos = -1;
            while (true) {
                String text = entry.getText();
                int pos = text.indexOf("\nMetadata");
                if (pos < 0) {
                    break;
                }
                if (oldPos < 0) {
                    oldPos = pos;
                }
                int endLine = text.indexOf("\n", pos+1);
                if (endLine < 0) {
                    endLine = text.length();
                }
                doc.remove(pos, endLine-pos);
            }
            if (oldPos < 0) {
                oldPos = entry.getText().length();
            }
            str.append("\n");
            if (entry.getText().indexOf("CreateArchiveZip") < 0) {
                str.append(
                        "CreateArchiveZip = archive.zip 10:1\n"
                );
            }
            if (entry.getText().indexOf("SaveToArchive") < 0) {
                str.append("# Uncomment the following line to send to the archive as part of this job.\n"+
                           "#SaveToArchive = archive.zip "+jAccess.getText()+" "+jSecret.getText()+"\n");
            }
            doc.insertString(oldPos, str.toString(), null);
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }
}
