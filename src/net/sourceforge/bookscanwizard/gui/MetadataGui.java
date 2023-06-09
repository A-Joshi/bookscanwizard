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

import java.awt.Desktop;
import java.awt.KeyboardFocusManager;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import net.sourceforge.bookscanwizard.BSW;
import net.sourceforge.bookscanwizard.PrefsHelper;
import net.sourceforge.bookscanwizard.op.Metadata;
import net.sourceforge.bookscanwizard.op.Metadata.KeyValue;
import net.sourceforge.bookscanwizard.s3.ArchiveTransfer;
import net.sourceforge.bookscanwizard.util.DropDowns;

public class MetadataGui extends javax.swing.JDialog {
    private static final String[] columnNames = {"Name", "Value"};
    private ArrayList<String[]> rows = new ArrayList<>();

    public MetadataGui(java.awt.Frame parent, boolean modal) throws Exception {
        super(parent, modal);
        setTitle("Save user preferences");
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
        jSubject = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jLabel12 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        languageCB = new javax.swing.JComboBox();
        jCheckBox1 = new javax.swing.JCheckBox();
        jPanel1 = new javax.swing.JPanel();
        jLabel13 = new javax.swing.JLabel();
        jIdentifier = new javax.swing.JTextField();
        jButton2 = new javax.swing.JButton();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jSecret = new javax.swing.JTextField();
        jAccess = new javax.swing.JTextField();
        jsaveToArchive = new javax.swing.JCheckBox();
        jLabel15 = new javax.swing.JLabel();
        jTestItem1 = new javax.swing.JCheckBox();
        jButton1 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setBackground(java.awt.Color.white);

        jLabel3.setText("<html><body>Use this form for saving metadata to PDF's,<br>or to upload books to the Internet Archive</body</html>");

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

        jLabel9.setText("Subject");

        jTable1.setModel(getTableModel());
        jTable1.setRowSelectionAllowed(false);
        jScrollPane2.setViewportView(jTable1);

        jLabel12.setText("Other Metadata to save with the book:");
        jLabel12.setToolTipText("Any sort of metadata can be saved with this book.");

        jLabel14.setText("Language");
        jLabel14.setToolTipText("The author or creator of this work");

        languageCB.setModel(new javax.swing.DefaultComboBoxModel(DropDowns.getMarcCodes()));
        System.out.println("local: "+DropDowns.getLocalizedLanguage());
        languageCB.setSelectedItem(DropDowns.getLocalizedLanguage());

        jCheckBox1.setText("Add another language");
        jCheckBox1.setToolTipText("Add another MARC code and description to the dropdown list");

        org.jdesktop.beansbinding.Binding binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, languageCB, org.jdesktop.beansbinding.ELProperty.create("${editable}"), jCheckBox1, org.jdesktop.beansbinding.BeanProperty.create("selected"));
        bindingGroup.addBinding(binding);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Internet Archive Options"));

        jLabel13.setText("Identifier");
        jLabel13.setToolTipText("");

        jIdentifier.setToolTipText("This is the identifier for the book.  If not filled in a modified title is used.");
        jIdentifier.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                jIdentifierPropertyChange(evt);
            }
        });

        jButton2.setText("Lookup Keys");
        jButton2.setToolTipText("This will launch a browser to the page where the two keys are listed");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jLabel10.setText("Default Access Key");
        jLabel10.setToolTipText("This is the access key that is used to save to the Internet Archive");

        jLabel11.setText("Defaul tSecret Key");
        jLabel11.setToolTipText("This is the secret key to save to the internet archive");

        jSecret.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jSecretActionPerformed(evt);
            }
        });

        jsaveToArchive.setText("Save this item to the Internet Archive");
        jsaveToArchive.setToolTipText("This will create an archive.zip file when this book is processed that can be uploaded to archive.org.");
        jsaveToArchive.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jsaveToArchiveActionPerformed(evt);
            }
        });

        jLabel15.setIcon(new javax.swing.ImageIcon(getClass().getResource("/archive_logo.jpg"))); // NOI18N
        jLabel15.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel15MouseClicked(evt);
            }
        });

        jTestItem1.setText("Test Item (Remove after 30 days)");
        jTestItem1.setToolTipText("If checked, this item will not be indexed and will be deleted after 30 days.");
        jTestItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTestItem1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel11)
                            .addComponent(jLabel13)
                            .addComponent(jLabel10))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jSecret)
                            .addComponent(jAccess, javax.swing.GroupLayout.PREFERRED_SIZE, 161, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jIdentifier, javax.swing.GroupLayout.PREFERRED_SIZE, 161, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel15))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addComponent(jButton2)
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jTestItem1)
                            .addComponent(jsaveToArchive))))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel15)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jIdentifier, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel13))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jAccess, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel10))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jSecret, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel11))))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jsaveToArchive))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTestItem1)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jButton1.setText("Ok");
        jButton1.setToolTipText("Saves this information and returns");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton3.setText("Cancel");
        jButton3.setToolTipText("Exits this dialog without saving");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(99, 99, 99)
                        .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 375, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 18, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel7)
                            .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTitle)))
                .addGap(26, 26, 26))
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
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
            .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButton1)
                .addGap(28, 28, 28)
                .addComponent(jButton3)
                .addGap(31, 31, 31))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
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
                .addGap(18, 18, 18)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton3)
                    .addComponent(jButton1))
                .addContainerGap(20, Short.MAX_VALUE))
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

    private void jAuthorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jAuthorActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jAuthorActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        dispose();
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jLabel15MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel15MouseClicked
        try {
            Desktop.getDesktop().browse(new URI("http://www.archive.org"));
        } catch (URISyntaxException | IOException ex) {
            UserFeedbackHelper.displayException(this, ex);
        }
    }//GEN-LAST:event_jLabel15MouseClicked

    private void jsaveToArchiveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jsaveToArchiveActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jsaveToArchiveActionPerformed

    private void jSecretActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jSecretActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jSecretActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        try {
            Desktop.getDesktop().browse(new URI("http://www.archive.org/account/s3.php"));
        } catch (URISyntaxException | IOException ex) {
            UserFeedbackHelper.displayException(this, ex);
        }
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jIdentifierPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_jIdentifierPropertyChange
        // TODO add your handling code here:
    }//GEN-LAST:event_jIdentifierPropertyChange

    private void jTestItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTestItem1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTestItem1ActionPerformed

    private TableModel getTableModel() {
        TableModel tableModel = new AbstractTableModel() {
            @Override
            public String getColumnName(int col) {
                return columnNames[col];
            }
            @Override
            public int getRowCount() { return rows.size() +1; }

            @Override
            public int getColumnCount() { return columnNames.length; }

            @Override
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
            @Override
            public void run() {
                MetadataGui dialog;
                try {
                    dialog = new MetadataGui(new javax.swing.JFrame(), true);
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
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTextField jSecret;
    private javax.swing.JTextField jSubject;
    private javax.swing.JTable jTable1;
    private javax.swing.JCheckBox jTestItem1;
    private javax.swing.JTextField jTitle;
    private javax.swing.JCheckBox jsaveToArchive;
    private javax.swing.JComboBox languageCB;
    private org.jdesktop.beansbinding.BindingGroup bindingGroup;
    // End of variables declaration//GEN-END:variables

    /**
     * Updates this gui with the configured metadata.
     */
    private void updateGui() throws Exception {
        List<Metadata.KeyValue> data = Metadata.getMetaData();
        ArrayList<Metadata.KeyValue> customData = new ArrayList<>(data);
        Collections.sort(customData);
        String user = (String) PrefsHelper.getPref("archive_user");
        if (user != null) {
            jAccess.setText(user);
            jSecret.setText((String) PrefsHelper.getPref("archive_pass"));
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
        jsaveToArchive.setSelected("true".equalsIgnoreCase(getAndRemove(customData, "noindex")));
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
    private void saveToConfig() throws Exception {
        try {
            ArchiveTransfer.validateId(jIdentifier.getText());
            ArrayList<String[]> data = new ArrayList<>();
            data.add(new String[]{"identifier", jIdentifier.getText()});
            data.add(new String[]{"title", jTitle.getText()});
            data.add(new String[]{"creator", jAuthor.getText()});
            data.add(new String[]{"date", jDate.getText()});
            data.add(new String[]{"language", languageCB.getSelectedItem().toString()});
            data.add(new String[]{"subject", jSubject.getText()});
            data.add(new String[]{"description", jDescription.getText()});
            data.add(new String[]{"keywords", jKeywords.getText()});
            if (jsaveToArchive.isSelected()) {
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
            int start = entry.getSelectionStart();

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
            BSW.instance().getMenuHandler().insertConfigNoPreview(str.toString(), false, false);
            if (oldPos < 0) {
                oldPos = entry.getText().length();
            }
            str.append("\n");
            PrefsHelper.setPref("archive_user", jAccess.getText());
            PrefsHelper.setPref("archive_pass", jSecret.getText());
            doc.insertString(oldPos, str.toString(), null);
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }
}
