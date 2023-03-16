# ping-source-operator

This application demonstrates usage of [JOSDK CachingInboundEventSource](https://javaoperatorsdk.io/docs/features#inbound-event-sources).

## Running the application in dev mode

You can run the application in dev mode that enables live coding using:

```shell script
./mvnw compile quarkus:dev
```

## Design

A PingSource resource allows to publish a message periodically. The content and the schedule can be specified.

```yaml
apiVersion: com.inulogic/v1alpha1
kind: PingSource
metadata:
  name: client
spec:
  schedule: "0/1 * * ? * * *"
  contentType: application/json
  data: |
    {}
```

| Field              | Description                                          |
| ------------------ | ---------------------------------------------------- |
| `spec.schedule`    | Specifies the cron schedule.                         |
| `spec.data`        | The data used as the body of the message.            |
| `spec.contentType` | The media type of `data`.                            |

- Scheduling is done using [Quarkus Quartz extension](https://quarkus.io/guides/quartz)
- Sending HTTP message is done using [Quarkus Extension for Reactive Messaging with HTTP](https://github.com/quarkiverse/quarkus-reactive-messaging-http)
