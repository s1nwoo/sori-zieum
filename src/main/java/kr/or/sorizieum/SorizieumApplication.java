package kr.or.sorizieum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 시작 클래스.
 * 이 패키지(kr.or.sorizieum) 하위가 기본 컴포넌트 스캔 대상입니다.
 */
@SpringBootApplication
public class SorizieumApplication {

    public static void main(String[] args) {
        SpringApplication.run(SorizieumApplication.class, args);
    }
}
