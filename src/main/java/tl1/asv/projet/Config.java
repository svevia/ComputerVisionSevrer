package tl1.asv.projet;


public class Config {

    public static final String[] ALLOWED_EXTENSIONS = {"png","jpg", "jpeg"};

    public static final String SERVER_UPLOAD_LOCATION_FOLDER = "tmp/img";

    public static final String SERVER_REFERENCES_FOLDER = "datasets/processedRefs";


    /**
     * have to be a valid extension
     * @param extension
     * @return
     */
    public static boolean isAllowedExtension(String extension) {
        extension=extension.toLowerCase();

        for (int i = 0; i < ALLOWED_EXTENSIONS.length; i++) {
            if(ALLOWED_EXTENSIONS[i].equals(extension)){
                return true;
            }
        }
        return false;
    }
}
