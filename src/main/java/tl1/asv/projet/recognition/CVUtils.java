package tl1.asv.projet.recognition;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_imgproc;

import static org.bytedeco.javacpp.opencv_imgcodecs.imread;

public class CVUtils {

    /**
     * Create oneimage and convert it to black and white.
     *
     * @param path
     * @return
     */
    public static OneImage createOneImage(String path) {
        Mat imread = imread(path);
        Mat convertedColor = new Mat(imread.size());
        opencv_imgproc.cvtColor(imread, convertedColor, opencv_imgproc.CV_BGR2GRAY);

        OneImage toReturn = new OneImage(path, convertedColor);

        /**
         * DETECT ALL POINTS
         */
        recoImage.detectPointsAndFeature(toReturn);

        return toReturn;
    }


    /**
     * Create image with name.
     * @param absolutePath
     * @param name
     * @return
     */
    public static OneImage createOneImage(String absolutePath, String name) {
        OneImage oneImage = createOneImage(absolutePath);
        oneImage.setName(name);
        return oneImage;
    }
}
