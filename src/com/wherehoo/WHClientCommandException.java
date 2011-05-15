package com.wherehoo;

import java.io.*;
/**
 * Signals that a client passed a command not recognized
 * by the current Wherehoo interface
 */
public class WHClientCommandException extends Exception {
    /**
     * Constructs a BadCommandException with null as its error detail message.
     */
    public WHClientCommandException(){}
    /**
     *Constructs a BadCommandException with the specified detail message.
     *@param s the detailed message
     */
    public WHClientCommandException(String s) {
        super(s);
    }
}
