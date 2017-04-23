package com.example.mywork.model;

/**
 * Command line parameter
 * @author mda
 *
 * @param <T> type of parameter
 */
public class CLParam<T> {
    final private String name;
    final private String description;
    final private String prefix;
    final private CLParamConverter<T> converter;
    final private T defvalue;
    private T value = null;

    /**
     * Constructor with defalut value
     * @param name param name
     * @param desc param description
     * @param prefix command line prefix
     * @param defvalue default value
     * @param converter value converter
     */
    public CLParam( String name, String desc, String prefix, T defvalue, CLParamConverter<T> converter ) {
        this.name = name;
        this.description = desc;
        this.prefix = prefix;
        this.converter = converter;
        this.defvalue = defvalue;
    }

    /**
     * Constructor without default value
     * @param name param name
     * @param desc param description
     * @param prefix command line prefix
     * @param converter value converter
     */
    public CLParam( String name, String desc, String prefix, CLParamConverter<T> converter ) {
        this.name = name;
        this.description = desc;
        this.prefix = prefix;
        this.converter = converter;
        this.defvalue = null;
    }

    /**
     * Parses command line arguments
     * @param args command line arguments
     * @throws CLArgumentException if fails
     */
    public void parse(String args[]) throws CLArgumentException{
        for( int i=0;  i<args.length;  ++i ){
            if( args[i].equals(prefix)){
                if( i < args.length-1 ){
                    try{
                        value = this.converter.convert(args[i+1]);
                    } catch( NumberFormatException e){
                        throw new CLArgumentException("Bad command argument value: "+prefix);
                    }
                } else {
                    throw new CLArgumentException("Empty command argument: "+prefix);
                }
                break;
            }
        }

        if( value == null ){
            if( defvalue != null ){
                value = defvalue;
            }

            if( value == null ){
                throw new CLArgumentException("Missing required param");
            }
        }
    }

    /**
     * Gets param name.
     * @return param name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets command line param description.
     * @return description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets command line prefix.
     * @return command line prefix
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Gets param value.
     * @return value
     */
    public T getValue(){
        return value;
    }

    /**
     * Prints param usage.
     */
    public void printUsage() {
        System.out.println( "\t"+this.getPrefix()+" "+this.getDescription() );
        if( defvalue != null ){
            System.out.println("\t\tdefault: "+defvalue.toString());
        }
    }
}
