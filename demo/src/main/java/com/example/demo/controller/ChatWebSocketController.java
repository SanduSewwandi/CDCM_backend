package com.example.demo.controller;

import com.example.demo.dto.SendMessageRequest;
import com.example.demo.model.Message;
import com.example.demo.service.ChatService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class ChatWebSocketController {

    @Autowired
    private ChatService chatService;


    @MessageMapping("/chat.send")
    public void sendMessage(
            SendMessageRequest request) {

        if (request.getConversationId() == null ||
                request.getConversationId().trim().isEmpty()) {

            return;
        }

        chatService.sendMessage(
                request.getConversationId(),
                request
        );
    }
}