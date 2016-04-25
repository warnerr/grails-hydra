package com.seven10.hydra;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.seven10.database.DBInvalidStateException;
import com.seven10.databasemanager.DBManagerException;
import com.seven10.databasemanager.DBManagerNotFoundException;
import com.seven10.databasemanager.device.DeviceDbManager;
import com.seven10.databasemanager.job.JobDBManager;
import com.seven10.databasemanager.job.object.*;
import com.seven10.hydra.servlets.helpers.*;
import com.seven10.hydra_util.execution.HydraSettings;
import com.seven10.hydrawebservice.exception.BadRequestException;
import com.seven10.hydrawebservice.exception.InvalidMediaTypeException;
import com.seven10.hydrawebservice.exception.NotFoundException;
import com.seven10.hydrawebservice.exception.ServerErrorException;
import com.seven10.hydrawebservice.helpers.EndpointResponse;
import com.seven10.hydrawebservice.helpers.JsonObjectTransformer;
import com.seven10.hydrawebservice.helpers.RestFileStreamer;
import com.seven10.hydrawebservice.security.HydraSecurityManager;
import com.seven10.metrics_manager.RabbitMetricsManager;
import com.seven10.rabbitmq.RabbitAdmin;
import com.seven10.rabbitmq.RabbitPinger;
import com.seven10.restobjects.RestObject;
import com.seven10.restobjects.devices.resource.DeviceResourceType;
import com.seven10.restobjects.devices.resource.Export;
import com.seven10.restobjects.devices.resource.Share;
import com.seven10.restobjects.jobs.JobItem;
import com.seven10.restobjects.jobs.JobStateRequest;
import com.seven10.restobjects.jobs.JobStatus;
import com.seven10.restobjects.jobs.JobType;
import com.seven10.restobjects.jobs.migration.*;
import com.seven10.restobjects.jobs.report.MigrationStatusReport;
import com.seven10.schedulermanager.SchedulerManager;
import com.seven10.schedulermanager.SimpleTriggerType;
import com.seven10.schedulermanager.jobs.MigrationJob;
import com.seven10.schedulermanager.jobs.SchedulerManagerJob;
import com.seven10.schedulermanager.logging.CsvLogger;
import com.seven10.work.serializer.JsonWorkSerializer;
import com.seven10.work.types.MonitorWork;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidParameterException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Created by root on 4/25/16.
 */
public class JobHandler extends BaseHandler {
    private static final Logger logger = LogManager.getFormatterLogger(JobHandler.class.getName());

    /**
     * End Points
     */
    private static final String		JOBS_ENDPOINT 					= "/jobs";
    private static final String		JOBS_MIGRATION_CUTOVER_ENDPOINT	= "/jobs/migration/cutover";
    private static final String		JOBS_EXCEPTION_ENDPOINT 		= "/jobs/exceptions";
    private static final String		JOBS_DATA_ENDPOINT 				= "/jobs/data";
    private static final String		JOBS_METRICS_ENDPOINT 			= "/jobs/metrics";
    private static final String		JOBS_STATUS_ENDPOINT 			= "/jobs/status";
    private static final String		JOBS_LOGS						= "/jobs/logs";

    /**
     * Query Params/Json type names
     */
    public static final String 		API_ID							= "id";
    public static final String 		API_PHASE						= "phase";
    public static final String 		API_COUNT						= "count";
    public static final String 		API_STATUS						= "status";
    public static final String		API_SOURCE_ID					= "source_id";
    public static final String		API_DESTINATION_ID				= "destination_id";

    /**
     * Members used to connect to our other Managers
     */
    private final JobDBManager m_jobDbMgr;
    private final DeviceDbManager m_deviceDbMgr;
    private final SchedulerManager m_schedulerManager;
    private final RabbitMetricsManager m_metricsManager;
    private final JobSchedulerConnector m_jobSechdulerConnector;

    // Map of JobID to the jobKey kept by the Scheduler
    private final Map<String, JobKey> m_jobMap;

    /**
     * Log constants
     * TODO: need to put this in a place accessible by both the Scheduler (who writes these files out) and this class (who sends them to the client)
     */
    private static final String TYPE_PARAM = "type";
    private static final String RESULT_TYPE = "result";
    private static final String FAILURE_TYPE = "failure";

    private static final String LOG_DIR_PREFIX = System.getProperty(HydraSettings.SYSPROP_LOGPATH) + File.separator;
    private static final String RESULT_DIR = LOG_DIR_PREFIX + "results" + File.separator;
    private static final String FAILURE_DIR = LOG_DIR_PREFIX + "failures" + File.separator;

    private static final String CSV_SUFFIX = CsvLogger.CSV_FILE;
    private static final String RESULT_AUDIT_PREFIX = "Results_";
    private static final String FAILURE_AUDIT_PREFIX = "Failures_";

    /**
     * Instantiates a new job http servlet.
     *
     * @param jobDbMgr the job db mgr
     * @param schedulerManager the scheduler manager
     * @param metricsManager the metrics manager
     * @throws IllegalStateException the illegal state exception
     */
    public JobHandler(JobDBManager jobDbMgr,
                          DeviceDbManager		deviceDbMgr,
                          SchedulerManager schedulerManager,
                          RabbitMetricsManager metricsManager,
                          HydraSecurityManager secManager) throws IllegalStateException
    {

        validateParams(jobDbMgr, schedulerManager, metricsManager);

        // Set our members to our params passed in
        this.m_jobDbMgr = jobDbMgr;
        this.m_schedulerManager = schedulerManager;
        this.m_metricsManager = metricsManager;
        this.m_deviceDbMgr = deviceDbMgr;

        // Use the schedule manager to create our connector so we do not have tons of unorganized code
        this.m_jobSechdulerConnector = new JobSchedulerConnector(m_schedulerManager);

        m_jobMap = new HashMap<String, JobKey>();

        //resetAndLoadAllJobs();
    }

    // ******************************************************************************************
    //
    //	General Job Servlet Functionality
    //
    // ******************************************************************************************
    @Override
    public String getBaseEndpoint()
    {
        return JOBS_ENDPOINT;
    }

    private void validateParams(JobDBManager jobDbMgr,
                                SchedulerManager schdeuleManager,
                                RabbitMetricsManager metricsManager)
    {
        if (jobDbMgr == null)
        {
            logger.error(".validateParams(): JobDBManager can not be null");
            throw new IllegalArgumentException("JobDBManager can not be null");
        }

        if (schdeuleManager == null)
        {
            logger.error(".validateParams(): SchedulerManager can not be null");
            throw new IllegalArgumentException("SchedulerManager can not be null");
        }

        if (metricsManager == null)
        {
            logger.error(".validateParams(): MetricsManager can not be null");
            throw new IllegalArgumentException("MetricsManager can not be null");
        }
    }

    /**
     * If any jobs were paused or running at startup, put them into the stopped state
     * along with all of its mappings
     */
    private void resetAndLoadAllJobs()
    {
        // There is a lot of really uncessary conversion here, but to keep this clean of any db objects it must
        // be done.  There is probably a way around this by making a separate class that takes in a list of Jobs
        // but that can be done at a later date.
        try
        {
            logger.trace(".loadAllJobs(): job servlet loading all jobs");
            List<JobItem> jobs = JobRestDBMapper.mapDbToRest(m_jobDbMgr.getAllJobs());

            jobs.forEach(job ->
            {
                // Get the job, set it's status to stopped since this is start up
                // Set all mappings to stopped
                DbJob dbJob = JobRestDBMapper.mapRestToDb(job, false);

                // if the job thinks its running or paused stop it
                if (dbJob.getStatus() == DbJobStatus.RUNNING ||
                        dbJob.getStatus() == DbJobStatus.PAUSED)
                {
                    logger.trace(".loadAllJobs(): job:%s thinks it's running, setting it's status to stopped",
                            job.getId());
                    dbJob.setStatus(DbJobStatus.STOPPED);
                }

                // 'Stop' all mappings
                DBMigrationJobMapping[] mappings = ((DbMigrationJobParameters) dbJob.getJobParams()).getMapping();

                for (int i=0; i<mappings.length; i++)
                {
                    DBMigrationJobMapping mapping = mappings[i];

                    // if the mapping thinks its running or paused
                    if(mapping.getRuntimeStatus() == DbJobRuntimeStatus.RUNNING ||
                            mapping.getRuntimeStatus() == DbJobRuntimeStatus.PAUSED)
                    {
                        //logger.trace(".loadAllJobs(): mapping at index:%d thinks it's running, setting it's status to stopped",
                         //       i);
                        mapping.setRuntimeStatus(DbJobRuntimeStatus.STOPPED);
                    }
                }// end stopping all mappings and the job

                try
                {
                    m_jobDbMgr.updateJob(dbJob);
                }
                catch (DBManagerException e)
                {
                    //logger.error(".loadlAllJobs(): unable to update job:%s",
                    //        job.getId());
                    logger.error(".loadlAllJobs(): unable to update job",
                            e);
                }

                JobMapObject jmo = m_jobSechdulerConnector.registerJob(dbJob,
                        SimpleTriggerType.SimpleTriggerNone,
                        LocalDateTime.ofInstant(new Date().toInstant(),
                                ZoneId.systemDefault()),
                        1,
                        1);

                m_jobMap.put(jmo.getId(), jmo.getJobKey());
                //logger.trace(".loadAllJobs(): loaded job %s %s", job.getId(), job.getName());
            });
        }
        catch (DBManagerException ex)
        {
            logger.error(".loadAllJobs(): failed to get all jobs from db - %s", ex.getMessage());
            logger.trace(".loadAllJobs(): failed to get all jobs from db", ex);
            throw new IllegalStateException("Unable to start job servlet");
        }
        catch (IllegalArgumentException | IllegalStateException | NullPointerException ex)
        {
            logger.error(".loadAllJobs(): failed registerJob() - %s", ex.getMessage());
            logger.trace(".loadAllJobs(): failed registerJob()", ex);
            throw new IllegalStateException("Unable to start job servlet");
        }

        logger.trace(".ctor(): job servlet created");
    }

    // ******************************************************************************************
    //
    //	SERVLET REST CALLS
    //
    // ******************************************************************************************
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException
    {
        // Get the Request URI.  This will show us where to go
        String reqPath = request.getRequestURI();

        logger.trace(".doGet(): processing endpoint %s", reqPath);

        // Validation authorization and if it fails, just return
        if (false == validateAuthHeader(request, response))
        {
            logger.trace(".doGet(): failed authorization");
            return;
        }

        // Response and status code captured for generic write out
        EndpointResponse resp;

        switch(reqPath)
        {
            case (JOBS_ENDPOINT):
            {
                resp = doGetJobsEndpoint(request);
                break;
            }
            case (JOBS_METRICS_ENDPOINT):
            {
                resp = doGetJobsMetricsEndpoint(request);
                break;
            }
            case (JOBS_DATA_ENDPOINT):
            {
                resp = doGetJobsDataEndpoint(request);
                break;
            }
	        /* Keeping this here because we don't want to throw it away yet.
	        case (JOBS_PHASEDATA_ENDPOINT):
	        {
	        	resp = doJobsPhaseDataEndpoint(request);
	        	break;
	        }
	        */
            default: // Catch all for resource path not found
            {
                logger.error(".doGet(): Invalid Endpoint");
                resp = new EndpointResponse( JsonObjectTransformer.formatJSONErrorBody("Fail", "Invalid Endpoint"),
                        HttpServletResponse.SC_NOT_FOUND);
            }
        }// End Switch

        // Write the response to the request
        logger.trace(".doGet(): Writing response: %s", resp);
        super.writeResponseAndStatus(response, resp.getResponse(), BaseHandler.SERVLET_CONTENT_TYPE_JSON,  resp.getStatusCode());
    }

    private EndpointResponse doGetJobsDataEndpoint(HttpServletRequest request)
    {
        logger.trace(".doGetJobsDataEndpoint(): on " + JOBS_DATA_ENDPOINT);
        String id = request.getParameter(API_ID);

        if (id == null)
        {
            logger.trace("'id' parameter can not be null");
            BadRequestException ex = new BadRequestException("'id' parameter can not be null");


            logger.trace(".doGetJobsDataEndpoint(): failed", ex);
            return new EndpointResponse(JsonObjectTransformer.formatJSONErrorBody("Fail", ex.getSimpleError()),
                    ex.getHTTPCode());
        }

        try
        {
            String sourceId = request.getParameter(API_SOURCE_ID);
            String destinationId = request.getParameter(API_DESTINATION_ID);

            // If Either the source ID or Destination ID is null that means we just want to get
            // the entire Jobs mapping data.  That is all mappings reports that belong to a job
            if ( sourceId == null || destinationId == null)
            {
                logger.trace(".doGetJobsDataEndpoint(): Get all job mapping data by job ID");
                return new EndpointResponse( doGetAllJobData(id),
                        HttpServletResponse.SC_OK);
            }

            // If they are not null that means we want to get all the reports for a certain mapping
            logger.trace(".doGetJobsDataEndpoint(): Get job data by job ID, and mapping resource id");
            return new EndpointResponse( doGetJobMappingData(id, sourceId, destinationId),
                    HttpServletResponse.SC_OK);
        }
        catch (ServerErrorException | NotFoundException ex)
        {
            logger.trace(".doGetJobsDataEndpoint(): failed", ex);
            return new EndpointResponse(JsonObjectTransformer.formatJSONErrorBody("Fail", ex.getSimpleError()),
                    ex.getHTTPCode());
        }
        catch (Exception ex)
        {
            logger.error(".doGetJobsDataEndpoint(): Internal Error", ex);
            return new EndpointResponse(JsonObjectTransformer.formatJSONErrorBody("Fail", "Internal Error"),
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private EndpointResponse doGetJobsMetricsEndpoint(HttpServletRequest request)
    {
        try
        {
            logger.trace(".doGetJobsMetricsEndpoint(): on " + JOBS_ENDPOINT + "/metrics");
            return new EndpointResponse(doGetMetrics(request), HttpServletResponse.SC_OK);
        }
        catch (BadRequestException ex)
        {
            logger.error(".doGetJobsMetricsEndpoint(): Bad Request", ex);
            return new EndpointResponse(JsonObjectTransformer.formatJSONErrorBody("Fail", "Bad Request " + ex.getMessage()),
                    ex.getHTTPCode());
        }
    }

    private EndpointResponse doGetJobsEndpoint(HttpServletRequest request)
    {
        logger.trace(".doGetJobsEndpoint(): on " + JOBS_ENDPOINT);

    	/*
		 * See if the request had id as a query parameter
		 * It's fine if it doesn't. However, if it is there but it's null, it will throw
		 */
        String strID = null;
        String strCount = request.getParameter(API_COUNT);
        String strStatus = request.getParameter(API_STATUS);

        try
        {
            strID = requestHasId(request);
        }
        catch (BadRequestException ex)
        {
            logger.error(".doGetJobsEndpoint(): Bad Request", ex);
            return new EndpointResponse( JsonObjectTransformer.formatJSONErrorBody("Fail", ex.getSimpleError()), ex.getHTTPCode());
        }

        if (strCount != null)
        {
            if (strStatus != null)
            {
                try
                {
                    return new EndpointResponse( doGetJobsCount("status",strStatus.toUpperCase()),
                            HttpServletResponse.SC_OK);
                }
                catch (ServerErrorException ex)
                {
                    logger.trace(".doGetJobsEndpoint(): failed", ex);
                    return new EndpointResponse( JsonObjectTransformer.formatJSONErrorBody("Fail", ex.getSimpleError()),
                            ex.getHTTPCode());
                }
            }
            else
            {
                try
                {
                    return new EndpointResponse( doGetJobsCount(), HttpServletResponse.SC_OK);
                }
                catch (ServerErrorException ex)
                {
                    logger.trace(".doGetJobsEndpoint(): failed", ex);
                    return new EndpointResponse(  JsonObjectTransformer.formatJSONErrorBody("Fail", ex.getSimpleError()),
                            ex.getHTTPCode());
                }
            }
        }
        // Get all Jobs
        else if (strID == null)
        {
            try
            {
                logger.trace(".doGetJobsEndpoint(): get all Jobs");
                return new EndpointResponse(
                        doGetAllJobs(), HttpServletResponse.SC_OK);
            }
            catch (ServerErrorException ex)
            {
                logger.trace(".doGetJobsEndpoint(): failed", ex);
                return new EndpointResponse(  JsonObjectTransformer.formatJSONErrorBody("Fail", ex.getSimpleError()),
                        ex.getHTTPCode());
            }
            catch (Exception ex)
            {
                logger.error(".doGetJobsEndpoint(): Internal Error", ex);
                return new EndpointResponse( JsonObjectTransformer.formatJSONErrorBody("Fail", "Internal Error"),
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
        // Get specific Job
        else
        {
            try
            {
                logger.trace(".doGetJobsEndpoint(): get by job ID");
                return new EndpointResponse(doGetJobByID(strID),
                        HttpServletResponse.SC_OK);
            }
            catch (NotFoundException | ServerErrorException ex)
            {
                logger.trace(".doGetJobsEndpoint(): failed", ex);
                return new EndpointResponse( JsonObjectTransformer.formatJSONErrorBody("Fail", ex.getSimpleError()),
                        ex.getHTTPCode());
            }
            catch (Exception ex)
            {
                logger.error(".doGetJobsEndpoint(): Internal Error", ex);
                return new EndpointResponse( JsonObjectTransformer.formatJSONErrorBody("Fail", "Internal Error"),
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }

    }

    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response) throws ServletException, IOException
    {
        // Get the Request URI.  This will show us where to go
        String reqPath = request.getRequestURI();

        logger.trace(".doPost(): processing endpoint %s", reqPath);

        // Validation authorization and if it fails, just return
        if (false == validateAuthHeader(request, response))
        {
            logger.trace(".doPost(): failed authorization");
            return;
        }

        String strResponse = new String();
        int iStatusCode;

        try
        {
            switch (reqPath)
            {
                // End point used to create a job
                case (JOBS_ENDPOINT):
                {
                    logger.trace(".doPost(): hitting endpoint:%s",
                            JOBS_ENDPOINT);

	        		/*
	        		 * validate Id query parameter
	        		 */
                    String strID = request.getParameter("id");

                    // We need a request body to process this otherwise it's invalid
                    JsonObject jsonBodyObject;

                    // Get request body and validate
                    logger.trace(".doPost(): reading request body");
                    jsonBodyObject = super.getBodyJson(request);

                    // Process the valid request (thus far)
                    if (strID == null)
                    {
                        logger.trace(".doPost(): adding new job");
                        strResponse = doPostJobAdd(jsonBodyObject);
                        iStatusCode = HttpServletResponse.SC_CREATED;
                    }
                    else
                    {
                        logger.trace(".doPost(): updating job: " + strID);
                        strResponse = doPostJobUpdate(jsonBodyObject, strID);
                        iStatusCode = HttpServletResponse.SC_ACCEPTED;
                    }
                    break;
                }
                // Change the status of a job
                case (JOBS_STATUS_ENDPOINT):
                {
                    logger.trace(".doPost(): hitting endpoint:%s",
                            JOBS_STATUS_ENDPOINT);

                    // Get request body and validate
                    logger.trace(".doPost(): Reading request body");
                    JobStateRequest stateRequest = RestObject.fromJson(super.getBodyString(request),
                            JobStateRequest.class);

					/*
	        		 * validate Id query parameter
	        		 */
                    String strID = requestHasId(request);

                    if (strID == null)
                    {
                        logger.trace(".doPost(): id parameter missing");
                        throw new BadRequestException("id parameter missing");
                    }

                    if (stateRequest.getStatus() == null)
                    {
                        logger.trace(".doPost(): status body parameter missing");
                        throw new BadRequestException("status body parameter missing");
                    }

                    JobKey jobKey = m_jobMap.get(strID);
                    if (jobKey == null)
                    {
                        logger.trace(".doPost(): failed to find in job map id " + strID);
                        throw new NotFoundException("Could not find job with id " + strID);
                    }

                    // Process the valid request (thus far)
                    // TODO: Send back a response!
                    switch (stateRequest.getStatus())
                    {
                        // TODO: Validate current state of job?
                        case STOP:
                        {
                            m_schedulerManager.interruptJob(jobKey);
                            iStatusCode = HttpServletResponse.SC_OK;
                            break;
                        }
                        case RUNNOW:
                        {
                            try
                            {
                                // See if kafka is connected so we can run successfully
//	        					boolean isConnected = KafkaPinger.isConnected(m_schedulerManager.getZkServers(),
//	        																  m_schedulerManager.getKafkaBrokers());
//

                                boolean isConnected = RabbitPinger.isConnected(RabbitAdmin.generateAddresses(m_schedulerManager.getKafkaBrokers()));

                                if (!isConnected)
                                {
                                    // Get job
                                    DbJob job = m_jobDbMgr.getJob(strID);

                                    // update with error
                                    job.setLastError("Unable to connect to the message broker(s)");
                                    job.setStatus(DbJobStatus.STOPPED_WITH_ERROR);

                                    m_jobDbMgr.updateJob(job);
                                    iStatusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                                    strResponse = JsonObjectTransformer.formatJSONErrorBody("Fail", "Message brokers unavailable");
                                }
                                else
                                {
                                    m_schedulerManager.triggerJob(jobKey);
                                    iStatusCode = HttpServletResponse.SC_OK;
                                }
                            }
                            catch (IllegalStateException ex)
                            {
                                logger.error(".doPost(): failed to trigger job due to state exception - %s", ex.getMessage());
                                logger.trace(".doPost()", ex);
                                throw new NotFoundException("Job not found in scheduler");
                            }
                            break;
                        }
                        default:
                        {
                            strResponse = JsonObjectTransformer.formatJSONErrorBody("Fail", "Invalid status value of " + stateRequest.getStatus().toString());
                            iStatusCode = HttpServletResponse.SC_BAD_REQUEST;
                            break;
                        }
                    }// end switch

                    break;
                }
                case (JOBS_LOGS):
                {
                    logger.trace(".doPost(): hitting endpoint:%s",
                            JOBS_LOGS);

                    EndpointResponse resp = doJobsLogsEndpoint(request, response);
                    strResponse = resp.getResponse();
                    iStatusCode = resp.getStatusCode();

                    break;
                }
                case(JOBS_EXCEPTION_ENDPOINT):
                {
                    logger.trace(".doPost(): hitting endpoint:%s",
                            JOBS_EXCEPTION_ENDPOINT);

                    //TODO: empty body response?
                    iStatusCode = doPostProcessException(request);
                    strResponse = "";
                    break;
                }
                case JOBS_MIGRATION_CUTOVER_ENDPOINT:
                {
                    logger.trace(".doPost(): processing endpoint " + JOBS_MIGRATION_CUTOVER_ENDPOINT);

                    // Get request body and validate
                    logger.trace(".doPost(): reading request body");
                    MigrationJobUpdateMappingRequest requestBody = RestObject.fromJson(super.getBodyString(request),
                            MigrationJobUpdateMappingRequest.class);

					/*
	        		 * validate Id query parameter
	        		 */
                    String strID = requestHasId(request);

                    if (strID == null)
                    {
                        logger.trace(".doPost(): id parameter missing");
                        throw new BadRequestException("id parameter missing");
                    }

                    JobKey jobKey = m_jobMap.get(strID);
                    if (jobKey == null)
                    {
                        logger.trace(".doPost(): failed to find in job map id " + strID);
                        throw new NotFoundException("Could not find job with id " + strID);
                    }

                    // First set the mapping phases.
                    doPostJobUpdateMigrateMappingsPhases(strID,
                            requestBody,
                            DbMigrationPhase.CUTOVER);

                    // See if job is currently running
                    SchedulerManagerJob job = m_schedulerManager.getRunningJob(jobKey);

                    // Job is not current running, we are done.
                    if (job == null)
                    {
                        logger.trace(".doPost(): set cutover phases, job is not currently running so we are done");
                        iStatusCode = HttpServletResponse.SC_OK;
                        break;
                    }

                    logger.trace(".doPost(): set cutover phases, job is currently running, signal it");

                    // Job is running, tell it to look for cutovers.
                    MigrationJob migrationJob;
                    try
                    {
                        migrationJob = (MigrationJob) job;
                    }
                    catch (ClassCastException ex)
                    {
                        logger.error(".doPost: invalid job type " + job.getClass().getName() + " casted for job with key %s" + jobKey.toString());
                        throw new BadRequestException("Job with key " + jobKey.toString()
                                + " found class type " + job.getClass().getName()
                                + " not class type " + MigrationJob.class.getName());
                    }

                    migrationJob.checkForCutovers();
                    iStatusCode = HttpServletResponse.SC_CREATED;
                    break;
                }
                default:
                {
                    logger.error(".doPost(): Invalid Endpoint");
                    strResponse = JsonObjectTransformer.formatJSONErrorBody("Fail", "Invalid Endpoint");
                    iStatusCode = HttpServletResponse.SC_NOT_FOUND;
                    break;
                }
            }// end switch
        }// end try
        catch (InvalidMediaTypeException ex)
        {
            logger.error(".doPost(): Fail", ex);
            strResponse = JsonObjectTransformer.formatJSONErrorBody("Fail", ex.getSimpleError());
            iStatusCode = ex.getHTTPCode();
        }
        catch (JsonParseException ex)
        {
            logger.error(".doPost(): Fail", ex);
            strResponse = JsonObjectTransformer.formatJSONErrorBody("Fail", "Cannot parse body");
            iStatusCode = HttpServletResponse.SC_BAD_REQUEST;
        }
        catch (NotFoundException | BadRequestException | ServerErrorException ex)
        {
            logger.error(".doPost(): Fail", ex);
            strResponse = JsonObjectTransformer.formatJSONErrorBody("Fail", ex.getSimpleError());
            iStatusCode = ex.getHTTPCode();
        }
        catch (IOException ex)
        {
            logger.error(".doPost(): Fail", ex);
            strResponse = JsonObjectTransformer.formatJSONErrorBody("Fail", ex.getMessage());
            iStatusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }
        catch (Exception ex)
        {
            logger.error(".doPost(): Fail", ex);
            strResponse = JsonObjectTransformer.formatJSONErrorBody("Fail", "Internal error - " + ex.getMessage());
            iStatusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }

        super.writeResponseAndStatus(response,
                strResponse,
                BaseHandler.SERVLET_CONTENT_TYPE_JSON,
                iStatusCode);
    }

    protected void doDelete(HttpServletRequest request,
                            HttpServletResponse response) throws ServletException, IOException
    {
        // Get the Request URI.  This will show us where to go
        String reqPath = request.getRequestURI();

        logger.trace(".doDelete(): processing endpoint %s", reqPath);

        // Validation authorization and if it fails, just return
        if (false == validateAuthHeader(request, response))
        {
            logger.trace(".doDelete(): failed authorization");
            return;
        }

        // Response and status code captured for generic write out
        String strResponse = new String();
        int iStatusCode;

        switch(reqPath)
        {
            case (JOBS_ENDPOINT):
            {
	        	/*
        		 * See if the request had id as a query parameter in order to handle a delete.
        		 */
                String strID;

                try
                {
                    strID = requestHasId(request);
                }
                catch (BadRequestException ex)
                {
                    logger.trace(".doDelete(): bad request on id parameter", ex);
                    strResponse = JsonObjectTransformer.formatJSONErrorBody("Fail", ex.getSimpleError());
                    iStatusCode = ex.getHTTPCode();
                    break;
                }

                // Validate
                if (strID == null)
                {
                    logger.trace(".doDelete(): bad request - id parameter missing");
                    strResponse = JsonObjectTransformer.formatJSONErrorBody("Fail", "id parameter missing");
                    iStatusCode = HttpServletResponse.SC_BAD_REQUEST;
                    break;
                }

                try
                {
                    doDeleteJobByID(strID);
                    // If the delete didn't throw, it was succesful, so read no content.
                    iStatusCode = HttpServletResponse.SC_NO_CONTENT;
                    break;
                }
                catch (NotFoundException | ServerErrorException ex)
                {
                    logger.trace(".doDelete(): failed", ex);
                    strResponse = JsonObjectTransformer.formatJSONErrorBody("Fail", ex.getSimpleError());
                    iStatusCode = ex.getHTTPCode();
                    break;
                }
                catch (Exception ex)
                {
                    logger.error(".doDelete(): internal error", ex);
                    strResponse = JsonObjectTransformer.formatJSONErrorBody("Fail", "Internal Error");
                    iStatusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                    break;
                }
            }
            case (JOBS_DATA_ENDPOINT):
            {
                logger.trace(".doDelete(): on " + JOBS_DATA_ENDPOINT);
                String id = request.getParameter(API_ID);

                if (id == null)
                {
                    logger.trace("'id' parameter can not be null");
                    BadRequestException ex = new BadRequestException("'id' parameter can not be null");


                    strResponse = JsonObjectTransformer.formatJSONErrorBody("Fail", ex.getSimpleError());
                    iStatusCode = ex.getHTTPCode();
                    logger.trace(".doDelete(): failed", ex);
                    break;
                }

                try
                {
                    doDeleteJobPhaseReport(id);
                    logger.trace(".doDelete(): deleted data for job id - %s", id);
                    iStatusCode = HttpServletResponse.SC_OK;
                    break;
                }
                catch (ServerErrorException | NotFoundException ex)
                {
                    logger.error(".doDelete(): failed delete data for job id %s - %s", id, ex.getMessage());
                    logger.trace(".doDelete(): failed", ex);
                    strResponse = JsonObjectTransformer.formatJSONErrorBody("Fail", ex.getSimpleError());
                    iStatusCode = ex.getHTTPCode();
                    break;
                }
                catch (Exception ex)
                {
                    logger.error(".doDelete(): failed delete data for job id %s - %s", id, ex.getMessage());
                    logger.trace(".doDelete(): internal error", ex);
                    strResponse = JsonObjectTransformer.formatJSONErrorBody("Fail", "Internal Error");
                    iStatusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                    break;
                }

            }
            default: // Catch all for resource path not found
            {
                logger.error(".doDelete(): Invalid Endpoint");
                strResponse = JsonObjectTransformer.formatJSONErrorBody("Fail", "Invalid endpoint specified - " + reqPath);
                iStatusCode = HttpServletResponse.SC_NOT_FOUND;
            }
        }

        // Write the response to the request
        logger.trace(".doDelete(): Writing response %s with status code %d", strResponse, iStatusCode);
        super.writeResponseAndStatus(response,
                strResponse,
                BaseHandler.SERVLET_CONTENT_TYPE_JSON,
                iStatusCode);
    }

    // ******************************************************************************************
    //
    //	REQUEST SPECIFIC FUNCTIONS
    //
    // ******************************************************************************************

    // ------------------------------
    //
    //	GET REQUEST SPECIFIC
    //
    // ------------------------------
    /**
     * Get a phase report for the job specified
     *
     * @param jobId the job id
     * @return the JobReport
     * @throws ServerErrorException the server error exception
     * @throws NotFoundException the not found exception
     */
    private String doGetAllJobData(String jobId) throws ServerErrorException, NotFoundException
    {
        logger.trace(".doGetJobData(): starting");
        try
        {

            List<MigrationStatusReport> reports = JobRestDBMapper.mapDbToRestReportList(m_jobDbMgr.getJobPhaseReports(jobId));

            if (reports == null)
            {
                logger.debug(".doGetJobData(): failed to find report for job id %s", jobId);
                throw new NotFoundException("Failed to find the report data for job id " + jobId);
            }

            logger.trace(".doGetJobData(): end");
            return RestObject.toJson(reports);
        }
        catch (DBManagerException ex)
        {
            logger.error(".doGetJobData(): failed getLifetimeJobReport for job id %s - %s", jobId, ex.getMessage());
            logger.trace(".doGetJobData()", ex);
            throw new ServerErrorException("Failed to get the report data for job id " + jobId);
        }
    }

    /**
     * Get a phase report for the job specified
     *
     * @param jobId the job id
     * @return the JobReport
     * @throws ServerErrorException the server error exception
     * @throws NotFoundException the not found exception
     */
    private String doGetJobMappingData(String jobId, String sourceId, String destinationId) throws ServerErrorException, NotFoundException
    {
        logger.trace(".doGetJobData(): starting");
        try
        {
            List<MigrationStatusReport> reports = JobRestDBMapper.mapDbToRestReportList(m_jobDbMgr.getTaskAllPhaseReports(jobId ,sourceId, destinationId));

            if (reports == null)
            {
                logger.debug(".doGetJobData(): failed to find report for job id %s", jobId);
                throw new NotFoundException("Failed to find the report data for job id " + jobId);
            }

            /**
             * Class used as a custom comparator to sort based on DbJobMappingPhaseReport start times
             * @author amarcionek@seven10storage.com
             *
             */
            class MigrationStatusReportDateComparator implements Comparator<MigrationStatusReport>
            {
                public int compare(MigrationStatusReport rep1, MigrationStatusReport rep2)
                {
                    return rep1.getStartTime().compareTo(rep2.getStartTime());
                }
            }

            // Sort them
            reports.sort(new MigrationStatusReportDateComparator());

            logger.trace(".doGetJobData(): end");
            return RestObject.toJson(reports);
        }
        catch (DBManagerException ex)
        {
            logger.error(".doGetJobData(): failed getLifetimeJobReport for job id %s - %s", jobId, ex.getMessage());
            logger.trace(".doGetJobData()", ex);
            throw new ServerErrorException("Failed to get the report data for job id " + jobId);
        }
    }

    /**
     * Get all jobs
     * @return {@literal List<JobItem>}
     * @throws ServerErrorException if something else went wrong
     * @throws DBInvalidStateException
     */
    public String doGetAllJobs() throws ServerErrorException, DBInvalidStateException
    {
        logger.trace(".doGetAllJobs(): starting");

        try
        {
            // Get all jobs from the db and convert them
            List<JobItem> allRestJobs = JobRestDBMapper.mapDbToRest(m_jobDbMgr.getAllJobs());

            logger.trace(".doGetAllJobs(): finished - num jobs: %d", allRestJobs.size());

            return RestObject.toJson(allRestJobs);
        }
        catch (DBManagerException ex)
        {
            logger.trace(".doGetAllJobs(): caught exception", ex);
            throw new ServerErrorException("failed getAllJobs() - " + ex.getMessage());
        }
    }

    /**
     * Get job by an ID
     * @param strID
     * @return {@literal JobItem}
     * @throws NotFoundException if the job is not found
     * @throws ServerErrorException if something else went wrong
     */
    private String doGetJobByID(String strID) throws NotFoundException, ServerErrorException
    {
        logger.trace(".doGetJobByID(): Processing Request for id %s", strID);

        try
        {
            DbJob dbJob = m_jobDbMgr.getJob(strID);

            if (dbJob == null)
            {
                logger.error(".doGetJobByID(): Job not found - %s", strID);
                throw new NotFoundException("Job not found");
            }

            JobItem job = JobRestDBMapper.mapDbToRest(dbJob);

            JobType type = job.getType();
            String strResponse = null;

            switch(type)
            {
                case MIGRATE:
                {
                    strResponse = doGetMigrationJob(job);
                    break;
                }
                default:
                {
                    strResponse = RestObject.toJson(job);
                    break;
                }
            }


            return strResponse;
        }
        catch (DBManagerException | DBManagerNotFoundException ex)
        {
            throw new ServerErrorException("failed getJob() - " + ex.getMessage());
        }
    }

    /**
     * Get job by an ID
     * @return {@literal JobItem}
     * @throws NotFoundException if the job is not found
     * @throws ServerErrorException if something else went wrong
     * @throws DBManagerNotFoundException
     * @throws DBManagerException
     */
    private String doGetMigrationJob(JobItem job) throws ServerErrorException, DBManagerException, DBManagerNotFoundException
    {

        MigrationJobGetParams params = (MigrationJobGetParams) job.getParams();
        MigrationGetMapping[] mappings = params.getMapping();
        MigrationGetResource source = null;
        MigrationGetResource destination = null;
        String sourceId = "";
        String destinationId = "";

        for(int i =0; i < mappings.length; i++)
        {
            // Get the source out of the mappings and then get its Id so we can do
            // a database query on the id.
            source = mappings[i].getSource();
            sourceId = source.getId();

            if (source.getType() == DeviceResourceType.SHARE)
            {
                Share srcRes = RestDBMapper.mapDbToRest(m_deviceDbMgr.getSingleShare(sourceId));
                source.setPath(srcRes.getPath());
            }
            else if (source.getType() == DeviceResourceType.EXPORT)
            {
                Export srcRes = RestDBMapper.mapDbToRest(m_deviceDbMgr.getSingleExport(sourceId));
                source.setPath(srcRes.getPath());
            }
            else
            {
                throw new ServerErrorException("Unsupported Resource Type: " + source.getType().name());
            }

            // Get the destination out of the mappings and then get its Id so we can do
            // a database query on the id.
            destination = mappings[i].getDestination();
            destinationId = destination.getId();

            if (destination.getType() == DeviceResourceType.SHARE)
            {
                Share destRes = RestDBMapper.mapDbToRest(m_deviceDbMgr.getSingleShare(destinationId));
                destination.setPath(destRes.getPath());
            }
            else if (source.getType() == DeviceResourceType.EXPORT)
            {
                Export destRes = RestDBMapper.mapDbToRest(m_deviceDbMgr.getSingleExport(destinationId));
                destination.setPath(destRes.getPath());
            }
            else
            {
                throw new ServerErrorException("Unsupported Resource Type: " + destination.getType().name());
            }

            mappings[i].setSource(source);
            mappings[i].setDestination(destination);
        }

        params.setMapping(mappings);
        job.setParams(params);

        String strResponse = RestObject.toJson(job);
        return strResponse;

    }

    private String doGetJobsCount() throws ServerErrorException
    {
        logger.trace(".doGetJobsCount(): start count for Jobs");
        String strResponse = "";
        long totalCount = 0;
        try
        {
            totalCount = m_jobDbMgr.countJobs();
        }
        catch (DBManagerException ex)
        {
            logger.trace(".doGetJobsCount()", ex);
            throw new ServerErrorException("Could not get total collection count of Jobs");
        }

        strResponse = Long.toString(totalCount);
        return strResponse;
    }

    private String doGetJobsCount(String colName, String colParam) throws ServerErrorException
    {
        logger.trace(".getCountAgents(): start count for Agents");
        String strResponse = "";
        long totalCount = 0;
        try
        {
            totalCount = m_jobDbMgr.countJobs(colName, colParam);
        }
        catch (DBManagerException ex)
        {
            logger.trace(".getCountAgents()", ex);
            throw new ServerErrorException("Could not get total collection count of Agents");
        }

        strResponse = Long.toString(totalCount);
        return strResponse;
    }

    private String doGetMetrics(HttpServletRequest request) throws BadRequestException
    {
        logger.trace(".doGetMetrics(): Looking up metrics");

        String strID = requestHasId(request);
        if (strID == null)
        {
            logger.error(".doGetMetrics(): Id parameter missing");
            throw new BadRequestException("Id parameter missing");
        }

        MonitorWork monitorWork = m_metricsManager.getLatestPipelineMonitorWork(strID);

        if (monitorWork == null)
        {
            logger.error(".doGetMetrics(): MonitorWork is null");
            return JsonObjectTransformer.formatJSONErrorBody("Fail", "MonitorWork is null");
        }
        else
        {
            return JsonWorkSerializer.getJsonString(monitorWork);
        }
    }

    // ------------------------------
    //
    //	POST REQUEST SPECIFIC
    //
    // ------------------------------
    /**
     * Given a JSON object, add an Agent
     * @throws ServerErrorException
     */
    private String doPostJobAdd(JsonObject jsonBodyObject) throws BadRequestException, ServerErrorException
    {
        logger.trace(".doPostJobAdd(): Attempting to add a job");

        // NOTE: Will throw BadRequestException if it can't get the parameter.
        String stringType = JsonObjectTransformer.getStringParameter(jsonBodyObject,
                JobHttpServletRequestConstants.OBJ_TYPE);

        switch (stringType)
        {
            case JobHttpServletRequestConstants.JOB_TYPE_MIGRATE:
            {
                logger.trace(".doPostJobAdd(): Creating new migration job");
                return doPostJobAddMigrate(jsonBodyObject);
            }
            default:
            {
                logger.error(".doPostJobAdd(): Unknown job type specified");
                throw new BadRequestException("Malformed request, invalid \"" + JobHttpServletRequestConstants.OBJ_TYPE + "\" value of \"" + stringType + "\"");
            }
        }
    }

    /**
     * Given a JSON object, add an Agent.
     *
     * @param jsonBodyObject JSON object representing the Agent to Add
     * @return String containing the JSON object body for the Agent just added
     * @throws BadRequestException the bad request exception
     * @throws ServerErrorException the server error exception
     */
    private String doPostJobAddMigrate(JsonObject jsonBodyObject) throws BadRequestException,
            ServerErrorException
    {
        logger.trace(".doPostJobAddMigrate(): Adding a new migration job");

        // Class for deserialized body
        MigrationJobCreateRequest migrateJobRequest;

        try
        {
            logger.trace(".doPostJobAddMigrate(): Parsing Json request body");
            migrateJobRequest = MigrationJobCreateRequest.fromJson(jsonBodyObject,
                    MigrationJobCreateRequest.class);
        }
        catch (JsonSyntaxException ex)
        {
            logger.debug(".doPostJobAddMigrate(): Caught JsonSyntaxException - %s", ex.toString());
            logger.trace(".doPostJobAddMigrate(): Caught JsonSyntaxException", ex);
            throw new BadRequestException("Could not parse body");
        }
        catch (Exception ex)
        {
            logger.debug(".doPostJobAddMigrate(): Caught JsonSyntaxException - %s", ex.toString());
            logger.error(".doPostJobAddMigrate(): Caught JsonSyntaxException", ex);
            throw new ServerErrorException("Could not parse body");
        }

        logger.trace(".doPostJobAddMigrate(): parsed request now creating job %s", migrateJobRequest.getName());

        // Persist to our database
        JobItem job = new JobItem();

        // Set Job Info
        job.setType(JobType.MIGRATE);
        job.setName(migrateJobRequest.getName());
        job.setDescription(migrateJobRequest.getDescription());
        job.setParams(migrateJobRequest.getParams());
        job.setStatus(JobStatus.NOT_STARTED);

        // Set Times
        Date now = new Date();

        job.setCreateDate(now);
        job.setLastExecutedTime(null);
        job.setNextScheduledDate(now);

        try
        {
            logger.trace(".(): Saving job to database");
            String id = m_jobDbMgr.createJobReturnId(JobRestDBMapper.mapRestToDb(job, true));
            job.setId(id);
        }
        catch (Exception ex)
        {
            logger.error(".doPostJobAddMigrate(): Caught exception - %s", ex.toString());
            logger.trace(".doPostJobAddMigrate(): Caught exception", ex);
            throw new ServerErrorException("Failed to save job to database");
        }

        try
        {
            JobMapObject jmo = m_jobSechdulerConnector.registerJob(JobRestDBMapper.mapRestToDb(job, true),
                    SimpleTriggerType.SimpleTriggerNone,
                    LocalDateTime.ofInstant(now.toInstant(), ZoneId.of("Z")),
                    1,
                    1);
            m_jobMap.put(jmo.getId(), jmo.getJobKey());
        }
        catch (IllegalArgumentException | IllegalStateException | NullPointerException ex)
        {
            logger.error(".doPostJobAddMigrate(): failed registerJob() - %s", ex.getMessage());
            logger.trace(".doPostJobAddMigrate(): failed registerJob()", ex);
            throw new IllegalStateException("Unable to save job to scheduler");
        }

        // Create response
        JsonObject jobCreationResponse = new JsonObject();
        jobCreationResponse.addProperty(API_ID, job.getId());

        logger.trace(".doPostJobAddMigrate(): sending response for job: %s", job.getId());

        return jobCreationResponse.toString();
    }

    /**
     * Given a JSON object, update a job.
     *
     * @param jsonBodyObject the json body object
     * @param jobId the job id
     * @return response string
     * @throws BadRequestException the bad request exception
     * @throws ServerErrorException the server error exception
     */
    private String doPostJobUpdate(JsonObject jsonBodyObject,
                                   String jobId) throws BadRequestException, ServerErrorException
    {
        logger.trace(".doPostJobAdd(): Attempting to add a job");

        // NOTE: Will throw BadRequestException if it can't get the parameter.
        String stringType = JsonObjectTransformer.getStringParameter(jsonBodyObject,
                JobHttpServletRequestConstants.OBJ_TYPE);

        switch (stringType)
        {
            case JobHttpServletRequestConstants.JOB_TYPE_MIGRATE:
            {
                logger.trace(".doPostJobAdd(): Creating new migration job");
                return doPostJobUpdateMigrate(jsonBodyObject,jobId);
            }
            default:
            {
                logger.error(".doPostJobAdd(): Unknown job type specified");
                throw new BadRequestException("Malformed request, invalid \"" + JobHttpServletRequestConstants.OBJ_TYPE + "\" value of \"" + stringType + "\"");
            }
        }
    }

    private String doPostJobUpdateMigrate(JsonObject jsonBodyObject,
                                          String jobId) throws BadRequestException, ServerErrorException
    {
        logger.trace(".doPostJobUpdateMigrate(): updating a migration job");

        // Class for deserialized body -- aaron says he sends the whole object back on update so we can just convert and push into db
        MigrationJobGetRequest migrateJobGetRequest;

        try
        {
            logger.trace(".doPostJobUpdateMigrate(): parsing Json request body");
            migrateJobGetRequest = MigrationJobGetRequest.fromJson(jsonBodyObject,
                    MigrationJobGetRequest.class);
        }
        catch (JsonSyntaxException ex)
        {
            logger.debug(".doPostJobUpdateMigrate(): Caught JsonSyntaxException - %s", ex.toString());
            logger.trace(".doPostJobUpdateMigrate(): Caught JsonSyntaxException", ex);
            throw new BadRequestException("Could not parse body");
        }
        catch (Exception ex)
        {
            logger.debug(".doPostJobUpdateMigrate(): Caught JsonSyntaxException - %s", ex.toString());
            logger.error(".doPostJobUpdateMigrate(): Caught JsonSyntaxException", ex);
            throw new ServerErrorException("Could not parse body");
        }

        logger.trace(".doPostJobUpdateMigrate(): parsed request now updating job %s", migrateJobGetRequest.getName());

        try
        {
            // We need to get the current job
            // NOTE:: Only updating the params, name and description from the front end
            JobItem currentJob = JobRestDBMapper.mapDbToRest(m_jobDbMgr.getJob(jobId));

            // Start Creating Our Updated Job Object
            JobItem job = new JobItem();

            job.setType(JobType.MIGRATE);

            // Update only wha they sent in terms of this data
            job.setParams(migrateJobGetRequest.getParams());
            job.setName(migrateJobGetRequest.getName());
            job.setDescription(migrateJobGetRequest.getDescription());

            // Right Now our Scheduled Date DOES NOT Work so we will have to skip over that for now
            job.setNextScheduledDate(currentJob.getNextScheduledDate());


            // Now lets update all the fields that we need to from our current job saved in the db
            job.setId(currentJob.getId());
            job.setCreateDate(currentJob.getCreateDate());
            job.setLastExecutedTime(currentJob.getLastExecutedTime());
            job.setStatus(currentJob.getStatus());

            logger.trace(".doPostJobUpdateMigrate(): updating job in database");

            // Now that our Job is all set lets update it
            m_jobDbMgr.updateJob(JobRestDBMapper.mapRestToDb(job, false));

            // Create response
            JsonObject jobCreationResponse = new JsonObject();

            logger.trace(".doPostJobUpdateMigrate(): sending response for job: %s",
                    job.getId());

            return jobCreationResponse.toString();
        }
        catch (DBManagerException ex)
        {
            logger.error(".doPostJobUpdateMigrate(): Caught exception - %s", ex.toString());
            logger.trace(".doPostJobUpdateMigrate(): Caught exception", ex);
            throw new ServerErrorException("Failed to save job to database");
        }
        catch (Exception ex)
        {
            logger.error(".doPostJobUpdateMigrate(): Caught exception - %s", ex.toString());
            logger.trace(".doPostJobUpdateMigrate(): Caught exception", ex);
            throw new ServerErrorException("Failed to save job to database");
        }
    }

    private EndpointResponse doJobsLogsEndpoint(HttpServletRequest request, HttpServletResponse response)
    {
        try
        {
            logger.trace(".doJobsLogsEndpoint(): on " + JOBS_LOGS);
            return doPostLogs(request, response);
        }
        catch (BadRequestException ex)
        {
            logger.error(".doJobsLogsEndpoint(): Bad Request", ex);
            return new EndpointResponse(JsonObjectTransformer.formatJSONErrorBody("Fail", "Bad Request " + ex.getMessage()),
                    ex.getHTTPCode());
        }
    }

    private EndpointResponse doPostLogs(HttpServletRequest request,
                                        HttpServletResponse response) throws BadRequestException
    {
        logger.trace(".doGetLogs(): looking up log file");

        Map<String, String> paramMap = super.readRequestParameters(request);

        // Get the query param for type of log to send
        if (!paramMap.containsKey(TYPE_PARAM))
        {
            logger.error(".doGetLogs(): missing log type query parameter");
            throw new BadRequestException("Log type is not specified as the 'type' query parameter");
        }

        /**
         *  Get the json object that tells us which mapping to get the log for
         *  this will throw if there's an issue with the body json
         */
        MigrationMappingMeta mappingMeta = getMappingMetaObject(request);

        // Get the type param and set our lookup boolean accordingly
        String strType = paramMap.get(TYPE_PARAM);
        boolean blnResults;

        switch (strType)
        {
            case(RESULT_TYPE):
            {
                logger.trace(".doGetLogs(): retrieving result log for job:%s, source:%s, destination:%s",
                        mappingMeta.getJobId(),
                        mappingMeta.getSourceId(),
                        mappingMeta.getDestinationId());
                blnResults = true;
                break;
            }
            case(FAILURE_TYPE):
            {
                logger.trace(".doGetLogs(): retrieving failure log for job:%s, source:%s, destination:%s",
                        mappingMeta.getJobId(),
                        mappingMeta.getSourceId(),
                        mappingMeta.getDestinationId());
                blnResults = false;
                break;
            }
            default:
            {
                logger.error(".doGetLogs(): invalid log type:%s specified",
                        strType);
                throw new BadRequestException("Invalid log type:'" + strType + "' specified");
            }
        }

        String finalPath = getLogPath(mappingMeta, blnResults);
        logger.trace(".doGetLogs(): attempting to stream file:%s", finalPath);

        // TODO: see if file exists
        RestFileStreamer streamer = new RestFileStreamer(finalPath, response);
        return streamer.streamFile();
    }// end doPostLogs

    private int doPostProcessException(HttpServletRequest request) throws BadRequestException
    {
        // Get the mapping object from the request body
        MigrationMappingMeta mappingMetadata = getMappingMetaObject(request);
        logger.trace(".doGetProcessException(): running the exception list for mapping in job:%s with source:%s and destination:%s",
                mappingMetadata.getJobId(),
                mappingMetadata.getSourceId(),
                mappingMetadata.getDestinationId());

        // Get the mapping's index in relation to the job (ensure the mapping exists in the job)
        int iMappingIndex = getMappingIndex(mappingMetadata);

        // Update the mapping with the flag to run the exception list
        try
        {
            m_jobDbMgr.setMappingExceptionState(mappingMetadata.getJobId(),
                    iMappingIndex);
        }
        catch (DBManagerException ex)
        {
            logger.error(".doGetProcessException(): unable to update the exception state for mapping in job:%s with source:%s and destination:%s - Exception:%s",
                    mappingMetadata.getJobId(),
                    mappingMetadata.getSourceId(),
                    mappingMetadata.getDestinationId(),
                    ex.getMessage());
            throw new BadRequestException("Unable to update mapping's exception state - Exception:" + ex.getMessage());
        }

        // call into the migration job to run the exception meow
        try
        {
            String strJobId = mappingMetadata.getJobId();

            JobKey jobKey = m_jobMap.get(strJobId);
            if (jobKey == null)
            {
                logger.trace(".runMappingExceptionList(): failed to find job in map with id " + strJobId);
                throw new BadRequestException("Could not find job with id " + strJobId);
            }

            // See if job is currently running
            SchedulerManagerJob job = m_schedulerManager.getRunningJob(jobKey);

            // Job is not current running, we're done
            if (job == null)
            {
                logger.error(".runMappingExceptionList(): job:%s is not running -- will start when job is run",
                        strJobId);
                return HttpServletResponse.SC_OK;
            }

            runMappingExceptionList(job, jobKey, iMappingIndex);
            return HttpServletResponse.SC_CREATED;
        }
        catch (JobExecutionException e)
        {
            logger.error(".doGetProcessException(): could not run the mapping for an exception list - Exception:%s",
                    e.getMessage());
            throw new BadRequestException("Could not run the exception list", e);
        }
    }

    private int getMappingIndex(MigrationMappingMeta mappingMetadata) throws BadRequestException
    {
        // Get the job
        DbJob job = null;
        try
        {
            job = m_jobDbMgr.getJob(mappingMetadata.getJobId());
        }
        catch (DBManagerException ex)
        {
            logger.error(".doPostProcessException(): unable to retrieve job:%s from the db", mappingMetadata.getJobId());
            throw new BadRequestException("Unable to retrieve job:" + mappingMetadata.getJobId() + " from the db");
        }

        // Get the index of the mapping from the job
        DbMigrationJobParameters migrateParams = (DbMigrationJobParameters) job.getJobParams();
        DBMigrationJobMapping[] dbMappings = migrateParams.getMapping();

        int iMappingIndex = -1;
        for (int i=0; i<dbMappings.length; i++)
        {
            DBMigrationJobMapping dbMapping = dbMappings[i];

            if ((dbMapping.getSourceId().contentEquals(mappingMetadata.getSourceId())) &&
                    (dbMapping.getDestinationId().contentEquals(mappingMetadata.getDestinationId())))
            {
                logger.trace(".doPostProcessException(): found the mapping for job:%s, source:%s, destination:%s at index:%d",
                        mappingMetadata.getJobId(),
                        mappingMetadata.getSourceId(),
                        mappingMetadata.getDestinationId(),
                        i);

                iMappingIndex = i;
                break;
            }
        }

        // Sanity check
        if (iMappingIndex == -1)
        {
            logger.error(".doPostProcessException(): unable to find mapping with sourceId:%s and destinationId:%s in job:%s",
                    mappingMetadata.getSourceId(),
                    mappingMetadata.getDestinationId(),
                    mappingMetadata.getJobId());
            throw new BadRequestException("Unable to find the specified mapping within the associated job");
        }

        return iMappingIndex;
    }

    private void runMappingExceptionList(SchedulerManagerJob job,
                                         JobKey jobKey,
                                         int iMappingIndex) throws BadRequestException,
            JobExecutionException
    {
        logger.trace(".runMappingExceptionList(): set exception flag, job is currently running, signal it");

        // Job is running, tell it to look for cutovers.
        MigrationJob migrationJob;
        try
        {
            migrationJob = (MigrationJob) job;
        }
        catch (ClassCastException ex)
        {
            logger.error(".runMappingExceptionList: invalid job type " + job.getClass().getName() + " casted for job with key:%s" + jobKey.toString());
            throw new BadRequestException("Job with key " + jobKey.toString()
                    + " found clazz type " + job.getClass().getName()
                    + " not clazz type " + MigrationJob.class.getName());
        }

        // Run the exception list for this mapping
        migrationJob.runExceptionList(iMappingIndex);
    }

    /**
     * Return the path to an audit file (either failure or result file)
     * @param meta The mapping metadata object that specified which log file to point to
     * @param blnResults boolean to specify result log (when true) or failure log (when false)
     *
     * @return the path to the specified file
     */
    private String getLogPath(MigrationMappingMeta meta,
                              boolean blnResults)
    {
        String strPath = null;

        if (blnResults)
        {
            logger.trace(".getLogPath(): generating result log path");
            strPath = RESULT_DIR + RESULT_AUDIT_PREFIX;
        }
        else
        {
            logger.trace(".getLogPath(): generating failure log path");
            strPath = FAILURE_DIR + FAILURE_AUDIT_PREFIX;
        }

        strPath += meta.getJobId();
        strPath += "__";
        strPath += meta.getSourceId();
        strPath += "_";
        strPath += meta.getDestinationId();
        strPath += CSV_SUFFIX;

        logger.trace(".getLogPath(): generated log path:%s for job:%s, sourceId:%s, and destinationId:%s",
                strPath,
                meta.getJobId(),
                meta.getSourceId(),
                meta.getDestinationId());
        return strPath;
    }

    private MigrationMappingMeta getMappingMetaObject(HttpServletRequest request) throws BadRequestException
    {
        String strBodyJson = null;
        MigrationMappingMeta mappingMetadata = null;

        // Get the body of the request
        try
        {
            strBodyJson = super.getBodyString(request);
            mappingMetadata = RestObject.fromJson(strBodyJson,
                    MigrationMappingMeta.class);

            logger.trace(".getMappingMetaObject(): successfully read request body mapping meta jobId:%s, sourceId:%s, and destinationId:%s",
                    mappingMetadata.getJobId(),
                    mappingMetadata.getSourceId(),
                    mappingMetadata.getDestinationId());
            return mappingMetadata;
        }
        catch (InvalidParameterException | InvalidMediaTypeException | IOException | JsonSyntaxException e)
        {
            logger.error(".getMappingMetaObject(): unable to parse request body -- Exception:%s",
                    e.getMessage());
            throw new BadRequestException("Unable to parse request body -- Reason:" + e.getMessage());
        }
    }

    /**
     * Set multiple mapping to one phase
     *
     * @param jobId the job id
     * @param request the request
     * @param phase the phase
     * @throws BadRequestException the bad request exception
     * @throws ServerErrorException the server error exception
     * TODO: Super fucking hackey as it takes a MigrationJobUpdateMappingRequest
     */
    private void doPostJobUpdateMigrateMappingsPhases(String jobId,
                                                      MigrationJobUpdateMappingRequest request,
                                                      DbMigrationPhase phase) throws BadRequestException,
            ServerErrorException
    {
        logger.trace(".doPostJobUpdateMigrateMappingSetPhases(): updating a migration job");

        try
        {
            logger.trace(".doPostJobUpdateMigrateMappingSetPhases(): find indices in job in database");
            List<Integer> indexList = new LinkedList<Integer>();

            MigrationJobUpdateMapping restMappings[] = request.getMapping();

            // We need to get the current job
            DbJob currentJob = m_jobDbMgr.getJob(jobId);
            DbMigrationJobParameters migrateParams = (DbMigrationJobParameters) currentJob.getJobParams();
            DBMigrationJobMapping[] dbMappings = migrateParams.getMapping();

            // We need to find the real index numbers of our current mappings in order to sent it to the job db manager.
            for (int i = 0; i < restMappings.length; i++)
            {
                MigrationJobUpdateMapping restMapping = restMappings[i];
                for (int iUse = 0; iUse < dbMappings.length; iUse++)
                {
                    DBMigrationJobMapping dbMapping = dbMappings[iUse];

                    if ((dbMapping.getSourceId().contentEquals(restMapping.getSource().getId())) &&
                            (dbMapping.getDestinationId().contentEquals(restMapping.getDestination().getId())))
                    {
                        indexList.add(new Integer(iUse));
                        break;
                    }
                }
            }

            logger.trace(".doPostJobUpdateMigrateMappingSetPhases(): updating job in database");

            m_jobDbMgr.setMultipleMappingPhases(jobId,
                    indexList,
                    phase);
        }
        catch (DBManagerException ex)
        {
            logger.error(".doPostJobUpdateMigrateMappingSetPhases(): caught exception - %s", ex.toString());
            logger.trace(".doPostJobUpdateMigrateMappingSetPhases(): caught exception", ex);
            throw new ServerErrorException("Failed to save job to database");
        }
        catch (Exception ex)
        {
            logger.error(".doPostJobUpdateMigrateMappingSetPhases(): caught exception - %s", ex.toString());
            logger.trace(".doPostJobUpdateMigrateMappingSetPhases(): caught exception", ex);
            throw new ServerErrorException("Failed to save job to database");
        }
    }

    // ------------------------------
    //
    //	DELETE REQUEST SPECIFIC
    //
    // ------------------------------
    /**
     * Delete job by an ID
     * @param strID
     * @throws NotFoundException if the job is not found
     * @throws ServerErrorException if something else went wrong
     */
    private void doDeleteJobByID(String strID) throws NotFoundException, ServerErrorException
    {
        logger.trace(".doDeleteJobByID(): processing for id %s", strID);

        try
        {
            // Get the job
            DbJob job = m_jobDbMgr.getJob(strID);

            // iterate and delete all mapping reports
            deleteAllMappingReports(job);

            m_jobDbMgr.deleteJob(strID);
        }
        catch (DBManagerNotFoundException ex)
        {
            throw new NotFoundException("could not find job with id " + strID);
        }
        catch (DBManagerException ex)
        {
            throw new ServerErrorException("failed to delete job " + strID + " - " + ex.getMessage());
        }
    }

    private void deleteAllMappingReports(DbJob job)
    {
        DBMigrationJobMapping[] mappings = ((DbMigrationJobParameters)job.getJobParams()).getMapping();

        for (int i=0; i<mappings.length; i++)
        {
            DBMigrationJobMapping mapping = mappings[i];

            // Create the meta object so we can look up the file path
            MigrationMappingMeta mappingMetadata = new MigrationMappingMeta(job.get_id().get$oid(),
                    mapping.getSourceId(),
                    mapping.getDestinationId());

            String strResultPath = getLogPath(mappingMetadata, true);
            Path resultPath = Paths.get(strResultPath);
            try
            {
                // Delete if it exists
                Files.deleteIfExists(resultPath);

                logger.trace(".deleteAllMappingReports(): deleted result log for mapping with source:%s and dest:%s",
                        mapping.getSourceId(),
                        mapping.getDestinationId());
            }
            catch (IOException e)
            {
                logger.error(".deleteAllMappingReports(): unable to delete result log for mapping with source:%s and dest:%s",
                        mapping.getSourceId(),
                        mapping.getDestinationId());
            }

            String strFailPath = getLogPath(mappingMetadata, false);
            Path failPath = Paths.get(strFailPath);
            try
            {
                // Delete if it exists
                Files.deleteIfExists(failPath);

                logger.trace(".deleteAllMappingReports(): deleted failure log for mapping with source:%s and dest:%s",
                        mapping.getSourceId(),
                        mapping.getDestinationId());
            }
            catch (IOException e)
            {
                logger.error(".deleteAllMappingReports(): unable to delete failure log for mapping with source:%s and dest:%s",
                        mapping.getSourceId(),
                        mapping.getDestinationId());
            }
        }
    }

    /**
     * Delete the Report for the job with the specified id
     *
     * @param jobId
     * @throws ServerErrorException
     */
    private void doDeleteJobPhaseReport(String jobId) throws ServerErrorException, NotFoundException
    {
        try
        {
            m_jobDbMgr.deleteAllTaskPhaseReport(jobId);
        }
        catch (DBManagerNotFoundException ex)
        {
            //logger.debug(".doDeleteJobData() failed to find job with id %s - %s", jobId, ex.getMessage());
            logger.trace(".doDeleteJobData()", ex);
            throw new ServerErrorException("Failed to find the report data for job id " + jobId);
        }
        catch (DBManagerException ex)
        {
            //logger.error(".doDeleteJobData() failed deleteLifetimeJobReport() for job id %s - %s", jobId, ex.getMessage());
            logger.trace(".doDeleteJobData()", ex);
            throw new ServerErrorException("Failed to delete the report data for job id " + jobId);
        }
    }
}
