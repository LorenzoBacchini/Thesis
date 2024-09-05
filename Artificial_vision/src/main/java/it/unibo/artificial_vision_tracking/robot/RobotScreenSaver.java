package it.unibo.artificial_vision_tracking.robot;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

import it.unibo.artificial_vision_tracking.aruco_markers.ResolutionEnum;

public class RobotScreenSaver {
    private static boolean running = true;

    //Parameter to scale the frame size to speed up the marker detection
    private static final int SCALE = 2;
    private static final int SCALE_CANVAS = 2;

    public static void screenSaver(Mat cameraMatrix, Mat distCoeffs, float markerLength, Dictionary dictionary, int Selectedcamera) {   
        //Connection to the Bot
        URI serverUri = null;
        ESP32Client client = null;
        try {
            serverUri = new URI("ws://10.0.0.6:81"); // Sostituisci con l'IP dell'ESP32
            client = new ESP32Client(serverUri);
            client.connectBlocking();
        } catch (Exception e) {
            e.printStackTrace();
        } 
 
        HashSet<Integer> borderCorners = new HashSet<>(List.of(2,3,4, 5));

        long startTime = 0;
        int totalFrames = 0;

        double totalReprojectionError = 0;
        int markerCount = 0;

        ArucoDetector arucoDetector = new ArucoDetector();
        //Setting the dictionary
        arucoDetector.setDictionary(dictionary);
        
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
                System.out.println("Frame Width: " + capture.get(Videoio.CAP_PROP_FRAME_WIDTH));
                System.out.println("Frame Height: " + capture.get(Videoio.CAP_PROP_FRAME_HEIGHT));
                resolutionSet = true;   
                break;
            }
        }

        //Setting the camera exposure to reduce the motion blur
        capture.set(Videoio.CAP_PROP_EXPOSURE, -6);

        //Getting the frame rate
        long frameDuration = (long) (1000 / capture.get(Videoio.CAP_PROP_FPS));
        System.out.println("Frame rate: " + capture.get(Videoio.CAP_PROP_FPS));

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

        double oldZeroMarkerX = 0;
        double oldZeroMarkerY = 0;
        boolean rotationStarted = false;
        long startTimeRotation = 0;
        long endTimeRotation = 0;
        int counter = 20;

        Mat frame = new Mat();
        long totalTimeDetection = 0;
        long totalTimePose = 0;
        long totalGetFrameTime = 0;
        boolean lose = false;
        long startGetFrameTime = 0;
        startTime = System.currentTimeMillis();
        long t = System.currentTimeMillis();
        long startWhile = 0;
        while (/*capture.read(frame) && */running) {
            startWhile = System.currentTimeMillis();
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
            
            Mat gray = new Mat();
            Imgproc.cvtColor(reducedFrame, gray, Imgproc.COLOR_BGR2GRAY);

            List<Mat> corners = new ArrayList<>();
            Mat ids = new Mat();

            List<Double> x = new ArrayList<>();
            List<Double> y = new ArrayList<>();
            double zeroMarkerX = 0;
            double zeroMarkerY = 0;

            long start = System.currentTimeMillis();
            arucoDetector.detectMarkers(gray, corners, ids);
            if (!corners.isEmpty()) {
                rescalePoints(corners);
            }
            totalTimeDetection += System.currentTimeMillis() - start;
            
            if (!ids.empty()) {
                //Convert ID Mat to int array
                int[] idArray = new int[(int) ids.total()];
                ids.get(0, 0, idArray);
                if (!containsZeroMarker(idArray)) {
                    if(!lose) {
                        t = System.currentTimeMillis();
                        lose = true;
                    }
                } else {
                    if (lose) {
                        lose = false;
                        System.out.println("Lose: " + (System.currentTimeMillis() - t));
                    }
                }
                long startPose = System.currentTimeMillis();
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
                        Calib3d.solvePnP(objPoints, cornerPoints, cameraMatrix, new MatOfDouble(distCoeffs), rvec, tvec, false, Calib3d.SOLVEPNP_ITERATIVE);
                        
                        if (borderCorners.contains(idArray[i])) {
                            x.add(tvec.get(0, 0)[0]);
                            y.add(tvec.get(1, 0)[0]);
                        } else if (idArray[i] == 0) {
                            zeroMarkerX = tvec.get(0, 0)[0];
                            zeroMarkerY = tvec.get(1, 0)[0];
                        }
                        // Calculating distance from camera
                        /*double distance = Core.norm(tvec);
                        System.out.printf("Marker ID: %d - Distance: %.2f m%n", (int) ids.get(i, 0)[0], distance);*/
                        // Draw marker axis
                        drawAxes(frame, cameraMatrix, new MatOfDouble(distCoeffs), rvec, tvec, markerLength);
                        double reprojectionError = calculateReprojectionError(objPoints, cornerPoints, rvec, tvec, cameraMatrix, distCoeffs);
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

                //Send the command to the bot
                Collections.sort(x);
                Collections.sort(y);
                int[] allIdsArray = new int[(int) ids.total()];
                ids.get(0, 0, allIdsArray);
                if (containsAllMarker(allIdsArray)) {
                    double lowestX = x.get(0);
                    double highestX = x.get(x.size() - 1);
                    double lowestY = y.get(0);
                    double highestY = y.get(y.size() - 1);

                    lowestX = lowestX + 0.2;
                    highestX = highestX - 0.2;
                    lowestY = lowestY + 0.2;
                    highestY = highestY - 0.2;

                    String message;
                    if ((zeroMarkerX > lowestX && zeroMarkerX < highestX 
                        && zeroMarkerY > lowestY && zeroMarkerY < highestY) 
                        || System.currentTimeMillis() - endTimeRotation < 1000) {
                        message = DirectionEnum.MOVE_FORWARD.toString();
                        try {
                            client.send(message);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (zeroMarkerX > highestX || zeroMarkerX < lowestX) {
                        if (oldZeroMarkerX < zeroMarkerX && oldZeroMarkerY > zeroMarkerY && !rotationStarted) {
                            message = DirectionEnum.TURN_LEFT.toString();
                            try {
                                client.send(message);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else if (oldZeroMarkerX < zeroMarkerX && oldZeroMarkerY < zeroMarkerY && !rotationStarted) {
                            message = DirectionEnum.TURN_RIGHT.toString();
                            try {
                                client.send(message);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else if (oldZeroMarkerX > zeroMarkerX && oldZeroMarkerY < zeroMarkerY && !rotationStarted) {
                            message = DirectionEnum.TURN_LEFT.toString();
                            try {
                                client.send(message);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else if (oldZeroMarkerX > zeroMarkerX && oldZeroMarkerY > zeroMarkerY && !rotationStarted) {
                            message = DirectionEnum.TURN_RIGHT.toString();
                            try {
                                client.send(message);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        if (!rotationStarted) {
                            System.out.println("LowestX: " + lowestX + " HighestX: " + highestX + " LowestY: " + lowestY + " HighestY: " + highestY + " ZeroX: " + zeroMarkerX + " ZeroY: " + zeroMarkerY);
                            startTimeRotation = System.currentTimeMillis();
                            rotationStarted = true;
                        }
                        if (System.currentTimeMillis() - startTimeRotation > 1000) {
                            message = DirectionEnum.MOVE_FORWARD.toString();
                            try {
                                client.send(message);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            rotationStarted = false;
                            endTimeRotation = System.currentTimeMillis();
                        }
                    } else if (zeroMarkerY > highestY || zeroMarkerY < lowestY) {
                        System.out.println("Alto/Basso");
                        System.out.println("Old: " + oldZeroMarkerX + " " + oldZeroMarkerY);
                        System.out.println("New: " + zeroMarkerX + " " + zeroMarkerY);
                        if (oldZeroMarkerX < zeroMarkerX && oldZeroMarkerY > zeroMarkerY && !rotationStarted) {
                            System.out.println("1");
                            message = DirectionEnum.TURN_RIGHT.toString();
                            try {
                                client.send(message);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else if (oldZeroMarkerX > zeroMarkerX && oldZeroMarkerY > zeroMarkerY && !rotationStarted) {
                            System.out.println("2");
                            message = DirectionEnum.TURN_LEFT.toString();
                            try {
                                client.send(message);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else if (oldZeroMarkerX < zeroMarkerX && oldZeroMarkerY < zeroMarkerY && !rotationStarted) {
                            System.out.println("3");
                            message = DirectionEnum.TURN_LEFT.toString();
                            try {
                                client.send(message);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else if (oldZeroMarkerX > zeroMarkerX && oldZeroMarkerY < zeroMarkerY && !rotationStarted) {
                            System.out.println("4");
                            message = DirectionEnum.TURN_RIGHT.toString();
                            try {
                                client.send(message);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        if (!rotationStarted) {
                            System.out.println("LowestX: " + lowestX + " HighestX: " + highestX + " LowestY: " + lowestY + " HighestY: " + highestY + " ZeroX: " + zeroMarkerX + " ZeroY: " + zeroMarkerY);
                            startTimeRotation = System.currentTimeMillis();
                            rotationStarted = true;
                        }
                        if (System.currentTimeMillis() - startTimeRotation > 1000) {
                            message = DirectionEnum.MOVE_FORWARD.toString();
                            try {
                                client.send(message);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            rotationStarted = false;
                            endTimeRotation = System.currentTimeMillis();
                        }
                    }
                    if (counter == 20) {
                        oldZeroMarkerX = zeroMarkerX;
                        oldZeroMarkerY = zeroMarkerY;
                        counter = 0;
                    } else {
                        counter++;
                    }
                }

                x.clear();
                y.clear();
                ids.release();
                corners.clear();
            } else {
                if (!lose) {
                    t = System.currentTimeMillis();
                    lose = true;
                }
            }
            totalFrames++;

            //Resizing the frame to display it (only beacuse it looks better)
            Mat resizedFrame1_2 = new Mat();
            Imgproc.resize(frame, resizedFrame1_2, new Size(frame.width() / SCALE_CANVAS, frame.height() / SCALE_CANVAS));
            
            canvas.showImage(converterToMat.convert(resizedFrame1_2));
            
            gray.release();
            reducedFrame.release();
            frame.release();
            /**
             * I can't release cameraMatrix because I need it in the next iteration.
             * 
             * I can release cameraMatrix only if i'm using undistorted Mat with the newCameraMatrix
             * so i don't need cameraMatrix anymore.
             */
            //cameraMatrix.release();
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

        client.close();
        frame.release();
        long endTime = System.currentTimeMillis();
        double avgTimePerFrame = (endTime - startTime) / (double) totalFrames;
        System.out.println("Average time per getFrameTime: " + totalGetFrameTime/ (double) totalFrames + " ms");
        System.out.println("Average time per detection: " + totalTimeDetection/ (double) totalFrames + " ms");
        System.out.println("Average time per pose estimation: " + totalTimePose/ (double) totalFrames + " ms");
        System.out.println("Average time per frame: " + avgTimePerFrame + " ms");
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
     * !IMPORTANT! this method takes a lot of time to be executed so use it only for debugging  
     * @param image
     * @param cameraMatrix
     * @param distCoeffs
     * @param rvec
     * @param tvec
     * @param length
     */
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

        String tvecText = String.format("tvec: [%.2f, %.2f, %.2f]", tvec.get(0, 0)[0], tvec.get(1, 0)[0], tvec.get(2, 0)[0]);
        String rvecText = String.format("rvec: [%.2f, %.2f, %.2f]", (rvec.get(0, 0)[0] * 180) / Math.PI, (rvec.get(1, 0)[0] * 180) / Math.PI, (rvec.get(2, 0)[0] * 180) / Math.PI);
        Point tvectextPos = new Point(pts[0].x -50, pts[0].y - 15); // Posizionare il testo sopra il marker
        Point rvectextPos = new Point(pts[0].x -50, pts[0].y + 75); // Posizionare il testo in alto a sinistra
        Imgproc.putText(image, tvecText, tvectextPos, Imgproc.FONT_HERSHEY_SIMPLEX, 1, new Scalar(255, 255, 255), 2, Imgproc.LINE_AA);
        Imgproc.putText(image, rvecText, rvectextPos, Imgproc.FONT_HERSHEY_SIMPLEX, 1, new Scalar(255, 255, 255), 2, Imgproc.LINE_AA);
        axis.release();
        projectedPoints.release();
    }

    /**
     * Method to calculate the reprojection error
     * @param objPoints
     * @param imgPoints
     * @param rvec
     * @param tvec
     * @param cameraMatrix
     * @param distCoeffs
     * @return
     */
    private static double calculateReprojectionError(MatOfPoint3f objPoints, MatOfPoint2f imgPoints, Mat rvec, Mat tvec, Mat cameraMatrix, Mat distCoeffs) {
        MatOfPoint2f projectedPoints = new MatOfPoint2f();
        Calib3d.projectPoints(objPoints, rvec, tvec, cameraMatrix, new MatOfDouble(distCoeffs), projectedPoints);

        // Calculating reprojection error
        double error = 0;
        
        error = Core.norm(imgPoints, projectedPoints, Core.NORM_L2);
        projectedPoints.release();
        return error;
    }

    /**
     * Method to rescale the points detected by the marker detection if the frame is resized to speed up the detection
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

    private static boolean containsAllMarker(int[] allIdsArray) {
        Set<Integer> ids = new HashSet<>();
        for (int i = 0; i < allIdsArray.length; i++) {
            ids.add(allIdsArray[i]);
        }

        if (ids.size() >= 5 && ids.contains(0)) {
            return true;
        }
        return false;
    }
}