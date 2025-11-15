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

  private static final String REDIRECT_URL = "/swagger-ui/index.html";
  private static final String DOC_URL_PATH = "/doc";
  private static final String ROOT_PATH = "/";

  @Value("${user.dir}")
  private String projectRoot;

  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    registry.addRedirectViewController(ROOT_PATH, REDIRECT_URL);
    registry.addRedirectViewController(DOC_URL_PATH, REDIRECT_URL);
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry
        .addResourceHandler(DOC_URL_PATH + "/**")
        .addResourceLocations("file:" + projectRoot + DOC_URL_PATH + ROOT_PATH);
  }
}
