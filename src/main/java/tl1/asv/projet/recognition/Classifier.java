package tl1.asv.projet.recognition;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains images for classifier.
 */
public class Classifier {
    final String name;
    final List<OneImage> references = new ArrayList<>();
    private float distance = 0.0f;


    public Classifier(String name) {
        this.name = name;
    }

    public List<OneImage> getReferences() {
        return references;
    }

    public void addDistance(float f){
        distance += f;
    }

    public float getDistance() {
        return distance;
    }
}
