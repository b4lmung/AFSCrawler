// Trie.java - Implement a trie data structure
//  Methods:
//  (1.1) addStr(String str) - add a string without specifying word freq
//  (1.2) addStr(String str, int freq) - add a string with word freq
//  (2) int containStr(String str) - Seach string in the trie
//        return -1(not found), 0(found partial string), 1(complete string) 
//  (3) int freq(String str) - Find the freq of the search word
//        return -1(not found, partial)

package com.job.nlp.word.segment;
import java.io.*;
import java.util.*;


public class Trie {

  @SuppressWarnings("rawtypes")
private Vector allNode;  //Implementing trie as Vector of Hashtables

  //*****************************************************
  //Constructor
  @SuppressWarnings({ "rawtypes", "unchecked" })
public Trie() {
    allNode = new Vector();
    Hashtable root = new Hashtable();
    allNode.addElement(root); //Adding root node at element 0   
  }
  
  //*****************************************************
  //Add a string into trie (without word freq)
  @SuppressWarnings({ "rawtypes", "unchecked" })
public void addStr(String str) {
    int nodeIndex=0, strIndex=0;
    String stemp;
    Hashtable curNode, newNode;

    while(strIndex<str.length()) {
      stemp="" + str.charAt(strIndex);
      curNode = (Hashtable)allNode.elementAt(nodeIndex);

      if(!curNode.containsKey(stemp)) {
        curNode.put(new String(stemp), new Integer(allNode.size()));
        newNode=new Hashtable();
        nodeIndex=allNode.size();
        allNode.addElement(newNode);
      }
      else {
        Integer val=(Integer) curNode.get(stemp);
        nodeIndex=val.intValue();
      }
      strIndex++;
    }
    curNode=(Hashtable)allNode.elementAt(nodeIndex);
    curNode.put("\0", new Integer(0));
  }

  //******************************************************
  //Add a string into trie (with word freq)
  @SuppressWarnings({ "rawtypes", "unchecked" })
public void addStr(String str, int freq) {
    int nodeIndex=0, strIndex=0;
    String stemp;
    Hashtable curNode, newNode;

    while(strIndex<str.length()) {
      stemp="" + str.charAt(strIndex);
      curNode = (Hashtable) allNode.elementAt(nodeIndex);

      if(!curNode.containsKey(stemp)) {
        curNode.put(new String(stemp), new Integer(allNode.size()));
        newNode=new Hashtable();
        nodeIndex=allNode.size();
        allNode.addElement(newNode);
      }
      else {
        Integer val=(Integer) curNode.get(stemp);
        nodeIndex=val.intValue();
      }
      strIndex++;
    }
    curNode=(Hashtable) allNode.elementAt(nodeIndex);
    curNode.put("\0", new Integer(freq));
  } //addStr

  //*******************************************************
  //Seach string in the trie
  @SuppressWarnings("rawtypes")
public int containStr(String str)
  {
    int nodeIndex=0, strIndex=0;
    String stemp;
    Hashtable curNode;

    while(strIndex<str.length()) {
      stemp="" + str.charAt(strIndex);
      curNode=(Hashtable) allNode.elementAt(nodeIndex);
      if(!curNode.containsKey(stemp))
        return -1; //not found
      else {
        Integer val=(Integer) curNode.get(stemp);
        nodeIndex=val.intValue();
      }
      strIndex++;
    }
    curNode=(Hashtable) allNode.elementAt(nodeIndex);
    if(curNode.containsKey("\0"))
      return 1; //str is a complete word 
    else 
      return 0; //str is part of a word
  }

  //*****************************************************
  //return word frequency
  @SuppressWarnings("rawtypes")
public int freq(String str) {
    int nodeIndex=0, strIndex=0;
    String stemp;
    Hashtable curNode;

    while(strIndex<str.length()) {
      stemp="" + str.charAt(strIndex);
      curNode=(Hashtable) allNode.elementAt(nodeIndex);
      if(!curNode.containsKey(stemp))
        return -1; //not found
      else {
        Integer val=(Integer) curNode.get(stemp);
        nodeIndex=val.intValue();
      }
      strIndex++;
    }
    curNode=(Hashtable) allNode.elementAt(nodeIndex);
    if(curNode.containsKey("\0"))
      return ((Integer)curNode.get("\0")).intValue(); //str is a complete word 
    else 
      return -1; //str is part of a word
  }

  //**************************************************************
  //FOR DEBUGGING PURPOSE
  @SuppressWarnings("unused")
public static void main(String[] args) {
    try {
      int numWord=0, freq;
      String word, word2, line, text, stemp;
      int index, pos;
      Trie dict=new Trie();
          
      //ADDING WORDS
      FileReader fr = new FileReader("test.txt");
      BufferedReader br = new BufferedReader(fr);
      System.out.println("Status: Reading words from dictionary ... ");
      while((line=br.readLine())!=null) {
        line=line.trim();
        if(line.length()>0) {
          if((index=line.indexOf(":"))==-1) //no freq found
            dict.addStr(line.trim());
          else {
            word=line.substring(0,index);
            word2=line.substring(index+1, line.length());
            dict.addStr(word.trim(), Integer.parseInt(word2.trim()));
          }
          numWord++;
        }
      }
      fr.close();
      System.out.println("Status: Number of words read = " + numWord);

      //TESTING WORDS
      fr = new FileReader("test.txt");
      br = new BufferedReader(fr);
      System.out.println("Status: Testing words ... ");
      while((line=br.readLine())!=null) {
        line=line.trim();
        if(line.length()>0) {
          word="";  pos=0;
          while((pos<line.length())&&(line.charAt(pos)>'~'))
            word+=line.charAt(pos++);
          System.out.println(" Word: " + word);
          //Checking word from trie
          if(dict.containStr(word)==-1)
            System.out.println("   Not found");
          else if(dict.containStr(word)==0)
            System.out.println("   Found: Part of a word");
          else
            System.out.println("   Found: Complete word (freq. = " + dict.freq(word) +")");
        }
      }
      fr.close();
    }
    catch (IOException e) {
      System.err.println(e);
    }
  }
}
    