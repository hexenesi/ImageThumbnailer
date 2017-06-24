/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hexenesi;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
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
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.TiffDirectoryType;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.apache.commons.imaging.formats.tiff.taginfos.TagInfo;
import org.apache.commons.imaging.formats.tiff.taginfos.TagInfoShort;
import org.imgscalr.Scalr;

/**
 *
 * @author jpcazarez
 */
public class Scaler {

    private static int TARGET_HEIGHT = 300;
    private static int TARGET_WIDTH = 500;
    private static String OUTPUT_EXTENSION="png";
    File file = null;
    String output = null;
    String output_thumb = null;
    boolean saveSubSampled = false;

    public Scaler(){   
    }
    
    @Deprecated
    public Scaler(String file, String output, String output_thumb) {
        this.file = new File(file);
        this.output = output;
        this.output_thumb = output_thumb;
    }
    
    public void setFile(String file){
        this.file=new File(file);
        String tmpName=file.substring(0, file.indexOf("."));
        this.output=tmpName+"_output."+OUTPUT_EXTENSION;
        this.output_thumb=tmpName+"_output_thumb."+OUTPUT_EXTENSION;
    }

    public void setDimensions(Dimension d){
        TARGET_HEIGHT=d.height;
        TARGET_WIDTH=d.width;
    }
    public void setSaveSubSampled(boolean saveSubSampled) {
        this.saveSubSampled = saveSubSampled;
    }

    /**
     * DO a scaling to fit inside a TARGET_WIDTHxTARGET_HEIGHT rectangle.
     */
    public void doScaling() {
        try {
            int orientation = getExifOrientation(file);
            System.out.println(file.getName());
            //Dimension d = Imaging.getImageSize(file);
            ImageInputStream imageInputStream = ImageIO.createImageInputStream(file);
            // Use a subsampled image from the original, avoids read images too large to fit in memory
            BufferedImage bufferedImage = subsampleImage(imageInputStream, TARGET_WIDTH * 2, TARGET_HEIGHT * 2);
            ImageInformation ii=new ImageInformation(orientation, bufferedImage.getWidth(), bufferedImage.getHeight());
            AffineTransform transform=getExifTransformation(ii);
            bufferedImage=transformImage(bufferedImage, transform);            
            Scalr.Mode mode;
            // calculate which side will be largest/smaller
            // this works with any image size
            double scaleX=(TARGET_WIDTH*1.0)/(bufferedImage.getWidth()*1.0);
            double scaleY=(TARGET_HEIGHT*1.0)/(bufferedImage.getHeight()*1.0);
            if (scaleX<scaleY) {
                mode = Scalr.Mode.FIT_TO_WIDTH;
            } else {
                mode = Scalr.Mode.FIT_TO_HEIGHT;
            } 
            
            BufferedImage thumbnail = Scalr.resize(bufferedImage,
                    Scalr.Method.QUALITY,
                    mode, TARGET_WIDTH, TARGET_HEIGHT,
                    Scalr.OP_ANTIALIAS);
            BufferedImage combined = new BufferedImage(TARGET_WIDTH, TARGET_HEIGHT, BufferedImage.TYPE_INT_ARGB);
            int x = 0, y = 0;
            if (mode == Scalr.Mode.FIT_TO_HEIGHT) {
                x = (TARGET_WIDTH - thumbnail.getWidth()) / 2;
            }
            if (mode == Scalr.Mode.FIT_TO_WIDTH) {
                y = (TARGET_HEIGHT - thumbnail.getHeight()) / 2;
            }
            Graphics g = combined.getGraphics();
            g.setColor(new java.awt.Color(0.0f, 0.0f, 0.0f, 0.0f));
            g.fillRect(0, 0, combined.getWidth(), combined.getHeight());
            g.drawImage(thumbnail, x, y, null);
            g.dispose();
            //Writes test subsampled image taken from original
            if (saveSubSampled) {
                ImageIO.write(bufferedImage, OUTPUT_EXTENSION, new File(output));
            }
            //Writes thumbnail, created from the subsampled image.
            ImageIO.write(combined, OUTPUT_EXTENSION, new File(output_thumb));

        } catch (IOException ex) {
            Logger.getLogger(Scaler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Scaler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Read a subsampled image from source file, instead of loading full image.
     *
     * @param inputStream
     * @param x
     * @param y
     * @return
     * @throws IOException
     */
    public BufferedImage subsampleImage(
            ImageInputStream inputStream,
            int x,
            int y) throws IOException {
        BufferedImage resampledImage = null;
        Iterator<ImageReader> readers = ImageIO.getImageReaders(inputStream);
        if (!readers.hasNext()) {
            throw new IOException("No reader available for supplied image stream.");
        }
        // Get first reader if any.
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

    /**
     * Obtains the subsampling rate, This method is somewhat tricky, some images
     * may look bad.
     *
     * @param d1 Dimension of source image
     * @param d2 Dimension needed
     * @return long subsample factor
     */
    public long scaleSubsamplingMaintainAspectRatio(Dimension d1, Dimension d2) {
        long subsampling = 1;

        if (d1.getWidth() > d2.getWidth()) {
            subsampling = Math.round(d1.getWidth() / d2.getWidth());
        } else if (d1.getHeight() > d2.getHeight()) {
            subsampling = Math.round(d1.getHeight() / d2.getHeight());
        }

        return subsampling;
    }

    /**
     * Retrieve the orientation of a file from the EXIF data. Required, as
     * built-in ExifInterface is not always reliable.
     *
     * @param imageFile the image file.
     * @return the orientation value.
     */
    protected int getExifOrientation(final File imageFile) {
        try {
            final ImageMetadata metadata = Imaging.getMetadata(imageFile);
            TiffImageMetadata tiffImageMetadata;

            if (metadata instanceof JpegImageMetadata) {
                tiffImageMetadata = ((JpegImageMetadata) metadata).getExif();
            } else if (metadata instanceof TiffImageMetadata) {
                tiffImageMetadata = (TiffImageMetadata) metadata;
            } else {
                return TiffTagConstants.ORIENTATION_VALUE_HORIZONTAL_NORMAL;
            }
            if(tiffImageMetadata==null){
                return TiffTagConstants.ORIENTATION_VALUE_HORIZONTAL_NORMAL;
            }
            TiffField field = tiffImageMetadata.findField(TiffTagConstants.TIFF_TAG_ORIENTATION);
            if (field != null) {
                return field.getIntValue();
            } else {
                TagInfo tagInfo = new TagInfoShort("Orientation", 0x115, TiffDirectoryType.TIFF_DIRECTORY_IFD0); // MAGIC_NUMBER
                field = tiffImageMetadata.findField(tagInfo);
                if (field != null) {
                    return field.getIntValue();
                } else {
                    return TiffTagConstants.ORIENTATION_VALUE_HORIZONTAL_NORMAL;
                }
            }
        } catch (ImageReadException | IOException e) {
            return TiffTagConstants.ORIENTATION_VALUE_HORIZONTAL_NORMAL;
        }
    }
    // Inner class containing image information

    public  class ImageInformation {

        public final int orientation;
        public final int width;
        public final int height;

        public ImageInformation(int orientation, int width, int height) {
            this.orientation = orientation;
            this.width = width;
            this.height = height;
        }

        public String toString() {
            return String.format("%dx%d,%d", this.width, this.height, this.orientation);
        }
    }

    public  BufferedImage transformImage(BufferedImage image, AffineTransform transform) throws Exception {
        AffineTransformOp op = new AffineTransformOp(transform, AffineTransformOp.TYPE_BICUBIC);
        BufferedImage destinationImage = op.createCompatibleDestImage(image, (image.getType() == BufferedImage.TYPE_BYTE_GRAY) ? image.getColorModel() : null);
        Graphics2D g = destinationImage.createGraphics();
        g.setBackground(Color.WHITE);
        g.clearRect(0, 0, destinationImage.getWidth(), destinationImage.getHeight());
        destinationImage = op.filter(image, destinationImage);
        return destinationImage;
    }

    public  AffineTransform getExifTransformation(ImageInformation info) {
        AffineTransform t = new AffineTransform();
        switch (info.orientation) {
            case TiffTagConstants.ORIENTATION_VALUE_HORIZONTAL_NORMAL:
                break;
            case TiffTagConstants.ORIENTATION_VALUE_MIRROR_HORIZONTAL: // Flip X
                t.scale(-1.0, 1.0);
                t.translate(-info.width, 0);
                break;
            case TiffTagConstants.ORIENTATION_VALUE_ROTATE_180: // PI rotation 
                t.translate(info.width, info.height);
                t.rotate(Math.PI);
                break;
            case TiffTagConstants.ORIENTATION_VALUE_MIRROR_VERTICAL: // Flip Y
                t.scale(1.0, -1.0);
                t.translate(0, -info.height);
                break;
            case TiffTagConstants.ORIENTATION_VALUE_MIRROR_HORIZONTAL_AND_ROTATE_270_CW: // - PI/2 and Flip X
                t.rotate(-Math.PI / 2);
                t.scale(-1.0, 1.0);
                break;
            case TiffTagConstants.ORIENTATION_VALUE_ROTATE_90_CW: // -PI/2 and -width
                t.translate(info.height, 0);
                t.rotate(Math.PI / 2);
                break;
            case TiffTagConstants.ORIENTATION_VALUE_MIRROR_HORIZONTAL_AND_ROTATE_90_CW: // PI/2 and Flip
                t.scale(-1.0, 1.0);
                t.translate(-info.height, 0);
                t.translate(0, info.width);
                t.rotate(3 * Math.PI / 2);
                break;
            case TiffTagConstants.ORIENTATION_VALUE_ROTATE_270_CW: // PI / 2
                t.translate(0, info.width);
                t.rotate(3 * Math.PI / 2);
                break;
        }
        return t;
    }
}
