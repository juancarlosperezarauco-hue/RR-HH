package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.usecase;

import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.service.TenantTimezoneService;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.service.OrganizationalReportingService;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.OrganizationalReportingRepository.OrganizationalEmployee;
import org.springframework.stereotype.Service;
import java.util.*;

@Service public class GetOrganizationChartUseCase {
    private final OrganizationalReportingService reporting; private final TenantTimezoneService timezone;
    public GetOrganizationChartUseCase(OrganizationalReportingService reporting,TenantTimezoneService timezone){this.reporting=reporting;this.timezone=timezone;}
    public OrganizationChartResponse execute(){var date=timezone.today();List<OrganizationalEmployee> employees=reporting.current(date);Map<UUID,UUID> supervisors=reporting.resolveSupervisors(employees);Map<UUID,OrganizationChartNodeResponse> nodes=new HashMap<>();
        for(OrganizationalEmployee e:employees)nodes.put(e.employeeId(),new OrganizationChartNodeResponse(e.employeeId(),e.employeeNumber(),e.fullName(),e.departmentId(),e.departmentName(),e.positionId(),e.positionName(),supervisors.get(e.employeeId()),new ArrayList<>()));
        List<OrganizationChartNodeResponse> roots=new ArrayList<>();for(OrganizationChartNodeResponse n:nodes.values()){UUID parent=n.supervisorEmployeeId();if(parent==null)roots.add(n);else{OrganizationChartNodeResponse p=nodes.get(parent);if(p==null)throw new com.hrcoresystem.hrcore_api.common.exception.BusinessConflictException("La estructura organizacional contiene relaciones que requieren regularización");((List<OrganizationChartNodeResponse>)p.children()).add(n);}}
        Comparator<OrganizationChartNodeResponse> order=Comparator.comparing(OrganizationChartNodeResponse::positionName,Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)).thenComparing(OrganizationChartNodeResponse::fullName,String.CASE_INSENSITIVE_ORDER).thenComparing(OrganizationChartNodeResponse::employeeNumber).thenComparing(OrganizationChartNodeResponse::employeeId);nodes.values().forEach(n->((List<OrganizationChartNodeResponse>)n.children()).sort(order));roots.sort(order);return new OrganizationChartResponse(date,nodes.size(),roots);
    }
}
