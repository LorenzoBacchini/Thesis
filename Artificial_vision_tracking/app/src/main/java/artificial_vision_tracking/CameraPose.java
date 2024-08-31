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

    //Parameter to scale the frame size to speed up the marker detection
    private static final int SCALE = 2;
    private static final int SCALE_CANVAS = 2;

    private Mat cameraMatrix; 
    private Mat distCoeffs;

    private float markerLength;
    private Dictionary dictionary; 
    private int selectedcamera;
    private OpenCVFrameConverter.ToMat converterToMat;

    //Constructor
    public CameraPose(Mat cameraMatrix, Mat distCoeffs, float markerLength, Dictionary dictionary, int selectedcamera) {
        this.cameraMatrix = cameraMatrix;
        this.distCoeffs = distCoeffs;
        this.markerLength = markerLength;
        this.dictionary = dictionary;
        this.selectedcamera = selectedcamera;
        this.converterToMat = new OpenCVFrameConverter.ToMat();
    }

    //Getters
    public static int getScale() {
        return SCALE;
    }

    public static int getScaleCanvas() {
        return SCALE_CANVAS;
    }

    public Mat getCameraMatrix() {
        return cameraMatrix;
    }

    public Mat getDistCoeffs() {
        return distCoeffs;
    }

    public float getMarkerLength() {
        return markerLength;
    }

    public Dictionary getDictionary() {
        return dictionary;
    }

    public int getSelectedcamera() {
        return selectedcamera;
    }

    public OpenCVFrameConverter.ToMat getConverterToMat() {
        return converterToMat;
    }

    /**
     * Method to get the ArucoDetector
     * @return ArucoDetector
     */
    public ArucoDetector getArucoDetector() {
        ArucoDetector arucoDetector = new ArucoDetector();
        //Setting the dictionary
        arucoDetector.setDictionary(this.dictionary);

        //Setting the detector parameters
        DetectorParameters parameters = new DetectorParameters();
        //parameters to detect the markers with different thresholds
        parameters.set_adaptiveThreshWinSizeMin(3);
        parameters.set_adaptiveThreshWinSizeMax(23);
        parameters.set_adaptiveThreshWinSizeStep(10);
        parameters.set_adaptiveThreshConstant(7);
        //parameter to set the size of the black border around the marker
        parameters.set_markerBorderBits(1);
        //Augmented pixel per cell (reduce if the performance is too low)
        parameters.set_perspectiveRemovePixelPerCell(16);
        //Margin of pixels to remove from the final image (0.1 is 10%)
        parameters.set_perspectiveRemoveIgnoredMarginPerCell(0.1);
        arucoDetector.setDetectorParameters(parameters);

        return arucoDetector;
    }

    /**
     * Method to get the camera
     * @return VideoCapture
     */
    public VideoCapture getCamera() {
        //Getting the camera
        VideoCapture capture = new VideoCapture(this.selectedcamera); // Use 0 for the primary camera
        if (!capture.isOpened()) {
            System.out.println("Error: impossible to open webcam aprire la webcam.");
            converterToMat.close();
            return null;
        }

        //Setting the proper camera resolution
        boolean resolutionSet = false;
        for (ResolutionEnum resolution : ResolutionEnum.values()) {
            if (capture.set(Videoio.CAP_PROP_FRAME_WIDTH, resolution.getWidth()) &&
            capture.set(Videoio.CAP_PROP_FRAME_HEIGHT, resolution.getHeight())) {
                System.out.println("Frame Width: " + capture.get(Videoio.CAP_PROP_FRAME_WIDTH));
                System.out.println("Frame Height: " + capture.get(Videoio.CAP_PROP_FRAME_HEIGHT));
                resolutionSet = true;   
                break;
            }
        }

        //Setting the camera exposure to reduce the motion blur
        capture.set(Videoio.CAP_PROP_EXPOSURE, -6);

        //Getting the frame rate
        System.out.println("Frame rate: " + capture.get(Videoio.CAP_PROP_FPS));

        if (!resolutionSet) {
            System.out.println("Error: impossible to set camera resolution.");
        }

        return capture;
    }

    /**
     * Method to get the canvas
     * @param title
     * @return CanvasFrame
     */
    public static CanvasFrame getCanvas(String title) {
        //Canvas to display the webcam feed
        CanvasFrame canvas = new CanvasFrame(title);
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

        return canvas;
    }

    public void calcPose() {        
        long startTime = 0;
        int totalFrames = 0;

        double totalReprojectionError = 0;
        int markerCount = 0;

        //Getting the ArucoDetector
        ArucoDetector arucoDetector = getArucoDetector();
        
        //Getting the camera
        VideoCapture capture = getCamera();

        long frameDuration = (long) (1000 / capture.get(Videoio.CAP_PROP_FPS));
    
        //Create the object points of the marker
        MatOfPoint3f objPoints = new MatOfPoint3f(
            new Point3(0, 0, 0),
            new Point3(markerLength, 0, 0),
            new Point3(markerLength, markerLength, 0),
            new Point3(0, markerLength, 0)
        );
    
        //Canvas to display the webcam feed
        CanvasFrame canvas = getCanvas("Webcam");
        
        Mat frame = new Mat();

        //Variable to check if the marker zero is lost
        boolean lose = false;

        //Variables to calculate the time of the detection and pose estimation
        long totalTimeDetection = 0;
        long totalTimePose = 0;
        long totalGetFrameTime = 0;
        long startGetFrameTime = 0;
        startTime = System.currentTimeMillis();
        long loseTime = System.currentTimeMillis();
        long startWhile = 0;
        while (running) {
            startWhile = System.currentTimeMillis();

            //Getting the frame
            startGetFrameTime = System.currentTimeMillis();
            capture.read(frame);
            if (frame.empty()) {
                break;
            }
            totalGetFrameTime += System.currentTimeMillis() - startGetFrameTime;

            //Resize the frame to speed up the marker detection
            Mat reducedFrame = new Mat();
            Imgproc.resize(frame, reducedFrame, new Size(frame.width() / SCALE, frame.height() / SCALE));
            
            /*Mat undistorted = new Mat();
            Mat newCameraMatrix = Calib3d.getOptimalNewCameraMatrix(cameraMatrix, distCoeffs, frame.size(), 0);
            Calib3d.undistort(frame, undistorted, cameraMatrix, distCoeffs, newCameraMatrix);*/
            
            //Convert the frame to gray in order to detect the markers
            Mat gray = new Mat();
            Imgproc.cvtColor(reducedFrame, gray, Imgproc.COLOR_BGR2GRAY);

            //Corners and ids of the detected markers
            List<Mat> corners = new ArrayList<>();
            Mat ids = new Mat();

            //Detecting the markers
            long start = System.currentTimeMillis();
            arucoDetector.detectMarkers(gray, corners, ids);
            if (!corners.isEmpty()) {
                rescalePoints(corners);
            }
            totalTimeDetection += System.currentTimeMillis() - start;
            
            //Pose estimation
            if (!ids.empty()) {
                //Convert ID Mat to int array
                int[] idArray = new int[(int) ids.total()];
                ids.get(0, 0, idArray);
                
                //Check if the zero marker is lost
                if (!containsZeroMarker(idArray)) {
                    if(!lose) {
                        loseTime = System.currentTimeMillis();
                        lose = true;
                    }
                } else {
                    if (lose) {
                        lose = false;
                        System.out.println("Lose: " + (System.currentTimeMillis() - loseTime));
                    }
                }

                //Starting the pose estimation
                long startPose = System.currentTimeMillis();
                for (int i = 0; i < idArray.length; i++) {
                    //Select the current marker corner
                    Mat cornerMat = corners.get(i).clone();
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
                    MatOfPoint2f cornerPoints = new MatOfPoint2f();
                    cornerPoints.fromArray(cornerPointsArray);

                    //Mat to store the rotation and translation vectors
                    Mat rvec = new Mat();
                    Mat tvec = new Mat();

                    //Pose estimation using solvePnP
                    if (cornerPoints.rows() >= 4) {
                        Calib3d.solvePnP(objPoints, cornerPoints, cameraMatrix, new MatOfDouble(distCoeffs), rvec, tvec, false, Calib3d.SOLVEPNP_ITERATIVE);

                        // Calculating distance from camera
                        /*double distance = Core.norm(tvec);
                        System.out.printf("Marker ID: %d - Distance: %.2f m%n", (int) ids.get(i, 0)[0], distance);*/
                        // Draw marker axis
                        drawAxes(frame, rvec, tvec, markerLength);
                        // Calculate reprojection error
                        double reprojectionError = calculateReprojectionError(objPoints, cornerPoints, rvec, tvec);
                        totalReprojectionError += reprojectionError * reprojectionError;
                        markerCount += cornerPoints.total();
                    }

                    //Mats cleanup
                    rvec.release();
                    tvec.release();
                    cornerPoints.release();
                    cornerMat.release();
                }
                totalTimePose += System.currentTimeMillis() - startPose;
                Objdetect.drawDetectedMarkers(frame, corners, ids);
                ids.release();
                corners.clear();
            } else {
                //Also if ids is empty I need to update the lose variable
                if (!lose) {
                    loseTime = System.currentTimeMillis();
                    lose = true;
                }
            }
            totalFrames++;

            //Resizing the frame to display it (only beacuse it looks better)
            Mat resizedFrame1_2 = new Mat();
            Imgproc.resize(frame, resizedFrame1_2, new Size(frame.width() / SCALE_CANVAS, frame.height() / SCALE_CANVAS));
            
            //Display the frame
            canvas.showImage(converterToMat.convert(resizedFrame1_2));
            
            //Mats cleanup
            gray.release();
            reducedFrame.release();
            frame.release();
            resizedFrame1_2.release();

            // Code to limit the frame rate to the camera frame rate
            if (System.currentTimeMillis() - startWhile < frameDuration) {
                try {
                    Thread.sleep(frameDuration - (System.currentTimeMillis() - startWhile));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        frame.release();
        long endTime = System.currentTimeMillis();
        System.out.println("Average time per getFrameTime: " + totalGetFrameTime / (double) totalFrames + " ms");
        System.out.println("Average time per detection: " + totalTimeDetection / (double) totalFrames + " ms");
        System.out.println("Average time per pose estimation: " + totalTimePose / (double) totalFrames + " ms");
        System.out.println("Average time per frame: " + (endTime - startTime) / (double) totalFrames + " ms");
        if (markerCount > 0) {
            double avgReprojectionError = Math.sqrt(totalReprojectionError / markerCount);
            System.out.printf("Average reprojection error: %.2f%n", avgReprojectionError);
        }
        canvas.dispose();
        capture.release();
        converterToMat.close();
    }


    /**
     * Method to draw the axes of the marker an the position of the marker
     * !IMPORTANT this method takes a lot of time to be executed so use it only for debugging  
     * @param image
     * @param rvec
     * @param tvec
     * @param length
     */
    private void drawAxes(Mat image, Mat rvec, Mat tvec, float length) {
        MatOfPoint3f axis = new MatOfPoint3f(
            new Point3(0, 0, 0),
            new Point3(length, 0, 0),
            new Point3(0, length, 0),
            new Point3(0, 0, -length)
        );

        MatOfPoint2f projectedPoints = new MatOfPoint2f();
        Calib3d.projectPoints(axis, rvec, tvec, cameraMatrix, new MatOfDouble(distCoeffs), projectedPoints);

        Point[] pts = projectedPoints.toArray();
        // Draw the X, Y, Z axis
        Imgproc.line(image, pts[0], pts[1], new Scalar(0, 0, 255), 2); // X axis in red
        Imgproc.line(image, pts[0], pts[2], new Scalar(0, 255, 0), 2); // Y axis in green
        Imgproc.line(image, pts[0], pts[3], new Scalar(255, 0, 0), 2); // Z axis in blue 

        // Draw the text for the tvec and rvec
        String tvecText = String.format("tvec: [%.2f, %.2f, %.2f]", tvec.get(0, 0)[0], tvec.get(1, 0)[0], tvec.get(2, 0)[0]);
        String rvecText = String.format("rvec: [%.2f, %.2f, %.2f]", (rvec.get(0, 0)[0] * 180) / Math.PI, (rvec.get(1, 0)[0] * 180) / Math.PI, (rvec.get(2, 0)[0] * 180) / Math.PI);
        Point tvectextPos = new Point(pts[0].x -50, pts[0].y - 15); // Put the text on the top of the marker
        Point rvectextPos = new Point(pts[0].x -50, pts[0].y + 75); // Put the text on the bottom of the marker
        Imgproc.putText(image, tvecText, tvectextPos, Imgproc.FONT_HERSHEY_SIMPLEX, 1, new Scalar(255, 255, 255), 2, Imgproc.LINE_AA);
        Imgproc.putText(image, rvecText, rvectextPos, Imgproc.FONT_HERSHEY_SIMPLEX, 1, new Scalar(255, 255, 255), 2, Imgproc.LINE_AA);
        
        //Mats cleanup
        axis.release();
        projectedPoints.release();
    }

    /**
     * Method to calculate the reprojection error
     * @param objPoints
     * @param imgPoints
     * @param rvec
     * @param tvec
     * @return Reprojection error
     */
    private double calculateReprojectionError(MatOfPoint3f objPoints, MatOfPoint2f imgPoints, Mat rvec, Mat tvec) {
        MatOfPoint2f projectedPoints = new MatOfPoint2f();
        Calib3d.projectPoints(objPoints, rvec, tvec, cameraMatrix, new MatOfDouble(distCoeffs), projectedPoints);

        // Calculating reprojection error
        double error = 0;
        error = Core.norm(imgPoints, projectedPoints, Core.NORM_L2);
        projectedPoints.release();
        return error;
    }

    /**
     * Method to rescale the points 
     * !IMPORTANT 
     *  if you resize the image before the detection 
     *  than you need to call this method to rescale the points
     *  detected by the marker detection to the right size
     *  otherwise the pose estimation will not work as expected
     * !IMPORTANT
     * @param corners
     */
    private static void rescalePoints(List<Mat> corners) {
        for (Mat corner : corners) {
            for (int i = 0; i < 4; i++) {
                double[] data = corner.get(0, i);
                data[0] *= SCALE;
                data[1] *= SCALE;
                corner.put(0, i, data);
            }
        }
    }

    /**
     * Method to check if the marker array contains the zero marker
     * @param idsArray
     * @return
     */
    private static boolean containsZeroMarker(int[] idsArray) {
        for (int i = 0; i < idsArray.length; i++) {
            if (idsArray[i] == 0) {
                return true;
            }
        }
        return false;
    }
}