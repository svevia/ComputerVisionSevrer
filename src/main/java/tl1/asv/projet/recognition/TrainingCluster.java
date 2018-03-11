package tl1.asv.projet.recognition;

import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.sun.org.apache.xpath.internal.operations.And;
import org.bytedeco.javacpp.*;
import org.bytedeco.javacpp.indexer.FloatRawIndexer;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_features2d.BOWKMeansTrainer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import tl1.asv.projet.Config;
import tl1.asv.projet.db.ClientsDatabase;

import java.io.*;
import java.math.BigInteger;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

public class TrainingCluster {

    private JSONObject indexJson;
    private String vocabularyDir = "etc/";
    private Mat vocabulary;
    //private File rootDir = new File("datasets/processedRefs");
    private File rootDir = new File("etc/refs");


    int nFeatures = 0;
    int nOctaveLayers = 5;
    double contrastThreshold = 0.03;
    double edgeThreshold = 10;
    double sigma = 1.6;
    private int maxWords = 50;
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

        File voc = new File(this.vocabularyDir + "/vocabulary.yml");
        if (voc.exists()) {
            opencv_core.FileStorage loader = new opencv_core.FileStorage(voc.getAbsolutePath(), opencv_core.FileStorage.READ);
            this.vocabulary = loader.get("vocabulary").mat();
            loader.close();
            System.out.println("Vocabulaire chargé !");

        } else if (rootDir != null) {
            opencv_core.TermCriteria term = new opencv_core.TermCriteria();
            term.type(opencv_core.TermCriteria.MAX_ITER);
            term.epsilon(0.0001);
            term.maxCount(250);
            BOWKMeansTrainer trainer = new BOWKMeansTrainer(this.maxWords, term, 1, opencv_core.KMEANS_RANDOM_CENTERS);
            int i = 0;
            File[] imagesTrain = rootDir.listFiles();
            Arrays.sort(imagesTrain);

            if (imagesTrain == null) {
                System.out.println("Error...");
                return null;
            }
            for (File imgTrain : imagesTrain) {
                if (!imgTrain.isFile()) {
                    continue;
                }
                Mat trainMat = opencv_imgcodecs.imread(imgTrain.getAbsolutePath(), opencv_imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
                opencv_core.KeyPointVector keypoints = new opencv_core.KeyPointVector();
                Mat descriptor = new Mat();
                opencv_xfeatures2d.SIFT sift = opencv_xfeatures2d.SIFT.create(nFeatures, nOctaveLayers, contrastThreshold, edgeThreshold, sigma);
                sift.detect(trainMat, keypoints);
                sift.compute(trainMat, keypoints, descriptor);

                //
                if (descriptor.rows() > 0 && descriptor.cols() > 0) {
                    trainer.add(descriptor);
                    System.out.println("Train Vocabulary " + (i + 1) + " => " + imgTrain.getName());

                } else {
                    System.out.println(imgTrain.getName() + " not working...");
                }

                i++;
            }

            System.out.println("Clustering now..");
            this.vocabulary = trainer.cluster();


            opencv_core.FileStorage ds = new opencv_core.FileStorage(this.vocabularyDir + "/vocabulary.yml", opencv_core.FileStorage.WRITE);
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

        try {
            checkLockfile();
            createLockfile();
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            return;
        }


        this.indexJson = new JSONObject();
        try {
            indexJson.put("vocabulary", "vocabulary.yml");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        buildVocabulary();
        Mat samples = new Mat();
        Mat histo = new Mat();
        Mat trainMat;
        String class_name = "";
        opencv_xfeatures2d.SIFT sift = opencv_xfeatures2d.SIFT.create(nFeatures, nOctaveLayers, contrastThreshold, edgeThreshold, sigma);
        opencv_features2d.BOWImgDescriptorExtractor extractor = new opencv_features2d.BOWImgDescriptorExtractor(sift, new opencv_features2d.FlannBasedMatcher());
        extractor.setVocabulary(this.vocabulary);
        File classLocation = null;

        File[] imagesTrain = this.rootDir.listFiles();
        Arrays.sort(imagesTrain);

        for (File trainImg : imagesTrain) {
            if (!trainImg.isFile()) {
                continue;
            }

            if (!class_name.equals(trainImg.getName().split("_")[0])) {
                class_name = trainImg.getName().split("_")[0];
                classLocation = new File(this.classifierDir + "/" + class_name + ".xml");
            }

            if (classLocation != null && classLocation.exists()) {
                System.out.println("Passing " + trainImg.getName() + ", class already exist in directory");
            } else {
                trainMat = opencv_imgcodecs.imread(trainImg.getAbsolutePath(), opencv_imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);

                opencv_core.KeyPointVector keypoints = new opencv_core.KeyPointVector();
                Mat descriptors = new Mat();

                sift.detectAndCompute(trainMat, new Mat(), keypoints, descriptors);

                System.out.println("Computing words for " + trainImg.getName());
                extractor.compute(trainMat, keypoints, histo, new opencv_core.IntVectorVector(), new Mat());

                samples.push_back(histo);
            }
        }

        //Fabrication des SVM avec les labels
        int globalIndex = 0;
        int indexStart = 0;
        int indexStop = samples.rows();
        class_name = "";
        JSONArray jsonArrayTmp = new JSONArray();
        int[] resp = new int[samples.rows()];
        for (File trainImg : this.rootDir.listFiles()) {
            if (!trainImg.isFile()) {
                System.out.println("skipping " + trainImg.getName());
                continue;
            } else {
                System.out.println("file=" + trainImg.getName());
            }

            if (globalIndex != 0 && (!class_name.equals(trainImg.getName().split("_")[0])
                    || globalIndex == this.rootDir.listFiles().length - 1)) {
                classLocation = new File(this.classifierDir + "/" + class_name + ".xml");
                if (classLocation.exists()) {
                    System.out.println("Existing SVM for classe " + class_name);
                    //Création objet

                    addArrayTmpJSON(class_name, jsonArrayTmp);
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

                    svm.train(samples, opencv_ml.ROW_SAMPLE, labels);
                    svm.save(this.classifierDir + "/" + class_name + ".xml");
                    System.out.println("Saving " + class_name + ".xml");

                    //Création objet
                    addArrayTmpJSON(class_name, jsonArrayTmp);
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
            File vocab = new File(this.vocabularyDir + "/vocabulary.yml");
            if (vocab.exists()) {
                System.out.println("Signing with md5");
                hash = this.getHashMd5(vocab.getAbsolutePath());
                this.indexJson.put("vocab_hash", hash);
                File fileIndex = new File(this.vocabularyDir + "/" + "index.json");
                FileWriter fw = new FileWriter(fileIndex);
                fw.write(this.indexJson.toString());
                fw.close();
            } else {
                System.err.println("Error when finalizing, please clear directory data and try again");
                removeLockfile();

            }

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        removeLockfile();

        notifyClients();


    }

    private void notifyClients() {

        Notification notification = new Notification("ComputeServer", "Le training est terminé.");

        Message me = Message.builder()
                .setNotification(notification)
                .setTopic(Config.DEFAULT_FCM_TOPIC)
                .build();

        try {

            FirebaseMessaging firebaseMessaging = FirebaseMessaging.getInstance();
            String s = firebaseMessaging.sendAsync(me).get();
            System.out.println("Send end training to topic: "+ s);

        } catch(Exception ex){

        }


    }

    /**
     * Crée début de structure json
     * @param class_name
     * @param jsonArrayTmp
     */
    private void addArrayTmpJSON(String class_name, JSONArray jsonArrayTmp) {
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

    private void checkLockfile() throws Exception {
        File lock = new File("etc/train.lock");
        if (lock.exists()) {
            throw new Exception("Lock file is present.");
        }
    }
    private void createLockfile() throws Exception {
        File lock = new File("etc/train.lock");
        if (!lock.createNewFile()) {
            throw new Exception("Can't create lock file");
        }
    }

    private void removeLockfile() {
        File lock = new File("etc/train.lock");
        if (lock.exists()) {
            lock.delete();
        }
    }
}
