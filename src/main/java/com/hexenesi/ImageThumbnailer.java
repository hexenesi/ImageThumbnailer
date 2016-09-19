package com.hexenesi;

import java.io.File;

public class ImageThumbnailer {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        for (String arg : args) {
            File file = new File(arg);
            Scaler s = new Scaler(file);
        }
    }

}
