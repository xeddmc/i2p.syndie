package syndie.data;

import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;
import syndie.Constants;

public class HTMLTag {
    /** tag name, lower case */
    public String name;
    /** attributes on the tag */
    public Properties attributes;
    /** start index for the body text that the tag is applicable to */
    public int startIndex;
    /** the tag was closed at the given body index */
    public int endIndex;
    public int srcLine;
    /** 
     * the tag is at least partly within the parent: <a><b/></a> and <a><b></a><c/></b> both use 'a' as
     * the parent for 'b'
     */
    public HTMLTag parent;
    public boolean consumed;
    
    public HTMLTag(String tagBody, int startIndex, HTMLTag parent, int srcLine) {
        this.startIndex = startIndex;
        endIndex = -1;
        this.parent = parent;
        this.srcLine = srcLine;
        consumed = false;
        attributes = new Properties();
        int attribNameStart = -1;
        int attribNameEnd = -1;
        int attribValueStart = -1;
        int quoteChar = -1;

        if (tagBody.charAt(0) == '/') // endTag
            tagBody = tagBody.substring(1);
        
        int len = tagBody.length();
        for (int i = 0; i < len; i++) {
            char c = tagBody.charAt(i);
            if (Character.isWhitespace(c)) {// || (c == '/')) {
                if (this.name == null) {
                    if (i == 0)
                        this.name = "";
                    this.name = Constants.lowercase(tagBody.substring(0, i));
                } else {
                    if (quoteChar != -1) {
                        // keep going, we are inside a quote
                    } else {
                        if (attribNameStart == -1) {
                            // whitespace outside an attribute.. ignore
                        } else if (attribNameEnd == -1) {
                            // whitespace does terminate an attribute ("href = 'foo'")
                            attribNameEnd = i;
                        } else if (attribValueStart == -1) {
                                // whitespace doesn't start an attribute 
                        } else {
                            // whitespace does terminate an unquoted attribute value though
                            quoteChar = -1;
                            String name = Constants.lowercase(tagBody.substring(attribNameStart, attribNameEnd));
                            String val = tagBody.substring(attribValueStart, i);
                            attributes.setProperty(name, val);
                            attribNameStart = -1;
                            attribNameEnd = -1;
                            attribValueStart = -1;
                        }
                    }
                }
            }  else if ((quoteChar != -1) && (quoteChar == c) && (attribValueStart != -1)) {
                quoteChar = -1;
                String name = Constants.lowercase(tagBody.substring(attribNameStart, attribNameEnd));
                String val = tagBody.substring(attribValueStart, i);
                attributes.setProperty(name, val);
                attribNameStart = -1;
                attribNameEnd = -1;
                attribValueStart = -1;
            } else if (this.name != null) {
                // already have our name, so we are parsing attributes
                if (attribNameStart == -1) {
                    attribNameStart = i;
                } else if (attribNameEnd == -1) {
                    if (c == '=') {
                        attribNameEnd = i;
                    }
                } else if (attribValueStart == -1) {
                    if (c == '\'') {
                        quoteChar = c;
                        attribValueStart = i+1;
                    } else if (c == '\"') {
                        quoteChar = c;
                        attribValueStart = i+1;
                    } else {
                        attribValueStart = i;
                    }
                }
            } else {
                // name not known, and we haven't reached whitespace yet.  keep going
            }
        } // end looping over the tag body
        if ((this.name == null) || (this.name.trim().length() <= 0)) {
            //System.out.println("name is empty for tag [" + tagBody + "] @ " + startIndex);
            this.name = Constants.lowercase(tagBody);
        }
    }
    
    public String getAttribValue(String name) { return attributes.getProperty(Constants.lowercase(name)); }
    public void setAttribValue(String name, String value) { attributes.setProperty(Constants.lowercase(name), value); }
    public void removeAttribValue(String name) { attributes.remove(Constants.lowercase(name)); }
    public boolean wasConsumed() { return consumed; }
    public void consume() { consumed = true; }
    
    public String toString() {
        StringBuffer rv = new StringBuffer();
        rv.append(toHTML());
        rv.append("[" + this.startIndex + (endIndex >= 0 ? ":" + endIndex : ":?") + ":" + this.srcLine + "]");
        return rv.toString();
    }
    public String toHTML() {
        StringBuffer rv = new StringBuffer();
        rv.append('<');
        rv.append(this.name);
        rv.append(' ');
        for (Iterator iter = attributes.keySet().iterator(); iter.hasNext(); ) {
            String name = (String)iter.next();
            String val = attributes.getProperty(name);
            rv.append(name).append('=').append('\'').append(val).append('\'').append(' ');
        }
        rv.append('>');
        return rv.toString();
    }
}