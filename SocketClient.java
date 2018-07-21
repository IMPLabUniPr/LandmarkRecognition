package advmobdev.unipr.it.landmarkrecognition;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

public class SocketClient extends AsyncTask<Void, Void, String> {
    String dstAddress;
    int dstPort;
    int response = 0;
    float[] descriptor = new float[2048];
    boolean success;
    Context context;

    SocketClient(String addr, int port, float[] descriptor, Context context) {
        dstAddress = addr;
        dstPort = port;
        this.descriptor = descriptor;
        this.context = context;
    }

    @Override
    protected String doInBackground(Void... arg0) {

        Socket socket = null;
        DataInputStream dataInputStream = null;
        DataOutputStream dataOutputStream = null;
        String toSend = "";

        try {
            socket = new Socket(dstAddress, dstPort);
            System.out.println("SOCKET CONNECTED!");

            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataInputStream = new DataInputStream(socket.getInputStream());

            ///////     Conversione per evitare il passaggio in notazione esponenziale via socket      ///////
            for (int i=0; i<descriptor.length; i++) {
                //descriptor[i] = descriptor[i] * 100000.0f;
            }
            ///////////////////////////////////////////////

            //invio descrittore un float alla volta convertendo ogni numero in stringa
            for (int i=0; i<2048; i++) {
                dataOutputStream.writeUTF(String.valueOf(descriptor[i]) + "Q");
                response = dataInputStream.read(); //attending ACK

            }


            if (response == 1 ) {
                success = true;
            } else {
                success = false;
            }


        } catch (java.io.IOException e) {
            e.printStackTrace();
            response = 0;
            success = false;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // close input stream
            if (dataInputStream != null) {
                try {
                    dataInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // close output stream
            if (dataOutputStream != null) {
                try {
                    dataOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (success = true)
            return "Success";
        else
            return "Failed";
    }

    @Override
    protected void onPostExecute(String result) {
        if (success) {
            Toast.makeText(context, "Connection Done", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Unable to connect", Toast.LENGTH_SHORT).show();
        }
    }
}
