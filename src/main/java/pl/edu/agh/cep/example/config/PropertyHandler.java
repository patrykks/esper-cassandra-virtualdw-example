package pl.edu.agh.cep.example.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertyHandler{
    private static final Logger LOG = LoggerFactory.getLogger(PropertyHandler.class);
    private static final String PROPERTIES_FILENAME = "application.properties";

    private static PropertyHandler instance;

    private Properties props;

    private PropertyHandler(){
        this.props = new Properties();

        try (InputStream inputStream = PropertyHandler.class.getClassLoader().getResourceAsStream(PROPERTIES_FILENAME)) {
            this.props.load(inputStream);
        } catch (IOException ex) {
            LOG.error("Problem occurs while reading property file !", ex);
        }

        LOG.info("Property file read with success");
    }

    public static synchronized PropertyHandler getInstance(){
        if (instance == null)
            instance = new PropertyHandler();
        return instance;
    }

    public String getValue(String propKey){
        return this.props.getProperty(propKey);
    }
}
