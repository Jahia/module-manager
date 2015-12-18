package org.jahia.modules.modulemanager;

import java.io.Serializable;
import java.util.List;

import org.jahia.modules.modulemanager.payload.BundleInfo;

/**
 * An interface that represent the processed module operation result.

 * @author bdjiba
 */
public interface OperationResult extends Serializable {

  /**
   * Get the operation result messages
   * @return the message
   */
  String[] getMessages();
  
  /**
   * Get the operation result flag
   * @return true if the operation is successfully performed otherwise false
   */
  boolean isSuccess();
  
  /**
   * Get the information of the list of bundles
   * @return
   */
  List<BundleInfo> getBundleInfoList();
  
  /**
   * Add a message to the result
   * @param v the message to add
   */
  void addMessage(String v);
}
