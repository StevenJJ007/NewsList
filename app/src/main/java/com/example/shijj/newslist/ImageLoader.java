package com.example.shijj.newslist;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.LruCache;
import android.widget.ImageView;
import android.widget.ListView;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by shijj on 2016/2/17.
 */
public class ImageLoader {

    private ImageView mImageView;
    private String mUrl;

    private LruCache<String, Bitmap> mCache;

    private ListView mListview;
    /*用来存储listview中所有图片的Tag*/
    private Set<NewsAsyncTask> mTask;


    public ImageLoader(ListView listView) {
        mListview = listView;
        mTask = new HashSet<NewsAsyncTask>();
        //获取运行时最大内存
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int cacheSize = maxMemory / 4;

        mCache = new LruCache<String, Bitmap>(cacheSize) {

            @Override
            protected int sizeOf(String key, Bitmap value) {
                //在每次存入缓存的时候调用
                return value.getByteCount();
            }
        };
    }

    /**
     * 增加到缓存
     *
     * @param url
     * @param bitmap
     */
    public void addBitmapToCache(String url, Bitmap bitmap) {
        /*判断缓存中是否存在当前url对应的图片*/
        if (getBitmapFromCache(url) == null) {
            mCache.put(url, bitmap);
        }

    }

    /**
     * 从缓存中换取数据
     *
     * @param url
     * @return
     */
    public Bitmap getBitmapFromCache(String url) {


        return mCache.get(url);
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (mImageView.getTag().equals(mUrl)) {
                mImageView.setImageBitmap((Bitmap) msg.obj);
            }
        }
    };

    public void showImageByThread(ImageView imageView, final String url) {
        mImageView = imageView;
        mUrl = url;

        new Thread() {
            @Override
            public void run() {
                super.run();
                Bitmap bitmap = getBitmapFromUrl(url);
                Message message = Message.obtain();
                message.obj = bitmap;
                mHandler.sendMessage(message);
            }
        }.start();
    }


    public Bitmap getBitmapFromUrl(String urlString) {
        Bitmap bitmap;
        InputStream is = null;
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            is = new BufferedInputStream(connection.getInputStream());
            bitmap = BitmapFactory.decodeStream(is);
            connection.disconnect();

            return bitmap;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;

    }

    public void showImageByAsyncTask(ImageView imageView, String url) {
        /*先判断缓存中是否已经有当前url对应的图片，没有就设置为默认图片，把控制权教给loadImages()方法去下载，有就直接设置bitmap*/
        Bitmap bitmap = getBitmapFromCache(url);
        if (bitmap == null) {
            imageView.setImageResource(R.mipmap.ic_launcher);
        } else {
            imageView.setImageBitmap(bitmap);
        }
    }

    /**
     * 取消所有下载任务
     */
    public void cancelAllTasks() {
        if (mTask != null) {
            for (NewsAsyncTask task : mTask) {
                task.cancel(false);
            }
        }

    }

    /**
     * 用来加载加载从start到end的所有图片
     *
     * @param start
     * @param end
     */
    public void loadImages(int start, int end) {
        for (int i = start; i < end; i++) {
            String url = NewsAdapter.URLS[i];//取出每张图片的地址
             /*先判断缓存中是否已经有当前url对应的图片，没有开启线程下载，有就直接设置bitmap*/
            Bitmap bitmap = getBitmapFromCache(url);
            if (bitmap == null) {
                NewsAsyncTask task = new NewsAsyncTask(url);
                task.execute(url);
                mTask.add(task);
            } else {
                ImageView imageView = (ImageView) mListview.findViewWithTag(url);
                imageView.setImageBitmap(bitmap);
            }
        }
    }

    private class NewsAsyncTask extends AsyncTask<String, Void, Bitmap> {

        // private ImageView mImageView;
        private String mUrl;

        public NewsAsyncTask(String url) {
            //  mImageView = imageView;
            mUrl = url;
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            /*开启线程下载后，为了方便下载好的bitmap以后可以不再下载，可将它添加到缓存中*/
            String url = params[0];
            Bitmap bitmap = getBitmapFromUrl(url);/*从网络获得图片*/
            /*判断图片不为空，就把它添加到缓存中*/
            if (bitmap != null) {
               /*将不在缓存的图片加入图片*/
                addBitmapToCache(url, bitmap);
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            ImageView imageView = (ImageView) mListview.findViewWithTag(mUrl);
            if (imageView != null && bitmap != null) {
                imageView.setImageBitmap(bitmap);
            }
        }
    }
}
