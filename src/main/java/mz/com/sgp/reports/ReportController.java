package mz.com.sgp.reports;

import java.time.LocalDate;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.*;

@RestController
@RequestMapping("/api/reports/v1")
@PreAuthorize("hasRole('MANAGER') and !hasRole('ADMIN')")
public class ReportController {
    private final ReportService service;
    public ReportController(ReportService service) { this.service = service; }
    @GetMapping("/{type}")
    public ReportService.Report report(@PathVariable String type,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(defaultValue="") String search, @RequestParam(defaultValue="") String filter,
        @RequestParam(defaultValue="") String seller, @RequestParam(defaultValue="0") int page,
        @RequestParam(defaultValue="10") int size, @RequestParam(defaultValue="false") boolean print) {
        return service.read(type, from, to, search, filter, seller, print ? 0 : page, print ? 10000 : size, print);
    }
    @GetMapping("/{type}/excel")
    public ResponseEntity<byte[]> excel(@PathVariable String type,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(defaultValue="") String search, @RequestParam(defaultValue="") String filter,
        @RequestParam(defaultValue="") String seller) throws java.io.IOException {
        var report = service.read(type, from, to, search, filter, seller, 0, 10000, true);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=relatorio-" + type + ".xlsx")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(ReportWorkbook.write(report));
    }
}
