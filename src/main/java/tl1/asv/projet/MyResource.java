package tl1.asv.projet;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.TopicManagementResponse;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import tl1.asv.projet.db.ClientsDatabase;
import tl1.asv.projet.recognition.general.GetFromServer;
import tl1.asv.projet.recognition.training.RecognitionTrainerController;
import tl1.asv.projet.recognition.training.TrainingCluster;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import static tl1.asv.projet.Config.*;


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


    @GET
    @Path("/test")
    @Produces(MediaType.TEXT_PLAIN)
    public String getServ() {

        GetFromServer.init();

        return "Got it!";
    }


    @GET
    @Path("/train")
    @Produces(MediaType.TEXT_PLAIN)
    public String trainSErv() {

        new Thread(() -> {

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


        }).start();


        return "trained";
    }


    /**
     * This method is to allow uploading image to server.
     *
     * @param uploadedInputStream
     * @param fileDetail
     * @return
     */
    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadFile(
            @FormDataParam("file") InputStream uploadedInputStream,
            @FormDataParam("file") FormDataContentDisposition fileDetail,
            @FormDataParam("file") final FormDataBodyPart body) {

        String[] fileName = fileDetail.getFileName().split("\\.");

        String extension = fileName[fileName.length - 1].toLowerCase();


        // generate new filename
        String newFileName = "test_" + generateRandomInt();
        String totalPath = newFileName + ".";


        if (fileDetail.getFileName().equals("cropped")) {
            totalPath += ".jpg";
        } else if (!Config.isAllowedExtension(extension)) {
            System.out.println("Received: " + body.getMediaType());
            return Response.status(415).entity("Extension refused").build();
        } else {
            totalPath += extension;
        }

        String uploadedFileLocation = SERVER_UPLOAD_LOCATION_FOLDER + "/" + totalPath;

        // save it
        saveFile(uploadedInputStream, uploadedFileLocation);

        String output = totalPath;
        System.out.println(output);

        return Response.status(200).entity(output).build();

    }

    /**
     * Start Analyse
     *
     * @param file
     * @return
     */
    @GET
    @Path("/analyse/{file}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response analyse(@PathParam("file") String file, @QueryParam("token") String token, @QueryParam("fcm") String fcm) {
        System.out.println("Starting analyse");

        if (!ClientsDatabase.checkPair(fcm, token)) {
           /* System.err.println("Tokens not authorized.");
            return Response.status(403).entity("Tokens mismatch").build();*/
        }


        Thread t = new Thread(() -> {
            String className = "NOK";

            String filepath = SERVER_UPLOAD_LOCATION_FOLDER + "/" + file;


            RecognitionTrainerController recognitionTrainerController = new RecognitionTrainerController();
            try {
                className = recognitionTrainerController.analyse(filepath);

                // move file in staging
                moveFileToStaging(filepath, className);

            } catch (Exception e) {
                e.printStackTrace();
            }


            // have to notify the client
            FirebaseMessaging firebaseMessaging = FirebaseMessaging.getInstance();

            Message message = Message.builder()
                    .putData("action", "analyse")
                    .putData("prediction", className)
                    .putData("code","1")
                    .setToken(fcm)
                    .build();
            System.out.println("Sent to " + fcm);

            try {
                String firebaseResult = null;
                firebaseResult = firebaseMessaging.sendAsync(message).get();

                System.out.println("Message sent to " + token);
                System.out.println(firebaseResult);

            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        });
        t.start();

        return Response.status(200).entity("IP").build();
    }


    @GET
    @Path("/tokregister/{token}")
    public Response regtoken(@PathParam("token") String token) {

        if (token.isEmpty() || token.length() < 10) {
            return Response.status(400).entity("Bad token").build();
        }

        HashMap<String, String> clients = ClientsDatabase.clients;

        if (clients.get(token) != null) {
            System.out.println(token + " device has arrived, but already defined. Regenerating.");
        } else {
            System.out.println(token + " device has arrived,generating new token.");
        }


        String tok2 = "id-" + generateRandomInt();
        clients.put(token, tok2);

        // register to default topic "news"
        TopicManagementResponse response = null;
        try {
            response = FirebaseMessaging.getInstance().subscribeToTopicAsync(Arrays.asList(token), Config.DEFAULT_FCM_TOPIC).get();
            System.out.println(response.getSuccessCount() + " tokens were subscribed successfully");

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }




        return Response.status(200).entity(tok2).build();

    }


    @GET
    @Path("/validate/{valid}/{file}")
    public Response validate(@PathParam("file") String file, @PathParam("valid") String validate) {

        if (validate.equals("yes")) {

            if (moveStgFileToRef(file)) {
                return Response.status(200).entity("moved to refs files").build();
            } else {
                return Response.status(500).entity("Error on moving...").build();
            }
        } else {
            if (removeStgFile(file)) {
                return Response.status(200).entity("removed from stg files").build();
            } else {
                return Response.status(500).entity("Error on deletion...").build();
            }
        }

    }

    /**
     * Move files to staging for validation.
     *
     * @param filepath
     * @param className
     */
    private void moveFileToStaging(String filepath, String className) {
        File file = new File(filepath);

        String[] split = file.getName().split("_");
        split[0] = className;

        String finalName = String.join("_", split);
        String newFilePath = SERVER_STAGING_LOCATION_FOLDER + "/" + finalName;

        file.renameTo(new File(newFilePath));
        System.out.println(newFilePath + ": new filename for stg");
    }


    /**
     * Sets image as ref.
     *
     * @param filename
     * @return
     */
    private boolean moveStgFileToRef(String filename) {
        File file = new File(SERVER_STAGING_LOCATION_FOLDER + "/" + filename);
        if (!file.isFile()) {
            return false;
        }
        System.out.println("Moved " + file.getName() + " to references");
        return file.renameTo(new File(SERVER_REFERENCES_FOLDER + "/" + filename));

    }

    /**
     * Removes stg file.
     *
     * @return
     */
    private boolean removeStgFile(String filename) {
        File file = new File(SERVER_STAGING_LOCATION_FOLDER + "/" + filename);
        if (!file.isFile()) {
            return false;
        }

        System.out.println("Removed " + file.getName() + " to references");

        return file.delete();
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
