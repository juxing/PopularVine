package com.example.vine;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreProtocolPNames;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class HttpOperation {
    final static Semaphore downloadSem = new Semaphore(1);
    private static String key = null;  //Store the value of session-id when after login.
    
    public static void login() {
		BufferedReader in = null;
		
		try {
			HttpClient client = new DefaultHttpClient();
			String url = "https://api.vineapp.com/users/authenticate";	
			HttpPost request = new HttpPost(url);
			List <NameValuePair> datas = new ArrayList <NameValuePair>();   
	        datas.add(new BasicNameValuePair("username", "mingzhangsjtu@gmail.com"));
	        datas.add(new BasicNameValuePair("password", "123456789"));
	        request.setEntity(new UrlEncodedFormEntity(datas));
	        
			HttpResponse response;
		    response = client.execute(request);
			in = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));			
			StringBuilder sb = new StringBuilder();
			String line = null;
			String NL = System.getProperty("line.separator");
			while((line = in.readLine()) != null)
				sb.append(line + NL);
			in.close();
			
			JSONObject jo = new JSONObject(sb.toString());			
            JSONObject data = jo.getJSONObject("data"); 
            key = data.getString("key");
            
            Log.i("sessionId", "sid is " + key);
	
		}  catch (ClientProtocolException cpe) {
			Log.e("httpmz", "cp" + cpe.toString());
		} catch (IOException ioe) {
			Log.e("httpmz", "io" + ioe.toString());
		} catch (IllegalStateException ile) {
			Log.e("httpmz", "ill" + ile.toString());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	//public static List<String> videoUrls = new ArrayList<String>();
	
	public static void getMetas(final String[] urls, final String[] des,  
    		final String[] comments, final String[] created, final String[] foursquareVenueId, 
    		final String[] liked, final String[] likes, final String[] postId,
    		final String[] promoted, final String[] tags, 
    		final String[] userId, final String[] username,
    		final String[] followerCount, final String[] verified, final String[] authoredPostCount, 
    		final String[] privateT, final String[] likeCount, final String[] following, final String[] postCount, final String[] followingCount,
    		final String[] explicitContent, final String[] blocking, final String[] blocked, final String[] reposts,
    		final String[] totalTagCounts,
    		final int page, final int size) {
		// The http ops below because involve some dns query and other time consuming functions, so is required
		// to be done in thread but not UI thread.
		Log.i("debugloadmore", "I am in getMetas");
		Runnable myRunnable = new Runnable() {
	    	public void run() {
	    		login();  //First login, get the session-id, very important!!!
	    		
                popular(urls, des, comments, created, foursquareVenueId, liked, likes, 
            			postId, promoted, tags, userId, username,
            			followerCount, verified,
            			authoredPostCount, privateT, likeCount, following, postCount, followingCount,
            			explicitContent, blocking, blocked, reposts, totalTagCounts,
            			page, size);
	    	}
	    };	    
	    Thread myThread = new Thread(myRunnable);
	    myThread.start();
	}
	
	public static void popular(String[] urls, String[] des, 
			String[] comments, String[] created, String[] foursquareVenueId, 
    		String[] liked, String[] likes, String[] postId,
    		String[] promoted, String[] tags, String[] userId, String[] username,
    		String[] followerCount, String[] verified, String[] authoredPostCount, 
    		String[] privateT, String[] likeCount, String[] following, String[] postCount, String[] followingCount,
    		String[] explicitContent, String[] blocking, String[] blocked, String[] reposts, String[] totalTagCounts,
    		int page, int size) {
		/*List<String> pops = new ArrayList<String>();
		Log.i("debugloadmore", "I am in popular");*/
		try {
			HttpClient client = new DefaultHttpClient();
			String baseUrl = "https://api.vineapp.com/timelines/popular?";	
			List<NameValuePair> params = new LinkedList<NameValuePair>();
			params.add(new BasicNameValuePair("page", String.valueOf(page)));
	        params.add(new BasicNameValuePair("size", String.valueOf(size)));
	        String paramString = URLEncodedUtils.format(params, "utf-8");
	        baseUrl = baseUrl + paramString;
						
			HttpGet request = new HttpGet(baseUrl);
			request.setHeader("user-agent", "com.vine.iphone/1.0.3 (unknown, iPhone OS 6.1.0, iPhone, Scale/2.000000)");
			Log.i("sessionId", "sid is " + key);
			request.setHeader("vine-session-id", key);
			request.setHeader("accept-language", "en, sv, fr, de, ja, nl, it, es, pt, pt-PT, da, fi, nb, ko, zh-Hans, zh-Hant, ru, pl, tr, uk, ar, hr, cs, el, he, ro, sk, th, id, ms, en-GB, ca, hu, vi, en-us;q=0.8");
	        			
			HttpResponse response = client.execute(request);
			BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));			
			StringBuilder sb = new StringBuilder();
			String line = null;
			String NL = System.getProperty("line.separator");
			while((line = in.readLine()) != null) {
				sb.append(line + NL);
			}			
			in.close();			
			
			JSONObject jo = new JSONObject(sb.toString());			
            JSONObject data = jo.getJSONObject("data"); 
            JSONArray records = data.getJSONArray("records");
            
            Log.i("debugloadmore", "before for");
            for(int i = 0; i < size; i++) {
                des[i] = records.getJSONObject(i).getString("description");
                //Log.i("desDebug", "des is " + des[i]);
                urls[i] = records.getJSONObject(i).getString("videoUrl");               
				comments[i] = records.getJSONObject(i).getJSONObject("comments").getString("count");
				created[i] = records.getJSONObject(i).getString("created");
				foursquareVenueId[i] = records.getJSONObject(i).getString("foursquareVenueId");
				//latitude[i] = records.getJSONObject(i).getString("latitude");  // No lat.
				liked[i] = records.getJSONObject(i).getString("liked");
                likes[i] = records.getJSONObject(i).getJSONObject("likes").getString("count");
                
                //No location anymore?
                //location[i] = records.getJSONObject(i).getString("location");
                //longitude[i] = records.getJSONObject(i).getString("longitude"); // No log.
                postId[i] = records.getJSONObject(i).getString("postId");
                //postToFacebook[i] = records.getJSONObject(i).getString("postToFacebook");
                promoted[i] = records.getJSONObject(i).getString("promoted");
                tags[i] = records.getJSONObject(i).getString("tags");
                userId[i] = records.getJSONObject(i).getString("userId");
                username[i] = records.getJSONObject(i).getString("username");  
                userInformation(userId[i], i, followerCount, verified, authoredPostCount, privateT, 
                		likeCount, following, postCount, followingCount, explicitContent, blocking, blocked);
                reposts[i] = records.getJSONObject(i).getJSONObject("reposts").getString("count");               
                long test = calcTagCounts(tags[i]);
                totalTagCounts[i] = String.valueOf(test);
                		
                Log.i("regexdd", test + " " + tags[i]);                
                Log.i("debugloadmore", i + " item: " + des[i]);
            }
		}  catch (ClientProtocolException cpe) {
			Log.e("httpmz", "cp" + cpe.toString());
		} catch (IOException ioe) {
			Log.e("httpmz", "io" + ioe.toString());
		} catch (IllegalStateException ile) {
			Log.e("httpmz", "ill" + ile.toString());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	} 
	
	public static void userInformation(String userId, int i, String[] followerCount, String[] verified,
			String[] authoredPostCount, String[] privateT, String[] likeCount, String[] following, 
			String[] postCount, String[] followingCount, String[] explicitContent, String[] blocking, 
			String[] blocked) {
		
		try {
			HttpClient client = new DefaultHttpClient();
			String baseUrl = "https://api.vineapp.com/users/profiles/" + userId;	
						
			HttpGet request = new HttpGet(baseUrl);
			request.setHeader("user-agent", "com.vine.iphone/1.0.3 (unknown, iPhone OS 6.1.0, iPhone, Scale/2.000000)");
			request.setHeader("vine-session-id", key);
			request.setHeader("accept-language", "en, sv, fr, de, ja, nl, it, es, pt, pt-PT, da, fi, nb, ko, zh-Hans, zh-Hant, ru, pl, tr, uk, ar, hr, cs, el, he, ro, sk, th, id, ms, en-GB, ca, hu, vi, en-us;q=0.8");
	        			
			HttpResponse response = client.execute(request);
			BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));			
			StringBuilder sb = new StringBuilder();
			String line = null;
			String NL = System.getProperty("line.separator");
			while((line = in.readLine()) != null) {
				sb.append(line + NL);
			}			
			in.close();			
			
			JSONObject jo = new JSONObject(sb.toString());			
            JSONObject data = jo.getJSONObject("data"); 
            
            followerCount[i] = data.getString("followerCount");
            verified[i] = data.getString("verified");               
			authoredPostCount[i] = data.getString("authoredPostCount");
			privateT[i] = data.getString("private");
			likeCount[i] = data.getString("likeCount");
            following[i] = data.getString("following");
            postCount[i] = data.getString("postCount");
            followingCount[i] = data.getString("followingCount");
            explicitContent[i] = data.getString("explicitContent");
            blocking[i] = data.getString("blocking");
            blocked[i] = data.getString("blocked");                
            //Log.i("debugloadmore", i + " item: " + des[i]);

		}  catch (ClientProtocolException cpe) {
			Log.e("httpmz", "cp" + cpe.toString());
		} catch (IOException ioe) {
			Log.e("httpmz", "io" + ioe.toString());
		} catch (IllegalStateException ile) {
			Log.e("httpmz", "ill" + ile.toString());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	
	public static List<String> regexTags(String tagStr) {
		List<String> tags = new ArrayList<String>();
		String regex = "\"tag\":\"([^\"]+)\"";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(tagStr);
		while(m.find()) {
			tags.add(m.group(1));
			Log.i("regex", m.group(1));
		}			
		return tags;
	}
	
	public static long calcTagCounts(String tagStr) {
		List<String> tags = regexTags(tagStr);
		long totalTagCounts = 0;
		
		HttpClient client = new DefaultHttpClient();        			
		HttpResponse response = null; 							
		String line = null;
		String NL = System.getProperty("line.separator");
		
		for(int i = 0; i < tags.size(); i++) {
			try {			
				String baseUrl = "https://api.vineapp.com/timelines/tags/" + tags.get(i);					
				HttpGet request = new HttpGet(baseUrl);
				request.setHeader("user-agent", "com.vine.iphone/1.0.3 (unknown, iPhone OS 6.1.0, iPhone, Scale/2.000000)");
				request.setHeader("vine-session-id", key);
				request.setHeader("accept-language", "en, sv, fr, de, ja, nl, it, es, pt, pt-PT, da, fi, nb, ko, zh-Hans, zh-Hant, ru, pl, tr, uk, ar, hr, cs, el, he, ro, sk, th, id, ms, en-GB, ca, hu, vi, en-us;q=0.8");
				response = client.execute(request);
				
				BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
				StringBuilder sb = new StringBuilder();
				while((line = in.readLine()) != null) {
					sb.append(line + NL);
				}
				in.close();			
				
				JSONObject jo = new JSONObject(sb.toString());			
		        JSONObject data = jo.getJSONObject("data");
		        String count = data.getString("count"); 
		        totalTagCounts +=  Integer.parseInt(count);	        
	        
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return totalTagCounts;
	}
	
	public static void downloadVideo(String fileName, String videoUrl) {			
		try {
	        final int TIMEOUT_CONNECTION = 5000;//5sec
	        final int TIMEOUT_SOCKET = 30000;//30sec
	
	        URL vurl = new URL(videoUrl);
	
	        //long startTime = System.currentTimeMillis();
	        
	        //Open a connection to that URL.	        
	        URLConnection ucon = vurl.openConnection();
	
	        //this timeout affects how long it takes for the app to realize there's a connection problem
	        ucon.setReadTimeout(TIMEOUT_CONNECTION);
	        ucon.setConnectTimeout(TIMEOUT_SOCKET);
	
	        //Define InputStreams to read from the URLConnection.
	        // uses 3KB download buffer
	        InputStream is = ucon.getInputStream();
	        BufferedInputStream inStream = new BufferedInputStream(is, 1024 * 1024 * 2);
	        
	        // To see if file exists, if so just return, if not, keep going.
	        FileOutputStream outStream;
	        downloadSem.acquire();	        
	        File f = new File(fileName);
			if(f.exists()) {
				downloadSem.release();
				inStream.close();
				Log.i("fileexist", "File exists, I quit." + Thread.currentThread().getId());				
			    return;
			}
			else {
				outStream = new FileOutputStream(fileName);
				Log.i("fileexist", "File not exists, go ahead." + Thread.currentThread().getId());
			    downloadSem.release();
		    }
						
	        byte[] buff = new byte[5 * 1024];
	
	        //Read bytes (and store them) until there is nothing more to read(-1)
	        int len;
	        while ((len = inStream.read(buff)) != -1)
	        {
	            outStream.write(buff,0,len);
	        }
	
	        //clean up
	        outStream.flush();
	        outStream.close();
	        inStream.close();
	        
	        Log.i("MediaPlayermz", "Download finished by " + Thread.currentThread().getId());
        
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		return;
        
	}
	
	public static void uploadStatToServer(String fileName) throws ClientProtocolException, IOException {
	    // TODO Auto-generated method stub   
	    HttpClient httpClient = new DefaultHttpClient();
	    httpClient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);

	    HttpPost httpPost = new HttpPost("http://192.168.1.2:8080/mingzhang/UploadFileServer");
	    File file = new File(fileName);

	    MultipartEntity mpEntity = new MultipartEntity();
	    ContentBody cbFile = new FileBody(file, "text/plain");
	    mpEntity.addPart("userfile", cbFile);

	    httpPost.setEntity(mpEntity);
	    //System.out.println("executing request " + httpPost.getRequestLine());
	    HttpResponse response = httpClient.execute(httpPost);
	    HttpEntity resEntity = response.getEntity();

	    /*System.out.println(response.getStatusLine());
	    if (resEntity != null) {
	      System.out.println(EntityUtils.toString(resEntity));
	    }
	    if (resEntity != null) {
	      resEntity.consumeContent();
	    }*/

	    httpClient.getConnectionManager().shutdown();
	}
	
	public static void uploadStatistics(final String fileName) {
		Runnable myRunnable = new Runnable() {
	    	public void run() {
	    		try {
					uploadStatToServer(fileName);
				} catch (ClientProtocolException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	    	}
	    };	    
	    Thread myThread = new Thread(myRunnable);
	    myThread.start();
	}

}
