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

package org.activiti.engine.impl.cmd;

import java.io.Serializable;
import java.util.List;

import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.task.Task;


// TODO: Auto-generated Javadoc
/**
 * The Class GetSubTasksCmd.
 *
 * @author Tom Baeyens
 */
public class GetSubTasksCmd implements Command<List<Task>>, Serializable {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 1L;
  
  /** The parent task id. */
  protected String parentTaskId;
  
  /**
   * Instantiates a new gets the sub tasks cmd.
   *
   * @param parentTaskId the parent task id
   */
  public GetSubTasksCmd(String parentTaskId) {
    this.parentTaskId = parentTaskId;
  }

  /* (non-Javadoc)
   * @see org.activiti.engine.impl.interceptor.Command#execute(org.activiti.engine.impl.interceptor.CommandContext)
   */
  public List<Task> execute(CommandContext commandContext) {
    return Context
      .getCommandContext()
      .getTaskManager()
      .findTasksByParentTaskId(parentTaskId);
  }

}