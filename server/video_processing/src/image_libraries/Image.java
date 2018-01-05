/*
Copyright 2018 Austin Walker Milt

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/


package image_libraries;

import training.math.Matrix2D;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.*;
import java.io.*;

/**
 * This class loads and handles images and their attributes.
 *
 * @author Austin Milt
 * @see NameImageLibrary
 */
public class Image implements Serializable {

    private Integer height;
    private Integer width;
    private Integer type;
    private String source;
    private BufferedImage image;
    private String name;
    private final String IMAGE_FORMAT = "png";


    /**
     * Construct a new Image with null attributes.
     */
    public Image(){
        height = null;
        width = null;
        type = null;
        source = null;
        image = null;
        name = null;
    }


    /**
     * Constructs a new Image from the given source.
     *
     * @param source path to the image source file
     * @throws IOException
     */
    public Image(String source) throws IOException {
        this.load(source);
    }


    /**
     * Constructs a new Image from the given source and assigns a name.
     *
     * @param source path to the image source file
     * @param name name associated with this Image
     * @throws IOException
     */
    public Image(String source, String name) throws IOException {
        this.name = name;
        this.load(source);
    }


    /**
     * Loads an image and overwrites caller's properties.
     *
     * @param source path to the image source file
     * @throws IOException
     */
    public void load(String source) throws IOException {
        this.image = ImageIO.read(new File(source));
        this.width = this.image.getWidth();
        this.height = this.image.getHeight();
        this.type = this.image.getType();
        this.source = source;
    }


    /**
     * Gets the name of the image.
     *
     * @return Image name
     */
    public String get_name(){
        return this.name;
    }


    /**
     * Sets the name of the image.
     *
     * @param name new name to give the image
     */
    public void set_name(String name){
        this.name = name;
    }


    /**
     * Gets the height of the image.
     *
     * @return height of the image in pixels
     */
    public Integer get_height(){
        return this.height;
    }


    /**
     * Gets the width of the image.
     *
     * @return width of the image in pixels
     */
    public Integer get_width(){
        return this.width;
    }


    /**
     * Gets the pixel type (e.g. rgb, grayscale, etc) of the image.
     * <p>
     * Type is BufferedImage type.
     *
     * @return image pixel type
     * @see BufferedImage
     */
    public Integer get_type() { return this.type; }


    /**
     * Gets the image object associated with the image.
     *
     * @return image object associated with the image
     */
    public BufferedImage get_image(){
        return this.image;
    }


    /**
     * Gets the path of the source image when it was loaded.
     *
     * @return source file path
     */
    public String get_source(){
        return this.source;
    }


    protected void set_image(BufferedImage image){
        this.image = image;
        this.width = image.getWidth();
        this.height = image.getHeight();
        this.type = image.getType();
    }


    protected void set_source(String source){
        this.source = source;
    }


    protected void set_type(Integer type) { this.type = type; }


    /** Gets the alpha, red, green, and blue values of a pixel. */
    public int[] get_argb(int x, int y){
        int argb = this.image.getRGB(x, y);
        int out[] = new int[4];
        Color c = new Color(argb);
        out[0] = c.getAlpha();
        out[1] = c.getRed();
        out[2] = c.getGreen();
        out[3] = c.getBlue();
        return out;
    }


    /** Calculates the luminosity give an RGB value. */
    public static double luminosity(int r, int g, int b){
        return r*0.299 + g*0.587 + b*0.114;
    }


    /**
     * Calculates euclidean distance of pixel rgb value to white (255, 255, 255).
     *
     * @param r red value
     * @param g green value
     * @param b blue value
     * @return distance (0-1) from white
     */
    public static double distance_to_white(double r, double g, double b) {
        double dR = (255d - r) / 255d;
        double dG = (255d - g) / 255d;
        double dB = (255d - b) / 255d;
        return Math.sqrt(dR*dR + dG*dG + dB*dB) / 3.;
    }


    /** Calculates luminosity of a pixel. */
    public double get_pixel_luminosity(int x, int y){
        int[] argb = get_argb(x, y);
        return luminosity(argb[1], argb[2], argb[3]);
    }



    /**
     * Converts the image to grayscale based on a luminosity formula.
     * <p>
     * Luminosity formula is r*0.299 + g*0.587 + b*0.114
     *
     * @return image converted to grayscale
     */
    public Image as_grayscale(){

        // Create output Image
        Image output = new Image();
        output.set_name(this.name);
        output.set_source(this.source);
        output.set_image(new BufferedImage(this.width, this.height, BufferedImage.TYPE_BYTE_GRAY));
        output.set_type(BufferedImage.TYPE_BYTE_GRAY);

        // Convert to grayscale
        int[] argb;
        int grayLevel;
        int gray;
        for (int i = 0; i < this.height; i++){
            for (int j = 0; j < this.width; j++){
                argb = this.get_argb(j, i);
                grayLevel = (int) luminosity(argb[1], argb[2], argb[3]);
                gray = (argb[0] << 24) + (grayLevel << 16) + (grayLevel << 8) + grayLevel;
                output.image.setRGB(j, i, gray);
            }
        }
        return output;
    }


    /**
     * Returns the pixels of the image as a Matrix2D
     *<p>
     * If not already, image will be converted to grayscale first.
     * <p>
     * Pixel values will range in 0-1.
     *
     * @return matrix of the image
     */
    public Matrix2D to_Matrix2D_grayscale(){
        Matrix2D output = new Matrix2D(this.height, this.width);
        for (int i = 0; i < this.height; i++){
            for (int j = 0; j < this.width; j++){
                output.set(i, j, this.get_pixel_luminosity(j, i)/255.);
            }
        }
        return output;
    }


    /**
     * Returns the pixels of the image as a Matrix2D
     *<p>
     * If not already, image will be converted to grayscale first.
     * <p>
     * Pixel values will range in 0-1.
     *
     * @return matrix of the image
     */
    public Matrix2D to_Matrix2D_whiteness(){
        Matrix2D output = new Matrix2D(this.height, this.width);
        int[] argb;
        for (int i = 0; i < this.height; i++){
            for (int j = 0; j < this.width; j++){
                argb = get_argb(j, i);
                output.set(i, j, 1d - distance_to_white(argb[1], argb[2], argb[3]));
            }
        }
        return output;
    }


    public static void main(String[] args) throws IOException {
        String source = "D:\\Dropbox\\youtube_game_hud\\server\\src\\server\\job_execution\\video_parsing\\training\\training_data\\hero_names\\CLOCKWERK\\2.png";
        String name = "CLOCKWERK";
        Image image = new Image(source, name);
        Image gray = image.as_grayscale();
        Matrix2D mat = gray.to_Matrix2D_grayscale();
        mat.print();
    }

}
