package com.example.administrator.face_show;

import android.graphics.Bitmap;
import android.provider.ContactsContract;
import android.util.Log;

import com.facepp.error.FaceppParseException;
import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;

import org.apache.http.HttpRequest;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;

/**
 * Created by Administrator on 2015/10/9.
 */
public class FaceDetect {

    public interface CallBack{
        void success(JSONObject jsonObject);
        void error(FaceppParseException e);
    }

    public static void detect(Bitmap bitmap,final CallBack callBack){

           final HttpRequests requests = new HttpRequests(Constant.Key, Constant.Secret, true, true);
            Bitmap bitmap1 = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight());
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap1.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            byte[] bytes = stream.toByteArray();
          final  PostParameters parameters=new PostParameters();
            parameters.setImg(bytes);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                    JSONObject jsonObject= requests.detectionDetect(parameters);
                        System.out.println("TAG-----"+jsonObject.toString());
                        //   Log.e("TAG-----",jsonObject.toString());
                        if(callBack!=null){
                            callBack.success(jsonObject);
                        }
                    }catch (FaceppParseException e){
                        e.printStackTrace();
                        if(callBack!=null){
                            callBack.error(e);
                        }
                        System.out.println("ErrorMessage-----"+e.getErrorMessage());
                    }
                }
            }).start();
    }
}
