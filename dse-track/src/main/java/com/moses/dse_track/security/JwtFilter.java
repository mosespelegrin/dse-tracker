package com.moses.dse_track.security;

import com.moses.dse_track.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final JwtService jwtService;

    // Paths a mustChangePassword=true account can still reach — everything
    // else is blocked below until the password is changed.
    private static final Set<String> PASSWORD_CHANGE_ALLOWED_PATHS =
            Set.of("/auth/change-password", "/auth/logout");

    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException{
      //get header
      String authHeader=request.getHeader("Authorization");
      //if no token just let it through  may be the end point is public
        if (authHeader==null || !authHeader.startsWith("Bearer ")){
            filterChain.doFilter(request,response);
            return;
        }
        //remove prefix Bearer
        String token=authHeader.substring(7);
        //validate
        if (!jwtService.isTokenValid(token)){
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("token expired o invalid");
            return;
        }
        // Step 5: Extract userId and store in Spring Security context
        Long userId = jwtService.extractUserId(token);
        String role = jwtService.extractRole(token);

        // hasRole("ADMIN") (used by SecurityConfig) checks for a "ROLE_ADMIN"
        // authority — Spring's convention, hence the prefix here.
        List<SimpleGrantedAuthority> authorities = role != null
                ? List.of(new SimpleGrantedAuthority("ROLE_" + role))
                : List.of();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, authorities);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Account is flagged for a forced password change (e.g. the seeded
        // admin's default password) — refuse everything except changing it
        // or logging out, regardless of role or endpoint.
        if (jwtService.extractMustChangePassword(token)
                && !PASSWORD_CHANGE_ALLOWED_PATHS.contains(request.getRequestURI())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("You must change your password before continuing — PUT /auth/change-password");
            return;
        }

        // Step 6: Continue to controller
        filterChain.doFilter(request, response);


    }
}
