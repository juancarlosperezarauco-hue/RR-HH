package com.hrcoresystem.hrcore_api.modules.tenant.employees.infrastructure.api;
import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto.MyEmployeeResponses.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.usecase.*;
import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/me") public class MyEmployeeController {
 private final GetMyEmployeeProfileUseCase profile; private final GetMyEmploymentSummaryUseCase employment;
 public MyEmployeeController(GetMyEmployeeProfileUseCase p,GetMyEmploymentSummaryUseCase e){profile=p;employment=e;}
 @GetMapping("/profile") @PreAuthorize("isAuthenticated()") public ApiResponse<Profile> profile(){return ApiResponse.success("Perfil propio consultado correctamente",profile.execute());}
 @GetMapping("/employment") @PreAuthorize("isAuthenticated()") public ApiResponse<Employment> employment(){return ApiResponse.success("Situación laboral consultada correctamente",employment.execute());}
}
