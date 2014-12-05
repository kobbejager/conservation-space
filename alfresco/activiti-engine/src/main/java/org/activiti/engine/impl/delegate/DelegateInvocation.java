/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.engine.impl.delegate;

import org.activiti.engine.impl.interceptor.DelegateInterceptor;

// TODO: Auto-generated Javadoc
/**
 * Provides context about the invocation of usercode and handles the actual
 * invocation.
 *
 * @author Daniel Meyer
 * @see DelegateInterceptor
 */
public abstract class DelegateInvocation {

  /** The invocation result. */
  protected Object invocationResult;
  
  /** The invocation parameters. */
  protected Object[] invocationParameters;

  /**
   * make the invocation proceed, performing the actual invocation of the user
   * code.
   * 
   * @throws Exception
   *           the exception thrown by the user code
   */
  public void proceed() throws Exception {
    invoke();
  }

  /**
   * Invoke.
   *
   * @throws Exception the exception
   */
  protected abstract void invoke() throws Exception;

  /**
   * Gets the invocation result.
   *
   * @return the result of the invocation (can be null if the invocation does
   * not return a result)
   */
  public Object getInvocationResult() {
    return invocationResult;
  }

  /**
   * Gets the invocation parameters.
   *
   * @return an array of invocation parameters (null if the invocation takes no
   * parameters)
   */
  public Object[] getInvocationParameters() {
    return invocationParameters;
  }
  
  /**
   * returns the target of the current invocation, ie. JavaDelegate, ValueExpression ...
   *
   * @return the target
   */
  public abstract Object getTarget();

}