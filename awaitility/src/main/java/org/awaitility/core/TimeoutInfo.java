/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.awaitility.core;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class TimeoutInfo {

    private static final Set<TimeoutReason> LATE_TIMEOUT_REASON_SET = new HashSet<>(Arrays.asList(
            TimeoutReason.CONDITION_NOT_MET,
            TimeoutReason.CONDITION_NOT_HELD
    ));

    private final String alias;
    private final String timeoutMessage;
    private final Duration evaluationDuration;
    private final TimeoutReason timeoutReason;

    public TimeoutInfo(String alias, String timeoutMessage, Duration evaluationDuration, TimeoutReason timeoutReason) {
        this.alias = alias;
        this.timeoutMessage = timeoutMessage;
        this.evaluationDuration = evaluationDuration;
        this.timeoutReason = timeoutReason;
    }

    public String getAlias() {
        return alias;
    }

    public String getTimeoutMessage() {
        return timeoutMessage;
    }

    public Duration getEvaluationDuration() {
        return evaluationDuration;
    }

    public TimeoutReason getTimeoutReason() {
        return timeoutReason;
    }

    public boolean isEarlyTimeout() {
        return !LATE_TIMEOUT_REASON_SET.contains(timeoutReason);
    }

    public boolean isLateTimeout() {
        return LATE_TIMEOUT_REASON_SET.contains(timeoutReason);
    }
}
