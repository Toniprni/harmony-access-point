package eu.domibus.plugin.webService.backend.rules;

import java.io.Serializable;
import java.util.Date;

/**
 * @author François Gautier
 * @since 5.0
 */
public enum WSPluginRetryStrategy {

    CONSTANT("CONSTANT", WSPluginRetryStrategy.ConstantAttemptAlgorithm.ALGORITHM),
    SEND_ONCE("SEND_ONCE", WSPluginRetryStrategy.SendOnceAttemptAlgorithm.ALGORITHM);

    private final String name;
    private final WSPluginRetryStrategy.AttemptAlgorithm algorithm;

    WSPluginRetryStrategy(final String name, final WSPluginRetryStrategy.AttemptAlgorithm attemptAlgorithm) {
        this.name = name;
        this.algorithm = attemptAlgorithm;
    }

    public String getName() {
        return this.name;
    }

    public WSPluginRetryStrategy.AttemptAlgorithm getAlgorithm() {
        return this.algorithm;
    }

    public enum ConstantAttemptAlgorithm implements WSPluginRetryStrategy.AttemptAlgorithm {

        ALGORITHM {
            @Override
            public Date compute(final Date received, int maxAttempts, final int timeoutInMinutes) {
                int MULTIPLIER_MINUTES_TO_SECONDS = 60000;
                if(maxAttempts < 0 || timeoutInMinutes < 0 || received == null) {
                    return null;
                }
                if(maxAttempts > MULTIPLIER_MINUTES_TO_SECONDS) {
                    maxAttempts = MULTIPLIER_MINUTES_TO_SECONDS;
                }
                final long now = System.currentTimeMillis();
                long retry = received.getTime();
                final long stopTime = received.getTime() + ( (long)timeoutInMinutes * MULTIPLIER_MINUTES_TO_SECONDS ) + 5000; // We grant 5 extra seconds to avoid not sending the last attempt
                while (retry <= (stopTime)) {
                    retry += (long)timeoutInMinutes * MULTIPLIER_MINUTES_TO_SECONDS / maxAttempts;
                    if (retry > now && retry < stopTime) {
                        return new Date(retry);
                    }
                }
                return null;
            }
        }
    }

    public enum SendOnceAttemptAlgorithm implements WSPluginRetryStrategy.AttemptAlgorithm {

        ALGORITHM {
            @Override
            public Date compute(final Date received, final int currentAttempts, final int timeoutInMinutes) {
                return null;
            }
        }
    }

    /**
     * NOT FINISHED *
     */
    public interface AttemptAlgorithm extends Serializable {
        Date compute(Date received, int maxAttempts, int timeoutInMinutes);
    }

}
