package tl1.asv.projet.recognition;


import org.bytedeco.javacpp.*;
import org.bytedeco.javacpp.opencv_core.FileStorage;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_features2d.BOWImgDescriptorExtractor;
import org.bytedeco.javacpp.opencv_xfeatures2d.SIFT;
import org.opencv.core.Core;
import org.opencv.ml.CvSVM;
import tl1.asv.vocabulary.Brand;
import tl1.asv.vocabulary.References;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;

import static org.bytedeco.javacpp.opencv_core.CV_STORAGE_READ;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;
import static tl1.asv.projet.Config.SERVER_REFERENCES_FOLDER;

public class RecognitionAnalyseController {

    static {
        Loader.load(opencv_calib3d.class);
        Loader.load(opencv_shape.class);
        Loader.load(opencv_ml.class);
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

        //create a matcher with FlannBased Euclidien distance (possible also with BruteForce-Hamming)
        final opencv_features2d.FlannBasedMatcher matcher;
        matcher = new opencv_features2d.FlannBasedMatcher();

        //create BoF (or BoW) descriptor extractor
        final BOWImgDescriptorExtractor bowide;
        bowide = new BOWImgDescriptorExtractor(detector, matcher);

        //Set the dictionary with the vocabulary we created in the first step
        bowide.setVocabulary(vocabulary);
        System.out.println("Vocab is set");

        return bowide;

    }


    private void
    lastCode(SIFT detector, BOWImgDescriptorExtractor bowide){

        References singleton = References.getSingleton();

        int classNumber = singleton.getBrands().size();
        String[] class_names;
        class_names = new String[classNumber];

        int i_=0;
        for (Brand brand : singleton.getBrands()) {
            class_names[i_++] = brand.getClassifier();
        }


        Loader.load(opencv_ml.class);

        final CvSVM[] classifiers;
        classifiers = new CvSVM [classNumber];
        for (int i = 0 ; i < classNumber ; i++) {
            //System.out.println("Ok. Creating class name from " + className);
            //open the file to write the resultant descriptor
            String finalPath = "etc/" + class_names[i] ;
            System.out.println("Loading ... " + finalPath);

            FileStorage fs = new FileStorage();
            boolean open = fs.open(finalPath, CV_STORAGE_READ);

            classifiers[i] = new CvSVM();
            classifiers[i].load(finalPath);
        }

        Mat response_hist = new Mat();
        opencv_core.KeyPoint keypoints = new opencv_core.KeyPoint();
        Mat inputDescriptors = new Mat();


        opencv_core.MatVector imagesVec;

        File root = new File("etc/refs");
        FilenameFilter imgFilter = (dir, name) -> {
            name = name.toLowerCase();
            return name.endsWith(".jpg") || name.endsWith(".pgm") || name.endsWith(".png");
        };

        File[] imageFiles = root.listFiles(imgFilter);

        imagesVec = new opencv_core.MatVector(imageFiles.length);

        //  Mat labels = new Mat(imageFiles.length, 1, CV_32SC1);
        //  IntBuffer labelsBuf = labels.createBuffer();


        for (File im : imageFiles) {

            //System.out.println("path:" + im.getName());

            Mat imageTest = imread(im.getAbsolutePath(), 1);
          //  detector.detectAndCompute(imageTest, Mat.EMPTY, keypoints, inputDescriptors);

            opencv_core.KeyPointVector keyPointVector = new opencv_core.KeyPointVector();
            detector.detect(imageTest,keyPointVector);
            detector.compute(imageTest,keyPointVector,inputDescriptors);


            bowide.compute(imageTest,keyPointVector,response_hist);

        //    bowide.compute(imageTest, keypoints, response_hist);

            // Finding best match
            float minf = Float.MAX_VALUE;
            String bestMatch = null;

            long timePrediction = System.currentTimeMillis();
            // loop for all classes
            for (int i = 0; i < classNumber; i++) {
                float res = 0f;
                // classifier prediction based on reconstructed histogram
            //    float res = classifiers[i].predict(response_hist, true);


                //classifiers[i].predict(response_hist,true);

                //System.out.println(class_names[i] + " is " + res);
                if (res < minf) {
                    minf = res;
                    bestMatch = class_names[i];
                }
            }
            timePrediction = System.currentTimeMillis() - timePrediction;
            System.out.println(im.getName() + "  predicted as " + bestMatch + " in " + timePrediction + " ms");

        }

        return;


    }





    public String analyse(String filepath){


        Mat vocabulary = loadVocabulary();
        BOWImgDescriptorExtractor bowide = createBowDescriptor(recoImage.sift, vocabulary);

        lastCode(recoImage.sift,bowide);

        return "IP";
    }





    public String analyse(String filepath, boolean notused) {
        HashMap<OneImage, Float> dists = new HashMap<>();
        HashMap<String, Float> categoryDistance = new HashMap<>();

        OneImage testImage = CVUtils.createOneImage(filepath);


        /**
         * On teste les distances entre l'image de test et les images de références.
         */
        for (File f : listImages()) {
            if (!f.isDirectory()) {
                OneImage refImage = CVUtils.createOneImage(f.getAbsolutePath(), f.getName());

                float resultMoy = recoImage.getDist(testImage, refImage);

                dists.put(refImage, resultMoy);
                System.out.println("Result for: " + f.getName() + "\t" + resultMoy);
            }
        }

        /**
         *
         */
        Set<Map.Entry<OneImage, Float>> sortedSet = dists.entrySet();

        Iterator<Map.Entry<OneImage, Float>> testPrint = sortedSet.iterator();
        while (testPrint.hasNext()) {
            Map.Entry<OneImage, Float> next = testPrint.next();
            System.out.println("next: " + next.getKey().getPath() + ":" + next.getValue());
        }

        Iterator<Map.Entry<OneImage, Float>> it = sortedSet.iterator();
        int kMatch = sortedSet.size();

        // parcours des 7 premières je crois...
        for (int i = 0; i < kMatch; i++) {
            Map.Entry<OneImage, Float> item = it.next();
            String classifierName = item.getKey().getName().split("_")[0];

            if (!classifiers.containsKey(classifierName)) {
                classifiers.put(classifierName, new Classifier(classifierName));

            }


            // Gets classifier, add a reference image and it's distance from test image
            Classifier classifier = classifiers.get(classifierName);
            classifier.getReferences().add(item.getKey());
            classifier.addDistance(item.getValue());

        }


        float lowest = -1;
        String mostConfident = "NOK";

        // print all distances
        for (String key :
                classifiers.keySet()) {
            float val = classifiers.get(key).getDistance();

            int nbReferences = classifiers.get(key).getReferences().size();

            System.out.println("Original Value: " + key + ":" + val + " /" + nbReferences);
            val /= nbReferences; // moyenne par le nombre d'entrée référencée
            categoryDistance.put(key, val);
            System.out.println("Final Value: " + key + ":" + val);


            // set as the lowest if needed
            if (lowest == -1 || lowest > val) {
                lowest = val;
                mostConfident = key;
                System.out.println("Becoming new lowest");
            }

        }




        System.out.println("Most confident: " + mostConfident);

        return mostConfident;
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
