package kr.or.sorizieum.kiosk;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

/**
 * 키오스크 폼 입력값 DTO.
 * - consent는 체크박스가 "체크됨(true)" 이어야만 통과하도록 @AssertTrue 사용
 */
public class VisitForm {

    @NotBlank(message = "이름은 필수입니다.")
    private String name;

    @NotBlank(message = "연락처는 필수입니다.")
    private String phone;

    @NotBlank(message = "성별을 선택해주세요.")
    private String gender;

    @NotBlank(message = "나이를 선택해주세요.")
    private String ageGroup;

    @NotBlank(message = "지역은 필수입니다.")
    private String region;

    private String message;

    @AssertTrue(message = "개인정보이용 수집에 동의해야 합니다.")
    private Boolean consent;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getAgeGroup() { return ageGroup; }
    public void setAgeGroup(String ageGroup) { this.ageGroup = ageGroup; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Boolean getConsent() { return consent; }
    public void setConsent(Boolean consent) { this.consent = consent; }
}
