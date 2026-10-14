package com.hrcoresystem.hrcore_api.common.security.jwt;

import com.hrcoresystem.hrcore_api.common.security.JwtProperties;
import com.hrcoresystem.hrcore_api.common.security.authorization.domain.PermissionCodeValidator;
import com.hrcoresystem.hrcore_api.common.security.principal.AuthenticatedUserPrincipal;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Service
public class JwtTokenService {
    private static final java.util.regex.Pattern ROLE_PATTERN=java.util.regex.Pattern.compile("^[A-Z][A-Z0-9_]{0,99}$");
    private final JwtProperties properties; private final SecretKey secretKey;
    public JwtTokenService(JwtProperties properties){this.properties=properties;String secret=properties.getSecret();if(!StringUtils.hasText(secret)||secret.length()<32)throw new IllegalStateException("JWT_SECRET must be configured with at least 32 characters");this.secretKey=Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));}
    public String generateAccessToken(AccessTokenPayload p){Map<String,Object> c=new LinkedHashMap<>();c.put(JwtClaimNames.SCOPE,scope(p.scope()));c.put(JwtClaimNames.TENANT,tenant(p.tenantSlug(),p.scope()));c.put(JwtClaimNames.SESSION_ID,p.sessionId().toString());c.put(JwtClaimNames.ROLES,normalizeRoles(p.roles()));c.put(JwtClaimNames.PERMISSIONS,normalizePermissions(p.permissions()));c.put(JwtClaimNames.SECURITY_VERSION,positive(p.securityVersion()));c.put(JwtClaimNames.CATALOG_VERSION,positive(p.catalogVersion()));if(JwtClaimNames.TENANT_SCOPE.equals(p.scope()))c.put(JwtClaimNames.AUTHORIZATION_VERSION,positive(Objects.requireNonNull(p.authorizationVersion())));if(p.email()!=null)c.put(JwtClaimNames.EMAIL,p.email());if(p.displayName()!=null)c.put(JwtClaimNames.DISPLAY_NAME,p.displayName());return builder(p.subjectId(),p.tokenId(),p.issuedAt(),p.expiresAt(),c,JwtClaimNames.ACCESS);}
    public String generateRefreshToken(RefreshTokenPayload p){Map<String,Object> c=Map.of(JwtClaimNames.SCOPE,scope(p.scope()),JwtClaimNames.TENANT,tenant(p.tenantSlug(),p.scope()),JwtClaimNames.SESSION_ID,p.sessionId().toString(),JwtClaimNames.REFRESH_GENERATION,positive(p.generation()));return builder(p.subjectId(),p.tokenId(),p.issuedAt(),p.expiresAt(),c,JwtClaimNames.REFRESH);}
    public String generateAccessToken(String subject,String tenantSlug,List<String> roles){return legacy(subject,null,null,tenantSlug,roles,List.of());}
    public String generateAccessToken(String subject,String email,String tenantSlug,List<String> roles){return legacy(subject,email,null,tenantSlug,roles,List.of());}
    public String generateAccessToken(String subject,String email,String displayName,String tenantSlug,List<String> roles,List<String> permissions){return legacy(subject,email,displayName,tenantSlug,roles,permissions);}
    private String legacy(String subject,String email,String displayName,String tenant,List<String> roles,List<String> permissions){Instant now=Instant.now();String scope="platform".equalsIgnoreCase(tenant)?JwtClaimNames.PLATFORM_SCOPE:JwtClaimNames.TENANT_SCOPE;return generateAccessToken(new AccessTokenPayload(UUID.fromString(subject),UUID.randomUUID(),tenant,scope,email,displayName,roles,permissions,1,scope.equals(JwtClaimNames.TENANT_SCOPE)?1L:null,1,now,now.plusMillis(properties.getAccessExpirationMs()),UUID.randomUUID()));}
    public String generateRefreshToken(String subject,String tenant){Instant now=Instant.now();String scope="platform".equalsIgnoreCase(tenant)?JwtClaimNames.PLATFORM_SCOPE:JwtClaimNames.TENANT_SCOPE;return generateRefreshToken(new RefreshTokenPayload(UUID.fromString(subject),UUID.randomUUID(),tenant,scope,1,now,now.plusMillis(properties.getRefreshExpirationMs()),UUID.randomUUID()));}
    public AuthenticatedUserPrincipal parseAccessToken(String token){Claims c=parseClaims(token);if(!JwtClaimNames.ACCESS.equals(c.get(JwtClaimNames.TOKEN_TYPE,String.class)))throw new JwtException("Invalid token type");String scope=require(c.get(JwtClaimNames.SCOPE,String.class)),tenant=require(c.get(JwtClaimNames.TENANT,String.class));UUID subject=uuid(c.getSubject()),sid=uuid(c.get(JwtClaimNames.SESSION_ID,String.class)),jti=uuid(c.getId());long sv=positiveNumber(c,JwtClaimNames.SECURITY_VERSION),cv=positiveNumber(c,JwtClaimNames.CATALOG_VERSION);Long av=JwtClaimNames.TENANT_SCOPE.equals(scope)?positiveNumber(c,JwtClaimNames.AUTHORIZATION_VERSION):null;if(JwtClaimNames.PLATFORM_SCOPE.equals(scope)&&!"platform".equalsIgnoreCase(tenant))throw new JwtException("Invalid platform tenant");return new AuthenticatedUserPrincipal(subject,sid,c.get(JwtClaimNames.EMAIL,String.class),c.get(JwtClaimNames.DISPLAY_NAME,String.class),tenant,scope,normalizeRoles(read(c,JwtClaimNames.ROLES)),normalizePermissions(read(c,JwtClaimNames.PERMISSIONS)),sv,av,cv);}
    public ParsedRefreshToken parseRefreshTokenPayload(String token){Claims c=parseClaims(token);if(!JwtClaimNames.REFRESH.equals(c.get(JwtClaimNames.TOKEN_TYPE,String.class)))throw new JwtException("Invalid refresh token type");return new ParsedRefreshToken(uuid(c.getSubject()),uuid(c.get(JwtClaimNames.SESSION_ID,String.class)),require(c.get(JwtClaimNames.TENANT,String.class)),require(c.get(JwtClaimNames.SCOPE,String.class)),positiveNumber(c,JwtClaimNames.REFRESH_GENERATION),c.getIssuedAt().toInstant(),c.getExpiration().toInstant(),uuid(c.getId()));}
    public Claims parseRefreshToken(String token){Claims c=parseClaims(token);if(!JwtClaimNames.REFRESH.equals(c.get(JwtClaimNames.TOKEN_TYPE,String.class)))throw new JwtException("Invalid refresh token type");return c;}
    private String builder(UUID sub,UUID jti,Instant i,Instant e,Map<String,Object> claims,String type){Map<String,Object> all=new LinkedHashMap<>(claims);all.put(JwtClaimNames.TOKEN_TYPE,type);return Jwts.builder().setSubject(sub.toString()).setId(jti.toString()).addClaims(all).setIssuedAt(Date.from(i)).setExpiration(Date.from(e)).signWith(secretKey).compact();}
    private Claims parseClaims(String token){return Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token).getBody();}
    private UUID uuid(Object v){try{return UUID.fromString(require(v instanceof String s?s:null));}catch(Exception e){throw new JwtException("Invalid UUID claim");}}
    private String require(String v){if(!StringUtils.hasText(v))throw new JwtException("Missing claim");return v.trim();}
    private String scope(String s){if(!JwtClaimNames.TENANT_SCOPE.equals(s)&&!JwtClaimNames.PLATFORM_SCOPE.equals(s))throw new JwtException("Invalid scope");return s;}
    private String tenant(String t,String s){return JwtClaimNames.PLATFORM_SCOPE.equals(s)?"platform":require(t);}
    private long positive(long n){if(n<1)throw new JwtException("Invalid version");return n;}
    private long positiveNumber(Claims c,String key){Object v=c.get(key);if(!(v instanceof Number n)||n.longValue()<1)throw new JwtException("Invalid claim "+key);return n.longValue();}
    private List<String> read(Claims c,String k){Object v=c.get(k);if(v==null)return List.of();if(!(v instanceof List<?> l)||l.stream().anyMatch(x->!(x instanceof String)))throw new JwtException("Invalid claim "+k);return l.stream().map(Object::toString).toList();}
    private List<String> normalizeRoles(List<String> in){LinkedHashSet<String> out=new LinkedHashSet<>();for(String r:in){if(!StringUtils.hasText(r))throw new JwtException("Invalid role");String x=r.trim();if(x.startsWith("ROLE_")||!ROLE_PATTERN.matcher(x).matches())throw new JwtException("Invalid role");out.add(x);}return List.copyOf(out);}
    private List<String> normalizePermissions(List<String> in){LinkedHashSet<String> out=new LinkedHashSet<>();for(String p:in){if(!PermissionCodeValidator.isValid(p))throw new JwtException("Invalid permission");out.add(p.trim());}return List.copyOf(out);}
}
