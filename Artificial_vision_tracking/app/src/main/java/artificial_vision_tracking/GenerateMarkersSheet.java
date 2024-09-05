package artificial_vision_tracking;

import org.opencv.core.CvType;
import org.opencv.objdetect.Dictionary;
import org.opencv.objdetect.GridBoard;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;

public class GenerateMarkersSheet {
    private static final int FULL_COLOR = 255;
    private static final int IMAGE_MARGIN_SIZE = 10;
    private static final int MARKER_BORDER_BITS= 1;

    // Horizontal markers
    private int markersX;
    // Vertical markers
    private int markersY;
    // Marker length in pixels
    private float markerLength;
    // Separation between markers in pixels
    private float markerSeparation;
    // Aruco dictionary
    Dictionary dictionary;
    // Output file name
    private String fileName;

    public GenerateMarkersSheet(int markersX, int markersY, float markerLength, float markerSeparation, Dictionary dictionary, String fileName) {
        this.markersX = markersX;
        this.markersY = markersY;
        this.markerLength = markerLength;
        this.markerSeparation = markerSeparation;
        this.dictionary = dictionary;
        this.fileName = fileName;
    }

    /**
     * Method to generate the Aruco markers sheet
     */
    public void generateMarkersSheet() {
        GridBoard gridBoard = new GridBoard(new Size(markersX, markersY), markerLength, markerSeparation, dictionary);

        // Calculate the total width and height of the image
        int totalWidth = (int) (markersX * (markerLength + markerSeparation) - markerSeparation);
        int totalHeight = (int) (markersY * (markerLength + markerSeparation) - markerSeparation);
        Size imageSize = new Size(totalWidth, totalHeight);

        // Create an image with a white background
        Mat markerImage = new Mat(imageSize, CvType.CV_8UC1, new Scalar(FULL_COLOR));

        // Drawing the markers grid
        gridBoard.generateImage(imageSize, markerImage, IMAGE_MARGIN_SIZE, MARKER_BORDER_BITS);

        // Saving the image
        Imgcodecs.imwrite(fileName + ".png", markerImage);

        System.out.println("Immagine dei marker Aruco generata e salvata come " + fileName + ".png");
    }
}