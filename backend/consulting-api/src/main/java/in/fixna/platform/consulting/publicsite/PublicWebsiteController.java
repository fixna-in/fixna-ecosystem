package in.fixna.platform.consulting.publicsite;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import in.fixna.platform.consulting.publicsite.dto.PublicWebsiteResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/public/sites")
@Tag(name = "public-website", description = "Unauthenticated public marketing sites")
public class PublicWebsiteController {

    private final PublicWebsiteService publicWebsiteService;

    public PublicWebsiteController(PublicWebsiteService publicWebsiteService) {
        this.publicWebsiteService = publicWebsiteService;
    }

    @Operation(summary = "Get published public website content by slug")
    @GetMapping("/{slug}")
    public ResponseEntity<PublicWebsiteResponse> get(@PathVariable String slug) {
        return ResponseEntity.ok(publicWebsiteService.getBySlug(slug));
    }
}
