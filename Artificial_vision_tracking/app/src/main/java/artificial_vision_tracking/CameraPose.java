package artificial_vision_tracking;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;

import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.opencv.aruco.Aruco;
import org.opencv.aruco.Dictionary;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

public class CameraPose {
    private static boolean running = true;

    public static void calcPose(Mat cameraMatrix, Mat distCoeffs, float markerLength, Dictionary dictionary, int Selectedcamera) {
        long startTime = System.currentTimeMillis();
        int totalFrames = 0;
        double totalReprojectionError = 0;
        int markerCount = 0;
        
        OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();

        VideoCapture capture = new VideoCapture(1); // Usa 0 per la fotocamera predefinita
        if (!capture.isOpened()) {
            System.out.println("Errore: impossibile aprire la webcam.");
            converterToMat.close();
            return;
        }
    
        MatOfPoint3f objPoints = new MatOfPoint3f(
            new Point3(0, 0, 0),
            new Point3(markerLength, 0, 0),
            new Point3(markerLength, markerLength, 0),
            new Point3(0, markerLength, 0)
        );
    
        CanvasFrame canvas = new CanvasFrame("Webcam");
        canvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);

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

        Mat frame = new Mat();
        while (capture.read(frame) && running) {
            Mat gray = new Mat();
            Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);

            List<Mat> corners = new ArrayList<>();
            Mat ids = new Mat();
            Aruco.detectMarkers(gray, dictionary, corners, ids);

            if (!ids.empty()) {
                // Converti la Mat degli ID in un array di tipo int
                int[] idArray = new int[(int) ids.total()];
                ids.get(0, 0, idArray);

                for (int i = 0; i < idArray.length; i++) {
                    // Estrai gli angoli del marker corrente
                    Mat cornerMat = corners.get(i);
                    MatOfPoint2f cornerPoints = new MatOfPoint2f();
                    ArrayList<double[]> cornerData = new ArrayList<>();
                    //Estrai i quattro angoli del marker corrente
                    for (int h = 0; h < 4; h++) {
                        cornerData.add(cornerMat.get(0, h));
                    }

                    //Salva i quattro angoli come Point
                    Point[] cornerPointsArray = new Point[4];
                    for (int j = 0; j < cornerData.size(); j ++) {
                        double[] data = cornerData.get(j);
                        cornerPointsArray[j] = new Point(data[0], data[1]);
                    }

                    //Crea la MatOfPoint3f con gli angoli del marker
                    cornerPoints.fromArray(cornerPointsArray);

                    // Stima la posa usando solvePnP
                    Mat rvec = new Mat();
                    Mat tvec = new Mat();

                    if (cornerPoints.rows() >= 4) {
                        Calib3d.solvePnP(objPoints, cornerPoints, cameraMatrix, new MatOfDouble(distCoeffs), rvec, tvec);

                        // Calcola la distanza del marker dalla fotocamera
                        double distance = Core.norm(tvec);
                        System.out.printf("Marker ID: %d - Distanza: %.2f m%n", (int) ids.get(i, 0)[0], distance);

                        // Disegna gli assi
                        drawAxes(frame, cameraMatrix, new MatOfDouble(distCoeffs), rvec, tvec, markerLength);
                        //Calib3d.drawFrameAxes(frame, cameraMatrix, distCoeffs, rvec, tvec, markerLength);
                        double repojectionError = calculateReprojectionError(objPoints, cornerPoints, rvec, tvec, cameraMatrix, distCoeffs);
                        totalReprojectionError += repojectionError;
                        markerCount++;
                    }

                    // Stampa i vettori di rotazione e traslazione
                    //System.out.println("ID Marker: " + idArray[i]);
                    //System.out.println("rvec: " + rvec.dump());
                    //System.out.println("tvec: " + tvec.dump());
                }

                totalFrames++;
                Aruco.drawDetectedMarkers(frame, corners, ids);
            }

            canvas.showImage(converterToMat.convert(frame));		
        }

        long endTime = System.currentTimeMillis();
        double avgTimePerFrame = (endTime - startTime) / (double) totalFrames;

        System.out.println("Average time per frame: " + avgTimePerFrame + " ms");
        if (markerCount > 0) {
            double avgReprojectionError = totalReprojectionError / markerCount;
            System.out.printf("Errore di Riproiezione Medio: %.2f%n", avgReprojectionError);
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
    }

    private static double calculateReprojectionError(MatOfPoint3f objPoints, MatOfPoint2f imgPoints, Mat rvec, Mat tvec, Mat cameraMatrix, Mat distCoeffs) {
        MatOfPoint2f projectedPoints = new MatOfPoint2f();
        Calib3d.projectPoints(objPoints, rvec, tvec, cameraMatrix, new MatOfDouble(distCoeffs), projectedPoints);

        // Calcola l'errore di riproiezione
        double error = 0;
        
        error = Core.norm(imgPoints, projectedPoints, Core.NORM_L2) / imgPoints.rows();
        return error;
    }
}