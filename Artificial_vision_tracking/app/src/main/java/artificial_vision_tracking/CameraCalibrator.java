package artificial_vision_tracking;

import org.bytedeco.javacv.FrameGrabber;
import org.opencv.aruco.Aruco;
import org.opencv.aruco.Dictionary;
import org.opencv.aruco.GridBoard;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
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
        File dir = new File(".\\src\\main\\resources\\images\\");
        File[] imageFiles = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".png"));

        if (imageFiles == null || imageFiles.length == 0) {
            System.out.println("Error: Could not find any images in the specified directory.");
            return null;
        }

        for (File imageFile : imageFiles) {
            Mat image = Imgcodecs.imread(imageFile.getAbsolutePath());

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



/*
 * 
 package artificial_vision_tracking;

import org.opencv.aruco.Aruco;
import org.opencv.aruco.Dictionary;
import org.opencv.aruco.GridBoard;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.io.File;

public class CameraCalibrator {
    public static List<Mat> calibration(int markersX, int markersY, float markerLength, float markerSeparation, Dictionary dictionary) {
        List<List<Mat>> allCorners = new ArrayList<>();
        List<Mat> allIds = new ArrayList<>();
        Size imageSize = new Size();

        // Create a grid board
        GridBoard gridBoard = GridBoard.create(markersX, markersY, markerLength, markerSeparation, dictionary);

        File dir = new File(".\\src\\main\\resources\\images\\");
        File[] imageFiles = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".png"));

        if (imageFiles == null || imageFiles.length == 0) {
            System.out.println("Error: Could not find any images in the specified directory.");
            return null;
        }

        for (File imageFile : imageFiles) {
            System.out.println("Processing file: " + imageFile.getName());
            Mat image = Imgcodecs.imread(imageFile.getAbsolutePath());
            Mat gray = new Mat();
            Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);

            List<Mat> corners = new ArrayList<>();
            Mat ids = new Mat();
            List<Mat> rejected = new ArrayList<>();

            Aruco.detectMarkers(gray, dictionary, corners, ids);

            if (!ids.empty()) {
                Aruco.refineDetectedMarkers(image, gridBoard, corners, ids, rejected);

                allCorners.add(corners);
                allIds.add(ids);
                imageSize = image.size();
            }
        }

        if (allIds.isEmpty()) {
            System.out.println("No markers detected.");
            return null;
        }

        Mat cameraMatrix = Mat.eye(3, 3, CvType.CV_64F);
        Mat distCoeffs = Mat.zeros(5, 1, CvType.CV_64F);

        List<Mat> objectPoints = new ArrayList<>();
        List<Mat> imagePoints = new ArrayList<>();

        for (int i = 0; i < allIds.size(); i++) {
            Mat currentObjPoints = new Mat();
            Mat currentImgPoints = new Mat();

            Aruco.getBoardObjectAndImagePoints(gridBoard, allCorners.get(i), allIds.get(i), currentObjPoints, currentImgPoints);

            if (currentImgPoints.total() > 0 && currentObjPoints.total() > 0) {
                imagePoints.add(currentImgPoints);
                objectPoints.add(currentObjPoints);
            }
        }

        // Ensure there are enough points for calibration
        if (imagePoints.size() < 10 || objectPoints.size() < 10) {
            System.out.println("Not enough marker detections for calibration. Please provide more images.");
            return null;
        }

        double error = Calib3d.calibrateCamera(objectPoints, imagePoints, imageSize, cameraMatrix, distCoeffs, new ArrayList<>(), new ArrayList<>(), Calib3d.CALIB_FIX_ASPECT_RATIO);

        System.out.println("Calibration error: " + error);
        List<Mat> calibrationResult = new ArrayList<>();
        calibrationResult.add(cameraMatrix);
        calibrationResult.add(distCoeffs);

        return calibrationResult;
    }
}
 * 
 */