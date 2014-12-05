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

import org.activiti.engine.ActivitiException;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.GroupEntity;


// TODO: Auto-generated Javadoc
/**
 * The Class SaveGroupCmd.
 *
 * @author Joram Barrez
 */
public class SaveGroupCmd implements Command<Void>, Serializable {
  
  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 1L;
  
  /** The group. */
  protected GroupEntity group;
  
  /**
   * Instantiates a new save group cmd.
   *
   * @param group the group
   */
  public SaveGroupCmd(GroupEntity group) {
    this.group = group;
  }
  
  /* (non-Javadoc)
   * @see org.activiti.engine.impl.interceptor.Command#execute(org.activiti.engine.impl.interceptor.CommandContext)
   */
  public Void execute(CommandContext commandContext) {
    if(group == null) {
      throw new ActivitiException("group is null");
    }
    if (group.getRevision()==0) {
      commandContext
        .getGroupManager()
        .insertGroup(group);
    } else {
      commandContext
        .getGroupManager()
        .updateGroup(group);
    }
    
    return null;
  }

}