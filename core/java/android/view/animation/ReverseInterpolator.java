/*
 * Copyright (C) 2016 AllianceROM, ~Morningstar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.view.animation;

import android.view.animation.Interpolator;

public class ReverseInterpolator implements Interpolator {

    private final Interpolator mInterpolator;

    public ReverseInterpolator(Interpolator interpolator) {
        mInterpolator = interpolator;
    }

    @Override
    public float getInterpolation(float input) {
        return mInterpolator.getInterpolation(reverseInput(input));
    }

    private float reverseInput(float input) {
        if (input <= 0.5) {
            return input * 2;
        } else {
            return Math.abs(input - 1) * 2;
        }
    }
}