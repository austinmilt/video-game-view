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


package training.ann.networks;

import training.math.Matrix2D;

import java.util.Collection;

public abstract class Network {
    public abstract Matrix2D predict(Matrix2D sample);
    public abstract String predict(Matrix2D sample, Collection<String> validIDs);
    public abstract int[] get_input_shape();
}
