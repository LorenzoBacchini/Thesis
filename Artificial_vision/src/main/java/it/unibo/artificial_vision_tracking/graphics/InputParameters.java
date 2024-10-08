package it.unibo.artificial_vision_tracking.graphics;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

/**
 * Class to create a input window for the app parameters.
 */
public class InputParameters {
    //Cosnatnts
    private static final String[] DICTIONARIES = {
        "DICT_4X4_100", "DICT_4X4_1000", "DICT_5X5_1000", "DICT_6X6_100", "DICT_6X6_1000"
    };

    private final JFrame parentFrame;
    private final JDialog dialog;
    private final JButton submitButton;
    private final JButton selectFolderButton;
    private final JLabel folderLabel;

    private final JTextField squaresXField;
    private final JTextField squaresYField;
    private final JTextField markerLengthField;
    private final JFileChooser folderChooser;
    private final JTextField selectedCameraField;
    private final JComboBox<String> dictionaryTypeField;
    private int squaresX;
    private int squaresY;
    private float markerLength;
    private String directoryPath;
    private int selectedCamera;
    private String dictionaryType;

    /**
     * Constructor of the class.
     */
    public InputParameters() {
        parentFrame = new JFrame();
        dialog = new JDialog(parentFrame, "Input Parameters", true); // Modal dialog
        submitButton = new JButton("Submit");
        selectFolderButton = new JButton("Select Folder");
        folderLabel = new JLabel("No folder selected");

        squaresXField = new JTextField();
        squaresYField = new JTextField();
        markerLengthField = new JTextField();
        folderChooser = new JFileChooser();
        selectedCameraField = new JTextField();
        dictionaryTypeField = new JComboBox<>(DICTIONARIES);
    }

    /**
     * Method to create the window.
     */
    public void createWindow() {
        // Setting the parent frame
        parentFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        final Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        parentFrame.setLocation(dim.width / 2 - parentFrame.getSize().width / 2, 
            dim.height / 2 - parentFrame.getSize().height / 2);
        parentFrame.pack();
        parentFrame.setVisible(true);

        // Setting the dialog
        dialog.setLayout(new GridLayout(0, 4, 10, 0));
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        // Close the application if the dialog is closed
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final java.awt.event.WindowEvent windowEvent) {
                System.exit(0);
            }
        });

        // Component to select the number of squares on the X axis of the chessboard
        dialog.add(new JLabel("SquaresX:"));
        dialog.add(squaresXField);

        // Component to select the number of squares on the Y axis of the chessboard
        dialog.add(new JLabel("SquaresY:"));
        dialog.add(squaresYField);

        // Component to select the side length of the marker
        dialog.add(new JLabel("Marker Length:"));
        dialog.add(markerLengthField);

        // Component to select the folder containing the calibration images for the camera
        dialog.add(folderLabel);
        dialog.add(selectFolderButton);
        selectFolderButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

                final int result = folderChooser.showOpenDialog(dialog);

                // If the user selects a folder
                if (result == JFileChooser.APPROVE_OPTION) {
                    final File selectedFolder = folderChooser.getSelectedFile();
                    folderLabel.setText("Selected folder: " + selectedFolder.getAbsolutePath());
                } else {
                    folderLabel.setText("Folder selection canceled");
                }
            }
        });

        // Component to select the camera index
        dialog.add(new JLabel("Selected Camera:"));
        dialog.add(selectedCameraField);

        // Component to select the dictionary type
        dialog.add(new JLabel("Dictionary Type:"));
        dialog.add(dictionaryTypeField);

        // Component to submit the parameters
        submitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                squaresX = Integer.parseInt(squaresXField.getText());
                squaresY = Integer.parseInt(squaresYField.getText());
                markerLength = Float.parseFloat(markerLengthField.getText());
                directoryPath = folderChooser.getSelectedFile().getAbsolutePath();
                selectedCamera = Integer.parseInt(selectedCameraField.getText());
                dictionaryType = dictionaryTypeField.getSelectedItem().toString();
                dialog.dispose();
            }
        });
        dialog.add(submitButton);

        // Center the dialog
        dialog.setLocation(parentFrame.getLocation());
        dialog.pack();  // Automatically size the dialog
        dialog.setVisible(true);
    }

    /**
     * Return the number of squares on the X axis of the chessboard.
     * @return the number of squares on the X axis of the chessboard
     */
    public int getSquaresX() {
        return squaresX;
    }

    /**
     * Return the number of squares on the Y axis of the chessboard.
     * @return the number of squares on the Y axis of the chessboard
     */
    public int getSquaresY() {
        return squaresY;
    }

    /**
     * Return the side length of the marker.
     * @return the side length of the marker
     */
    public float getMarkerLength() {
        return markerLength;
    }

    /**
     * Return the directory path containing the camera calibration images.
     * @return the directory path containing the camera calibration images
     */
    public String getDirectoryPath() {
        return directoryPath;
    }

    /**
     * Return the selected camera index.
     * @return the selected camera index
     */
    public int getCameraIndex() {
        return selectedCamera;
    }

    /**
     * Return the dictionary type.
     * @return the dictionary type
     */
    public String getDictionaryType() {
        return dictionaryType;
    }

    /**
     * Close the input parameters window.
     */
    public void close() {
        parentFrame.dispose();
    }
}
