package au.edu.ardc.registry.common.config;

import au.edu.ardc.registry.common.service.APILoggingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Autowired
	APILoggingService loggingService;

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**").allowedOrigins("*")
				.allowedMethods(HttpMethod.GET.toString(), HttpMethod.POST.toString(), HttpMethod.PUT.toString(),
						HttpMethod.DELETE.toString(), HttpMethod.OPTIONS.toString())
				.allowedHeaders("*").allowCredentials(true);
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new LoggerInterceptor(loggingService));
	}

}
