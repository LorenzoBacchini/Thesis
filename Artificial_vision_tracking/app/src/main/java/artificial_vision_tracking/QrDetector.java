package artificial_vision_tracking;

import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.javacv.Frame;
import org.bytedeco.opencv.opencv_objdetect.QRCodeDetector;
import org.bytedeco.javacpp.BytePointer;

public class QrDetector {
    public void detect() throws FrameGrabber.Exception, InterruptedException {
        OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(1);
        grabber.start();
        CanvasFrame canvas = new CanvasFrame("Webcam");
        canvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
        QRCodeDetector qrCodeDetector = new QRCodeDetector();
        OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();

        while (canvas.isVisible() && (grabber.grab()) != null) {
            Frame frame = grabber.grab();


            Mat mat = converterToMat.convert(frame);
          
            Mat points = new Mat();
            BytePointer bytePointer = qrCodeDetector.detectAndDecode(mat); 
          
            if (bytePointer!=null && !bytePointer.toString().isEmpty()) {
                String decodedText = bytePointer.getString();
                System.out.println("QR Code detected: " + decodedText);
                for (int i = 0; i < points.size().height(); i++) {
                    Rect rect = new Rect(points.ptr(i));
                    opencv_imgproc.rectangle(mat, rect, new Scalar(0, 255, 0, 0));
                }
            }
            canvas.showImage(grabber.grab());
            points.close();
        }
        grabber.stop();
        grabber.close();
        qrCodeDetector.close();
        converterToMat.close();
        canvas.dispose();
    }
}