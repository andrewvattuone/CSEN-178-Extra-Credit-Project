-- queries.sql
--
-- Example report queries for the AI Hardware Benchmark Tracker dataset.
-- These statements are useful for verifying the database contents and exercising the query APIs.

USE DBAIHardwareBenchmark;

-- SELECT * FROM User;
-- SELECT * FROM Experiment_Run;
-- SELECT * FROM Hardware_System;
-- SELECT * FROM Result_Metric;

-- Query 1: Find average experimental latency for each hardware system (display both system name and its id)
SELECT HS.system_id, HS.system_name, AVG(RM.metric_value) AS avg_latency
FROM Hardware_System HS, Hardware_Configuration HC, Experiment_Run ER, Benchmark_Result BR, Result_Metric RM
WHERE HS.system_id = HC.system_id
  AND HC.config_id = ER.config_id
  AND HC.system_id = ER.system_id
  AND ER.run_id = BR.run_id
  AND BR.result_id = RM.result_id
  AND RM.metric_name = 'Latency'
GROUP BY HS.system_id, HS.system_name;

-- Query 2: Find experiment runs whose latency is lower than the average latency
SELECT ER.run_id, ER.run_name, RM.metric_value AS latency, RM.unit
FROM Experiment_Run ER, Benchmark_Result BR, Result_Metric RM
WHERE ER.run_id = BR.run_id
  AND BR.result_id = RM.result_id
  AND RM.metric_name = 'Latency'
  AND RM.metric_value < (SELECT AVG(RM2.metric_value)
                                           FROM Result_Metric RM2
                                           WHERE RM2.metric_name = 'Latency');

-- Query 3: Find experiment runs that use all optimization methods
SELECT ER.run_id, ER.run_name
FROM Experiment_Run ER
WHERE NOT EXISTS (SELECT OM.optimization_id
                                      FROM Optimization_Method OM
                                      WHERE NOT EXISTS (SELECT ERO.optimization_id
                                           FROM Experiment_Run_Optimization ERO
                                           WHERE ERO.run_id = ER.run_id
                                             AND ERO.optimization_id = OM.optimization_id));

-- Query 4: Find the 3 hardware systems with the lowest average power consumption
SELECT HS.system_id, HS.system_name, AVG(RM.metric_value) AS avg_power_consumption
FROM Hardware_System HS, Hardware_Configuration HC, Experiment_Run ER, Benchmark_Result BR, Result_Metric RM
WHERE HS.system_id = HC.system_id
  AND HC.config_id = ER.config_id
  AND HC.system_id = ER.system_id
  AND ER.run_id = BR.run_id
  AND BR.result_id = RM.result_id
  AND RM.metric_name = 'Power Consumption'
GROUP BY HS.system_id, HS.system_name
ORDER BY avg_power_consumption ASC
LIMIT 3;

-- Query 5: Find the hardware system that has had the most experiments run on it
SELECT HS.system_id, HS.system_name, COUNT(ER.run_id) AS num_experiments
FROM Hardware_System HS, Hardware_Configuration HC, Experiment_Run ER
WHERE HS.system_id = HC.system_id
  AND HC.config_id = ER.config_id
  AND HC.system_id = ER.system_id
GROUP BY HS.system_id, HS.system_name
HAVING COUNT(ER.run_id) = (
	SELECT MAX(run_count)
	FROM (
		SELECT COUNT(ER2.run_id) AS run_count
		FROM Hardware_System HS2, Hardware_Configuration HC2, Experiment_Run ER2
		WHERE HS2.system_id = HC2.system_id
		  AND HC2.config_id = ER2.config_id
		  AND HC2.system_id = ER2.system_id
		GROUP BY HS2.system_id
	) AS Counts
);

-- Query 6: Find the hardware system, hardware configuration, and software environment experiment setup that has the lowest latency
SELECT HS.system_id, 
       HS.system_name, 
       HC.config_id, 
       HC.config_name, 
       SE.environment_id, 
       SE.os_name, 
       SE.framework, 
       AVG(RM.metric_value) AS avg_latency
FROM Hardware_System HS, 
     Hardware_Configuration HC, 
     Experiment_Run ER, 
     Software_Environment SE, 
     Benchmark_Result BR, 
     Result_Metric RM
WHERE HS.system_id = HC.system_id
  AND HC.config_id = ER.config_id
  AND HS.system_id = ER.system_id
  AND ER.environment_id = SE.environment_id
  AND ER.run_id = BR.run_id
  AND BR.result_id = RM.result_id
  AND RM.metric_name = 'Latency'
GROUP BY HS.system_id, 
         HS.system_name, 
         HC.config_id, 
         HC.config_name, 
         SE.environment_id, 
         SE.os_name, 
         SE.framework
HAVING AVG(RM.metric_value) = (
    SELECT MIN(avg_latency)
    FROM (
        SELECT AVG(RM2.metric_value) AS avg_latency
        FROM Hardware_System HS2, 
             Hardware_Configuration HC2, 
             Experiment_Run ER2, 
             Benchmark_Result BR2, 
             Result_Metric RM2,
             Software_Environment SE2
        WHERE HS2.system_id = HC2.system_id
          AND HC2.config_id = ER2.config_id
          AND HC2.system_id = ER2.system_id
          AND ER2.environment_id = SE2.environment_id
          AND ER2.run_id = BR2.run_id
          AND BR2.result_id = RM2.result_id
          AND RM2.metric_name = 'Latency'
        GROUP BY HS2.system_id, 
                 HC2.config_id, 
                 SE2.environment_id
    ) AS SetupAverages
);

-- Query 7: Find all users who have tested transformer models
SELECT DISTINCT U.user_id, U.first_name, U.last_name
FROM User U, Works_On WO, Project P, Experiment_Run ER, AI_Model AIM
WHERE U.user_id = WO.user_id
  AND P.project_id = ER.project_id
  AND ER.model_id = AIM.model_id
  AND WO.project_id = P.project_id
  AND AIM.model_family = 'Transformer';


