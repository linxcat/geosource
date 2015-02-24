package hoopsnake.geosource.data;

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

    //change this if and only if a new implementation is incompatible with an old one
    private static final long serialVersionUID = 1L;

    public FieldWithContent(String title, FieldType type, boolean isRequired, Serializable content)
    {
        super(title, type, isRequired);

        setContent(content);
        assertNotNull(content);
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

    public void setContent(Serializable newContent)
    {
        assertNotNull(type);
        assertNotNull(newContent);

        if (contentMatchesType(newContent, type))
            content =  newContent;
        else
            throw new RuntimeException("field content is not of type " + type + ".");
    }

    public Serializable getContent() {
        return content;
    }

    /**
     * Does the given content object match the given field type? i.e. is it an admissible object
     * for the specified field type?
     * @param content a Serializable content object. This is probably the 'content' field from a
     *                FieldWithContent object.
     * @param type  An instance of FieldType. This is probably the 'type' field from a FieldWithContent object.
     * @return true if the content and type match, false otherwise.
     */
    public static boolean contentMatchesType(Serializable content, FieldType type)
    {
        assertNotNull(type);
        assertNotNull(content);

        switch (type)
        {
            case IMAGE:
                return (content instanceof SerialBitmap);
            case VIDEO:
                throw new RuntimeException("Video object type not yet implemented, sorry!");
            case STRING:
                return (content instanceof String);
            case SOUND:
                throw new RuntimeException("Sound object type not yet implemented, sorry!");
            default:
                throw new RuntimeException("field type is invalid.");
        }
    }
}
