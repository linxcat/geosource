package Communication;

import Control.Controller;
import ServerClientShared.Commands.IOCommand;
import ServerClientShared.FieldWithContent;
import ServerClientShared.FieldWithoutContent;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;

/**
 * 
 * @author Connor
 */
public class CommSocket implements Callable<Future<FieldWithContent>>{
    
    private static final int portNum = 0;
    
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
    
    public Future<FieldWithContent> call()
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
            OutputStream outStream = clientSocket.getOutputStream();
            InputStream inStream = clientSocket.getInputStream();
            System.out.println("Client Connected");
            
            CipherOutputStream cipherOut = new CipherOutputStream(outStream, desCipher);
            GZIPOutputStream zipOut = new GZIPOutputStream(cipherOut);
            out = new ObjectOutputStream(zipOut);
            
            CipherInputStream cipherIn = new CipherInputStream(inStream, desCipher);
            GZIPInputStream zipIn = new GZIPInputStream(cipherIn);
            in = new ObjectInputStream(zipIn);
            
            //Read command
            IOCommand command = (IOCommand)in.readObject();
            
            //Read/Send appropriate data
            switch (command)
            {
                case GetForm:
                {
                    String channelName = in.readUTF();
                    ArrayList<FieldWithoutContent> formList = controller.getForm(channelName);
                    out.writeObject(formList);
                    break;
                }
                case SendIncident:
                {
                    break;
                }
            }
        }
        catch (IOException IOe)
        {
            System.out.println("IO Exception: " + IOe.getMessage());
        }
        catch (InvalidKeyException IKe)
        {
            System.out.println("invalid encryption password");
        }
        catch (InvalidKeySpecException IKSe) {
            System.out.println("invalid key specification");
        }
        catch (NoSuchAlgorithmException NSAe) {
            System.out.println("invalid encryption algorithm");
        }
        catch (NoSuchPaddingException NSPe) {
            System.out.println("No Such Padding");
        }
        finally
        {
            return null;
        }
    }
}
