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

import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.task.Comment;


// TODO: Auto-generated Javadoc
/**
 * The Class GetProcessInstanceCommentsCmd.
 *
 * @author Tom Baeyens
 */
public class GetProcessInstanceCommentsCmd implements Command<List<Comment>>, Serializable {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 1L;
  
  /** The process instance id. */
  protected String processInstanceId;
  
  /**
   * Instantiates a new gets the process instance comments cmd.
   *
   * @param processInstanceId the process instance id
   */
  public GetProcessInstanceCommentsCmd(String processInstanceId) {
    this.processInstanceId = processInstanceId;
  }

  /* (non-Javadoc)
   * @see org.activiti.engine.impl.interceptor.Command#execute(org.activiti.engine.impl.interceptor.CommandContext)
   */
  @SuppressWarnings("unchecked")
  public List<Comment> execute(CommandContext commandContext) {
    return commandContext
      .getCommentManager()
      .findCommentsByProcessInstanceId(processInstanceId);
  }
}