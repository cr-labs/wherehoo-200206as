package com.wherehoo;

public class WHCommandIndexFields{
    
    private boolean[] values;
    private int[] command_fields = {WHServer.WID,WHServer.LEN,WHServer.HDG,WHServer.RAD,
				    WHServer.DAT,WHServer.SHP,WHServer.SHA,WHServer.ACT,
				    WHServer.IDT,WHServer.PRO,WHServer.UID,WHServer.MIM,
				    WHServer.LLH,WHServer.PJT,WHServer.LIM,WHServer.BEG,
				    WHServer.END,WHServer.MET};

    protected  WHCommandIndexFields (){
	values = new boolean[command_fields.length];
	this.setAll(true);
    }
    protected void setField(int command_index, boolean value){
	try{
	    values[indexOf(command_index)] = value;
	} catch (IndexOutOfBoundsException e){};
    }

    protected boolean getField(int command_index){
	return values[indexOf(command_index)];
    }
    
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
	
}
