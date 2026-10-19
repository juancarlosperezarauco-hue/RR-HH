package com.hrcoresystem.hrcore_api.modules.tenant.reports.infrastructure.api;
import com.hrcoresystem.hrcore_api.common.response.ApiResponse; import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.dto.ReportFilter; import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.service.HrReportExportService; import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.service.HrReportService; import java.nio.charset.StandardCharsets; import java.time.LocalDate; import java.util.*; import org.springframework.format.annotation.DateTimeFormat; import org.springframework.http.*; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/hr/reports") public class HrReportController {private final HrReportService service;private final HrReportExportService exports;public HrReportController(HrReportService s,HrReportExportService e){service=s;exports=e;}
 @GetMapping("/employees") @PreAuthorize("hasAuthority('hr.reports.employees.read')") public ApiResponse<Map<String,Object>> employees(ReportFilterInput f){return ApiResponse.success("Reporte de empleados generado correctamente",service.employees(f.filter()));}
 @GetMapping("/departments") @PreAuthorize("hasAuthority('hr.reports.departments.read')") public ApiResponse<Map<String,Object>> departments(ReportFilterInput f){return ApiResponse.success("Reporte de departamentos generado correctamente",service.departments(f.filter()));}
 @GetMapping("/positions") @PreAuthorize("hasAuthority('hr.reports.positions.read')") public ApiResponse<Map<String,Object>> positions(ReportFilterInput f){return ApiResponse.success("Reporte de cargos generado correctamente",service.positions(f.filter()));}
 @GetMapping("/contracts") @PreAuthorize("hasAuthority('hr.reports.contracts.read')") public ApiResponse<Map<String,Object>> contracts(ReportFilterInput f){return ApiResponse.success("Reporte de contratos generado correctamente",service.contracts(f.filter()));}
 @GetMapping("/attendance") @PreAuthorize("hasAuthority('hr.reports.attendance.read')") public ApiResponse<Map<String,Object>> attendance(ReportFilterInput f){return ApiResponse.success("Reporte de asistencia generado correctamente",service.attendance(f.filter()));}
 @GetMapping("/leaves") @PreAuthorize("hasAuthority('hr.reports.leaves.read')") public ApiResponse<Map<String,Object>> leaves(ReportFilterInput f){return ApiResponse.success("Reporte de permisos generado correctamente",service.leaves(f.filter()));}
 @GetMapping("/incidents") @PreAuthorize("hasAuthority('hr.reports.incidents.read')") public ApiResponse<Map<String,Object>> incidents(ReportFilterInput f){return ApiResponse.success("Reporte de incidencias generado correctamente",service.incidents(f.filter()));}
 @GetMapping("/files") @PreAuthorize("hasAuthority('hr.reports.files.read')") public ApiResponse<Map<String,Object>> files(ReportFilterInput f){return ApiResponse.success("Reporte de expedientes generado correctamente",service.files(f.filter()));}
 @GetMapping("/shifts") @PreAuthorize("hasAuthority('hr.reports.shifts.read')") public ApiResponse<Map<String,Object>> shifts(ReportFilterInput f){return ApiResponse.success("Reporte de turnos generado correctamente",service.shifts(f.filter()));}
 @GetMapping("/integrated") @PreAuthorize("hasAuthority('hr.reports.integrated.read')") public ApiResponse<Map<String,Object>> integrated(ReportFilterInput f){return ApiResponse.success("Reporte integrado generado correctamente",service.integrated(f.filter()));}
 @GetMapping(value="/{report}/export",produces="text/csv") @PreAuthorize("hasAuthority('hr.reports.export')") public ResponseEntity<byte[]> export(@PathVariable String report,ReportFilterInput f){service.assertExportAllowed(report);return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv")).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename="+report+"-report.csv").body(service.csv(report,f.filter()));}
 @GetMapping("/{report}/export/{format}") @PreAuthorize("hasAuthority('hr.reports.export')") public ResponseEntity<byte[]> exportFile(@PathVariable String report,@PathVariable String format,ReportFilterInput f){var file=exports.export(report,format,f.filter());return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.contentType())).header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(file.filename(),StandardCharsets.UTF_8).build().toString()).body(file.content());}
 public static class ReportFilterInput {
  @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) private LocalDate fromDate;
  @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) private LocalDate toDate;
  private UUID departmentId; private UUID positionId; private UUID employeeId; private UUID employmentPeriodId; private String status;
  public LocalDate getFromDate(){return fromDate;} public void setFromDate(LocalDate value){fromDate=value;}
  public LocalDate getToDate(){return toDate;} public void setToDate(LocalDate value){toDate=value;}
  public UUID getDepartmentId(){return departmentId;} public void setDepartmentId(UUID value){departmentId=value;}
  public UUID getPositionId(){return positionId;} public void setPositionId(UUID value){positionId=value;}
  public UUID getEmployeeId(){return employeeId;} public void setEmployeeId(UUID value){employeeId=value;}
  public UUID getEmploymentPeriodId(){return employmentPeriodId;} public void setEmploymentPeriodId(UUID value){employmentPeriodId=value;}
  public String getStatus(){return status;} public void setStatus(String value){status=value;}
  ReportFilter filter(){return new ReportFilter(fromDate,toDate,departmentId,positionId,employeeId,employmentPeriodId,status);}
 }
}
