/**
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 .
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */
package org.jboss.loom.migrators.logging.jaxb;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.xml.bind.annotation.*;
import org.eclipse.persistence.oxm.annotations.XmlPath;
import org.jboss.loom.migrators.OriginWiseJaxbBase;
import org.jboss.loom.spi.IConfigFragment;
import org.jboss.loom.spi.ann.ConfigPartDescriptor;

/**
 * JAXB bean for appender (AS5)
 *
 * @author Roman Jakubco
 */
@ConfigPartDescriptor(
    name = "Logging appender ${appenderName}"
)
@XmlRootElement(name = "appender")
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "appender")
public class AppenderBean extends OriginWiseJaxbBase<AppenderBean> implements IConfigFragment {

    @XmlAttribute(name = "name")
    private String appenderName;

    @XmlAttribute(name = "class")
    private String appenderClass;

    @XmlElements(@XmlElement(name = "param", type = ParameterBean.class))
    private Set<ParameterBean> parameters;

    @XmlPath("appender-ref/@ref")
    private Set<String> appenderRefs;

    @XmlPath("layout/param/@name")
    private String layoutParamName;

    @XmlPath("layout/param/@value")
    private String layoutParamValue;

    
    public String getAppenderName() { return appenderName; }
    public void setAppenderName(String appenderName) { this.appenderName = appenderName; }
    public String getAppenderClass() { return appenderClass; }
    public void setAppenderClass(String appenderClass) { this.appenderClass = appenderClass; }
    public Set<ParameterBean> getParameters() { return parameters; }
    public void setParameters(Collection<ParameterBean> parameters) {
        Set<ParameterBean> temp = new HashSet();
        temp.addAll(parameters);
        this.parameters = temp;
    }
    public String getLayoutParamName() { return layoutParamName; }
    public void setLayoutParamName(String layoutParamName) { this.layoutParamName = layoutParamName; }
    public String getLayoutParamValue() { return layoutParamValue; }
    public void setLayoutParamValue(String layoutParamValue) { this.layoutParamValue = layoutParamValue; }
    public Set<String> getAppenderRefs() { return appenderRefs; }
    public void setAppenderRefs(Collection<String> appenderRefs) {
        Set<String> temp = new HashSet();
        temp.addAll(appenderRefs);
        this.appenderRefs = temp;
    }
    
}// class