package org.schmidrules.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.schmidrules.configuration.dto.ArchitectureDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationLoader {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * The default name of the file containing the XML configuration.
     */
    public static final String DEFAULT_CONFIGURATION_FILE_NAME = "schmid-rules.xml";

    private ArchitectureDto architecture;

    /**
     * Creates new instance and loads default configuration.
     */
    public ConfigurationLoader() {
        loadConfiguration(DEFAULT_CONFIGURATION_FILE_NAME);
    }

    /**
     * @param fileName
     *            name of the <code>File</code> in the classpath to load configuration from.
     */
    public ConfigurationLoader(String fileName) {
        loadConfiguration(fileName);
    }

    protected void loadConfiguration(final String fileName) {
        File file = new File(fileName);
        InputStream stream = null;

        if (!file.exists()) {
            final ClassLoader classLoader = getClass().getClassLoader();
            stream = classLoader.getResourceAsStream(fileName);
        }

        if (stream == null) {
            try {
                stream = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                throw new IllegalArgumentException("Configration File not found: " + fileName, e);
            }
        }
        architecture = XmlUtil.unmarshal(new InputStreamReader(stream), ArchitectureDto.class);
        logger.info("Loading architecture from configuration file finished.");
    }

    public ArchitectureDto getArchitecture() {
        return architecture;
    }

}
