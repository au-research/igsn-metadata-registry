package au.edu.ardc.registry.common.config;

import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.common.service.APILoggingService;
import au.edu.ardc.registry.common.service.KeycloakService;
import org.slf4j.MDC;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LoggerInterceptor extends HandlerInterceptorAdapter {

	private final APILoggingService loggingService;
	private final KeycloakService kcService;

	public LoggerInterceptor(APILoggingService loggingService, KeycloakService kcService) {
		this.loggingService = loggingService;
		this.kcService = kcService;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		// setup the user before the handling of the request (for logging and quick retrieval)
		try {
			User user = kcService.getLoggedInUser(request);
			request.setAttribute(String.valueOf(User.class), user);
		} catch (Exception e) {
			// no user is logged in
		}

		return super.preHandle(request, response, handler);
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {
		super.postHandle(request, response, handler, modelAndView);
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
			throws Exception {

		loggingService.log(new MultiReadHttpServletRequest(request), response);
		MDC.clear();

		super.afterCompletion(request, response, handler, ex);
	}

}
