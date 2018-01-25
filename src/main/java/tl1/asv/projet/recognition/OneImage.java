package tl1.asv.projet.recognition;

import org.bytedeco.javacpp.opencv_core;

public class OneImage {

    String path;

    opencv_core.Mat img;

    opencv_core.KeyPointVector keyPointVector;

    opencv_core.Mat descriptors;
    private String name;

    protected OneImage(String path, opencv_core.Mat cvtColor) {

        this.path = path;
        this.img = cvtColor;

    }

    public String getPath() {
        return path;
    }

    public opencv_core.Mat getImg() {
        return img;
    }

    public opencv_core.KeyPointVector getKeyPointVector() {
        return keyPointVector;
    }

    public void setKeyPointVector(opencv_core.KeyPointVector keyPointVector) {
        this.keyPointVector = keyPointVector;
    }

    public opencv_core.Mat getDescriptors() {
        return descriptors;
    }

    public void setDescriptors(opencv_core.Mat descriptors) {
        this.descriptors = descriptors;
    }


    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
