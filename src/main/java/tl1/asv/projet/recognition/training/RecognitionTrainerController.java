package tl1.asv.projet.recognition.training;


import org.bytedeco.javacpp.indexer.FloatRawIndexer;
import org.bytedeco.javacpp.*;
import org.bytedeco.javacpp.opencv_core.KeyPointVector;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_features2d.BOWImgDescriptorExtractor;
import tl1.asv.projet.recognition.general.Classifier;
import tl1.asv.vocabulary.Brand;
import tl1.asv.vocabulary.References;

import java.io.File;
import java.util.List;
import java.util.TreeMap;

import static org.bytedeco.javacpp.opencv_core.NORM_L2;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;
import static org.bytedeco.javacpp.opencv_imgcodecs.imwrite;
import static tl1.asv.projet.Config.SERVER_REFERENCES_FOLDER;

public class RecognitionTrainerController {


    static opencv_features2d.BFMatcher matcher;

    private static final opencv_xfeatures2d.SIFT sift;

    // keep ordered classifiers
    private TreeMap<String, Classifier> classifiers = new TreeMap<>();
    private final static Mat vocabulary;

    static {


        int nFeatures = 0;
        int nOctaveLayers = 5;
        double contrastThreshold = 0.03;
        double edgeThreshold = 10;
        double sigma = 1.6;


        sift = opencv_xfeatures2d.SIFT.create(nFeatures, nOctaveLayers, contrastThreshold, edgeThreshold, sigma);
        matcher = new opencv_features2d.BFMatcher(NORM_L2, false);

        // classifiers
        vocabulary = loadVocabulary();

    }


    private static Mat loadVocabulary() {

        File fileP = new File(References.DIRECTORY + "/" + References.getSingleton().getVocabulary());
        opencv_core.FileStorage loader = new opencv_core.FileStorage(fileP.getAbsolutePath(), opencv_core.FileStorage.READ);
        Mat mat = loader.get("vocabulary").mat();
        loader.close();

        return mat;
    }


    public String analyse(String filepath) throws Exception {


        if (vocabulary == null) {
            throw new Exception("Vocabulary is null.");
        }

        List<Brand> classPath = References.getSingleton().getBrands();


        /**
         * Analyse de l'image actuelle
         */
        BOWImgDescriptorExtractor extractor = new BOWImgDescriptorExtractor(sift, new opencv_features2d.FlannBasedMatcher());
        extractor.setVocabulary(vocabulary);

        //Prediction
        System.out.println("Predicting file " + filepath);
        Mat testImg = imread(filepath, opencv_imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);

        /*
        400 * x = 700
        x = 700 / 400

        hori

         */

        int wantedHeight = 700;
        float div = (wantedHeight * 1.0f) / testImg.size().height();

        float width = div * testImg.size().width();

        System.out.println("Resizing to " + width + ":" + wantedHeight);
        opencv_imgproc.resize(testImg, testImg, new opencv_core.Size((int) width, (int) wantedHeight));

        /**
         * Should save the new image with new size
         */
        imwrite(filepath,testImg);
        System.out.println("Resizing image");


        KeyPointVector keypoints = new KeyPointVector();

        System.out.println("Detecting features");
        sift.detect(testImg, keypoints);
        System.out.println("Calculate words frequencies's histogram");

        Mat histo = new Mat();
        extractor.compute(testImg, keypoints, histo, new opencv_core.IntVectorVector(), new Mat());


        float minF = Float.MAX_VALUE;
        String bestMatch = "IP";
        for (Brand brand : classPath) {


            try {
                opencv_ml.SVM svm;


                System.out.println("Loading classifier: " + brand.getClassifier());
                svm = opencv_ml.SVM.load(References.DIRECTORY + "/classifiers/" + brand.getClassifier());

                Mat retM = new Mat();

                svm.setKernel(opencv_ml.SVM.RBF);
                svm.setType(opencv_ml.SVM.C_SVC);

                float ret = svm.predict(histo, retM, 1);

                if (retM != null && retM.cols() > 0 && retM.rows() > 0) {
                    FloatRawIndexer indexer = retM.createIndexer();
                    ret = indexer.get(0, 0); //R�cup�ration de la valeur dans la MAT

                }
                if (ret < minF) {
                    minF = ret;
                    bestMatch = brand.getBrandname();
                }
                System.out.println("Prediction for class " + brand.getBrandname() + " : " + ret);
            } catch (Exception ex) {
                System.out.println("ERROR FOR PREDICTION FROM " + brand.getBrandname());
                // ex.printStackTrace();
            }
        }


        return bestMatch;

    }


    /**
     * List all files in a directory.
     *
     * @return
     */
    private File[] listImages() {
        File dir = new File(SERVER_REFERENCES_FOLDER);
        return dir.listFiles();
    }

}
