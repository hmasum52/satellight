/*
 * Copyright (c) 2016 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration. All Rights Reserved.
 */

package github.hmasum18.satellight.experimental;

import android.content.res.Resources;

import github.hmasum18.satellight.R;
import gov.nasa.worldwind.util.Logger;
import gov.nasa.worldwind.util.WWUtil;

public class SkyProgram extends AtmosphereProgram {

    public static final Object KEY = SkyProgram.class;

    public SkyProgram(Resources resources) {
        try {
            String vs = WWUtil.readResourceAsText(resources, R.raw.gov_nasa_worldwind_skyprogram_vert);
            String fs = WWUtil.readResourceAsText(resources, R.raw.gov_nasa_worldwind_skyprogram_frag);
            this.setProgramSources(vs, fs);
            this.setAttribBindings("vertexPoint");
        } catch (Exception logged) {
            Logger.logMessage(Logger.ERROR, "SkyProgram", "constructor", "errorReadingProgramSource", logged);
        }
    }
}
