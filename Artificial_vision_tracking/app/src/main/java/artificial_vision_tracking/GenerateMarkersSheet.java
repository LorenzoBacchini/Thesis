package artificial_vision_tracking;

import org.opencv.aruco.Aruco;
import org.opencv.core.CvType;

import org.opencv.aruco.Dictionary;
import org.opencv.aruco.GridBoard;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;

public class GenerateMarkersSheet {
    public void generateMarkersSheet() {
        int markersX = 8; // Numero di marker in orizzontale
        int markersY = 10; // Numero di marker in verticale
        float markerLength = 50; // Lunghezza del marker in pixel
        float markerSeparation = 10; // Separazione tra i marker in pixel

        // Crea il dizionario Aruco per i marker 4x4
        Dictionary dictionary = Aruco.getPredefinedDictionary(Aruco.DICT_4X4_100);

        // Crea una griglia di marker Aruco
        GridBoard gridBoard = GridBoard.create(markersX, markersY, markerLength, markerSeparation, dictionary);

        // Calcola la dimensione totale dell'immagine
        int totalWidth = (int) (markersX * (markerLength + markerSeparation) - markerSeparation);
        int totalHeight = (int) (markersY * (markerLength + markerSeparation) - markerSeparation);
        Size imageSize = new Size(totalWidth, totalHeight);

        // Crea un'immagine vuota
        Mat markerImage = new Mat(imageSize, CvType.CV_8UC1, new Scalar(255));

        // Disegna la griglia dei marker sull'immagine
        
        gridBoard.draw(imageSize, markerImage, 10, 1);

        // Salva l'immagine con i marker
        Imgcodecs.imwrite("aruco_markers.png", markerImage);

        System.out.println("Immagine dei marker Aruco generata e salvata come aruco_markers.png");
    }
}