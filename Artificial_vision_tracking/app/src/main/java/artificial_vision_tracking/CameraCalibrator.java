package artificial_vision_tracking;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CameraCalibrator {

    public static List<Mat> calibration(int boardWidth, int boardHeight) {
        Size boardSize = new Size(boardWidth, boardHeight);

        List<Mat> objectPoints = new ArrayList<>();
        List<Mat> imagePoints = new ArrayList<>();
        List<String> imageFiles = getImageFiles("..\\..\\python\\images\\");

        MatOfPoint3f objectPoint = new MatOfPoint3f();
        for (int i = 0; i < boardHeight; i++) {
            for (int j = 0; j < boardWidth; j++) {
                objectPoint.push_back(new MatOfPoint3f(new Point3(j, i, 0.0f)));
            }
        }

        for (String filePath : imageFiles) {
            System.out.println("Processing " + filePath);
            Mat image = Imgcodecs.imread(filePath);
            Mat grayImage = new Mat();
            Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);

            MatOfPoint2f imageCorners = new MatOfPoint2f();
            //boolean found = Calib3d.findChessboardCorners(grayImage, boardSize, Calib3d.CALIB_CB_ADAPTIVE_THRESH + Calib3d.CALIB_CB_NORMALIZE_IMAGE + Calib3d.CALIB_CB_FAST_CHECK);
            boolean found = Calib3d.findChessboardCorners(grayImage, boardSize, imageCorners);

            if (found) {
                Imgproc.cornerSubPix(grayImage, imageCorners, new Size(11, 11), new Size(-1, -1),
                        new TermCriteria(TermCriteria.EPS + TermCriteria.COUNT, 30, 0.001));

                imagePoints.add(imageCorners);
                objectPoints.add(objectPoint);
                Calib3d.drawChessboardCorners(image, boardSize, imageCorners, found);
                //Imgcodecs.imwrite("output_" + new File(filePath).getName(), image);
            }
        }

        Mat cameraMatrix = Mat.eye(3, 3, CvType.CV_64F);
        Mat distCoeffs = Mat.zeros(8, 1, CvType.CV_64F);
        List<Mat> rvecs = new ArrayList<>();
        List<Mat> tvecs = new ArrayList<>();

        double rms = Calib3d.calibrateCamera(objectPoints, imagePoints, boardSize, cameraMatrix, distCoeffs, rvecs, tvecs);

        System.out.println("RMS error: " + rms);
        System.out.println("Camera Matrix: \n" + cameraMatrix.dump());
        System.out.println("Distortion Coefficients: \n" + distCoeffs.dump());

        // Reprojection error calculation
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
