package com.koushikdutta.urlimageviewhelper.sample;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.androidquery.AQuery;
import com.androidquery.callback.AjaxStatus;
import com.androidquery.callback.BitmapAjaxCallback;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpGet;
import com.koushikdutta.async.http.AsyncHttpResponse;
import com.koushikdutta.async.http.ResponseCacheMiddleware;
import com.koushikdutta.urlimageviewhelper.UrlDownloader;
import com.koushikdutta.urlimageviewhelper.UrlImageViewCallback;
import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;
import com.squareup.picasso.LruCache;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

public class UrlImageViewHelperSample extends Activity {
    // turn a stream into a string
    private static String readToEnd(InputStream input) throws IOException
    {
        DataInputStream dis = new DataInputStream(input);
        byte[] stuff = new byte[1024];
        ByteArrayOutputStream buff = new ByteArrayOutputStream();
        int read = 0;
        while ((read = dis.read(stuff)) != -1)
        {
            buff.write(stuff, 0, read);
        }
        
        return new String(buff.toByteArray());
    }
    
    private ListView mListView;
    private MyAdapter mAdapter;

    private class Row extends ArrayList {
        
    }
    
    private class MyGridAdapter extends BaseAdapter {
        public MyGridAdapter(Adapter adapter) {
            mAdapter = adapter;
            mAdapter.registerDataSetObserver(new DataSetObserver() {
                @Override
                public void onChanged() {
                    super.onChanged();
                    notifyDataSetChanged();
                }
                @Override
                public void onInvalidated() {
                    super.onInvalidated();
                    notifyDataSetInvalidated();
                }
            });
        }
        Adapter mAdapter;
        
        @Override
        public int getCount() {
            return (int)Math.ceil((double)mAdapter.getCount() / 4d);
        }

        @Override
        public Row getItem(int position) {
            Row row = new Row();
            for (int i = position * 4; i < 4; i++) {
                if (mAdapter.getCount() < i)
                    row.add(mAdapter.getItem(i));
                else
                    row.add(null);
            }
            return row;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = getLayoutInflater().inflate(R.layout.row, null);
            LinearLayout row = (LinearLayout)convertView;
            LinearLayout l = (LinearLayout)row.getChildAt(0);
            for (int child = 0; child < 4; child++) {
                int i = position * 4 + child;
                LinearLayout c = (LinearLayout)l.getChildAt(child);
                c.removeAllViews();
                if (i < mAdapter.getCount()) {
                    c.addView(mAdapter.getView(i, null, null));
                }
            }
            
            return convertView;
        }
        
    }

    long start;
    private class MyAdapter extends ArrayAdapter<String> {

        public MyAdapter(Context context) {
            super(context, 0);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = getLayoutInflater().inflate(R.layout.image, null);

            final ImageView iv = (ImageView)convertView.findViewById(R.id.image);
            loader.load(getItem(position), iv);
            return convertView;
        }
    }

    private static interface AbstractImageLoader {
    	public void load(String url, ImageView iv);
    	public void clear();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem legacy = menu.add("UrlImage");
        legacy.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
            	UrlImageViewHelper.getDownloaders().remove(downloader);
            	loader = UrlImageViewHelperSample.this.koush;
                return true;
            }
        });
        MenuItem koush = menu.add("UrlImage+Async");
        koush.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
            	UrlImageViewHelper.getDownloaders().remove(downloader);
            	UrlImageViewHelper.getDownloaders().add(0, downloader);
            	loader = UrlImageViewHelperSample.this.koush;
                return true;
            }
        });
        MenuItem volley = menu.add("Volley");
        volley.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
            	loader = UrlImageViewHelperSample.this.volley;
                return true;
            }
        });
        MenuItem picasso = menu.add("Picasso");
        picasso.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
            	loader = UrlImageViewHelperSample.this.picasso;
                return true;
            }
        });
        MenuItem universal = menu.add("Universal");
        universal.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
            	loader = UrlImageViewHelperSample.this.universal;
                return true;
            }
        });
        MenuItem aquery = menu.add("AQuery");
        aquery.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
            	UrlImageViewHelper.getDownloaders().remove(downloader);
            	loader = UrlImageViewHelperSample.this.aquery;
                return true;
            }
        });
        MenuItem clear = menu.add("clear caches");
        clear.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				mAdapter.clear();
				UrlImageViewHelperSample.this.aquery.clear();
				UrlImageViewHelperSample.this.universal.clear();
				UrlImageViewHelperSample.this.picasso.clear();
				UrlImageViewHelperSample.this.koush.clear();
				UrlImageViewHelperSample.this.volley.clear();
				deleteDirectory(getFilesDir());
				deleteDirectory(new File(getCacheDir(), "volley"));
				deleteDirectory(new File(getCacheDir(), "sample"));
				deleteDirectory(new File(getCacheDir(), "picasso-cache"));
				deleteDirectory(new File(getCacheDir(), "aquery"));
				return true;
			}
		});
        return super.onCreateOptionsMenu(menu);
    }
    
    AbstractImageLoader volley;
	
    private void updateTime() {
    	if (Looper.myLooper() == null) {
    		runOnUiThread(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					updateTime();
				}
			});
    		return;
    	}
		setTitle("" + (System.currentTimeMillis() - start) + "ms");
    }
    
	AbstractImageLoader koush = new AbstractImageLoader() {
		@Override
		public void load(String url, ImageView iv) {
			UrlImageViewHelper.setUrlDrawable(iv, url, R.drawable.loading, new UrlImageViewCallback() {
				@Override
				public void onLoaded(ImageView imageView, Bitmap loadedBitmap, String url, boolean loadedFromCache) {
					updateTime();
				}
			});
		}
		
		@Override
		public void clear() {
			UrlImageViewHelper.clear(UrlImageViewHelperSample.this);
			try {
				rcache.clear();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	};
	
	AbstractImageLoader universal = new AbstractImageLoader() {
		@Override
		public void load(String url, ImageView iv) {
			com.nostra13.universalimageloader.core.ImageLoader.getInstance().displayImage(url, iv,  new ImageLoadingListener() {

				@Override
				public void onLoadingCancelled(String arg0, View arg1) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void onLoadingComplete(String arg0, View arg1,
						Bitmap arg2) {
					updateTime();
					
				}

				@Override
				public void onLoadingFailed(String arg0, View arg1,
						FailReason arg2) {
					updateTime();
					
				}

				@Override
				public void onLoadingStarted(String arg0, View arg1) {
					// TODO Auto-generated method stub
					
				}
				
			});
		}
		
		@Override
		public void clear() {
			com.nostra13.universalimageloader.core.ImageLoader.getInstance().clearDiscCache();
			com.nostra13.universalimageloader.core.ImageLoader.getInstance().clearMemoryCache();
		}
	};
	
	AbstractImageLoader aquery = new AbstractImageLoader() {
		@Override
		public void load(String url, ImageView iv) {
			BitmapAjaxCallback cb =  new BitmapAjaxCallback() {
				@Override
				protected void callback(String url, ImageView iv, Bitmap bm,
						AjaxStatus status) {
					updateTime();
					super.callback(url, iv, bm, status);
				}
				@Override
				public Bitmap getResult() {
					return super.getResult();
				}
			};
			cb.url(url);
			new AQuery(iv).image(cb);
		}
		
		@Override
		public void clear() {
			BitmapAjaxCallback.clearCache();
		}
	};
	
	AbstractImageLoader picasso = new AbstractImageLoader() {
		@Override
		public void load(String url, ImageView iv) {
			Picasso.with(UrlImageViewHelperSample.this).load(url)
				.resize(128, 128) // under the hood, urlimageviewhelper also resizes the image too. picasso hits a oom without this.
				.transform(new Transformation() {
				@Override
				public Bitmap transform(Bitmap arg0) {
					updateTime();
					return arg0;
				}
				
				@Override
				public String key() {
					return "timer";
				}
			}).into(iv);
		}
		
		@Override
		public void clear() {
			try {
				Field cache = Picasso.class.getDeclaredField("cache");
				cache.setAccessible(true);
				LruCache plru = (LruCache)cache.get(Picasso.with(UrlImageViewHelperSample.this));
				plru.evictAll();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	};
    
	static public boolean deleteDirectory(File path) {
		try {
			if (path.exists()) {
				File[] files = path.listFiles();
				if (files != null) {
					for (int i = 0; i < files.length; i++) {
						if (files[i].isDirectory()) {
							deleteDirectory(files[i]);
						} else {
							files[i].delete();
						}
					}
				}
			}
			return (path.delete());
		}
		catch (Exception e) {
			return false;
		}
	}
	
    AbstractImageLoader loader = koush;

    ResponseCacheMiddleware rcache;
	UrlDownloader downloader;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        com.nostra13.universalimageloader.core.ImageLoader.getInstance().init(ImageLoaderConfiguration.createDefault(this));

        volley = new AbstractImageLoader() {
            Hashtable<String, Bitmap> hash = new Hashtable<String, Bitmap>();
            ImageLoader volley = new ImageLoader(Volley.newRequestQueue(UrlImageViewHelperSample.this), new ImageLoader.ImageCache() {
                @Override
                public Bitmap getBitmap(String url) {
                    return hash.get(url);
                }

                @Override
                public void putBitmap(String url, Bitmap bitmap) {
                    hash.put(url, bitmap);
                }
            });

            @Override
    		public void load(String url, final ImageView iv) {
            	volley.get(url, new ImageLoader.ImageListener() {
                  @Override
                  public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                      if (response.getBitmap() != null) {
                          iv.setImageBitmap(response.getBitmap());
                          updateTime();
                      }
                      else {
                          iv.setImageResource(R.drawable.loading);
                      }
                  }

                  @Override
                  public void onErrorResponse(VolleyError error) {
                      iv.setImageResource(R.drawable.loading);
                      updateTime();
                  }
            	});
            }
    		
    		@Override
    		public void clear() {
    			hash.clear();
    		}
    	};

    	// UrlImageViewHelper doesn't use AndroidAsync out of the box. It uses HttpUrlConnection.
    	// Plug in a downloader to use AndroidAsync.
        final AsyncHttpClient client = new AsyncHttpClient(new AsyncServer());
        final Handler handler = new Handler();
        // match picasso's thread pool size.
        final ExecutorService exec = Executors.newFixedThreadPool(3);
        UrlImageViewHelper.getDownloaders().add(0, downloader = new UrlDownloader() {
            final UrlDownloader self = this;
            @Override
            public void download(Context context, String url, final String filename, final UrlDownloaderCallback callback, final Runnable completion) {
                AsyncHttpGet get = new AsyncHttpGet(url);
                get.setHandler(null);
                client.execute(get, filename, new AsyncHttpClient.FileCallback() {
                    @Override
                    public void onCompleted(Exception e, AsyncHttpResponse source, File result) {
                        assert(Looper.myLooper() == null);
                        if (e == null) {
                        	exec.execute(new Runnable() {
                        		@Override
                        		public void run() {
                                    callback.onDownloadComplete(self, null, filename);
                                    handler.post(completion);
                        		}
                        	});
                        	return;
                        }
                        handler.post(completion);
                    }
                });
            }

            @Override
            public boolean allowCache() {
                return false;
            }

            @Override
            public boolean canDownloadUrl(String url) {
                return true;
            }
        });
        try {
        	rcache = ResponseCacheMiddleware.addCache(client, new File(getCacheDir(), "sample"), 24L * 1024L * 1024L);
        }
        catch (Exception e) {
        }

        setContentView(R.layout.main);
        
        final Button search = (Button)findViewById(R.id.search);
        
        mListView = (ListView)findViewById(R.id.results);
        mAdapter = new MyAdapter(this);
        MyGridAdapter a = new MyGridAdapter(mAdapter);
        mListView.setAdapter(a);
        
        search.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            	mAdapter.clear();
            	start = System.currentTimeMillis();
            	for (int i = 0; i < 40; i++) {
            		mAdapter.add("https://raw.github.com/koush/dogs/master/" + i + ".jpg");
            	}
            }
        });
        
    }
}