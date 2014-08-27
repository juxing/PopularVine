package com.example.vine;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

public class MyAdapter extends BaseAdapter implements SurfaceTextureListener, MediaPlayer.OnPreparedListener {    
    private LayoutInflater inflater;    
    public List<Map<String, Object>> data = null;  // Make it public so in MainActivity onDestroy can read data
                                                   // and create stat file.
    //private List<Thread> allocThreads = new ArrayList<Thread>();
    private Context mContext = null;
    MediaPlayer mMediaPlayer = null;
    int nowPlaying = -1; // Label which video is playing now, in case that at the app begin, multiple threads try to play item 0.
    //boolean _run = true; // Used to stop all threads remaining.
    // To lock MediaPlayer.
    boolean mpLocked = false;
    final Semaphore mpSem = new Semaphore(1);
    //Handler myHandler = new Handler();
    
    private boolean threadsRunning = true;
    
    //To kill six redundant threads at the very begining.
    int prePos = -1;
    final Semaphore killSem = new Semaphore(1);
    
    //Define public place to store all view item data, then start a deamon to centrally control them.
    private class ViewComponent {
    	public View convertView;
    	public TextureView mPreview;
    	public boolean downloaded;
    	
    	public ViewComponent(View v, TextureView tv, boolean d) {
    		convertView = v;
    		mPreview = tv;
    		downloaded = d;
    	}
    }
    private Semaphore containerLock = null;
    private TreeMap<Integer, ViewComponent> viewsContainer = null;
    private int isplaying;
    private ListView mylv = null;
    private float mySpeed = 0;  //Current screen rolling speed, help to make decision about downloading videos.
    
    private HashMap<Long, View> myViews = new HashMap<Long, View>();
    //private Handler deamonHandler;
    
    //int printStat = 0;  // When surface destroyed, to track if has already print statistics, because destroy
                        // be executed several times, just like getView.
    
	/*String[] files = {
			"0c42aa3d95a53dd4e88e8a382ae00c77",
			"2a10fd73d8a5798ea054f602139fcee5",
			"2ba5ebb2377a8d26c4d9ed2fa709887a",
			"01be5d66f16443ce3e0e2f9c0f3936a1",
			"03f524a503db391e950de91245be61df",
	};*/
    
    int curPage = 1;  // Lable which page is being viewed.
    int size = 10;  // Each page contains 10 videos.
    // Here are tmp location to store info, data is the real data sturcture used by adapter.
    
    // Video info.
    String[] files = new String[size];  // Video files names.
    String[] videoUrls = new String[size];  // Video file urls.
    String[] des = new String[size];  // Vide descriptions.
    String[] comments = new String[size];
    String[] created = new String[size];
    String[] foursquareVenueId = new String[size];
    //String[] latitude = new String[size];
    String[] liked = new String[size];
    String[] likes = new String[size];
    //String[] location = new String[size];
    //String[] longitude = new String[size];
    String[] postId = new String[size];
    //String[] postToFacebook = new String[size];
    String[] promoted = new String[size];
    String[] tags = new String[size];
    String[] userId = new String[size];
    String[] username = new String[size];
    String[] reposts = new String[size];
    String[] totalTagCounts = new String[size];
    
    // Video author info.
    String[] followerCount = new String[size];
    String[] verified = new String[size];
    String[] authoredPostCount = new String[size];
    String[] privateT = new String[size];
    String[] likeCount = new String[size];
    String[] following = new String[size];
    String[] postCount = new String[size];
    String[] followingCount = new String[size];
    String[] explicitContent = new String[size];
    String[] blocking = new String[size];
    String[] blocked = new String[size];
       
    // Load next page's information by its page number and size.
    private void getMetaDataByPage(String[] files, String[] videoUrls, String[] des, 
    		String[] comments, String[] created, String[] foursquareVenueId, 
    		String[] liked, String[] likes, String[] postId,
    		String[] promoted, String[] tags, String[] userId, String[] username,
    		String[] followerCount, String[] verified, String[] authoredPostCount, 
    		String[] privateT, String[] likeCount, String[] following, String[] postCount, String[] followingCount,
    		String[] explicitContent, String[] blocking, String[] blocked, String[] reposts, String[] totalTagCounts,
    		int page, int size) {
    	Log.i("debugloadmore", "I am in getMetaDataByPage");
    	for(int i = 0; i < size; i++) {
    		files[i] = String.valueOf((page - 1) * size + i);  // We hardcode that video file names are numbers.
    	}    	
    	HttpOperation.getMetas(videoUrls, des, comments, created, foursquareVenueId, liked, likes, 
    			 postId, promoted, tags, userId, username, followerCount, verified,
    			authoredPostCount, privateT, likeCount, following, postCount, followingCount,
    			explicitContent, blocking, blocked, reposts, totalTagCounts,
    			page, size);  // Get urls and description from vine API.
    }
    
    // Initialize listview adapter.
    public MyAdapter(Context context, ListView lv) {    
        inflater = LayoutInflater.from(context);  
        mContext = context;     
        mylv = lv;
        initAdapter();    
    }    
    
    //初始化    
    private void initAdapter() {   
    	getMetaDataByPage(files, videoUrls, des, comments, created, foursquareVenueId, liked, likes, 
    			 postId, promoted, tags, userId, username,
    			followerCount, verified,
    			authoredPostCount, privateT, likeCount, following, postCount, followingCount,
    			explicitContent, blocking, blocked, reposts, totalTagCounts,
    			1, size);  // Get information of the first page.
    	 	
    	// Because threads to get info, so UI main thread has to wait until all info are fetched.
    	// But just wait 5 sec.
    	long checkTime = System.currentTimeMillis();
    	while(!((files[files.length-1] != null) && (videoUrls[videoUrls.length-1] != null) 
    			&& (des[des.length-1] != null) && (comments[comments.length-1] != null) 
    			&& (created[created.length-1] != null) && (foursquareVenueId[foursquareVenueId.length-1] != null) 
    			&& (liked[liked.length-1] != null) && (likes[likes.length-1] != null) 
    			&& (postId[postId.length-1] != null) 
    			&& (promoted[promoted.length-1] != null) 
    			&& (tags[tags.length-1] != null) && (userId[userId.length-1] != null) 
    			&& (username[username.length-1] != null) && (totalTagCounts[totalTagCounts.length-1] != null))) {
    		long waitTime = System.currentTimeMillis();
    		if((waitTime - checkTime) > 10000) {
    			Log.i("timedebug", "initAdapter waits too long, just quit.");
    			break;
    		}
    		else 
    			continue;
    	}
    	
    	// Initialize the data container used by adapter.
        data = new ArrayList<Map<String, Object>>();    
        for (int i = 0; i < files.length; i++) {    
            Map<String, Object> map = new HashMap<String, Object>();    
            //map.put("url", "/sdcard/Android/data/co.vine.android/cache/"+files[i]);
            
            // Add video info.
            map.put("fileName", "/sdcard/myVine/"+files[i]);
            map.put("description", des[i]);
            map.put("videoUrl", videoUrls[i]);
            map.put("played", 0);  // To track if this video is played.
            map.put("comments", comments[i]);
            map.put("created", created[i]);
            map.put("foursquareVenueId", foursquareVenueId[i]);
            //map.put("latitude", latitude[i]);
            map.put("liked", liked[i]);
            map.put("likes", likes[i]);
            //map.put("location", location[i]);
            //map.put("longitude", longitude[i]);
            map.put("postId", postId[i]);
            //map.put("postToFacebook", postToFacebook[i]);
            map.put("promoted", promoted[i]);
            map.put("tags", tags[i]);
            map.put("userId", userId[i]);
            map.put("username", username[i]);
            map.put("reposts", reposts[i]);
            map.put("totalTagCounts", totalTagCounts[i]);
            
            // Add video author info.
            map.put("followerCount", followerCount[i]);
            map.put("verified", verified[i]);
            map.put("authoredPostCount", authoredPostCount[i]);
            map.put("privateT", privateT[i]);
            map.put("likeCount", likeCount[i]);
            map.put("following", following[i]);
            map.put("postCount", postCount[i]);
            map.put("followingCount", followingCount[i]);
            map.put("explicitContent", explicitContent[i]);
            map.put("blocking", blocking[i]);
            map.put("blocked", blocked[i]);
                       
            data.add(map);    
        }    
        
        //Initialize central control
        containerLock = new Semaphore(1);
        viewsContainer = new TreeMap<Integer, ViewComponent>();
        isplaying = -1;
        //mMediaPlayer = new MediaPlayer();
        Log.i("deamontest", "Deamon initialized, viewsContainer size is " + viewsContainer.size());
        Runnable deamonRunnable = new Runnable() {
			public void run() {
				//Receive message from main UI.
				/*Looper.prepare();
				deamonHandler = new Handler() {
					public void handleMessage(Message msg) {
						Log.i("handlertest", (String)msg.obj);
						threadsRunning = false;
					}
				};
				Looper.loop();*/
				try {
					Thread.sleep(500);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}	
				
				Log.i("deamontest", "Deamon starts!");
				while(threadsRunning) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}					
					try {
						containerLock.acquire();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if(viewsContainer.size() == 0) {
						//Log.i("deamontest", "container is empty, wait a sec.");
						containerLock.release();
						//continue;
					}
					else {
						containerLock.release();
						
						int pos = mylv.getFirstVisiblePosition();
						//Log.i("deamontest", "first visible position is " + pos);
						for(int i = (pos+1); i >= pos; i--) {
							ViewComponent vc = null;
							if(i < viewsContainer.size()) {
								vc = viewsContainer.get(i);
								Log.i("duptest", i + ": " + vc.convertView.hashCode());
							}
							if((vc != null)) {
								int[] position = new int[2];
								vc.convertView.getLocationInWindow(position);
								Log.i("loctest", i + " location is " + position[1]);
								if((position[1] < 450) && (position[1] >= 0) && (mySpeed < 0.2)) {  //The video right in the place, and user is not skipping over it.
									//Log.i("deamontest", Thread.currentThread().getId() + ": Start download and play." + pos);
									if(vc.downloaded == false) {  //Useless? Because downloadVideo() will check
										                          //if the video has been downloaded.
										Log.i("deamontest", "download " + i);
										
										HttpOperation.downloadVideo((String)data.get(i).get("fileName"), 
																	(String)data.get(i).get("videoUrl"));
										vc.downloaded = true;
									}
									if(i != isplaying) {
										Log.i("deamontest", i + " is going to replace " + isplaying);
										Log.i("TextureViewmz", "tvid in use: "+vc.mPreview.hashCode());
										streamMPtoST(i, vc.mPreview, vc.convertView);										
									}
									
									//Prefetch
									final String tag = "Prefetch";
									if(i < (data.size()-1)){
										HttpOperation.downloadVideo((String)data.get(i+1).get("fileName"), 
																	(String)data.get(i+1).get("videoUrl"));
										Log.i(tag, "Prefetch " + (i+1));
									}
									
									break;
								}
							}
						}
					}	
					
				}
				Log.i("deamonmz", "Deamon stop!");
				
        	}
        };
        Thread deamonThread = new Thread(deamonRunnable);
		deamonThread.start();
		
		//Start a new thread responsible for monitor speed.
		Runnable speedMonitor = new Runnable() {
			public void run() {
			    final String tag = "speedtest";
			    LinkedList<Float> speedRecords = new LinkedList<Float>();
			    int lastLocation = 0;
			    long lastTime = System.currentTimeMillis();
			    
				while(threadsRunning) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					
					View test = viewsContainer.get(0).convertView;
					int[] tt = new int[2];
					test.getLocationInWindow(tt);

					if(Math.abs(tt[1] - lastLocation) < 1000) {
						int locaDiff = Math.abs(tt[1] - lastLocation);
						long timeDiff = System.currentTimeMillis() - lastTime;
						float currSpeed = (float)locaDiff / (float)timeDiff;
						if(speedRecords.size() >= 10) {
							speedRecords.remove();
						}
						speedRecords.add(currSpeed);
						//Log.i(tag, speedRecords.toString());						
					}	
					lastLocation = tt[1];
					lastTime = System.currentTimeMillis();
					
					float sumSpeed = (float) 0.0;
					for(int i = 0; i < speedRecords.size(); i++)
						sumSpeed += speedRecords.get(i);
					mySpeed = sumSpeed / 10;
					Log.i(tag, "" + mySpeed);
				}
				Log.i(tag, "Thread stop.");
			}
        };
        Thread speedMoniThread = new Thread(speedMonitor);
		speedMoniThread.start();
    }    
	
	public boolean loadMoreData() { 
		/*int count = data.size();
        for (int i = 0; i < files.length; i++) {  
        	Map<String, Object> map = new HashMap<String, Object>();    
            //map.put("url", "/sdcard/Android/data/co.vine.android/cache/"+files[i]);
            map.put("url", "/sdcard/myVine/"+files[i]);
            map.put("title", "None" + i + count);
            data.add(map);
        }*/ 
		
		curPage++;  // Next page number.
		Log.i("debugloadmore", "Want to load page " + curPage);
		if(curPage == 11) // From json content, we know that popular will return 100 records.
			return false;		
		
		// First clear old information.
		for(int i = 0; i < files.length; i++) {
			// Clear video info.
			files[i] = null;
			videoUrls[i] = null;
			des[i] = null;
			comments[i] = null;
			created[i] = null;
			foursquareVenueId[i] = null;
			//latitude[i] = null;
			liked[i] = null;
			likes[i] = null;
			//location[i] = null;
			//longitude[i] = null;
			postId[i] = null;
			//postToFacebook[i] = null;
			promoted[i] = null;
			tags[i] = null;
			userId[i] = null;
			username[i] = null;
			reposts[i] = null;
			totalTagCounts[i] = null;
			
			// Clear video author info.
			followerCount[i] = null;
			verified[i] = null;
			authoredPostCount[i] = null;
			privateT[i] = null;
			likeCount[i] = null;
			following[i] = null;
			postCount[i] = null;
			followingCount[i] = null;
			explicitContent[i] = null;
			blocking[i] = null;
			blocked[i] = null;			
		}
		
		Log.i("debugloadmore", "Before end getMetaDataByPage");
		getMetaDataByPage(files, videoUrls, des, comments, created, foursquareVenueId, liked, likes, 
    			postId, promoted, tags, userId, username, 
    			followerCount, verified,
    			authoredPostCount, privateT, likeCount, following, postCount, followingCount,
    			explicitContent, blocking, blocked, reposts, totalTagCounts,
    			curPage, size);
		// Wait for the thread to fill in all information.
		// But sometimes at one page we may not get 10 items, maybe only 7 or 8, so just check if the last item
		// of array is filled in is not enough, so we add a timeer, if wait longer than 3 sec, we just return,
		// even if the arrays are not full.
		// If arrays are not full, our app can handle well now, it just leave the empty videos there blank, and
		// strings are null, but later we will improve it, try to remove the empty one and make it unnoticable.
		long checkTime = System.currentTimeMillis();
		while(!((files[files.length-1] != null) && (videoUrls[videoUrls.length-1] != null) 
				&& (des[des.length-1] != null) && (comments[comments.length-1] != null) 
    			&& (created[created.length-1] != null) && (foursquareVenueId[foursquareVenueId.length-1] != null) 
    			&& (liked[liked.length-1] != null) && (likes[likes.length-1] != null) 
    			&& (postId[postId.length-1] != null) 
    			&& (promoted[promoted.length-1] != null) 
    			&& (tags[tags.length-1] != null) && (userId[userId.length-1] != null) 
    			&& (username[username.length-1] != null) && (totalTagCounts[totalTagCounts.length-1] != null))) {
			long waitTime = System.currentTimeMillis();
			if((waitTime - checkTime) > 10000) {
				Log.i("timedebug", "wait too long, just return");
				break;
			}
			else
				continue;
		}
				
		for (int i = 0; i < files.length; i++) {    
            Map<String, Object> map = new HashMap<String, Object>();    
            //map.put("url", "/sdcard/Android/data/co.vine.android/cache/"+files[i]);
            // Add video info.
            map.put("fileName", "/sdcard/myVine/"+files[i]);
            map.put("description", des[i]);
            map.put("videoUrl", videoUrls[i]);
            map.put("played", 0);  // To track if this video is played.
            map.put("comments", comments[i]);
            map.put("created", created[i]);
            map.put("foursquareVenueId", foursquareVenueId[i]);
            //map.put("latitude", latitude[i]);
            map.put("liked", liked[i]);
            map.put("likes", likes[i]);
            //map.put("location", location[i]);
            //map.put("longitude", longitude[i]);
            map.put("postId", postId[i]);
            //map.put("postToFacebook", postToFacebook[i]);
            map.put("promoted", promoted[i]);
            map.put("tags", tags[i]);
            map.put("userId", userId[i]);
            map.put("username", username[i]);
            map.put("reposts", reposts[i]);
            map.put("totalTagCounts", totalTagCounts[i]);
            
            // Add video author info.
            map.put("followerCount", followerCount[i]);
            map.put("verified", verified[i]);
            map.put("authoredPostCount", authoredPostCount[i]);
            map.put("privateT", privateT[i]);
            map.put("likeCount", likeCount[i]);
            map.put("following", following[i]);
            map.put("postCount", postCount[i]);
            map.put("followingCount", followingCount[i]);
            map.put("explicitContent", explicitContent[i]);
            map.put("blocking", blocking[i]);
            map.put("blocked", blocked[i]);
                       
            data.add(map);  // Add to data, so now data has more items.
        }
		
		return true;
	}
    
    @Override    
    public int getCount() {  
    	Log.i("getcount", "data size is "+data.size());
        return data.size();            
    }    
    
    @Override    
    public Object getItem(int position) {    
        return null;    
    }    
    
    @Override    
    public long getItemId(int position) {    
        return 0;    
    }    

    // ListView item's layout.
    public final class ViewHolder {    
        /*public VideoView video;    
        public TextView title;    
        public TextView url; */   
    	public FrameLayout fl;
    	public TextView tv;
    }  
    
    /*private MediaPlayer mMediaPlayer;
	private TextureView mPreview;*/
    
    @Override    
    // In my design, all the important action happen when getView incur, maybe there are better solution.
    public View getView(final int position, View convertView, final ViewGroup parent) {     	
    	//There are only 2 convertView instances, and be used alternatively.
    	
    	// getView is called 7 times at the app begin, donot know why, but this leads to multiple threads problem.
    	// 5 of their's surfacetexture is available, and 2 are not available, do not know why.
    	Log.i("surfaceavailable", "view "+position+" is ready");
    	Log.i("testgetviewpos", " " + position);
        ViewHolder holder = null;    
        TextureView mPreview = new TextureView(mContext);  //This TextureView object will exist for each view until exit from app.
        mPreview.setSurfaceTextureListener(this);	    
	    
	    //SurfaceTexture st = mPreview.getSurfaceTexture();
	    //((View) st).setTag(position);
	    
        //convertView为null的时候初始化convertView。    
        /*if (convertView == null) {    
            holder = new viewHolder();    
            convertView = inflater.inflate(R.layout.list_view_item, null);    
            holder.title = (TextView) convertView.findViewById(R.id.title);    
            holder.url = (TextView) convertView.findViewById(R.id.url);    
            convertView.setTag(holder);    
        } else {    
            holder = (viewHolder) convertView.getTag();    
        }    
  
        holder.title.setText(data.get(position).get("title").toString());    
        holder.url.setText(data.get(position).get("url").toString()); */
        
        if (convertView == null) {    
            holder = new ViewHolder();    
            convertView = inflater.inflate(R.layout.list_view_item, null); 
            Log.i("convertView", "New convertVie: "+convertView.hashCode());
            holder.fl = (FrameLayout)convertView.findViewById(R.id.frame);  
            holder.tv = (TextView)convertView.findViewById(R.id.text);
            convertView.setTag(holder);    
        } else {   
        	Log.i("convertView", "Old convertVie: "+convertView.hashCode());
            holder = (ViewHolder)convertView.getTag();    
        }          

	    holder.fl.addView(mPreview);
	    holder.tv.setText(data.get(position).get("username") + "\t\t" 
	    		+ data.get(position).get("created") + "\n"
	    		+ data.get(position).get("description") + "\n" 
	    		+ data.get(position).get("tags") + "\n" 
	    		+ data.get(position).get("totalTagCounts") + "\n"
	    		+ data.get(position).get("likes") + " likes" + "\t\t"	    		
	    		+ data.get(position).get("comments") + " comments" + "\n"
	    		+ data.get(position).get("reposts") + " revines" + "\n\n"
	    		
	    		// Show video info.
	    		/*+ "foursquareVenueId " + data.get(position).get("foursquareVenueId") + "\n" 
	    		+ "liked " + data.get(position).get("liked") + "\n" 
	    		+ "location " + data.get(position).get("location") + "\n" 
	    		+ "postId " + data.get(position).get("postId") + "\n"
	    		+ "postToFacebook " + data.get(position).get("postToFacebook") + "\n" 
	    		+ "promoted " + data.get(position).get("promoted") + "\n" 
	    		+ "userId " + data.get(position).get("userId") + "\n"*/ 
	    		
	    		// Show video author info.
				+ "followerCount " + data.get(position).get("followerCount") + "\n" 
				+ "verified " + data.get(position).get("verified") + "\n" 
				+ "authoredPostCount " + data.get(position).get("authoredPostCount") + "\n"
				+ "private " + data.get(position).get("privateT") + "\n" 
				+ "likeCount " + data.get(position).get("likeCount") + "\n" 
				+ "following " + data.get(position).get("following") + "\n"
				+ "postCount " + data.get(position).get("postCount") + "\n"
				+ "followingCount " + data.get(position).get("followingCount") + "\n"
				+ "explicitContent " + data.get(position).get("explicitContent") + "\n"
				+ "blocking " + data.get(position).get("blocking") + "\n"
				+ "blocked " + data.get(position).get("blocked") + "\n"
	    		);
	    
	    //Copy view data to central control place	   
	    try {
			containerLock.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    if(viewsContainer.get(position) == null) {
	    	 ViewComponent vc = new ViewComponent(convertView, mPreview, false);
	    	 viewsContainer.put(position, vc);
	    	 Log.i("TextureViewmz", "tvid new tv: "+convertView.hashCode());
	    	 Log.i("deamontest", "add position " + position);
	    	 Log.i("deamontest", "container size is " + viewsContainer.size());
	    }	    
	    else {
	    	//Because sometimes there is only audio but no video, maybe because the TextureView is old and destroyed, and since getView() produce new mPreview everytime,
	    	//so just keep the mPreview are the freshest.
	    		
	    	viewsContainer.get(position).convertView = convertView;
	    	viewsContainer.get(position).mPreview = mPreview;
	    	Log.i("TextureViewmz", "tvid replace: "+convertView.hashCode());
	    }
	    containerLock.release();
	    
	    // Download and play mp4 thread
	   /* final View tmpView = convertView;  //Cannot directly use convertView, syntax require the View must be final, so we use a tmp variable.
	    Runnable downloadRunnable = new Runnable() {
			public void run() {	
				//At the very beginning, kill redundant simultanesous threads.
				//We assume normally, getView() should provide different positions, if the same, then concurrent threads should be killed.
				try {
					killSem.acquire();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}				
				if(position == prePos) {
					killSem.release();
					Log.i("NewThreadKiller", Thread.currentThread().getId() + ": quits.");
					return;  //This thread is redundant, just return and die.
				}
				else {
					prePos = position;
				    killSem.release();
				}
				
				int[] pos = new int[2];
				tmpView.getLocationInWindow(pos);
				while(threadsRunning && ((pos[1] > -2000) || (pos[1] > 2000))) {  //Screen is 1280*720, head of activity is 146.
			    	if((pos[1] < 450) && (pos[1] >= 0)) {  //If after playing, we stay, will the video be replayed? Now there is a bug, seven threads running in cycle,
			    		                                   //make app extremely slow down.
				        Log.i("ViewPosition", Thread.currentThread().getId() + ": Start download and play.");
				    	HttpOperation.downloadVideo((String)data.get(position).get("fileName"), 
				    								(String)data.get(position).get("videoUrl"));
						streamMPtoST(position, mPreview, parent);	
			    	}
			    	
			    	tmpView.getLocationInWindow(pos);
				}
		    	Log.i("ViewPositionTest" , Thread.currentThread().getId() + " pos is " + pos[1] + " and quits.");
		    }
		};	    
		Thread downloadThread = new Thread(downloadRunnable);
		downloadThread.start();*/
		
	    /*}
	    else
	    	Log.i("predict", "Do not download " + position);*/
	    /*Runnable myRunnable = new Runnable() {
	    	public void run() {
	    		Log.i("threadmz", "Start thread: " + Thread.currentThread().getId());
	    		streamMPtoST(position, mPreview);
	    		Log.i("threadmz", "Finish thread: " + Thread.currentThread().getId());
	    	}
	    };	    
	    Thread myThread = new Thread(myRunnable);
	    //allocThreads.add(myThread);
	    myThread.start();*/
	    
        return convertView;    
    }
    
    private void streamMPtoST(int pos, TextureView tv, View cv) {
    	//Log.i("TextureViewmz", "tvid passed: "+tv.hashCode());
    	//Log.i("threadmz", "Thread before while: " + Thread.currentThread().getId());
    	/* while(!tv.isAvailable()) {
    		if(!_run) {
    			Log.i("threadmz", "Finish thread from while: " + Thread.currentThread().getId());
    			return;
    		}
    		try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}*/
    	
    	// If still not available, just drop, Keep It Simple Stupid! But the sleep seems it is no use anymore,
    	// because in http operation we have already filter out duplicate threads.
    	/*try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
    	// Sometimes all 7 st are not available, a bug.
    	/*if(!tv.isAvailable()) {
    		Log.i("deamontest", "Thread surfacetexture is not available: " + Thread.currentThread().getId());
    		return;
    	}*/
    	
    	// Decide if should play this video, if not in visible region, just return.
    	// Base on my design, my screen can display 2 items at most.
    	/*ListView parentlv = (ListView)parent;
    	int firstVisiblePos = parentlv.getFirstVisiblePosition();
    	if((pos < firstVisiblePos) || (pos > (firstVisiblePos + 1)))
    		return;    	
    	Log.i("TextureViewmz", "tvid passed ahead: "+tv.hashCode());
    	Log.i("tvavailable", "tv available at " + Thread.currentThread().getId());*/
        	
    	/*if(s == null)
    		Log.i("deamontest", "surface is null.");*/
    	
    	// I try to kill all multiple threads, but sometimes there still 2 threads play at the same time, check later.
    	/*try {
			mpSem.acquire();  // This used to protect mpLocked.
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    	
    	if(mpLocked == false) {
    		Log.i("mplock", "Start lock!");
    		mpLocked = true;  // This protect the MediaPlay config process.
    	}
    	else {
    		Log.i("threadmz", "Thread failed compete mp: " + Thread.currentThread().getId());
    		mpSem.release();
    		return;
    	}
    	
    	mpSem.release();
    	
    	Log.i("lockcheck", "Before try: " + Thread.currentThread().getId());*/
    	
    	// This block lost.
    	// Only one thread can enter.
		try {
			   /*if((mMediaPlayer != null) && (pos != nowPlaying)) {  // If another video is playing.
*/				   // To track if the last video is played, must larger than 5 sec.
			if(mMediaPlayer != null) {
				if(mMediaPlayer.isPlaying()) {
					int mCurPos = mMediaPlayer.getCurrentPosition();
					if((mCurPos < 5000))
						   data.get(isplaying).put("played", 0);
					mMediaPlayer.stop();									
				}
				mMediaPlayer.release();				
			}
			isplaying = pos;
			
			//Deal with fatal bug about memory, sometimes TextureView is not available to provide SurfaceTexture, if you just use it may lead to fatal memory bug,
			//app will be forced out without notify.
			
			int limit = 0;
	    	while(!tv.isAvailable() && (limit < 10)) {
	    		try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	    		limit++;
	    		Log.i("fataldebug", "no available");
	    	}
	    	Surface s = null;
	    	if(tv.isAvailable())
	    		s = new Surface(tv.getSurfaceTexture()); 
	    	else {
	    		Log.i("fataldebug", "TextureView fail to be available");
	    		return;
	    	}
				     // If play shorter than 5 sec, and not finish
				                                               // playing, we assume user 
				                                               // does not watch it.
				   // Otherwise if play more than 5 sec or finish playing then stop, we assume user played it.
				   
				   //nowPlaying = pos;
				   ;
				   //mMediaPlayer.release();				   
				   //mMediaPlayer = null;
				   //Log.i("MediaPlayermz", "MediaPlayer stopped by " + Thread.currentThread().getId());
			   //}
			   /*else if((pos == nowPlaying)){  // If try to play the same video, just return.
				   // Sometime when flip too fast, will lead thread quit from here, remember to release the mpLock.
				   Log.i("threadmz", Thread.currentThread().getId()+" stopped because dup.");
				   
				   try {
						mpSem.acquire();
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block					
						e1.printStackTrace();
					}
			    	
			        Log.i("mplock", "release lock!");
			    	mpLocked = false;
			    	
			    	mpSem.release();
				   
				    return;
			   }*/
			
			   //nowPlaying = pos;
			  /* Log.i("lockcheck", "After clean lasttime: " + Thread.currentThread().getId());
			   
			   Log.i("threadmz", Thread.currentThread().getId()+" is now playing.");*/
			   // At this point, there is risk of multiple threads compete for one MediaPlayer, fix it later.
		       /*mMediaPlayer= new MediaPlayer();
		       Log.i("MediaPlayermz", "New MediaPlayer " + Thread.currentThread().getId());*/
		       //mMediaPlayer.setDataSource("http://daily3gp.com/vids/747.3gp");
		       //Log.i("MediaPlayermz", "Play " + pos + " item, file name is " + files[pos] + " by " + Thread.currentThread().getId());
		       
		       // In my design, the first time app start, 7 threads incur, when a thread start to download a file, 
		       // other threads will just return and go on try to play, this may lead to a fact that the thread
		       // download one file may not be the one playing it, so maybe one thread try to play a video,
		       // but infact that video is still being download by another thread and no finish yet, so this 
		       // cause setDataSource failed, even though this just happen at the very special beginning once,
		       // I try to finish it by check the video size before setDataSource, if it is still changing, just
		       // wait for a while.
		       File videoFile = new File((String)data.get(pos).get("fileName"));
		       /*long videoSize = videoFile.length();
		       long laterSize = 0;*/
		       /*while(true) {
		    	   try {
					Thread.sleep(500);  // 500 is a enough period to check size change, if smaller, may fail.
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		    	   laterSize = videoFile.length();
		    	   if(videoSize == laterSize)
		    		   break;
		    	   else {
		    		   Log.i("MediaPlayermz", "Not finish downloading " + Thread.currentThread().getId());
		    		   videoSize = laterSize;
		    		   continue;
		    	   }
		       }*/
		       mMediaPlayer = new MediaPlayer();
		       Log.i("deamontest", "start to config mediaplayer.");
		       mMediaPlayer.setDataSource((new FileInputStream(videoFile).getFD()));		       
		       Log.i("MediaPlayermz", "Set data source " + Thread.currentThread().getId());
		       mMediaPlayer.setSurface(s);
		       Log.i("MediaPlayermz", "Set surface" + Thread.currentThread().getId());
		       //mMediaPlayer.prepare();
		       mMediaPlayer.prepareAsync(); 
		       Log.i("MediaPlayermz", "Prepare async " + Thread.currentThread().getId());
		       /*mMediaPlayer.setOnBufferingUpdateListener(this);
		       mMediaPlayer.setOnCompletionListener(this);*/
		       mMediaPlayer.setOnPreparedListener(this);
		       Log.i("MediaPlayermz", "Set prepare listener " + Thread.currentThread().getId());
		       //mMediaPlayer.setOnVideoSizeChangedListener(this);*/
		       mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		       Log.i("MediaPlayermz", "Set audio stream " + Thread.currentThread().getId());
		       //mMediaPlayer.setLooping(true);
		       Log.i("MediaPlayermz", "Quit from " + Thread.currentThread().getId());
		       //mMediaPlayer.start(); // Thread does all the background work, but UI related update should be handled by UI thread.
		       //mMediaPlayer.setOnBufferingUpdateListener(this);
		       Log.i("lockcheck", "Before release lock: " + Thread.currentThread().getId());
		       Log.i("deamontest", "finish config mediaplayer.");
		       
		       //data.get(nowPlaying).put("played", 1);  // Label this video as being played by user.
		       		       
		       // One thread finish the config, then return.
		       /*try {
					mpSem.acquire();
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block					
					e1.printStackTrace();
				}
		    	
		        Log.i("mplock", "release lock!");
		    	mpLocked = false;
		    	
		    	mpSem.release();*/
		       
		} catch (IllegalArgumentException e) {
		        // TODO Auto-generated catch block		    	
		        e.printStackTrace();
		} catch (SecurityException e) {
		        // TODO Auto-generated catch block
		        e.printStackTrace();
		} catch (IllegalStateException e) {
		        // TODO Auto-generated catch block
		        e.printStackTrace();
		} catch (IOException e) {
		        // TODO Auto-generated catch block
		        e.printStackTrace();
		}   	
    }

	@Override
	public void onSurfaceTextureAvailable(SurfaceTexture arg0, int arg1, int arg2) {
		/*Log.i("surfaceavailable", "surface is available");
		// TODO Auto-generated method stub
		 Surface s = new Surface(arg0);
		 //MediaPlayer mMediaPlayer = null;

		 try {
		       mMediaPlayer= new MediaPlayer();
		       //mMediaPlayer.setDataSource("http://daily3gp.com/vids/747.3gp");
		       mMediaPlayer.setDataSource((new FileInputStream((new File("/sdcard/Android/data/co.vine.android/cache/0c42aa3d95a53dd4e88e8a382ae00c77"))).getFD()));		       
		       mMediaPlayer.setSurface(s);
		       mMediaPlayer.prepare();
		       mMediaPlayer.setOnBufferingUpdateListener(this);
		       mMediaPlayer.setOnCompletionListener(this);
		       mMediaPlayer.setOnPreparedListener(this);
		       mMediaPlayer.setOnVideoSizeChangedListener(this);
		       mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		       mMediaPlayer.start();
		      } catch (IllegalArgumentException e) {
		        // TODO Auto-generated catch block
		        e.printStackTrace();
		      } catch (SecurityException e) {
		        // TODO Auto-generated catch block
		        e.printStackTrace();
		      } catch (IllegalStateException e) {
		        // TODO Auto-generated catch block
		        e.printStackTrace();
		      } catch (IOException e) {
		        // TODO Auto-generated catch block
		        e.printStackTrace();
		      }*/   
	}

	@Override
	public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
		Log.i("TestST", "One st destroyed.");
		// TODO Auto-generated method stub
		//Log.i("mz", "destroy");
		//mMediaPlayer.stop();
		if(mMediaPlayer != null)
			mMediaPlayer.release();
		
		threadsRunning = false;
		
		/*if(deamonHandler != null) {
			Message deamonMsg = deamonHandler.obtainMessage();
			deamonMsg.obj = "You should stop.";
			deamonHandler.sendMessage(deamonMsg);
			deamonHandler.getLooper().quit();
		}*/
		//mMediaPlayer.reset();
		//_run = false;
		//mMediaPlayer = null;
		//for(int i = 0; i < allocThreads.size(); i++)
		//	allocThreads.get(i).stop();
		return true;

		//Log.i("mz", "destroy");
		//return true;
	}

	@Override
	public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width,
			int height) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSurfaceTextureUpdated(SurfaceTexture surface) {
		// TODO Auto-generated method stub
		//Log.i("mz", "update");
	}

	/*@Override
	public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
		// TODO Auto-generated method stub
		
	} */

	@Override
	public void onPrepared(MediaPlayer mp) {
		// TODO Auto-generated method stub
		// Let UI thread start mp after when prepare is good idea.
		// And start play on onPrepared is recommended because it is stable.
		mp.start();
		Log.i("deamontest", "Play start by " + Thread.currentThread().getId());
		
		data.get(isplaying).put("played", 1); 
	}

	/* @Override
	public void onCompletion(MediaPlayer mp) {
		// TODO Auto-generated method stub
		// If release at this point, when one item finishes playing, other items will not be played.
		//mp.stop();
		//mp.release();
		//mp = null;
		Log.i("MediaPlayermz", "Play stopped at " + Thread.currentThread().getId());
	}*/

	/*@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		// TODO Auto-generated method stub
		Log.i("trackbuffer", "haha " + percent);
	}*/ 

	//Just for test
	private double speed = 0;
	public void setSpeed(double s) {
		speed = s;
	}
}    
