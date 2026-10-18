package com.example.demo.service;

import com.example.demo.dto.ConversationResponseDTO;
import com.example.demo.dto.SendMessageRequest;
import com.example.demo.model.Appointment;
import com.example.demo.model.Conversation;
import com.example.demo.model.Doctor;
import com.example.demo.model.Message;
import com.example.demo.model.Patient;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.ConversationRepository;
import com.example.demo.repository.DoctorRepository;
import com.example.demo.repository.MessageRepository;
import com.example.demo.repository.PatientRepository;
import com.example.demo.repository.HospitalRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    // PARTICIPANT AUTHENTICATION & OWNERSHIP
    // =========================================================

    public static class AuthenticatedParticipant {
        private final String id;
        private final String role;

        public AuthenticatedParticipant(String id, String role) {
            this.id = id;
            this.role = role;
        }

        public String getId() {
            return id;
        }

        public String getRole() {
            return role;
        }
    }

    private AuthenticatedParticipant validateParticipant(
            Conversation conversation,
            Authentication authentication) {

        if (conversation == null) {
            throw new IllegalArgumentException("Conversation not found");
        }

        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            throw new AccessDeniedException("Authentication required to access conversation");
        }

        String email = authentication.getName();
        boolean isDoctor = authentication.getAuthorities() != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("DOCTOR") || a.getAuthority().equalsIgnoreCase("ROLE_DOCTOR"));
        boolean isPatient = authentication.getAuthorities() != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("PATIENT") || a.getAuthority().equalsIgnoreCase("ROLE_PATIENT"));

        String convDoctorId = resolveDoctorId(conversation);
        String convPatientId = resolvePatientId(conversation);

        if (isDoctor) {
            Doctor doctor = doctorRepository.findByEmail(email)
                    .orElseThrow(() -> new AccessDeniedException("Authenticated doctor not found"));
            if (convDoctorId == null || !convDoctorId.equals(doctor.getId())) {
                throw new AccessDeniedException("Access denied: Authenticated doctor is not a participant in this conversation");
            }
            return new AuthenticatedParticipant(doctor.getId(), "DOCTOR");
        } else if (isPatient) {
            Patient patient = patientRepository.findByEmail(email)
                    .orElseThrow(() -> new AccessDeniedException("Authenticated patient not found"));
            if (convPatientId == null || !convPatientId.equals(patient.getId())) {
                throw new AccessDeniedException("Access denied: Authenticated patient is not a participant in this conversation");
            }
            return new AuthenticatedParticipant(patient.getId(), "PATIENT");
        } else {
            throw new AccessDeniedException("Access denied: Role not authorized for chat");
        }
    }


    // =========================================================
    // HELPER: RESOLVE PATIENT & DOCTOR IDS
    // =========================================================

    public String resolvePatientId(Conversation c) {
        if (c == null) {
            return null;
        }
        if (c.getPatientId() != null && !c.getPatientId().trim().isEmpty()) {
            return c.getPatientId().trim();
        }
        if (c.getAppointmentId() != null && !c.getAppointmentId().trim().isEmpty()) {
            Optional<Appointment> appt = appointmentRepository.findById(c.getAppointmentId().trim());
            if (appt.isPresent() && appt.get().getPatientId() != null && !appt.get().getPatientId().trim().isEmpty()) {
                String pid = appt.get().getPatientId().trim();
                c.setPatientId(pid);
                return pid;
            }
        }
        return null;
    }

    public String resolveDoctorId(Conversation c) {
        if (c == null) {
            return null;
        }
        if (c.getDoctorId() != null && !c.getDoctorId().trim().isEmpty()) {
            return c.getDoctorId().trim();
        }
        if (c.getAppointmentId() != null && !c.getAppointmentId().trim().isEmpty()) {
            Optional<Appointment> appt = appointmentRepository.findById(c.getAppointmentId().trim());
            if (appt.isPresent() && appt.get().getDoctorId() != null && !appt.get().getDoctorId().trim().isEmpty()) {
                String did = appt.get().getDoctorId().trim();
                c.setDoctorId(did);
                return did;
            }
        }
        return null;
    }

    private String resolveGroupingPatientKey(Conversation c) {
        String pid = resolvePatientId(c);
        if (pid != null && !pid.isEmpty()) {
            return pid;
        }
        if (c.getAppointmentId() != null && !c.getAppointmentId().trim().isEmpty()) {
            Optional<Appointment> appt = appointmentRepository.findById(c.getAppointmentId().trim());
            if (appt.isPresent() && appt.get().getPatientName() != null && !appt.get().getPatientName().trim().isEmpty()) {
                return "__name__" + appt.get().getPatientName().trim().toLowerCase();
            }
        }
        return "__unknown__" + c.getId();
    }

    private String resolveGroupingDoctorKey(Conversation c) {
        String did = resolveDoctorId(c);
        if (did != null && !did.isEmpty()) {
            return did;
        }
        return "__unknown__" + c.getId();
    }


    // =========================================================
    // HELPER: FIND LATEST APPOINTMENT
    // =========================================================

    private Appointment findLatestAppointment(String patientId, String doctorId) {
        if (patientId == null || doctorId == null) {
            return null;
        }
        List<Appointment> appointments = appointmentRepository.findByPatientId(patientId.trim());
        if (appointments == null || appointments.isEmpty()) {
            return null;
        }
        return appointments.stream()
                .filter(a -> doctorId.trim().equals(a.getDoctorId()))
                .max(Comparator.comparing(
                        (Appointment a) -> a.getCreatedAt() != null ? a.getCreatedAt() : LocalDateTime.MIN
                ).thenComparing(a -> a.getId() != null ? a.getId() : ""))
                .orElse(null);
    }


    // =========================================================
    // HELPER: CANONICAL CONVERSATION (DETERMINISTIC)
    // =========================================================

    public Conversation getCanonical(List<Conversation> convs) {
        if (convs == null || convs.isEmpty()) {
            return null;
        }
        return convs.stream()
                .min(Comparator
                        .comparing((Conversation c) -> c.getCreatedAt() != null ? c.getCreatedAt() : LocalDateTime.MAX)
                        .thenComparing(c -> c.getId() != null ? c.getId() : "")
                )
                .orElse(convs.get(0));
    }


    // =========================================================
    // HELPER: BUILD UNIFIED CONVERSATION
    // =========================================================

    private Conversation buildUnifiedConversation(List<Conversation> group) {
        if (group == null || group.isEmpty()) {
            return null;
        }

        Conversation canonical = getCanonical(group);
        String patientId = resolvePatientId(canonical);
        String doctorId = resolveDoctorId(canonical);

        if (patientId == null) {
            for (Conversation c : group) {
                String pid = resolvePatientId(c);
                if (pid != null && !pid.isEmpty()) {
                    patientId = pid;
                    break;
                }
            }
        }
        if (doctorId == null) {
            for (Conversation c : group) {
                String did = resolveDoctorId(c);
                if (did != null && !did.isEmpty()) {
                    doctorId = did;
                    break;
                }
            }
        }

        // Safely determine latest appointment metadata from existing appointments without mutating canonical
        Appointment latestAppt = findLatestAppointment(patientId, doctorId);
        String activeApptId = (latestAppt != null) ? latestAppt.getId() : null;

        if (activeApptId == null) {
            activeApptId = group.stream()
                    .filter(c -> c.getAppointmentId() != null && !c.getAppointmentId().trim().isEmpty())
                    .sorted((a, b) -> {
                        LocalDateTime t1 = a.getCreatedAt() != null ? a.getCreatedAt() : LocalDateTime.MIN;
                        LocalDateTime t2 = b.getCreatedAt() != null ? b.getCreatedAt() : LocalDateTime.MIN;
                        return t2.compareTo(t1);
                    })
                    .map(Conversation::getAppointmentId)
                    .findFirst()
                    .orElse(canonical.getAppointmentId());
        }

        Conversation latestMsgConv = group.stream()
                .filter(c -> c.getLastMessageAt() != null)
                .max(Comparator.comparing(Conversation::getLastMessageAt))
                .orElse(null);

        String lastMessage = canonical.getLastMessage();
        LocalDateTime lastMessageAt = canonical.getLastMessageAt();

        if (latestMsgConv != null) {
            lastMessage = latestMsgConv.getLastMessage();
            lastMessageAt = latestMsgConv.getLastMessageAt();
        } else {
            for (Conversation c : group) {
                if (c.getLastMessage() != null && !c.getLastMessage().trim().isEmpty()) {
                    lastMessage = c.getLastMessage();
                    break;
                }
            }
        }

        Conversation unified = new Conversation();
        unified.setId(canonical.getId());
        unified.setPatientId(patientId);
        unified.setDoctorId(doctorId);
        unified.setAppointmentId(activeApptId);
        unified.setLastMessage(lastMessage);
        unified.setLastMessageAt(lastMessageAt);
        unified.setCreatedAt(canonical.getCreatedAt());

        return unified;
    }


    // =========================================================
    // CREATE CONVERSATION
    // =========================================================

    public Conversation createConversation(
            String appointmentId,
            String patientId,
            String doctorId) {

        // First check if a conversation already exists for this Doctor <-> Patient pair
        if (patientId != null && !patientId.trim().isEmpty() &&
                doctorId != null && !doctorId.trim().isEmpty()) {

            List<Conversation> existingPairConvs =
                    conversationRepository.findByPatientIdAndDoctorId(
                            patientId.trim(),
                            doctorId.trim()
                    );

            if (existingPairConvs != null && !existingPairConvs.isEmpty()) {
                // Reuse existing canonical conversation without overwriting its appointmentId
                return getCanonical(existingPairConvs);
            }
        }

        // Check if an existing conversation exists by appointmentId
        if (appointmentId != null && !appointmentId.trim().isEmpty()) {
            Optional<Conversation> existing =
                    conversationRepository.findByAppointmentId(
                            appointmentId.trim()
                    );

            if (existing.isPresent()) {
                return existing.get();
            }
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

        if (patientId == null || patientId.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<Conversation> all = conversationRepository.findByPatientId(patientId.trim());
        if (all == null || all.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, List<Conversation>> grouped = all.stream().collect(
                Collectors.groupingBy(this::resolveGroupingDoctorKey)
        );

        List<ConversationResponseDTO> dtos = new ArrayList<>();
        for (List<Conversation> group : grouped.values()) {
            Conversation unified = buildUnifiedConversation(group);
            dtos.add(convertToDTO(unified));
        }

        dtos.sort((a, b) -> {
            LocalDateTime t1 = a.getLastMessageAt() != null ? a.getLastMessageAt() : a.getCreatedAt();
            LocalDateTime t2 = b.getLastMessageAt() != null ? b.getLastMessageAt() : b.getCreatedAt();
            if (t1 == null && t2 == null) return 0;
            if (t1 == null) return 1;
            if (t2 == null) return -1;
            return t2.compareTo(t1);
        });

        return dtos;
    }


    // =========================================================
    // DOCTOR CONVERSATIONS
    // =========================================================

    public List<ConversationResponseDTO> getDoctorConversations(
            String doctorId) {

        if (doctorId == null || doctorId.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<Conversation> all = conversationRepository.findByDoctorId(doctorId.trim());
        if (all == null || all.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, List<Conversation>> grouped = all.stream().collect(
                Collectors.groupingBy(this::resolveGroupingPatientKey)
        );

        List<ConversationResponseDTO> dtos = new ArrayList<>();
        for (List<Conversation> group : grouped.values()) {
            Conversation unified = buildUnifiedConversation(group);
            dtos.add(convertToDTO(unified));
        }

        dtos.sort((a, b) -> {
            LocalDateTime t1 = a.getLastMessageAt() != null ? a.getLastMessageAt() : a.getCreatedAt();
            LocalDateTime t2 = b.getLastMessageAt() != null ? b.getLastMessageAt() : b.getCreatedAt();
            if (t1 == null && t2 == null) return 0;
            if (t1 == null) return 1;
            if (t2 == null) return -1;
            return t2.compareTo(t1);
        });

        return dtos;
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
        return getMessages(
                conversationId,
                SecurityContextHolder.getContext().getAuthentication()
        );
    }

    public List<Message> getMessages(
            String conversationId,
            Authentication authentication) {

        if (conversationId == null || conversationId.trim().isEmpty()) {
            return Collections.emptyList();
        }

        Conversation requestedConv = conversationRepository
                .findById(conversationId.trim())
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        // Enforce participant ownership
        if (authentication != null) {
            validateParticipant(requestedConv, authentication);
        } else {
            Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
            if (currentAuth != null && currentAuth.isAuthenticated()) {
                validateParticipant(requestedConv, currentAuth);
            } else {
                throw new AccessDeniedException("Authentication required to access conversation messages");
            }
        }

        String patientId = resolvePatientId(requestedConv);
        String doctorId = resolveDoctorId(requestedConv);

        List<Conversation> relatedConversations = new ArrayList<>();
        if (patientId != null && !patientId.trim().isEmpty() &&
                doctorId != null && !doctorId.trim().isEmpty()) {
            List<Conversation> pairConvs = conversationRepository.findByPatientIdAndDoctorId(
                    patientId.trim(),
                    doctorId.trim()
            );
            if (pairConvs != null) {
                relatedConversations.addAll(pairConvs);
            }
            // Also include conversations found via all appointments of this doctor-patient pair
            List<Appointment> pairAppts = appointmentRepository.findByPatientId(patientId.trim());
            if (pairAppts != null) {
                for (Appointment appt : pairAppts) {
                    if (doctorId.trim().equals(appt.getDoctorId())) {
                        conversationRepository.findByAppointmentId(appt.getId())
                                .ifPresent(c -> {
                                    if (relatedConversations.stream().noneMatch(existing -> existing.getId().equals(c.getId()))) {
                                        relatedConversations.add(c);
                                    }
                                });
                    }
                }
            }
        } else {
            relatedConversations.add(requestedConv);
        }

        List<String> convIds = new ArrayList<>();
        Map<String, String> convToApptId = new HashMap<>();

        if (relatedConversations != null) {
            for (Conversation c : relatedConversations) {
                if (c.getId() != null) {
                    convIds.add(c.getId());
                    if (c.getAppointmentId() != null && !c.getAppointmentId().trim().isEmpty()) {
                        convToApptId.put(c.getId(), c.getAppointmentId().trim());
                    }
                }
            }
        }

        if (!convIds.contains(requestedConv.getId())) {
            convIds.add(requestedConv.getId());
        }

        List<Message> messages;
        if (convIds.size() == 1) {
            messages = messageRepository.findByConversationIdOrderBySentAtAsc(convIds.get(0));
        } else {
            messages = messageRepository.findByConversationIdInOrderBySentAtAsc(convIds);
        }

        // For OLD messages that do not contain Message.appointmentId:
        // derive appointment context from the ORIGINAL historical Conversation.appointmentId.
        // Do not persist this derived value during read.
        if (messages != null) {
            for (Message msg : messages) {
                if (msg.getAppointmentId() == null || msg.getAppointmentId().trim().isEmpty()) {
                    String apptId = convToApptId.get(msg.getConversationId());
                    if (apptId != null) {
                        msg.setAppointmentId(apptId);
                    } else if (requestedConv.getAppointmentId() != null) {
                        msg.setAppointmentId(requestedConv.getAppointmentId().trim());
                    }
                }
            }
        }

        return messages != null ? messages : Collections.emptyList();
    }


    // =========================================================
    // SEND MESSAGE
    // =========================================================

    public Message sendMessage(
            String conversationId,
            SendMessageRequest request) {
        return sendMessage(
                conversationId,
                request,
                SecurityContextHolder.getContext().getAuthentication()
        );
    }

    public Message sendMessage(
            String conversationId,
            SendMessageRequest request,
            Authentication authentication) {

        // =====================================================
        // VALIDATE MESSAGE & CONVERSATION
        // =====================================================

        if (request == null || request.getContent() == null ||
                request.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Message cannot be empty"
            );
        }

        if (conversationId == null || conversationId.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Conversation ID cannot be empty"
            );
        }

        String inputConversationId = conversationId.trim();

        Conversation requestedConv = conversationRepository.findById(inputConversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        // =====================================================
        // AUTHENTICATION & PARTICIPANT VALIDATION
        // =====================================================

        AuthenticatedParticipant participant = null;
        if (authentication != null) {
            participant = validateParticipant(requestedConv, authentication);
        } else {
            Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
            if (currentAuth != null && currentAuth.isAuthenticated()) {
                participant = validateParticipant(requestedConv, currentAuth);
            }
        }

        // =====================================================
        // RESOLVE CANONICAL CONVERSATION
        // =====================================================

        String patientId = resolvePatientId(requestedConv);
        String doctorId = resolveDoctorId(requestedConv);
        Conversation targetConv = requestedConv;

        if (patientId != null && !patientId.trim().isEmpty() &&
                doctorId != null && !doctorId.trim().isEmpty()) {
            List<Conversation> allCandidateConvs = new ArrayList<>();
            List<Conversation> pairConvs =
                    conversationRepository.findByPatientIdAndDoctorId(
                            patientId.trim(),
                            doctorId.trim()
                    );
            if (pairConvs != null) {
                allCandidateConvs.addAll(pairConvs);
            }
            List<Appointment> pairAppts = appointmentRepository.findByPatientId(patientId.trim());
            if (pairAppts != null) {
                for (Appointment appt : pairAppts) {
                    if (doctorId.trim().equals(appt.getDoctorId())) {
                        conversationRepository.findByAppointmentId(appt.getId())
                                .ifPresent(c -> {
                                    if (allCandidateConvs.stream().noneMatch(existing -> existing.getId().equals(c.getId()))) {
                                        allCandidateConvs.add(c);
                                    }
                                });
                    }
                }
            }
            if (!allCandidateConvs.isEmpty()) {
                targetConv = getCanonical(allCandidateConvs);
            }
        }

        String targetConversationId = targetConv.getId();

        // =====================================================
        // CREATE MESSAGE
        // =====================================================

        Message message = new Message();
        message.setConversationId(targetConversationId);

        // PREVENT SENDER SPOOFING:
        // Use authenticated participant identity when authenticated
        if (participant != null) {
            message.setSenderId(participant.getId());
            message.setSenderRole(participant.getRole());
        } else {
            message.setSenderId(request.getSenderId());
            message.setSenderRole(request.getSenderRole());
        }

        message.setContent(request.getContent().trim());
        message.setSentAt(LocalDateTime.now());
        message.setRead(false);

        // =====================================================
        // APPOINTMENT CONTEXT PRESERVATION AND VALIDATION
        // =====================================================

        if (request.getAppointmentId() != null && !request.getAppointmentId().trim().isEmpty()) {
            String requestedApptId = request.getAppointmentId().trim();
            Appointment appt = appointmentRepository.findById(requestedApptId)
                    .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + requestedApptId));

            boolean patientMatch = appt.getPatientId() != null && appt.getPatientId().equals(patientId);
            boolean doctorMatch = appt.getDoctorId() != null && appt.getDoctorId().equals(doctorId);

            if (!patientMatch || !doctorMatch) {
                throw new IllegalArgumentException("Invalid appointmentId: appointment does not belong to this doctor-patient pair");
            }
            message.setAppointmentId(requestedApptId);
        } else {
            // Frontend did not supply appointmentId: derive latest appointment for this pair
            Appointment latestAppt = findLatestAppointment(patientId, doctorId);
            if (latestAppt != null) {
                message.setAppointmentId(latestAppt.getId());
            } else if (targetConv.getAppointmentId() != null) {
                message.setAppointmentId(targetConv.getAppointmentId().trim());
            }
        }

        // =====================================================
        // SAVE MESSAGE
        // =====================================================

        Message savedMessage = messageRepository.save(message);

        // =====================================================
        // UPDATE CONVERSATION (WITHOUT OVERWRITING APPOINTMENT ID)
        // =====================================================

        targetConv.setLastMessage(savedMessage.getContent());
        targetConv.setLastMessageAt(savedMessage.getSentAt());
        conversationRepository.save(targetConv);

        // =====================================================
        // BROADCAST REALTIME MESSAGE
        // =====================================================

        messagingTemplate.convertAndSend(
                "/topic/conversation/" + targetConversationId,
                savedMessage
        );

        if (!targetConversationId.equals(inputConversationId)) {
            messagingTemplate.convertAndSend(
                    "/topic/conversation/" + inputConversationId,
                    savedMessage
            );
        }

        return savedMessage;
    }
}