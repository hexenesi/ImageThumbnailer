/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hexenesi;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.imgscalr.Scalr;

/**
 *
 * @author jpcazarez
 */
public class Scaler {

    public Scaler(File file) {

        try {
            Scalr.Mode mode = Scalr.Mode.FIT_TO_WIDTH;
            try {
                Dimension d = Imaging.getImageSize(file);
                if (d.height > d.width) {
                    mode = Scalr.Mode.FIT_TO_HEIGHT;
                } else {
                    mode = Scalr.Mode.FIT_TO_WIDTH;
                }
            } catch (ImageReadException ex) {
                Logger.getLogger(Scaler.class.getName()).log(Level.SEVERE, null, ex);
            }
            ImageInputStream imageInputStream = ImageIO.createImageInputStream(file);
            BufferedImage bufferedImage = subsampleImage(imageInputStream, 1000, 600);
            BufferedImage thumbnail = Scalr.resize(bufferedImage,
                    Scalr.Method.ULTRA_QUALITY,
                    mode, TARGET_WIDTH, TARGET_HEIGHT,
                    Scalr.OP_ANTIALIAS);
            BufferedImage combined = new BufferedImage(TARGET_WIDTH, TARGET_HEIGHT, BufferedImage.TYPE_INT_RGB);
            int x=0, y=0;
            if(mode==Scalr.Mode.FIT_TO_HEIGHT){
                x=(TARGET_WIDTH-thumbnail.getWidth())/2;
            }
            if(mode==Scalr.Mode.FIT_TO_WIDTH){
                y=(TARGET_HEIGHT-thumbnail.getHeight())/2;
            }       
            Graphics g = combined.getGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, combined.getWidth(), combined.getHeight());
            g.drawImage(thumbnail, x, y, null);
            g.dispose();
            // writes to output file
            ImageIO.write(bufferedImage, "jpg", new File("output.jpg"));
            ImageIO.write(combined, "jpg", new File("output_thumb.jpg"));

        } catch (IOException ex) {
            Logger.getLogger(Scaler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    private static final int TARGET_HEIGHT = 300;
    private static final int TARGET_WIDTH = 500;

    public static BufferedImage subsampleImage(
            ImageInputStream inputStream,
            int x,
            int y) throws IOException {
        BufferedImage resampledImage = null;

        Iterator<ImageReader> readers = ImageIO.getImageReaders(inputStream);

        if (!readers.hasNext()) {
            throw new IOException("No reader available for supplied image stream.");
        }

        ImageReader reader = readers.next();

        ImageReadParam imageReaderParams = reader.getDefaultReadParam();
        reader.setInput(inputStream);

        Dimension d1 = new Dimension(reader.getWidth(0), reader.getHeight(0));
        Dimension d2 = new Dimension(x, y);
        int subsampling = (int) scaleSubsamplingMaintainAspectRatio(d1, d2);
        imageReaderParams.setSourceSubsampling(subsampling, subsampling, 0, 0);
        resampledImage = reader.read(0, imageReaderParams);
        return resampledImage;
    }

    public static long scaleSubsamplingMaintainAspectRatio(Dimension d1, Dimension d2) {
        long subsampling = 1;

        if (d1.getWidth() > d2.getWidth()) {
            subsampling = Math.round(d1.getWidth() / d2.getWidth());
        } else if (d1.getHeight() > d2.getHeight()) {
            subsampling = Math.round(d1.getHeight() / d2.getHeight());
        }

        return subsampling;
    }

}
