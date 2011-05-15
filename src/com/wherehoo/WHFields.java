package com.wherehoo;

public class WHFields{
    
    private boolean[] values;
    private int[] command_fields = {WHServer.WID,WHServer.LEN,WHServer.HDG,WHServer.RAD,
				    WHServer.DAT,WHServer.SHP,WHServer.SHA,WHServer.ACT,
				    WHServer.IDT,WHServer.PRO,WHServer.UID,WHServer.MIM,
				    WHServer.LLH,WHServer.PJT,WHServer.LIM,WHServer.BEG,
				    WHServer.END,WHServer.MET,WHServer.DBG,WHServer.NOP,
				    WHServer.BYE,WHServer.DOT};

    protected  WHFields (){
	values = new boolean[command_fields.length];
	this.setAll(false);
    }
   /**
     *Sets the field corresponding to command_index to <tt>value</tt>.
     *@param command_index int value of some Wherehoo command.
     *@param value boolean that the field corresponding to command_index is being set to.
     */
    protected void set(int command_index, boolean value){
	try{
	    values[indexOf(command_index)] = value;
	} catch (IndexOutOfBoundsException e){};
    }
    /**
     * Returns the value of the field of the command corresponding to command_index.
     *@param command_index int index of Wherehoo command.
     *@return boolean value of the field of the command corresponding to command_index
     */
    protected boolean get(int command_index){
	return values[indexOf(command_index)];
    }
    /**
     *Sets all fields to boolean <tt>value</tt>.
     *@param value boolean that all the fields of this <tt>WHFields</tt> are being set to.
     */
    protected void setAll(boolean value){
	for (int i=0;i<values.length;i++){
	    values[i]=value;
	}
    }
    private int indexOf(int c){
	for (int i=0;i<command_fields.length;i++){
	    if (command_fields[i]==c) return i;
	}
	return -1;
    }

    /** 
     *returns the String object containing the names of the commands which fields are set to true
     *@return the String object containing the names of the commands which fields are set to trus
     */
    public String toString(){
	String result = "";
	for (int i=0;i<values.length;i++){
	    if (values[i]){
		result += WHServer.commandName(command_fields[i]);
		result += " ";
	    }
	}
	result.trim();
	return result;
    }	
}
