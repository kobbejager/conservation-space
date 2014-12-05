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
package org.activiti.engine.impl.pvm.runtime;

import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.impl.pvm.process.ScopeImpl;


// TODO: Auto-generated Javadoc
/**
 * The Class AtomicOperationTransitionNotifyListenerEnd.
 *
 * @author Tom Baeyens
 */
public class AtomicOperationTransitionNotifyListenerEnd extends AbstractEventAtomicOperation {

  /* (non-Javadoc)
   * @see org.activiti.engine.impl.pvm.runtime.AbstractEventAtomicOperation#getScope(org.activiti.engine.impl.pvm.runtime.InterpretableExecution)
   */
  @Override
  protected ScopeImpl getScope(InterpretableExecution execution) {
    return (ScopeImpl) execution.getActivity();
  }

  /* (non-Javadoc)
   * @see org.activiti.engine.impl.pvm.runtime.AbstractEventAtomicOperation#getEventName()
   */
  @Override
  protected String getEventName() {
    return org.activiti.engine.impl.pvm.PvmEvent.EVENTNAME_END;
  }

  /* (non-Javadoc)
   * @see org.activiti.engine.impl.pvm.runtime.AbstractEventAtomicOperation#eventNotificationsCompleted(org.activiti.engine.impl.pvm.runtime.InterpretableExecution)
   */
  @Override
  protected void eventNotificationsCompleted(InterpretableExecution execution) {
    execution.performOperation(TRANSITION_DESTROY_SCOPE);
  }
}