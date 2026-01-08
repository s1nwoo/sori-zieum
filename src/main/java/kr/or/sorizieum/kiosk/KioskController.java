package kr.or.sorizieum.kiosk;

import jakarta.validation.Valid;
import kr.or.sorizieum.visit.VisitEntry;
import kr.or.sorizieum.visit.VisitEntryRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * 키오스크 화면 컨트롤러.
 * - GET: 폼 화면
 * - POST: 검증 후 DB 저장 -> 완료 화면
 */
@Controller
public class KioskController {

    private final VisitEntryRepository visitEntryRepository;

    public KioskController(VisitEntryRepository visitEntryRepository) {
        this.visitEntryRepository = visitEntryRepository;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/kiosk";
    }

    @GetMapping("/kiosk")
    public String kioskForm(Model model) {
        model.addAttribute("request", new VisitForm());
        return "kiosk/form";
    }

    @PostMapping("/kiosk")
    public String submit(
            @Valid @ModelAttribute("request") VisitForm request,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            return "kiosk/form";
        }

        // 연락처는 저장 시 숫자만 남깁니다. (010-1234-5678 -> 01012345678)
        String phoneDigits = request.getPhone().replaceAll("[^0-9]", "");

        VisitEntry entry = new VisitEntry(
                request.getName().trim(),
                phoneDigits,
                request.getGender(),
                request.getAgeGroup(),
                request.getRegion().trim(),
                request.getMessage() == null ? null : request.getMessage().trim(),
                Boolean.TRUE.equals(request.getConsent())
        );

        visitEntryRepository.save(entry);

        return "kiosk/success";
    }
}
