package com.example.demo.service;

import com.example.demo.dto.ConversationResponseDTO;
import com.example.demo.dto.SendMessageRequest;
import com.example.demo.model.Appointment;
import com.example.demo.model.Conversation;
import com.example.demo.model.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.ConversationRepository;
import com.example.demo.repository.DoctorRepository;
import com.example.demo.repository.MessageRepository;
import com.example.demo.repository.PatientRepository;
import com.example.demo.repository.HospitalRepository;

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
    private HospitalRepository hospitalRepository;

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
                conversationRepository.findByAppointmentId(
                        appointmentId
                );

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


        // =====================================================
        // CONVERSATION ID
        // =====================================================

        dto.setId(
                conversation.getId()
        );


        // =====================================================
        // APPOINTMENT
        // =====================================================

        dto.setAppointmentId(
                conversation.getAppointmentId()
        );


        if (conversation.getAppointmentId() != null &&
                !conversation.getAppointmentId().trim().isEmpty()) {

            appointmentRepository
                    .findById(
                            conversation.getAppointmentId()
                    )
                    .ifPresentOrElse(

                            appointment -> {

                                // ---------------------------------
                                // APPOINTMENT NUMBER
                                // ---------------------------------

                                dto.setAppointmentNumber(
                                        appointment.getAppointmentNumber()
                                );


                                // ---------------------------------
                                // APPOINTMENT DATE
                                // ---------------------------------

                                if (appointment.getDate() != null &&
                                        !appointment.getDate().trim().isEmpty()) {

                                    dto.setAppointmentDate(
                                            appointment.getDate()
                                    );
                                }


                                // =================================
                                // HOSPITAL
                                // =================================

                                String hospitalId =
                                        appointment.getHospitalId();


                                System.out.println(
                                        "----------------------------------------"
                                );

                                System.out.println(
                                        "Conversation ID: " +
                                                conversation.getId()
                                );

                                System.out.println(
                                        "Appointment ID: " +
                                                appointment.getId()
                                );

                                System.out.println(
                                        "Hospital ID from Appointment: " +
                                                hospitalId
                                );


                                if (hospitalId != null &&
                                        !hospitalId.trim().isEmpty()) {


                                    // -----------------------------
                                    // SET HOSPITAL ID
                                    // -----------------------------

                                    dto.setHospitalId(
                                            hospitalId
                                    );


                                    // -----------------------------
                                    // FIND HOSPITAL
                                    // -----------------------------

                                    hospitalRepository
                                            .findById(
                                                    hospitalId
                                            )
                                            .ifPresentOrElse(

                                                    hospital -> {

                                                        String hospitalName =
                                                                hospital.getName();


                                                        System.out.println(
                                                                "Hospital FOUND: " +
                                                                        hospitalName
                                                        );


                                                        if (hospitalName != null &&
                                                                !hospitalName.trim().isEmpty()) {

                                                            dto.setHospitalName(
                                                                    hospitalName
                                                            );

                                                        }

                                                    },

                                                    () -> {

                                                        System.out.println(
                                                                "Hospital NOT FOUND for ID: " +
                                                                        hospitalId
                                                        );

                                                    }
                                            );


                                } else {

                                    System.out.println(
                                            "Appointment does NOT contain hospitalId."
                                    );

                                }


                                System.out.println(
                                        "----------------------------------------"
                                );

                            },

                            () -> {

                                System.out.println(
                                        "Appointment NOT FOUND for ID: " +
                                                conversation.getAppointmentId()
                                );

                            }
                    );
        }


        // =====================================================
        // PATIENT
        // =====================================================

        dto.setPatientId(
                conversation.getPatientId()
        );


        if (conversation.getPatientId() != null &&
                !conversation.getPatientId().trim().isEmpty()) {

            patientRepository
                    .findById(
                            conversation.getPatientId()
                    )
                    .ifPresent(patient -> {

                        String firstName =
                                patient.getFirstName() != null
                                        ? patient.getFirstName()
                                        : "";

                        String lastName =
                                patient.getLastName() != null
                                        ? patient.getLastName()
                                        : "";


                        String name =
                                (firstName + " " + lastName)
                                        .trim();


                        dto.setPatientName(
                                name
                        );
                    });
        }


        // =====================================================
        // DOCTOR
        // =====================================================

        dto.setDoctorId(
                conversation.getDoctorId()
        );


        if (conversation.getDoctorId() != null &&
                !conversation.getDoctorId().trim().isEmpty()) {

            doctorRepository
                    .findById(
                            conversation.getDoctorId()
                    )
                    .ifPresent(doctor -> {

                        StringBuilder name =
                                new StringBuilder();


                        if (doctor.getTitle() != null &&
                                !doctor.getTitle().trim().isEmpty()) {

                            name.append(
                                    doctor.getTitle().trim()
                            ).append(" ");
                        }


                        if (doctor.getFirstName() != null &&
                                !doctor.getFirstName().trim().isEmpty()) {

                            name.append(
                                    doctor.getFirstName().trim()
                            ).append(" ");
                        }


                        if (doctor.getLastName() != null &&
                                !doctor.getLastName().trim().isEmpty()) {

                            name.append(
                                    doctor.getLastName().trim()
                            );
                        }


                        dto.setDoctorName(
                                name.toString().trim()
                        );
                    });
        }


        // =====================================================
        // LAST MESSAGE
        // =====================================================

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


        // =====================================================
        // VALIDATE MESSAGE
        // =====================================================

        if (request.getContent() == null ||
                request.getContent().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Message cannot be empty"
            );
        }


        // =====================================================
        // CREATE MESSAGE
        // =====================================================

        Message message =
                new Message();


        message.setConversationId(
                conversationId
        );


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


        message.setRead(
                false
        );


        // =====================================================
        // SAVE MESSAGE
        // =====================================================

        Message savedMessage =
                messageRepository.save(
                        message
                );


        // =====================================================
        // UPDATE CONVERSATION
        // =====================================================

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


        // =====================================================
        // BROADCAST REALTIME MESSAGE
        // =====================================================

        messagingTemplate.convertAndSend(
                "/topic/conversation/" + conversationId,
                savedMessage
        );


        return savedMessage;
    }
}