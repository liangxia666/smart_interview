package com.smartinterview.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

public class JwtUtil {

    /**
     * 生成jwt
     */
    public static String createJWT(String secretKey, long ttlMillis, Map<String, Object> claims) {
        // 1. 生成签名需要的安全密钥
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

        // 2. 生成过期时间
        long expMillis = System.currentTimeMillis() + ttlMillis;
        Date exp = new Date(expMillis);

        // 3. 构建 JWT
        return Jwts.builder()
                .setClaims(claims)
                .signWith(key, SignatureAlgorithm.HS256) // 使用新的签名方法
                .setExpiration(exp)
                .compact();
    }

    /**
     * Token解密
     */
    public static Claims parseJWT(String secretKey, String token) {
        // 1. 生成与加密时一致的安全密钥
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

        // 2. 解析
        return Jwts.parserBuilder() // 注意：新版使用 parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}