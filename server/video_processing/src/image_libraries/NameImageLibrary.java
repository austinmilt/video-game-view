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

import training.Glob;

import javax.imageio.ImageIO;
import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * This class is used to hold and load images with their associated names and
 * feed them into a NameTrainer.
 *
 * @author Austin Milt
 * @see training.trainers.NameTrainerAvgImg
 */
public class NameImageLibrary implements Serializable, Iterable<Image> {

    private transient HashMap<String, ArrayList<Image>> images = new HashMap<>();
    private final String IMAGE_FORMAT = "png";


    /**
     * Implements a unit test of the NameImageLibrary
     *
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        String path = "D:\\Dropbox\\youtube_game_hud\\server\\src\\server\\job_execution\\video_parsing\\training\\training_data\\hero_names";
        String search = "**/*.png";
        String out = "D:/Downloads/my_library.ser";

        // make the library
        System.out.print("Attempting to make the library...");
        NameImageLibrary library = make_from_imagedir(path, search);
        System.out.println(" success!");


        // save and reload the library
        System.out.print("Attempting to save the library...");
        library.save_library(out);
        System.out.println(" success!");

        System.out.print("Attempting to reload the library...");
        NameImageLibrary L2 = NameImageLibrary.load_library(out);
        System.out.println(" success!\n");
        for (String name : L2.images.keySet()){
            System.out.println("Name: " + name);
            for (Image image : L2.images.get(name)){
                System.out.println(String.format("\t%s: (%d, %d)", image.get_source(), image.get_width(), image.get_height()));
            }
            System.out.println("");
        }

        System.out.println("Using library iterator...");
        for (Image img : L2){
            System.out.println(String.format("Name: %s, Width: %d, Height: %d", img.get_name(), img.get_width(), img.get_height()));
        }

    }

    /**
     * Constructs the NameImageLibrary from the given set of images.
     *
     * @param images images to use to construct the library
     */
    public NameImageLibrary(Image[] images){
        String name;
        for (Image image : images){
            name = image.get_name();
            if (!this.images.containsKey(name)){
                this.images.put(name, new ArrayList<Image>());
            }
            this.images.get(name).add(image);
        }
    }


    /**
     * Creates a new NameImageLibrary by reading in files from a directory.
     * <p>
     * The image directory must be organized as follows:
     *  [imageDir]
     *      [name_1]
     *          [name_1_image_1]
     *          [name_1_image_2]
     *          ...
     *      [name_2]
     *          [name_2_image_1]
     *          ...
     *      ...
     * where the directory name corresponds to the name to be given to all the
     * images within that directory. The names of the image files themselves
     * does not matter.
     *
     * @param imageDir path of upper-level directory containing images
     * @param searchString FileUtils style search string
     * @return new NameImageLibrary
     * @see org.apache.commons.io.FileUtils
     */
    public static NameImageLibrary make_from_imagedir(String imageDir, String searchString) throws IOException {
        Path[] files = Glob.match(imageDir, searchString);
        String path;
        String name;
        Image[] images = new Image[files.length];
        for (int i = 0; i < files.length; i++){
            path = files[i].toAbsolutePath().toString();
            name = files[i].getParent().getFileName().toString();
            images[i] = new Image(path, name);
        }
        return new NameImageLibrary(images);
    }


    /**
     * Loads an NameImageLibrary that has been saved to disk.
     *
     * @param path path to the saved NameImageLibrary file
     * @return the loaded NameImageLibrary
     */
    public static NameImageLibrary load_library(String path) throws IOException {
        ObjectInputStream objectinputstream = null;
        FileInputStream streamIn;
        NameImageLibrary library = null;
        try {
            streamIn = new FileInputStream(path);
            objectinputstream = new ObjectInputStream(streamIn);
            library = (NameImageLibrary) objectinputstream.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(objectinputstream != null){
                objectinputstream .close();
            }
        }
        return library;
    }


    /**
     * Saves the caller to a serialized file on disk.
     *
     * @param path path to save the NameImageLibrary to
     * @throws IOException
     */
    public void save_library(String path) throws IOException {
        ObjectOutputStream oos = null;
        FileOutputStream fout;
        try{
            fout = new FileOutputStream(path, false);
            oos = new ObjectOutputStream(fout);
            oos.writeObject(this);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if(oos  != null){
                oos.close();
            }
        }
    }


    /**
     * Used for serialization of the object.
     *
     * @param out
     * @throws IOException
     * @see Serializable
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeInt(this.images.size()); // write the number of names that must be loaded
        ArrayList<Image> nameImages;
        Image image;
        ByteArrayOutputStream buffer;
        for (String name : this.images.keySet()){
            out.writeUTF(name); // write the name for this set of images
            nameImages = this.images.get(name);
            out.writeInt(nameImages.size()); // write the number of images for this name
            for (int i = 0; i < nameImages.size(); i++){
                image = nameImages.get(i);
                out.writeUTF(image.get_source()); // image source
                buffer = new ByteArrayOutputStream();
                ImageIO.write(image.get_image(), IMAGE_FORMAT, buffer);
                out.writeInt(buffer.size()); // size of image to read back
                buffer.writeTo(out); // the image object
            }
        }
    }


    /**
     * Used to recreate a serialized object
     * @param in
     * @throws IOException
     * @throws ClassNotFoundException
     * @see Serializable
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {

        // repeat the order of the writing process but for reading to
        // reconstruct the library
        in.defaultReadObject();
        final int nameCount = in.readInt();
        int imageCount;
        String name;
        Image image;
        ArrayList<Image> nameImages;
        HashMap<String, ArrayList<Image>> images = new HashMap<>();
        int imageBytes;
        byte[] buffer;
        for (int i = 0; i < nameCount; i++){
            name = in.readUTF();
            imageCount = in.readInt();
            nameImages = new ArrayList<Image>();
            images.put(name, nameImages);
            for (int j = 0; j < imageCount; j++){

                // make the Image and set its basic info
                image = new Image();
                image.set_name(name);
                image.set_source(in.readUTF());

                // set the image and add to the library
                imageBytes = in.readInt();
                buffer = new byte[imageBytes];
                in.readFully(buffer);
                image.set_image(ImageIO.read(new ByteArrayInputStream(buffer)));
                nameImages.add(image);
            }
        }
        this.images = images;
    }


    /**
     * Returns the number of images in the library.
     * @return
     */
    public int get_size(){
        int count = 0;
        for (String key : this.images.keySet()){
            count += this.images.get(key).size();
        }
        return count;
    }


    /**
     * Returns the list of images associated with the given name.
     *
     * @param name name to pull images from
     * @return list of images associated with the name in the library
     */
    public ArrayList<Image> get(String name){
        return this.images.get(name);
    }


    /**
     * Returns the names in the library.
     *
     * @return set of names in the library
     */
    public Set<String> get_names(){
        return this.images.keySet();
    }


    @Override
    public Iterator<Image> iterator() {
        return new ImageIterator();
    }

    private class ImageIterator implements Iterator {
        private final Iterator<String> namesIterator = get_names().iterator();
        private int count = 0;
        private final int SIZE = get_size();
        private String name;
        private Iterator<Image> imageIterator;

        public boolean hasNext() {
            return count != SIZE;
        }

        public Image next() {
            if (this.hasNext()) {
                if ((name == null) || (!imageIterator.hasNext())) {
                    name = namesIterator.next();
                    imageIterator = get(name).iterator();
                }
                count += 1;
                return imageIterator.next();
            }
            else {
                return null;
            }
        }
    }
}
