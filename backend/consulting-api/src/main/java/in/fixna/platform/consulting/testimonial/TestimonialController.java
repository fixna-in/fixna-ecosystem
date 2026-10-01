package in.fixna.platform.consulting.testimonial;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import in.fixna.platform.consulting.testimonial.dto.TestimonialRequest;
import in.fixna.platform.consulting.testimonial.dto.TestimonialResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/testimonials")
@Tag(name = "testimonials", description = "Tenant-scoped website testimonials")
public class TestimonialController {

    private final TestimonialApplicationService testimonialService;

    public TestimonialController(TestimonialApplicationService testimonialService) {
        this.testimonialService = testimonialService;
    }

    @Operation(summary = "List testimonials for the current tenant")
    @GetMapping
    public ResponseEntity<List<TestimonialResponse>> list() {
        return ResponseEntity.ok(testimonialService.list());
    }

    @Operation(summary = "Get testimonial by id")
    @GetMapping("/{id}")
    public ResponseEntity<TestimonialResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(testimonialService.get(id));
    }

    @Operation(summary = "Create a testimonial")
    @PostMapping
    public ResponseEntity<TestimonialResponse> create(@Valid @RequestBody TestimonialRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(testimonialService.create(request));
    }

    @Operation(summary = "Update a testimonial")
    @PutMapping("/{id}")
    public ResponseEntity<TestimonialResponse> update(
            @PathVariable UUID id, @Valid @RequestBody TestimonialRequest request) {
        return ResponseEntity.ok(testimonialService.update(id, request));
    }

    @Operation(summary = "Delete a testimonial")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        testimonialService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
