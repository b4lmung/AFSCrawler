package com.job.nlp.word.segment;

import java.io.*;
import java.util.*;

import com.job.nlp.word.segment.VTrie;

public class ParseTree {

  //Private variables
  private VTrie dict;              //For storing words from dictionary
  @SuppressWarnings("rawtypes")
private Vector indexList;        //List of index positions
  @SuppressWarnings("rawtypes")
private Vector typeList;         //List of word types
  @SuppressWarnings("rawtypes")
private Vector frontDepChar;     //Front dependent characters: must have front characters
  @SuppressWarnings("rawtypes")
private Vector rearDepChar;      //Rear dependent characters: must have rear characters
  @SuppressWarnings("rawtypes")
private Vector tonalChar;        //Tonal characters
  @SuppressWarnings("rawtypes")
private Vector endingChar;       //Ending characters
      
  /*******************************************************************/
  /************************ Constructor ******************************/
  /*******************************************************************/
  @SuppressWarnings({ "rawtypes", "unchecked" })
public ParseTree(VTrie dict, Vector indexList, Vector typeList) throws IOException {
  	
    this.dict=dict;
    this.indexList=indexList;
    this.typeList=typeList;
     
    frontDepChar=new Vector();
    rearDepChar=new Vector();
    tonalChar=new Vector();
    endingChar=new Vector();
    
    //Adding front-dependent characters
    frontDepChar.addElement("ะ"); frontDepChar.addElement("ั"); frontDepChar.addElement("า"); 
    frontDepChar.addElement("ำ"); frontDepChar.addElement("ิ"); frontDepChar.addElement("ี"); 
    frontDepChar.addElement("ึ"); frontDepChar.addElement("ื"); frontDepChar.addElement("ุ"); 
    frontDepChar.addElement("ู"); frontDepChar.addElement("ๅ"); frontDepChar.addElement("็"); 
    frontDepChar.addElement("์"); frontDepChar.addElement("ํ");

    //Adding rear-dependent characters
    rearDepChar.addElement("ั"); rearDepChar.addElement("ื"); rearDepChar.addElement("เ"); 
    rearDepChar.addElement("แ"); rearDepChar.addElement("โ"); rearDepChar.addElement("ใ"); 
    rearDepChar.addElement("ไ"); rearDepChar.addElement("ํ");

    //Adding tonal characters
    tonalChar.addElement("่"); tonalChar.addElement("้"); tonalChar.addElement("๊"); 
    tonalChar.addElement("๋"); 
    
    //Adding ending characters
    endingChar.addElement("ๆ"); endingChar.addElement("ฯ");    
  }
  
  /****************************************************************/
  /********************** nextWordValid ***************************/
  /****************************************************************/
  private boolean nextWordValid(int beginPos, String text) {

    int pos=beginPos+1;
    int status;
    
    if(beginPos==text.length())
      return true;
    else if(text.charAt(beginPos)<='~')  //English alphabets/digits/special characters
      return true;
    else {
      while(pos<=text.length()) {
      	status=dict.contains(text.substring(beginPos,pos));
        if(status==1)
          return true;
        else if(status==0)
          pos++;
        else
          break;
      }
    }
    return false;
  } //nextWordValid
  
  /****************************************************************/
  /********************** parseWordInstance ***********************/
  /****************************************************************/
  @SuppressWarnings({ "unused", "unchecked" })
public int parseWordInstance(int beginPos, String text) {
  	
    char prevChar='\0';      //Previous character
    int longestPos=-1;       //Longest position
    int longestValidPos=-1;  //Longest valid position
    int domainValidPos=-1;   //Longest domain valid position
    int numValidPos=0;       //Number of longest value pos (for determining ambiguity)
    int returnPos=-1;        //Returned text position
    int returnType=-1;       //Returned word type 
    int pos, status, value;
    
    status=1;
    numValidPos=0;
    pos=beginPos+1;
    while((pos<=text.length())&&(status!=-1)) {
      status=dict.contains(text.substring(beginPos, pos));
     
      //Record maximum frequency so far
      if(status==1) {
      	longestPos=pos;
        value=dict.getValue(text.substring(beginPos, pos));
        if(nextWordValid(pos, text)) {
     	  numValidPos++;
          longestValidPos=pos; 
      	  if(value==1)
            domainValidPos=pos;
        }
      }
      pos++;
    } //while

    //--------------------------------------------------
    //For checking rear dependent character
    if(beginPos>=1)
      prevChar=text.charAt(beginPos-1);    	
    
    //Unknown word
    if(longestPos==-1) {
      returnPos=beginPos+1;      
      //Combine unknown segments	
      if((indexList.size()>0)&&
          (frontDepChar.contains("" + text.charAt(beginPos))||
           tonalChar.contains("" + text.charAt(beginPos))||
           rearDepChar.contains("" + prevChar)||
           (((Integer)typeList.elementAt(typeList.size()-1)).intValue()==0))) {
        indexList.setElementAt(new Integer(returnPos), indexList.size()-1);
        typeList.setElementAt(new Integer(0), typeList.size()-1);
      }
      else { 
        indexList.addElement(new Integer(returnPos));
        typeList.addElement(new Integer(0));
      }
      return returnPos;
    }
    //--------------------------------------------------
    //Known or ambiguous word
    else {
      //If there is no merging point
      if(longestValidPos==-1) {
      	returnPos=longestPos;      	
      	//Check whether front char requires rear segment
      	if(rearDepChar.contains("" + prevChar)) {
          indexList.setElementAt(new Integer(returnPos), indexList.size()-1);
          typeList.setElementAt(new Integer(0), typeList.size()-1);
      	}
      	else {
          typeList.addElement(new Integer(1));
          indexList.addElement(new Integer(returnPos));  
        }
        return(returnPos);  //known followed by unknown: consider longestPos
      }
      else {
      	if(domainValidPos!=-1)
          returnPos=domainValidPos;
      	else
      	  returnPos=longestValidPos;
      	//Check whether front char requires rear segment
      	if(rearDepChar.contains("" + prevChar)) {
          indexList.setElementAt(new Integer(returnPos), indexList.size()-1);
          typeList.setElementAt(new Integer(0), typeList.size()-1);
      	}
        else if(numValidPos==1) {
          typeList.addElement(new Integer(1)); //known
          indexList.addElement(new Integer(returnPos)); 
        }
        else {
          typeList.addElement(new Integer(2)); //ambiguous
          indexList.addElement(new Integer(returnPos));
        }
        return(returnPos);
      }
    }
  } //parseWordInstance
}