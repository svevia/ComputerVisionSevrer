package tl1.asv.projet;

import static org.bytedeco.javacpp.opencv_imgcodecs.imread;
import static tl1.asv.projet.Config.SERVER_UPLOAD_LOCATION_FOLDER;

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
import tl1.asv.projet.recognition.CVUtils;
import tl1.asv.projet.recognition.OneImage;
import tl1.asv.projet.recognition.RecognitionAnalyseController;
import tl1.asv.projet.recognition.recoImage;


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

        RecognitionAnalyseController recognitionAnalyseController = null;
        try {
            recognitionAnalyseController= new RecognitionAnalyseController();
            className = recognitionAnalyseController.analyse(filepath);
        } catch(Exception ex){
            System.out.println("Exception on thread.");
            ex.printStackTrace();
        } finally {
            if(recognitionAnalyseController!=null){
                recognitionAnalyseController = null;
                System.gc();
            }
        }



        return Response.status(200).entity(className).build();
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
