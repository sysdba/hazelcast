/*
 * Copyright (c) 2008-2020, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.sql.impl.state;

import com.hazelcast.sql.impl.QueryId;
import com.hazelcast.sql.impl.QueryPlan;

/**
 * Query state which is specific to the initiator node only.
 */
public class QueryInitiatorState {
    /** Query ID. */
    private final QueryId queryId;

    /** Plan. */
    private final QueryPlan plan;

    /** Iteartor. */
    private final QueryStateRowSource rowSource;

    /** Query timeout. */
    private final long timeout;

    public QueryInitiatorState(
        QueryId queryId,
        QueryPlan plan,
        QueryStateRowSource rowSource,
        long timeout
    ) {
        this.queryId = queryId;
        this.plan = plan;
        this.rowSource = rowSource;
        this.timeout = timeout;
    }

    public QueryId getQueryId() {
        return queryId;
    }

    public QueryPlan getPlan() {
        return plan;
    }

    public QueryStateRowSource getRowSource() {
        return rowSource;
    }

    public long getTimeout() {
        return timeout;
    }
}