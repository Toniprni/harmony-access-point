package eu.domibus.core.logging;

import eu.domibus.api.cluster.Command;
import eu.domibus.api.cluster.CommandProperty;
import eu.domibus.core.clustering.CommandTask;
import eu.domibus.logging.IDomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author Cosmin Baciu
 * @since 4.2
 */
@Service
public class LoggingSetLevelCommandTask implements CommandTask {

    private static final IDomibusLogger LOGGER = DomibusLoggerFactory.getLogger(LoggingSetLevelCommandTask.class);

    protected LoggingService loggingService;

    public LoggingSetLevelCommandTask(LoggingService loggingService) {
        this.loggingService = loggingService;
    }

    @Override
    public boolean canHandle(String command) {
        return StringUtils.equalsIgnoreCase(Command.LOGGING_SET_LEVEL, command);
    }

    @Override
    public void execute(Map<String, String> properties) {
        LOGGER.debug("Setting log level command");

        final String level = properties.get(CommandProperty.LOG_LEVEL);
        final String name = properties.get(CommandProperty.LOG_NAME);
        loggingService.setLoggingLevel(name, level);
    }
}
