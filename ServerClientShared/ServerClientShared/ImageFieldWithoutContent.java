package ServerClientShared;

/**
 * Created by wsv759 on 12/03/15.
 */
public class ImageFieldWithoutContent extends FieldWithoutContent{
    //change this if and only if a new implementation is incompatible with an old one
    private static final long serialVersionUID = 1L;

    public static final String TYPE = "image";

    public ImageFieldWithoutContent(String title, boolean isRequired) {
        super(title, TYPE, isRequired);
    }
}
