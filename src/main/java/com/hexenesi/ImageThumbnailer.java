package com.hexenesi;

import java.awt.Dimension;

public class ImageThumbnailer {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Scaler s=new Scaler();
        for (String arg : args) {
            s.setFile(arg);
            Dimension d=new Dimension(250, 250);
            
            s.setDimensions(d);
            s.doScaling();
        }
    }

}
