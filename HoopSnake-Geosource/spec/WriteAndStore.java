package spec;

import DataBase.DBAccess;
import DataBase.Queries;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import ServerClientShared.FieldWithoutContent;
import ServerClientShared.ImageFieldWithoutContent;
import ServerClientShared.StringFieldWithoutContent;
import java.sql.ResultSet;
import java.sql.SQLException;



/**
 * Created by kts192 on 7/03/15.
 * this class writes a FieldWithoutContent to file, naming it in the following
 * format: <username>.specNumber ex: okenso.1
 */
public class WriteAndStore implements Serializable  {
	
    /** takes an arrayed FieldWithoutContent list and writes the object to file. Names it 
     * after the username and specnumber
     * @param fwc the arrayedlist of a FieldWithoutContent class
     * @param chName the name of the channel
     * @param username the owner of the channel
     * @param ispublic whether the channel is private or public */
    //public WriteAndStore(String fwc, String chName, String username, boolean ispublic){ //string version for testing
    public WriteAndStore(ArrayList<FieldWithoutContent> fwc, String chName, String username, boolean ispublic, String[] fields){
        try{
            dbInsertSpec spec = new dbInsertSpec(chName, username, ispublic);
            if (spec.getSpecNum() != -1){ // if it fails, dont write anything.
                FileOutputStream fout = new FileOutputStream("media/spec/" + username + "." + spec.getSpecNum());
                ObjectOutputStream oos = new ObjectOutputStream(fout);   
                oos.writeObject(fwc);
                oos.close();
                this.createPosts(username, chName, fields);
                System.out.println("Done");
            }
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }

    /** Checks if the media/spec folder exists..
     * @return true if there, false otherwise
     * @throws Exception */
    private static boolean FolderExists() throws Exception{
        Path folder = Paths.get("media/spec");
        if (Files.notExists(folder, LinkOption.NOFOLLOW_LINKS)){
            throw new Exception("The media/spec folder does not exist");
        } else {
            return true;
        }
    }
    
    /*  */
    public void createPosts(String ownername, String channelname, String[] fields) throws SQLException{
        //dbAccess.execute(Queries.createPosts(ownername, channelname, fields));
        //ResultSet results = DBAccess.(Queries.createPosts(ownername, channelname, fields));
        DBAccess dba = new DBAccess();
        String sql = Queries.createPosts(ownername, channelname, fields);
        //System.out.println(sql);
        dba.easyQuery(sql);
    }

    /** Tests the functionality of this class */
    public static void main (String[] args) throws Exception{
        System.out.println("oy");
        FolderExists();
        ArrayList<FieldWithoutContent> newSpec = new ArrayList();
        newSpec.add(new StringFieldWithoutContent("TitleText", true));
        newSpec.add(new ImageFieldWithoutContent("PictureField", false));

        String[] fields = {"pic", "video", "audio", "string"};
        WriteAndStore test = new WriteAndStore(newSpec, "march13", "okenso", true, fields); // this tests the string version.


        /*
        dbInsertSpec spec = new dbInsertSpec("pothozlezz", "frank", true);
        System.out.println(spec.getSpecNum());
        */
        //WriteAndStore test = new WriteAndStore("yes", "newname", "okenso", true); // this tests the string version.

    }
}