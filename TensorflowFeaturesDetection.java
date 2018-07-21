package advmobdev.unipr.it.landmarkrecognition;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.Operation;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.lang.Math;
import java.util.Arrays;
import java.util.List;

import static java.lang.Math.round;


public class TensorflowFeaturesDetection {

    private AssetManager assetManager;
    private static final String MODEL_NAME = "file:///android_asset/model.pb";
    private static final String INPUT_NAME = "input_1";
    private static final String OUTPUT_NAME = "activation_43/Relu";
    private int[] intValues;
    private float[] floatValues;
    private static final int N_REGIONS = 14;
    private static final int N_CHANNELS = 2048;
    private Context context;

    //.getAssets()
    TensorflowFeaturesDetection(Context context) {
        this.assetManager = context.getAssets();
        this.context = context;
    }

    public float[] detectFeatures(Bitmap bitmapImage) throws IOException, JSONException {
        TensorFlowInferenceInterface inferenceInterface = new TensorFlowInferenceInterface(assetManager, MODEL_NAME);

        //debug
        System.out.println("*****  BITMAP IMAGE  *****");
        System.out.println(bitmapImage.getHeight());
        System.out.println(bitmapImage.getWidth());


        intValues = new int[bitmapImage.getWidth()*bitmapImage.getHeight()];
        floatValues = new float[bitmapImage.getHeight()*bitmapImage.getWidth()*3];
        bitmapImage.getPixels(intValues, 0, bitmapImage.getWidth(), 0, 0, bitmapImage.getWidth(), bitmapImage.getHeight());

        for (int i=0; i<intValues.length; i++) {
            final int val = intValues[i];
            floatValues[i * 3 + 0] = (((val >> 16) & 0xFF));
            floatValues[i * 3 + 1] = (((val >> 8) & 0xFF));
            floatValues[i * 3 + 2] = ((val & 0xFF));
        }

        //debug
        JSONObject jsonObject = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        for (int i=0; i<floatValues.length; i++) {
            jsonArray.put(floatValues[i]);
            //System.out.println(floatValues[i]);
        }
        try {
            jsonObject.put("Output", jsonArray);
            // inserting image size into json data
            jsonObject.put("Width", bitmapImage.getWidth());
            jsonObject.put("Height", bitmapImage.getHeight());
        } catch (JSONException e) {
                    e.printStackTrace();
        }


        System.out.println("******************  JSON OBJECT  ******************");
        System.out.print(jsonObject);



        //new SocketJSONClient("192.168.3.155", 7001,jsonObject,context).execute();
        new SocketJSONClient("192.168.1.101", 7001,jsonObject,context).execute();



        float[] finalVector = new float[2048];


        return finalVector;
    }


    private float[][] getTotalMacVector(float[] outputs, int W, int H) {
        // RMAC Calculation for each 2048 channels
        //float[][] macVector = new float[N_REGIONS+20][N_CHANNELS];
        float[][] macVector = new float[N_REGIONS][N_CHANNELS];

        float[] tempOutput = new float[W*H];
        int index;
        for (int ch=0; ch<N_CHANNELS; ch++) {
            for (int j=0; j<(W*H); j++) {
                index = (ch*(W*H)) + j;
                tempOutput[j] = outputs[index];
            }

            macVector = calculateRMAC(tempOutput,W,H,3,macVector, ch);
        }

        return macVector;
    }


    private float[] getTensorflowOutputs(Bitmap bitmapImage, TensorFlowInferenceInterface inferenceInterface, int numElements) {
        intValues = new int[bitmapImage.getWidth()*bitmapImage.getHeight()];
        floatValues = new float[bitmapImage.getHeight()*bitmapImage.getWidth()*3];
        bitmapImage.getPixels(intValues, 0, bitmapImage.getWidth(), 0, 0, bitmapImage.getWidth(), bitmapImage.getHeight());

        for (int i=0; i<intValues.length; i++) {
            final int val = intValues[i];
            floatValues[i * 3 + 0] = (((val >> 16) & 0xFF));
            floatValues[i * 3 + 1] = (((val >> 8) & 0xFF));
            floatValues[i * 3 + 2] = ((val & 0xFF));
        }

        // Get the tensorflow node
        Operation operation = inferenceInterface.graph().operation(OUTPUT_NAME);

        // Inspect its shape
        final int numClasses = (int) operation.output(0).shape().size(3);
        System.out.println("MODEL LOADED - numClasses: " + numClasses);


        System.out.println("DEBUG - Heigth: " + bitmapImage.getHeight() + "Width: " + bitmapImage.getWidth());
        inferenceInterface.feed(INPUT_NAME, floatValues, 1, bitmapImage.getWidth(), bitmapImage.getHeight(), 3);
        String[] outputNames =  new String[] {"activation_43/Relu"};
        inferenceInterface.run(outputNames, true);


        float[] outputs = new float[numElements]; // manual calcolated
        inferenceInterface.fetch(OUTPUT_NAME, outputs);

        return outputs;
    }

    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        android.graphics.Matrix matrix = new android.graphics.Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return resizedBitmap;
    }



    public float calculateMAC(float[] outputs) {
        float max = Float.MIN_VALUE;
        for (int i=0; i<outputs.length; i++) {
            if (outputs[i] > max)
                max = outputs[i];
        }


        return max;
    }


    public float[][] calculateRMAC(float[] outputs, int W, int H, int L, float[][] R_Mac_Matrix, int ch) {
        int regionIndex = 0;
        for (int l=1; l<L+1; l++) {
            //int dimension = (int)Math.ceil(2*Math.min(W,H)/(l+1));
            int widthRegion = 0, heightRegion = 0;
            int xRegions = 0, yRegions = 0, initialX, initialY, finalX, finalY;
            if (l==1) {
                heightRegion = widthRegion = Math.min(W,H);
                if (W < H) {
                    xRegions = 1;
                    yRegions = 2;
                }
                else {
                    xRegions =2;
                    yRegions = 1;
                }
            }
            else {
                widthRegion = heightRegion = (int)Math.ceil(2*Math.min(W,H)/(l+1));
                if (l==2) {
                    xRegions = 3;
                    yRegions = 2;
                }
                else if (l==3) {
                    xRegions = 2;
                    yRegions = 3;
                }
            }


            if (widthRegion*xRegions < W)
                widthRegion = (int)Math.ceil(W/xRegions);
            if (heightRegion*yRegions < H)
                heightRegion = (int)Math.ceil(H/yRegions);

            float coefW = W / xRegions;
            float coefH = H / yRegions;
            for (int x=0; x<xRegions; x++) {
                for (int y=0; y<yRegions; y++) {
                    initialX = round(x*coefW);
                    initialY = round(y*coefH);
                    finalX = initialX + widthRegion;
                    finalY = initialY + heightRegion;
                    if (finalX > W) {
                        finalX = W;
                        initialX = finalX - widthRegion;
                    }
                    if (finalY > H) {
                        finalY = H;
                        initialY = finalY - heightRegion;
                    }

                    float[] region = regionCrop(outputs, initialX, finalX, initialY, finalY, W, H);

                    R_Mac_Matrix[regionIndex][ch] = calculateMAC(region);

                    regionIndex++;

                }
            }


        }


        // debug
//        System.out.println("*****************************************************************************");
//        System.out.println(regionIndex);
//        System.out.println("*****************************************************************************");
        return R_Mac_Matrix;
    }



    public float[] regionCrop(float[] matrixElements, int initialX, int finalX, int initialY, int finalY, int W, int H) {
        int width = finalX - initialX;
        int height = finalY - initialY;
        int index = 0;
        float[] croppedMatrix = new float[width*height];
        for (int y=0; y<H; y++) {
            for (int x=0; x<W; x++) {
                if (x >= initialX && x < finalX && y >= initialY && y < finalY) {
                    croppedMatrix[index] = matrixElements[y * W + x];
                    index++;
                }
            }
        }

        return croppedMatrix;
    }



    public float[] normalizeL2(float[] vector) {
        // compute vector 2-norm
        float norm2 = 0.0f;
        for (int i = 0; i < vector.length; i++) {
            norm2 += vector[i] * vector[i];
        }
        norm2 = (float) Math.sqrt(norm2);


        for (int i = 0; i < vector.length; i++) {
            vector[i] = vector[i] / norm2;
        }


        return vector;
    }

    public float[][]



    matrixNormalizationL2(float[][] matrix) {
        for (int reg=0; reg<N_REGIONS; reg++) {
            float norm2 = 0.0f;
            for (int ch=0; ch<N_CHANNELS; ch++) {
                norm2 = norm2 + (matrix[reg][ch] * matrix[reg][ch]);
            }
            norm2 = (float) Math.sqrt(norm2);


            for (int ch=0; ch<N_CHANNELS; ch++) {
                matrix[reg][ch] = matrix[reg][ch] / norm2;
            }
        }

        return matrix;
    }

    public float normaL2(ArrayList<Float> vector1, ArrayList<Float> vector2) {
        // compute vector 2-norm
        float norm2 = 0.0f;
        for (int i = 0; i < vector1.size(); i++) {
            norm2 += (vector1.get(i) - vector2.get(i))*(vector1.get(i) - vector2.get(i));
        }
        norm2 = (float) Math.sqrt(norm2);

        return norm2;
    }



    public double[][] matrixFileReading(String filename) {
        float val;
        BufferedReader reader = null;
        double[][] matrix = new double[2048][2048];
        try {
            reader = new BufferedReader(
                    new InputStreamReader(assetManager.open(filename)));

            // do reading, usually loop until end of file reading
            String mLine;
            int height = 0, width = 0, count = 0; // altezza e larghezza matrice
            while ((mLine = reader.readLine()) != null) {
                String[] separated = mLine.split(" ");
                for (int i = 0; i < separated.length; i++) {
                    val = Float.valueOf(separated[i]);
                    matrix[count][i] = val;
                }
                count++;
            }

            return matrix;
        } catch (IOException e) {
            //log the exception
            return matrix;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    //log the exception
                }
            }
            return matrix;
        }
    }



}
