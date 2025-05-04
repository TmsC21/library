package sk.nextIT.project.conf;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {
    @Autowired
    private AppConfig appConfig;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated()) // Ensure all requests are authenticated
                .httpBasic(Customizer.withDefaults()) // Enable HTTP Basic authentication
                .csrf(csrf -> csrf.disable()) // Disable CSRF for easier development
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // CORS configuration
                .build();
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService() {
        UserDetails user = User.withUsername(appConfig.getUsername())
                .password("{noop}" + appConfig.getPassword()) // {noop} is used to indicate no password encoder
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(user);
    }

    // CORS Configuration
    private UrlBasedCorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin("http://localhost:4200"); // Allow your frontend to access this backend
        config.addAllowedMethod("*"); // Allow all HTTP methods (GET, POST, PATCH, etc.)
        config.addAllowedHeader("*"); // Allow all headers
        config.setAllowCredentials(true); // Allow credentials (e.g., cookies, tokens)
        source.registerCorsConfiguration("/**", config); // Apply CORS to all endpoints
        return source;
    }
}
