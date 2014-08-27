package com.example.vine;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends ListActivity implements OnScrollListener{
	private ListView mListView = null;
	private int vLastItem = -1;
	//private int vItemCount = -1;
	private View loadMoreView = null;
	private TextView loadMoreText = null;
	private Handler mHandler = new Handler();
	MyAdapter adapter = null;
	boolean loading = false;
	
	//Test scroll speed.
	private int preFirstVisibleItem = 0;
	private long preEventTime = 0;
	//private int preBottomPos = 0;
	public double speed = 0;
	//private long lastSampleTime = 0;
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// Create dir to store video files.
		File vdir = new File("/sdcard/myVine");  // Let java handle it.
		if(!vdir.exists())
		    vdir.mkdirs();
		
		//loadMoreView = getLayoutInflater().inflate(R.layout.load_more_view, null);  
		loadMoreView = ((LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.load_more_view, null, false);
        loadMoreText = (TextView)loadMoreView.findViewById(R.id.loadmoretext);
		mListView = getListView();
		mListView.addFooterView(loadMoreView); // Footer must be added before set adapter, otherwise you can't 
		                                       // see it.
		/*ListView listview = getListView();
		listview.requestFocusFromTouch();
	    listview.setOnItemClickListener(new OnItemClickListener() {  
            public void onItemClick(AdapterView<?> parent, View view,  
                    int position, long id) {  
            	 Log.i("mz", "playing");
        		VideoView video = (VideoView)view.findViewById(R.id.videoview);
        		video.setVideoURI(Uri.parse("http://clips.vorwaerts-gmbh.de/VfE_html5.mp4"));
                video.setMediaController(new MediaController(MainActivity.this));  
                video.requestFocus();  
                video.start();
               
            }  
        });  */
					
		adapter = new MyAdapter(this, mListView);
		setListAdapter(adapter);
	    mListView.setOnScrollListener(this);
	    		    
	    /* Width 720 Height 1280
	     * DisplayMetrics metrics = new DisplayMetrics();
	    getWindowManager().getDefaultDisplay().getMetrics(metrics);
	    int screenWidth=metrics.widthPixels;            //屏幕宽度
	    int screenHeight=metrics.heightPixels;        //屏幕高度
	    Log.i("screen", " "+screenWidth+" "+screenHeight);*/
	    
	    // Can use below function to find where is the apk installed, now it is at /data/data/com.example.vine/files,
	    // so it is installed at phone, not sdcard, maybe can install it at sdcard, check it later.
	    /*File f = this.getFilesDir();
        System.out.println(f.getAbsolutePath());*/
	}
	
	/*protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		VideoView video = (VideoView)v.findViewById(R.id.videoview);
		video.setVideoURI(Uri.parse("http://clips.vorwaerts-gmbh.de/VfE_html5.mp4"));
        video.setMediaController(new MediaController(MainActivity.this));  
        video.requestFocus();  
        video.start();
		TextView text = (TextView)v.findViewById(R.id.title);
		String tt = (String) text.getText();
        Log.i("mz", "playing"+tt);
    }*/

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub			
		// Clear all video files.
		File dir = new File("/sdcard/myVine");
		File files[] = dir.listFiles();
		for(int i = 0; i < files.length; i++)
			files[i].delete();
		
		// Log statistics into Stat file.
		String statFileName = "/sdcard/myVine/Stat";
		FileOutputStream outStream = null;
		try {
			//outStream = mContext.openFileOutput(statFileName,Context.MODE_PRIVATE);
			outStream = new FileOutputStream(statFileName);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < adapter.data.size(); i++)
			sb.append(adapter.data.get(i).get("played") + " " + "qid:1" 
					/*+ " fileName:" + (String)adapter.data.get(i).get("fileName") 
					+ " description:" + (String)adapter.data.get(i).get("description") 
					+ " videoUrl:" + (String)adapter.data.get(i).get("videoUrl") 
					+ " comments:" + (String)adapter.data.get(i).get("comments") 
					+ " created:" + (String)adapter.data.get(i).get("created") 
					+ " foursquareVenueId:" + (String)adapter.data.get(i).get("foursquareVenueId") 
					+ " liked:" + (String)adapter.data.get(i).get("liked") 
					+ " likes:" + (String)adapter.data.get(i).get("likes") 
					+ " postId:" + (String)adapter.data.get(i).get("postId") 
					+ " postToFacebook:" + (String)adapter.data.get(i).get("postToFacebook") 
					+ " promoted:" + (String)adapter.data.get(i).get("promoted") 
					+ " tags:" + (String)adapter.data.get(i).get("tags") 
					+ " userId:" + (String)adapter.data.get(i).get("userId") 
					+ " username:" + (String)adapter.data.get(i).get("username") 					
					+ "\n");*/	
					
					+ " 1:" + (String)adapter.data.get(i).get("comments") 										 
					+ " 2:" + (String)adapter.data.get(i).get("liked") 
					+ " 3:" + (String)adapter.data.get(i).get("likes") 					 
					//+ " 4:" + (String)adapter.data.get(i).get("postToFacebook") 
					+ " 5:" + (String)adapter.data.get(i).get("promoted") 	
					+ " 6:" + (String)adapter.data.get(i).get("reposts")
					+ " 7:" + (String)adapter.data.get(i).get("totalTagCounts")
					+ " 8:" + (String)adapter.data.get(i).get("followerCount")
					+ " 9:" + (String)adapter.data.get(i).get("verified")
					+ " 10:" + (String)adapter.data.get(i).get("authoredPostCount")
					+ " 11:" + (String)adapter.data.get(i).get("privateT")
					+ " 12:" + (String)adapter.data.get(i).get("likeCount")
					+ " 13:" + (String)adapter.data.get(i).get("following")
					+ " 14:" + (String)adapter.data.get(i).get("postCount")
					+ " 15:" + (String)adapter.data.get(i).get("followingCount")
					+ " 16:" + (String)adapter.data.get(i).get("explicitContent")
					+ " 17:" + (String)adapter.data.get(i).get("blocking")
					+ " 18:" + (String)adapter.data.get(i).get("blocked")
					+ "\n");
		
		try {
			outStream.write(sb.toString().getBytes());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			outStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Upload Stat file to server. Just leave the file there, at first I try to delete all files at the end,
		// but because upload is executed by a child thread, so err occurs because this file may have already be
		// deleted by main thread before child thread upload it.
		HttpOperation.uploadStatistics("/sdcard/myVine/Stat");
		
	    super.onDestroy();
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		// TODO Auto-generated method stub
        //vItemCount = visibleItemCount;  
		
        vLastItem = firstVisibleItem + visibleItemCount - 1; 
        Log.i("trackscroll", "first:"+firstVisibleItem+" visible:"+visibleItemCount+" total"+totalItemCount);
        
        //Test scroll speed of changing one item.
        if(preFirstVisibleItem != firstVisibleItem) {
        	long currTime = System.currentTimeMillis();
        	long timeToScrollOneItem = currTime - preEventTime;
        	speed = ((double)1/timeToScrollOneItem) * 1000;
        	adapter.setSpeed(speed);
        	preFirstVisibleItem = firstVisibleItem;
        	preEventTime = currTime;
        	Log.i("ScrollSpeed", " " + speed + " ");
        }
        
        //Another way to test speed.
        /*long sampleTime = System.currentTimeMillis();
        if((sampleTime - lastSampleTime) > 500) {
        	lastSampleTime = sampleTime;*/
        	
	        /*if(preFirstVisibleItem == firstVisibleItem) {
	        	View firstView = mListView.getChildAt(firstVisibleItem);
	        	if(firstView != null) {
		        	int bottomPos = firstView.getBottom();
		        	int moveDis = preBottomPos - bottomPos;
		        	long currTime = System.currentTimeMillis();
		        	long timeDis = currTime - preTime;
		        	if(timeDis != 0)
		        		speed = Math.abs((double)moveDis/timeDis);
		        	Toast.makeText(getApplicationContext(), " " + speed,
		        		     Toast.LENGTH_SHORT).show();
		        	if(speed > 0.5){
		        		Log.i("movespeed", "speed is high");
		        		Toast.makeText(getApplicationContext(), " " + speed,
			        		     Toast.LENGTH_SHORT).show();
		        	}
		        	preBottomPos = bottomPos;
		        	preTime = currTime;
	        	}
	        }
	        else {
	        	View firstView = mListView.getChildAt(firstVisibleItem);
	        	if(firstView != null) {
		        	preBottomPos = firstView.getBottom();
		        	preTime = System.currentTimeMillis();
	        	}
	        }*/
	        
        	
              
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		// TODO Auto-generated method stub
        // 不滚动时保存当前滚动到的位置  
		/* Used to calculate item position. */
        if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {  
             /*int index = mListView.getFirstVisiblePosition();
             View top = mListView.getChildAt(index);
             int[] loc = new int[2];
             top.getLocationOnScreen(loc);
             Log.i("screen", "firstitem:" + index + " x:" + loc[0] + " y:" + loc[1]); //+" firstitemtop"+top);*/ 
        	
             int index = mListView.getFirstVisiblePosition();  // Can we use AbsListView view?
             View currentTopView = mListView.getChildAt(index);
             int transpose = 0;
             
             while(currentTopView == null)  // Sometime you maybe fail to get the view, so we get above view's
            	                            // top and bottom, and then use transpose to calculate the view's top
            	                            // and bottom we want.
             {
                 transpose++;
                 currentTopView = mListView.getChildAt(index - transpose); //What if index-transpose < 0?
             }
             
             int botom = currentTopView.getBottom();
             int top = currentTopView.getTop();
             int height = currentTopView.getHeight();
             botom = botom + height * (transpose - index);
             top = top + height * (transpose - index);             
             //Log.i("location", "first:"+index+" top:"+top+" bottom:"+botom);
        }
        /* End */
                         
        /* Used to load more items. */
        int lastItem = adapter.getCount() - 1;    //数据集最后一项的索引  
        lastItem = lastItem + 1;            //加上底部的loadMoreView项  
        Log.i("loadmore", "vLastItem: "+vLastItem+" lastItem: "+lastItem);
        if (scrollState == OnScrollListener.SCROLL_STATE_IDLE && vLastItem == lastItem) {  
            //如果是自动加载,可以在这里放置异步加载数据的代码  
            //Log.i("LOADMORE", "loading...");  
        	// In case sometimes when you scroll to the bottom, this event will be triggered several times,
        	// we just want to load once. But below code might not be very thread safe, check it later.
        	if(loading == true)
        		return;
            Log.i("loadmore", "Am I here?");
            loadMore();
        }
        /* End. */      
	}
	
	private void loadMore() {
		loadMoreText.setText("Loading..."); //设置按钮文字loading  
		loading = true;
        mHandler.postDelayed(new Runnable() {  
            @Override  
            public void run() {    
            	Log.i("debugloadmore", "Before end adapter.loadMoreData().");
                boolean more = adapter.loadMoreData();    
                if(more) {
	                adapter.notifyDataSetChanged(); //数据集变化后,通知adapter  
	                //mListView.setSelection(vLastItem - 1); //设置选中项     
	                mListView.setSelection(vLastItem);
	                loadMoreText.setText("More");  //恢复按钮文字
	                loading = false;
                }
                else {
                	mListView.setSelection(vLastItem - 1); //设置选中项                    
	                loadMoreText.setText("No More Record");  //恢复按钮文字
	                //loading = false; // No more load will happen again.
                }
            }  
        }, 1000);
	}

}
