/*
 * Copyright 2018-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.extensions.failauth;

import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.auth.SimpleAuthenticator;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthInput;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthOutput;
import com.hivemq.extension.sdk.api.events.client.ClientLifecycleEventListener;
import com.hivemq.extension.sdk.api.events.client.parameters.*;
import com.hivemq.extension.sdk.api.interceptor.connack.ConnackOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.connack.ConnackOutboundInterceptorProvider;
import com.hivemq.extension.sdk.api.interceptor.connack.parameter.ConnackOutboundInput;
import com.hivemq.extension.sdk.api.interceptor.connack.parameter.ConnackOutboundOutput;
import com.hivemq.extension.sdk.api.interceptor.connack.parameter.ConnackOutboundProviderInput;
import com.hivemq.extension.sdk.api.interceptor.connect.ConnectInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.connect.ConnectInboundInterceptorProvider;
import com.hivemq.extension.sdk.api.interceptor.connect.parameter.ConnectInboundInput;
import com.hivemq.extension.sdk.api.interceptor.connect.parameter.ConnectInboundOutput;
import com.hivemq.extension.sdk.api.interceptor.connect.parameter.ConnectInboundProviderInput;
import com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode;
import com.hivemq.extension.sdk.api.parameter.*;
import com.hivemq.extension.sdk.api.services.Services;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Dasha Samkova
 */
public class HelloWorldMain implements ExtensionMain {

    private static final @NotNull Logger log = LoggerFactory.getLogger(HelloWorldMain.class);

    @Override
    public void extensionStart(
            final @NotNull ExtensionStartInput extensionStartInput,
            final @NotNull ExtensionStartOutput extensionStartOutput) {

        try {
            final ClientLifecycleEventListener clientLifecycleEventListener = new ClientLifecycleEventListener(){

                @Override
                public void onMqttConnectionStart(@NotNull ConnectionStartInput connectionStartInput) {
                    log.debug("ClientLifecycleEventListener.onMqttConnectionStart() --> Client ID: {}, Connect MQTT version: {}, Connection MQTT version: {}",
                            connectionStartInput.getClientInformation().getClientId(),
                            connectionStartInput.getConnectPacket().getMqttVersion(),
                            connectionStartInput.getConnectionInformation().getMqttVersion());
                }

                @Override
                public void onAuthenticationSuccessful(@NotNull AuthenticationSuccessfulInput authenticationSuccessfulInput) {
                    log.debug("ClientLifecycleEventListener.onAuthenticationSuccessful() --> Client ID: {}, Connection MQTT version: {}",
                            authenticationSuccessfulInput.getClientInformation().getClientId(),
                            authenticationSuccessfulInput.getConnectionInformation().getMqttVersion());
                }

                @Override
                public void onDisconnect(@NotNull DisconnectEventInput disconnectEventInput) {
                    log.debug("ClientLifecycleEventListener.onDisconnect() --> Client ID: {}, Connection MQTT version: {}, Reason code: {}, Reason string: {}",
                            disconnectEventInput.getClientInformation().getClientId(),
                            disconnectEventInput.getConnectionInformation().getMqttVersion(),
                            disconnectEventInput.getReasonCode(),
                            disconnectEventInput.getReasonString());
                }

                @Override
                public void onAuthenticationFailedDisconnect(@NotNull AuthenticationFailedInput authenticationFailedInput) {
                    log.debug("ClientLifecycleEventListener.onAuthenticationFailedDisconnect() --> Client ID: {}, Connection MQTT version: {}, Reason code: {}, Reason string: {}",
                            authenticationFailedInput.getClientInformation().getClientId(),
                            authenticationFailedInput.getConnectionInformation().getMqttVersion(),
                            authenticationFailedInput.getReasonCode(),
                            authenticationFailedInput.getReasonString());

                    ClientLifecycleEventListener.super.onAuthenticationFailedDisconnect(authenticationFailedInput);
                }

                @Override
                public void onConnectionLost(@NotNull ConnectionLostInput connectionLostInput) {
                    log.debug("ClientLifecycleEventListener.onConnectionLost() --> Client ID: {}, Connection MQTT version: {}, Reason code: {}, Reason string: {}",
                            connectionLostInput.getClientInformation().getClientId(),
                            connectionLostInput.getConnectionInformation().getMqttVersion(),
                            connectionLostInput.getReasonCode(),
                            connectionLostInput.getReasonString());

                    ClientLifecycleEventListener.super.onConnectionLost(connectionLostInput);
                }

                @Override
                public void onClientInitiatedDisconnect(@NotNull ClientInitiatedDisconnectInput clientInitiatedDisconnectInput) {
                    log.debug("ClientLifecycleEventListener.onClientInitiatedDisconnect() --> Client ID: {}, Connection MQTT version: {}, Reason code: {}, Reason string: {}",
                            clientInitiatedDisconnectInput.getClientInformation().getClientId(),
                            clientInitiatedDisconnectInput.getConnectionInformation().getMqttVersion(),
                            clientInitiatedDisconnectInput.getReasonCode(),
                            clientInitiatedDisconnectInput.getReasonString());

                    ClientLifecycleEventListener.super.onClientInitiatedDisconnect(clientInitiatedDisconnectInput);
                }

                @Override
                public void onServerInitiatedDisconnect(@NotNull ServerInitiatedDisconnectInput serverInitiatedDisconnectInput) {
                    log.debug("ClientLifecycleEventListener.onServerInitiatedDisconnect() --> Client ID: {}, Connection MQTT version: {}, Reason code: {}, Reason string: {}",
                            serverInitiatedDisconnectInput.getClientInformation().getClientId(),
                            serverInitiatedDisconnectInput.getConnectionInformation().getMqttVersion(),
                            serverInitiatedDisconnectInput.getReasonCode(),
                            serverInitiatedDisconnectInput.getReasonString());

                    ClientLifecycleEventListener.super.onServerInitiatedDisconnect(serverInitiatedDisconnectInput);
                }
            };
            Services.eventRegistry().setClientLifecycleEventListener(input -> clientLifecycleEventListener);
            final ConnackOutboundInterceptor connackOutboundInterceptor = new ConnackOutboundInterceptor() {
                @Override
                public void onOutboundConnack(final @NotNull ConnackOutboundInput connackOutboundInput, final @NotNull ConnackOutboundOutput connackOutboundOutput) {

                    try {
                        log.debug("connackOutboundInterceptor.onOutboundConnack() --> Client ID: {}, Reason code: {}, Reason string: {}, MQTT version: {}",
                                connackOutboundInput.getClientInformation().getClientId(),
                                connackOutboundInput.getConnackPacket().getReasonCode(),
                                connackOutboundInput.getConnackPacket().getReasonString(),
                                connackOutboundInput.getConnectionInformation().getMqttVersion());


                    } catch (final Exception e) {
                        log.debug("Connack outbound interception failed: ", e);
                    }
                }
            };

            Services.interceptorRegistry().setConnackOutboundInterceptorProvider(new ConnackOutboundInterceptorProvider() {
                @Override
                public ConnackOutboundInterceptor getConnackOutboundInterceptor(final @NotNull ConnackOutboundProviderInput input) {
                    return connackOutboundInterceptor;
                }
            });

            final ConnectInboundInterceptor connectInboundInterceptor = new ConnectInboundInterceptor() {

                @Override
                public void onConnect(@NotNull ConnectInboundInput connectInboundInput, @NotNull ConnectInboundOutput connectInboundOutput) {
                    log.debug("ConnectInboundInterceptor.onConnect() --> Client ID: {}, MQTT version: {}",
                            connectInboundInput.getClientInformation().getClientId(),
                            connectInboundInput.getConnectionInformation().getMqttVersion());
                }
            };
            Services.interceptorRegistry().setConnectInboundInterceptorProvider(new ConnectInboundInterceptorProvider() {
                @Override
                public @Nullable ConnectInboundInterceptor getConnectInboundInterceptor(@NotNull ConnectInboundProviderInput input) {
                    return connectInboundInterceptor;
                }
            });

            final SimpleAuthenticator failingAuthenticator = new SimpleAuthenticator() {
                @Override
                public void onConnect(@NotNull SimpleAuthInput simpleAuthInput, @NotNull SimpleAuthOutput simpleAuthOutput) {
                    log.debug("failingAuthenticator.onConnect() --> Client ID: {}, MQTT version: {}",
                            simpleAuthInput.getClientInformation().getClientId(),
                            simpleAuthInput.getConnectionInformation().getMqttVersion());
                    simpleAuthOutput.failAuthentication(ConnackReasonCode.NOT_AUTHORIZED, "Reason Code: NOT_AUTHORIZED");
                }
            };

            Services.securityRegistry().setAuthenticatorProvider(authenticatorProviderInput -> failingAuthenticator);

            final ExtensionInformation extensionInformation = extensionStartInput.getExtensionInformation();
            log.info("Started " + extensionInformation.getName() + ":" + extensionInformation.getVersion());

        } catch (final Exception e) {
            log.error("Exception thrown at extension start: ", e);
        }
    }

    @Override
    public void extensionStop(
            final @NotNull ExtensionStopInput extensionStopInput,
            final @NotNull ExtensionStopOutput extensionStopOutput) {

        final ExtensionInformation extensionInformation = extensionStopInput.getExtensionInformation();
        log.info("Stopped " + extensionInformation.getName() + ":" + extensionInformation.getVersion());
    }


}