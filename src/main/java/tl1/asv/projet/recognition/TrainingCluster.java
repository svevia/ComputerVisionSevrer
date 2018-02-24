package tl1.asv.projet.recognition;

import org.bytedeco.javacpp.*;
import org.bytedeco.javacpp.indexer.FloatRawIndexer;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.math.BigInteger;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class TrainingCluster {

    private JSONObject indexJson;
    private String vocabularyDir = "etc/";
    private Mat vocabulary;
    private File rootDir = new File("etc/refs");




    int nFeatures = 0;
    int nOctaveLayers = 5;
    double contrastThreshold = 0.03;
    double edgeThreshold = 10;
    double sigma = 1.6;
    private int maxWords=200;
    private String classifierDir = "etc/classifiers";


    public String getHashMd5(String path) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
            File inputFile = new File(path);
            InputStream is = new FileInputStream(inputFile);
            DigestInputStream dis = new DigestInputStream(is, md);
            byte[] digest = md.digest();
            BigInteger bigInt = new BigInteger(1, digest);
            String hashtext = bigInt.toString(16);
            return hashtext;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "";
    }

    private Mat buildVocabulary() {

        File voc = new File(this.vocabularyDir + "/vocab.yml");
        if (voc.exists()) {
            opencv_core.FileStorage loader = new opencv_core.FileStorage(voc.getAbsolutePath(), opencv_core.FileStorage.READ);
            this.vocabulary = loader.get("vocabulary").mat();
            loader.close();
            System.out.println("Vocabulaire chargé !");
        } else if (rootDir != null) {
            File[] imagesTrain = rootDir.listFiles();
            opencv_core.TermCriteria term = new opencv_core.TermCriteria();
            term.type(opencv_core.TermCriteria.MAX_ITER);
            term.epsilon(0.0001);
            term.maxCount(100);
            opencv_features2d.BOWKMeansTrainer trainer = new opencv_features2d.BOWKMeansTrainer(this.maxWords, term, 1, opencv_core.KMEANS_RANDOM_CENTERS);
            int i = 0;
            for (File imgTrain : imagesTrain) {
                Mat trainMat = opencv_imgcodecs.imread(imgTrain.getAbsolutePath(), opencv_imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
                opencv_core.KeyPointVector keypoints = new opencv_core.KeyPointVector();
                Mat descriptor = new Mat();
                opencv_xfeatures2d.SIFT sift = opencv_xfeatures2d.SIFT.create(nFeatures, nOctaveLayers, contrastThreshold, edgeThreshold, sigma);
                sift.detect(trainMat,keypoints);
                sift.compute(trainMat, keypoints,descriptor);
                trainer.add(descriptor);
                System.out.println("Train Vocabulary " + (i + 1) + " => " + imgTrain.getName());
                i++;
            }
            this.vocabulary = trainer.cluster();

            opencv_core.FileStorage ds = new opencv_core.FileStorage(this.vocabularyDir + "/vocab.yml", opencv_core.FileStorage.WRITE);
            ds.write("vocabulary", this.vocabulary);
            ds.close();
            return this.vocabulary;
        }
        return null;
    }

    private void showMat(Mat m) {

        FloatRawIndexer ind = m.createIndexer();
        for (int rows = 0; rows < m.rows(); rows++) {
            for (int cols = 0; cols < m.cols(); cols++) {
                System.out.print(ind.get(rows, cols));
            }
            System.out.println("");
        }
    }

    public void train() {
        this.indexJson = new JSONObject();
        try {
            indexJson.put("vocabulary", "vocab.yml");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        buildVocabulary();
        showMat(this.vocabulary);
        Mat samples = new Mat();
        Mat histo = new Mat();
        Mat trainMat;
        String class_name = "";
        opencv_xfeatures2d.SIFT sift = opencv_xfeatures2d.SIFT.create(nFeatures, nOctaveLayers, contrastThreshold, edgeThreshold, sigma);
        opencv_features2d.BOWImgDescriptorExtractor extractor = new opencv_features2d.BOWImgDescriptorExtractor(sift, new opencv_features2d.FlannBasedMatcher());
        extractor.setVocabulary(this.vocabulary);
        File classLocation = null;
        for (File trainImg : this.rootDir.listFiles()) {
            if (!class_name.equals(trainImg.getName().split("_")[0])) {
                class_name = trainImg.getName().split("_")[0];
                classLocation = new File(this.classifierDir + "/" + class_name + ".xml");
            }

            if (classLocation != null && classLocation.exists()) {
                System.out.println("Passing " + trainImg.getName() + ", class already exist in directory");
            } else {
                trainMat = opencv_imgcodecs.imread(trainImg.getAbsolutePath(), opencv_imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);

                opencv_core.KeyPointVector keypoints = new opencv_core.KeyPointVector();

                sift.detect(trainMat, keypoints);

                System.out.println("Computing words for " + trainImg.getName());
                extractor.compute(trainMat, keypoints, histo, new opencv_core.IntVectorVector(), new Mat());

                samples.push_back(histo);
            }
        }

        //Fabrication des SVM avec les labels
        int globalIndex = 0;
        int currentSvm = 0;
        int indexStart = 0;
        int indexStop = samples.rows();
        class_name = "";
        JSONArray jsonArrayTmp = new JSONArray();
        int[] resp = new int[samples.rows()];
        for (File trainImg : this.rootDir.listFiles()) {

            if (globalIndex != 0 && (!class_name.equals(trainImg.getName().split("_")[0])
                    || globalIndex == this.rootDir.listFiles().length - 1)) {
                classLocation = new File(this.classifierDir + "/" + class_name + ".xml");
                if (classLocation.exists()) {
                    System.out.println("Existing SVM for classe " + class_name);
                    //Création objet
                    try {
                        JSONObject tmpObj = new JSONObject();
                        tmpObj.put("brandname", class_name);
                        tmpObj.put("url", "");
                        tmpObj.put("classifier", class_name + ".xml");
                        jsonArrayTmp.put(tmpObj);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("Save SVM for classe " + class_name);
                    indexStop = globalIndex;
                    if (globalIndex == this.rootDir.listFiles().length - 1) indexStop = resp.length;
                    for (int j = 0; j < resp.length; j++) {
                        if (j >= indexStart && j < indexStop) {
                            resp[j] = 1;
                        } else resp[j] = 0;
                    }
                    indexStart = indexStop;
                    IntPointer pointerInt = new IntPointer(resp);
                    Mat labels = new Mat(pointerInt);

                    opencv_ml.SVM svm = opencv_ml.SVM.create();
                    svm.setKernel(opencv_ml.SVM.RBF);
                    svm.setType(opencv_ml.SVM.C_SVC);
                    svm.train(samples, opencv_ml.ROW_SAMPLE, labels);
                    svm.save(this.classifierDir + "/" + class_name + ".xml");

                    //Création objet
                    try {
                        JSONObject tmpObj = new JSONObject();
                        tmpObj.put("brandname", class_name);
                        tmpObj.put("url", "");
                        tmpObj.put("classifier", class_name + ".xml");
                        jsonArrayTmp.put(tmpObj);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            }
            if (!class_name.equals(trainImg.getName().split("_")[0])) {
                class_name = trainImg.getName().split("_")[0];
                System.out.println("Changing Train class " + class_name);
            }

            globalIndex++;
        }
        try {
            indexJson.put("brands", jsonArrayTmp);
            String hash = "";
            File vocab = new File(this.vocabularyDir + "/vocab.yml");
            if (vocab.exists()) {
                System.out.println("Signing with md5");
                hash = this.getHashMd5(vocab.getAbsolutePath());
                this.indexJson.put("vocab_hash", hash);
                File fileIndex = new File(this.vocabularyDir + "\\index.json");
                FileWriter fw = new FileWriter(fileIndex);
                fw.write(this.indexJson.toString());
                fw.close();
            } else {
                System.out.println("Error when finalizing, please clear directory data and try again");
            }

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
