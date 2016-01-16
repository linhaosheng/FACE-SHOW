package com.example.administrator.face_show;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.facepp.error.FaceppParseException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;


public class MyActivity extends Activity implements View.OnClickListener{

    private ImageView mPhoto;
    private Button mGetImage;
    private TextView mTip;
    private Button mDetect;
    private View mWaiting;
    private String cuttentImageDir;
    private static final int PIC_CODE=0x11;
    private Bitmap mPhotoImg;
    private static final int MSG_SUCCESS=0X12;
    private static final int MSG_ERROR=0X13;
    private static final int MSG_CAMERA=0x14;
    Paint mPaint=null;
    private LayoutInflater inflater=null;
    private  String mCameraPath=null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        initViews();
        initEvent();
        mPaint=new Paint();
    }

private void initViews(){
    mDetect=(Button)findViewById(R.id.id_detect);
    mTip=(TextView)findViewById(R.id.id_tip);
    mGetImage=(Button)findViewById(R.id.id_getImage);
    mPhoto=(ImageView)findViewById(R.id.id_photo);
    mWaiting=(View)findViewById(R.id.id_waiting_);
}
    private void initEvent(){
        mGetImage.setOnClickListener(this);
        mDetect.setOnClickListener(this);
    }

    private void initDialog(){
        inflater=LayoutInflater.from(this);
        View dialog =inflater.inflate(R.layout.device_dialog,null);
        Button camera=(Button)dialog.findViewById(R.id.camera);
        Button photo=(Button)dialog.findViewById(R.id.photo);

        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setView(dialog);
        builder.show();
        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               Intent intent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                mCameraPath= "mnt/sdcard/DCIM/Camera/" + getPhotoFileName();
                File mCamera=new File(mCameraPath);

                    if (!mCamera.exists()) {
                        try {
                               mCamera.createNewFile();
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
               intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mCamera));
                startActivityForResult(intent,MSG_CAMERA);
            }
        });
        photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent=new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent,PIC_CODE);
            }
        });
    }

    /**
     * 根据时间生成照片的名称
     * @return
     */
    private String getPhotoFileName(){
        Date date=new Date(System.currentTimeMillis());
        SimpleDateFormat dateFormat=new SimpleDateFormat("'IMG'_yyyyMMdd_HHmmss");
        return dateFormat.format(date) + ".jpg";
    }
private Handler handler=new Handler(){
    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        switch (msg.what){
            case MSG_SUCCESS:
                mWaiting.setVisibility(View.GONE);
                JSONObject jsonObject=(JSONObject)msg.obj;
                preparRsBitmap(jsonObject);
                mPhoto.setImageBitmap(mPhotoImg);
                break;
            case MSG_ERROR:
                mWaiting.setVisibility(View.GONE);
                String error=(String)msg.obj;
                if(TextUtils.isEmpty(error)){
                    mTip.setText("Error");
                }else {
                    mTip.setText(error);
                }
                break;
        }
    }
};

    /**
     * 解析JSONObject
     * @param jsonObject
     */
    private void  preparRsBitmap(JSONObject jsonObject){
        Bitmap bitmap=Bitmap.createBitmap(mPhotoImg.getWidth(),mPhotoImg.getHeight(),mPhotoImg.getConfig());
        Canvas canvas=new Canvas(bitmap);
        mPaint.setColor(getResources().getColor(R.color.white));
        mPaint.setStrokeWidth(3);
        canvas.drawBitmap(mPhotoImg,0,0,null);
    try{
        JSONArray array=jsonObject.getJSONArray("face");
        int faceCount=array.length();
        for(int i=0;i<faceCount;i++){
            //单独拿到face对象
            JSONObject face=array.getJSONObject(i);
            JSONObject posObj=face.getJSONObject("position");

            float x=(float)posObj.getJSONObject("center").getDouble("x");
            float y=(float)posObj.getJSONObject("center").getDouble("y");

            float w=(float)posObj.getDouble("width");
            float h=(float)posObj.getDouble("height");

             x=x/100 *bitmap.getWidth();
            y=y/100 *bitmap.getHeight();

            w=w/100 *bitmap.getWidth();
            h=h/100 *bitmap.getHeight();

            //画box
            canvas.drawLine(x-w/2,y-h/2,x-w/2,y+h/2,mPaint);
            canvas.drawLine(x-w/2,y-h/2,x+w/2,y-h/2,mPaint);
            canvas.drawLine(x+w/2,y-h/2,x+w/2,y+h/2,mPaint);
            canvas.drawLine(x-w/2,y+h/2,x+w/2,y+h/2,mPaint);

            int age=face.getJSONObject("attribute").getJSONObject("age").getInt("value");
            String gender=face.getJSONObject("attribute").getJSONObject("gender").getString("value");

            Bitmap ageBitmap=builAgeBitmap(age,"Male".equals(gender));

            int ageWith=ageBitmap.getWidth();
            int ageHeigth=ageBitmap.getHeight();
            /**
             * 设置TextView的宽度，防止原来的图片过小的遮挡住了
             */
            if(bitmap.getWidth()<mPhoto.getWidth() && bitmap.getHeight()<mPhoto.getHeight()){
                float radio=Math.max(bitmap.getWidth() * 1.0f /mPhoto.getWidth(), bitmap.getHeight() * 1.0f/mPhoto.getHeight());
                 ageBitmap=Bitmap.createScaledBitmap(ageBitmap,(int)(ageWith*radio),(int)(ageHeigth*radio),false);
            }

            canvas.drawBitmap(ageBitmap,x-ageBitmap.getWidth()/2, y-h/2-ageBitmap.getHeight(),null);
            mPhotoImg=bitmap;
        }
    }catch (JSONException e){
        e.printStackTrace();
    }

    }

    /**
     * 通过age 和gender  来构建一张图片
     * @param age
     * @param ismale
     * @return
     */
    private Bitmap builAgeBitmap(int age,boolean ismale){
       TextView tv=(TextView)mWaiting.findViewById(R.id.id_age_and_gender);
        tv.setText(age+"");
        if(ismale){
           tv.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.male1),null,null,null);
        }else {
           tv.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.female1),null,null,null);
        }
        tv.setDrawingCacheEnabled(true);
        Bitmap bitmap=Bitmap.createBitmap(tv.getDrawingCache());
        tv.destroyDrawingCache();
        return  bitmap;
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.id_detect:
                mWaiting.setVisibility(View.VISIBLE);
                if (cuttentImageDir !=null && !cuttentImageDir.trim().equals("")){
                    resizePhoto();
                }else {
                    mPhotoImg=BitmapFactory.decodeResource(getResources(),R.drawable.t4);
                }
                FaceDetect.detect(mPhotoImg,new FaceDetect.CallBack() {
                    @Override
                    public void success(JSONObject jsonObject) {
                        Message msg=Message.obtain();
                        msg.what=MSG_SUCCESS;
                        msg.obj=jsonObject;
                        handler.sendMessage(msg);
                    }

                    @Override
                    public void error(FaceppParseException e) {
                        Message msg=Message.obtain();
                        msg.what=MSG_ERROR;
                        msg.obj=e.getErrorMessage();
                        handler.sendMessage(msg);
                    }
                });
                break;
            case R.id.id_getImage:
               initDialog();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==PIC_CODE && requestCode==Activity.RESULT_OK){
            if(data!=null){
                Uri uri=data.getData();
                Cursor cursor=getContentResolver().query(uri,null,null,null,null);
                cursor.moveToFirst();

                int index=cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                cuttentImageDir=cursor.getString(index);
                cursor.close();
                resizePhoto();
                mPhoto.setImageBitmap(mPhotoImg);
                mTip.setText("Click Detect--->");
            }
        }
      if(requestCode==MSG_CAMERA && requestCode==Activity.RESULT_OK){
           if(data!=null) {
             mPhotoImg=BitmapFactory.decodeFile(mCameraPath,null);
               cuttentImageDir=mCameraPath;
               resizePhoto();
             mPhoto.setImageBitmap(mPhotoImg);
           }
       }

    }
    //压缩图库里的图片
    private void resizePhoto(){
        BitmapFactory.Options options=new BitmapFactory.Options();
        options.inJustDecodeBounds=true;

        BitmapFactory.decodeFile(cuttentImageDir,options);
        double radio=Math.max(options.outWidth*1.0d/1024,options.outHeight*1.0d/1024);
        options.inSampleSize=(int)Math.ceil(radio);
        options.inJustDecodeBounds=false;
       mPhotoImg=BitmapFactory.decodeFile(cuttentImageDir,options);
    }


}
