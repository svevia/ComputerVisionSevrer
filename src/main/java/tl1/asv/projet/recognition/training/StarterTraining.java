package tl1.asv.projet.recognition.training;

import com.fasterxml.jackson.databind.ObjectMapper;
import tl1.asv.projet.utils.HttpDownloadUtility;
import tl1.asv.vocabulary.Brand;
import tl1.asv.vocabulary.References;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import static tl1.asv.projet.utils.Config.SERVER_CLASSIFIERS_FOLDER;
import static tl1.asv.projet.utils.Config.SERVER_ETC_FOLDER;

public class StarterTraining {

    private String source;

    /**
     * @param source, LOCAL, TEACHER, or remote HTTP
     */
    public StarterTraining(@NotNull String source) {
        this.source = source.toLowerCase();

        /**
         * Downloads or local mode.
         */
        if (this.source.equals("local"))
            startCluster();
        else
            downloadClassifiers();

        cacheClassifiers();
    }

    /**
     * Cluster
     */
    private void startCluster() {

        System.out.println("START LOCAL MODE");

        /**
         * remove classifiers
         */
        File dirClassifiers = new File(SERVER_CLASSIFIERS_FOLDER);
        File[] files = dirClassifiers.listFiles();
        for (File file : files) {
            if (file.isFile()) file.delete();
        }

        /**
         * remove index and vocab
         */
        File indexjson = new File(SERVER_ETC_FOLDER + "/index.json");
        File vocab = new File(SERVER_ETC_FOLDER + "/vocabulary.yml");

        if (indexjson.isFile()) indexjson.delete();
        if (vocab.isFile()) vocab.delete();

        TrainingCluster trainingCluster = new TrainingCluster();
        trainingCluster.train();

    }


    private void cacheClassifiers() {
        System.out.println("Server ok");
        ObjectMapper mapper = new ObjectMapper();
        try {
            References obj = mapper.readValue(new File("etc/index.json"), References.class);

            References.setSingleton(obj);

            System.out.println(obj.getVocabulary());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    /**
     * Downloads from external source.
     */
    private void downloadClassifiers() {
        System.out.println("Server ok, downloading base...");
        ObjectMapper mapper = new ObjectMapper() ;
        try {

            String baseHost = getSource();
            System.out.println("Downloading at " + baseHost);

            HttpDownloadUtility.downloadFile(baseHost + "/" + "index.json", References.DIRECTORY);
            References obj = mapper.readValue(new URL(baseHost + "/index.json"), References.class);

            References.setSingleton(obj);

            HttpDownloadUtility.downloadFile(baseHost + "/" + obj.getVocabulary(), References.DIRECTORY);
            for (Brand b : obj.getBrands()) {
                HttpDownloadUtility.downloadFile(baseHost + "/classifiers/" + b.getClassifier(), References.DIRECTORY);

                for (String image : b.getImages()) {
                    HttpDownloadUtility.downloadFile(baseHost + "/train-images/" + image, References.DIRECTORY + "/refs");

                }


            }


            System.out.println(obj.getVocabulary());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    /**
     * Returns the base source.
     *
     * @return
     */
    private String getSource() {
        switch (this.source) {
            case "teacher":
            case "prof":
                return "http://www-rech.telecom-lille.fr/nonfreesift";
            default:
                return source;
        }
    }
}
