package com.example.mywork.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Command line parser.
 * @author mda
 *
 */
public class CLParser {

    private List<CLParam> params = new ArrayList<CLParam>();

    /**
     * Constructs class instance.
     * @param params command line parameters.
     */
    public CLParser( CLParam... params ) {
        for( CLParam p : params ){
            this.params.add(p);
        }
    }

    /**
     * Parses command line arguments
     * @param args command line arguments
     * @throws CLArgumentException if fails
     */
    public void parse(String[] args) throws CLArgumentException {
        for( CLParam p : params ){
            p.parse(args);
        }
    }

    /**
     * Gets param value by name.
     * @param param param name
     * @return param value
     */
    public Object get(String param){
        for( CLParam p : params ){
            if( p.getName().equals(param) ){
                return p.getValue();
            }
        }
        return "";
    }

    /**
     * Prints program usage.
     */
    public void printUsage() {
        for( CLParam p : params ){
            p.printUsage();
        }
    }
}
