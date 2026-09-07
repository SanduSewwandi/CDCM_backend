package com.example.demo.service;

import com.example.demo.dto.ConversationResponseDTO;
import com.example.demo.dto.SendMessageRequest;
import com.example.demo.model.Appointment;
import com.example.demo.model.Conversation;
import com.example.demo.model.Message;
import com.example.demo.model.Doctor;
import com.example.demo.model.Patient;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.ConversationRepository;
import com.example.demo.repository.DoctorRepository;
import com.example.demo.repository.MessageRepository;
import com.example.demo.repository.PatientRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ChatService {

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;


    // =========================================================
    // CREATE CONVERSATION
    // =========================================================

    public Conversation createConversation(
            String appointmentId,
            String patientId,
            String doctorId) {

        Optional<Conversation> existing =
                conversationRepository.findByAppointmentId(appointmentId);

        if (existing.isPresent()) {
            return existing.get();
        }

        Conversation conversation = new Conversation();

        conversation.setAppointmentId(appointmentId);
        conversation.setPatientId(patientId);
        conversation.setDoctorId(doctorId);
        conversation.setCreatedAt(LocalDateTime.now());

        return conversationRepository.save(conversation);
    }


    // =========================================================
    // PATIENT CONVERSATIONS
    // =========================================================

    public List<ConversationResponseDTO> getPatientConversations(
            String patientId) {

        return conversationRepository
                .findByPatientId(patientId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }


    // =========================================================
    // DOCTOR CONVERSATIONS
    // =========================================================

    public List<ConversationResponseDTO> getDoctorConversations(
            String doctorId) {

        return conversationRepository
                .findByDoctorId(doctorId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }


    // =========================================================
    // CONVERT CONVERSATION TO DTO
    // =========================================================

    private ConversationResponseDTO convertToDTO(
            Conversation conversation) {

        ConversationResponseDTO dto =
                new ConversationResponseDTO();

        dto.setId(conversation.getId());

        // MongoDB appointment ID
        dto.setAppointmentId(
                conversation.getAppointmentId()
        );

        // -----------------------------------------------------
        // GET APPOINTMENT NUMBER
        // -----------------------------------------------------

        if (conversation.getAppointmentId() != null) {

            appointmentRepository
                    .findById(conversation.getAppointmentId())
                    .ifPresent(appointment -> {

                        dto.setAppointmentNumber(
                                appointment.getAppointmentNumber()
                        );
                    });
        }


        // -----------------------------------------------------
        // PATIENT
        // -----------------------------------------------------

        dto.setPatientId(
                conversation.getPatientId()
        );

        if (conversation.getPatientId() != null) {

            patientRepository
                    .findById(conversation.getPatientId())
                    .ifPresent(patient -> {

                        String name =
                                ((patient.getFirstName() != null)
                                        ? patient.getFirstName()
                                        : "")
                                        + " "
                                        + ((patient.getLastName() != null)
                                        ? patient.getLastName()
                                        : "");

                        dto.setPatientName(
                                name.trim()
                        );
                    });
        }


        // -----------------------------------------------------
        // DOCTOR
        // -----------------------------------------------------

        dto.setDoctorId(
                conversation.getDoctorId()
        );

        if (conversation.getDoctorId() != null) {

            doctorRepository
                    .findById(conversation.getDoctorId())
                    .ifPresent(doctor -> {

                        StringBuilder name =
                                new StringBuilder();

                        if (doctor.getTitle() != null) {
                            name.append(
                                    doctor.getTitle()
                            ).append(" ");
                        }

                        if (doctor.getFirstName() != null) {
                            name.append(
                                    doctor.getFirstName()
                            ).append(" ");
                        }

                        if (doctor.getLastName() != null) {
                            name.append(
                                    doctor.getLastName()
                            );
                        }

                        dto.setDoctorName(
                                name.toString().trim()
                        );
                    });
        }


        // -----------------------------------------------------
        // LAST MESSAGE
        // -----------------------------------------------------

        dto.setLastMessage(
                conversation.getLastMessage()
        );

        dto.setLastMessageAt(
                conversation.getLastMessageAt()
        );

        dto.setCreatedAt(
                conversation.getCreatedAt()
        );

        return dto;
    }


    // =========================================================
    // GET MESSAGES
    // =========================================================

    public List<Message> getMessages(
            String conversationId) {

        return messageRepository
                .findByConversationIdOrderBySentAtAsc(
                        conversationId
                );
    }


    // =========================================================
    // SEND MESSAGE
    // =========================================================

    public Message sendMessage(
            String conversationId,
            SendMessageRequest request) {

        if (request.getContent() == null ||
                request.getContent().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Message cannot be empty"
            );
        }


        Message message = new Message();

        message.setConversationId(conversationId);

        message.setSenderId(
                request.getSenderId()
        );

        message.setSenderRole(
                request.getSenderRole()
        );

        message.setContent(
                request.getContent().trim()
        );

        message.setSentAt(
                LocalDateTime.now()
        );

        message.setRead(false);


        // ==========================================
        // SAVE MESSAGE
        // ==========================================

        Message savedMessage =
                messageRepository.save(message);


        // ==========================================
        // UPDATE CONVERSATION
        // ==========================================

        conversationRepository
                .findById(conversationId)
                .ifPresent(conversation -> {

                    conversation.setLastMessage(
                            savedMessage.getContent()
                    );

                    conversation.setLastMessageAt(
                            savedMessage.getSentAt()
                    );

                    conversationRepository.save(
                            conversation
                    );
                });


        // ==========================================
        // BROADCAST MESSAGE
        // ==========================================

        messagingTemplate.convertAndSend(
                "/topic/conversation/" + conversationId,
                savedMessage
        );


        return savedMessage;
    }
}