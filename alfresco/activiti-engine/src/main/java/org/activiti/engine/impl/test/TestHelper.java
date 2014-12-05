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

package org.activiti.engine.impl.test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import junit.framework.AssertionFailedError;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.impl.ProcessEngineImpl;
import org.activiti.engine.impl.bpmn.deployer.BpmnDeployer;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.db.DbSqlSession;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.jobexecutor.JobExecutor;
import org.activiti.engine.impl.util.ClassNameUtil;
import org.activiti.engine.impl.util.ReflectUtil;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.test.Deployment;


// TODO: Auto-generated Javadoc
/**
 * The Class TestHelper.
 *
 * @author Tom Baeyens
 */
public abstract class TestHelper {
  
  /** The log. */
  private static Logger log = Logger.getLogger(TestHelper.class.getName());

  /** The Constant EMPTY_LINE. */
  public static final String EMPTY_LINE = "                                                                                           ";

  /** The Constant TABLENAMES_EXCLUDED_FROM_DB_CLEAN_CHECK. */
  public static final List<String> TABLENAMES_EXCLUDED_FROM_DB_CLEAN_CHECK = Arrays.asList(
    "ACT_GE_PROPERTY"
  );

  /** The process engines. */
  static Map<String, ProcessEngine> processEngines = new HashMap<String, ProcessEngine>(); 
  
  /**
   * Assert process ended.
   *
   * @param processEngine the process engine
   * @param processInstanceId the process instance id
   */
  public static void assertProcessEnded(ProcessEngine processEngine, String processInstanceId) {
    ProcessInstance processInstance = processEngine
      .getRuntimeService()
      .createProcessInstanceQuery()
      .processInstanceId(processInstanceId)
      .singleResult();
    
    if (processInstance!=null) {
      throw new AssertionFailedError("expected finished process instance '"+processInstanceId+"' but it was still in the db"); 
    }
  }
  
  /**
   * Annotation deployment set up.
   *
   * @param processEngine the process engine
   * @param testClass the test class
   * @param methodName the method name
   * @return the string
   */
  public static String annotationDeploymentSetUp(ProcessEngine processEngine, Class<?> testClass, String methodName) {
    String deploymentId = null;
    Method method = null;
    try {
      method = testClass.getDeclaredMethod(methodName, (Class<?>[])null);
    } catch (Exception e) {
      throw new ActivitiException("can't get method by reflection", e);
    }
    Deployment deploymentAnnotation = method.getAnnotation(Deployment.class);
    if (deploymentAnnotation != null) {
      log.fine("annotation @Deployment creates deployment for "+ClassNameUtil.getClassNameWithoutPackage(testClass)+"."+methodName);
      String[] resources = deploymentAnnotation.resources();
      if (resources.length == 0) {
        String name = method.getName();
        String resource = getBpmnProcessDefinitionResource(testClass, name);
        resources = new String[]{resource};
      }
      
      DeploymentBuilder deploymentBuilder = processEngine.getRepositoryService()
        .createDeployment()
        .name(ClassNameUtil.getClassNameWithoutPackage(testClass)+"."+methodName);
      
      for (String resource: resources) {
        deploymentBuilder.addClasspathResource(resource);
      }
      
      deploymentId = deploymentBuilder.deploy().getId();
    }
    
    return deploymentId;
  }
  
  /**
   * Annotation deployment tear down.
   *
   * @param processEngine the process engine
   * @param deploymentId the deployment id
   * @param testClass the test class
   * @param methodName the method name
   */
  public static void annotationDeploymentTearDown(ProcessEngine processEngine, String deploymentId, Class<?> testClass, String methodName) {
    log.fine("annotation @Deployment deletes deployment for "+ClassNameUtil.getClassNameWithoutPackage(testClass)+"."+methodName);
    if(deploymentId != null) {
      processEngine.getRepositoryService().deleteDeployment(deploymentId, true);      
    }
  }

  /**
   * get a resource location by convention based on a class (type) and a
   * relative resource name. The return value will be the full classpath
   * location of the type, plus a suffix built from the name parameter:
   * <code>BpmnDeployer.BPMN_RESOURCE_SUFFIXES</code>.
   * The first resource matching a suffix will be returned.
   *
   * @param type the type
   * @param name the name
   * @return the bpmn process definition resource
   */
  public static String getBpmnProcessDefinitionResource(Class< ? > type, String name) {
    for (String suffix : BpmnDeployer.BPMN_RESOURCE_SUFFIXES) {
      String resource = type.getName().replace('.', '/') + "." + name + "." + suffix;
      InputStream inputStream = ReflectUtil.getResourceAsStream(resource);
      if (inputStream == null) {
        continue;
      } else {
        return resource;
      }
    }
    return type.getName().replace('.', '/') + "." + name + "." + BpmnDeployer.BPMN_RESOURCE_SUFFIXES[0];
  }

  /**
   * Each test is assumed to clean up all DB content it entered.
   * After a test method executed, this method scans all tables to see if the DB is completely clean.
   * It throws AssertionFailed in case the DB is not clean.
   * If the DB is not clean, it is cleaned by performing a create a drop.
   *
   * @param processEngine the process engine
   */
  public static void assertAndEnsureCleanDb(ProcessEngine processEngine) {
    log.fine("verifying that db is clean after test");
    Map<String, Long> tableCounts = processEngine.getManagementService().getTableCount();
    StringBuilder outputMessage = new StringBuilder();
    for (String tableName : tableCounts.keySet()) {
      if (!TABLENAMES_EXCLUDED_FROM_DB_CLEAN_CHECK.contains(tableName)) {
        Long count = tableCounts.get(tableName);
        if (count!=0L) {
          outputMessage.append("  "+tableName + ": " + count + " record(s) ");
        }
      }
    }
    if (outputMessage.length() > 0) {
      outputMessage.insert(0, "DB NOT CLEAN: \n");
      log.severe(EMPTY_LINE);
      log.severe(outputMessage.toString());

      ((ProcessEngineImpl)processEngine)
        .getProcessEngineConfiguration()
        .getCommandExecutorTxRequired()
        .execute(new Command<Object>() {
          public Object execute(CommandContext commandContext) {
            DbSqlSession dbSqlSession = commandContext.getSession(DbSqlSession.class);
            dbSqlSession.dbSchemaDrop();
            dbSqlSession.dbSchemaCreate();
            return null;
          }
        });
      
      throw new AssertionError(outputMessage.toString());
    }
  }
  
  /**
   * Wait for job executor to process all jobs.
   *
   * @param processEngineConfiguration the process engine configuration
   * @param maxMillisToWait the max millis to wait
   * @param intervalMillis the interval millis
   */
  public static void waitForJobExecutorToProcessAllJobs(ProcessEngineConfigurationImpl processEngineConfiguration, long maxMillisToWait, long intervalMillis) {
    JobExecutor jobExecutor = processEngineConfiguration.getJobExecutor();
    jobExecutor.start();

    try {
      Timer timer = new Timer();
      InteruptTask task = new InteruptTask(Thread.currentThread());
      timer.schedule(task, maxMillisToWait);
      boolean areJobsAvailable = true;
      try {
        while (areJobsAvailable && !task.isTimeLimitExceeded()) {
          Thread.sleep(intervalMillis);
          areJobsAvailable = areJobsAvailable(processEngineConfiguration);
        }
      } catch (InterruptedException e) {
      } finally {
        timer.cancel();
      }
      if (areJobsAvailable) {
        throw new ActivitiException("time limit of " + maxMillisToWait + " was exceeded");
      }

    } finally {
      jobExecutor.shutdown();
    }
  }

  /**
   * Are jobs available.
   *
   * @param processEngineConfiguration the process engine configuration
   * @return true, if successful
   */
  public static boolean areJobsAvailable(ProcessEngineConfigurationImpl processEngineConfiguration) {
    return !processEngineConfiguration
      .getManagementService()
      .createJobQuery()
      .executable()
      .list()
      .isEmpty();
  }

  /**
   * The Class InteruptTask.
   */
  private static class InteruptTask extends TimerTask {
    
    /** The time limit exceeded. */
    protected boolean timeLimitExceeded = false;
    
    /** The thread. */
    protected Thread thread;
    
    /**
     * Instantiates a new interupt task.
     *
     * @param thread the thread
     */
    public InteruptTask(Thread thread) {
      this.thread = thread;
    }
    
    /**
     * Checks if is time limit exceeded.
     *
     * @return true, if is time limit exceeded
     */
    public boolean isTimeLimitExceeded() {
      return timeLimitExceeded;
    }
    
    /* (non-Javadoc)
     * @see java.util.TimerTask#run()
     */
    public void run() {
      timeLimitExceeded = true;
      thread.interrupt();
    }
  }

  /**
   * Gets the process engine.
   *
   * @param configurationResource the configuration resource
   * @return the process engine
   */
  public static ProcessEngine getProcessEngine(String configurationResource) {
    ProcessEngine processEngine = processEngines.get(configurationResource);
    if (processEngine==null) {
      log.fine("==== BUILDING PROCESS ENGINE ========================================================================");
      processEngine = ProcessEngineConfiguration
        .createProcessEngineConfigurationFromResource(configurationResource)
        .buildProcessEngine();
      log.fine("==== PROCESS ENGINE CREATED =========================================================================");
      processEngines.put(configurationResource, processEngine);
    }
    return processEngine;
  }

  /**
   * Close process engines.
   */
  public static void closeProcessEngines() {
    for (ProcessEngine processEngine: processEngines.values()) {
      processEngine.close();
    }
    processEngines.clear();
  }
}