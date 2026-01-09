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

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
public class AdminController {

    private final VisitEntryRepository visitEntryRepository;

    public AdminController(VisitEntryRepository visitEntryRepository) {
        this.visitEntryRepository = visitEntryRepository;
    }

    @GetMapping("/admin")
    public String dashboard(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String ageGroup,
            Model model
    ) {
        // ---------- 기간(최근 30일, KST 기준 00:00부터) ----------
        ZoneId KST = ZoneId.of("Asia/Seoul");
        ZonedDateTime nowKst = ZonedDateTime.now(KST);
        ZonedDateTime sinceKst = nowKst.minusDays(30).truncatedTo(ChronoUnit.DAYS);
        OffsetDateTime sinceUtc = sinceKst.withZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime();

        model.addAttribute("sinceLabel", sinceKst.toLocalDate().toString());

        // ---------- KPI ----------
        long totalCount = visitEntryRepository.count();
        OffsetDateTime last24hSince = OffsetDateTime.now(ZoneOffset.UTC).minusHours(24);
        long last24hCount = visitEntryRepository.countByCreatedAtAfter(last24hSince);

        model.addAttribute("totalCount", totalCount);
        model.addAttribute("last24hCount", last24hCount);

        // ---------- 차트: 성별 ----------
        Map<String, Long> genderMap = toMap(visitEntryRepository.genderStatsSince(sinceUtc));
        model.addAttribute("genderLabels", List.of("남", "여"));
        model.addAttribute("genderCounts", List.of(
                genderMap.getOrDefault("MALE", 0L),
                genderMap.getOrDefault("FEMALE", 0L)
        ));

        // ---------- 차트: 연령대 ----------
        Map<String, Long> ageMap = toMap(visitEntryRepository.ageGroupStatsSince(sinceUtc));
        model.addAttribute("ageLabels", List.of("7~12(초등)", "13~15(중등)", "16~18(고등)", "19~24(후기)"));
        model.addAttribute("ageCounts", List.of(
                ageMap.getOrDefault("AGE_7_12", 0L),
                ageMap.getOrDefault("AGE_13_15", 0L),
                ageMap.getOrDefault("AGE_16_18", 0L),
                ageMap.getOrDefault("AGE_19_24", 0L)
        ));

        // ---------- 차트: 월별 (개선: 데이터 없는 월도 모두 표시) ----------
        List<LabelCount> monthRows = visitEntryRepository.monthStatsSince(sinceUtc);
        Map<String, Long> monthMap = toMap(monthRows);

        // 최근 12개월 레이블 생성
        List<String> allMonthLabels = new ArrayList<>();
        List<Long> allMonthCounts = new ArrayList<>();

        for (int i = 11; i >= 0; i--) {
            ZonedDateTime targetMonth = nowKst.minusMonths(i);
            String monthLabel = String.format("%d-%02d",
                    targetMonth.getYear(),
                    targetMonth.getMonthValue());
            allMonthLabels.add(monthLabel);
            allMonthCounts.add(monthMap.getOrDefault(monthLabel, 0L));
        }

        model.addAttribute("monthLabels", allMonthLabels);
        model.addAttribute("monthCounts", allMonthCounts);

        // ---------- 차트: 분기별 (개선: 데이터 없는 분기도 모두 표시) ----------
        List<LabelCount> quarterRows = visitEntryRepository.quarterStatsSince(sinceUtc);
        Map<String, Long> quarterMap = new HashMap<>();
        for (LabelCount r : quarterRows) {
            if (r == null || r.getLabel() == null) continue;
            quarterMap.put(r.getLabel(), r.getCount() == null ? 0L : r.getCount());
        }

        // 최근 4분기 레이블 생성
        List<String> allQuarterLabels = new ArrayList<>();
        List<Long> allQuarterCounts = new ArrayList<>();

        for (int i = 3; i >= 0; i--) {
            ZonedDateTime targetQuarter = nowKst.minusMonths(i * 3);
            int year = targetQuarter.getYear();
            int quarter = (targetQuarter.getMonthValue() - 1) / 3 + 1;
            String quarterKey = year + "-Q" + quarter;
            String quarterLabel = year + "-" + quarter + "분기";

            allQuarterLabels.add(quarterLabel);
            allQuarterCounts.add(quarterMap.getOrDefault(quarterKey, 0L));
        }

        model.addAttribute("quarterLabels", allQuarterLabels);
        model.addAttribute("quarterCounts", allQuarterCounts);

        // ---------- 차트: 요일별 ----------
        Map<Integer, Long> dowMap = new HashMap<>();
        for (DowCount r : visitEntryRepository.dayOfWeekStatsSince(sinceUtc)) {
            if (r == null || r.getDow() == null) continue;
            dowMap.put(r.getDow(), r.getCount() == null ? 0L : r.getCount());
        }
        model.addAttribute("dowLabels", List.of("일", "월", "화", "수", "목", "금", "토"));
        model.addAttribute("dowCounts", List.of(
                dowMap.getOrDefault(0, 0L),
                dowMap.getOrDefault(1, 0L),
                dowMap.getOrDefault(2, 0L),
                dowMap.getOrDefault(3, 0L),
                dowMap.getOrDefault(4, 0L),
                dowMap.getOrDefault(5, 0L),
                dowMap.getOrDefault(6, 0L)
        ));

        // ---------- 차트: 평일/주말 ----------
        Map<String, Long> wwMap = toMap(visitEntryRepository.weekdayWeekendStatsSince(sinceUtc));
        model.addAttribute("wwLabels", List.of("평일", "주말"));
        model.addAttribute("wwCounts", List.of(
                wwMap.getOrDefault("WEEKDAY", 0L),
                wwMap.getOrDefault("WEEKEND", 0L)
        ));

        // ---------- 차트: 지역별 ----------
        Map<String, Long> regionMap = toMap(visitEntryRepository.regionStatsSince(sinceUtc));
        List<String> regionLabels = List.of("강서구", "양천구", "구로구", "영등포구", "기타");
        model.addAttribute("regionLabels", regionLabels);
        model.addAttribute("regionCounts", List.of(
                regionMap.getOrDefault("강서구", 0L),
                regionMap.getOrDefault("양천구", 0L),
                regionMap.getOrDefault("구로구", 0L),
                regionMap.getOrDefault("영등포구", 0L),
                regionMap.getOrDefault("기타", 0L)
        ));

        // ---------- 리스트(검색/필터/페이지네이션) ----------
        String qVal = normalizeBlankToNull(q);
        String genderVal = normalizeBlankToNull(gender);
        String ageVal = normalizeBlankToNull(ageGroup);

        String qLike = null;
        String qDigitsPattern = null;

        if (qVal != null) {
            String trimmed = qVal.trim();
            qLike = "%" + trimmed.toLowerCase() + "%";

            String digits = trimmed.replaceAll("\\D", "");
            if (!digits.isEmpty()) {
                qDigitsPattern = "%" + digits + "%";
            }
        }

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<VisitEntry> visits = visitEntryRepository.searchForAdmin(qLike, qDigitsPattern, genderVal, ageVal, pageable);

        model.addAttribute("visits", visits);
        model.addAttribute("q", qVal == null ? "" : qVal);
        model.addAttribute("gender", genderVal == null ? "" : genderVal);
        model.addAttribute("ageGroup", ageVal == null ? "" : ageVal);

        return "admin/dashboard";
    }

    private String normalizeBlankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private Map<String, Long> toMap(List<LabelCount> rows) {
        Map<String, Long> map = new HashMap<>();
        for (LabelCount r : rows) {
            if (r == null || r.getLabel() == null) continue;
            map.put(r.getLabel(), r.getCount() == null ? 0L : r.getCount());
        }
        return map;
    }

    private List<String> extractLabels(List<LabelCount> rows, java.util.function.Function<String, String> mapper) {
        List<String> labels = new ArrayList<>();
        for (LabelCount r : rows) {
            labels.add(mapper.apply(r.getLabel()));
        }
        return labels;
    }

    private List<Long> extractCounts(List<LabelCount> rows) {
        List<Long> counts = new ArrayList<>();
        for (LabelCount r : rows) {
            counts.add(r.getCount() == null ? 0L : r.getCount());
        }
        return counts;
    }

    private String toKoreanQuarter(String label) {
        // "2026-Q1" -> "2026-1분기"
        if (label == null) return "";
        Pattern p = Pattern.compile("^(\\d{4})-Q(\\d)$");
        Matcher m = p.matcher(label);
        if (!m.find()) return label;
        return m.group(1) + "-" + m.group(2) + "분기";
    }
}