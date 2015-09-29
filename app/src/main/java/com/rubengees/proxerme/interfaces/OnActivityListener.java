/*
 *   Copyright 2015 Ruben Gees
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.rubengees.proxerme.interfaces;

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
public interface OnActivityListener {
    /**
     * A Method that Fragments should implement. It is called, when the user clicks the `back`
     * Button.
     *
     * @return true, if the call was consumed by the Fragment or false if it was not and the caller
     * should consume it.
     */
    boolean onBackPressed();

    void showErrorIfNecessary();
}
