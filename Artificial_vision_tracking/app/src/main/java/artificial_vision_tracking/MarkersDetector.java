package artificial_vision_tracking;

import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.FrameGrabber.Exception;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Point2f;
import org.bytedeco.opencv.opencv_core.Point2fVector;
import org.bytedeco.opencv.opencv_core.Point2fVectorVector;
import org.bytedeco.opencv.opencv_core.Point3fVector;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.global.opencv_aruco;
import org.bytedeco.javacv.Frame;
import org.bytedeco.opencv.opencv_aruco.DetectorParameters;
import org.bytedeco.opencv.opencv_aruco.Dictionary;
import org.bytedeco.opencv.opencv_aruco.GridBoard;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;

import java.util.ArrayList;
import java.util.List;


public class MarkersDetector {
    public void detect() throws FrameGrabber.Exception, InterruptedException {
        OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(1);
        
        grabber.start();
        CanvasFrame canvas = new CanvasFrame("Webcam");
        canvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
        OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();
        
        //Aruco tag Detection
        Dictionary dictionary = opencv_aruco.getPredefinedDictionary(opencv_aruco.DICT_4X4_50);

        while (canvas.isVisible() && (grabber.grab()) != null) {
            Frame frame = grabber.grab();
            Mat mat = converterToMat.convert(frame);

            // Lista di vettori di punti per i marker rilevati
            MatVector markerCorners = new MatVector();
            Mat markerIds = new Mat();

            opencv_aruco.detectMarkers(mat, dictionary, markerCorners, markerIds);

            // Se sono stati rilevati marker, disegna i contorni
            if (markerIds.size().height() > 0) {
                opencv_aruco.drawDetectedMarkers(mat, markerCorners, markerIds, new Scalar(0, 255, 0, 0));
            }

            canvas.showImage(converterToMat.convert(mat));
        }
        grabber.stop();
        grabber.close();
        dictionary.close();
        converterToMat.close();
        canvas.dispose();
    }
}
