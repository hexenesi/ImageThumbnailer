package com.hexenesi;

public class ImageThumbnailer {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        for (String arg : args) {
            Scaler s = new Scaler(arg,"output.jpg","output_thumb.png");
            s.doScaling();
        }
    }

}
