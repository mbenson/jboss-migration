package cz.fi.muni.jboss.migration.security;

import javax.xml.bind.annotation.*;
import java.util.Collection;

/**
 * 
 * @author  Roman Jakubco
 * Date: 9/23/12
 * Time: 6:28 PM
 */
@XmlRootElement(name = "subsystem")
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "subsystem")
public class SecurityAS7 {
//    @XmlAttribute(name = "xmlns")
//    private String xmlns="urn:jboss:domain:security:1.1";
    @XmlElementWrapper(name = "security-domains")
    @XmlElements(@XmlElement(name = "security-domain", type = SecurityDomain.class))
    private Collection<SecurityDomain> securityDomains;

//    public String getXmlns() {
//        return xmlns;
//    }
//
//    public void setXmlns(String xmlns) {
//        this.xmlns = xmlns;
//    }

    public Collection<SecurityDomain> getSecurityDomains() {
        return securityDomains;
    }

    public void setSecurityDomains(Collection<SecurityDomain> securityDomains) {
        this.securityDomains = securityDomains;
    }




}
