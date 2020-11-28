import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.highgui.HighGui;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

public class Camera {
    public static void main(String[] args) {
        // Load Native Library
        OpenCV.loadShared();
        // image container object
        // Video device acces
        VideoCapture videoDevice = new VideoCapture();
        // 0:Start default video device 1,2 etc video device id
        videoDevice.open(0);
        // is contected
        if (videoDevice.isOpened()) {
            videoDevice.set(Videoio.CAP_PROP_FRAME_WIDTH, 600);
            videoDevice.set(Videoio.CAP_PROP_FRAME_HEIGHT, 400);
            // Get frame from camera
            int i = 0;
            while (i++ < Integer.MAX_VALUE) {
                Mat capturedImage = new Mat();
                videoDevice.read(capturedImage);

                Mat output = capturedImage.clone();

                Mat capturedFrameBgr = new Mat();
                Imgproc.cvtColor(capturedImage, capturedFrameBgr, Imgproc.COLOR_BGRA2BGR);
                Imgproc.medianBlur(capturedFrameBgr, capturedFrameBgr, 3);

                Mat capturedFrameLab = new Mat();
                Imgproc.cvtColor(capturedFrameBgr, capturedFrameLab, Imgproc.COLOR_BGR2Lab);

                Mat capturedFrameLabRed = new Mat();
                Scalar scalar1 = new Scalar(20, 150, 150);
                Scalar scalar2 = new Scalar(190, 255, 255);
                Core.inRange(capturedFrameLab, scalar1, scalar2, capturedFrameLabRed);
                Size size = new Size(5, 5);
                Imgproc.GaussianBlur(
                        capturedFrameLabRed, capturedFrameLabRed,
                        size, 2, 2
                );

                Mat circles = new Mat();
                Imgproc.HoughCircles(
                        capturedFrameLabRed,
                        circles,
                        Imgproc.HOUGH_GRADIENT,
                        1,
                        (float) capturedFrameLabRed.dims() / 8,
                        100,
                        18,
                        5,
                        60
                );

                for (int x = 0; x < circles.cols(); x++) {
                    double[] c = circles.get(0, x);
                    Point center = new Point(Math.round(c[0]), Math.round(c[1]));
                    Scalar color = new Scalar(0, 255, 0);
                    int thickness = 3;
                    // circle outline
                    int radius = (int) Math.round(c[2]);
                    Imgproc.circle(output, center, radius, color, thickness, 8, 0);
                }

                // image array
                HighGui.imshow("test", output);
                HighGui.waitKey(1);
            }

            // Release video device
            videoDevice.release();
        } else {
            System.out.println("Error.");
        }
    }
}