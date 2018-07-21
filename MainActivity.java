package advmobdev.unipr.it.landmarkrecognition;

import android.Manifest;
import android.support.v4.content.FileProvider;
import android.os.Bundle;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.app.Activity;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends Activity{

    ImageView viewImage;
    Button b, btn_invia;
    String mCurrentPhotoPath;
    float[] imageDescriptor = new float[2048];
    // second image
    float[] imageDescriptor2 = new float[2048];
    public static final int CAMERA_PICTURE = 1;
    public static final int IMAGE_FROM_GALLERY = 2;

    private Bitmap bitmapImage;
    private Bitmap bitmapImage2;

    /*
     * getter e setter Bitmap Image
     */
    public Bitmap getBitmapImage() {
        return bitmapImage;
    }
    public void setBitmapImage(Bitmap bitmapImage) {
        this.bitmapImage = bitmapImage;
    }
    public Bitmap getBitmapImage2() {
        return bitmapImage2;
    }
    public void setBitmapImage2(Bitmap bitmapImage) {
        this.bitmapImage2 = bitmapImage;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        b=(Button)findViewById(R.id.btnSelectPhoto);
        btn_invia=(Button)findViewById(R.id.button_invia);
        viewImage=(ImageView)findViewById(R.id.viewImage);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImage();
            }
        });
        btn_invia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    try {
                        imageDescriptor = detectFeatures(getBitmapImage());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    ArrayList<Float> desc_1 = new ArrayList<>();
                    for (int i=0; i<2048; i++) {
                        desc_1.add(imageDescriptor[i]);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                /*********    SOCKET CLIENT    ***************************************/
                //new SocketClient("192.168.1.101", 7001,imageDescriptor, getApplicationContext()).execute();
                new SocketClient("192.168.3.155", 7001,imageDescriptor, getApplicationContext()).execute();


            }
        });
        btn_invia.setVisibility(View.GONE);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds options to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private float[] detectFeatures(Bitmap bitmap) throws IOException, JSONException {
        float[] descriptor = new float[2048];
        TensorflowFeaturesDetection featuresDetection = new TensorflowFeaturesDetection(getApplicationContext());
        descriptor = featuresDetection.detectFeatures(bitmap);
        return descriptor;
    }

    private void selectImage() {
        final CharSequence[] options = { "Scatta Foto", "Scegli da Galleria","Annulla" };

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Inserisci Foto!");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (options[item].equals("Scatta Foto"))
                {
                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    // Ensure that there's a camera activity to handle the intent
                    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                        // Create the File where the photo should go
                        File photoFile = null;
                        try {
                            photoFile = createImageFile();
                        } catch (IOException ex) {
                            // Error occurred while creating the File
                            System.out.println("ERRORS during file creations");
                        }
                        // Continue only if the File was successfully created
                        if (photoFile != null) {
                            Uri photoURI = FileProvider.getUriForFile(MainActivity.this,
                                    "advmobdev.unipr.it.fileprovider",
                                    photoFile);
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                            startActivityForResult(takePictureIntent, 1);
                        }
                    }

                    btn_invia.setVisibility(View.VISIBLE);







                }
                else if (options[item].equals("Scegli da Galleria"))
                {

                    Intent intent = new   Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(intent, 2);

                    btn_invia.setVisibility(View.VISIBLE);
                }
                else if (options[item].equals("Annulla")) {
                    dialog.dismiss();
                }
            }
        });

        builder.show();
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {


        if (requestCode == CAMERA_PICTURE && resultCode == RESULT_OK) {
            /**********  GALLERY ADD PICTURE  *************/
            /**********************************************/
            viewImage.setDrawingCacheEnabled(true);
            Bitmap b = viewImage.getDrawingCache();
            MediaStore.Images.Media.insertImage(MainActivity.this.getContentResolver(), b,"Photo", "Prova");


            /*****************************************/
            /***********  SET PICTURE  ***************/
            // Get the dimensions of the View
            int targetW = viewImage.getWidth();
            int targetH = viewImage.getHeight();

            // Get the dimensions of the bitmap
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
            int photoW = bmOptions.outWidth;
            int photoH = bmOptions.outHeight;

            // Determine how much to scale down the image
            int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

            // Decode the image file into a Bitmap sized to fill the View
            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = scaleFactor;
            bmOptions.inPurgeable = true;

            Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
            viewImage.setImageBitmap(bitmap);
            setBitmapImage(bitmap);


        }
        if (requestCode == IMAGE_FROM_GALLERY) {
            System.out.println("DEBUG - Request for gallery image!");

            Uri uri = data.getData();
            String[] prjection ={MediaStore.Images.Media.DATA};
            Cursor cursor=getContentResolver().query(uri,prjection,null,null,null);
            cursor.moveToFirst();

            int columnIndex=cursor.getColumnIndex(prjection[0]);
            String path=cursor.getString(columnIndex);
            cursor.close();

            Bitmap selectFile = BitmapFactory.decodeFile(path);
            viewImage.setImageBitmap(selectFile);
            setBitmapImage(selectFile);
        }
        // second image
        if (requestCode == 8) {
            System.out.println("DEBUG - Request for second gallery image!");

            Uri uri = data.getData();
            String[] prjection ={MediaStore.Images.Media.DATA};
            Cursor cursor=getContentResolver().query(uri,prjection,null,null,null);
            cursor.moveToFirst();

            int columnIndex=cursor.getColumnIndex(prjection[0]);
            String path=cursor.getString(columnIndex);
            cursor.close();

            Bitmap selectFile = BitmapFactory.decodeFile(path);
            viewImage.setImageBitmap(selectFile);
            setBitmapImage2(selectFile);
        }
    }



    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        //File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }



}
