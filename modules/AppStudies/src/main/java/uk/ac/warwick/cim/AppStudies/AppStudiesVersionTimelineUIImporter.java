/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.ac.warwick.cim.AppStudies;

import javax.swing.*;
/**
 *
 * @author iain
 */
public class AppStudiesVersionTimelineUIImporter extends JPanel {

    private JTextField pathField;
    private JButton browseButton;

    public AppStudiesVersionTimelineUIImporter() {
        pathField = new JTextField(30);
        browseButton = new JButton("Browse");

        browseButton.addActionListener(e -> chooseFile());

        add(new JLabel("Input Path:"));
        add(pathField);
        add(browseButton);
    }

    private void chooseFile() {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            pathField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    public String getPath() {
        return pathField.getText();
    }

    public void setPath(String path) {
        pathField.setText(path);
    }
}