/**
 * 
 */
package org.jahia.modules.modulemanager.payload;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * @author bdjiba
 *
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.NONE)
@JsonSubTypes.Type(value = OperationResult.class)
public interface OperationResult extends Serializable {

  /**
   * Get the operation result message
   * @return the message
   */
  String getMessage();
  
  /**
   * Set the operation result message
   * @param v
   */
  void setMessage(String v);

  /**
   * Get the operation result flag
   * @return true if the operation is successfully performed otherwise false
   */
  boolean isSuccess();
 
  /**
   * Set the operation status value
   * @param v the status
   */
  void setSuccess(boolean v);
}
