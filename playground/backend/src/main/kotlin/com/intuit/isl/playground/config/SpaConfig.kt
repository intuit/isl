package com.intuit.isl.playground.config

import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.resource.PathResourceResolver

/**
 * Configuration to serve the React SPA from Spring Boot static resources.
 * 
 * This configuration:
 * - Serves static files (JS, CSS, images) from /static
 * - Forwards all non-API, non-static requests to index.html for client-side routing
 * - Allows React Router to handle routing on the client side
 */
@Configuration
class SpaConfig : WebMvcConfigurer {
    
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/**")
            .addResourceLocations("classpath:/static/")
            .resourceChain(true)
            .addResolver(object : PathResourceResolver() {
                override fun getResource(resourcePath: String, location: Resource): Resource? {
                    val requestedResource = location.createRelative(resourcePath)
                    
                    // If the resource exists (e.g., JS, CSS, images), return it
                    if (requestedResource.exists() && requestedResource.isReadable) {
                        return requestedResource
                    }
                    
                    // Don't forward API requests to index.html
                    if (resourcePath.startsWith("api/")) {
                        return null
                    }
                    
                    // For all other requests (client-side routes), return index.html
                    // This allows React Router to handle routing
                    return ClassPathResource("/static/index.html")
                }
            })
    }
}

