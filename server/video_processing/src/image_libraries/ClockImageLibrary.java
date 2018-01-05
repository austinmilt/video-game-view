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
import training.trainers.ClockTrainerDigitANN;

import javax.imageio.ImageIO;
import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * This class is used to hold and load images with their associated labels and
 * feed them into a ClockTrainer.
 *
 * @author Austin Milt
 * @see ClockTrainerDigitANN
 */
public class ClockImageLibrary implements Serializable, Iterable<Image> {

    private transient HashMap<String, Image> images = new HashMap<>();
    private final String IMAGE_FORMAT = "png";


    /**
     * Constructs the library from the given set of images.
     *
     * @param images images to use to construct the library
     */
    public ClockImageLibrary(Image[] images){
        String name;
        for (Image image : images){
            name = image.get_name();
            this.images.put(name, image);
        }
    }


    /**
     * Creates a new library by reading in files from a directory.
     *
     * @param imageDir path of upper-level directory containing images
     * @param searchString FileUtils style search string
     * @return new library
     * @see org.apache.commons.io.FileUtils
     */
    public static ClockImageLibrary make_from_imagedir(String imageDir, String searchString) throws IOException {
        Path[] files = Glob.match(imageDir, searchString);
        String path;
        String label;
        Image[] images = new Image[files.length];
        for (int i = 0; i < files.length; i++){
            path = files[i].toAbsolutePath().toString();
            label = ClockTrainerDigitANN.filename_to_label(path);
            images[i] = new Image(path, label);
        }
        return new ClockImageLibrary(images);
    }


    /**
     * Loads an NameImageLibrary that has been saved to disk.
     *
     * @param path path to the saved NameImageLibrary file
     * @return the loaded NameImageLibrary
     */
    public static ClockImageLibrary load_library(String path) throws IOException {
        ObjectInputStream objectinputstream = null;
        FileInputStream streamIn;
        ClockImageLibrary library = null;
        try {
            streamIn = new FileInputStream(path);
            objectinputstream = new ObjectInputStream(streamIn);
            library = (ClockImageLibrary) objectinputstream.readObject();
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
            image = this.images.get(name);
            out.writeUTF(image.get_source()); // image source
            buffer = new ByteArrayOutputStream();
            ImageIO.write(image.get_image(), IMAGE_FORMAT, buffer);
            out.writeInt(buffer.size()); // size of image to read back
            buffer.writeTo(out); // the image object
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
        String name;
        Image image;
        HashMap<String, Image> images = new HashMap<>();
        int imageBytes;
        byte[] buffer;
        for (int i = 0; i < nameCount; i++){
            name = in.readUTF();

            // make the Image and set its basic info
            image = new Image();
            image.set_name(name);
            image.set_source(in.readUTF());

            // set the image and add to the library
            imageBytes = in.readInt();
            buffer = new byte[imageBytes];
            in.readFully(buffer);
            image.set_image(ImageIO.read(new ByteArrayInputStream(buffer)));
            images.put(name, image);
        }
        this.images = images;
    }


    /**
     * Returns the number of character images represented by the composite character images in the library.
     * @return
     */
    public int get_size(){
        int n = 0;
        for (String k : this.images.keySet()){
            n += k.length();
        }
        return n;
    }


    /**
     * Returns the number of composite character images in the library.
     * @return
     */
    public int get_image_count(){
        return this.images.size();
    }


    /**
     * Returns the list of images associated with the given name.
     *
     * @param name name to pull images from
     * @return list of images associated with the name in the library
     */
    public Image get(String name){
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
        return this.images.values().iterator();
    }

}
