package com.pacgram.messaging.config;

import com.pacgram.common.security.JwtService;
import com.pacgram.messaging.repository.ConversationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final JwtService jwtService;
    private final ConversationRepository conversationRepository;
    private final List<String> allowedOrigins;

    public WebSocketConfig(
            JwtService jwtService,
            ConversationRepository conversationRepository,
            @Value("${pacgram.cors.allowed-origins}") String allowedOrigins) {
        this.jwtService = jwtService;
        this.conversationRepository = conversationRepository;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList();
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOrigins(allowedOrigins.toArray(String[]::new));
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null) {
                    return message;
                }
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authorization = accessor.getFirstNativeHeader("Authorization");
                    if (authorization == null || !authorization.startsWith("Bearer ")) {
                        throw new IllegalArgumentException("A bearer token is required for the WebSocket connection");
                    }
                    var principal = jwtService.parse(authorization.substring("Bearer ".length()));
                    accessor.setUser(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
                }
                if ((StompCommand.SUBSCRIBE.equals(accessor.getCommand())
                        || StompCommand.SEND.equals(accessor.getCommand()))
                        && accessor.getUser() != null) {
                    String destination = accessor.getDestination();
                    if (StompCommand.SEND.equals(accessor.getCommand())
                            && destination != null && destination.startsWith("/topic/")) {
                        throw new IllegalArgumentException("Clients may not send directly to broker topics");
                    }
                    if (destination != null && (destination.startsWith("/topic/conversations/")
                            || destination.startsWith("/app/conversations/"))) {
                        String[] segments = destination.split("/");
                        if (segments.length < 4) {
                            throw new IllegalArgumentException("Invalid conversation destination");
                        }
                        Long conversationId;
                        try {
                            conversationId = Long.valueOf(segments[3]);
                        } catch (NumberFormatException exception) {
                            throw new IllegalArgumentException("Invalid conversation destination", exception);
                        }
                        var authentication = (UsernamePasswordAuthenticationToken) accessor.getUser();
                        var principal = (com.pacgram.common.security.AccountPrincipal) authentication.getPrincipal();
                        if (!conversationRepository.isParticipant(conversationId, principal.userId())) {
                            throw new IllegalArgumentException("You are not a participant in this conversation");
                        }
                    }
                }
                return message;
            }
        });
    }
}
