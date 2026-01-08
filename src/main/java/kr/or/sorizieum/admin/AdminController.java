package kr.or.sorizieum.admin;

import kr.or.sorizieum.visit.DowCount;
import kr.or.sorizieum.visit.LabelCount;
import kr.or.sorizieum.visit.VisitEntry;
import kr.or.sorizieum.visit.VisitEntryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 관리자 대시보드 (단일 화면 /admin)
 *
 * 제공 기능
 * 1) 통계 차트 6종 (최근 12개월 기준, KST 집계)
 *   - 성별: 원형(Pie)
 *   - 연령대: 막대(Bar)
 *   - 월별: 막대
 *   - 분기별: 막대
 *   - 요일별: 막대
 *   - 평일/주말: 막대
 *
 * 2) 등록 리스트 (전체)
 *   - 10개 페이지네이션
 *   - 검색(q): 이름/지역/의견(문자 검색) + 연락처(숫자 검색)
 *   - 필터: 성별, 연령대
 *
 * 주의
 * - 로그인/권한은 다음 단계에서 적용 예정
 */
@Controller
public class AdminController {

    private final VisitEntryRepository visitEntryRepository;

    public AdminController(VisitEntryRepository visitEntryRepository) {
        this.visitEntryRepository = visitEntryRepository;
    }

    @GetMapping("/admin")
    public String dashboard(
            // 검색어(이름/연락처/지역/의견)
            @RequestParam(required = false) String q,
            // 성별 필터: MALE / FEMALE
            @RequestParam(required = false) String gender,
            // 연령대 필터: AGE_7_12 / AGE_13_15 / AGE_16_18 / AGE_19_24
            @RequestParam(required = false) String ageGroup,
            // 페이지네이션
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model
    ) {

        // ============================================================
        // 1) 차트 기준 기간: 최근 12개월 (한국시간 기준으로 월 단위 표시가 깔끔하게)
        // ============================================================
        ZoneId KST = ZoneId.of("Asia/Seoul");
        ZonedDateTime nowKst = ZonedDateTime.now(KST);

        // 이번 달 1일 00:00부터 11개월 전으로 설정 (총 12개월 포함)
        ZonedDateTime sinceKst = nowKst
                .withDayOfMonth(1)
                .truncatedTo(ChronoUnit.DAYS)
                .minusMonths(11);

        // DB created_at은 timestamptz(UTC) 기준이므로 UTC로 변환해서 비교 파라미터로 사용
        OffsetDateTime sinceUtc = sinceKst.withZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime();

        // ============================================================
        // 2) KPI
        // ============================================================
        long totalCount = visitEntryRepository.count();
        OffsetDateTime last24hSince = OffsetDateTime.now(ZoneOffset.UTC).minusHours(24);
        long last24hCount = visitEntryRepository.countByCreatedAtAfter(last24hSince);

        model.addAttribute("totalCount", totalCount);
        model.addAttribute("last24hCount", last24hCount);

        // 화면에 집계 시작일 표시용(예: 2026-01-01)
        model.addAttribute("sinceLabel", sinceKst.toLocalDate().toString());

        // ============================================================
        // 3) 차트 데이터
        // ============================================================

        // (1) 성별 분포 (Pie)
        Map<String, Long> genderMap = toMap(visitEntryRepository.genderStatsSince(sinceUtc));
        model.addAttribute("genderLabels", List.of("남", "여"));
        model.addAttribute("genderCounts", List.of(
                genderMap.getOrDefault("MALE", 0L),
                genderMap.getOrDefault("FEMALE", 0L)
        ));

        // (2) 연령대 분포 (Bar) - 원하는 순서 고정
        Map<String, Long> ageMap = toMap(visitEntryRepository.ageGroupStatsSince(sinceUtc));
        List<String> ageKeys = List.of("AGE_7_12", "AGE_13_15", "AGE_16_18", "AGE_19_24");
        List<String> ageLabels = List.of("7~12(초등)", "13~15(중등)", "16~18(고등)", "19~24(후기)");
        List<Long> ageCounts = new ArrayList<>();
        for (String k : ageKeys) {
            ageCounts.add(ageMap.getOrDefault(k, 0L));
        }
        model.addAttribute("ageLabels", ageLabels);
        model.addAttribute("ageCounts", ageCounts);

        // (3) 월별 등록 현황 (Bar)
        List<LabelCount> monthRaw = visitEntryRepository.monthlyStatsSince(sinceUtc);
        model.addAttribute("monthLabels", monthRaw.stream().map(LabelCount::getLabel).toList());
        model.addAttribute("monthCounts", monthRaw.stream().map(LabelCount::getCount).toList());

        // (4) 분기별 등록 현황 (Bar)
        List<LabelCount> quarterRaw = visitEntryRepository.quarterlyStatsSince(sinceUtc);
        model.addAttribute("quarterLabels", quarterRaw.stream().map(LabelCount::getLabel).toList());
        model.addAttribute("quarterCounts", quarterRaw.stream().map(LabelCount::getCount).toList());

        // (5) 요일별 등록 현황 (Bar) - 월~일 순서로 표시
        // Postgres extract(dow): 0=일, 1=월 ... 6=토
        List<DowCount> dowRaw = visitEntryRepository.dayOfWeekStatsSince(sinceUtc);
        long[] dowArr = new long[7]; // index: 0..6
        for (DowCount d : dowRaw) {
            if (d.getDow() == null) continue;
            int idx = d.getDow();
            if (0 <= idx && idx <= 6) {
                dowArr[idx] = (d.getCount() == null) ? 0L : d.getCount();
            }
        }
        model.addAttribute("dowLabels", List.of("월", "화", "수", "목", "금", "토", "일"));
        model.addAttribute("dowCounts", List.of(
                dowArr[1], dowArr[2], dowArr[3], dowArr[4], dowArr[5], dowArr[6], dowArr[0]
        ));

        // (6) 평일/주말 등록 현황 (Bar)
        Map<String, Long> wwMap = toMap(visitEntryRepository.weekdayWeekendStatsSince(sinceUtc));
        model.addAttribute("wwLabels", List.of("평일", "주말"));
        model.addAttribute("wwCounts", List.of(
                wwMap.getOrDefault("WEEKDAY", 0L),
                wwMap.getOrDefault("WEEKEND", 0L)
        ));

        // ============================================================
        // 4) 등록 리스트: 검색/필터/페이지네이션
        // ============================================================

        // 검색어(q) 처리
        String qTrim = (q == null) ? "" : q.trim();
        String qLike = qTrim.isBlank() ? null : ("%" + qTrim.toLowerCase() + "%");

        // 연락처 검색을 위해 숫자만 추출해서 LIKE 패턴으로 만든다 (중요: DB에서 '%' 결합 안 함)
        // 예: "010-1234" -> digits="0101234" -> pattern="%0101234%"
        String qDigitsPattern = null;
        if (!qTrim.isBlank()) {
            String digits = qTrim.replaceAll("[^0-9]", "");
            if (!digits.isBlank()) {
                qDigitsPattern = "%" + digits + "%";
            }
        }

        // 필터 파라미터 처리 (빈 문자열이면 null로)
        String genderVal = (gender == null || gender.isBlank()) ? null : gender;
        String ageVal = (ageGroup == null || ageGroup.isBlank()) ? null : ageGroup;

        // 페이지/사이즈 안전장치
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 5), 50); // 5~50 제한

        // 최신 등록이 위로 오도록 id DESC
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "id"));

        Page<VisitEntry> visits = visitEntryRepository.searchForAdmin(
                qLike,
                qDigitsPattern,
                genderVal,
                ageVal,
                pageable
        );

        // 화면에 현재 검색/필터 값 유지
        model.addAttribute("visits", visits);
        model.addAttribute("q", qTrim);
        model.addAttribute("gender", genderVal == null ? "" : genderVal);
        model.addAttribute("ageGroup", ageVal == null ? "" : ageVal);

        return "admin/dashboard";
    }

    /**
     * (label,count) 결과를 Map으로 변환
     */
    private Map<String, Long> toMap(List<LabelCount> rows) {
        Map<String, Long> map = new HashMap<>();
        for (LabelCount r : rows) {
            if (r == null) continue;
            String label = r.getLabel();
            Long count = r.getCount();
            if (label == null) continue;
            map.put(label, count == null ? 0L : count);
        }
        return map;
    }
}
