import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.highgui.HighGui;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Camera {

    private enum HSVColor {
        LOWER_RED_1(0, 100, 100),
        UPPER_RED_1(10, 255, 255),

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
            videoDevice.set(Videoio.CAP_PROP_FRAME_WIDTH, 600);
            videoDevice.set(Videoio.CAP_PROP_FRAME_HEIGHT, 400);
            // Get frame from camera
            int i = 0;

            List<Point> points = new ArrayList<Point>(1000);

            while (i++ < Integer.MAX_VALUE) {
                Mat capturedFrame = new Mat();
                videoDevice.read(capturedFrame);

                Mat output = capturedFrame.clone();

                Mat capturedFrameBgr = new Mat();
                Imgproc.cvtColor(capturedFrame, capturedFrameBgr, Imgproc.COLOR_BGRA2BGR);
                Imgproc.medianBlur(capturedFrameBgr, capturedFrameBgr, 3);
//                Size size = new Size(5, 5);
//                Imgproc.GaussianBlur(
//                        capturedFrameBgr, capturedFrameBgr,
//                        size, 2, 2
//                );

                //hsv, linear blur, контуры, трек по центрам и чистить его

                Mat capturedFrameHSV = new Mat();
                Imgproc.cvtColor(capturedFrameBgr, capturedFrameHSV, Imgproc.COLOR_BGR2HSV);

                Mat capturedFrameHSVRed1 = new Mat();
                Mat capturedFrameHSVRed2 = new Mat();
                Mat capturedFrameHSVRed = new Mat();
                Core.inRange(capturedFrameHSV, HSVColor.LOWER_RED_1.scalar, HSVColor.UPPER_RED_1.scalar, capturedFrameHSVRed1);
                Core.inRange(capturedFrameHSV, HSVColor.LOWER_RED_2.scalar, HSVColor.UPPER_RED_2.scalar, capturedFrameHSVRed2);
                Core.addWeighted(
                        capturedFrameHSVRed1,
                        1,
                        capturedFrameHSVRed2,
                        1,
                        0,
                        capturedFrameHSVRed
                );
//                Size size = new Size(5, 5);
//                Imgproc.GaussianBlur(
//                        capturedFrameHSVRed, capturedFrameHSVRed,
//                        size, 2, 2
//                );

                List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
                Mat hier = new Mat();
                Imgproc.findContours(
                        capturedFrameHSVRed,
                        contours,
                        hier,
                        Imgproc.RETR_TREE,
                        Imgproc.CHAIN_APPROX_SIMPLE
                );

//                for (MatOfPoint contour : contours) {
//                    System.out.println(contour);
//                }
//                System.out.println();

                double maxVal = 0;
                int maxValIdx = 0;
                for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++) {
                    double contourArea = Imgproc.contourArea(contours.get(contourIdx));
                    if (maxVal < contourArea) {
                        maxVal = contourArea;
                        maxValIdx = contourIdx;
                    }
                }

                if (contours.size() > 0) {
                    MatOfPoint maxContour = contours.get(maxValIdx);

                    Point center = new Point();
                    float[] rad = new float[1];
                    MatOfPoint2f contour = new MatOfPoint2f(maxContour.toArray());
                    Imgproc.minEnclosingCircle(contour, center, rad);
                    Rect rect = Imgproc.boundingRect(maxContour);
                    if (true) {
                        float rd = (int) (0.9 * rad[0]);
                        double circleArea = (float)Math.PI * rd * rd;
                        double rectArea = rect.width * rect.height;
                        double contourArea = Imgproc.contourArea(contour);
                        System.out.printf("%.2f %.2f %n", circleArea, contourArea);
                        int percent = 10;
                        boolean isCircle = Math.abs(circleArea - contourArea) > Math.abs(rectArea - contourArea);
                        if (!isCircle) {
                            Imgproc.circle(output, center, (int) rad[0], RGBColor.GREEN.scalar, 3);
                            points.add(center);
                        }
                    } else {
                        double epsilon = Imgproc.arcLength(contour, true);
                        MatOfPoint2f approximatedContour = new MatOfPoint2f();
                        Imgproc.approxPolyDP(contour, approximatedContour, 0.001 * epsilon, true);
//                        System.out.println(approximatedContour.toList().size());
                        if (approximatedContour.toList().size() > 5) {
                            if (true) {
                                List<MatOfPoint> approximated = new ArrayList<MatOfPoint>();
                                MatOfPoint appCtr = new MatOfPoint();
                                approximatedContour.convertTo(appCtr, CvType.CV_32S);
                                approximated.add(appCtr);
                                points.add(center);

                                float rd = (int) (0.9 * rad[0]);

                                double circleArea = (float)Math.PI * rd * rd;
                                double contourArea = Imgproc.contourArea(approximatedContour);

                                System.out.printf("%.2f %.2f %n", circleArea, contourArea);

                                int percent = 15;
                                boolean over = 100 * circleArea / contourArea > 100 + percent;

                                if(!over) {
                                    Imgproc.drawContours(
                                            output,
                                            approximated,
                                            0,
                                            RGBColor.GREEN.scalar,
                                            5
                                    );
                                }
                            } else {
                                center = new Point();
                                rad = new float[1];
                                contour = approximatedContour;
                                Imgproc.minEnclosingCircle(contour, center, rad);
                                Imgproc.circle(output, center, (int) (0.9 * rad[0]), RGBColor.GREEN.scalar, 3);
                                points.add(center);
                            }
                        }

//                        Imgproc.drawContours(output, contours, maxValIdx, RGBColor.GREEN.scalar, 5);
                    }
                }

//                Mat edges = new Mat();
//                Imgproc.Canny(capturedFrameHSVRed, edges, 1000, 1000);
//                HighGui.imshow("test222", edges);

                for (int j = 1; j < points.size(); j++) {
                    Point start = points.get(j);
                    Point end = points.get(j - 1);
                    line(output, start, end);
                }
                forget(points);

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