package kr.or.sorizieum.visit;

/**
 * 요일(dow: 0=일, 1=월 ... 6=토) + 건수 형태 Projection
 */
public interface DowCount {
    Integer getDow();
    Long getCount();
}