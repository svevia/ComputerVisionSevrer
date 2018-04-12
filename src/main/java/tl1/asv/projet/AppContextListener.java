package tl1.asv.projet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import tl1.asv.projet.recognition.training.StarterTraining;
import tl1.asv.projet.recognition.training.TrainingCluster;
import tl1.asv.projet.utils.HttpDownloadUtility;
import tl1.asv.vocabulary.Brand;
import tl1.asv.vocabulary.References;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;

public class AppContextListener implements ServletContextListener {

    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void contextInitialized(ServletContextEvent arg0) {

        try {
            loadFCM();
        } catch (IOException e) {
            System.out.println("Can't load FCM.");
            e.printStackTrace();
        }

        // ON INIT, train local classifiers
        new StarterTraining("local");


    }

    /**
     * Loads the google messaging.
     * @throws IOException
     */
    private void loadFCM() throws IOException {

        FileInputStream serviceAccount = new FileInputStream("etc/serviceKeys.json");

        FirebaseOptions options = new FirebaseOptions.Builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .setDatabaseUrl("https://computervision-4f52d.firebaseio.com/")
                .build();

        FirebaseApp.initializeApp(options);
        System.out.println("FCM loaded.");


    }



}
