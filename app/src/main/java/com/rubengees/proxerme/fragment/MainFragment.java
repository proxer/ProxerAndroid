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

package com.rubengees.proxerme.fragment;

import android.support.v4.app.Fragment;

import com.rubengees.proxerme.activity.DashboardActivity;
import com.rubengees.proxerme.interfaces.OnActivityListener;

/**
 * An abstract {@link Fragment}, which all other Fragments should inherit from. It provides some
 * useful methods and implements interfaces, all inheritors share.
 */
public abstract class MainFragment extends Fragment implements OnActivityListener {

    public MainFragment() {
        
    }

    protected DashboardActivity getDashboardActivity(){
        try {
            return (DashboardActivity) getActivity();
        }catch(ClassCastException e){
            throw new RuntimeException("Don't use this Fragment in another" +
                    " Activity than DashboardActivity.");
        }
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    @Override
    public void showErrorIfNecessary() {

    }
}
