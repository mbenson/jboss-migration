package cz.fi.muni.jboss.Migration.Logging;

import org.eclipse.persistence.oxm.annotations.XmlPath;

import javax.xml.bind.annotation.*;

/**
 * Created with IntelliJ IDEA.
 * User: Roman Jakubco
 * Date: 10/2/12
 * Time: 8:14 PM
 */
@XmlRootElement(name = "console-handler")
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "console-handler")
public class ConsoleHandler{
    @XmlAttribute(name = "name")
    private String name;
    //TODO: @XmlPath("encoding")
    @XmlPath("level/@name")
    private String level;
    @XmlPath("filter/@value")
    private String filter;
    @XmlPath("formatter/pattern-formatter/@pattern")
    private String formatter;
    @XmlAttribute(name = "autoflush")
    private String autoflush;
    @XmlPath("target/@name")
    private String target;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public String getFormatter() {
        return formatter;
    }

    public void setFormatter(String formatter) {
        this.formatter = formatter;
    }

    public String getAutoflush() {
        return autoflush;
    }

    public void setAutoflush(String autoflush) {
        this.autoflush = autoflush;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }
}