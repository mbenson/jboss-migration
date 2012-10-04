package cz.fi.muni.jboss.Migration.Security;

import javax.xml.bind.annotation.*;

/**
 * Created with IntelliJ IDEA.
 * User: Roman Jakubco
 * Date: 10/2/12
 * Time: 9:04 PM
 */
@XmlRootElement(name = "module-option")
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "module-option")
public class ModuleOptionAS5{
    @XmlValue
    private String moduleValue;
    @XmlAttribute(name = "name")
    private String moduleName;

    public String getModuleValue() {
        return moduleValue;
    }

    public void setModuleValue(String moduleValue) {
        this.moduleValue = moduleValue;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }
}