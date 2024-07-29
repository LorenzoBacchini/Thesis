package artificial_vision_tracking;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;

import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.opencv.objdetect.Dictionary;
import org.opencv.objdetect.Objdetect;
import org.opencv.objdetect.DetectorParameters;
import org.opencv.objdetect.ArucoDetector;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

public class CameraPose {
    private static boolean running = true;

    public static void calcPose(Mat cameraMatrix, Mat distCoeffs, float markerLength, Dictionary dictionary, int Selectedcamera) {        
        long startTime = System.currentTimeMillis();
        int totalFrames = 0;

        double totalReprojectionError = 0;
        int markerCount = 0;

        ArucoDetector arucoDetector = new ArucoDetector();
        //Setting the dictionary
        arucoDetector.setDictionary(dictionary);
        
        //Setting the detector parameters
        DetectorParameters parameters = new DetectorParameters();
        parameters.set_adaptiveThreshWinSizeMin(3);
        parameters.set_adaptiveThreshWinSizeMax(23);
        parameters.set_adaptiveThreshWinSizeStep(10);
        parameters.set_adaptiveThreshConstant(7);
        arucoDetector.setDetectorParameters(parameters);
        
        //Getting the camera
        OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();

        VideoCapture capture = new VideoCapture(1); // Use 0 for the primary camera
        if (!capture.isOpened()) {
            System.out.println("Error: impossible to open webcam aprire la webcam.");
            converterToMat.close();
            return;
        }

        //Setting the proper camera resolution
        boolean resolutionSet = false;
        for (ResolutionEnum resolution : ResolutionEnum.values()) {
            if (capture.set(Videoio.CAP_PROP_FRAME_WIDTH, resolution.getWidth()) &&
            capture.set(Videoio.CAP_PROP_FRAME_HEIGHT, resolution.getHeight())) {
                System.out.println("Width: " + capture.get(Videoio.CAP_PROP_FRAME_WIDTH));
                System.out.println("Height: " + capture.get(Videoio.CAP_PROP_FRAME_HEIGHT));
                resolutionSet = true;   
                break;
            }
        }

        if (!resolutionSet) {
            System.out.println("Error: impossible to set camera resolution.");
        }
    
        MatOfPoint3f objPoints = new MatOfPoint3f(
            new Point3(0, 0, 0),
            new Point3(markerLength, 0, 0),
            new Point3(markerLength, markerLength, 0),
            new Point3(0, markerLength, 0)
        );
    
        //Canvas to display the webcam feed
        CanvasFrame canvas = new CanvasFrame("Webcam");
        canvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);

        //Key listener to close the application
        canvas.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_Q) {
                    running = false;
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_Q) {
                    running = false;
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_Q) {
                    running = false;
                }
            }
        });

        long temp = System.currentTimeMillis();
        Mat frame = new Mat();
        while (capture.read(frame) && running) {
            Mat undistorted = new Mat();
            Mat newCameraMatrix = Calib3d.getOptimalNewCameraMatrix(cameraMatrix, distCoeffs, frame.size(), 0);
            Calib3d.undistort(frame, undistorted, cameraMatrix, distCoeffs, newCameraMatrix);
            Mat gray = new Mat();
            Imgproc.cvtColor(undistorted, gray, Imgproc.COLOR_BGR2GRAY);

            List<Mat> corners = new ArrayList<>();
            Mat ids = new Mat();
            arucoDetector.detectMarkers(gray, corners, ids);

            if (!ids.empty()) {
                //Convert ID Mat to int array
                int[] idArray = new int[(int) ids.total()];
                ids.get(0, 0, idArray);
                for (int i = 0; i < idArray.length; i++) {
                    //Select the current marker
                    Mat cornerMat = corners.get(i).clone();
                    MatOfPoint2f cornerPoints = new MatOfPoint2f();
                    ArrayList<double[]> cornerData = new ArrayList<>();
                    //Extract the four corner points of the marker
                    for (int h = 0; h < 4; h++) {
                        cornerData.add(cornerMat.get(0, h));
                    }

                    //Save the corner points of the marker in an array
                    Point[] cornerPointsArray = new Point[4];
                    for (int j = 0; j < cornerData.size(); j ++) {
                        double[] data = cornerData.get(j);
                        cornerPointsArray[j] = new Point(data[0], data[1]);
                    }

                    //Create MatOfPoint2f mat with the corner points of the marker
                    cornerPoints.fromArray(cornerPointsArray);

                    //Pose estimation using solvePnP
                    Mat rvec = new Mat();
                    Mat tvec = new Mat();

                    if (cornerPoints.rows() >= 4) {
                        Calib3d.solvePnP(objPoints, cornerPoints, newCameraMatrix, new MatOfDouble(distCoeffs), rvec, tvec, false, Calib3d.SOLVEPNP_ITERATIVE);

                        // Calculating distance from camera
                        /*double distance = Core.norm(tvec);
                        System.out.printf("Marker ID: %d - Distance: %.2f m%n", (int) ids.get(i, 0)[0], distance);*/

                        // Draw marker axis
                        drawAxes(undistorted, newCameraMatrix, new MatOfDouble(distCoeffs), rvec, tvec, markerLength);
                        double reprojectionError = calculateReprojectionError(objPoints, cornerPoints, rvec, tvec, newCameraMatrix, distCoeffs);
                        totalReprojectionError += reprojectionError * reprojectionError;
                        markerCount += cornerPoints.total();
                        markerCount++;
                    }

                    // Print rotation and translation vectors
                    //System.out.println("ID Marker: " + idArray[i]);
                    //System.out.println("rvec: " + rvec.dump());
                    //System.out.println("tvec: " + tvec.dump());
                    //System.out.println("ID Marker: " + idArray[i] + " tvec: " + tvec.get(2, 0)[0]);
                    if (System.currentTimeMillis() - temp > 1500) {
                        System.out.println("ID Marker: " + idArray[i] + " tvec: " + tvec.dump());
                        temp = System.currentTimeMillis();
                    }

                    //Mats cleanup
                    rvec.release();
                    tvec.release();
                    cornerPoints.release();
                    cornerMat.release();
                }

                totalFrames++;
                Objdetect.drawDetectedMarkers(undistorted, corners, ids);
                ids.release();
                corners.clear();
            }

            Mat resizedFrame1_2 = new Mat();
            Imgproc.resize(undistorted, resizedFrame1_2, new Size(undistorted.width() / 2, undistorted.height() / 2));
            canvas.showImage(converterToMat.convert(resizedFrame1_2));
            
            gray.release();
            undistorted.release();
            newCameraMatrix.release();
            resizedFrame1_2.release();	
        }

        frame.release();
        long endTime = System.currentTimeMillis();
        double avgTimePerFrame = (endTime - startTime) / (double) totalFrames;

        System.out.println("Average time per frame: " + avgTimePerFrame + " ms");
        if (markerCount > 0) {
            double avgReprojectionError = Math.sqrt(totalReprojectionError / markerCount);
            System.out.printf("Average reprojection error: %.2f%n", avgReprojectionError);
        }
        canvas.dispose();
        capture.release();
        converterToMat.close();
    }

    private static void drawAxes(Mat image, Mat cameraMatrix, MatOfDouble distCoeffs, Mat rvec, Mat tvec, float length) {
        MatOfPoint3f axis = new MatOfPoint3f(
            new Point3(0, 0, 0),
            new Point3(length, 0, 0),
            new Point3(0, length, 0),
            new Point3(0, 0, -length)
        );

        MatOfPoint2f projectedPoints = new MatOfPoint2f();
        Calib3d.projectPoints(axis, rvec, tvec, cameraMatrix, distCoeffs, projectedPoints);

        Point[] pts = projectedPoints.toArray();
        Imgproc.line(image, pts[0], pts[1], new Scalar(0, 0, 255), 2); // X asse in rosso
        Imgproc.line(image, pts[0], pts[2], new Scalar(0, 255, 0), 2); // Y asse in verde
        Imgproc.line(image, pts[0], pts[3], new Scalar(255, 0, 0), 2); // Z asse in blu
        /*Imgproc.putText(
            image, 
            "x: " + rvec.get(0,0)[0] + "y: " + rvec.get(1,0)[0], 
            pts[0], 
            Imgproc.FONT_HERSHEY_PLAIN,
            1.0, 
            new Scalar(0, 0, 255),
            2, 
            Imgproc.LINE_AA
        );*/
        axis.release();
        projectedPoints.release();
    }

    private static double calculateReprojectionError(MatOfPoint3f objPoints, MatOfPoint2f imgPoints, Mat rvec, Mat tvec, Mat cameraMatrix, Mat distCoeffs) {
        MatOfPoint2f projectedPoints = new MatOfPoint2f();
        Calib3d.projectPoints(objPoints, rvec, tvec, cameraMatrix, new MatOfDouble(distCoeffs), projectedPoints);

        // Calculating reprojection error
        double error = 0;
        
        error = Core.norm(imgPoints, projectedPoints, Core.NORM_L2);
        projectedPoints.release();
        return error;
    }
}