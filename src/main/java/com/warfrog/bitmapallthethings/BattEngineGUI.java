/*
*
* BitTwiddler - BMP transcoder
* Copyright (C) 2015  Tyler Pitchford
*
* This file is part of BitTwiddler.
*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; see the file COPYING.  If not, write to
* the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
*
*/

package com.warfrog.bitmapallthethings;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class BattEngineGUI {
    private JButton decodeButton;
    private JButton encodeButton;
    private JTextField outputDirectoryTextField;
    private JTextField inputTargetTextField;
    private JTextField archiveNameTextField;
    private JSpinner compressionLevelSpinner;
    private JSpinner recoveryRecordSpinner;
    private JCheckBox enableRarCheckBox;
    private JSpinner bytesPerPixelSpinner;
    private JSpinner widthSpinner;
    private JSpinner heightSpinner;
    private JTextField extensionFilterTextField;
    private JButton inputTargetButton;
    private JButton outputDirectoryButton;
    private JPanel buttonPanel;
    private JScrollPane consoleScroll;
    private JTextArea consoleOutput;
    private JPanel mainPanel;
    private JPanel rarPanel;
    private JLabel archiveLabel;
    private JLabel compressionLabel;
    private JLabel recoverRecordLabel;
    private JCheckBox cleanUpTemporaryFilesCheckBox;
    private JTextField fileSizeTextField;
    private JTextField rarLocationTextField;
    private JButton rarLocationButton;
    private JPasswordField rarPasswordPasswordField;

    private File openFileBrowser(int selectionMode) {
        File returnValue = null;
        //setup the button actions
        JFileChooser input = new JFileChooser();
        input.setFileSelectionMode(selectionMode);
        int returnVal = input.showOpenDialog(null);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            returnValue = input.getSelectedFile();
        }

        return returnValue;
    }

    private String[] buildTwiddlerCommands(String action) {
        //build the proper command string
        List<String> command = new ArrayList<String>();
        command.add("-s");
        command.add("-a");
        command.add(action);
        command.add("-i");
        command.add(inputTargetTextField.getText());
        command.add("-o");
        command.add(outputDirectoryTextField.getText());
        command.add("-w");
        command.add(String.valueOf((Integer) widthSpinner.getValue()));
        command.add("-h");
        command.add(String.valueOf((Integer) heightSpinner.getValue()));
        command.add("-m");
        command.add(String.valueOf(calculateFileSize()));
        command.add("-b");
        command.add(String.valueOf((Integer) bytesPerPixelSpinner.getValue()));
        command.add("-e");
        command.add(extensionFilterTextField.getText());

        if (cleanUpTemporaryFilesCheckBox.isSelected()) {
            command.add("-c");
        }

        if (enableRarCheckBox.isSelected()) {
            command.add("-r");
            command.add("-rx");
            command.add(String.valueOf((Integer) compressionLevelSpinner.getValue()));
            command.add("-rr");
            command.add(String.valueOf((Integer) recoveryRecordSpinner.getValue()));
            if (archiveNameTextField != null && !archiveNameTextField.getText().trim().isEmpty()) {
                command.add("-rn");
                command.add(archiveNameTextField.getText());
            }
            if (rarPasswordPasswordField != null && rarPasswordPasswordField.getPassword().length > 0) {
                command.add("-rp");
                command.add(new String(rarPasswordPasswordField.getPassword()));
            }
        }

        return command.toArray(new String[command.size()]);
    }

    private Integer calculateFileSize() {
        return ((Integer) widthSpinner.getValue()) * ((Integer) heightSpinner.getValue())
                * ((Integer) bytesPerPixelSpinner.getValue() / 8);
    }

    private void updateFileSize() {
        int calculatedFileSize = calculateFileSize();
        DecimalFormat formatter = new DecimalFormat("###,###");
        fileSizeTextField.setText(formatter.format(calculatedFileSize));
    }

    private PipedInputStream redirectStandardOut() throws Exception {
        //redirect the standard output
        PipedInputStream outPipe = new PipedInputStream();
        System.setOut(new PrintStream(new PipedOutputStream(outPipe), true));
        return outPipe;
    }

    private PipedInputStream redirectStandardErr() throws Exception {
        //redirect the standard err
        PipedInputStream errPipe = new PipedInputStream();
        System.setErr(new PrintStream(new PipedOutputStream(errPipe), true));
        return errPipe;
    }

    private void setupOutputConsole() throws Exception {
        //truncating document
        final int consoleBufferLimit = 1024 * 1024 * 2; //2MBs
        consoleOutput.setDocument(new PlainDocument() {
            @Override
            public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
                if (str == null)
                    return;
                super.insertString(offs, str, a);
                int length = getLength();
                if (length > consoleBufferLimit)
                    remove(0, length - consoleBufferLimit);
            }
        });

        enableAutoScrolling();

        //redirect System.out and System.err to the console
        final PipedInputStream outPipe = redirectStandardOut();
        final PipedInputStream errPipe = redirectStandardErr();

        // handle "System.out"
        new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                Scanner s = new Scanner(outPipe);
                while (s.hasNextLine()) publish(s.nextLine() + "\n");
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String line : chunks) consoleOutput.append(line);
            }
        }.execute();

        // handle "System.err"
        new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                Scanner s = new Scanner(errPipe);
                while (s.hasNextLine()) publish(s.nextLine() + "\n");
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String line : chunks) consoleOutput.append(line);
            }
        }.execute();

    }

    private void enableDragAndDropTargets() {
        //set up drag and drop on the input and output fields
        DropTarget inputDropTarget = new DropTarget(inputTargetTextField, new FileDropTarget(inputTargetTextField));
        DropTarget outputDirectoryDropTarget = new DropTarget(outputDirectoryTextField, new FileDropTarget(outputDirectoryTextField));
    }

    private void enableAutoScrolling() {
        //setup autoscrolling
        DefaultCaret caret = (DefaultCaret) consoleOutput.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
    }

    private void setupDefaultValues() {
        //fill in the defaults
        outputDirectoryTextField.setText(System.getProperty("user.dir"));
        extensionFilterTextField.setText("*.*");
        cleanUpTemporaryFilesCheckBox.setSelected(false);
    }

    private void checkRarStatus() {
        boolean rarAvailable = false;
        if (rarLocationTextField.getText().trim().isEmpty()) {
            rarAvailable = RarUtility.isRarAvailable();
        } else {
            rarAvailable = RarUtility.isRarAvailable(rarLocationTextField.getText());
        }

        //these should all be the same as rarAvailable
        enableRarCheckBox.setSelected(rarAvailable);
        rarPanel.setEnabled(rarAvailable);
        enableRarCheckBox.setEnabled(rarAvailable);
        compressionLabel.setEnabled(rarAvailable);
        compressionLevelSpinner.setEnabled(rarAvailable);
        recoverRecordLabel.setEnabled(rarAvailable);
        recoveryRecordSpinner.setEnabled(rarAvailable);
        archiveLabel.setEnabled(rarAvailable);
        archiveNameTextField.setEnabled(rarAvailable);

        if (!rarAvailable) {
            //if no rar, disable all the rar options
            System.err.println("Rar was not found in the current system path\n" +
                    "if rar is installed, please provide a location\n" +
                    "otherwise download rar from http://www.win-rar.com/download.html to enable these features.");
        }
    }

    private void setupInputTargetButton() {
        inputTargetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                File result = openFileBrowser(JFileChooser.FILES_AND_DIRECTORIES);
                if (result != null) {
                    inputTargetTextField.setText(result.getAbsolutePath());
                }
            }
        });
    }

    private void setupOutputDirectoryButton() {
        outputDirectoryButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                File result = openFileBrowser(JFileChooser.DIRECTORIES_ONLY);
                if (result != null) {
                    outputDirectoryButton.setText(result.getAbsolutePath());
                }
            }
        });
    }

    private void setupRarLocationButton() {
        rarLocationButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                File result = openFileBrowser(JFileChooser.DIRECTORIES_ONLY);
                if (result != null) {
                    rarLocationTextField.setText(result.getAbsolutePath());
                }
            }
        });
    }

    private void setupEncodeButton() {
        encodeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        try {
                            new BattEngine().start(buildTwiddlerCommands("encode"));
                        } catch (Exception ex) {
                            System.err.println(ex.getMessage());
                            System.err.println("");
                        }
                        return null;
                    }

                    @Override
                    protected void process(List<String> chunks) {
                        super.process(chunks);
                    }
                };
                worker.execute();
            }
        });
    }

    private void setupDecodeButton() {
        decodeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        try {
                            new BattEngine().start(buildTwiddlerCommands("decode"));
                        } catch (Exception ex) {
                            System.err.println(ex.getMessage());
                            System.err.println("");
                        }
                        return null;
                    }

                    @Override
                    protected void process(List<String> chunks) {
                        super.process(chunks);
                    }
                };
                worker.execute();
            }
        });
    }

    private void setupRarLocationField() {
        rarLocationTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                //see if we've found rar
                checkRarStatus();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                checkRarStatus();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                checkRarStatus();
            }
        });
    }

    private void setupWidthSpinner() {
        widthSpinner.setModel(new SpinnerNumberModel(4000, 1, 32000, 1));
        widthSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                //update the calculated value for file size
                updateFileSize();
            }
        });
    }

    private void setupHeightSpinner() {
        heightSpinner.setModel(new SpinnerNumberModel(4000, 1, 32000, 1));
        heightSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                //update the calculated value for file size
                updateFileSize();
            }
        });
    }

    private void setupBytesPerPixelSpinner() {
        bytesPerPixelSpinner.setModel(new SpinnerNumberModel(32, 8, 32, 8));
        bytesPerPixelSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                //update the calculated value for file size
                updateFileSize();
            }
        });
    }

    private void setupCompressionSpinner() {
        compressionLevelSpinner.setModel(new SpinnerNumberModel(0, 0, 5, 1));
    }

    private void setupRecoveryRecordSpinner() {
        recoveryRecordSpinner.setModel(new SpinnerNumberModel(10, 0, 100, 1));
    }

    public BattEngineGUI() throws Exception {
        //components
        setupDefaultValues();
        setupInputTargetButton();
        setupOutputDirectoryButton();
        setupRarLocationButton();
        setupEncodeButton();
        setupDecodeButton();
        setupWidthSpinner();
        setupHeightSpinner();
        setupBytesPerPixelSpinner();
        setupCompressionSpinner();
        setupRecoveryRecordSpinner();
        setupRarLocationField();
        //console
        setupOutputConsole();
        //input / output targets
        enableDragAndDropTargets();
        //scan for rar
        checkRarStatus();
        //file size
        updateFileSize();
    }

    public static void launch() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    final JFrame frame = new JFrame("B.A.T.T. - Bitmap All The Things");
                    frame.setContentPane(new BattEngineGUI().mainPanel);
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    frame.pack();
                    frame.setLocationRelativeTo(null);
                    frame.setVisible(true);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(5, 1, new Insets(0, 0, 0, 0), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(4, 3, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel1.setBorder(BorderFactory.createTitledBorder("Options"));
        final JLabel label1 = new JLabel();
        label1.setText("Input Target:");
        panel1.add(label1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        inputTargetTextField = new JTextField();
        panel1.add(inputTargetTextField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Output Directory:");
        panel1.add(label2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        outputDirectoryTextField = new JTextField();
        outputDirectoryTextField.setText("");
        panel1.add(outputDirectoryTextField, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        outputDirectoryButton = new JButton();
        outputDirectoryButton.setText("...");
        panel1.add(outputDirectoryButton, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Extension Filter:");
        panel1.add(label3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        extensionFilterTextField = new JTextField();
        extensionFilterTextField.setText("");
        panel1.add(extensionFilterTextField, new GridConstraints(0, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        inputTargetButton = new JButton();
        inputTargetButton.setText("...");
        panel1.add(inputTargetButton, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cleanUpTemporaryFilesCheckBox = new JCheckBox();
        cleanUpTemporaryFilesCheckBox.setText("Clean up temporary files");
        panel1.add(cleanUpTemporaryFilesCheckBox, new GridConstraints(3, 0, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(2, 6, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel2.setBorder(BorderFactory.createTitledBorder("Bitmap Options"));
        final JLabel label4 = new JLabel();
        label4.setText("File Size");
        panel2.add(label4, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Width");
        panel2.add(label5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        widthSpinner = new JSpinner();
        panel2.add(widthSpinner, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("Height");
        panel2.add(label6, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        heightSpinner = new JSpinner();
        panel2.add(heightSpinner, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("Bytes Per Pixel");
        panel2.add(label7, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        bytesPerPixelSpinner = new JSpinner();
        panel2.add(bytesPerPixelSpinner, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        fileSizeTextField = new JTextField();
        fileSizeTextField.setEditable(false);
        panel2.add(fileSizeTextField, new GridConstraints(1, 1, 1, 5, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        rarPanel = new JPanel();
        rarPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(rarPanel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        rarPanel.setBorder(BorderFactory.createTitledBorder("Rar Options"));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(5, 5, new Insets(0, 0, 0, 0), -1, -1));
        rarPanel.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        compressionLevelSpinner = new JSpinner();
        panel3.add(compressionLevelSpinner, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        compressionLabel = new JLabel();
        compressionLabel.setText("Compression Level:");
        panel3.add(compressionLabel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        recoverRecordLabel = new JLabel();
        recoverRecordLabel.setText("Recovery Record:");
        panel3.add(recoverRecordLabel, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        recoveryRecordSpinner = new JSpinner();
        panel3.add(recoveryRecordSpinner, new GridConstraints(2, 3, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label8 = new JLabel();
        label8.setText("Rar Location:");
        panel3.add(label8, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        rarLocationButton = new JButton();
        rarLocationButton.setText("...");
        panel3.add(rarLocationButton, new GridConstraints(4, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        archiveNameTextField = new JTextField();
        panel3.add(archiveNameTextField, new GridConstraints(1, 1, 1, 4, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        archiveLabel = new JLabel();
        archiveLabel.setText("Archive Name:");
        panel3.add(archiveLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        enableRarCheckBox = new JCheckBox();
        enableRarCheckBox.setText("Enable Rar");
        panel3.add(enableRarCheckBox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        rarLocationTextField = new JTextField();
        panel3.add(rarLocationTextField, new GridConstraints(4, 1, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label9 = new JLabel();
        label9.setText("Rar password:");
        panel3.add(label9, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        rarPasswordPasswordField = new JPasswordField();
        panel3.add(rarPasswordPasswordField, new GridConstraints(3, 1, 1, 4, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(buttonPanel, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        encodeButton = new JButton();
        encodeButton.setText("Encode");
        buttonPanel.add(encodeButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        decodeButton = new JButton();
        decodeButton.setText("Decode");
        buttonPanel.add(decodeButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        consoleScroll = new JScrollPane();
        consoleScroll.setVerticalScrollBarPolicy(22);
        mainPanel.add(consoleScroll, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(600, 300), new Dimension(600, 300), null, 0, false));
        consoleOutput = new JTextArea();
        consoleOutput.setBackground(new Color(-16777216));
        consoleOutput.setForeground(new Color(-16719581));
        consoleOutput.setLineWrap(true);
        consoleOutput.setWrapStyleWord(true);
        consoleScroll.setViewportView(consoleOutput);
        label1.setLabelFor(inputTargetTextField);
        label2.setLabelFor(outputDirectoryTextField);
        label3.setLabelFor(extensionFilterTextField);
        label5.setLabelFor(widthSpinner);
        label6.setLabelFor(heightSpinner);
        label7.setLabelFor(bytesPerPixelSpinner);
        compressionLabel.setLabelFor(compressionLevelSpinner);
        recoverRecordLabel.setLabelFor(recoveryRecordSpinner);
        label8.setLabelFor(rarLocationTextField);
        archiveLabel.setLabelFor(archiveNameTextField);
        label9.setLabelFor(rarPasswordPasswordField);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }
}
