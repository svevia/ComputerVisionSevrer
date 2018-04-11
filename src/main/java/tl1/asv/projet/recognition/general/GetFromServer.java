package tl1.asv.projet.recognition.general;

import com.google.api.client.http.*;
import com.google.api.client.http.apache.ApacheHttpTransport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class GetFromServer {

    public final static String url = "http://www-rech.telecom-lille.fr/nonfreesift/";

    public static void init() {

        String indexJson = downloadAsString(url + "index.json");
        // ya possibilité de télécharger en json via httpjackson

    }



    protected static String downloadAsString(String url){

        HttpTransport transport = new ApacheHttpTransport();
        HttpRequestFactory requestFactory = transport.createRequestFactory();

        try {
            HttpRequest httpRequest = requestFactory.buildGetRequest(new GenericUrl(url));

            System.out.println("HttpRequest");
            HttpResponse response = httpRequest.execute();

            InputStream is = response.getContent();

            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder out = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line);
            }
            System.out.println(out.toString());   //Prints the string content read from input stream
            reader.close();


            return out.toString();

        } catch (IOException e1) {
            e1.printStackTrace();
        }


        return null;
    }

}
