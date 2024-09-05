package artificial_vision_tracking;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CameraCalibrator {
    private static final float INITIAL_VALUE = 0.0f;

    private static final int WIN_X_SIZE = 11;
    private static final int WIN_Y_SIZE = 11;
    private static final int ZERO_X_ZONE = -1;
    private static final int ZERO_Y_ZONE = -1;
    private static final int MAX_ITERATION = 30;
    private static final double ACCURACY = 0.001;

    private static final int CAMERA_MATRIX_ROWS = 3;
    private static final int CAMERA_MATRIX_COLUMNS = 3;
    private static final int DIST_COEFFS_ROWS = 8;
    private static final int DIST_COEFFS_COLUMNS = 1;

    private int boardWidth;
    private int boardHeight;
    private String directoryPath;

    public CameraCalibrator(int boardWidth, int boardHeight, String directoryPath) {
        this.boardWidth = boardWidth;
        this.boardHeight = boardHeight;
        this.directoryPath = directoryPath;
    }

    /**
     * Method to calculate the calibration of the camera given a set of images
     * @param boardWidth width of the chessboard (in number of squares)
     * @param boardHeight height of the chessboard (in number of squares)
     * @param directoryPath path where the chessboard images are located
     * @return a collection of cameraMatrix and distCoeffs
     */
    public List<Mat> calibration() {
        Size boardSize = new Size(boardWidth, boardHeight);

        List<Mat> objectPoints = new ArrayList<>();
        List<Mat> imagePoints = new ArrayList<>();
        List<String> imageFiles = getImageFiles(directoryPath);

        MatOfPoint3f objectPoint = new MatOfPoint3f();
        for (int i = 0; i < boardHeight; i++) {
            for (int j = 0; j < boardWidth; j++) {
                objectPoint.push_back(new MatOfPoint3f(new Point3(j, i, INITIAL_VALUE)));
            }
        }

        //Scrolling through each image in the given path 
        for (String filePath : imageFiles) {
            System.out.println("Processing " + filePath);
            Mat image = Imgcodecs.imread(filePath);
            Mat grayImage = new Mat();
            //Converting to a gray scale image to reduce computational complexity
            Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);

            MatOfPoint2f imageCorners = new MatOfPoint2f();
            //Scanning the image to extract the corners
            boolean found = Calib3d.findChessboardCorners(grayImage, boardSize, imageCorners);

            //finishing the corners with the cornerSubPix method
            if (found) {
                Imgproc.cornerSubPix(grayImage, imageCorners, new Size(WIN_X_SIZE, WIN_Y_SIZE), new Size(ZERO_X_ZONE, ZERO_Y_ZONE),
                        new TermCriteria(TermCriteria.EPS + TermCriteria.COUNT, MAX_ITERATION, ACCURACY));

                imagePoints.add(imageCorners);
                objectPoints.add(objectPoint);
                //methods to draw a pattern on the image if the chessboard is recognized and then save the new image 
                //Calib3d.drawChessboardCorners(image, boardSize, imageCorners, found);
                //Imgcodecs.imwrite("output_" + new File(filePath).getName(), image);
            }
        }

        //Creating the two calibration matrix
        Mat cameraMatrix = Mat.eye(CAMERA_MATRIX_ROWS, CAMERA_MATRIX_COLUMNS, CvType.CV_64F);
        Mat distCoeffs = Mat.zeros(DIST_COEFFS_ROWS, DIST_COEFFS_COLUMNS, CvType.CV_64F);
        //Collections to calculate the reprojection error
        List<Mat> rvecs = new ArrayList<>();
        List<Mat> tvecs = new ArrayList<>();

        //calibration of the camera
        double rms = Calib3d.calibrateCamera(objectPoints, imagePoints, boardSize, cameraMatrix, distCoeffs, rvecs, tvecs);

        //Printing of the result obtained
        System.out.println("RMS error: " + rms);
        System.out.println("Camera Matrix: \n" + cameraMatrix.dump());
        System.out.println("Distortion Coefficients: \n" + distCoeffs.dump());

        //Reprojection error calculation
        double totalError = 0;
        double totalPoints = 0;
        for (int i = 0; i < objectPoints.size(); i++) {
            MatOfPoint2f projectedPoints = new MatOfPoint2f();
            Calib3d.projectPoints(new MatOfPoint3f(objectPoints.get(i)), rvecs.get(i), tvecs.get(i), cameraMatrix, new MatOfDouble(distCoeffs), projectedPoints);
            MatOfPoint2f imgPoints = new MatOfPoint2f(imagePoints.get(i));
            double error = Core.norm(imgPoints, projectedPoints, Core.NORM_L2);
            totalError += error * error;
            totalPoints += objectPoints.get(i).total();
        }

        double meanError = Math.sqrt(totalError / totalPoints);
        System.out.println("Mean Reprojection Error: " + meanError);

        return List.of(cameraMatrix, distCoeffs);
    }


    /**
     * Method for getting a list of images at a specified path
     * @param directoryPath
     * @return the list of the absolute path of each image
     */
    private static List<String> getImageFiles(String directoryPath) {
        File dir = new File(directoryPath);
        File[] files = dir.listFiles((dir1, name) -> name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".png"));
        List<String> imageFiles = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                imageFiles.add(file.getAbsolutePath());
            }
        }
        return imageFiles;
    }
}
