package com.hrcoresystem.hrcore_api.common.security.jwt;
import com.hrcoresystem.hrcore_api.common.security.principal.AuthenticatedUserPrincipal; import org.springframework.security.core.authority.SimpleGrantedAuthority; import java.util.*;
public class JwtAuthorityMapper { public List<SimpleGrantedAuthority> toAuthorities(AuthenticatedUserPrincipal p){LinkedHashSet<String> values=new LinkedHashSet<>();p.roles().forEach(r->values.add("ROLE_"+r));values.addAll(p.permissions());return values.stream().map(SimpleGrantedAuthority::new).toList();} }
