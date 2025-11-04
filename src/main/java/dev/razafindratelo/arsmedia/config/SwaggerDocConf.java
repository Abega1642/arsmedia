package dev.razafindratelo.arsmedia.config;

import dev.razafindratelo.arsmedia.InfraGenerated;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@InfraGenerated
@Configuration
public class SwaggerDocConf implements WebMvcConfigurer {

  @Value("${user.dir}")
  private String projectRoot;

  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    registry.addRedirectViewController("/", "/swagger-ui/index.html");
    registry.addRedirectViewController("/doc", "/swagger-ui/index.html");
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/doc/**").addResourceLocations("file:" + projectRoot + "/doc/");
  }
}
