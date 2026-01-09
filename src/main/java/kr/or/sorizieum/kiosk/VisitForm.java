package kr.or.sorizieum.kiosk;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 키오스크 방문 등록 폼 DTO
 *
 * 요구사항 반영:
 * - 지역(region): 5개 옵션 중 하나만 허용 (강서구/양천구/구로구/영등포구/기타)
 * - consent: 필수 체크 (@AssertTrue)
 *
 * 주의:
 * - KioskController에서 request.getConsent() 를 호출하는 경우가 있으므로
 *   getConsent() 메서드를 반드시 제공합니다.
 * - boolean의 JavaBeans 규칙상 isConsent()도 함께 제공해 호환성을 높입니다.
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

    @NotBlank(message = "지역을 선택해주세요.")
    @Pattern(regexp = "^(강서구|양천구|구로구|영등포구|기타)$", message = "지역을 선택해주세요.")
    private String region;

    // 선택 입력
    private String message;

    @AssertTrue(message = "개인정보 수집·이용 동의가 필요합니다.")
    private boolean consent;

    // ----- Getter/Setter -----

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getAgeGroup() {
        return ageGroup;
    }

    public void setAgeGroup(String ageGroup) {
        this.ageGroup = ageGroup;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * KioskController에서 getConsent()를 호출하는 코드가 있으므로 반드시 유지합니다.
     */
    public boolean getConsent() {
        return consent;
    }

    /**
     * boolean 프로퍼티의 표준 getter 형태도 함께 제공합니다.
     */
    public boolean isConsent() {
        return consent;
    }

    public void setConsent(boolean consent) {
        this.consent = consent;
    }
}
