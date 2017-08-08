package pl.edu.agh.cep.example.cassandra;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CassandraUtils {
    private static final Logger LOG = LoggerFactory.getLogger(CassandraUtils.class);


    public static Session initializeSession() {
        CassandraConnector connector = new CassandraConnector();
        connector.connect("172.17.0.2", null);
        return connector.getSession();
    }

    public static void createKeyspace(Session session, String keyspaceName, String replicatioonStrategy, int numberOfReplicas) {
        StringBuilder sb = new StringBuilder("CREATE KEYSPACE IF NOT EXISTS ").append(keyspaceName).append(" WITH replication = {").append("'class':'").append(replicatioonStrategy).append("','replication_factor':").append(numberOfReplicas).append("};");

        final String query = sb.toString();

        LOG.info("Executing query:" + query);
        session.execute(query);
    }

    public static void useKeyspace(Session session, String keyspace) {
        final String query = "USE " + keyspace;
        LOG.info("Executing query:" + query);
        session.execute(query);
    }

    public static void deleteKeyspace(Session session, String keyspaceName) {
        StringBuilder sb = new StringBuilder("DROP KEYSPACE ").append(keyspaceName);

        final String query = sb.toString();

        LOG.info("Executing query:" + query);
        session.execute(query);
    }

    public static void createTable(Session session, String keyspaceName) {
        StringBuilder sb = new StringBuilder(
                "CREATE TABLE IF NOT EXISTS ").
                append("MySampleWindow").
                append("(").
                append("cassandraid varchar,").
                append("key2 varchar,").
                append("value1 int,").
                append("value2 double,").
                append("PRIMARY KEY (cassandraid));");


        final String query = sb.toString();

        LOG.info("Executing query:" + query);
        session.execute(query);
    }

    public static void insertRow(Session session, String keyspaceName, String cassandraId, String key2, int value1, double value2) {
        StringBuilder sb = new StringBuilder("INSERT INTO ").append("MySampleWindow")
                .append("(cassandraid, key2, value1, value2) ")
                .append("VALUES ('")
                .append(cassandraId)
                .append("', '")
                .append(key2)
                .append("',")
                .append(value1)
                .append(",")
                .append(value2)
                .append(");");

        final String query = sb.toString();
        LOG.info("Executing query:" + query);
        session.execute(query);
    }

    public static void close(Session session) {
        Cluster cluster = session.getCluster();
        session.close();
        cluster.close();
    }
}
