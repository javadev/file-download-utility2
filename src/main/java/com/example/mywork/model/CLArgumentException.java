package com.example.mywork.model;

/**
 * Command line argument parser exception.
 * @author mda
 *
 */
public class CLArgumentException extends Exception {
    private static final long serialVersionUID = -4605652848572624024L;

    /**
     * Constructor.
     * @param error Error description
     */
    public CLArgumentException(String error) {
        super(error);
    }
}
