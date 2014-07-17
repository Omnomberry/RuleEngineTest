/*
 * A GraphClass that is the clique class of another GraphClass.
 *
 * $Id$
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */


package teo.isgci.gc;


/**
 * A GraphClass that is the clique class of another class.
 */
public class CliqueClass extends DerivedClass {
    
    /** Creates a new graph class based on <tt>gc</tt>. */
    public CliqueClass(GraphClass gc){
        super();
        setBase(gc);
    }

    
    /**
     * Constructs the name of this class by adding the prefix
     * "clique graphs of " to the name of the base class.
     */
    public void setName(){
        name = "clique graphs of "+ getBase().toString();
        nameExplicit = false;
    }


    public boolean subClassOf(GraphClass gc) {
        if (super.subClassOf(gc))
            return true;
        if (1128 ==gc.getID()) { //intID
            if (!"clique graphs".equals(gc.toString())) // Safety check
                throw new RuntimeException(
                        "1128 expected to be clique graphs");
            return true;
        }
        return false;
    }
    
}
/* EOF */
