package com.hrcoresystem.hrcore_api.modules.platform.enterprise.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.modules.platform.enterprise.application.dto.EnterpriseInquiryRequest;
import com.hrcoresystem.hrcore_api.modules.platform.enterprise.application.service.EnterpriseInquiryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/enterprise-inquiries")
public class EnterpriseInquiryController {

  private final EnterpriseInquiryService enterpriseInquiryService;

  public EnterpriseInquiryController(EnterpriseInquiryService enterpriseInquiryService) {
    this.enterpriseInquiryService = enterpriseInquiryService;
  }

  @PostMapping
  public ApiResponse<Void> send(@Valid @RequestBody EnterpriseInquiryRequest request) {
    enterpriseInquiryService.send(request);
    return ApiResponse.success(
        "Solicitud Enterprise enviada correctamente. Nos pondremos en contacto contigo.",
        null);
  }
}
