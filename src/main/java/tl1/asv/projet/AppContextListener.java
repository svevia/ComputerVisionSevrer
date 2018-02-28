package tl1.asv.projet;

import com.fasterxml.jackson.databind.ObjectMapper;
import tl1.asv.projet.recognition.TrainingCluster;
import tl1.asv.vocabulary.Brand;
import tl1.asv.vocabulary.References;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;

public class AppContextListener implements ServletContextListener {

    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void contextInitialized(ServletContextEvent arg0) {
        //downloadClassifiers();
       //

        TrainingCluster trainingCluster = new TrainingCluster();
        trainingCluster.train();

        localClassifiers();

    }


    private void localClassifiers() {
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

    private void downloadClassifiers() {
        System.out.println("Server ok");
        ObjectMapper mapper = new ObjectMapper();
        try {
            References obj = mapper.readValue(new URL("http://www-rech.telecom-lille.fr/nonfreesift/index.json"), References.class);

            References.setSingleton(obj);

            HttpDownloadUtility.downloadFile("http://www-rech.telecom-lille.fr/nonfreesift/" + obj.getVocabulary(), References.DIRECTORY);
            for (Brand b : obj.getBrands()) {
                HttpDownloadUtility.downloadFile("http://www-rech.telecom-lille.fr/nonfreesift/classifiers/" + b.getClassifier(), References.DIRECTORY);

                for (String image : b.getImages()) {
                    HttpDownloadUtility.downloadFile("http://www-rech.telecom-lille.fr/nonfreesift/train-images/" + image, References.DIRECTORY + "/refs");

                }


            }


            System.out.println(obj.getVocabulary());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
