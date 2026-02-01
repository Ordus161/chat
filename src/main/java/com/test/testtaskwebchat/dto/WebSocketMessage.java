package com.test.testtaskwebchat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {
    private String type; // "MESSAGE", "USER_JOINED", "USER_LEFT"
    private Object data;
    private Long timestamp;
}
