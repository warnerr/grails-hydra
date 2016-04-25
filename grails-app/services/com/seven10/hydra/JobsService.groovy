package com.seven10.hydra

import com.google.gson.JsonObject
import com.seven10.database.DBInstance
import com.seven10.database.DBInstanceOptions
import com.seven10.databasemanager.agent.AgentDBManager
import com.seven10.databasemanager.device.DeviceDbManager
import com.seven10.databasemanager.job.JobDBManager
import com.seven10.databasemanager.user.UserDbManager
import com.seven10.hydra.servlets.helpers.DbMgrFactory
import com.seven10.hydrawebservice.helpers.JsonObjectTransformer
import com.seven10.hydrawebservice.security.HydraSecurityManager
import com.seven10.metrics_manager.RabbitMetricsManager
import com.seven10.schedulermanager.SchedulerManager
import org.springframework.http.HttpRequest

import javax.xml.datatype.DatatypeConfigurationException

/**
 * Created by root on 4/25/16.
 */
class JobsService {

    private final static String MONITOR_TOPIC_NAME 		= "PROCESS_METRICS";
    def grailsApplication

    def getJobs(HttpRequest request) {

        String mongoList = grailsApplication.config.getProperty('hydra.mongoList')
        String[] properties = mongoList.split(':')
        try {

            SchedulerManager schedulerManager = new SchedulerManager(MONITOR_TOPIC_NAME);

            JobDBManager jobDbMgr = new JobDBManager(properties[0], properties[1] as Integer, new DBInstanceOptions())
            AgentDBManager agentMgr = DbMgrFactory.createAgentDbMgr();
            DeviceDbManager deviceDbManager = DbMgrFactory.createDeviceDbMgr();
            UserDbManager userDbMgr = DbMgrFactory.createUserDbMgr();
            DeviceDbManager deviceDbMgr = DbMgrFactory.createDeviceDbMgr();
            HydraSecurityManager hydraSecurityManager = new HydraSecurityManager(userDbMgr);
            RabbitMetricsManager rabbitMetricsManager = new RabbitMetricsManager();
            JobHandler jobHandler = new JobHandler(jobDbMgr, deviceDbManager, schedulerManager, rabbitMetricsManager, hydraSecurityManager)
            return jobHandler.doGetAllJobs()
        }
        catch (DatatypeConfigurationException e)
        {
            throw e
        }
    }

    def createJob(Map data) {
        String mongoList = grailsApplication.config.getProperty('hydra.mongoList')
        String[] properties = mongoList.split(':')
        try {

            SchedulerManager schedulerManager = new SchedulerManager(MONITOR_TOPIC_NAME);

            JobDBManager jobDbMgr = new JobDBManager(properties[0], properties[1] as Integer, new DBInstanceOptions())
            AgentDBManager agentMgr = DbMgrFactory.createAgentDbMgr();
            DeviceDbManager deviceDbManager = DbMgrFactory.createDeviceDbMgr();
            UserDbManager userDbMgr = DbMgrFactory.createUserDbMgr();
            DeviceDbManager deviceDbMgr = DbMgrFactory.createDeviceDbMgr();
            HydraSecurityManager hydraSecurityManager = new HydraSecurityManager(userDbMgr);
            RabbitMetricsManager rabbitMetricsManager = new RabbitMetricsManager();
            JobHandler jobHandler = new JobHandler(jobDbMgr, deviceDbManager, schedulerManager, rabbitMetricsManager, hydraSecurityManager)
            return jobHandler.doPostJobAdd(JsonObjectTransformer.convertStringToJson(data as String))
        }
        catch (DatatypeConfigurationException e)
        {
            throw e
        }
    }
}
