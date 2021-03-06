package contract.datastructure;

/**
 * The raw type of the data structure.
 *
 * @author Richard Sundqvist
 */
public enum RawType {
    array("Array", "array", AbstractType.tree), // An array of objects or
    // primitives.
    tree("Tree", "tree"), independentElement("Orphan", "independentElement"); // A
    // loose
    // element,
    // such
    // as
    // a
    // tmp
    // variable.

    /**
     * The permitted AbstractTypes for this RawType.
     */
    public transient final AbstractType[] absTypes;
    public transient final String pretty;
    public transient final String json;

    RawType (String pretty, String json, AbstractType... absType) {
        this.pretty = pretty;
        this.json = json;
        absTypes = absType;
    }

    /**
     * Parse a wrapper string.
     *
     * @param json The string to parse
     * @return The corresponding RawType, if applicable. Null otherwise.
     */
    public static RawType fromString (String json) {
        for (RawType rt : RawType.values()) {
            if (rt.json.equals(json)) {
                return rt;
            }
        }
        return null;
    }

    @Override
    public String toString () {
        return pretty;
    }
}
