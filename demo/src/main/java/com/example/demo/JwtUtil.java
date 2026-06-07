package com.example.demo;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import java.util.Date;

public class JwtUtil {

    private static final String SECRET = "secret-key-for-ksleep-brand-1234567890";
    private static final String ISSUER = "KSleep";
    
    // 3 days in milliseconds = 3 * 24 * 60 * 60 * 1000 = 259,200,000 ms
    private static final long EXPIRATION_TIME = 259200000L; 

    public static String generateToken(String email) {
        return generateToken(email, "CUSTOMER");
    }

    public static String generateToken(String email, String role) {
        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(email)
                .withClaim("role", role)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .sign(Algorithm.HMAC256(SECRET));
    }

    public static String validateTokenAndGetEmail(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(SECRET);
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(ISSUER)
                    .build();
            DecodedJWT jwt = verifier.verify(token);
            return jwt.getSubject();
        } catch (Exception e) {
            return null; // Token is invalid or expired
        }
    }

    public static String getRoleFromToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(SECRET);
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(ISSUER)
                    .build();
            DecodedJWT jwt = verifier.verify(token);
            return jwt.getClaim("role").asString();
        } catch (Exception e) {
            return null; // Token is invalid, expired, or doesn't have role
        }
    }
}
