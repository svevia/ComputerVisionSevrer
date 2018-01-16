package tl1.asv.projet;


public class Config {

    public static final String[] ALLOWED_EXTENSIONS = {"png","jpg", "jpeg"};

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
