package com.yyds.feng.crypto.ws.config;

import com.yyds.feng.crypto.ws.handler.PriceSpreadWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class CryptoWebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private PriceSpreadWebSocketHandler priceSpreadWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(priceSpreadWebSocketHandler, "/ws/price")
                .setAllowedOrigins("*");
    }
}
