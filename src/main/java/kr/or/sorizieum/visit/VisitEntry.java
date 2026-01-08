package kr.or.sorizieum.visit;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * 방문(방명록) 기록 엔티티
 * - 현재 단계에서는 phone을 원문(숫자만)으로 저장합니다.
 *   개인정보 리스크를 줄이려면 다음 단계에서 해시/마스킹 저장으로 바꿀 수 있습니다.
 */
@Entity
@Table(name = "visit_entry")
public class VisitEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50, nullable = false)
    private String name;

    @Column(length = 20, nullable = false)
    private String phone; // 숫자만 저장(예: 01012345678)

    @Column(length = 20, nullable = false)
    private String gender;

    @Column(name = "age_group", length = 20, nullable = false)
    private String ageGroup;

    @Column(length = 50, nullable = false)
    private String region;

    @Column(columnDefinition = "text")
    private String message;

    @Column(nullable = false)
    private boolean consent;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected VisitEntry() {
        // JPA 기본 생성자
    }

    public VisitEntry(String name, String phone, String gender, String ageGroup, String region, String message, boolean consent) {
        this.name = name;
        this.phone = phone;
        this.gender = gender;
        this.ageGroup = ageGroup;
        this.region = region;
        this.message = message;
        this.consent = consent;
    }

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getGender() { return gender; }
    public String getAgeGroup() { return ageGroup; }
    public String getRegion() { return region; }
    public String getMessage() { return message; }
    public boolean isConsent() { return consent; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
