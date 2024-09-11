/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package it.unibo.artificial_vision_tracking;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.opencv.opencv_java;

import it.unibo.artificial_vision_tracking.aruco_markers.CameraCalibrator;
import it.unibo.artificial_vision_tracking.aruco_markers.CameraPose;

import org.opencv.objdetect.Objdetect;
import org.opencv.core.Mat;

import java.util.List;

/**
 * Main class of the project.
 */
public final class App {
    /**
     * Loading the OpenCV native library
     */
    static {
        Loader.load(opencv_java.class);
    }

    private App() {
        throw new UnsupportedOperationException("Utility class");
    }
    /**
     * Main method.
     * @param args
     * @throws FrameGrabber.Exception
     * @throws InterruptedException
     */
    public static void main(final String[] args) throws FrameGrabber.Exception, InterruptedException {
        final int markersX = 11; // Numero di marker sull'asse X
        final int markersY = 8; // Numero di marker sull'asse Y
        final float markerLength = 0.07f; // Lunghezza del marker (in metri)
        final String directoryPath = "..\\calibration_images\\";
        //final Dictionary dictionary = Objdetect.getPredefinedDictionary(Objdetect.DICT_4X4_100);
        final int selectedCamera = 1;
        /*final int markerSheetMarkersX = 8; // Numero di marker sull'asse X
        final int markerSheetMarkersY = 10; // Numero di marker sull'asse Y
        final int markerSheetMarkerLength = 50; // Lunghezza del marker (in pixel)
        final int markerSheetMarkerSeparation = 10; // Separazione tra i marker (in pixel)*/
        final int dictionaryType = Objdetect.DICT_4X4_100;
        //final String fileName = "markersSheet";

        /*GenerateMarkersSheet gms = new GenerateMarkersSheet(markerSheetMarkersX, markerSheetMarkersY, 
            markerSheetMarkerLength, markerSheetMarkerSeparation, dictionaryType, fileName);*/
        //gms.generateMarkersSheet();
        final CameraCalibrator cc = new CameraCalibrator(markersX, markersY, directoryPath);
        final List<Mat> cameraParam = cc.calibration();



        /*List<Mat> cameraParam = new ArrayList<>();
        Mat cameraMatrix = new Mat();
        Mat distCoeffs = new Mat();

        cameraMatrix = new Mat(3, 3, org.opencv.core.CvType.CV_64F);
        double[] data = {
            1340.821804232236, 0, 945.5377637384079,
            0, 1339.251046705548, 581.4177912549047,
            0, 0, 1
        };
        cameraMatrix.put(0, 0, data);

        distCoeffs = new Mat(1, 5, org.opencv.core.CvType.CV_64F);
        double[] data2 = {
            -0.3898373600798533,
            0.08115247413122996,
            -1.965974706520358e-05,
            -0.0006330161088470909,
            0.1140937797457088
        };

        distCoeffs.put(0, 0, data2);

        cameraParam.add(cameraMatrix);
        cameraParam.add(distCoeffs);
        */


        final CameraPose cp = new CameraPose(cameraParam.get(0), cameraParam.get(1), 
            markerLength, dictionaryType, selectedCamera);
        cp.calcPose();

        //Test to calculate the pose of a single frame
        /*VideoCapture capture = cp.getCamera();
        long startTime = System.currentTimeMillis(); 
        int i = 0;
        //A FRAME LIMITER MAY BE REQUIRED (not sure about this)
        while(i < 100){
            System.out.println("\n" + cp.calcSinglePose(capture)[0].dump() + "\n");
            i++;
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Avg frame time: " + (endTime - startTime) / i + "ms");
        */

        //Test of RobotScreenSaver
        /*RobotScreenSaver rss = new RobotScreenSaver("ws://10.0.0.5:81", List.of(2,3,4,5));
        rss.screenSaver(cameraParam.get(0), cameraParam.get(1), markerLength, dictionaryType, selectedCamera);
        */
    }
}
