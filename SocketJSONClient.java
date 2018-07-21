package advmobdev.unipr.it.landmarkrecognition;

import android.content.Context;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static java.lang.Thread.sleep;

public class SocketJSONClient extends AsyncTask<Void, Void, String> {
    String dstAddress;
    int dstPort;
    int response = 0;
    JSONObject jsonObject = new JSONObject();
    boolean success;
    Context context;

    SocketJSONClient(String addr, int port, JSONObject jsonObject, Context context) {
        dstAddress = addr;
        dstPort = port;
        this.jsonObject = jsonObject;
        this.context = context;
    }

    @Override
    protected String doInBackground(Void... arg0) {

        DataInputStream is;
        DataOutputStream os;

        try {
            Socket socket = new Socket(dstAddress, dstPort);
//            is = new DataInputStream(socket.getInputStream());
            os = new DataOutputStream(socket.getOutputStream());
            PrintWriter pw = new PrintWriter(os);
            pw.println(jsonObject.toString());
            System.out.println("Send to the socket jsonObject.");
            pw.flush();

//            BufferedReader in = new BufferedReader(new InputStreamReader(is));
//            String response = in.readLine();
//            System.out.println("Response: " + response);
//            is.close();
            os.close();

        } catch (IOException e) {
            e.printStackTrace();
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
