package com.inulogic;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.Duration;

import javax.inject.Inject;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.inulogic.api.PingSource;
import com.inulogic.api.PingSourceSpec;

import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.Operator;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@WithWiremock(name = "ping-source-requests", urlProperty = "mp.messaging.outgoing.ping-source-requests.url")
class PingSourceOperatorE2ETest {

    @Inject
    Operator operator;

    @Inject
    KubernetesClient client;

    @Wiremock(name = "ping-source-requests")
    WireMockServer wireMockServer;

    @Test
    void canPingAndReportStatus() {
        operator.start();

        var job = new PingSource();
        job.setMetadata(
                new ObjectMetaBuilder()
                        .withName("test-ok")
                        .withNamespace(client.getNamespace())
                        .build());
        job.setSpec(new PingSourceSpec() {
            {
                setData("{}");
                setContentType("application/test");
                setSchedule("* * * ? * *");
            }
        });

        job = client.resource(job).createOrReplace();
        try {
            awaitUntilCondition(job, "Ready", "True");
            await()
                    .pollInterval(Duration.ofMillis(300))
                    .atMost(5, SECONDS)
                    .untilAsserted(
                            () -> {
                                wireMockServer.verify(postRequestedFor(urlEqualTo("/")));
                            });
            awaitUntilCondition(job, "Trigger", "True");
        } finally {
            client.resource(job).delete();
        }
    }

    @Test
    void canReportPingFailure() {
        operator.start();

        var job = new PingSource();
        job.setMetadata(
                new ObjectMetaBuilder()
                        .withName("test-ko")
                        .withNamespace(client.getNamespace())
                        .build());
        job.setSpec(new PingSourceSpec() {
            {
                setData("[]");
                setContentType("application/test");
                setSchedule("0/1 * * ? * * *");
            }
        });

        job = client.resource(job).createOrReplace();

        try {
            awaitUntilCondition(job, "Trigger", "False");
            await()
                .pollInterval(Duration.ofMillis(300))
                .atMost(5, SECONDS)
                .untilAsserted(
                        () -> {
                            wireMockServer.verify(postRequestedFor(urlEqualTo("/")));
                        });
        } finally {
            client.resource(job).delete();
        }
    }

    private void awaitUntilCondition(PingSource job, String type, String status) {
        await()
                .pollInterval(Duration.ofMillis(300))
                .atMost(5, SECONDS)
                .untilAsserted(
                        () -> {
                            var updatedJob = client
                                    .resources(PingSource.class)
                                    .inNamespace(job.getMetadata().getNamespace())
                                    .withName(job.getMetadata().getName())
                                    .get();

                            assertThat(updatedJob, is(notNullValue()));
                            assertThat(updatedJob.getStatus(), is(notNullValue()));
                            assertThat(updatedJob.getStatus().getConditions(), is(notNullValue()));
                            assertThat(updatedJob.getStatus().getConditions(),
                                    hasItem(both(hasProperty("type", equalTo(type)))
                                            .and(hasProperty("status", equalTo(status)))));
                        });
    }
}