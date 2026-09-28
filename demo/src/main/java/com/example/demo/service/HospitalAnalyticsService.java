package com.example.demo.service;

import com.example.demo.model.Appointment;
import com.example.demo.model.Doctor;
import com.example.demo.model.LabTest;
import com.example.demo.model.Schedule;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.DoctorRepository;
import com.example.demo.repository.LabTestRepository;
import com.example.demo.repository.ScheduleRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class HospitalAnalyticsService {

    private final AppointmentRepository appointmentRepository;
    private final ScheduleRepository scheduleRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorService doctorService;
    private final LabTestRepository labTestRepository;

    public HospitalAnalyticsService(AppointmentRepository appointmentRepository,
                                  ScheduleRepository scheduleRepository,
                                  DoctorRepository doctorRepository,
                                  DoctorService doctorService,
                                  LabTestRepository labTestRepository) {
        this.appointmentRepository = appointmentRepository;
        this.scheduleRepository = scheduleRepository;
        this.doctorRepository = doctorRepository;
        this.doctorService = doctorService;
        this.labTestRepository = labTestRepository;
    }

    public Map<String, Object> getHospitalAnalytics(String hospitalId) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 1. Fetch raw data
        List<Appointment> appointments = appointmentRepository.findByHospitalId(hospitalId);
        if (appointments == null) appointments = Collections.emptyList();

        List<Schedule> schedules = scheduleRepository.findByHospitalId(hospitalId);
        if (schedules == null) schedules = Collections.emptyList();

        List<Doctor> hospitalDoctors = doctorService.getDoctorsByHospital(hospitalId);
        if (hospitalDoctors == null) hospitalDoctors = Collections.emptyList();

        List<LabTest> labTests = labTestRepository.findByHospitalId(hospitalId);
        if (labTests == null) labTests = Collections.emptyList();

        // Map doctors by ID for fast lookup
        Map<String, Doctor> doctorMap = hospitalDoctors.stream()
                .filter(d -> d.getId() != null)
                .collect(Collectors.toMap(Doctor::getId, d -> d, (existing, replacement) -> existing));

        // 2. Metrics calculation
        int totalAppointments = appointments.size();
        int completedAppointments = 0;
        int confirmedAppointments = 0;
        int cancelledAppointments = 0;
        int pendingAppointments = 0;

        int physicalAppointments = 0;
        int videoAppointments = 0;

        double appointmentRevenue = 0.0;
        double physicalRevenue = 0.0;
        double videoRevenue = 0.0;

        // Grouping maps
        Map<String, Integer> statusCountMap = new LinkedHashMap<>();
        Map<String, Double> monthlyRevenueMap = new TreeMap<>();
        Map<String, Integer> monthlyAppointmentsMap = new TreeMap<>();
        Map<String, Integer> specialtyCountMap = new HashMap<>();

        // Doctor performance accumulator: doctorId -> Map of stats
        Map<String, Map<String, Object>> doctorStatsMap = new HashMap<>();

        for (Appointment appt : appointments) {
            String status = appt.getStatus() != null ? appt.getStatus().toUpperCase() : "PENDING";
            statusCountMap.put(status, statusCountMap.getOrDefault(status, 0) + 1);

            switch (status) {
                case "COMPLETED":
                    completedAppointments++;
                    break;
                case "CONFIRMED":
                case "PAID":
                    confirmedAppointments++;
                    break;
                case "CANCELLED":
                    cancelledAppointments++;
                    break;
                default:
                    pendingAppointments++;
                    break;
            }

            String cType = appt.getConsultationType() != null ? appt.getConsultationType().toUpperCase() : "PHYSICAL";
            if ("VIDEO".equals(cType)) {
                videoAppointments++;
            } else {
                physicalAppointments++;
            }

            double amt = appt.getAmount();
            // If amount is 0 but it's paid/completed, assume standard consultation fee
            if (amt <= 0 && (appt.isPaid() || "PAID".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status))) {
                amt = "VIDEO".equals(cType) ? 1000.0 : 1500.0;
            }

            if (appt.isPaid() || "PAID".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status)) {
                appointmentRevenue += amt;
                if ("VIDEO".equals(cType)) {
                    videoRevenue += amt;
                } else {
                    physicalRevenue += amt;
                }
            }

            // Monthly breakdown from appointment date (format: YYYY-MM-DD or similar)
            String apptDate = appt.getDate();
            String monthKey = extractMonthKey(apptDate);
            if (monthKey != null) {
                monthlyAppointmentsMap.put(monthKey, monthlyAppointmentsMap.getOrDefault(monthKey, 0) + 1);
                if (appt.isPaid() || "PAID".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status)) {
                    monthlyRevenueMap.put(monthKey, monthlyRevenueMap.getOrDefault(monthKey, 0.0) + amt);
                }
            }

            // Doctor stats accumulation
            String docId = appt.getDoctorId();
            if (docId != null && !docId.isBlank()) {
                Map<String, Object> dStats = doctorStatsMap.computeIfAbsent(docId, k -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("total", 0);
                    map.put("completed", 0);
                    map.put("revenue", 0.0);
                    return map;
                });

                dStats.put("total", (int) dStats.get("total") + 1);
                if ("COMPLETED".equals(status)) {
                    dStats.put("completed", (int) dStats.get("completed") + 1);
                }
                if (appt.isPaid() || "PAID".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status)) {
                    dStats.put("revenue", (double) dStats.get("revenue") + amt);
                }

                // Specialty count
                Doctor doc = doctorMap.get(docId);
                if (doc != null && doc.getSpecialization() != null && !doc.getSpecialization().isBlank()) {
                    String spec = doc.getSpecialization().trim();
                    specialtyCountMap.put(spec, specialtyCountMap.getOrDefault(spec, 0) + 1);
                }
            }
        }

        // 3. Lab Test calculations
        double labRevenue = 0.0;
        int completedLabTests = 0;
        int pendingLabTests = 0;

        for (LabTest test : labTests) {
            String testStatus = test.getStatus() != null ? test.getStatus().toLowerCase() : "pending";
            if ("completed".equals(testStatus)) {
                completedLabTests++;
            } else {
                pendingLabTests++;
            }
            if (test.isPaid()) {
                labRevenue += test.getPrice();
            }
        }

        double totalRevenue = appointmentRevenue + labRevenue;
        double cancellationRate = totalAppointments > 0 ? (cancelledAppointments * 100.0) / totalAppointments : 0.0;
        double completionRate = totalAppointments > 0 ? (completedAppointments * 100.0) / totalAppointments : 0.0;

        // 4. Build Summary Object
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalAppointments", totalAppointments);
        summary.put("completedAppointments", completedAppointments);
        summary.put("confirmedAppointments", confirmedAppointments);
        summary.put("cancelledAppointments", cancelledAppointments);
        summary.put("pendingAppointments", pendingAppointments);
        summary.put("physicalAppointments", physicalAppointments);
        summary.put("videoAppointments", videoAppointments);
        summary.put("totalRevenue", Math.round(totalRevenue * 100.0) / 100.0);
        summary.put("appointmentRevenue", Math.round(appointmentRevenue * 100.0) / 100.0);
        summary.put("physicalRevenue", Math.round(physicalRevenue * 100.0) / 100.0);
        summary.put("videoRevenue", Math.round(videoRevenue * 100.0) / 100.0);
        summary.put("labRevenue", Math.round(labRevenue * 100.0) / 100.0);
        summary.put("activeDoctorsCount", hospitalDoctors.size());
        summary.put("totalSchedules", schedules.size());
        summary.put("totalLabTests", labTests.size());
        summary.put("completedLabTests", completedLabTests);
        summary.put("pendingLabTests", pendingLabTests);
        summary.put("cancellationRate", Math.round(cancellationRate * 10.0) / 10.0);
        summary.put("completionRate", Math.round(completionRate * 10.0) / 10.0);

        result.put("summary", summary);

        // 5. Monthly trend array
        List<Map<String, Object>> monthlyTrends = new ArrayList<>();
        Set<String> allMonths = new TreeSet<>(monthlyAppointmentsMap.keySet());
        allMonths.addAll(monthlyRevenueMap.keySet());

        if (allMonths.isEmpty()) {
            // Default current month
            String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            allMonths.add(currentMonth);
        }

        for (String mKey : allMonths) {
            Map<String, Object> mItem = new LinkedHashMap<>();
            mItem.put("month", mKey);
            mItem.put("appointments", monthlyAppointmentsMap.getOrDefault(mKey, 0));
            mItem.put("revenue", Math.round(monthlyRevenueMap.getOrDefault(mKey, 0.0) * 100.0) / 100.0);
            monthlyTrends.add(mItem);
        }
        result.put("monthlyTrends", monthlyTrends);

        // 6. Consultation Type breakdown
        Map<String, Object> consultationBreakdown = new LinkedHashMap<>();
        consultationBreakdown.put("physical", physicalAppointments);
        consultationBreakdown.put("video", videoAppointments);
        consultationBreakdown.put("physicalRevenue", physicalRevenue);
        consultationBreakdown.put("videoRevenue", videoRevenue);
        result.put("consultationBreakdown", consultationBreakdown);

        // 7. Status breakdown
        result.put("statusBreakdown", statusCountMap);

        // 8. Top Performing Doctors Leaderboard
        List<Map<String, Object>> doctorLeaderboard = new ArrayList<>();
        for (Doctor doc : hospitalDoctors) {
            String dId = doc.getId();
            Map<String, Object> dStats = doctorStatsMap.getOrDefault(dId, Collections.emptyMap());

            int docAppts = (int) dStats.getOrDefault("total", 0);
            int docCompleted = (int) dStats.getOrDefault("completed", 0);
            double docRev = (double) dStats.getOrDefault("revenue", 0.0);

            Map<String, Object> docItem = new LinkedHashMap<>();
            docItem.put("doctorId", dId);
            docItem.put("doctorName", (doc.getTitle() != null ? doc.getTitle() + " " : "Dr. ") +
                    (doc.getFirstName() != null ? doc.getFirstName() : "") + " " +
                    (doc.getLastName() != null ? doc.getLastName() : "").trim());
            docItem.put("specialization", doc.getSpecialization() != null ? doc.getSpecialization() : "General Practitioner");
            docItem.put("profileImage", doc.getProfileImage());
            docItem.put("appointmentsCount", docAppts);
            docItem.put("completedCount", docCompleted);
            docItem.put("revenue", Math.round(docRev * 100.0) / 100.0);
            doctorLeaderboard.add(docItem);
        }

        // Sort doctors by total appointments descending
        doctorLeaderboard.sort((a, b) -> Integer.compare((int) b.get("appointmentsCount"), (int) a.get("appointmentsCount")));
        result.put("topDoctors", doctorLeaderboard);

        // 9. Specialty Breakdown
        List<Map<String, Object>> specialtyList = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : specialtyCountMap.entrySet()) {
            Map<String, Object> sItem = new LinkedHashMap<>();
            sItem.put("specialty", entry.getKey());
            sItem.put("count", entry.getValue());
            specialtyList.add(sItem);
        }
        specialtyList.sort((a, b) -> Integer.compare((int) b.get("count"), (int) a.get("count")));
        result.put("specialtyBreakdown", specialtyList);

        // 10. Recent Appointments (Top 10)
        List<Map<String, Object>> recentAppointments = new ArrayList<>();
        int count = 0;
        // Sort reverse order (latest first)
        List<Appointment> reversed = new ArrayList<>(appointments);
        Collections.reverse(reversed);

        for (Appointment a : reversed) {
            if (count++ >= 10) break;
            Map<String, Object> apptMap = new LinkedHashMap<>();
            apptMap.put("id", a.getId());
            apptMap.put("appointmentNumber", a.getAppointmentNumber());
            apptMap.put("patientName", a.getPatientName() != null ? a.getPatientName() : "Patient #" + a.getPatientId());
            Doctor doc = a.getDoctorId() != null ? doctorMap.get(a.getDoctorId()) : null;
            apptMap.put("doctorName", doc != null ? ((doc.getTitle() != null ? doc.getTitle() + " " : "Dr. ") + doc.getFirstName() + " " + doc.getLastName()).trim() : "Unknown Doctor");
            apptMap.put("date", a.getDate());
            apptMap.put("time", a.getTime());
            apptMap.put("consultationType", a.getConsultationType());
            apptMap.put("status", a.getStatus());
            apptMap.put("isPaid", a.isPaid());
            apptMap.put("amount", a.getAmount());
            recentAppointments.add(apptMap);
        }
        result.put("recentAppointments", recentAppointments);

        return result;
    }

    private String extractMonthKey(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            // Check if matches YYYY-MM-DD or YYYY-MM
            if (dateStr.length() >= 7 && dateStr.charAt(4) == '-') {
                return dateStr.substring(0, 7);
            }
        } catch (Exception ignored) {}
        return null;
    }
}
