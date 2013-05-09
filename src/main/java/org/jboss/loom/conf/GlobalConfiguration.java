/**
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 .
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */
package org.jboss.loom.conf;

import java.util.HashSet;
import java.util.Set;


/**
 * Class for storing global information needed for migration. Like dir of AS7, AS5, and profiles
 * <p/>
 * With regard of possibility to migrate from other vendors' AS,
 * split to AS 7 config class, and then 1 class per server (AS 5, WebLogic, ...).
 *
 * @author Roman Jakubco
 */
public class GlobalConfiguration {


    // AS 7 stuff
    private AS7Config as7Config = new AS7Config();

    // AS 5 stuff
    private AS5Config as5config = new AS5Config();


    // Non-server stuff
    private Set<String> appPaths = new HashSet();

    private boolean skipValidation;


    //<editor-fold defaultstate="collapsed" desc="get/set">
    public AS7Config getAS7Config() {
        return as7Config;
    }
    //public void setAS7Config(AS7Config as7Config) { this.as7Config = as7Config; }

    public AS5Config getAS5Config() {
        return as5config;
    }
    //public void setAS5Config(As5Config as5config) { this.as5config = as5config; }

    public Set<String> getAppPaths() {
        return appPaths;
    }

    public void addAppPath(String appPath) {
        this.appPaths.add( appPath );
    }

    public boolean isSkipValidation() {
        return skipValidation;
    }

    public void setSkipValidation(boolean skipValidation) {
        this.skipValidation = skipValidation;
    }
    //</editor-fold>


}// class
