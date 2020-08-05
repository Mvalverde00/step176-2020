package com.google.sps.servlets;

import java.util.ArrayList;
import com.google.sps.data.ChromeOSDevice;
import java.security.GeneralSecurityException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import com.google.sps.gson.Json;
import java.util.List;
import java.util.ArrayList;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.sps.servlets.Util;
import javax.servlet.annotation.WebServlet;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.FormBody;
import okhttp3.Response;
import java.util.Random;
import java.util.Arrays;

@WebServlet("/update")
public class UpdateServlet extends HttpServlet {

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    final UserService userService = UserServiceFactory.getUserService();
    final User currentUser = userService.getCurrentUser();
    if ((!userService.isUserLoggedIn()) || (currentUser == null)) {
      response.sendRedirect("/login");
      return;
    }
    final String userId = currentUser.getUserId();
    final List<ChromeOSDevice> allDevices = Util.getAllDevices(userId);
    final List<String> deviceIds = new ArrayList<>();
    for (ChromeOSDevice device : allDevices) {
        deviceIds.add(device.getDeviceId());
    }
    List<String> locations = Arrays.asList("NYC", "SF", "LA", "BOS", "DC");
    List<String> users = Arrays.asList("Bob", "Alice", "Eve", "george", "michael", "blab", "name");
    for (String deviceId : deviceIds) {
        Random rand = new Random();
        String newAnnotatedUser = locations.get(rand.nextInt(7));
        String newAnnotatedLocation = locations.get(rand.nextInt(5));
        updateDevice(userId, deviceId, newAnnotatedUser, newAnnotatedLocation);
    }
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

  }

  private void updateDevice(String userId, String deviceId, String newAnnotatedUser, String newAnnotatedLocation) throws IOException {
    final String myUrl = getUpdateUrl(deviceId);
    final String accessToken = Util.getAccessToken(userId);
    OkHttpClient client = new OkHttpClient();
    RequestBody formBody = new FormBody.Builder()
      .add("annotatedLocation", newAnnotatedLocation)
      .add("annotatedUser", newAnnotatedUser)
      .build();
    Request req = new Request.Builder().url(myUrl).put(formBody).addHeader("Authorization", "Bearer " + accessToken).build();
    Response myResponse = client.newCall(req).execute();
  }

  private String getUpdateUrl(String deviceId) {
      return "https://www.googleapis.com/admin/directory/v1/customer/my_customer/devices/chromeos/" + deviceId + "?projection=BASIC";
  }

}