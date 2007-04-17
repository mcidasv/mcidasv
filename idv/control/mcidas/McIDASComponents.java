package ucar.unidata.idv.control.mcidas;



/**
 * Holds a set of definitions concerning McIDAS data.
 */
public interface McIDASComponents {

    /** Image frame component */
    public static final String IMAGE = "McIDASComponents.image";

    /** Graphics frame component */
    public static final String GRAPHICS = "McIDASComponents.graphics";

    /** Color Table frame component */
    public static final String COLORTABLE = "McIDASComponents.colortable";

    /** Image dirty component */
    public static final String DIRTYIMAGE = "McIDASComponents.dirtyimage";

    /** Graphics dirty component */
    public static final String DIRTYGRAPHICS = "McIDASComponents.dirtygraphics";

    /** Color Table dirty component */
    public static final String DIRTYCOLORTABLE = "McIDASComponents.dirtycolortable";
}
