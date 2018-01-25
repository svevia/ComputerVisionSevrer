package tl1.asv.projet.recognition;


import org.bytedeco.javacpp.*;
import org.bytedeco.javacpp.opencv_core.FileStorage;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_features2d.BOWImgDescriptorExtractor;
import org.bytedeco.javacpp.opencv_features2d.FlannBasedMatcher;
import org.bytedeco.javacpp.opencv_features2d.KeyPoint;
import org.bytedeco.javacpp.opencv_ml.CvSVM;
import org.bytedeco.javacpp.opencv_nonfree.SIFT;

import tl1.asv.vocabulary.Brand;
import tl1.asv.vocabulary.References;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;

import static org.bytedeco.javacpp.opencv_core.CV_STORAGE_READ;
import static org.bytedeco.javacpp.opencv_core.NORM_L2;
import static tl1.asv.projet.Config.SERVER_REFERENCES_FOLDER;

public class RecognitionAnalyseController {


    static SIFT sift;
    static opencv_features2d.BFMatcher matcher;

    static {
        Loader.load(opencv_core.class);


        int nFeatures = 250;
        int nOctaveLayers = 5;
        double contrastThreshold = 0.03;
        int edgeThreshold = 10;
        double sigma = 1.6;


        sift = new SIFT(nFeatures, nOctaveLayers, contrastThreshold, edgeThreshold, sigma);
        matcher = new opencv_features2d.BFMatcher(NORM_L2, false);
    }



    // keep ordered classifiers
    private TreeMap<String, Classifier> classifiers = new TreeMap<>();



    private Mat loadVocabulary(){

        //final String pathToVocabulary = "vocabulary.yml" ; // to be define
        final Mat vocabulary;

        opencv_core.CvFileStorage storage = opencv_core.cvOpenFileStorage("etc/"+References.getSingleton().getVocabulary(), null, CV_STORAGE_READ);
        Pointer p = opencv_core.cvReadByName(storage, null, "vocabulary", opencv_core.cvAttrList());
        opencv_core.CvMat cvMat = new opencv_core.CvMat(p);
        vocabulary = new Mat(cvMat);
        System.out.println("vocabulary loaded " + vocabulary.rows() + " x " + vocabulary.cols());
        opencv_core.cvReleaseFileStorage(storage);

        return vocabulary;
    }


    private BOWImgDescriptorExtractor createBowDescriptor(SIFT detector, Mat vocabulary){

        FlannBasedMatcher FBMatcher = new FlannBasedMatcher();  // Create a Matcher with FlannBase Euclidien distance. Used to find the nearest word of the trained vocabulary for each keypoint descriptor of the image
        opencv_features2d.DescriptorExtractor DExtractor = sift.asDescriptorExtractor(); // Descriptor extractor that is used to compute descriptors for an input image and its keypoints.
        BOWImgDescriptorExtractor BOWDescriptor = new BOWImgDescriptorExtractor(DExtractor, FBMatcher); // Minimal constructor
        //Set the dictionary with the vocabulary we created in the first step
        BOWDescriptor.setVocabulary(vocabulary);

        System.out.println("Vocab is set");

        return BOWDescriptor;

    }


    private void  lastCode(SIFT detector, BOWImgDescriptorExtractor bowide){


        /**
         * truc muche
         */


        Mat imageDescriptor = new Mat();
        KeyPoint keyPoints = new KeyPoint();
        Mat inputDescriptors = new Mat();


    /*    String photoTest = GlobalTools.toCache(context, testedImagePath , "Pepsi_13.jpg").getAbsolutePath();

        Mat imageTest = GlobalTools.loadImg3ChannelColor(photoTest); // RGB image matrix


*/




        References singleton = References.getSingleton();

        int classNumber = singleton.getBrands().size();
        CvSVM[] class_names;
        class_names = new CvSVM[classNumber];

        int i_=0;
        for (Brand brand : singleton.getBrands()) {
            //class_names[i_++] = brand.getClassifier();
            final String finalPath = "etc/" + class_names[i_] ;

            class_names[i_] = new CvSVM();
            class_names[i_++].load(finalPath);
            System.out.println("Loading ... " + finalPath);
        }




        Mat response_hist = new Mat();


        File root = new File("etc/refs");
        FilenameFilter imgFilter = (dir, name) -> {
            name = name.toLowerCase();
            return name.endsWith(".jpg") || name.endsWith(".pgm") || name.endsWith(".png");
        };

        File[] imageFiles = root.listFiles(imgFilter);

        opencv_core.MatVector imagesVec = new opencv_core.MatVector(imageFiles.length);


        for (File im : imageFiles) {

            System.out.println("path:" + im.getName());

          //  Mat imageTest = imread(im.getAbsolutePath(), 1);

         /*   opencv_core.KeyPointVector keyPointVector = new opencv_core.KeyPointVector();
            detector.detect(imageTest,keyPointVector);

            System.out.println("kp: "+ keyPointVector.size());


            detector.compute(imageTest,keyPointVector,inputDescriptors);


            bowide.compute(imageTest,keyPointVector,response_hist);

            System.out.println("Hist: "+ response_hist.cols() +"*"+response_hist.rows());


        //    bowide.compute(imageTest, keypoints, response_hist);

            // Finding best match
            float minf = Float.MAX_VALUE;
            String bestMatch = null;

            long timePrediction = System.currentTimeMillis();
            // loop for all classes
            for (int i = 0; i < classNumber; i++) {
                // classifier prediction based on reconstructed histogram
                float res = classifiers[i].predict(response_hist);


                System.out.println(class_names[i] + " is " + res);
                if (res < minf) {
                    minf = res;
                    bestMatch = class_names[i];
                }
            }
            timePrediction = System.currentTimeMillis() - timePrediction;
            System.out.println(im.getName() + "  predicted as " + bestMatch + " in " + timePrediction + " ms");
*/


        }

        return;


    }





    public String analyse(String filepath){


        Mat vocabulary = loadVocabulary();
        BOWImgDescriptorExtractor bowide = createBowDescriptor(sift, vocabulary);

        lastCode(sift,bowide);

        return "IP";
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
