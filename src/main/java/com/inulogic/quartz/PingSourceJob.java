package com.inulogic.quartz;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Metadata;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import io.quarkus.reactivemessaging.http.runtime.OutgoingHttpMetadata;

public class PingSourceJob implements Job {

    @Channel("ping-source-requests")
    Emitter<String> pingSourceEmitter;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        var contentType = context.getMergedJobDataMap().getString("content-type");
        var data = context.getMergedJobDataMap().getString("data");

        final OutgoingHttpMetadata metadata = new OutgoingHttpMetadata .Builder()
                .addHeader("Content-Type", contentType)
                .build();

        CompletionStage<Void> acked = send(Message.of(data, Metadata.of(metadata)));
        // sending a payload returns a CompletionStage completed
        // when the message is acknowledged
        try {
            acked.toCompletableFuture().join();
        } catch (CompletionException e) {
            throw new JobExecutionException(e.getCause());
        }
    }

    private synchronized CompletionStage<Void> send(Message<String> payload) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        pingSourceEmitter.send(payload.withAck(() -> {
            future.complete(null);
            return CompletableFuture.completedFuture(null);
        }).withNack(
                reason -> {
                    future.completeExceptionally(reason);
                    return CompletableFuture.completedFuture(null);
                }));
        return future;
    }
}
