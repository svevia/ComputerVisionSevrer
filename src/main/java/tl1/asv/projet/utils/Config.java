package tl1.asv.projet.utils;


public class Config {

    public static final String[] ALLOWED_EXTENSIONS = {"png","jpg", "jpeg"};

    public static final String SERVER_UPLOAD_LOCATION_FOLDER = "tmp/img";
    public static final String SERVER_STAGING_LOCATION_FOLDER = "tmp/stg";

    public static final String SERVER_CLASSIFIERS_FOLDER = "etc/classifiers";
    public static final String SERVER_ETC_FOLDER = "etc/classifiers";
    public static final String SERVER_REFERENCES_FOLDER = "etc/refs";
    public static final String DEFAULT_FCM_TOPIC = "news";


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
