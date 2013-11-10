package com.redsamurai.model;


import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oracle.adf.share.logging.ADFLogger;

import oracle.jbo.AttributeList;
import oracle.jbo.Key;
import oracle.jbo.Row;
import oracle.jbo.ViewCriteria;
import oracle.jbo.server.EntityDefImpl;
import oracle.jbo.server.EntityImpl;
import oracle.jbo.server.QueryCollection;
import oracle.jbo.server.SQLBuilder;
import oracle.jbo.server.ViewDefImpl;
import oracle.jbo.server.ViewObjectImpl;
import oracle.jbo.server.ViewRowImpl;
import oracle.jbo.server.ViewRowSetImpl;

import org.w3c.dom.Element;


public class RegionsViewImpl extends ViewObjectImpl {

    public static final int TOLLERATED_EXEC_TIME = 10000; //10 seconds

    protected boolean queryLogging = true;

    //used for stamping the vo execution time
    private long initTime;

    private static final ADFLogger logger = ADFLogger.createADFLogger(RegionsViewImpl.class);

    /**
     * @param name
     * @param voDef
     */
    public RegionsViewImpl(String name, ViewDefImpl voDef) {
        super(name, voDef);
    }

    public RegionsViewImpl() {
        super();
    }

    /**
     * Bulk delete of multiple rows as per the given list of Keys
     * @param objectKeysToBeRemoved
     */
    public void bulkDelete(List objectKeysToBeRemoved) {
        if (objectKeysToBeRemoved != null) {
            for (Object object : objectKeysToBeRemoved) {
                Key objectKey = (Key)object;
                Row[] rowsFound = this.findByKey(objectKey, 1);
                if (rowsFound != null && rowsFound.length == 1) {
                    rowsFound[0].remove();
                }
            }
        }
    }

    /**
     * @param accessType
     * @return true if corresponding access type is allowed
     */
    public boolean isAccessAllowed(String accessType) {
        /*
        return !isUseBooster() ||
            ConveroModelUtils.findBoosterAM(getApplicationModule()).isAccessAllowed(CommonUtils.trimValueAfterSuffix(getName(),
                                                                                                                     "VO"),
                                                                                    accessType);
        */
        return true;
    }

    public boolean isUseBooster() {
        /*
        if (!initUseBooster) {
            useBooster =
                    ConveroModelUtils.findBoosterAM(getApplicationModule()).isUseBooster(CommonUtils.trimValueAfterSuffix(getName(),
                                                                                                                          "VO"));
            initUseBooster = true;
        }
        return useBooster;
        */
        return false;
    }

    /**
     * @return String[] arrays of entity name
     */
    public String[] getEntityNames() {
        EntityDefImpl[] defs = getEntityDefs();
        String[] names = new String[defs.length];
        for (int i = 0; i < defs.length; i++) {
            names[i] = defs[i].getName();
        }
        return names;
    }

  

    /**
     * @param viewRowSet
     * @param attrValList
     * @return
     */
    public ViewRowImpl createInstance(ViewRowSetImpl viewRowSet, AttributeList attrValList) {
        return super.createInstance(viewRowSet, attrValList);
    }

    /**
     * Override to execute Pre-Query and Pre-Record (meta-data) actions.
     * @param qc
     * @param resultSet
     * @return
     */
    public ViewRowImpl createInstanceFromResultSet(QueryCollection qc, ResultSet resultSet) {
        ViewRowImpl viewRow = super.createInstanceFromResultSet(qc, resultSet);
        /*
        if (isUseBooster()) {
            ((ConveroViewRowImpl)viewRow).executeQueryActionOnEntities();
            ((ConveroViewRowImpl)viewRow).executePreRecordActionOnEntities();
        }
        */
        return viewRow;
    }

    /**
     * @param eoIndices
     * @param entities
     * @param viewRowSet
     * @param attrValList
     * @return
     */
    protected ViewRowImpl createInstanceWithEntities(int[] eoIndices, EntityImpl[] entities, ViewRowSetImpl viewRowSet,
                                                     AttributeList attrValList) {
        return super.createInstanceWithEntities(eoIndices, entities, viewRowSet, attrValList);

    }

    /**
     * Override to control access to query as per meta-data.
     * Additonally is tracking/LOGGING sql server long running queries
     * @param qc
     * @param params
     * @param noUserParams
     */
    protected void executeQueryForCollection(Object qc, Object[] params, int noUserParams) {
        long init = System.currentTimeMillis();
        /*
        if (isAccessAllowed(BoosterConstants.ACCESS_SELECT)) {
            super.executeQueryForCollection(qc, params, noUserParams);
        }
        */
        super.executeQueryForCollection(qc, params, noUserParams); 
        long end = System.currentTimeMillis() - init;
        //more than 10 seconds
        if (end > TOLLERATED_EXEC_TIME) {
            logger.severe("SLOW_QUERY NOTIFICATION:" + this.getName() + ":" + end + " miliseconds, " + this.getRowCountInRange() + " records.");
            logger.severe("SLOW_QUERY STATEMENT:" + this.getQuery());
            if (params != null && getBindingStyle() == SQLBuilder.BINDING_STYLE_ORACLE_NAME) {
                Map<String, Object> bindsMap = new HashMap<String, Object>(params.length);
                for (Object param : params) {
                    Object[] nameValue = (Object[])param;
                    String name = (String)nameValue[0];
                    Object value = nameValue[1];
                    bindsMap.put(name, value);
                }
                logger.severe("SLOW_QUERY BIND VARS : {0}", bindsMap);
            }
        }
    }

    protected void executeQueryForCollectionByPassAccessChecking(Object qc, Object[] params, int noUserParams) {
        super.executeQueryForCollection(qc, params, noUserParams);
    }

    /**
     * method override, to allow logging
     * @param qc
     * @param params
     * @param stmt
     * @throws SQLException
     */
    @Override
    protected void bindParametersForCollection(QueryCollection qc, Object[] params,
                                               PreparedStatement stmt) throws SQLException {

        if (qc != null && queryLogging) {
            initTime = System.currentTimeMillis();
        }
        if (queryLogging) {
            logQueryStatementAndBindParameters(qc, params);
        }

        super.bindParametersForCollection(qc, params, stmt);

        if (queryLogging) {
            logger.finer("----[Elapsed time for VO=" + getName() + "= " + (System.currentTimeMillis() - initTime) +
                         " miliseconds]----");
        }
    }

    /**
     * method used to introspect the query produced at runtime by the vo.
     * @param qc
     * @param params
     */
    private void logQueryStatementAndBindParameters(QueryCollection qc, Object[] params) {
        String vrsiName = null;
        if (qc != null) {
            ViewRowSetImpl vrsi = qc.getRowSetImpl();
            vrsiName = vrsi.isDefaultRS() ? "<Default>" : vrsi.getName();
        }
        String voName = getName();
        String voDefName = getDefFullName();
        if (qc != null) {
            logger.finest("---- " + new java.util.Date() + " [Exec query for VO=" + voName + ", RS=" + vrsiName +
                          "]----");
        } else {
            logger.finest("---- " + new java.util.Date() + " [Exec COUNT query for VO=" + voName + "]----");
        }
        logger.fine("VO Definition Name = {0}", voDefName);

        String dbVCs = appliedCriteriaString(ViewCriteria.CRITERIA_MODE_QUERY);
        if (dbVCs != null && !dbVCs.isEmpty()) {
            logger.finest("Applied Database VCs = {0} ", dbVCs);
        }

        String memVCs = appliedCriteriaString(ViewCriteria.CRITERIA_MODE_CACHE);
        if (memVCs != null && !memVCs.isEmpty()) {
            logger.finest("Applied In-Memory VCs = {0} ", memVCs);
        }

        String bothVCs = appliedCriteriaString(ViewCriteria.CRITERIA_MODE_QUERY | ViewCriteria.CRITERIA_MODE_CACHE);
        if (bothVCs != null && !bothVCs.isEmpty()) {
            logger.finest("Applied 'Both' VCs = {0}", bothVCs);
        }

        logger.finest("Generated query : {0}", getQuery());

        if (params != null) {
            if (getBindingStyle() == SQLBuilder.BINDING_STYLE_ORACLE_NAME) {
                Map<String, Object> bindsMap = new HashMap<String, Object>(params.length);
                for (Object param : params) {
                    Object[] nameValue = (Object[])param;
                    String name = (String)nameValue[0];
                    Object value = nameValue[1];
                    bindsMap.put(name, value);
                }
                logger.finest("Bind Variables : {0}", bindsMap);
            }
        }
    }

    @Override
    protected ViewRowImpl createRowFromResultSet(Object object, ResultSet resultSet) {
        ViewRowImpl ret = super.createRowFromResultSet(object, resultSet);
        if (ret != null) {
            if (queryLogging)
                logger.finest("----[VO " + getName() + " fetched " + ret.getKey() + "]");
        }
        return ret;
    }


    private String appliedCriteriaString(int mode) {
        ViewCriteria[] appliedCriterias = getApplyViewCriterias(mode);
        String result = "";
        if (appliedCriterias != null && appliedCriterias.length > 0) {
            List<String> list = new ArrayList<String>(appliedCriterias.length);
            for (ViewCriteria vc : appliedCriterias) {
                list.add(vc.getName());
            }
            result = list.toString();
        }
        return result;
    }
    
    

    /**
     *tracking slow activation events
     * @param element
     * @param b
     */
    public void activateIteratorState(Element element, boolean b) {
        logger.severe("prepare for activation ----[VO " + getName() + "]");
        long init = System.currentTimeMillis();
        super.activateIteratorState(element, b);
        long end = System.currentTimeMillis() - init;
        //more than 10 seconds
        if (end > TOLLERATED_EXEC_TIME) {
            logger.severe("SLOW_QUERY ACTIVATION NOTIFICATION:" + this.getName() + ":" + end + " miliseconds, " + this.getRowCountInRange() + " records.");
        }
    }

    /**
     *tracking slow COUNT statements
     * @param element
     * @param b
     */
    public long getEstimatedRowCount() {
        long init = System.currentTimeMillis();
        long counter= super.getEstimatedRowCount();
        long end = System.currentTimeMillis() - init;
        //more than 10 seconds
        if (end > TOLLERATED_EXEC_TIME) {
            logger.severe("SLOW_QUERY COUNT NOTIFICATION:" + this.getName() + ":" + end + " miliseconds");
        }
        return counter;

    }
  
}

