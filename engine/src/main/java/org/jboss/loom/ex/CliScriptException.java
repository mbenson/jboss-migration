/**
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 .
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */
package org.jboss.loom.ex;

/**
 * Exception representing error in methods for generating CLI scripts.
 *
 * @author Roman Jakubco
 */

public class CliScriptException extends MigrationException {

    public CliScriptException(String message) {
        super(message);
    }

    public CliScriptException(String message, Throwable cause) {
        super(message, cause);
    }

    public CliScriptException(Throwable cause) {
        super(cause);
    }
}