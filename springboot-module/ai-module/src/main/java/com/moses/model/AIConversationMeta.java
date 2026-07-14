package com.moses.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIConversationMeta {
    private String conversationId;
    private String title;
    private LocalDateTime lastActiveAt;
}
