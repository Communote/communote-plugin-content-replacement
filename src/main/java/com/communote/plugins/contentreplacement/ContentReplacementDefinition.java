package com.communote.plugins.contentreplacement;

import java.io.Serializable;

/**
 * @author Communote GmbH - <a href="http://www.communote.com/">http://www.communote.com/</a>
 */
public class ContentReplacementDefinition implements Serializable {

    private static final long serialVersionUID = -5070682572010969074L;

    private int id;
    private String name;
    private String expression;
    private String replacement;
    private boolean inline;

    /**
     * 
     * @return the regular expression
     */
    public String getExpression() {
        return expression;
    }

    /**
     * 
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * 
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * 
     * @return the replacement of the content
     */
    public String getReplacement() {
        return replacement;
    }

    /**
     * 
     * @return true if the content replacement is in line, otherwise false.
     */
    public boolean isInline() {
        return inline;
    }

    /**
     * @param expression
     *            the regular expression to set
     */
    public void setExpression(String expression) {
        this.expression = expression;
    }

    /**
     * @param id
     *            the id to set
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * 
     * @param inline
     *            set whether the replacement is inline or not
     */
    public void setInline(boolean inline) {
        this.inline = inline;
    }

    /**
     * @param name
     *            the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 
     * @param replacement
     *            the replacement to set
     */
    public void setReplacement(String replacement) {
        this.replacement = replacement;
    }
}
