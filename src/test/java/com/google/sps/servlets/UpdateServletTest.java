package com.google.sps.servlets;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.sps.data.ChromeOSDevice;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.sps.gson.Json;
import com.google.sps.servlets.Util;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import org.json.simple.JSONArray;
import java.io.IOException;
import java.lang.reflect.Type;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

/*
 * Test the Update servlet.
 */
@RunWith(JUnit4.class)
public final class UpdateServletTest {

  private UpdateServlet servlet = new UpdateServlet();
  private HttpServletRequest request = mock(HttpServletRequest.class);
  private HttpServletResponse response = mock(HttpServletResponse.class);

  private final String TEST_USER_ID = "testUserId";
  private final String TEST_USER_EMAIL = "testEmail";
  private final String TEST_USER_AUTH_DOMAIN = "testAuthDomain";
  private final String TEST_ACCESS_TOKEN = "testAccessToken";

  private UserService mockedUserService;
  private Util mockedUtil;
  private User userFake;
  
  @Before
  public void setUp() {
    mockedUserService = mock(UserService.class);
    mockedUtil = mock(Util.class);
    userFake = new User(TEST_USER_EMAIL, TEST_USER_AUTH_DOMAIN, TEST_USER_ID);
  }

  @Test
  public void userNotLoggedIn() throws IOException {
    when(mockedUserService.isUserLoggedIn()).thenReturn(false);
    when(mockedUserService.getCurrentUser()).thenReturn(userFake);

    servlet.setUserService(mockedUserService);
    servlet.doPost(request, response);

    verify(response).sendRedirect(servlet.LOGIN_URL);
    verify(mockedUserService, times(1)).isUserLoggedIn();
  }

  @Test
  public void noFieldsToUpdate() throws IOException {
    when(mockedUserService.isUserLoggedIn()).thenReturn(true);
    when(mockedUserService.getCurrentUser()).thenReturn(userFake);

    when(request.getParameter("annotatedLocation")).thenReturn(null);
    when(request.getParameter("annotatedAssetId")).thenReturn(null);
    when(request.getParameter("annotatedUser")).thenReturn(null);
    when(request.getParameter(servlet.DEVICE_IDS_PARAMETER_NAME)).thenReturn("[device1,device2,device3]");

    servlet.setUserService(mockedUserService);
    servlet.setUtilObj(mockedUtil);

    servlet.doPost(request, response);

    verify(response).sendRedirect(servlet.INDEX_URL);
    verify(mockedUserService, times(1)).isUserLoggedIn();
    verify(request, times(1)).getParameter("annotatedLocation");
    verify(request, times(1)).getParameter("annotatedAssetId");
    verify(request, times(1)).getParameter("annotatedUser");
    verify(request, times(1)).getParameter(servlet.DEVICE_IDS_PARAMETER_NAME);
    verify(mockedUtil, times(1)).updateDevices(TEST_USER_ID, Arrays.asList("device1", "device2", "device3"), "{}");
  }

  @Test
  public void noDevicesToUpdate() throws IOException {
    when(mockedUserService.isUserLoggedIn()).thenReturn(true);
    when(mockedUserService.getCurrentUser()).thenReturn(userFake);

    when(request.getParameter("annotatedLocation")).thenReturn("Chicago");
    when(request.getParameter("annotatedAssetId")).thenReturn(null);
    when(request.getParameter("annotatedUser")).thenReturn(null);
    when(request.getParameter(servlet.DEVICE_IDS_PARAMETER_NAME)).thenReturn("[]");

    servlet.setUserService(mockedUserService);
    servlet.setUtilObj(mockedUtil);

    servlet.doPost(request, response);

    verify(response).sendRedirect(servlet.INDEX_URL);
    verify(mockedUserService, times(1)).isUserLoggedIn();
    verify(request, times(1)).getParameter("annotatedLocation");
    verify(request, times(1)).getParameter("annotatedAssetId");
    verify(request, times(1)).getParameter("annotatedUser");
    verify(request, times(1)).getParameter(servlet.DEVICE_IDS_PARAMETER_NAME);
    verify(mockedUtil, times(1)).updateDevices(TEST_USER_ID, Arrays.asList(), "{\"annotatedLocation\":\"Chicago\"}");
  }

  @Test
  public void oneDeviceToUpdate() throws IOException {
    when(mockedUserService.isUserLoggedIn()).thenReturn(true);
    when(mockedUserService.getCurrentUser()).thenReturn(userFake);

    when(request.getParameter("annotatedLocation")).thenReturn(null);
    when(request.getParameter("annotatedAssetId")).thenReturn(null);
    when(request.getParameter("annotatedUser")).thenReturn("bob");
    when(request.getParameter(servlet.DEVICE_IDS_PARAMETER_NAME)).thenReturn("[device1]");

    servlet.setUserService(mockedUserService);
    servlet.setUtilObj(mockedUtil);

    servlet.doPost(request, response);

    verify(response).sendRedirect(servlet.INDEX_URL);
    verify(mockedUserService, times(1)).isUserLoggedIn();
    verify(request, times(1)).getParameter("annotatedLocation");
    verify(request, times(1)).getParameter("annotatedAssetId");
    verify(request, times(1)).getParameter("annotatedUser");
    verify(request, times(1)).getParameter(servlet.DEVICE_IDS_PARAMETER_NAME);
    verify(mockedUtil, times(1)).updateDevices(TEST_USER_ID, Arrays.asList("device1"), "{\"annotatedUser\":\"bob\"}");
  }

  @Test
  public void testSeveralDevices() throws IOException {
    when(mockedUserService.isUserLoggedIn()).thenReturn(true);
    when(mockedUserService.getCurrentUser()).thenReturn(userFake);

    when(request.getParameter("annotatedLocation")).thenReturn(null);
    when(request.getParameter("annotatedAssetId")).thenReturn("ABC123");
    when(request.getParameter("annotatedUser")).thenReturn(null);
    when(request.getParameter(servlet.DEVICE_IDS_PARAMETER_NAME)).thenReturn("[device1, device2]");

    servlet.setUserService(mockedUserService);
    servlet.setUtilObj(mockedUtil);

    servlet.doPost(request, response);

    verify(response).sendRedirect(servlet.INDEX_URL);
    verify(mockedUserService, times(1)).isUserLoggedIn();
    verify(request, times(1)).getParameter("annotatedLocation");
    verify(request, times(1)).getParameter("annotatedAssetId");
    verify(request, times(1)).getParameter("annotatedUser");
    verify(request, times(1)).getParameter(servlet.DEVICE_IDS_PARAMETER_NAME);
    verify(mockedUtil, times(1)).updateDevices(TEST_USER_ID, Arrays.asList("device1", "device2"), "{\"annotatedAssetId\":\"ABC123\"}");
  }

}