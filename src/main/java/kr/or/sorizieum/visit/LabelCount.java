package kr.or.sorizieum.visit;

/**
 * (라벨, 건수) 형태의 통계 결과를 받기 위한 Projection 인터페이스
 */
public interface LabelCount {
    String getLabel();
    Long getCount();
}
