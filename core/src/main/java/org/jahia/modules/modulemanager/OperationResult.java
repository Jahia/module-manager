package org.jahia.modules.modulemanager;

import java.io.Serializable;

/**
 * An interface that represent the processed module operation result.

 * @author bdjiba
 */
public interface OperationResult extends Serializable {

  /**
   * Get the operation result message
   * @return the message
   */
  String getMessage();
  
  /**
   * Get the operation result flag
   * @return true if the operation is successfully performed otherwise false
   */
  boolean isSuccess();
}
