package com.inulogic;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.function.Predicate;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import io.quarkus.test.common.QuarkusTestResourceConfigurableLifecycleManager;

public class WiremockTestResource implements QuarkusTestResourceConfigurableLifecycleManager<WithWiremock> {

    private WireMockServer wireMockServer;
    private String name;
    private String urlProperty;
    private WireMockConfiguration config;

    @Override
    public void init(WithWiremock annotation) {
        this.name = annotation.name();
        this.urlProperty = annotation.urlProperty();
        this.config = WireMockConfiguration.options()
        .notifier(new ConsoleNotifier(true))
        .dynamicPort();
    }

    @Override
    public Map<String, String> start() {
        wireMockServer = new WireMockServer(config);
        wireMockServer.start();

        return Map.of(urlProperty, wireMockServer.baseUrl());
    }

    @Override
    public void stop() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Override
    public void inject(TestInjector testInjector) {
        testInjector.injectIntoFields(wireMockServer,
                new AnnotatedAndMatchesTypeAndName(this.name));
    }

    class AnnotatedAndMatchesTypeAndName implements Predicate<Field> {

        public AnnotatedAndMatchesTypeAndName(String name) {
        }

        @Override
        public boolean test(Field field) {
            Wiremock annotation = field.getAnnotation(Wiremock.class);
            if (annotation == null) {
                return false;
            }

            if (!name.equals(annotation.name())) {
                return false;
            }

            return field.getType().isAssignableFrom(WireMockServer.class);
        }
    }
}