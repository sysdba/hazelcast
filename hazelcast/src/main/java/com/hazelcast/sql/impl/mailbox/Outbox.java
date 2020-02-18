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

package com.hazelcast.sql.impl.mailbox;

import com.hazelcast.cluster.Member;
import com.hazelcast.spi.impl.NodeEngine;
import com.hazelcast.sql.HazelcastSqlException;
import com.hazelcast.sql.SqlErrorCode;
import com.hazelcast.sql.impl.QueryFragmentContext;
import com.hazelcast.sql.impl.QueryId;
import com.hazelcast.sql.impl.SqlServiceImpl;
import com.hazelcast.sql.impl.operation.QueryBatchOperation;
import com.hazelcast.sql.impl.row.Row;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * Outbox which sends data to a single remote stripe.
 */
public class Outbox extends AbstractMailbox {
    /** Target member ID. */
    private final UUID targetMemberId;

    /** Batch size. */
    private final int batchSize;

    /** SQL service. */
    private SqlServiceImpl sqlService;

    /** Target member. */
    private Member targetMember;

    /** Pending rows.. */
    private List<Row> batch;

    public Outbox(
        QueryId queryId,
        int edgeId,
        UUID targetMemberId,
        int batchSize
    ) {
        super(queryId, edgeId);

        this.targetMemberId = targetMemberId;
        this.batchSize = batchSize;
    }

    public void setup(QueryFragmentContext context) {
        NodeEngine nodeEngine = context.getNodeEngine();

        sqlService = nodeEngine.getSqlService();

        targetMember = nodeEngine.getClusterService().getMember(targetMemberId);

        if (targetMember == null) {
            throw HazelcastSqlException.error(SqlErrorCode.MEMBER_LEAVE,
                "Outbox target member has left topology: " + targetMemberId);
        }
    }

    public UUID getTargetMemberId() {
        return targetMemberId;
    }

    /**boolean success = false;

        try {

        } catch (Exception e) {
            throw HazelcastSqlException.error(SqlErrorCode.MEMBER_LEAVE,
                "Failed to send data batch to member: " + targetMemberId
            );
        }
     * Accept a row.
     *
     * @param row Row.
     * @return {@code True} if the outbox can accept more data.
     */
    public boolean onRow(Row row) {
        if (batch == null) {
            batch = new LinkedList<>();
        }

        batch.add(row);

        if (batch.size() >= batchSize) {
            send(false);
        }

        return true;
    }

    /**
     * Flush remaining rows.
     */
    public void flush() {
        send(true);
    }

    /**
     * Send rows to target member.
     *
     * @param last Whether this is the last batch.
     */
    private void send(boolean last) {
        List<Row> batch0 = batch;

        if (batch0 == null) {
            batch0 = Collections.emptyList();
        }

        QueryBatchOperation op = new QueryBatchOperation(
            sqlService.getEpochWatermark(), queryId,
            getEdgeId(),
            new SendBatch(batch0, last)
        );

        boolean success = sqlService.sendRequest(op, targetMember.getAddress());

        if (!success) {
            throw HazelcastSqlException.error(SqlErrorCode.MEMBER_LEAVE,
                "Failed to send data batch to member: " + targetMemberId);
        }

        batch = null;
    }

    @Override
    public String toString() {
        return "Outbox {queryId=" + queryId
            + ", edgeId=" + getEdgeId()
            + ", targetMemberId=" + targetMemberId
            + '}';
    }
}