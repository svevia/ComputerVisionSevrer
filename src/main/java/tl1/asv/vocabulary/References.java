package tl1.asv.vocabulary;

import java.util.List;

public class References {
	
	public static final String DIRECTORY = "etc";

	private String vocabulary;
	private List<Brand> brands;
	
	
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
	
	

}
