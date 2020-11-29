import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.highgui.HighGui;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.util.ArrayList;
import java.util.List;

public class Camera {

    private enum HSVColor {
        RED_1(20, 150, 150),
        RED_2(190, 255, 255);

        private Scalar scalar;

        HSVColor(double hue, double saturation, double value) {
            this.scalar = new Scalar(hue, saturation, value);
        }
    }

    private enum RGBColor {
        RED(255, 0, 0),
        GREEN(0, 255, 0),
        YELLOW(255, 242, 0),
        PURPLE(202, 0, 202);

        private Scalar scalar;

        RGBColor(double red, double green, double blue) {
            this.scalar = new Scalar(blue, green, red);
        }
    }

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

            List<Point> points1 = new ArrayList<Point>(1000);
            List<Point> points2 = new ArrayList<Point>(1000);

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
                Scalar scalar1 = HSVColor.RED_1.scalar;
                Scalar scalar2 = HSVColor.RED_2.scalar;
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
                        20,
                        600
                );

                double[] c = circles.get(0, 0);
                if (c != null) {
                    Point center = new Point(Math.round(c[0]), Math.round(c[1]));
                    Scalar color = RGBColor.GREEN.scalar;
                    int thickness = 3;
                    int maxRadius = 0;
                    for (int x = 0; x < circles.cols(); x++) {
                        // circle outline
                        int radius = (int) Math.round(c[2]);
                        if (radius > maxRadius) {
                            c = circles.get(0, x);
                            center = new Point(Math.round(c[0]), Math.round(c[1]));
                            color = RGBColor.GREEN.scalar;
                            maxRadius = radius;
                        }
                    }
                    Imgproc.circle(output, center, maxRadius, color, thickness, 8, 0);
                    Point point1 = new Point(center.x, center.y - maxRadius);
                    Point point2 = new Point(center.x, center.y + maxRadius);
                    points1.add(point1);
                    points2.add(point2);
                }

                for (int j = 1; j < points1.size(); j++) {
                    Point start = points1.get(j);
                    Point end = points1.get(j - 1);
                    line(output, start, end);
                }

                for (int j = 1; j < points2.size(); j++) {
                    Point start = points2.get(j);
                    Point end = points2.get(j - 1);
                    line(output, start, end);
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

    private static void line(Mat img, Point start, Point end) {
        int thickness = 3;
        int lineType = 8;
        int shift = 0;
        Imgproc.line(img,
                start,
                end,
                RGBColor.YELLOW.scalar,
                thickness,
                lineType,
                shift);
    }
}