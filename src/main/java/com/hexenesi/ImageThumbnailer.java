package com.hexenesi;

public class ImageThumbnailer {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Scaler s=new Scaler();
        for (String arg : args) {
            s.setFile(arg);
            s.doScaling();
        }
    }

}
