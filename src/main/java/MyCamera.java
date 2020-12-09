import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.highgui.HighGui;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MyCamera {

    private enum HSVColor {
        LOWER_RED_1(0, 100, 100),
        UPPER_RED_1(15, 255, 255),

        LOWER_RED_2(160, 100, 100),
        UPPER_RED_2(180, 255, 255);

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
            // Get frame from camera
            int i = 0;

            List<Point> points = new ArrayList<Point>(1000);

            while (i++ < Integer.MAX_VALUE) {
                Mat capturedFrame = new Mat();
                videoDevice.read(capturedFrame);

//                Imgproc.resize(capturedFrame, capturedFrame, new Size(640, 480));
                Mat output = capturedFrame.clone();

                Size size = new Size(11, 11);
                Imgproc.GaussianBlur(
                        capturedFrame, capturedFrame,
                        size, 0, 0
                );

                Mat capturedFrameHSV = new Mat();
                Imgproc.cvtColor(capturedFrame, capturedFrameHSV, Imgproc.COLOR_BGR2HSV);

                Mat capturedFrameHSVRed1 = new Mat();
                Mat capturedFrameHSVRed2 = new Mat();
                Mat capturedFrameHSVRed = new Mat();
                Core.inRange(
                        capturedFrameHSV,
                        HSVColor.LOWER_RED_1.scalar,
                        HSVColor.UPPER_RED_1.scalar,
                        capturedFrameHSVRed1
                );
                Core.inRange(
                        capturedFrameHSV,
                        HSVColor.LOWER_RED_2.scalar,
                        HSVColor.UPPER_RED_2.scalar,
                        capturedFrameHSVRed2
                );
                Core.addWeighted(
                        capturedFrameHSVRed1,
                        1,
                        capturedFrameHSVRed2,
                        1,
                        0,
                        capturedFrameHSVRed
                );

                int iterations = 4;
                Imgproc.erode(
                        capturedFrameHSVRed, capturedFrameHSVRed,
                        new Mat(), new Point(-1, -1), iterations
                );
                Imgproc.dilate(
                        capturedFrameHSVRed, capturedFrameHSVRed,
                        new Mat(), new Point(-1, -1), iterations
                );

                List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
                Mat hierarchy = new Mat();
                Imgproc.findContours(
                        capturedFrameHSVRed,
                        contours,
                        hierarchy,
                        Imgproc.RETR_EXTERNAL,
                        Imgproc.CHAIN_APPROX_SIMPLE
                );

                int width = capturedFrame.width();
                int height = capturedFrame.height();

                Mat contourMat = new Mat(height, width, CvType.CV_8UC4);
                contourMat = Mat.zeros(capturedFrame.size(), CvType.CV_8UC3);
                Imgproc.drawContours(
                        contourMat,
                        contours,
                        -1,
                        new Scalar(255, 255, 255)
                );

                for (MatOfPoint contour: contours) {
                    Imgproc.fillPoly(
                            contourMat,
                            Collections.singletonList(contour),
                            new Scalar(255, 255, 255)
                    );
                }

                Mat gray = new Mat();
                Imgproc.cvtColor(contourMat, gray, Imgproc.COLOR_BGR2GRAY);

                HighGui.imshow("Mask", gray);

                Mat circles = new Mat();
                Imgproc.HoughCircles(
                        gray,
                        circles,
                        Imgproc.HOUGH_GRADIENT,
                        1,
                        500,
                        100,
                        13,
                        30,
                        160
                );

                double[] c = circles.get(0, 0);
                if (c != null) {
                    Point center = null;
                    Scalar color = RGBColor.GREEN.scalar;
                    int thickness = 3;
                    int maxRadius = 0;
                    for (int x = 0; x < circles.cols(); x++) {
                        // circle outline
                        int radius = (int) Math.round(c[2]);
                        if (radius > maxRadius) {
                            c = circles.get(0, x);
                            center = new Point(Math.round(c[0]), Math.round(c[1]));
                            maxRadius = radius;
                        }
                    }

                    if (center != null) {
                        Imgproc.circle(
                                output,
                                center,
                                maxRadius,
                                color,
                                thickness
                        );
                        points.add(center);
                    }
                }

//                for (int j = 1; j < points.size(); j++) {
//                    Point start = points.get(j);
//                    Point end = points.get(j - 1);
//                    line(output, start, end);
//                }
//                forget(points);

                HighGui.imshow("Image", output);
                HighGui.waitKey(1);
            }

            // Release video device
            videoDevice.release();
        } else {
            System.out.println("Error.");
        }
    }

    private static MatOfPoint to(MatOfPoint2f matOfPoint2f) {
        MatOfPoint point = new MatOfPoint();
        point.convertTo(matOfPoint2f, CvType.CV_32S);
        return point;
    }

    private static MatOfPoint2f to2f(MatOfPoint matOfPoint) {
        return new MatOfPoint2f(matOfPoint.toArray());
    }

    private static void forget(List<Point> points) {
        if (points.size() > 50) {
            points.remove(0);
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
