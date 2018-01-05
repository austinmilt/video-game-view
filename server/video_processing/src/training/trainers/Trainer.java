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


package training.trainers;

import org.opencv.core.Mat;
import training.math.Matrix2D;

import java.io.File;
import java.io.IOException;

public abstract class Trainer {

    private static File trainingDir;
    public abstract String save_image(Mat frame, String label);
    public abstract void train() throws IOException;
    public static Matrix2D prepare_input(Matrix2D input){ throw new RuntimeException("Trainers must implement their own prepare_input()."); }

    /** Gets the directory where new training images for this detector should be stored. */
    public static File get_training_dir() { return trainingDir; }
}
