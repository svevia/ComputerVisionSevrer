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

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.bytedeco.javacpp.opencv_core.Mat;


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
    
    
    
	private static final String SERVER_UPLOAD_LOCATION_FOLDER = "C://Users/Aurelien/Documents/Telecom/projets/projet/img";

	/**
	 * Upload a File
	 */

	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadFile(@FormParam("img") String img) {

		
		String ext = "png";
		String filePath = SERVER_UPLOAD_LOCATION_FOLDER	+ "/test/" + generateRandomInt() + "." + ext;

		
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
	
	public String ChooseClass(String filepath){
		HashMap<String, Float> dists = new HashMap<String,Float>();
		HashMap<String, Float> category = new HashMap<String,Float>();

		Mat testMat = imread(filepath);
		
	
		for(File f : listImgaes()){
			if(!f.isDirectory()){
				Mat matImg = imread(f.getAbsolutePath());
				dists.put(f.getName(), recoImage.getDist(testMat, matImg));
			}
		}
		
		Map<String,Float> sorted = recoImage.sortByComparator(dists, true);
		Set<Map.Entry<String, Float>> sortedSet = sorted.entrySet();
		Iterator <Map.Entry<String, Float>> it = sortedSet.iterator();
		int kMatch = 7;
		for(int i = 0; i< kMatch;i++){
			Entry<String,Float> item = it.next();
			String cat = item.getKey().split("-")[0];
			if(category.containsKey(cat)){
				category.put(cat, category.get(cat) + 1);
			}
			else{
				category.put(cat, (float) 1);
			}
		}
		
		Map<String,Float> sortedCat = recoImage.sortByComparator(category, false);
		Set<Map.Entry<String, Float>> sortedSetCat = sortedCat.entrySet();
		Iterator <Map.Entry<String, Float>> itCat = sortedSetCat.iterator();
		
		
		return itCat.next().getKey();
	}
	
	public File[] listImgaes(){
		File dir = new File(SERVER_UPLOAD_LOCATION_FOLDER);
		return dir.listFiles();
	}

	// save uploaded file to a defined location on the server
	private void saveFile(InputStream uploadedInputStream,
			String serverLocation) {

		try {
			OutputStream outpuStream = new FileOutputStream(new File(serverLocation));
			int read = 0;
			byte[] bytes = new byte[1024];

			outpuStream = new FileOutputStream(new File(serverLocation));
			while ((read = uploadedInputStream.read(bytes)) != -1) {
				outpuStream.write(bytes, 0, read);
			}
			outpuStream.flush();
			outpuStream.close();
		} catch (IOException e) {

			e.printStackTrace();
		}

	}
	
	private int generateRandomInt(){
		SecureRandom test = new SecureRandom();
		int result = test.nextInt(1000000000);
		return result;
	}
}
