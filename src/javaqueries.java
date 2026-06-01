import java.sql.*;

public class oldMain {

    public static void runQuery(String title, String sql) {

        System.out.println("\n=================================");
        System.out.println(title);
        System.out.println("=================================");

        try (
                Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)
        ) {

            ResultSetMetaData meta = rs.getMetaData();
            int columns = meta.getColumnCount();

            while (rs.next()) {

                for (int i = 1; i <= columns; i++) {

                    System.out.print(
                            meta.getColumnName(i)
                                    + ": "
                                    + rs.getString(i)
                                    + "    "
                    );
                }

                System.out.println();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        runQuery(
                "Query 1",
                """
                SELECT HS.system_id, HS.system_name, AVG(RM.metric_value) AS avg_latency
FROM Hardware_System HS, Hardware_Configuration HC, Experiment_Run ER, Benchmark_Result BR, Result_Metric RM
WHERE HS.system_id = HC.system_id
  AND HC.config_id = ER.config_id
  AND HC.system_id = ER.system_id
  AND ER.run_id = BR.run_id
  AND BR.result_id = RM.result_id
  AND RM.metric_name = 'Latency'
GROUP BY HS.system_id, HS.system_name;
                """
        );

        runQuery(
                "Query 2",
                """
                SELECT ER.run_id, ER.run_name, RM.metric_value AS latency, RM.unit
FROM Experiment_Run ER, Benchmark_Result BR, Result_Metric RM
WHERE ER.run_id = BR.run_id
  AND BR.result_id = RM.result_id
  AND RM.metric_name = 'Latency'
  AND RM.metric_value < (SELECT AVG(RM2.metric_value)
                                           FROM Result_Metric RM2
                                           WHERE RM2.metric_name = 'Latency');
                """
        );

        runQuery(
                "Query 3",
                """
                SELECT ER.run_id, ER.run_name
FROM Experiment_Run ER
WHERE NOT EXISTS (SELECT OM.optimization_id
                                      FROM Optimization_Method OM
                                      WHERE NOT EXISTS (SELECT ERO.optimization_id
                                           FROM Experiment_Run_Optimization ERO
                                           WHERE ERO.run_id = ER.run_id
                                             AND ERO.optimization_id = OM.optimization_id))
                """
        );

        runQuery(
                "Query 4",
                """
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
                """
        );

        runQuery(
                "Query 5",
                """
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
)
                """
        );

        runQuery(
                "Query 6",
                """
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
)
                """
        );

        runQuery(
                "Query 7",
                """
                SELECT DISTINCT U.user_id, U.first_name, U.last_name
FROM User U, Works_On WO, Project P, Experiment_Run ER, AI_Model AIM
WHERE U.user_id = WO.user_id
  AND P.project_id = ER.project_id
  AND ER.model_id = AIM.model_id
  AND WO.project_id = P.project_id
  AND AIM.model_family = 'Transformer'
                """
        );
    }
}