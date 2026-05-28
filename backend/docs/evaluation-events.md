# Evaluation Kafka Event Contract

This document separates Kafka events from HTTP queries.

Kafka is used for asynchronous work:

- Start evaluation
- Collect/store evaluation results
- Notify completion/failure
- Preserve malformed messages in DLQ topics

HTTP is used for synchronous queries:

- Read evaluation result from MongoDB

## Ordering And Partitioning

Ordering is sensitive when multiple messages for the same business object must be processed in order.

Examples:

- Multiple updates for the same evaluation session
- Partial result collection for the same n8n session
- Status transitions such as STARTED -> COMPLETED -> FAILED

The evaluation start flow is less ordering-sensitive because one campaign benefit usually creates one start request.
The collect/result flow is more sensitive because several result fragments can belong to the same session.

Initial production-friendly recommendation:

| Topic | Partitions | Replication factor |
| --- | ---: | ---: |
| `evaluation.start` | 3 | 3 in production, 1 in local |
| `evaluation.collect` | 3 | 3 in production, 1 in local |
| `evaluation.completed` | 3 | 3 in production, 1 in local |
| `evaluation.failed` | 3 | 3 in production, 1 in local |
| `evaluation.start.dlq` | 3 | 3 in production, 1 in local |
| `evaluation.collect.dlq` | 3 | 3 in production, 1 in local |

Partition count can be increased later, but cannot be decreased.
Increasing partitions can change key-to-partition mapping for new messages.

## Consumer Group

`matching-evaluation` is the consumer group for the evaluation worker service.

Same group:

- Multiple matching-evaluation instances share the work.
- One message is processed by one instance in the group.

Different group:

- Each group receives its own copy of the same event.

## Events

### `evaluation.start`

Purpose: app requests matching-evaluation to start an evaluation.

Producer: `app`

Consumer: `matching-evaluation`

Key: `campaignPublicId`

Current compatibility payload:

```json
{
  "campaignIdx": "campaign-public-id",
  "campaign": {},
  "benefit": {}
}
```

Preferred future payload:

```json
{
  "campaignPublicId": "campaign-public-id",
  "campaignIdx": 100001,
  "campaign": {},
  "benefit": {}
}
```

### `evaluation.collect`

Purpose: collect evaluation result fragments and prepare MongoDB persistence.

Producer: app or result callback adapter

Consumer: `matching-evaluation`

Key: `sessionId` / `uuid`

Payload fields should include:

```json
{
  "sessionId": "n8n-session-id",
  "campaignPublicId": "campaign-public-id",
  "benefitIdx": 100001,
  "category": "CUSTOMER",
  "overallScore": 80,
  "improvementDirections": []
}
```

### `evaluation.completed`

Purpose: matching-evaluation tells other services that evaluation finished.

Producer: `matching-evaluation`

Consumer: app or notification service, optional

Key: `campaignPublicId`

Payload:

```json
{
  "eventId": "uuid",
  "eventType": "EVALUATION_COMPLETED",
  "schemaVersion": "1.0",
  "occurredAt": "2026-05-22T00:00:00Z",
  "campaignPublicId": "campaign-public-id",
  "sessionId": "n8n-session-id",
  "resultId": "mongodb-document-id"
}
```

### `evaluation.failed`

Purpose: matching-evaluation tells other services that evaluation failed.

Producer: `matching-evaluation`

Consumer: app or notification service, optional

Key: `campaignPublicId` or `sessionId`

Payload:

```json
{
  "eventId": "uuid",
  "eventType": "EVALUATION_FAILED",
  "schemaVersion": "1.0",
  "occurredAt": "2026-05-22T00:00:00Z",
  "campaignPublicId": "campaign-public-id",
  "sessionId": "n8n-session-id",
  "failedStage": "N8N_WEBHOOK",
  "reason": "timeout"
}
```

### DLQ Topics

DLQ means Dead Letter Queue.

Use DLQ topics to preserve messages that cannot be deserialized or processed.

Topics:

- `evaluation.start.dlq`
- `evaluation.collect.dlq`

Payload:

```json
{
  "eventId": "uuid",
  "eventType": "EVALUATION_START_DEAD_LETTER",
  "schemaVersion": "1.0",
  "occurredAt": "2026-05-22T00:00:00Z",
  "sourceTopic": "evaluation.start",
  "key": "campaign-public-id",
  "rawPayload": "{}",
  "reason": "deserialize failed"
}
```

## HTTP Query Contract

`result` is not a Kafka event.

Recommended API:

```http
GET /evaluation/result?campaignPublicId={campaignPublicId}
```

Current compatibility API:

```http
GET /evaluation/result?campaignIdx={campaignPublicId}
```

The matching-evaluation service should query MongoDB by `publicId` / `campaignPublicId`.
