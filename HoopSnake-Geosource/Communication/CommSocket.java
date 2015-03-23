package Communication;

import Control.Controller;
import ServerClientShared.Commands.IOCommand;
import ServerClientShared.FieldWithoutContent;
import ServerClientShared.Incident;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import javax.crypto.NoSuchPaddingException;

/**
 * 
 * @author Connor
 */
public class CommSocket implements Callable<Incident>{
    
    public static int portNum = 0;
    private final static int compressionBlockSize = 1024; //indicates the size in bytes of the blocks that are sent over the stream
    
    private static Controller controller = null;

    private ObjectOutputStream out;
    private ObjectInputStream in;

    protected static ServerSocket serverSocket = null;
    private Socket clientSocket;
    
    // Password must be at least 8 characters
    private static final String password = "hiedlbrand";
    
    public CommSocket(Controller parentControl)
    {
        if (serverSocket == null)
        {
            try
            {
                serverSocket = new ServerSocket(portNum); //global socket bind
            }
            catch (IOException IOe)
            {
                System.out.println("Binding server Socket Failed");
            }
        }
        
        if (controller == null)
        {
            controller = parentControl; //controller to query for data/files
        }
    }
    
    /**
     * a function often called by an executor that leaves behind a possibly
     * filled future, allowing later checking for data once the task completes
     * @return any Incident submission returned by a connection to an android app
     */
    @Override
    public Incident call()
    {
        try
        {
            // Create Key
            byte key[] = password.getBytes();
            DESKeySpec desKeySpec = new DESKeySpec(key);
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey secretKey = keyFactory.generateSecret(desKeySpec);

            // Create Cipher
            Cipher desCipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
            desCipher.init(Cipher.ENCRYPT_MODE, secretKey);
            
            // Bind client and create streams
            clientSocket = serverSocket.accept();
            
            
            System.out.println("Client Connected");
            
            OutputStream outStream = clientSocket.getOutputStream();
            InputStream inStream = clientSocket.getInputStream();
            //CipherOutputStream cipherOut = new CipherOutputStream(outStream, desCipher);
            //CipherInputStream cipherIn = new CipherInputStream(inStream, desCipher);
            //CompressedBlockOutputStream zipOut = new CompressedBlockOutputStream(cipherOut, compressionBlockSize);
            //CompressedBlockInputStream zipIn = new CompressedBlockInputStream(cipherIn);
            out = new ObjectOutputStream(outStream);
            in = new ObjectInputStream(inStream);
            out.flush();
            
            //Read command
            IOCommand command = (IOCommand)in.readObject();
            
            //Read/Send appropriate data
            switch (command)
            {
                case GET_FORM:
                {
                    String channelName = in.readUTF();
                    String ownerName = in.readUTF();
                    ArrayList<FieldWithoutContent> formList = controller.getForm(channelName, ownerName);
                    out.writeObject(formList);
                    out.flush();
                    return null;
                }
                case SEND_INCIDENT:
                {
                    Incident newIncident = (Incident)in.readObject();
                    return newIncident;
                }
            }
            clientSocket.close();
        }
        catch (IOException IOe)
        {
            System.err.println("IO Exception: ");
            IOe.printStackTrace();
        }
        catch (InvalidKeyException IKe)
        {
            System.err.println("invalid encryption password");
        }
        catch (InvalidKeySpecException IKSe) {
            System.err.println("invalid key specification");
        }
        catch (NoSuchAlgorithmException NSAe) {
            System.err.println("invalid encryption algorithm");
        }
        catch (NoSuchPaddingException NSPe) {
            System.err.println("No Such Padding");
        }
        catch (Exception e){
            e.printStackTrace();
        }
        
        return null;

    }
}