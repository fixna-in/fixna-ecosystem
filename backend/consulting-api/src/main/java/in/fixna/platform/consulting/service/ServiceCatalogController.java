package in.fixna.platform.consulting.service;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import in.fixna.platform.consulting.service.dto.ServiceCatalogRequest;
import in.fixna.platform.consulting.service.dto.ServiceCatalogResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/services")
@Tag(name = "services", description = "Consulting service catalog")
public class ServiceCatalogController {

    private final ServiceCatalogApplicationService serviceCatalog;

    public ServiceCatalogController(ServiceCatalogApplicationService serviceCatalog) {
        this.serviceCatalog = serviceCatalog;
    }

    @Operation(summary = "List catalog services")
    @GetMapping
    public ResponseEntity<List<ServiceCatalogResponse>> list() {
        return ResponseEntity.ok(serviceCatalog.list());
    }

    @Operation(summary = "Create catalog service")
    @PostMapping
    public ResponseEntity<ServiceCatalogResponse> create(@Valid @RequestBody ServiceCatalogRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceCatalog.create(request));
    }

    @Operation(summary = "Get catalog service by id")
    @GetMapping("/{id}")
    public ResponseEntity<ServiceCatalogResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(serviceCatalog.get(id));
    }

    @Operation(summary = "Update catalog service")
    @PutMapping("/{id}")
    public ResponseEntity<ServiceCatalogResponse> update(
            @PathVariable UUID id, @Valid @RequestBody ServiceCatalogRequest request) {
        return ResponseEntity.ok(serviceCatalog.update(id, request));
    }
}
