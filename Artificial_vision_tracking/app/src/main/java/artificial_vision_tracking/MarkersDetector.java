package artificial_vision_tracking;

import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.videoio.VideoCapture;
import org.opencv.objdetect.Dictionary;
import org.opencv.objdetect.Objdetect;
import org.opencv.objdetect.ArucoDetector;

import java.util.List;
import java.util.ArrayList;



public class MarkersDetector {
    public void detect(Dictionary dictionary, int selectedCamera) throws FrameGrabber.Exception, InterruptedException {
        ArucoDetector arucoDetector = new ArucoDetector();
        arucoDetector.setDictionary(dictionary);

        VideoCapture capture = new VideoCapture(selectedCamera);
        if (!capture.isOpened()) {
            System.out.println("Errore: impossibile aprire la webcam.");
            return;
        }


        //OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(1);
        
        //grabber.start();
        CanvasFrame canvas = new CanvasFrame("Webcam");
        canvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
        OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();
        
        //Aruco tag Detection
        while (canvas.isVisible() && capture.grab()) {
            Mat mat = new Mat();
            capture.retrieve(mat);

            // Lista di vettori di punti per i marker rilevati
            List<Mat> markerCorners = new ArrayList<>();
            Mat markerIds = new Mat();

            arucoDetector.detectMarkers(mat, markerCorners, markerIds);

            // Se sono stati rilevati marker, disegna i contorni
            if (!markerIds.empty()) {
                Objdetect.drawDetectedMarkers(mat, markerCorners, markerIds, new Scalar(0, 255, 0, 0));
            }

            canvas.showImage(converterToMat.convert(mat));
        }
        
        converterToMat.close();
        canvas.dispose();
    }
}
