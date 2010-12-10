//=========================================================================
//
//  Part of PEG parser generator Mouse.
//
//  Copyright (C) 2009, 2010 by Roman R. Redziejowski (www.romanredz.se).
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//
//-------------------------------------------------------------------------
//
//  Change log
//    090720 Created for Mouse 1.1.
//   Version 1.2
//    100320 Bug fix in accept(): upgrade error info on success.
//    100320 Bug fix in rejectNot(): backtrack before registering failure.
//   Version 1.3
//    100429 Bug fix in errMerge(Phrase): assignment to errText replaced
//           by clear + addAll (assignment produced alias resulting in
//           explosion of errText in memo version).
//
//=========================================================================

package net.sourceforge.unitsinjava;

import net.sourceforge.unitsinjava.Source;
import java.util.Vector;


//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
//
//  ParserBase
//
//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH


public class ParserBase implements net.sourceforge.unitsinjava.CurrentRule
{
  //-------------------------------------------------------------------
  //  Input
  //-------------------------------------------------------------------
  Source source;                    // Source of text to parse
  int endpos;                       // Position after the end of text
  int pos;                          // Current position in the text

  //-------------------------------------------------------------------
  //  Semantics (base)
  //-------------------------------------------------------------------
  protected net.sourceforge.unitsinjava.SemanticsBase sem;

  //-------------------------------------------------------------------
  //  Trace string.
  //-------------------------------------------------------------------
  protected String trace = "";

  //-------------------------------------------------------------------
  //  Current phrase (top of parse stack).
  //-------------------------------------------------------------------
  Phrase current = null;

  //-------------------------------------------------------------------
  //  Constructor
  //-------------------------------------------------------------------
  protected ParserBase()
    {}

  //-------------------------------------------------------------------
  //  Initialize parsing
  //-------------------------------------------------------------------
  public void init(Source src)
    {
      source = src;
      pos = 0;
      endpos = source.end();
      current = new Phrase("","",0); // Dummy bottom of parse stack
    }

  //-------------------------------------------------------------------
  //  Implementation of Parser interface CurrentRule
  //-------------------------------------------------------------------
  public Phrase lhs()
    { return current; }

  public Phrase rhs(int i)
    { return current.rhs.elementAt(i); }

  public int rhsSize()
    { return current.rhs.size(); }

  public String rhsText(int i,int j)
    { return source.at(rhs(i).start,rhs(j-1).end); }

  //-------------------------------------------------------------------
  //  Set trace
  //-------------------------------------------------------------------
  public void setTrace(String trace)
    {
      this.trace = trace;
      sem.trace = trace;
    }

  //-------------------------------------------------------------------
  //  Print final error message (if not caught otherwise).
  //-------------------------------------------------------------------
  protected boolean failure()
    {
      String message = current.errMsg();
      System.out.println(message.replace("\n","\\n").replace("\t","\\t").replace("\r","\\r"));
      return false;
    }

  //=====================================================================
  //
  //  Methods called from parsing procedures
  //
  //=====================================================================
  //-------------------------------------------------------------------
  //  Initialize processing of a nonterminal
  //-------------------------------------------------------------------
  protected void begin(final String name)
    {
      Phrase p = new Phrase(name,name,pos);
      p.parent = current;
      current = p;
    }

  protected void begin(final String name,final String diag)
    {
      Phrase p = new Phrase(name,diag,pos);
      p.parent = current;
      current = p;
    }

  //-------------------------------------------------------------------
  //  Accept Rule
  //-------------------------------------------------------------------
  protected boolean accept()
    {
      Phrase p = pop();                // Pop p from compile stack
      p.rhs = null;                    // Remove right-hand side of p
      if (p.errPos==p.start)           // Upgrade error info of p
        p.errSet(p.diag,p.start);
      p.success = true;                // Indicate p successful
      current.end = pos;               // Update end of parent
      current.rhs.add(p);              // Attach p to rhs of parent
      current.errMerge(p);             // Merge error info with parent
      return true;
    }

  //-------------------------------------------------------------------
  //  Accept Rule by true return from boolean action
  //-------------------------------------------------------------------
  protected boolean acceptBoolean()
    {
      Phrase p = pop();                // Pop p from compile stack
      p.rhs = null;                    // Remove right-hand side of p
      p.errClear();
      p.success = true;                // Indicate p successful
      current.end = pos;               // Update end of parent
      current.rhs.add(p);              // Attach p to rhs of parent
      return true;
    }

  //-------------------------------------------------------------------
  //  Accept Inner
  //-------------------------------------------------------------------
  protected boolean acceptInner()
    {
      Phrase p = pop();                // Pop p from compile stack
      p.success = true;                // Indicate p successful
      current.end = pos;               // Update end of parent
      current.rhs.addAll(p.rhs);       // Add rhs of p to rhs of parent
      current.errMerge(p);             // Merge error info with parent
      return true;
    }

  //-------------------------------------------------------------------
  //  Accept And-predicate (argument was accepted)
  //-------------------------------------------------------------------
  protected boolean acceptAnd()
    {
      Phrase p = pop();                // Pop p from compile stack
      p.end = p.start;                 // Reset end of P
      p.rhs = null;                    // Remove right-hand side of p
      p.errClear();                    // Remove error info from p
      p.success = true;                // Indicate p successful
      pos = p.start;                   // Backtrack to start of p
      return true;
    }

  //-------------------------------------------------------------------
  //  Accept Not-predicate (argument was rejected)
  //-------------------------------------------------------------------
  protected boolean acceptNot()
    {
      Phrase p = pop();                // Pop p from compile stack
      p.rhs = null;                    // Remove right-hand side of p
      p.errClear();                    // Remove error info from p
      p.success = true;                // Indicate p successful
      return true;
    }



  //-------------------------------------------------------------------
  //  Reject Rule
  //-------------------------------------------------------------------
  protected boolean reject()
    {
      Phrase p = pop();                // Pop p from compile stack
      p.end = p.start;                 // Reset end of p
      p.rhs = null;                    // Remove right-hand side of p
      if (p.errPos==p.start)           // Upgrade error info of p
        p.errSet(p.diag,p.start);
      p.success = false;               // Indicate p failed
      current.errMerge(p);             // Merge error info with parent
      pos = p.start;                   // Backtrack to start of p
      return false;
    }

  //-------------------------------------------------------------------
  //  Reject Rule by false return from boolean action
  //-------------------------------------------------------------------
  protected boolean rejectBoolean()
    {
      Phrase p = pop();                // Pop p from compile stack
      p.end = p.start;                 // Reset end of p
      p.rhs = null;                    // Remove right-hand side of p
      System.out.println(p.diag + " " + source.where(p.start));
      System.out.println(current.errTxt + " " + current.errPos);
      p.errSet(p.diag,p.start);
      p.success = false;               // Indicate p failed
      current.errMerge(p);             // Merge error info with parent
      pos = p.start;                   // Backtrack to start of p
      return false;
    }

  //-------------------------------------------------------------------
  //  Reject Inner
  //-------------------------------------------------------------------
  protected boolean rejectInner()
    {
      Phrase p = pop();                // Pop p from compile stack
      p.end = p.start;                 // Reset end of p
      p.rhs = null;                    // Remove right-hand side of p
      p.success = false;               // Indicate p failed
      current.errMerge(p);             // Merge error info with parent
      pos = p.start;                   // Backtrack to start of p
      return false;
    }

  //-------------------------------------------------------------------
  //  Reject And-predicate (argument was rejected)
  //-------------------------------------------------------------------
  protected boolean rejectAnd()
    {
      Phrase p = pop();                // Pop p from compile stack
      p.rhs = null;                    // Remove right-hand side of p
      p.errSet(p.diag,pos);            // Register 'xxx expected'
      p.success = false;               // Indicate p failed
      current.errMerge(p);             // Merge error info with parent
      return false;
    }

  //-------------------------------------------------------------------
  //  Reject Not-predicate (argument was accepted)
  //-------------------------------------------------------------------
  protected boolean rejectNot()
    {
      Phrase p = pop();                // Pop p from compile stack
      p.end = p.start;                 // Reset end of p
      p.rhs = null;                    // Remove right-hand side of p
      pos = p.start;                   // Backtrack to start of p
      p.errSet(p.diag,pos);            // Register 'xxx not expected'
      p.success = false;               // Indicate p failed
      current.errMerge(p);             // Merge error info with parent
      return false;
    }


  //-------------------------------------------------------------------
  //  Execute expression 'c'
  //-------------------------------------------------------------------
  protected boolean next(char ch)
    {
      if (pos<endpos && source.at(pos)==ch) return consume(1);
      else return fail("'" + ch + "'");
    }

  //-------------------------------------------------------------------
  //  Execute expression &'c'
  //-------------------------------------------------------------------
  protected boolean ahead(char ch)
    {
      if (pos<endpos && source.at(pos)==ch) return true;
      else return fail("'" + ch + "'");
    }

  //-------------------------------------------------------------------
  //  Execute expression !'c'
  //-------------------------------------------------------------------
  protected boolean aheadNot(char ch)
    {
      if (pos<endpos && source.at(pos)==ch) return fail("not '" + ch + "'");
      else return true;
    }


  //-------------------------------------------------------------------
  //  Execute expression "s"
  //-------------------------------------------------------------------
  protected boolean next(String s)
    {
      int lg = s.length();
      if (pos+lg<=endpos && source.at(pos,pos+lg).equals(s)) return consume(lg);
      else return fail("'" + s + "'");
    }

  //-------------------------------------------------------------------
  //  Execute expression &"s"
  //-------------------------------------------------------------------
  protected boolean ahead(String s)
    {
      int lg = s.length();
      if (pos+lg<=endpos && source.at(pos,pos+lg).equals(s)) return true;
      else return fail("'" + s + "'");
    }

  //-------------------------------------------------------------------
  //  Execute expression !"s"
  //-------------------------------------------------------------------
  protected boolean aheadNot(String s)
    {
      int lg = s.length();
      if (pos+lg<=endpos && source.at(pos,pos+lg).equals(s)) return fail("not '" + s + "'");
      else return true;
    }


  //-------------------------------------------------------------------
  //  Execute expression [s]
  //-------------------------------------------------------------------
  protected boolean nextIn(String s)
    {
      if (pos<endpos && s.indexOf(source.at(pos))>=0) return consume(1);
      else return fail("[" + s + "]");
    }

  //-------------------------------------------------------------------
  //  Execute expression &[s]
  //-------------------------------------------------------------------
  protected boolean aheadIn(String s)
    {
      if (pos<endpos && s.indexOf(source.at(pos))>=0) return true;
      else return fail("[" + s + "]");
    }

  //-------------------------------------------------------------------
  //  Execute expression ![s]
  //-------------------------------------------------------------------
  protected boolean aheadNotIn(String s)
    {
      if (pos<endpos && s.indexOf(source.at(pos))>=0) return fail("not [" + s + "]");
      else return true;
    }


  //-------------------------------------------------------------------
  //  Execute expression [a-z]
  //-------------------------------------------------------------------
  protected boolean nextIn(char a, char z)
    {
      if (pos<endpos && source.at(pos)>=a && source.at(pos)<=z)
        return consume(1);
      else return fail("[" + a + "-" + z + "]");
    }

  //-------------------------------------------------------------------
  //  Execute expression &[a-z]
  //-------------------------------------------------------------------
  protected boolean aheadIn(char a, char z)
    {
      if (pos<endpos && source.at(pos)>=a && source.at(pos)<=z)
        return true;
      else return fail("[" + a + "-" + z + "]");
    }

  //-------------------------------------------------------------------
  //  Execute expression ![a-z]
  //-------------------------------------------------------------------
  protected boolean aheadNotIn(char a, char z)
    {
      if (pos<endpos && source.at(pos)>=a && source.at(pos)<=z)
        return fail("not [" + a + "-" + z + "]");
      else return true;
    }


  //-------------------------------------------------------------------
  //  Execute expression _
  //-------------------------------------------------------------------
  protected boolean next()
    {
      if (pos<endpos) return consume(1);
      else return fail("any character");
    }

  //-------------------------------------------------------------------
  //  Execute expression &_
  //-------------------------------------------------------------------
  protected boolean ahead()
    {
      if (pos<endpos) return true;
      else return fail("any character");
    }

  //-------------------------------------------------------------------
  //  Execute expression !_
  //-------------------------------------------------------------------
  protected boolean aheadNot()
    {
      if (pos<endpos) return fail("end of text");
      else return true;
    }


  //-------------------------------------------------------------------
  //  Pop Phrase from compile stack
  //-------------------------------------------------------------------
  private Phrase pop()
    {
      Phrase p = current;
      current = p.parent;
      p.parent = null;
      return p;
    }

  //-------------------------------------------------------------------
  //  Consume terminal
  //-------------------------------------------------------------------
  private boolean consume(int n)
    {
      Phrase p = new Phrase("","",pos);
      pos += n;
      p.end = pos;
      current.rhs.add(p);
      current.end = pos;
      return true;
    }

  //-------------------------------------------------------------------
  //  Fail
  //-------------------------------------------------------------------
  private boolean fail(String msg)
    {
      current.errMerge(msg,pos);
      return false;
    }



  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
  //
  //  Phrase
  //
  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH

  public class Phrase implements net.sourceforge.unitsinjava.Phrase
  {
    //===================================================================
    //
    //  Data
    //
    //===================================================================

    final String name;
    final String diag;
    final int start;
    int end;
    boolean success;
    Vector<Phrase> rhs = new Vector<Phrase>(10,10);
    Object value = null;
    Phrase parent = null;

    int errPos = -1;
    Vector<String> errTxt   = new Vector<String>();


    //===================================================================
    //
    //  Constructor
    //
    //===================================================================

    Phrase(final String name,final String diag,int start)
      {
        this.name = name;
        this.diag = diag;
        this.start = start;
        this.end = start;
      }

    //===================================================================
    //
    //  Interface 'units.Phrase'
    //
    //===================================================================
    //-----------------------------------------------------------------
    //  Set value
    //-----------------------------------------------------------------
    public void put(Object o)
      { value = o; }

    //-----------------------------------------------------------------
    //  Get value
    //-----------------------------------------------------------------
    public Object get()
      { return value; }

    //-----------------------------------------------------------------
    //  Get text
    //-----------------------------------------------------------------
    public String text()
      { return source.at(start,end); }

    //-------------------------------------------------------------------
    //  Get i-th character of text
    //-------------------------------------------------------------------
    public char charAt(int i)
      { return source.at(start+i); }

    //-----------------------------------------------------------------
    //  Is text empty?
    //-----------------------------------------------------------------
    public boolean isEmpty()
      { return start==end; }

    //-----------------------------------------------------------------
    //  Is this s?
    //-----------------------------------------------------------------
    public boolean isA(String s)
      { return name.equals(s); }

    //-----------------------------------------------------------------
    //  Get error message
    //-----------------------------------------------------------------
    public String errMsg()
      {
        if (errPos<0) return "";
        return source.where(errPos) + ": expected " + listErr();
      }

    //-----------------------------------------------------------------
    //  Clear error message
    //-----------------------------------------------------------------
    public void errClear()
      {
        errTxt.clear();
        errPos = -1;
      }


    //===================================================================
    //
    //  Operations on error info
    //
    //===================================================================

    void errSet(final String msg, int where)
      {
        errTxt.clear();
        errTxt.add(msg);
        errPos = where;
      }

    void errMerge(final String msg, int newPos)
      {
        if (errPos<pos && newPos<pos)   // If we passed all error points
        {
          errClear();
          return;
        }

        if (errPos>newPos) return;      // If new position older: forget
        if (errPos<newPos)              // If new position newer: replace all info
        {
          errTxt.clear();
          errPos = newPos;
          errTxt.add(msg);
          return;
        }
                                        // If error in p at same position: add
        errTxt.add(msg);
      }

    void errMerge(final Phrase p)
      {
        if (p.errPos<pos && errPos<pos) // If we passed all error points
        {
          errClear();
          return;
        }

        if (p.errPos<0) return;         // If no error in p: forget
        if (errPos>p.errPos) return;    // If error in p older: forget
        if (errPos<p.errPos)            // If error in p newer: replace all info
        {
          errTxt.clear();
          errPos = p.errPos;
          errTxt.addAll(p.errTxt);
          return;
        }
                                        // If error in p at same position
        errTxt.addAll(p.errTxt);        // Add messages from p
      }

    //-----------------------------------------------------------------
    //  List errors
    //-----------------------------------------------------------------
    private String listErr()
      {
        StringBuffer sb = new StringBuffer();
        String sp = "";
        Vector<String> done = new Vector<String>();
        for (String s: errTxt)
        {
          if (done.contains(s)) continue;
          sb.append(sp + s);
          done.add(s);
          sp = " or ";
        }
        return sb.toString();
      }
  }

}



