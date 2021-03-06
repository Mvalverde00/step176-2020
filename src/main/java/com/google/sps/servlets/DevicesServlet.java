package com.google.sps.servlets;

import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.appengine.api.datastore.PreparedQuery.TooManyResultsException;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.sps.data.ChromeOSDevice;
import com.google.sps.gson.Json;
import com.google.sps.servlets.Util;
import java.io.IOException;
import java.security.GeneralSecurityException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/devices")
public class DevicesServlet extends HttpServlet {

  private UserService userService = UserServiceFactory.getUserService();
  private Util utilObj = new Util();
  public final String LOGIN_URL = "/login";
  public final String AUTHORIZE_URL = "/authorize.html";
  public final String MAX_DEVICES_COUNT_PARAMETER_NAME = "maxDeviceCount";
  public final String PAGE_TOKEN_PARAMETER_NAME = "pageToken";

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    final User currentUser = userService.getCurrentUser();
    if ((!userService.isUserLoggedIn()) || (currentUser == null)) {
      response.sendRedirect(LOGIN_URL);
      return;
    }
    final String userId = currentUser.getUserId();
    final String maxDeviceCount = request.getParameter(MAX_DEVICES_COUNT_PARAMETER_NAME);
    final String pageToken = request.getParameter(PAGE_TOKEN_PARAMETER_NAME);
    try {
      final String devicePageResponse = utilObj.getNextResponse(userId, maxDeviceCount, pageToken);
      response.setContentType("application/json");
      response.getWriter().println(devicePageResponse);
    } catch (IOException e) {//something went wrong during getting the devices
        response.sendRedirect(AUTHORIZE_URL);
    } catch (TooManyResultsException e) {
        response.sendRedirect(LOGIN_URL);//TODO: Return proper error message
    }
  }

  public void setUserService(UserService newUserService) {
    this.userService = newUserService;
  }

  public void setUtilObj(Util util) {
    this.utilObj = util;
  }

}
