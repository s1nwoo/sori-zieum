package kr.or.sorizieum.visit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface VisitEntryRepository extends JpaRepository<VisitEntry, Long> {

    // 기존(최근 20건)
    List<VisitEntry> findTop20ByOrderByIdDesc();

    // 기존(최근 24시간 카운트)
    long countByCreatedAtAfter(OffsetDateTime time);

    /**
     * 성별 분포 (최근 N일/월 기준으로 보고 싶으면 since를 사용)
     * - created_at은 timestamptz(OffsetDateTime)로 저장되어 있으므로,
     *   KST 기준 집계를 위해 AT TIME ZONE 'Asia/Seoul' 사용
     */
    @Query(value = """
        select gender as label, count(*) as count
        from visit_entry
        where created_at >= :since
        group by gender
        order by gender
        """, nativeQuery = true)
    List<LabelCount> genderStatsSince(@Param("since") OffsetDateTime since);

    @Query(value = """
        select age_group as label, count(*) as count
        from visit_entry
        where created_at >= :since
        group by age_group
        order by age_group
        """, nativeQuery = true)
    List<LabelCount> ageGroupStatsSince(@Param("since") OffsetDateTime since);

    @Query(value = """
        select to_char(created_at at time zone 'Asia/Seoul', 'YYYY-MM') as label, count(*) as count
        from visit_entry
        where created_at >= :since
        group by 1
        order by 1
        """, nativeQuery = true)
    List<LabelCount> monthlyStatsSince(@Param("since") OffsetDateTime since);

    @Query(value = """
        select
          (to_char(created_at at time zone 'Asia/Seoul', 'YYYY') || '-' ||
           extract(quarter from created_at at time zone 'Asia/Seoul')::int || '분기') as label,
          count(*) as count
        from visit_entry
        where created_at >= :since
        group by 1
        order by 1
        """, nativeQuery = true)
    List<LabelCount> quarterlyStatsSince(@Param("since") OffsetDateTime since);

    @Query(value = """
        select extract(dow from created_at at time zone 'Asia/Seoul')::int as dow, count(*) as count
        from visit_entry
        where created_at >= :since
        group by 1
        order by 1
        """, nativeQuery = true)
    List<DowCount> dayOfWeekStatsSince(@Param("since") OffsetDateTime since);

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
        """)
    Page<VisitEntry> searchForAdmin(
            @Param("qLike") String qLike,
            @Param("qDigitsPattern") String qDigitsPattern,
            @Param("gender") String gender,
            @Param("ageGroup") String ageGroup,
            Pageable pageable
    );


}
