package com.hrcoresystem.hrcore_api.modules.identity.access.application.catalog;

import com.hrcoresystem.hrcore_api.common.security.authorization.domain.PermissionCodeValidator;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.model.PermissionDefinition;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.model.PermissionScope;
import java.util.ArrayList;
import java.util.List;

public final class PermissionCatalogRegistry {
    private PermissionCatalogRegistry() {}
    public static List<PermissionDefinition> definitions() {
        List<PermissionDefinition> list = new ArrayList<>();
        add(list,"access","permissions.read","roles.read","roles.create","roles.detail","roles.update","roles.activate","roles.deactivate","users.roles.read","users.roles.assign");
        add(list,"users","list","create","detail","update","activate","deactivate");
        add(list,"departments","create","list","detail","update","activate","deactivate","history.read");
        add(list,"positions","create","list","detail","update","activate","deactivate","requirements.list","requirements.create","requirements.update","requirements.delete");
        add(list,"employees","create","list","detail","update","status.update","assignments.create","assignments.list","assignments.detail","assignments.update","assignments.close","contacts.create","contacts.list","contacts.detail","contacts.update","contacts.delete");
        add(list,"contracts","create","list","detail","update","status.update","renew","history.read","document.download");
        add(list,"employee_files","create","list","detail","update","documents.create","documents.list","documents.detail","documents.update","documents.verify","documents.upload","documents.download","documents.confidential.read");
        add(list,"shifts","create","list","detail","update","activate","deactivate","breaks.create","breaks.list","breaks.detail","breaks.update","breaks.delete","assignments.create","assignments.list","assignments.detail","assignments.update","assignments.cancel","assignments.close","calendar.create","calendar.list","calendar.detail","calendar.update","calendar.delete","schedule.read");
        add(list,"attendance","records.list","records.detail","records.calculate","punches.create","punches.manage","punches.list","punches.detail","adjustments.create","adjustments.list","adjustments.detail","adjustments.approve","adjustments.reject","adjustments.cancel","overtime.create","overtime.list","overtime.detail","overtime.approve","overtime.reject","overtime.cancel");
        add(list,"leaves","types.create","types.list","types.detail","types.update","types.activate","types.deactivate","balances.list","balances.detail","balances.create","balances.adjust","requests.create","requests.list","requests.detail","requests.update","requests.submit","requests.approve","requests.reject","requests.cancel","history.read","attachments.upload","attachments.list","attachments.download");
        add(list,"audit","events.read"); add(list,"notifications","templates.read","templates.detail","deliveries.read");
        list.add(new PermissionDefinition("tenant-settings.read","tenantsettings","tenant settings read",PermissionScope.TENANT,true,true));
        list.add(new PermissionDefinition("tenant-settings.update","tenantsettings","tenant settings update",PermissionScope.TENANT,false,true));
        list.add(new PermissionDefinition("backups.create","backups","backups create",PermissionScope.TENANT,true,true));
        list.add(new PermissionDefinition("backups.list","backups","backups list",PermissionScope.TENANT,true,true));
        list.add(new PermissionDefinition("backups.detail","backups","backups detail",PermissionScope.TENANT,true,true));
        list.add(new PermissionDefinition("backups.download","backups","backups download",PermissionScope.TENANT,true,true));
        list.add(new PermissionDefinition("backups.restore","backups","backups restore",PermissionScope.TENANT,false,true));
        list.add(new PermissionDefinition("billing.subscription.manage","billing","billing subscription manage",PermissionScope.TENANT,false,true));
        list.add(new PermissionDefinition("dashboard.tenant.summary.read","dashboard","dashboard tenant summary read",PermissionScope.TENANT,true,true));
        list = list.stream().map(p -> p.module().equals("access") || List.of("billing.subscription.manage","backups.restore","tenant-settings.update").contains(p.code()) ? new PermissionDefinition(p.code(),p.module(),p.description(),p.scope(),false,p.active()) : p).toList();
        validate(list);
        return List.copyOf(list);
    }
    private static void add(List<PermissionDefinition> list,String module,String... codes){ for(String code:codes) list.add(new PermissionDefinition(module+"."+code,module,module+" "+code,PermissionScope.TENANT,!module.equals("access"),true)); }
    private static void validate(List<PermissionDefinition> list){
        if(list.size()!=144 || list.stream().map(PermissionDefinition::code).distinct().count()!=144) throw new IllegalStateException("Permission catalog must contain 144 unique definitions");
        if(list.stream().filter(p->!p.customRoleAssignable()).count()!=12 || list.stream().filter(PermissionDefinition::customRoleAssignable).count()!=132) throw new IllegalStateException("Permission catalog classification is invalid");
        list.forEach(p->{ PermissionCodeValidator.requireValid(p.code()); if(p.scope()!=PermissionScope.TENANT||!p.active()||p.description()==null||p.description().isBlank()) throw new IllegalStateException("Invalid permission definition: "+p.code()); });
        if(list.stream().anyMatch(p->p.code().equals("employee_files.confidential.read")) || list.stream().noneMatch(p->p.code().equals("employee_files.documents.confidential.read"))) throw new IllegalStateException("Confidential permission is invalid");
    }
}
