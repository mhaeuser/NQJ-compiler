package analysis;

import frontend.SourcePosition;
import notquitejava.ast.NQJElement;

//
// CHANGE: No longer extend RuntimeException as all type errors are output now
//         (ref FileAnalysisTest.java, testChecker()).
//
/**
 * Wrapper for type errors that saves the position.
 */
public class TypeError {
    private SourcePosition source;

    private String message;

    public TypeError(String message, int line, int column) {
        this.message = message;
        this.source = new SourcePosition("", line, column, line, column);
    }

    /**
     * Type error constructor for an AST class.
     *
     * @param element AST element
     * @param message Error message
     */
    public TypeError(NQJElement element, String message) {
        this.message = message;
        while (element != null) {
            this.source = element.getSourcePosition();
            if (this.source != null) {
                break;
            }
            element = element.getParent();
        }
    }

    public int getLine() {
        return source.getLine();
    }

    public int getColumn() {
        return source.getColumn();
    }

    @Override
    public String toString() {
        //
        // CHANGE: Use the string repesentation of SourcePosition directly.
        //
        return "Error in line " + source.toString() + ": " + getMessage();
    }

    public SourcePosition getSource() {
        return source;
    }

    /**
     * Gets the error message.
     *
     * @return  The error message.
     */
    public String getMessage() {
        return message;
    }
}