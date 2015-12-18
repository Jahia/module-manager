/**
 * 
 */
package org.jahia.modules.modulemanager.payload;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlType;

/**
 * @author bdjiba
 *
 */
@XmlType(propOrder={"symbolicName", "version"})
public class BundleInfo implements Serializable{
  private static final long serialVersionUID = 1L;
  
  private String symbolicName;
  private String version;

  /**
   * 
   */
  public BundleInfo() {
  }
  
  public BundleInfo(String bundleSymbolicName, String bundleVersion) {
    this.symbolicName = bundleSymbolicName;
    this.version = bundleVersion;
  }

  /**
   * @return the symbolicName
   */
  public String getSymbolicName() {
    return symbolicName;
  }

  /**
   * @param symbolicName the symbolicName to set
   */
  public void setSymbolicName(String symbolicName) {
    this.symbolicName = symbolicName;
  }

  /**
   * @return the version
   */
  public String getVersion() {
    return version;
  }

  /**
   * @param version the version to set
   */
  public void setVersion(String version) {
    this.version = version;
  }
  
  /**
   * Gets the bundle informations in an array
   * @return the bundle information array
   */
  public String[] getInfos() {
    return new String[] {this.symbolicName, this.version};
  }

}
