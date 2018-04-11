package tl1.asv.projet.recognition.ipi;

import tl1.asv.projet.recognition.general.Classifier;
import tl1.asv.projet.recognition.general.OneImage;

import java.io.File;
import java.util.*;

import static org.bytedeco.javacpp.opencv_imgcodecs.imread;
import static tl1.asv.projet.Config.SERVER_REFERENCES_FOLDER;

public class RecognitionAnalyseController {

    // keep ordered classifiers
    private TreeMap<String, Classifier> classifiers = new TreeMap<>();


    public String analyse(String filepath) {
        HashMap<OneImage, Float> dists = new HashMap<>();
        HashMap<String, Float> category = new HashMap<>();
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


            // Gets classifier, add a refence image and it's distance from test image
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
