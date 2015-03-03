package hoopsnake.geosource.data;
//TODO All the Field classes are now shared. remove them and import instead. This one should be converted into a subclass of the shared one, using Uris.
import android.net.Uri;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import hoopsnake.geosource.media.SerialBitmap;

import static junit.framework.Assert.assertNotNull;

/**
 * Created by wsv759 on 18/02/15.
 *
 * A single field within an incident.
 * Includes title, type, and a content field. The content field may be null.
 * Type and content are guaranteed to match.
 */
public class FieldWithContent extends Field implements Serializable
{
    /**
     * The contents of the field, as entered in by the user. This must adhere to the type of this
     * Field.
     */
    protected Serializable content;

    /**
     * The Uri representing the file which will be converted into the Serializable content object
     * at some point. If this is non-null, the content field is assumed to be filled.
     *
     * If this is null, the content field may still be non-null!
     *
     * This should only ever be non-null if the content actually corresponds to a file
     * (i.e. Image, Video, or Audio content.)
     */
    protected Uri contentFileUri;

    //change this if and only if a new implementation is incompatible with an old one
    private static final long serialVersionUID = 1L;

    /**
     * Construct with non-null content.
     * @param title
     * @param type
     * @param isRequired
     * @param content
     */
    public FieldWithContent(String title, FieldType type, boolean isRequired, Serializable content)
    {
        super(title, type, isRequired);

        setContent(content);
        assertNotNull(content);

    }

    /**
     * Construct with null content.
     * @param title
     * @param type
     * @param isRequired
     */
    public FieldWithContent(String title, FieldType type, boolean isRequired)
    {
        super(title, type, isRequired);

        setContent(null);
    }


    /** Serializable implementation. */
    private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException
    {
        title = (String) in.readObject();
        type = (FieldType) in.readObject();
        setContent((Serializable) in.readObject());
    }

    /** Serializable implementation. */
    private void writeObject(ObjectOutputStream out) throws IOException
    {
        out.writeObject(title);
        out.writeObject(type);
        out.writeObject(content);
    }

    /** Serializable implementation. */
    private void readObjectNoData() throws InvalidObjectException {
        throw new InvalidObjectException("Stream data required");
    }

    /**
     * Sets content to the specified value, but only if newContent matches the current type.
     * newContent can be null.
     * @param newContent a Serializable object.
     */
    public void setContent(Serializable newContent)
    {
        assertNotNull(type);

        if (contentMatchesType(newContent, type))
            content =  newContent;
        else
            throw new RuntimeException("field content is not of type " + type + ".");
    }

    public Serializable getContent() {
        return content;
    }

    private boolean typeCouldHaveUri()
    {
        switch(type)
        {
            case IMAGE:
                return true;
            case STRING:
                return false;
            case VIDEO:
                return true;
            case AUDIO:
                return true;
            default:
                return false;
        }
    }

    public Uri getContentFileUri() {
        return contentFileUri;
    }

    public void setContentFileUri(Uri contentFileUri) {
        if (typeCouldHaveUri()) {
            //TODO add file-type-checking, probably using Files.probeContentType().
            this.contentFileUri = contentFileUri;
        }
        else
            throw new RuntimeException("type " + type + "can't have a Uri.");
    }

    public boolean contentIsFilled()
    {
        if (contentFileUri != null)
            return true;

        return content != null;
    }

    public String getContentStringRepresentation()
    {
        if (!contentIsFilled())
            return "";

        switch(type)
        {
            case IMAGE:
                return contentFileUri.toString();
            case STRING:
                return (String) content;
            case VIDEO:
                return contentFileUri.toString();
            case AUDIO:
                return contentFileUri.toString();
            default:
                return "";
        }
    }

    /**
     * Does the given content object match the given field type? i.e. is it an admissible object
     * for the specified field type?
     *
     * NOTE: null content is always admissible.
     *
     * @param content a Serializable content object. This is probably the 'content' field from a
     *                FieldWithContent object.
     * @param type  An instance of FieldType. This is probably the 'type' field from a FieldWithContent object.
     * @return true if the content and type match, false otherwise.
     */
    public static boolean contentMatchesType(Serializable content, FieldType type)
    {
        assertNotNull(type);

        if (content == null)
            return true;

        switch (type)
        {
            case IMAGE:
                return (content instanceof SerialBitmap);
            case VIDEO:
                //TODO implement this.
                throw new RuntimeException("Video object type not yet implemented, sorry!");
            case STRING:
                return (content instanceof String);
            case AUDIO:
                //TODO implement this.
                throw new RuntimeException("Sound object type not yet implemented, sorry!");
            default:
                throw new RuntimeException("field type is invalid.");
        }
    }
}