package artificial_vision_tracking;

import org.bytedeco.javacv.FrameGrabber;
import org.opencv.aruco.Aruco;
import org.opencv.aruco.Dictionary;
import org.opencv.aruco.GridBoard;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import java.util.ArrayList;
import java.util.List;

public class CameraCalibrator {
    public static List<Mat> calibration(int markersX, int markersY, float markerLength, float markerSeparation, Dictionary dictionary) throws FrameGrabber.Exception{
        // Parametri per la creazione della GridBoard
        boolean refindStrategy = true;

        List<List<Mat>> allMarkerCorners = new ArrayList<>();
        List<Mat> allMarkerIds = new ArrayList<>();
        Size imageSize = new Size();

        GridBoard gridBoard = GridBoard.create(markersX, markersY, markerLength, markerSeparation, dictionary);

        // Collected frames for calibration
        VideoCapture capture = new VideoCapture(1);
        if (!capture.isOpened()) {
            System.out.println("Errore: impossibile aprire la webcam.");
            return null;
        }
        
        while (capture.grab()) {
            Mat image = new Mat();
            capture.retrieve(image);

            Mat gray = new Mat();
            Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);

            List<Mat> markerCorners = new ArrayList<>();
            List<Mat> rejectedMarkers = new ArrayList<>();
            Mat markerIds = new Mat();  // IDs dei marker

            // Detect markers
            Aruco.detectMarkers(gray, dictionary, markerCorners, markerIds);

            // Refind strategy to detect more markers
            if (refindStrategy) {
                Aruco.refineDetectedMarkers(image, gridBoard, markerCorners, markerIds, rejectedMarkers);
            } 

            // Verifica se sono stati rilevati marker
            if (!markerIds.empty()) {
                allMarkerCorners.add(markerCorners);
                allMarkerIds.add(markerIds);
                imageSize = gray.size();
            }

            // Interrompi dopo un certo numero di fotogrammi per l'esempio
            if (allMarkerIds.size() >= 10) {
                capture.release();
                break;
            }
        }

        Mat cameraMatrix = new Mat();
        Mat distCoeffs = new Mat();

        int calibrationFlags = Calib3d.CALIB_FIX_ASPECT_RATIO;
        double aspectRatio = 1.0;

        if ((calibrationFlags & Calib3d.CALIB_FIX_ASPECT_RATIO) != 0) {
            cameraMatrix = Mat.eye(3, 3, CvType.CV_64F);
            cameraMatrix.put(0, 0, aspectRatio);
        }

        // Prepare data for calibration
        List<Mat> processedObjectPoints = new ArrayList<>();
        List<Mat> processedImagePoints = new ArrayList<>();

        long nFrames = allMarkerCorners.size();

        for (int frame = 0; frame < nFrames; frame++) {
            Mat currentObjPoints = new Mat();
            Mat currentImgPoints = new Mat();

            Aruco.getBoardObjectAndImagePoints(gridBoard, allMarkerCorners.get(frame), allMarkerIds.get(frame), currentObjPoints, currentImgPoints);

            if (currentImgPoints.total() > 0 && currentObjPoints.total() > 0) {
                processedImagePoints.add(currentImgPoints);
                processedObjectPoints.add(currentObjPoints);
            }
        }

        //System.out.println("\nProcessed image points: " + processedImagePoints + "\nProcessed object points: " + processedObjectPoints);

        // Calibrate camera
        double repError = Calib3d.calibrateCamera(
                processedObjectPoints,
                processedImagePoints,
                imageSize,
                cameraMatrix,
                distCoeffs,
                (List<Mat>)new ArrayList<Mat>(),                
                (List<Mat>)new ArrayList<Mat>(),
                calibrationFlags
        );

        System.out.println("\nCalibration error: " + repError);
        System.out.println("\nCamera Matrix: " + cameraMatrix.dump());
        System.out.println("\nDistortion Coefficients: " + distCoeffs.dump());

        return List.of(cameraMatrix, distCoeffs);
    }
}
