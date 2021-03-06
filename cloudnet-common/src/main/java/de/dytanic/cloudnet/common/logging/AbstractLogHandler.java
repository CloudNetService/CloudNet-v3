package de.dytanic.cloudnet.common.logging;

/**
 * This is a basic abstract implementation of the ILogHandler class.
 * It should help, to create a simple
 */
public abstract class AbstractLogHandler implements ILogHandler {

    /**
     * A formatter with a default initialization value with the DefaultLogFormatter class.
     *
     * @see DefaultLogFormatter
     */
    protected IFormatter formatter = new DefaultLogFormatter();

    public AbstractLogHandler() {
    }

    public AbstractLogHandler(IFormatter formatter) {
        this.formatter = formatter;
    }

    public IFormatter getFormatter() {
        return formatter;
    }

    /**
     * Set the new formatter
     *
     * @return the current instance of the AbstractLogHandler class
     */
    public AbstractLogHandler setFormatter(IFormatter formatter) {
        this.formatter = formatter;
        return this;
    }

    @Override
    public void close() {

    }
}