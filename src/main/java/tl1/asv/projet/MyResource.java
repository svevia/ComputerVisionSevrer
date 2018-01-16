package tl1.asv.projet;

import static org.bytedeco.javacpp.opencv_imgcodecs.imread;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.net.URLDecoder;
import java.io.UnsupportedEncodingException;


import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.json.JSONObject;


/**
 * Root resource (exposed at "myresource" path)
 */
@Path("myresource")
public class MyResource {

    /**
     * Method handling HTTP GET requests. The returned object will be sent
     * to the client as "text/plain" media type.
     *
     * @return String that will be returned as a text/plain response.
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getIt() {
        return "Got it!";
    }


    private static final String SERVER_UPLOAD_LOCATION_FOLDER = "tmp/img";
    private static final String SERVER_REFERENCES_FOLDER = "datasets/processedRefs";

    /**
     * Upload a File
     */

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response uploadFile(String json) {
        System.out.println(json);
        JSONObject obj = new JSONObject(json);
        String img = obj.getString("img");
        try {
            img = URLDecoder.decode(img, "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        String ext = "png";
        String filePath = SERVER_UPLOAD_LOCATION_FOLDER + "/test/" + generateRandomInt() + "." + ext;


        byte[] data = Base64.getDecoder().decode(img);
        try (OutputStream stream = new FileOutputStream(filePath)) {
            stream.write(data);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        String output = ChooseClass(filePath);

        return Response.status(200).entity(output).build();
    }


    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadFile(
            @FormDataParam("file") InputStream uploadedInputStream,
            @FormDataParam("file") FormDataContentDisposition fileDetail) {

        String[] fileName = fileDetail.getFileName().split(".");

        String extension = fileName[fileName.length - 1].toLowerCase();
        if (!Config.isAllowedExtension(extension)) {
            return Response.status(415).entity("Extension refused").build();

        }


        String newFileName = "test_" + generateRandomInt();

        String totalPath = newFileName + extension;
        String uploadedFileLocation = SERVER_UPLOAD_LOCATION_FOLDER + "/" + totalPath;

        // save it
        saveFile(uploadedInputStream, uploadedFileLocation);

        String output = totalPath;
        System.out.println(output);

        return Response.status(200).entity(output).build();

    }

    @GET
    @Path("/analyse/{file}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response analyse(@PathParam("file") String file) {

        String className = "NOK";

        String filepath = SERVER_UPLOAD_LOCATION_FOLDER + "/" + file;

        className = ChooseClass(filepath);


        return Response.status(200).entity(className).build();
    }


    public String ChooseClass(String filepath) {
        HashMap<String, Float> dists = new HashMap<>();
        HashMap<String, Float> category = new HashMap<>();
        HashMap<String, Float> categoryDistance = new HashMap<>();

        Mat testMat = imread(filepath);


        /**
         * On teste les distances entre l'image de test et les images de références.
         */
        for (File f : listImages()) {
            if (!f.isDirectory()) {
                Mat matImg = imread(f.getAbsolutePath());
                float resultMoy = recoImage.getDist(testMat, matImg);
                dists.put(f.getName(), resultMoy);
                System.out.println("Result for: " + f.getName() + "\t" + resultMoy);
            }
        }

        /**
         *
         */
        Map<String, Float> sorted = recoImage.sortByComparator(dists, true);
        Set<Map.Entry<String, Float>> sortedSet = sorted.entrySet();


        Iterator<Entry<String, Float>> testPrint = sortedSet.iterator();
        while (testPrint.hasNext()) {
            Entry<String, Float> next = testPrint.next();
            System.out.println("next: " + next.getKey() + ":" + next.getValue());
        }

        Iterator<Map.Entry<String, Float>> it = sortedSet.iterator();
        int kMatch = sortedSet.size();

        // parcours des 7 premières je crois...
        for (int i = 0; i < kMatch; i++) {
            Entry<String, Float> item = it.next();
            String cat = item.getKey().split("_")[0];
            if (category.containsKey(cat)) {
                category.put(cat, category.get(cat) + 1);

                // add current distance
                categoryDistance.put(cat, categoryDistance.get(cat) + item.getValue());
            } else {
                category.put(cat, (float) 1);
                categoryDistance.put(cat, item.getValue());
            }
        }


        // print all distances
        for (String key :
                categoryDistance.keySet()) {
            float val = categoryDistance.get(key);

            System.out.println("Original Value: " + key + ":" + val + " /" + category.get(key));
            val /= category.get(key); // moyenne par le nombre d'entrée référencée
            categoryDistance.put(key, val);
            System.out.println("Final Value: " + key + ":" + val);


        }


        float lowest = -1;
        String mostConfident = "NOK";
        for (String marque :
                categoryDistance.keySet()) {
            if (lowest == -1 || lowest > categoryDistance.get(marque)) {
                lowest = categoryDistance.get(marque);
                mostConfident = marque;
            }
        }


        System.out.println("Most confident: " + mostConfident);


        return mostConfident;
/*
        Map<String, Float> sortedCat = recoImage.sortByComparator(category, false);
        Set<Map.Entry<String, Float>> sortedSetCat = sortedCat.entrySet();
        Iterator<Map.Entry<String, Float>> itCat = sortedSetCat.iterator();

        System.out.println("SCORES");
        Entry<String, Float> chosen = itCat.next();
        String chosenClass= chosen.getKey();
        System.out.println("Chosen with: " + chosenClass + "=>"+chosen.getValue());
        while(itCat.hasNext()){
            Entry<String, Float> next = itCat.next();
            System.out.println(next.getKey() +"=>"+next.getValue());
        }

        return chosenClass;*/
    }

    public File[] listImages() {
        File dir = new File(SERVER_REFERENCES_FOLDER);
        return dir.listFiles();
    }

    // save uploaded file to a defined location on the server
    private void saveFile(InputStream uploadedInputStream,
                          String serverLocation) {

        try {
            OutputStream outputStream;
            int read = 0;
            byte[] bytes = new byte[1024];

            outputStream = new FileOutputStream(new File(serverLocation));
            while ((read = uploadedInputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {

            e.printStackTrace();
        }

    }

    private int generateRandomInt() {
        SecureRandom test = new SecureRandom();
        int result = test.nextInt(1000000000);
        return result;
    }
}
