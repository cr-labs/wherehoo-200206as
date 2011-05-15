package com.wherehoo;

import java.io.*;
/**
 * Signals that a client passed a command not recognized
 * by the current Wherehoo interface
 */
public class WHTimeException extends Exception {
    /**
     * Constructs a <tt>WHTimeException</tt> with null as its error detail message.
     */
    public WHTimeException(){}
    /**
     *Constructs a <tt>WHTimeException</tt> with the specified detail message.
     *@param s the detailed message
     */
    public WHTimeException(String s) {
        super(s);
    }
}
