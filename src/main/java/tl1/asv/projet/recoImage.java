package tl1.asv.projet;

import static org.bytedeco.javacpp.opencv_core.NORM_L2;
import static org.bytedeco.javacpp.opencv_features2d.drawKeypoints;
import static org.bytedeco.javacpp.opencv_highgui.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.opencv_calib3d;
import org.bytedeco.javacpp.opencv_imgproc;
import org.bytedeco.javacpp.opencv_shape;
import org.bytedeco.javacpp.opencv_core.DMatch;
import org.bytedeco.javacpp.opencv_core.DMatchVector;
import org.bytedeco.javacpp.opencv_core.KeyPointVector;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.opencv_features2d.BFMatcher;
import org.bytedeco.javacpp.opencv_features2d.DrawMatchesFlags;
import org.bytedeco.javacpp.opencv_xfeatures2d.SIFT;

public class recoImage {

    // Chargement statique des librairies pour optimisation
    static {
        Loader.load(opencv_calib3d.class);
        Loader.load(opencv_shape.class);
    }

    static DMatchVector selectBest(DMatchVector matches, int numberToSelect) {
        DMatch[] sorted = toArray(matches);
        Arrays.sort(sorted, (a, b) -> {
            return a.lessThan(b) ? -1 : 1;
        });
        DMatch[] best = Arrays.copyOf(sorted, numberToSelect);
        return new DMatchVector(best);
    }

    static DMatch[] toArray(DMatchVector matches) {
        assert matches.size() <= Integer.MAX_VALUE;
        int n = (int) matches.size();
        //	Convert	keyPoints	to	Scalar	sequence
        DMatch[] result = new DMatch[n];
        for (int i = 0; i < n; i++) {
            result[i] = new DMatch(matches.get(i));
        }
        return result;
    }


    static SIFT sift;

    static {
        int nFeatures = 250;
        int nOctaveLayers = 5;
        double contrastThreshold = 0.03;
        int edgeThreshold = 10;
        double sigma = 1.6;


        sift = SIFT.create(nFeatures, nOctaveLayers, contrastThreshold, edgeThreshold, sigma);

    }

    /**
     * Obtient la distance entre deux matrices d'image.
     *
     * @param img1
     * @param img2
     * @return
     */
    static float getDist(Mat img1_original, Mat img2_original) {


        // BB conversion

        Mat img1 = new Mat(img1_original.size());
        opencv_imgproc.cvtColor(img1_original, img1, opencv_imgproc.CV_BGR2GRAY);

        Mat img2 = new Mat(img2_original.size());
        opencv_imgproc.cvtColor(img2_original, img2, opencv_imgproc.CV_BGR2GRAY);


        KeyPointVector kpv1 = new KeyPointVector();
        KeyPointVector kpv2 = new KeyPointVector();

        sift.detect(img1, kpv1);
        sift.detect(img2, kpv2);

        Mat feature1 = new Mat();
        Mat feature2 = new Mat();

        Mat desc1 = new Mat();
        Mat desc2 = new Mat();

        sift.compute(img1, kpv1, desc1);
        sift.compute(img2, kpv2, desc2);


        BFMatcher matcher = new BFMatcher(NORM_L2, false);
        DMatchVector match = new DMatchVector();
        matcher.match(desc1, desc2, match);

        /**
         * On sélectionne les 25 meilleurs points.
         */
        DMatchVector bestMatches = selectBest(match, 25);
        float moy = 0;
        for (int j = 0; j < bestMatches.size(); j++) {
            moy += bestMatches.get(j).distance();


            // removes all variables
            bestMatches.put(j, null);


        }
        moy = moy / bestMatches.size();


        // remove variables...
        kpv1 = null;
        kpv2 = null;
        img2 = null;
        img1 = null;
        img1_original = null;
        img2_original = null;
        System.gc();


        return moy;

    }

    private static void showImage(Mat img, boolean force) {
        namedWindow("r", WINDOW_AUTOSIZE);    //	Create	a	window	for	display.
        imshow("r", img);    //	Show	our	image	inside	it.
        waitKey(0);    //	Wait	for	a	keystroke	in	the	window
    }


    static Map<String, Float> sortByComparator(Map<String, Float> unsortMap, final boolean order) {

        List<Entry<String, Float>> list = new LinkedList<Entry<String, Float>>(unsortMap.entrySet());

        // Sorting the list based on values
        Collections.sort(list, (o1, o2) -> {
            if (order) {
                return o1.getValue().compareTo(o2.getValue());
            } else {
                return o2.getValue().compareTo(o1.getValue());

            }
        });

        // Maintaining insertion order with the help of LinkedList
        Map<String, Float> sortedMap = new LinkedHashMap<String, Float>();
        for (Entry<String, Float> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }

}
