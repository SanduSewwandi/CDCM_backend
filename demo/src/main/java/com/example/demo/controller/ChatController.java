package com.example.demo.controller;

import com.example.demo.dto.ConversationResponseDTO;
import com.example.demo.dto.SendMessageRequest;
import com.example.demo.model.Message;
import com.example.demo.service.ChatService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "http://localhost:5173")
public class ChatController {

    @Autowired
    private ChatService chatService;


    // ==========================================
    // PATIENT CONVERSATIONS
    // ==========================================

    @GetMapping("/patient/{patientId}")
    public List<ConversationResponseDTO> getPatientConversations(
            @PathVariable String patientId) {

        return chatService.getPatientConversations(
                patientId
        );
    }


    // ==========================================
    // DOCTOR CONVERSATIONS
    // ==========================================

    @GetMapping("/doctor/{doctorId}")
    public List<ConversationResponseDTO> getDoctorConversations(
            @PathVariable String doctorId) {

        return chatService.getDoctorConversations(
                doctorId
        );
    }


    // ==========================================
    // GET MESSAGES
    // ==========================================

    @GetMapping("/{conversationId}/messages")
    public List<Message> getMessages(
            @PathVariable String conversationId) {

        return chatService.getMessages(
                conversationId
        );
    }


    // ==========================================
    // SEND MESSAGE
    // ==========================================

    @PostMapping("/{conversationId}/messages")
    public Message sendMessage(
            @PathVariable String conversationId,
            @RequestBody SendMessageRequest request) {

        return chatService.sendMessage(
                conversationId,
                request
        );
    }
}