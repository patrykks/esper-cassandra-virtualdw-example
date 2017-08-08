package pl.edu.agh.cep.example.virtualdw;

import com.datastax.driver.core.Session;
import com.espertech.esper.client.*;
import com.espertech.esper.client.util.EventUnderlyingType;
import com.espertech.esper.util.UuidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.edu.agh.CassandraVirtualDataWindowFactory;
import pl.edu.agh.cep.example.cassandra.CassandraUtils;
import pl.edu.agh.cep.example.config.PropertyHandler;

import java.util.Random;
import java.util.stream.IntStream;

public class SampleVirtualDataWindowMain {
    private static final Logger log = LoggerFactory.getLogger(SampleVirtualDataWindowMain.class);
    private Random random;

    public static void main(String[] args) {
        SampleVirtualDataWindowMain sample = new SampleVirtualDataWindowMain();
        try {
            sample.run();
        } catch (RuntimeException ex) {
            log.error("Unexpected exception :" + ex.getMessage(), ex);
        }
    }

    public void run() {
        log.info("Populating cassandra-database.");
        populateCassandraDatabaseWithSampleData(random);

        log.info("Setting up engine instance.");

        Configuration config = new Configuration();
        config.getEngineDefaults().getEventMeta().setDefaultEventRepresentation(EventUnderlyingType.MAP); // use Map-type events for testing
        config.addPlugInVirtualDataWindow("sample", "samplevdw", CassandraVirtualDataWindowFactory.class.getName());
        config.addEventTypeAutoName(SampleVirtualDataWindowMain.class.getPackage().getName());    // import all event classes

        EPServiceProvider epService = EPServiceProviderManager.getProvider("LargeExternalDataExample", config);

        // First: Create an event type for rows of the external data - here the example use a Map-based event and any of the other types (POJO, XML) can be used as well.
        // Populate event property names and types.
        // Note: the type must match the data returned by virtual data window indexes.
        epService.getEPAdministrator().createEPL("create schema SampleEvent as (cassandraid string, key2 string, value1 int, value2 double)");

        log.info("Creating named window with virtual.");

        // Create Named Window holding SampleEvent instances
        epService.getEPAdministrator().createEPL("create window MySampleWindow.sample:samplevdw('\n" +
                "{\n" +
                "    \"host\":" + PropertyHandler.getInstance().getValue("CASSANDRA_HOST") + ", \n" +
                "    \"keyspace\":" + PropertyHandler.getInstance().getValue("CASSANDRA_KEYSPACE") +",\n" +
                "    \"table\":"+ PropertyHandler.getInstance().getValue("CASSANDRA_TABLE") + ",\n" +
                "    \"username-property\":" + PropertyHandler.getInstance().getValue("CASSANDRA_USERNAME_PROPERTY") +",\n" +
                "    \"password-property\":" + PropertyHandler.getInstance().getValue("CASSANDRA_PASSWORD_PROPERTY") +"\n" +
                "    \"root-username-property\":" + PropertyHandler.getInstance().getValue("CASSANDRA_ROOT_USERNAME_PROPERTY") +",\n" +
                "    \"root-password-property\":" + PropertyHandler.getInstance().getValue("CASSANDRA_ROOT_PASSWORD_PROPERTY") +"\n" +
                "}') \n" +
                "as SampleEvent");

        // Example subquery
        log.info("Running subquery example.");
        runSubquerySample(epService);

        // Example joins
        log.info("Running join example.");
        runJoinSample(epService);

        // Sample FAF
        log.info("Running fire-and-forget query example.");
        runSampleFireAndForgetQuery(epService);

        // Sample On-Merge
        log.info("Running on-merge example.");
        runSampleOnMerge(epService);

        // Cleanup
        log.info("Destroying engine instance, sample completed successfully.");
        epService.destroy();
    }

    private void populateCassandraDatabaseWithSampleData(Random random) {
        Session session = CassandraUtils.initializeSession();

        CassandraUtils.createKeyspace(session, "sample", "SimpleStrategy", 1);
        CassandraUtils.useKeyspace(session, "sample");
        CassandraUtils.createTable(session, "sample");
        CassandraUtils.insertRow(session, "sample", "sample1", "sample2", random.nextInt(), random.nextDouble());
        IntStream.range(1, 10).forEach(
                $ -> CassandraUtils.insertRow(session, "sample", UuidGenerator.generate(), UuidGenerator.generate(), random.nextInt(), random.nextDouble()));

        CassandraUtils.close(session);
    }

    private void runSubquerySample(EPServiceProvider epService) {

        String epl = "select (select key2 from MySampleWindow where cassandraid = ste.triggerKey) as key2 from SampleTriggerEvent ste";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SampleUpdateListener sampleListener = new SampleUpdateListener();
        stmt.addListener(sampleListener);

        epService.getEPRuntime().sendEvent(new SampleTriggerEvent("sample1"));
        log.info("Subquery returned: " + sampleListener.getLastEvent().get("key2"));
        // For assertions against expected results please see the regression test suite
    }

    private void runJoinSample(EPServiceProvider epService) {
        String epl = "select sw.* " +
                "from SampleJoinEvent#lastevent sje, MySampleWindow sw " +
                "where sw.cassandraid = sje.propOne and sw.key2 = sje.propTwo";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SampleUpdateListener sampleListener = new SampleUpdateListener();
        stmt.addListener(sampleListener);

        epService.getEPRuntime().sendEvent(new SampleJoinEvent("sample1", "sample2")); // see values in SampleVirtualDataWindowIndex
        log.info("Join query returned: " + sampleListener.getLastEvent().get("cassandraid") + " and " + sampleListener.getLastEvent().get("key2"));

        // For assertions against expected results please see the regression test suite
    }

    private void runSampleFireAndForgetQuery(EPServiceProvider epService) {
        String fireAndForget = "select * from MySampleWindow where cassandraid = 'sample1' and key2 = 'sample2'"; // see values in SampleVirtualDataWindowIndex
        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery(fireAndForget);

        log.info("Fire-and-forget query returned: " + result.getArray()[0].get("cassandraid") + " and " + result.getArray()[0].get("key2"));

        // For assertions against expected results please see the regression test suite
    }

    private void runSampleOnMerge(EPServiceProvider epService) {

        String onDelete =
                "on SampleMergeEvent " +
                        "merge MySampleWindow " +
                        "where cassandraid = propOne " +
                        "when not matched then insert select propOne as cassandraid, propTwo as key2, 0 as value1, 0d as value2 " +
                        "when matched then update set key2 = propTwo";

        EPStatement stmt = epService.getEPAdministrator().createEPL(onDelete);
        SampleUpdateListener sampleListener = new SampleUpdateListener();
        stmt.addListener(sampleListener);

        // not-matching case
        epService.getEPRuntime().sendEvent(new SampleMergeEvent("mykey-sample", "hello"));
        log.info("Received inserted key: " + sampleListener.getLastEvent().get("cassandraid") + " and " + sampleListener.getLastEvent().get("key2"));

        // matching case
        epService.getEPRuntime().sendEvent(new SampleMergeEvent("sample1", "newvalue"));  // see values in SampleVirtualDataWindowIndex
        log.info("Received updated key: " + sampleListener.getLastEvent().get("cassandraid") + " and " + sampleListener.getLastEvent().get("key2"));

        // For assertions against expected results please see the regression test suite
    }
}


