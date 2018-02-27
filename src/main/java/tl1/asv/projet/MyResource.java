package tl1.asv.projet;

import static tl1.asv.projet.Config.SERVER_REFERENCES_FOLDER;
import static tl1.asv.projet.Config.SERVER_STAGING_LOCATION_FOLDER;
import static tl1.asv.projet.Config.SERVER_UPLOAD_LOCATION_FOLDER;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import tl1.asv.projet.recognition.*;


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

        TrainingCluster trainingCluster = new TrainingCluster();
        trainingCluster.train();


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
            @FormDataParam("file") FormDataContentDisposition fileDetail) {

        String[] fileName = fileDetail.getFileName().split("\\.");

        String extension = fileName[fileName.length - 1].toLowerCase();
        if (!Config.isAllowedExtension(extension)) {
            return Response.status(415).entity("Extension refused").build();

        }


        String newFileName = "test_" + generateRandomInt();

        String totalPath = newFileName + "." + extension;
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
    public Response analyse(@PathParam("file") String file) {
        System.out.println("Starting analyse");
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

        return Response.status(200).entity(className).build();
    }


    @GET
    @Path("/validate/{valid}/{file}")
    public Response validate(@PathParam("file") String file, @PathParam("valid") String validate){

        if(validate.equals("yes")){

            if(moveStgFileToRef(file)){
                return Response.status(200).entity("moved to refs files").build();
            } else {
                return Response.status(500).entity("Error on moving...").build();
            }
        } else {
            if(removeStgFile(file)){
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
