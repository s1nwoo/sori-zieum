package kr.or.sorizieum.visit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface VisitEntryRepository extends JpaRepository<VisitEntry, Long> {

    long countByCreatedAtAfter(OffsetDateTime since);

    /**
     * 성별 분포
     */
    @Query(value = """
        select gender as label, count(*) as count
        from visit_entry
        where created_at >= :since
        group by gender
        order by gender
        """, nativeQuery = true)
    List<LabelCount> genderStatsSince(@Param("since") OffsetDateTime since);

    /**
     * 연령대 분포
     */
    @Query(value = """
        select age_group as label, count(*) as count
        from visit_entry
        where created_at >= :since
        group by age_group
        order by age_group
        """, nativeQuery = true)
    List<LabelCount> ageGroupStatsSince(@Param("since") OffsetDateTime since);

    /**
     * 월별 등록 현황 (KST 기준 월)
     */
    @Query(value = """
        select to_char(created_at at time zone 'Asia/Seoul', 'YYYY-MM') as label, count(*) as count
        from visit_entry
        where created_at >= :since
        group by 1
        order by 1
        """, nativeQuery = true)
    List<LabelCount> monthStatsSince(@Param("since") OffsetDateTime since);

    /**
     * 분기별 등록 현황 (KST 기준)
     * label 예: 2026-Q1
     */
    @Query(value = """
        select (extract(year from created_at at time zone 'Asia/Seoul')::int || '-Q' ||
                extract(quarter from created_at at time zone 'Asia/Seoul')::int) as label,
               count(*) as count
        from visit_entry
        where created_at >= :since
        group by 1
        order by 1
        """, nativeQuery = true)
    List<LabelCount> quarterStatsSince(@Param("since") OffsetDateTime since);

    /**
     * 요일별 등록 현황 (KST 기준, 0=일 .. 6=토)
     */
    @Query(value = """
        select extract(dow from created_at at time zone 'Asia/Seoul')::int as dow, count(*) as count
        from visit_entry
        where created_at >= :since
        group by 1
        order by 1
        """, nativeQuery = true)
    List<DowCount> dayOfWeekStatsSince(@Param("since") OffsetDateTime since);

    /**
     * 평일/주말 등록 현황 (KST 기준)
     * label: WEEKDAY / WEEKEND
     */
    @Query(value = """
        select case when extract(dow from created_at at time zone 'Asia/Seoul') in (0,6)
                    then 'WEEKEND' else 'WEEKDAY' end as label,
               count(*) as count
        from visit_entry
        where created_at >= :since
        group by 1
        order by 1
        """, nativeQuery = true)
    List<LabelCount> weekdayWeekendStatsSince(@Param("since") OffsetDateTime since);

    /**
     * 지역별 등록 현황
     * label: 강서구/양천구/구로구/영등포구/기타
     */
    @Query(value = """
        select region as label, count(*) as count
        from visit_entry
        where created_at >= :since
        group by region
        order by region
        """, nativeQuery = true)
    List<LabelCount> regionStatsSince(@Param("since") OffsetDateTime since);

    /**
     * 관리자 검색/필터 목록
     */
    @Query("""
        select v from VisitEntry v
        where (
            (:qLike is null and :qDigitsPattern is null)
            or (:qLike is not null and (
                lower(v.name) like :qLike
                or lower(v.region) like :qLike
                or lower(coalesce(v.message, '')) like :qLike
            ))
            or (:qDigitsPattern is not null and v.phone like :qDigitsPattern)
        )
        and (:gender is null or v.gender = :gender)
        and (:ageGroup is null or v.ageGroup = :ageGroup)
        order by v.id desc
        """)
    Page<VisitEntry> searchForAdmin(
            @Param("qLike") String qLike,
            @Param("qDigitsPattern") String qDigitsPattern,
            @Param("gender") String gender,
            @Param("ageGroup") String ageGroup,
            Pageable pageable
    );
}
