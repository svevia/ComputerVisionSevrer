package tl1.asv.projet;

import java.io.IOException;
import java.net.URL;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.fasterxml.jackson.databind.ObjectMapper;

import tl1.asv.vocabulary.Brand;
import tl1.asv.vocabulary.References;

public class AppContextListener implements ServletContextListener{

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		System.out.println("Server ok");
		ObjectMapper mapper = new ObjectMapper();
		try {
			References obj = mapper.readValue(new URL("http://www-rech.telecom-lille.fr/nonfreesift/index.json"), References.class);

			References.setSingleton(obj);

			HttpDownloadUtility.downloadFile("http://www-rech.telecom-lille.fr/nonfreesift/" + obj.getVocabulary(),References.DIRECTORY);
			for(Brand b : obj.getBrands()){
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
