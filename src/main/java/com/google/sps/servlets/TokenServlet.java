package com.google.sps.servlets;

import com.google.gson.Gson;
import com.google.sps.data.ChromeOSDevice;
import com.google.sps.data.ListDeviceResponse;
import com.google.sps.gson.Json;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import java.security.GeneralSecurityException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.HttpUrl;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.JSONParser;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.io.FileReader;
import java.io.File;

@WebServlet("/token")
public class TokenServlet extends HttpServlet {

  private final Gson GSON_OBJECT = new Gson();
  private final String CLIENT_SECRET_FILE = "/client_info.json";

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

  }


  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    final String authCode = (String) request.getParameter("code");
    
    File file = new File(this.getClass().getResource(CLIENT_SECRET_FILE).getFile());

    GoogleClientSecrets clientSecrets =
        GoogleClientSecrets.load(
            JacksonFactory.getDefaultInstance(), new FileReader(file));
            
    GoogleTokenResponse tokenResponse =
            new GoogleAuthorizationCodeTokenRequest(
                new NetHttpTransport(),
                JacksonFactory.getDefaultInstance(),
                "https://oauth2.googleapis.com/token",
                clientSecrets.getDetails().getClientId(),
                clientSecrets.getDetails().getClientSecret(),
                authCode,
                "http://localhost:8080") 
                .execute();

    String accessToken = tokenResponse.getAccessToken();
    OkHttpClient client = new OkHttpClient();
    HttpUrl.Builder urlBuilder = HttpUrl.parse("https://www.googleapis.com/admin/directory/v1/customer/my_customer/devices/chromeos").newBuilder();
    urlBuilder.addQueryParameter("maxResults", "55");
    urlBuilder.addQueryParameter("projection", "FULL");
    urlBuilder.addQueryParameter("sortOrder", "ASCENDING");
    urlBuilder.addQueryParameter("key", "AIzaSyBq4godZxCMXHkkqLDSve1x27gCSYmBfVM");
    String myUrl = urlBuilder.build().toString();
    Request req = new Request.Builder()
        .url(myUrl).addHeader("Authorization", "Bearer " + accessToken)
        .build();
    Response myResponse = client.newCall(req).execute();








    final String content = myResponse.body().string();//has the nextpage token, and chromeosdevices list
    final List<ChromeOSDevice> allDevices = new ArrayList<>();
    ListDeviceResponse resp = (ListDeviceResponse) Json.fromJson(content, ListDeviceResponse.class);
    while (resp.hasNextPageToken()) {
        allDevices.addAll(resp.getDevices());
        urlBuilder = HttpUrl.parse("https://www.googleapis.com/admin/directory/v1/customer/my_customer/devices/chromeos").newBuilder();
        urlBuilder.addQueryParameter("maxResults", "55");
        urlBuilder.addQueryParameter("projection", "FULL");
        urlBuilder.addQueryParameter("sortOrder", "ASCENDING");
        urlBuilder.addQueryParameter("key", "AIzaSyBq4godZxCMXHkkqLDSve1x27gCSYmBfVM");
        System.out.println((String) resp.getNextPageToken());
        urlBuilder.addQueryParameter("pageToken", (String) resp.getNextPageToken());
        String newUrl = urlBuilder.build().toString();
        Request newReq = new Request.Builder()
            .url( newUrl).addHeader("Authorization", "Bearer " + accessToken)
            .build();
        Response newResponse = client.newCall(newReq).execute();
        final String newContent = newResponse.body().string();
        resp = (ListDeviceResponse) Json.fromJson(newContent, ListDeviceResponse.class);
    }
    System.out.println(allDevices);


    // try {
    //     JSONParser parser = new JSONParser();
    //     Object obj = parser.parse(content);
    //     JSONObject mainResponseJSON = (JSONObject) obj;
    //     JSONArray devices = (JSONArray)  mainResponseJSON.get("chromeosdevices");
    //     System.out.println(devices.size());

    //     while (mainResponseJSON.containsKey("nextPageToken")) {
    //         System.out.println("next page token detected!");
    //         urlBuilder = HttpUrl.parse("https://www.googleapis.com/admin/directory/v1/customer/my_customer/devices/chromeos").newBuilder();
    //         urlBuilder.addQueryParameter("maxResults", "55");
    //         urlBuilder.addQueryParameter("projection", "FULL");
    //         urlBuilder.addQueryParameter("sortOrder", "ASCENDING");
    //         urlBuilder.addQueryParameter("key", "AIzaSyBq4godZxCMXHkkqLDSve1x27gCSYmBfVM");
    //         System.out.println((String) mainResponseJSON.get("nextPageToken"));
    //         urlBuilder.addQueryParameter("pageToken", (String) mainResponseJSON.get("nextPageToken"));
    //          String newUrl = urlBuilder.build().toString();
    //         Request newReq = new Request.Builder()
    //             .url( newUrl).addHeader("Authorization", "Bearer " + accessToken)
    //             .build();
    //         Response newResponse = client.newCall(newReq).execute();
    //         final String newContent  =newResponse.body().string();
    //         obj = parser.parse(newContent);
    //         mainResponseJSON = (JSONObject) obj;
    //         devices.addAll( (JSONArray)  mainResponseJSON.get("chromeosdevices"));
    //         System.out.println(devices.size());
    //     }

    //     String allDevicesString = devices.toString();
    //     System.out.println("all devices: ");
    //     System.out.println(allDevicesString);

    //     System.out.println(devices.size());
    //     System.out.println();
    // } catch(ParseException pe) {
		
    //      System.out.println("position: " + pe.getPosition());
    //      System.out.println(pe);
    // }



  }

}