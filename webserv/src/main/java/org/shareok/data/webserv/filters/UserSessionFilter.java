/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shareok.data.webserv.filters;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.shareok.data.config.ShareokdataManager;
import org.shareok.data.kernel.api.services.user.RedisUserService;
import org.shareok.data.redis.RedisUser;
import org.shareok.data.webserv.exceptions.UserRegisterInfoNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.session.ExpiringSession;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
/**
 *
 * @author Tao Zhao
 */
public class UserSessionFilter implements Filter{
    
    @Autowired
    private RedisUserService redisUserService;
    
    @Override
    public void destroy() {
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String contextPath = httpRequest.getServletPath();
            if(contextPath.contains("/")){
                contextPath = contextPath.split("/")[1];
            }//.getContextPath();
            if(null != contextPath && !"".equals(contextPath) && ShareokdataManager.requiredUserAuthentication(contextPath)){
                SessionRepository<Session> repo = (SessionRepository<Session>) httpRequest.getAttribute(SessionRepository.class.getName());
                
                if(contextPath.equals("register")){                    
                    String email = (String) httpRequest.getAttribute("email");
                    String password = (String) httpRequest.getAttribute("password");
                    if(null == email || "".equals(email)){
                        throw new UserRegisterInfoNotFoundException("Valid email register information is required!");
                    }
                    if(null == password || "".equals(password)){
                        throw new UserRegisterInfoNotFoundException("Valid password is required for registration!");
                    }
                    /*****************
                     * Some password validation logic here:
                     */                    
                    ExpiringSession session = (ExpiringSession) repo.createSession();
                    session.setMaxInactiveIntervalInSeconds(600);
                    RedisUser user = redisUserService.findUserByUserEmail(email);
                    if(null != user){
                        session.setAttribute(ShareokdataManager.getSessionRedisUserAttributeName(), user);
                        user.setSessionKey(session.getId());
                        redisUserService.updateUser(user);
                    }
                    else{
                        ApplicationContext context = new ClassPathXmlApplicationContext("redisContext.xml");
                        user = (RedisUser) context.getBean("redisUser");

                        user.setEmail(email);
                        user.setPassword(password);
                        user.setSessionKey(session.getId());
                        redisUserService.addUser(user);
                    }
                    
                    session.setAttribute(ShareokdataManager.getSessionRedisUserAttributeName(), user);
                    repo.save(session);
                    
                }
                else{
                    boolean sessionValidated = false;
                    HttpSession session = (HttpSession) httpRequest.getSession(false);
                    if(null != session){
                        ExpiringSession exSession = (ExpiringSession) repo.getSession(session.getId());
                        if(null != exSession){
                            RedisUser user = (RedisUser) session.getAttribute(ShareokdataManager.getSessionRedisUserAttributeName());
                            if(null != user){
                                RedisUser userPersisted = redisUserService.findUserByUserEmail(user.getEmail());
                                if(null != userPersisted){
                                    sessionValidated = true;
                                }
                            }
                        }
                    }
                    
                    if(!sessionValidated){
                        if(null != session){
                            repo.delete(session.getId());
                            session.setAttribute(ShareokdataManager.getSessionRedisUserAttributeName(), null);
                            session.invalidate();
                        }
                        httpRequest.logout();
//                        request.getRequestDispatcher("/WEB-INF/jsp/logout.jsp").forward(request, response);
                    }
                }
            }
            chain.doFilter(request, response);
        } catch (IOException ex) {
                request.setAttribute("errorMessage", ex);
                request.getRequestDispatcher("/WEB-INF/jsp/userError.jsp").forward(request, response);
        } catch (ServletException ex) {
            request.setAttribute("errorMessage", ex);
            request.getRequestDispatcher("/WEB-INF/jsp/userError.jsp").forward(request, response);
        } catch (UserRegisterInfoNotFoundException ex) {
            request.setAttribute("errorMessage", ex);
            request.getRequestDispatcher("/WEB-INF/jsp/userError.jsp").forward(request, response);
        }

    }
}
