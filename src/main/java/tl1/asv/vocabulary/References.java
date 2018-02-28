package tl1.asv.vocabulary;

import java.util.List;

public class References {

    public static final String DIRECTORY = "etc";
    private static References references = null;


    private String vocabulary = "vocabulary.yml";
    private List<Brand> brands;

    private String vocab_hash;

    public String getVocabulary() {
        return vocabulary;
    }

    public void setVocabulary(String vocabulary) {
        this.vocabulary = vocabulary;
    }

    public List<Brand> getBrands() {
        return brands;
    }

    public void setBrands(List<Brand> brands) {
        this.brands = brands;
    }

    public static References getSingleton() {

        if(references==null) references=new References();

        return references;
    }

    public static void setSingleton(References references_) {
        references=references_;
    }

    public String getVocab_hash() {
        return vocab_hash;
    }

    public void setVocab_hash(String vocab_hash) {
        this.vocab_hash = vocab_hash;
    }
}
