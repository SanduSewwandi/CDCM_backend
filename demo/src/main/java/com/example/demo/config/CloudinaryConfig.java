package com.example.demo.config;

import com.cloudinary.Cloudinary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class CloudinaryConfig {
    @Bean
    public Cloudinary cloudinary() {
        Map<String, String> config = new HashMap<>();

        // The left side MUST be these exact strings.
        // The right side is your actual data.
        config.put("cloud_name", "dannasesv");
        config.put("api_key", "848684526161479");
        config.put("api_secret", "FkJvXXzRd7hpMc9DHJNLZSOCrPc");

        return new Cloudinary(config);
    }
}